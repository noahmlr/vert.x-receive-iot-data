package com.smarthome.gateway;

import io.vertx.core.Vertx;

public class Main {
  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SL4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
