// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A where clause: eq, gt, gte, lt, lte, range, regex, isNull, and, or, not. and/or stay flat and
 * a double not cancels, because the server caps where depth at 5. */
public final class Where {
  final Map<String, Object> wire;

  Where(Map<String, Object> wire) { this.wire = wire; }

  public static Where of(String type, String property, Object value) {
    Map<String, Object> w = new LinkedHashMap<>();
    w.put("type", type);
    w.put("property", property);
    w.put("value", value);
    return new Where(w);
  }

  @SuppressWarnings("unchecked")
  private static Where join(String op, Where... clauses) {
    List<Object> conds = new ArrayList<>();
    for (Where c : clauses) {
      if (op.equals(c.wire.get("type"))) conds.addAll((List<Object>) c.wire.get("conditions"));
      else conds.add(c.wire);
    }
    if (conds.size() == 1) return new Where((Map<String, Object>) conds.get(0));
    Map<String, Object> w = new LinkedHashMap<>();
    w.put("type", op);
    w.put("conditions", conds);
    return new Where(w);
  }

  public static Where and(Where... clauses) { return join("and", clauses); }

  /** No clause: matches nothing. */
  public static Where or(Where... clauses) {
    if (clauses.length == 0) {
      Map<String, Object> w = new LinkedHashMap<>();
      w.put("type", "or");
      w.put("conditions", List.of());
      return new Where(w);
    }
    return join("or", clauses);
  }

  @SuppressWarnings("unchecked")
  public static Where not(Where clause) {
    if ("not".equals(clause.wire.get("type"))) {
      return new Where((Map<String, Object>) ((List<Object>) clause.wire.get("conditions")).get(0));
    }
    Map<String, Object> w = new LinkedHashMap<>();
    w.put("type", "not");
    w.put("conditions", List.of(clause.wire));
    return new Where(w);
  }

  public Where and(Where other) { return and(this, other); }

  public Where or(Where other) { return or(this, other); }

  public Map<String, Object> toWire() { return wire; }
}
