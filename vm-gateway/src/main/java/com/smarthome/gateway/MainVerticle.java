package com.smarthome.gateway;

import discovery.DiscoveryManager;
import http.DevicesHealth;
import http.Registration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;
import io.vertx.servicediscovery.types.HttpEndpoint;
import mqtt.MqttManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  private final MqttManager mqttManager;
  private ServiceDiscovery serviceDiscovery;

  public MainVerticle() {
    this.mqttManager = new MqttManager();
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
//    mqttManager.startAndConnectMqttClient(vertx).onSuccess(ok -> {
//      System.out.println(mqttManager.getMqttClient().clientId());
//    });
    this.serviceDiscovery = DiscoveryManager.createDiscoveryService(vertx);

    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    ServiceDiscoveryRestEndpoint.create(router, this.serviceDiscovery);

    router.post("/register")
      .handler(ctx -> {
        boolean auth = Optional.ofNullable(ctx.request().getHeader("smart.token"))
          .map(value -> value.equals("smart.home"))
          .orElse(false);

        ctx.response().putHeader("Content-Type", "application/json");
        if (!auth) {
          ctx.response().setStatusCode(403)
            .end(new JsonObject().put("message", "Authentication Failed").toString());
        }

        JsonObject requestBody = ctx.getBodyAsJson();
        JsonObject requestMetaData = copyMetadata(requestBody);
        String name = requestBody.getString("category");
        Record httpRecord = HttpEndpoint.createRecord(
          name,
          requestBody.getString("host"),
          requestBody.getInteger("port"),
          "/",
          requestMetaData);
        logger.info("Request Body => {}, Request Metadata => {}", requestBody, requestMetaData);
        serviceDiscovery.getRecord(new JsonObject().put("name", name), ar -> {
          if (ar.succeeded()) {
            Record result = ar.result();
            if (result == null) {
              serviceDiscovery.publish(httpRecord, par -> {
                if (par.succeeded()) {
                  ctx.response().setStatusCode(200).end(new JsonObject().put("message", "Successfully registered device").toString());
                } else {
                  logger.error("Failed to register device", ar.cause());
                  ctx.response().setStatusCode(500).end(new JsonObject().put("message", "Internal Server Error").toString());
                }
              });
            } else {
              ctx.response().setStatusCode(400).end(new JsonObject().put("message", String.format("Device already registered with name %s", name)).toString());
            }
          } else {
            logger.error("Failed to register device", ar.cause());
            ctx.response().setStatusCode(500).end(new JsonObject().put("message", "Internal Server Error").toString());
          }
        });
      });

    vertx.setPeriodic(5000, id -> {
      mqttManager.startAndConnectMqttClient(vertx)
        .onSuccess(mqttConnect -> {
          serviceDiscovery.getRecords(new JsonObject().put("type", "http-endpoint"))
            .onSuccess(handler -> {
              List<ServiceReference> serviceReferences = handler.stream().map(serviceDiscovery::getReference).collect(Collectors.toList());
              serviceReferences.forEach(reference -> {
                WebClient client = reference.getAs(WebClient.class);
                client.get("/").send()
                  .onSuccess(responseHandler -> {
                    JsonObject request = responseHandler.bodyAsJsonObject();
                    logger.info("Device data {} - {}", reference.record().getName(), request.toString());
                    mqttManager.publish(request)
                      .onSuccess(i -> logger.info("Published device data {}", i));
                  })
                  .onFailure(throwable -> logger.error("Could not invoke device service", throwable))
                  .onComplete(ar -> reference.release());
              });
            })
            .onFailure(throwable -> logger.error("Failed to read records", throwable));
        });
    });

    server.requestHandler(router).listen(9090);
  }

  private JsonObject copyMetadata(JsonObject original) {
    return original
      .stream()
      .filter(entry -> !Set.of("category", "host", "port").contains(entry.getKey()))
      .collect(Collector.of(JsonObject::new, (object, entry) -> object.put(entry.getKey(), entry.getValue()), JsonObject::mergeIn));
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    this.serviceDiscovery.getRecords((JsonObject) null, ar -> {
      if (ar.succeeded()) {
        List<Record> records = ar.result();
        for (Record record : records) {
          serviceDiscovery.unpublish(record.getRegistration());
        }
      } else {
        logger.error("Failed to release devices", ar.cause());
      }
    });

    stopPromise.complete();
  }

}
