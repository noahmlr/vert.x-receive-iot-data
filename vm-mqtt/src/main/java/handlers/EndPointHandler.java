package handlers;

import data.Store;
import io.vertx.core.Handler;
import io.vertx.mqtt.MqttEndpoint;

public class EndPointHandler {

  public static Handler<MqttEndpoint> handler = mqttEndpoint -> {
    // Add new endpoint to list
    Store.getMqttEndpoints().put(mqttEndpoint.clientIdentifier(), mqttEndpoint);
    // Accept the new endpoint
    mqttEndpoint.accept(false);

    // Triggered when MQTT client subscribes to a topic
    mqttEndpoint.subscribeHandler(SubscribeHandler.handler(mqttEndpoint));
    mqttEndpoint.publishHandler(PublishHandler.handler(mqttEndpoint));
    mqttEndpoint.disconnectHandler(DisconnectHandler.handler(mqttEndpoint));
  };

}
