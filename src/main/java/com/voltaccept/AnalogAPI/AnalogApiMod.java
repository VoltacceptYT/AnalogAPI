package com.voltaccept.AnalogAPI;

import com.voltaccept.AnalogAPI.api.AnalogAPI;
import com.voltaccept.AnalogAPI.server.AnalogApiServer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnalogApiMod implements ClientModInitializer {
  public static final String MOD_ID = "analog_api";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  private static AnalogApiServer server;

  @Override
  public void onInitializeClient() {
    AnalogAPI.getInstance();

    int port = parsePort(System.getProperty("analogapi.port", System.getenv("ANALOGAPI_PORT")), 27800);
    String host = firstNonNull(System.getProperty("analogapi.host"), System.getenv("ANALOGAPI_HOST"), "127.0.0.1");

    try {
      server = new AnalogApiServer(host, port);
      server.start();
      LOGGER.info("AnalogAPI HTTP server started on http://{}:{}", host, port);
    } catch (Exception e) {
      LOGGER.error("Failed to start AnalogAPI HTTP server", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (server != null) server.stop();
    }, "AnalogAPI-Shutdown"));
  }

  public static AnalogApiServer getServer() {
    return server;
  }

  private static int parsePort(String value, int fallback) {
    if (value == null || value.isBlank()) return fallback;
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static String firstNonNull(String... values) {
    for (String v : values) if (v != null && !v.isBlank()) return v;
    return null;
  }
}
