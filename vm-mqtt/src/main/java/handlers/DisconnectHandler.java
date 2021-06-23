package handlers;

import data.Store;
import io.vertx.core.Handler;
import io.vertx.mqtt.MqttEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Remove the endpoint from the list of the endpoints
public class DisconnectHandler {
  private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

  public static Handler<Void> handler(MqttEndpoint mqttEndpoint) {
    return  (Void unused)  -> {
      Store.getMqttEndpoints().remove(mqttEndpoint.clientIdentifier());
      logger.info("Removed {} from endpoint list", mqttEndpoint.clientIdentifier());
    };
  }
}
