package devices;

import communications.Mqtt;
import io.vertx.mqtt.MqttClient;
import sensors.HumiditySensor;
import sensors.Sensor;
import sensors.TemperatureSensor;
import sensors.eCO2Sensor;

import java.util.List;

public class MqttDevice implements Device, Mqtt {

  private static final String CATEGORY = "mqtt";
  private final String id;
  private final String location;
  private final List<Sensor> sensors;
  private String host;
  private int port;
  private MqttClient mqttClient;

  public MqttDevice(String id, String location) {
    this.id = id;
    this.location = location;
    this.sensors = List.of(new TemperatureSensor(), new HumiditySensor(), new eCO2Sensor());
  }

  @Override
  public MqttClient getMqttClient() {
    return this.mqttClient;
  }

  @Override
  public void setMqttClient(MqttClient mqttClient) {
    this.mqttClient = mqttClient;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getLocation() {
    return location;
  }

  @Override
  public List<Sensor> getSensors() {
    return sensors;
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public String getHost() {
    return this.host;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public MqttDevice setHost(String host) {
    this.host = host;
    return this;
  }

  @Override
  public MqttDevice setPort(int port) {
    this.port = port;
    return this;
  }
}
