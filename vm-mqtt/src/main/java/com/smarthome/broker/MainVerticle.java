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
import io.vertx.core.net.PemKeyCertOptions;
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
    boolean mqttSSL = Boolean.parseBoolean(Optional.ofNullable(System.getProperty("MQTT_SSL")).orElse("false"));

    String connectionString = String.format("mongodb://%s:%d", mongoHost, mongoPort);

    MongoStore.initialize(vertx, connectionString, mongoBaseName);

    String mqttKey = Optional.ofNullable(System.getProperty("MQTT_KEY")).orElse("things.home.smart.key");
    String mqttCert = Optional.ofNullable(System.getProperty("MQTT_CERT")).orElse("things.home.smart.crt");
    var mqttPort = Integer.parseInt(Optional.ofNullable(System.getenv("MQTT_PORT")).orElse("1883"));
    MqttServerOptions options = new MqttServerOptions()
      .setPort(mqttPort)
      .setKeyCertOptions(new PemKeyCertOptions()
        .setKeyPath(mqttKey)
        .setCertPath(mqttCert)
      ).setSsl(mqttSSL);
    MqttServer mqttServer = MqttServer.create(vertx, options);


    mqttServer
      .endpointHandler(EndPointHandler.handler)
      .listen()
      .onSuccess(success -> logger.info("MQTT server is listening on port {}", success.actualPort()))
      .onFailure(throwable -> logger.error("Failed to listen to port: {}", mqttPort, throwable));

  }

}
