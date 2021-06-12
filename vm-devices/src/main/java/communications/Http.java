package communications;

import devices.Device;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;

public interface Http {
  HttpRequest<Buffer> createRegisterToGatewayRequest(Vertx vertx, String domainName, int port, boolean ssl, String token);

  Router createRouter(Vertx vertx);

  HttpServer createHttpServer(Vertx vertx, Router router);

  Device setPort(int port);
}
