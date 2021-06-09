package discovery;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
