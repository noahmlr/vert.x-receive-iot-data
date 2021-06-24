package com.smarthome.smartdevice;

import communications.Mqtt;
import devices.HttpDevice;
import devices.MqttDevice;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.mqtt.MqttClientOptions;
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
    String deviceLocation = SystemUtil.getProperty("DEVICE_LOCATION", "bedroom");
    String id = SystemUtil.getProperty("DEVICE_ID", "AX3345");
    String deviceType = SystemUtil.getProperty("DEVICE_TYPE", "http");
    logger.info("Device type: {}", deviceType);
    if (!deviceType.equals("http")) {
      createMqttDevice(id, deviceLocation);
      return;
    }
    String httpPort = SystemUtil.getProperty("HTTP_PORT", "8081");
    String hostName = SystemUtil.getProperty("DEVICE_HOSTNAME", "localhost");
    String gatewayToken = SystemUtil.getProperty("GATEWAY_TOKEN", "smart.home");

    HttpDevice httpDevice = new HttpDevice(id, deviceLocation);

    int gatewayPort = Integer.parseInt(SystemUtil.getProperty("GATEWAY_PORT", "9090"));
    httpDevice.createRegisterToGatewayRequest(vertx, "localhost", gatewayPort, false, gatewayToken)
      .sendJsonObject(new JsonObject()
        .put("id", id)
        .put("host", hostName)
        .put("port", Integer.parseInt(httpPort))
        .put("root", "/")
        .put("position", deviceLocation)
        .put("category", deviceType))
      .onSuccess(response -> {
        if (response.statusCode() == 200) {
          logger.info("Successfully registered device to gateway");
          httpDevice.setConnectedToGateway(true);
        } else {
          logger.info("Failed to register device to gateway - status code {}", response.statusCode());
          httpDevice.setConnectedToGateway(false);
        }
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

  private void createMqttDevice(String id, String deviceLocation) {
    String clientId = SystemUtil.getProperty("MQTT_CLIENT_ID", "mqtt_001");
    int port = Integer.parseInt(SystemUtil.getProperty("MQTT_PORT", "1883"));
    String host = SystemUtil.getProperty("MQTT_HOST", "mqtt.home.smart");
    String topic = SystemUtil.getProperty("MQTT_TOPIC", "house");
    boolean mqttSSL = Boolean.parseBoolean(Optional.ofNullable(System.getProperty("MQTT_SSL")).orElse("false"));
    MqttDevice device = new MqttDevice(id, deviceLocation)
      .setHost(host)
      .setPort(port);

    String mqttKey = Optional.ofNullable(System.getProperty("MQTT_KEY")).orElse("things.home.smart.key");
    String mqttCert = Optional.ofNullable(System.getProperty("MQTT_CERT")).orElse("things.home.smart.crt");
    // Options are used to set client id
    MqttClientOptions options = new MqttClientOptions()
      .setClientId(clientId)
      .setKeyCertOptions(new PemKeyCertOptions()
        .setKeyPath(mqttKey)
        .setCertPath(mqttCert))
      .setSsl(mqttSSL);

    vertx.setPeriodic(5000, time -> device.startAndConnectMqttClient(vertx, options)
      .onSuccess(connection -> {
        JsonObject object = device.jsonValue();
        device.getMqttClient().publish(topic, Buffer.buffer(object.toString()), MqttQoS.AT_MOST_ONCE, false, false)
          .onSuccess(i -> logger.info("Successfully published mqtt message"))
          .onFailure(throwable -> logger.error("Failed to publish message", throwable));
      })
      .onFailure(throwable -> logger.error("Failed to connect to MQTT")));

    testSubscription(device, topic, options);
  }

  private void testSubscription(MqttDevice device, String topic, MqttClientOptions options) {
    device.startAndConnectMqttClient(vertx, options)
      .onSuccess(connection -> {
        device.getMqttClient()
          .publishHandler(s -> {
            logger.info("Received Message {}", s.payload().toString());
          }).subscribe(topic, 0);
      });
  }

}
