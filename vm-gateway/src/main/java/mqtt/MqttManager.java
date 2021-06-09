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
        new CircuitBreakerOptions().setMaxRetries(5).setTimeout(5000));
    }
    return breaker;
  }

  // create and connect the MQTT client "in" a Circuit Breaker
  public Future<MqttConnAckMessage> startAndConnectMqttClient(Vertx vertx) {

    var mqttPort = Integer.parseInt(Optional.ofNullable(System.getenv("MQTT_PORT")).orElse("1883"));
    var mqttHost = Optional.ofNullable(System.getenv("MQTT_HOST")).orElse("mqtt.home.smart");

    return getBreaker(vertx).execute(promise -> {

      mqttClient = MqttClient.create(vertx);

      mqttClient.connect(mqttPort, mqttHost, ar -> {
        if (ar.succeeded()) {
          promise.complete(ar.result());
        } else {
          promise.fail(ar.cause());
        }
      });
    });

  }

  public Future<Integer> publish(JsonObject object) {
    var mqttClientId = Optional.ofNullable(System.getenv("MQTT_CLIENT_ID")).orElse("gateway");
    return this.mqttClient.publish(mqttClientId, Buffer.buffer(object.toString()), MqttQoS.AT_LEAST_ONCE, false, false);
  }
}

