package sensors;

public class HumiditySensor implements Sensor {

  @Override
  public String getName() {
    return "humidity";
  }

  @Override
  public String getUnit() {
    return "%";
  }

  @Override
  public double getLevel(int t) {
    double minHumidity = 0;
    double maxHumidity = 100;
    return simulate(t, minHumidity, maxHumidity);
  }
}
