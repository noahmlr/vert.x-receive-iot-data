package handlers;

import data.Store;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Handler;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import models.MqttSubscription;

import java.util.ArrayList;
import java.util.List;

// When a subscription is created you need to add it to a list of subscriptions and
// acknowledge the subscription
public class SubscribeHandler {

  public static Handler<MqttSubscribeMessage> handler(MqttEndpoint mqttEndpoint) {

    return mqttSubscribeMessage -> {
      List<MqttQoS> grantedQosLevels = new ArrayList<>();
      for (MqttTopicSubscription s : mqttSubscribeMessage.topicSubscriptions()) {
        s.topicName();
        grantedQosLevels.add(s.qualityOfService());
      }

      mqttEndpoint.subscribeAcknowledge(mqttSubscribeMessage.messageId(), grantedQosLevels);
    };
  }

}
