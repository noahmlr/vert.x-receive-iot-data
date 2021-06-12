package com.smarthome.gateway;

import discovery.DiscoveryManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
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
    this.serviceDiscovery = DiscoveryManager.createDiscoveryService(vertx);
    DiscoveryManager.clear(serviceDiscovery);

    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    ServiceDiscoveryRestEndpoint.create(router, this.serviceDiscovery);
    String token = Optional.ofNullable(System.getProperty("GATEWAY_TOKEN")).orElse("smart.home");

    router.post("/register")
      .handler(ctx -> {
        boolean auth = Optional.ofNullable(ctx.request().getHeader("smart.token"))
          .map(value -> value.equals(token))
          .orElse(false);

        ctx.response().putHeader("Content-Type", "application/json");
        if (!auth) {
          ctx.response().setStatusCode(403).end(message("Authentication Failed").toString());
        }

        JsonObject requestBody = ctx.getBodyAsJson();
        Record httpRecord = createHttpRecord(requestBody);

        logger.info("Request Body => {}", requestBody);
        serviceDiscovery.getRecord(new JsonObject().put("name", httpRecord.getName()))
          .onSuccess(record -> publishDevice(record, httpRecord, ctx))
          .onFailure(handler -> {
            logger.error("Failed to register device", handler.getCause());
            ctx.response().setStatusCode(500).end(message("Internal Server Error").toString());
          });
      });

    vertx.setPeriodic(5000, id -> mqttManager.startAndConnectMqttClient(vertx)
      .onSuccess(mqttConnect ->
        serviceDiscovery.getRecords(new JsonObject().put("type", "http-endpoint"))
          .onSuccess(this::publishDeviceData)
          .onFailure(throwable -> logger.error("Failed to read records", throwable)))
      .onFailure(throwable -> logger.error("Failed to connect to MQTT via Circuit Breaker", throwable)));

    server.requestHandler(router).listen(9090);
  }

  private JsonObject copyMetadata(JsonObject original) {
    return original
      .stream()
      .filter(entry -> !Set.of("id", "host", "port").contains(entry.getKey()))
      .collect(Collector.of(JsonObject::new, (object, entry) -> object.put(entry.getKey(), entry.getValue()), JsonObject::mergeIn));
  }

  private JsonObject message(String message) {
    return new JsonObject().put("message", message);
  }

  private void publishDevice(Record record, Record newRecord, RoutingContext context) {
    if (record == null) {
      serviceDiscovery.publish(newRecord)
        .onSuccess(handler -> context.response().setStatusCode(200).end(new JsonObject().put("message", "Successfully registered device").toString()))
        .onFailure(handler -> {
          logger.error("Failed to register device", handler.getCause());
          context.response().setStatusCode(500).end(message("Internal Server Error").toString());
        });
    } else {
      context.response().setStatusCode(400).end(message(String.format("Device already registered with id %s", newRecord.getName())).toString());
    }
  }

  private void publishDeviceData(List<Record> httpServices) {
    List<ServiceReference> serviceReferences = httpServices.stream().map(serviceDiscovery::getReference).collect(Collectors.toList());
    serviceReferences.forEach(reference -> {
      WebClient client = reference.getAs(WebClient.class);
      client.get("/").send()
        .onSuccess(responseHandler -> {
          JsonObject request = responseHandler.bodyAsJsonObject();
          logger.info("Device data {} - {}", reference.record().getName(), request.toString());
          mqttManager.publish(request)
            .onSuccess(i -> logger.info("Published device data {}", i))
            .onFailure(throwable -> logger.error("Failed to publish device data", throwable));
        })
        .onFailure(throwable -> logger.error("Could not invoke device service {}", reference.record().getName(), throwable))
        .onComplete(ar -> reference.release());
    });
  }

  private Record createHttpRecord(JsonObject request) {
    JsonObject requestMetaData = copyMetadata(request);
    String id = request.getString("id");
    return HttpEndpoint.createRecord(
      id,
      request.getString("host"),
      request.getInteger("port"),
      Optional.ofNullable(request.getString("root")).orElse("/"),
      requestMetaData);
  }

  private void releaseDevices() {
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
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    releaseDevices();
    stopPromise.complete();
  }

}
