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

import java.util.Optional;

public class MqttManager {
  private MqttClient mqttClient;
  private CircuitBreaker breaker;

  public MqttClient getMqttClient() {
    return mqttClient;
  }

  // get a circuit breaker
  private CircuitBreaker getBreaker(Vertx vertx) {
    if(breaker==null) {
      breaker = CircuitBreaker.create("circuit-breaker", vertx);
    }
    return breaker;
  }

  // create and connect the MQTT client "in" a Circuit Breaker
  public Future<MqttConnAckMessage> startAndConnectMqttClient(Vertx vertx) {

    var mqttPort = Integer.parseInt(Optional.ofNullable(System.getenv("MQTT_PORT")).orElse("1883"));
    var mqttHost = Optional.ofNullable(System.getenv("MQTT_HOST")).orElse("mqtt.home.smart");

    return getBreaker(vertx).execute(promise -> {

      mqttClient = MqttClient.create(vertx);

      // some code executing with the breaker
      // the code reports failures or success on the given promise.
      // if this promise is marked as failed, the breaker increased the
      // number of failures

      // connect the mqttClient
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

