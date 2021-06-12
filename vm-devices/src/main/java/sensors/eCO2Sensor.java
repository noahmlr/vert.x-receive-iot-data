package sensors;

public class eCO2Sensor implements Sensor {

  @Override
  public String getName() {
    return "eC02";
  }

  @Override
  public String getUnit() {
    return "ppm";
  }

  @Override
  public double getLevel(int t) {
    double minPpm = 400;
    double maxPpm = 20_000;
    return simulate(t, 400, 20_000);
  }
}
