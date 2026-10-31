// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The objects of one type, narrowed by where(); immutable, so a set can be reused. */
public final class ObjectSet<T> {
  private final ConvergeClient client;
  private final Class<T> type;
  private final String apiName;
  private final Where filter;
  private final Map<String, Object> order;

  public ObjectSet(ConvergeClient client, Class<T> type, String apiName, Where filter, Map<String, Object> order) {
    this.client = client;
    this.type = type;
    this.apiName = apiName;
    this.filter = filter;
    this.order = order;
  }

  public ObjectSet<T> where(Where w) {
    return new ObjectSet<>(client, type, apiName, filter == null ? w : Where.and(filter, w), order);
  }

  public ObjectSet<T> orderBy(Property<?> p, boolean descending) {
    Map<String, Object> o = new LinkedHashMap<>();
    o.put("property", p.apiName);
    o.put("direction", descending ? "desc" : "asc");
    return new ObjectSet<>(client, type, apiName, filter, o);
  }

  private String path(String suffix) { return "/objects/" + ConvergeClient.seg(apiName) + suffix; }

  /** pageSize 1 to 1000; pageToken null for the first page. */
  public Page<T> page(int pageSize, String pageToken) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("pageSize", pageSize);
    if (filter != null) body.put("where", filter.wire);
    if (order != null) body.put("orderBy", order);
    if (pageToken != null) body.put("pageToken", pageToken);
    JsonNode r = client.request("POST", path("/search"), body);
    List<T> data = new ArrayList<>();
    for (JsonNode o : r.path("data")) data.add(ConvergeClient.JSON.convertValue(o, type));
    JsonNode next = r.path("nextPageToken");
    JsonNode total = r.path("totalCount");
    return new Page<>(data, next.isTextual() ? next.asText() : null, total.isMissingNode() || total.isNull() ? null : total.asText());
  }

  /** At most n objects, across pages. */
  /** Every object, read page by page as the iteration reaches it. */
  public Iterable<T> iterate(int pageSize) {
    return () -> new java.util.Iterator<T>() {
      private java.util.Iterator<T> page = java.util.Collections.emptyIterator();
      private String token = null;
      private boolean done = false;

      @Override public boolean hasNext() {
        while (!page.hasNext() && !done) {
          Page<T> p = ObjectSet.this.page(pageSize, token);
          page = p.data().iterator();
          token = p.nextPageToken();
          done = token == null;
        }
        return page.hasNext();
      }

      @Override public T next() {
        if (!hasNext()) throw new java.util.NoSuchElementException();
        return page.next();
      }
    };
  }

  public List<T> take(int n) {
    List<T> out = new ArrayList<>();
    String token = null;
    while (out.size() < n) {
      Page<T> p = page(Math.min(Math.max(n, 1), 1000), token);
      for (T o : p.data()) {
        if (out.size() < n) out.add(o);
      }
      token = p.nextPageToken();
      if (token == null) break;
    }
    return out;
  }

  /** One object by primary key ("c1") or by its $rid ("customer/c1"). */
  public T get(String primaryKey) {
    String k = primaryKey.contains("/") ? primaryKey : apiName.toLowerCase(java.util.Locale.ROOT) + "/" + primaryKey;
    return ConvergeClient.JSON.convertValue(client.request("GET", path("/" + ConvergeClient.seg(k)), null), type);
  }

  /** A fixed-width bucket group-by for aggregate(): {"field": p, "fixedWidth": width}. */
  public static Map<String, Object> fixedWidth(Property<?> p, Number width) {
    return Map.of("field", p.apiName, "fixedWidth", width);
  }

  /** A date/time bucket group-by for aggregate(): n units (SECONDS ... YEARS), in UTC; weeks start on Monday. */
  public static Map<String, Object> duration(Property<?> p, int n, String unit) {
    return Map.of("field", p.apiName, "duration", n, "unit", unit);
  }

  /** An exact group-by keeping the n largest groups (the answer is then APPROXIMATE). */
  public static Map<String, Object> maxGroupCount(Property<?> p, int n) {
    return Map.of("field", p.apiName, "maxGroupCount", n);
  }

  /** One metric of a several-metrics aggregate(): type, field (null for count) and name (null for the default). */
  public static Map<String, Object> metric(String type, Property<?> field, String name) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", type);
    if (field != null) m.put("field", field.apiName);
    if (name != null) m.put("name", name);
    return m;
  }

  /** Several metrics in one call; orderByMetric (a metric's name, or null) orders the groups by it. */
  public JsonNode aggregate(List<Map<String, Object>> metrics, List<Object> groupBy, String orderByMetric, boolean descending) {
    Map<String, Object> agg = new LinkedHashMap<>();
    agg.put("metrics", metrics);
    if (filter != null) agg.put("where", filter.wire);
    if (groupBy != null && !groupBy.isEmpty()) agg.put("group_by", groupBy);
    if (orderByMetric != null) agg.put("order", Map.of("metric", orderByMetric, "direction", descending ? "desc" : "asc"));
    return client.request("POST", path("/aggregate"), Map.of("aggregation", agg));
  }

  /** The numNeighbors objects of this set whose vector property is closest to query (exact cosine,
   * as "$score", best first); the answer's "skipped" counts candidates with no comparable vector. */
  public JsonNode nearestNeighbors(List<Double> query, int numNeighbors, Property<?> property) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("property", property.apiName);
    body.put("numNeighbors", numNeighbors);
    body.put("query", Map.of("type", "vector", "value", query));
    if (filter != null) body.put("where", filter.wire);
    return client.request("POST", path("/nearestNeighbors"), body);
  }

  /** A range group-by for aggregate(): each range is [lo, hi]. */
  public static Map<String, Object> ranges(Property<?> p, List<List<?>> ranges) {
    return Map.of("field", p.apiName, "ranges", ranges);
  }

  /** One aggregation over this set (count, sum, avg, min, max, exactDistinct, approximateDistinct);
   * field and groupBy may be null. A groupBy entry is a property name, or a map with "field" and
   * "ranges" or "fixedWidth". The server's answer as is. */
  public JsonNode aggregate(String aggregation, Property<?> field, List<Object> groupBy) {
    Map<String, Object> agg = new LinkedHashMap<>();
    agg.put("type", aggregation);
    if (filter != null) agg.put("where", filter.wire);
    if (field != null) agg.put("field", field.apiName);
    if (groupBy != null && !groupBy.isEmpty()) agg.put("group_by", groupBy);
    return client.request("POST", path("/aggregate"), Map.of("aggregation", agg));
  }

  /** How many objects are in this set. */
  public long count() {
    JsonNode data = aggregate("count", null, null).path("data");
    return data.path(0).path("metrics").path(0).path("value").asLong();
  }

  /** The link routes take the bare primary key: "c1" from "c1" or from the $rid "customer/c1".
   * A $rid of another type is refused, not read as this type's key. */
  static String bareKey(String apiName, String primaryKey) {
    int i = primaryKey.indexOf('/');
    if (i < 0) return primaryKey;
    if (!primaryKey.substring(0, i).equals(apiName.toLowerCase(java.util.Locale.ROOT))) {
      throw new IllegalArgumentException("not a " + apiName + " $rid");
    }
    return primaryKey.substring(i + 1);
  }
}
