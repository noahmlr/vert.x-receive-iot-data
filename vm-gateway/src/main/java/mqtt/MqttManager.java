package mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttConnAckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MqttManager {
  private static final Logger logger = LoggerFactory.getLogger(MqttManager.class);

  private MqttClient mqttClient;
  private CircuitBreaker breaker;

  private CircuitBreaker getBreaker(Vertx vertx) {
    if (breaker == null) {
      breaker = CircuitBreaker.create("circuit-breaker", vertx,
        new CircuitBreakerOptions().setMaxRetries(2).setTimeout(5000));
    }
    return breaker;
  }

  // create and connect the MQTT client "in" a Circuit Breaker
  public Future<MqttConnAckMessage> startAndConnectMqttClient(Vertx vertx, MqttClientOptions options) {

    var mqttPort = Integer.parseInt(Optional.ofNullable(System.getenv("MQTT_PORT")).orElse("1883"));
    var mqttHost = Optional.ofNullable(System.getenv("MQTT_HOST")).orElse("localhost");

    return getBreaker(vertx).execute(promise -> {

      mqttClient = MqttClient.create(vertx, options);

      mqttClient.connect(mqttPort, mqttHost, ar -> {
        if (ar.succeeded()) {
          promise.complete(ar.result());
        } else {
          logger.error("Failed to connect to MQTT", ar.cause());
          promise.fail(ar.cause());
        }
      });
    });

  }

  public Future<Integer> publish(JsonObject object) {
    var mqttTopic = Optional.ofNullable(System.getenv("MQTT_TOPIC")).orElse("house");
    return this.mqttClient.publish(mqttTopic, Buffer.buffer(object.toString()), MqttQoS.AT_LEAST_ONCE, false, false);
  }
}

