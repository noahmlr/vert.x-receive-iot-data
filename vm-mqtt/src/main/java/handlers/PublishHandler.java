package handlers;

import data.MongoStore;
import data.Store;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

// (1) Store the message to a MongoDB collection
// (2) Dispatch the message to all subscribed clients of that topic
// (3) Acknowledge the message
public class PublishHandler {
  private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

  public static Handler<MqttPublishMessage> handler(MqttEndpoint mqttEndpoint) {
    return mqttPublishMessage -> {
      JsonObject mongoDocument = createMongoDocument(mqttPublishMessage.payload().toJsonObject(), mqttPublishMessage.topicName());
      MongoStore.getMongoClient().save("devices", mongoDocument)
        .onSuccess(id -> {
          logger.info("Successfully saved object to MongoDB with id: {}", id);
          Store.getMqttSubscriptions().forEach((clientId, subscription) -> {
            if (subscription.getTopic().equals(mqttPublishMessage.topicName())) {
              subscription.getMqttEndpoint()
                .publish(mqttPublishMessage.topicName(),
                  mongoDocument.toBuffer(),
                  MqttQoS.AT_MOST_ONCE,
                  false,
                  false)
                .onSuccess(pid -> logger.info("Successfully published to subscription {}", clientId))
                .onFailure(throwable -> logger.error("Failed to publish to subscription {}", clientId, throwable));
            }
          });

        })
        .onFailure(throwable -> logger.error("Failed to save object to MongoDB", throwable));

      if (mqttPublishMessage.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
        mqttEndpoint.publishAcknowledge(mqttPublishMessage.messageId());
      } else if (mqttPublishMessage.qosLevel() == MqttQoS.EXACTLY_ONCE) {
        mqttEndpoint.publishReceived(mqttPublishMessage.messageId());
      }

    };
  }

  private static JsonObject createMongoDocument(JsonObject message, String topic) {
    LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("America/Chicago"));
    String date = currentTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    String hour = currentTime.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
    return new JsonObject()
      .put("topic", topic)
      .put("device", message)
      .put("date", date)
      .put("hour", hour);
  }
}
