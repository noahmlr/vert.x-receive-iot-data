package handlers;

import data.Store;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Handler;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import models.MqttSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// When a subscription is created you need to add it to a list of subscriptions and
// acknowledge the subscription
public class SubscribeHandler {
  private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

  public static Handler<MqttSubscribeMessage> handler(MqttEndpoint mqttEndpoint) {

    return mqttSubscribeMessage -> {
      logger.info("Client {} requested subscription", mqttEndpoint.clientIdentifier());

      List<MqttQoS> grantedQosLevels = new ArrayList<>();
      if (mqttSubscribeMessage.topicSubscriptions().size() == 1) {
        MqttTopicSubscription topicSubscription = mqttSubscribeMessage.topicSubscriptions().get(0);
        String topicName = topicSubscription.topicName();
        logger.info("Subscription Topic: {}", topicName);
        grantedQosLevels.add(topicSubscription.qualityOfService());
        MqttSubscription mqttSubscription = new MqttSubscription(topicName, mqttEndpoint);
        Store.getMqttSubscriptions().put(mqttEndpoint.clientIdentifier(), mqttSubscription);
      }

      mqttEndpoint.subscribeAcknowledge(mqttSubscribeMessage.messageId(), grantedQosLevels);
    };
  }

}
