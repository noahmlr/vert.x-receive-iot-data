package communications;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.messages.MqttConnAckMessage;

public interface Mqtt {

  String getHost();

  Mqtt setHost(String host);

  int getPort();

  Mqtt setPort(int port);

  MqttClient getMqttClient();

  void setMqttClient(MqttClient client);

  default MqttClient createMqttClient(Vertx vertx) {
    return MqttClient.create(vertx);
  }

  default CircuitBreaker getBreaker(Vertx vertx) {
    return CircuitBreaker.create("breaker", vertx);
  }

  default Future<MqttConnAckMessage> startAndConnectMqttClient(Vertx vertx) {
    return getBreaker(vertx).execute(promise -> {

      MqttClient mqttClient = createMqttClient(vertx);
      setMqttClient(mqttClient);

      getMqttClient().connect(getPort(), getHost(), ar -> {
        if (ar.succeeded()) {
          promise.complete(ar.result());
        } else {
          promise.fail(ar.cause());
        }
      });
    });
  }
}
