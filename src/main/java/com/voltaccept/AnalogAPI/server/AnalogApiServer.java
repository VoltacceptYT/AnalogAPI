package com.voltaccept.AnalogAPI.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.voltaccept.AnalogAPI.api.AnalogAPI;
import com.voltaccept.AnalogAPI.api.MovementState;
import com.voltaccept.AnalogAPI.api.VirtualInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class AnalogApiServer {
  private final String host;
  private final int port;
  private HttpServer server;

  public AnalogApiServer(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress(host, port), 0);
    server.setExecutor(Executors.newCachedThreadPool());

    server.createContext("/api/health", new HealthHandler());
    server.createContext("/api/state", new StateHandler());
    server.createContext("/api/virtual-input", new VirtualInputHandler());

    server.start();
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  private static void writeJson(HttpExchange ex, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    ex.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(bytes);
    }
  }

  private static String readBody(HttpExchange ex) throws IOException {
    try (InputStream is = ex.getRequestBody()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static final class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      writeJson(ex, 200, "{\"ok\":true,\"name\":\"AnalogAPI\"}");
    }
  }

  private static final class StateHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
        writeJson(ex, 405, "{\"error\":\"method_not_allowed\"}");
        return;
      }
      MovementState state = AnalogAPI.getInstance().getMovementState();
      writeJson(ex, 200, state.toJson());
    }
  }

  private static final class VirtualInputHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
      String method = ex.getRequestMethod().toUpperCase();
      AnalogAPI api = AnalogAPI.getInstance();

      switch (method) {
        case "GET" -> {
          VirtualInput vi = api.getVirtualInput();
          if (vi == null) {
            writeJson(ex, 200, "{\"active\":false}");
          } else {
            String body = "{\"active\":true,\"forward\":" + vi.forward
                + ",\"sideways\":" + vi.sideways
                + ",\"jump\":" + vi.jump
                + ",\"sneak\":" + vi.sneak
                + ",\"sprint\":" + vi.sprint + "}";
            writeJson(ex, 200, body);
          }
        }
        case "DELETE" -> {
          api.clearVirtualInput();
          writeJson(ex, 200, "{\"active\":false}");
        }
        case "POST", "PUT" -> {
          try {
            Map<String, String> fields = JsonLite.parseFlat(readBody(ex));
            float forward = parseFloat(fields.get("forward"), 0f);
            float sideways = parseFloat(fields.get("sideways"), 0f);
            boolean jump = parseBool(fields.get("jump"));
            boolean sneak = parseBool(fields.get("sneak"));
            boolean sprint = parseBool(fields.get("sprint"));
            api.setVirtualInput(new VirtualInput(forward, sideways, jump, sneak, sprint));
            writeJson(ex, 200, "{\"active\":true}");
          } catch (Exception e) {
            writeJson(ex, 400, "{\"error\":\"invalid_json\",\"message\":\""
                + e.getMessage().replace("\"", "'") + "\"}");
          }
        }
        default -> writeJson(ex, 405, "{\"error\":\"method_not_allowed\"}");
      }
    }

    private static float parseFloat(String v, float fallback) {
      if (v == null) return fallback;
      try { return Float.parseFloat(v); } catch (NumberFormatException e) { return fallback; }
    }

    private static boolean parseBool(String v) {
      return v != null && (v.equalsIgnoreCase("true") || v.equals("1"));
    }
  }

  static final class JsonLite {
    static Map<String, String> parseFlat(String json) {
      Map<String, String> out = new HashMap<>();
      if (json == null) return out;
      String s = json.trim();
      if (s.isEmpty()) return out;
      if (s.startsWith("{")) s = s.substring(1);
      if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
      int i = 0;
      while (i < s.length()) {
        while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
        if (i >= s.length()) break;
        if (s.charAt(i) != '"') throw new IllegalArgumentException("expected '\"' at " + i);
        int keyStart = ++i;
        while (i < s.length() && s.charAt(i) != '"') i++;
        String key = s.substring(keyStart, i);
        i++;
        while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ':')) i++;
        if (i >= s.length()) throw new IllegalArgumentException("missing value for " + key);
        String value;
        if (s.charAt(i) == '"') {
          int valStart = ++i;
          while (i < s.length() && s.charAt(i) != '"') i++;
          value = s.substring(valStart, i);
          i++;
        } else {
          int valStart = i;
          while (i < s.length() && s.charAt(i) != ',' && !Character.isWhitespace(s.charAt(i))) i++;
          value = s.substring(valStart, i);
        }
        out.put(key, value);
      }
      return out;
    }
  }
}
