package devices;

import communications.Http;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sensors.HumiditySensor;
import sensors.Sensor;
import sensors.TemperatureSensor;
import sensors.eCO2Sensor;

import java.util.List;

public class HttpDevice implements Device, Http {

  private static final Logger logger = LoggerFactory.getLogger(HttpDevice.class);

  private final String id;
  private final String location;
  private static final String CATEGORY = "http";
  private final List<Sensor> sensors;

  private int port;
  private boolean connectedToGateway;
  private HttpServer httpServer;

  public HttpDevice(String id, String location) {
    this.id = id;
    this.location = location;
    this.sensors = List.of(new TemperatureSensor(), new HumiditySensor(), new eCO2Sensor());
  }


  @Override
  public HttpRequest<Buffer> createRegisterToGatewayRequest(Vertx vertx, String domainName, int port, boolean ssl, String token) {
    return WebClient.create(vertx).post(port, domainName, "/register")
      .putHeader("smart.token", token);
  }

  @Override
  public Router createRouter(Vertx vertx) {
    return Router.router(vertx);
  }

  @Override
  public HttpServer createHttpServer(Vertx vertx, Router router) {
    this.httpServer = Vertx.vertx().createHttpServer().requestHandler(router);
    return httpServer;
  }

  @Override
  public Device setPort(int port) {
    this.port = port;
    httpServer.listen(port)
      .onSuccess(server -> logger.info("Listening to port {}", port))
      .onFailure(error -> logger.error("Could not listen to port {}", port, error));
    return this;
  }

  @Override
  public JsonObject jsonValue() {
    return new JsonObject()
      .put("id", this.id)
      .put("location", this.location)
      .put("category", CATEGORY)
      .put("sensors", createSensorArray());
  }

  private JsonArray createSensorArray() {
    return this.sensors
      .stream()
      .map(Sensor::jsonValue)
      .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
  }

  public void setConnectedToGateway(boolean connectedToGateway) {
    this.connectedToGateway = connectedToGateway;
  }
}
