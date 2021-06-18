package devices;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import sensors.Sensor;

import java.util.List;

public interface Device {
  List<Sensor> getSensors();

  String getId();

  String getLocation();

  String getCategory();

  default JsonObject jsonValue() {
    return new JsonObject()
      .put("id", getId())
      .put("location", getLocation())
      .put("category", getCategory())
      .put("sensors", createSensorArray());
  }

  private JsonArray createSensorArray() {
    return getSensors()
      .stream()
      .map(Sensor::jsonValue)
      .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
  }
}
