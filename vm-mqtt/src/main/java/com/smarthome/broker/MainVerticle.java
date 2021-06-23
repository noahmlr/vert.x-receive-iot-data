package com.smarthome.broker;

import data.MongoStore;
import data.Store;
import handlers.DisconnectHandler;
import handlers.EndPointHandler;
import handlers.PublishHandler;
import handlers.SubscribeHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    stopPromise.complete();
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    String mongoHost = Optional.ofNullable(System.getProperty("MONGO_HOST")).orElse("localhost");
    int mongoPort = Integer.parseInt(Optional.ofNullable(System.getProperty("MONGO_HOST")).orElse("27017"));
    String mongoBaseName = Optional.ofNullable(System.getProperty("MONGO_BASE_NAME")).orElse("smarthome_db");

    String connectionString = String.format("mongodb://%s:%d", mongoHost, mongoPort);

    MongoStore.initialize(vertx, connectionString, mongoBaseName);

    MqttServer mqttServer = MqttServer.create(vertx);
    int mqttPort = 1883;

    mqttServer
      .endpointHandler(EndPointHandler.handler)
      .listen(mqttPort)
      .onSuccess(success -> logger.info("MQTT server is listening on port {}", success.actualPort()))
      .onFailure(throwable -> logger.error("Failed to listen to port: {}", mqttPort, throwable));

  }

}
