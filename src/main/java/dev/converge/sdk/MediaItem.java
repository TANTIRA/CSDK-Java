// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/** One media reference property of one object. The server checks the object and the media set. */
public final class MediaItem {
  private final ConvergeClient client;
  private final String path;
  /** The MediaReference the property holds (an object, or JSON text), or null. */
  public final JsonNode reference;

  public MediaItem(ConvergeClient client, String objectType, String primaryKey, String property, JsonNode reference) {
    this.client = client;
    String rid = primaryKey.contains("/") ? primaryKey : objectType.toLowerCase(java.util.Locale.ROOT) + "/" + primaryKey;
    this.path = "/objects/" + ConvergeClient.seg(objectType) + "/" + ConvergeClient.seg(rid)
        + "/media/" + ConvergeClient.seg(property);
    this.reference = reference != null && reference.isTextual() ? parse(reference.asText()) : reference;
  }

  /** The server may hold the reference as JSON text; null when it is not JSON. */
  private static JsonNode parse(String text) {
    try {
      return ConvergeClient.JSON.readTree(text);
    } catch (java.io.IOException e) {
      return null;
    }
  }

  /** The bytes, as a stream to close. Untrusted: never serve them back as a page (HTML, SVG) on your own origin.
   * shortcut: buffered in memory, so very large items cost their size; stream the response if that matters. */
  public java.io.InputStream openStream() {
    return new java.io.ByteArrayInputStream(client.bytes("GET", path + "/content", null, null));
  }

  /** path, sizeBytes and mediaType. */
  public JsonNode fetchMetadata() {
    return client.request("GET", path + "/metadata", null);
  }

  /** Upload bytes into the media set of a media property -> the MediaReference an action writes. */
  public static JsonNode upload(ConvergeClient client, String objectType, String property, byte[] data, String mediaItemPath) {
    String q = mediaItemPath == null ? "" : "?mediaItemPath=" + ConvergeClient.seg(mediaItemPath);
    byte[] r = client.bytes("POST", "/objectTypes/" + ConvergeClient.seg(objectType) + "/media/"
        + ConvergeClient.seg(property) + "/upload" + q, java.util.Objects.requireNonNull(data, "data"),
        "application/octet-stream");
    try {
      return ConvergeClient.JSON.readTree(r);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }
}
