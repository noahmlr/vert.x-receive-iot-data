package util;

import java.util.Optional;

public class SystemUtil {
  private SystemUtil() {

  }

  public static String getProperty(String key, String defaultValue) {
    return Optional.ofNullable(System.getProperty(key)).orElse(defaultValue);
  }
}
