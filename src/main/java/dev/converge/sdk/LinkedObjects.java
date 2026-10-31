// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** The objects linked to one object through one link type, as the caller may read them. */
public final class LinkedObjects<T> {
  private final ConvergeClient client;
  private final Class<T> type;
  private final String path;

  public LinkedObjects(ConvergeClient client, Class<T> type, String fromApiName, String primaryKey, String link) {
    this.client = client;
    this.type = type;
    this.path = "/objects/" + ConvergeClient.seg(fromApiName) + "/"
        + ConvergeClient.seg(ObjectSet.bareKey(fromApiName, primaryKey)) + "/links/" + ConvergeClient.seg(link);
  }

  /** pageSize 1 to 1000; pageToken null for the first page. */
  public Page<T> page(int pageSize, String pageToken) {
    String q = "?pageSize=" + pageSize + (pageToken == null ? "" : "&pageToken=" + ConvergeClient.seg(pageToken));
    JsonNode r = client.request("GET", path + q, null);
    List<T> data = new ArrayList<>();
    for (JsonNode o : r.path("data")) data.add(ConvergeClient.JSON.convertValue(o, type));
    JsonNode next = r.path("nextPageToken");
    return new Page<>(data, next.isTextual() ? next.asText() : null, null);
  }

  /** One linked object ("c1" or its $rid "customer/c1"); a ConvergeException 404 when it is not linked. */
  public T get(String primaryKey) {
    return ConvergeClient.JSON.convertValue(
        client.request("GET", path + "/" + ConvergeClient.seg(primaryKey.substring(primaryKey.indexOf('/') + 1)), null), type);
  }
}
