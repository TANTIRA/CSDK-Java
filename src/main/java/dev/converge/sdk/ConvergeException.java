// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/** A non-2xx answer (a redirect included): status and the server's error body. */
public final class ConvergeException extends RuntimeException {
  public final int status;
  public final JsonNode body;

  public ConvergeException(int status, JsonNode body) {
    super("HTTP " + status + (body != null && body.has("errorName") ? " " + body.get("errorName").asText() : ""));
    this.status = status;
    this.body = body;
  }
}
