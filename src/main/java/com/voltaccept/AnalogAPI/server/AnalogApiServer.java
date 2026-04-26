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
import java.util.Locale;
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
    server.createContext("/", new StaticFileHandler("/web", "index.html"));

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

  private static final class StaticFileHandler implements HttpHandler {
    private final String resourceRoot;
    private final String indexFile;

    StaticFileHandler(String resourceRoot, String indexFile) {
      String r = resourceRoot;
      while (r.endsWith("/")) r = r.substring(0, r.length() - 1);
      this.resourceRoot = r;
      this.indexFile = indexFile;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
      String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);
      if (!method.equals("GET") && !method.equals("HEAD")) {
        writeJson(ex, 405, "{\"error\":\"method_not_allowed\"}");
        return;
      }

      String path = ex.getRequestURI().getPath();
      if (path == null || path.isEmpty()) path = "/";
      int q = path.indexOf('?');
      if (q >= 0) path = path.substring(0, q);

      String relative = path.startsWith("/") ? path.substring(1) : path;
      if (relative.isEmpty() || relative.endsWith("/")) {
        relative = relative + indexFile;
      }

      if (relative.contains("..") || relative.contains("//") || relative.startsWith("/")) {
        writeJson(ex, 404, "{\"error\":\"not_found\"}");
        return;
      }

      String resourcePath = resourceRoot + "/" + relative;
      try (InputStream is = StaticFileHandler.class.getResourceAsStream(resourcePath)) {
        if (is == null) {
          writeJson(ex, 404, "{\"error\":\"not_found\"}");
          return;
        }
        byte[] bytes = is.readAllBytes();
        ex.getResponseHeaders().add("Content-Type", contentTypeFor(relative));
        ex.getResponseHeaders().add("Cache-Control", "no-cache");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (method.equals("HEAD")) {
          ex.sendResponseHeaders(200, -1);
          ex.getResponseBody().close();
        } else {
          ex.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
          }
        }
      }
    }

    private static String contentTypeFor(String name) {
      String lower = name.toLowerCase(Locale.ROOT);
      if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
      if (lower.endsWith(".css")) return "text/css; charset=utf-8";
      if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=utf-8";
      if (lower.endsWith(".json")) return "application/json; charset=utf-8";
      if (lower.endsWith(".png")) return "image/png";
      if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
      if (lower.endsWith(".gif")) return "image/gif";
      if (lower.endsWith(".svg")) return "image/svg+xml";
      if (lower.endsWith(".ico")) return "image/x-icon";
      if (lower.endsWith(".webp")) return "image/webp";
      if (lower.endsWith(".woff")) return "font/woff";
      if (lower.endsWith(".woff2")) return "font/woff2";
      if (lower.endsWith(".ttf")) return "font/ttf";
      if (lower.endsWith(".txt") || lower.endsWith(".md")) return "text/plain; charset=utf-8";
      return "application/octet-stream";
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
