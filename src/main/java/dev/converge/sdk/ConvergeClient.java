// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/** Requests against one API base URL ("https://host/api"). Redirects are refused, so the
 * token never follows one, and a plain-http base is refused except for localhost. */
public final class ConvergeClient {
  public static final ObjectMapper JSON = new ObjectMapper();
  private static final Set<String> LOCAL = Set.of("localhost", "127.0.0.1", "[::1]");
  private final String base;
  private final String token;
  private final String ontology;
  private final HttpClient http;

  /** ontology = the ontology rid every request is scoped to
   *  ("/v2/ontologies/{rid}/..."). A generated package's Ontology.client()
   *  passes its own RID. */
  public ConvergeClient(String baseUrl, String token, String ontology) {
    // checked here, never echoed: HttpClient would put a bad header value (a trailing
    // newline from a token file, say) into its exception message
    if (token == null || !token.matches("[\\x21-\\x7e]+")) {
      throw new IllegalArgumentException("token must be printable ASCII without spaces or line breaks");
    }
    URI u = URI.create(baseUrl);
    String host = u.getHost() == null ? "" : u.getHost().toLowerCase(java.util.Locale.ROOT);
    boolean local = "http".equalsIgnoreCase(u.getScheme()) && u.getRawUserInfo() == null && LOCAL.contains(host);
    if (!"https".equalsIgnoreCase(u.getScheme()) && !local) {
      throw new IllegalArgumentException("the API base must be https (plain http only for localhost)");
    }
    this.base = baseUrl.replaceAll("/+$", "");
    this.token = token;
    this.ontology = java.util.Objects.requireNonNull(ontology, "ontology");
    this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(30)).build();
  }

  @Override public String toString() { return "ConvergeClient(" + base + ")"; }

  public static String seg(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"); }

  /** JSON request to base + /v2/ontologies/{ontology} + path; the parsed answer (null if empty). */
  public JsonNode request(String method, String path, Object body) {
    try {
      byte[] r = bytes(method, path, body == null ? null : JSON.writeValueAsBytes(body), "application/json");
      return r.length == 0 ? null : JSON.readTree(r);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  /** A raw request to base + /v2/ontologies/{ontology} + path (body null for none); the answer's bytes. */
  public byte[] bytes(String method, String path, byte[] body, String contentType) {
    try {
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/v2/ontologies/" + seg(ontology) + path))
          .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + token);
      if (body == null) {
        b.method(method, HttpRequest.BodyPublishers.noBody());
      } else {
        b.header("Content-Type", contentType).method(method, HttpRequest.BodyPublishers.ofByteArray(body));
      }
      HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
      if (r.statusCode() >= 300) {
        JsonNode err = null;
        try {
          err = r.body().length == 0 ? null : JSON.readTree(r.body());
        } catch (java.io.IOException notJson) {
          // keep err null: the status is the answer
        }
        throw new ConvergeException(r.statusCode(), err);
      }
      return r.body();
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
