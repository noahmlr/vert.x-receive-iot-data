package com.smarthome.webapp;

import data.AdminUser;
import io.reactivex.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class MainVerticle extends AbstractVerticle {


  public static void main(String[] args) {
    io.vertx.reactivex.core.Vertx.vertx().deployVerticle(new MainVerticle());
  }

  @Override
  public Completable rxStop() {
    System.out.println("Webapp stopped");
    return super.rxStop();
  }

  @Override
  public Completable rxStart() {
    AdminUser user = new AdminUser("root", "admin");
    int httpPort = Integer.parseInt(Optional.ofNullable(System.getProperty("HTTP_PORT")).orElse("8080"));

    Router router = Router.router(vertx);

    /* These buffers aren't working
    Buffer privateKeyBuffer = Buffer.buffer();
    vertx.fileSystem().readFileBlocking("./private_key.pem").writeToBuffer(privateKeyBuffer);

    Buffer publicKeyBuffer = Buffer.buffer();
    vertx.fileSystem().readFileBlocking("./public_key.pem").writeToBuffer(publicKeyBuffer);
     */

    String privateKey = null;
    try {
      privateKey = String.join("\n", Files.readAllLines(Paths.get("./private_key.pem"), StandardCharsets.UTF_8));
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    String publicKey = null;
    try {
      publicKey = String.join("\n", Files.readAllLines(Paths.get("./public_key.pem"), StandardCharsets.UTF_8));
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    JWTAuthOptions options = new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setBuffer(privateKey))
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setBuffer(publicKey));

    System.out.println("Test");

    JWTAuth jwtAuth = JWTAuth.create(vertx, options);

    JWTAuthHandler jwtHandler = JWTAuthHandler.create(jwtAuth);

    BodyHandler bodyHandler = BodyHandler.create();

    router.route(HttpMethod.POST, "/authenticate")
      .handler(bodyHandler)
      .handler(routingContext -> {
        JsonObject request = routingContext.getBodyAsJson();
        String username = request.getString("username");
        String password = request.getString("password");
        if (username.equals(user.getUsername()) && password.equals(user.getPassword())) {
          user.setAuthenticated(true);

          String s = jwtAuth.generateToken(
            new JsonObject().put("greetingMessage", String.format("Welcome %s", username)),
            new JWTOptions().setAlgorithm("RS256").setExpiresInMinutes(5).setIssuer("issuer").setSubject(username)
          );
          routingContext.response().putHeader("Content-Type", "application/json").setStatusCode(200).end(s);
        } else {
          routingContext.response().setStatusCode(400).end();
        }
      })
      .failureHandler(ctx -> {
        System.out.println("Something went wrong");
        ctx.response().setStatusCode(500).end();
      });

    router.route(HttpMethod.GET, "/say-hello")
      .handler(jwtHandler)
      .handler(routingContext -> {
        JsonObject principal = routingContext.user().principal();
        System.out.println(principal);
        JsonObject response = new JsonObject()
          .put("subject", principal.getString("sub"))
          .put("greetingMessage", principal.getString("greetingMessage"));
        routingContext.response().setStatusCode(200).end(response.toString());
      });

    router.route(HttpMethod.GET, "/disconnect")
      .handler(ctx -> {
        user.setAuthenticated(false);
        ctx.json(new JsonObject().put("message", "disconnected"));
      });

    // =============== Start the http server  ===============
    var httpserver = vertx.createHttpServer().requestHandler(router);

    return httpserver.rxListen(httpPort)
      .doOnSuccess(ok-> {
        System.out.println("Web Application: HTTP server started on port " + httpPort);
      }).doOnError(error -> {
        System.out.println(error.getCause().getMessage());
      }).ignoreElement();

  }

}
