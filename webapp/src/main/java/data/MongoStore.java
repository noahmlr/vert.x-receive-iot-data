package data;

import io.reactivex.Flowable;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.reactivex.core.Vertx;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.streams.ReadStream;
import io.vertx.reactivex.ext.mongo.MongoClient;

public class MongoStore {

  private static MongoClient mongoClient;

  public static void initialize(Vertx vertx, String connectionString, String dataBaseName) {
    mongoClient = MongoClient.create(vertx, new JsonObject()
      .put("db_name", dataBaseName)
      .put("connection_string", connectionString));
  }

  public static Flowable<JsonObject> getLastDevicesMetricsFlowable(Integer howMany) {
    ReadStream<JsonObject> devices = mongoClient.findBatchWithOptions("devices", new JsonObject(), new FindOptions().setLimit(howMany));

    return devices.toFlowable();
  }
}
