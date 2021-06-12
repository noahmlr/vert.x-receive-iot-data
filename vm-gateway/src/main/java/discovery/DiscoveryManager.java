package discovery;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class DiscoveryManager {

  private static final Logger logger = LoggerFactory.getLogger(DiscoveryManager.class);

  public static ServiceDiscovery createDiscoveryService(Vertx vertx) {
    String redisHost = Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("localhost");
    logger.info("Connecting to REDIS_HOST {}", redisHost);
    return ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions().setBackendConfiguration(
      new JsonObject()
        .put("connectionString", String.format("redis://%s:6379", redisHost))
        .put("key", "records")
    ));
  }

  public static void clear(ServiceDiscovery serviceDiscovery) {
    boolean releaseDevices = Boolean.parseBoolean(Optional.ofNullable(System.getProperty("RELEASE_DEVICES")).orElse("false"));
    logger.info("Releasing device flag set to {}", releaseDevices);
    if (!releaseDevices) return;
    serviceDiscovery.getRecords((JsonObject) null, ar -> {
      if (ar.succeeded()) {
        List<Record> records = ar.result();
        for (Record record : records) {
          serviceDiscovery.unpublish(record.getRegistration());
        }
      } else {
        logger.error("Failed to release devices", ar.cause());
      }
    });
  }
}
