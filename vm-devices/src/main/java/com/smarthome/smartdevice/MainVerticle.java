package com.smarthome.smartdevice;

import devices.HttpDevice;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.SystemUtil;

import java.util.Optional;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    stopPromise.complete();
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    String httpPort = SystemUtil.getProperty("HTTP_PORT", "8081");
    String deviceType = SystemUtil.getProperty("DEVICE_TYPE", "http");
    if (!deviceType.equals("http")) {
      throw new IllegalArgumentException("This application only supports http devices at the moment");
    }
    String hostName = SystemUtil.getProperty("DEVICE_HOSTNAME", "localhost");
    String deviceLocation = SystemUtil.getProperty("DEVICE_LOCATION", "bedroom");
    String id = SystemUtil.getProperty("DEVICE_ID", "AX3345");
    String gatewayToken = SystemUtil.getProperty("GATEWAY_TOKEN", "smart.home");

    HttpDevice httpDevice = new HttpDevice(id, deviceLocation);

    httpDevice.createRegisterToGatewayRequest(vertx, "localhost", 9090, false, gatewayToken)
      .sendJsonObject(new JsonObject()
        .put("id", id)
        .put("host", hostName)
        .put("port", Integer.parseInt(httpPort))
        .put("root", "/")
        .put("position", deviceLocation)
        .put("category", deviceType))
      .onSuccess(response -> {
        logger.info("Successfully registered device to gateway");
        httpDevice.setConnectedToGateway(true);
      })
      .onFailure(error -> {
        logger.info("Failed to register device to gateway", error);
        httpDevice.setConnectedToGateway(false);
      });


    Router router = httpDevice.createRouter(vertx);
    router.get("/")
      .handler(ctx -> {
        ctx.response().putHeader("Content-Type", "application/json");

        JsonObject responseBody = httpDevice.jsonValue();
        logger.info("Sending response body - {}", responseBody.toString());
        ctx.response().setStatusCode(200).end(responseBody.toString());
      });

    /*
      Start the http server of the device
     */

    httpDevice.createHttpServer(vertx, router);

    httpDevice.setPort(Integer.parseInt(httpPort));

  }

}
