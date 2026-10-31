// Part of the Converge SDK runtime. Synced with TANTIRA/CSDK-Java
// (dev.converge:converge-sdk); edit there or here and mirror the other.
package dev.converge.sdk;

/** A property of an object type, to filter or sort by. */
public final class Property<T> {
  public final String apiName;

  public Property(String apiName) { this.apiName = apiName; }

  public Where eq(T v) { return Where.of("eq", apiName, v); }
  /** Also true for an object with no value, like the server's not(eq). */
  public Where ne(T v) { return Where.not(eq(v)); }
  public Where gt(T v) { return Where.of("gt", apiName, v); }
  public Where gte(T v) { return Where.of("gte", apiName, v); }
  public Where lt(T v) { return Where.of("lt", apiName, v); }
  public Where lte(T v) { return Where.of("lte", apiName, v); }
  public Where range(T lo, T hi) { return Where.of("range", apiName, java.util.List.of(lo, hi)); }
  /** Matches the whole value; ^ and $ are refused by the server. */
  public Where regex(String pattern) { return Where.of("regex", apiName, pattern); }
  /** Whole words, case-insensitive; terms are separated by whitespace or punctuation. */
  public Where containsAnyTerm(String terms) { return Where.of("containsAnyTerm", apiName, terms); }
  public Where containsAllTerms(String terms) { return Where.of("containsAllTerms", apiName, terms); }
  /** The terms as an adjacent phrase, in order. */
  public Where containsAllTermsInOrder(String terms) { return Where.of("containsAllTermsInOrder", apiName, terms); }
  /** As containsAllTermsInOrder, the last term matching as a prefix. */
  public Where containsAllTermsInOrderPrefixLastTerm(String terms) {
    return Where.of("containsAllTermsInOrderPrefixLastTerm", apiName, terms);
  }
  /** Values that start with prefix (a full-match regex on the server, the prefix escaped). */
  public Where startsWith(String prefix) {
    return Where.of("regex", apiName, prefix.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0") + "[\\s\\S]*");
  }
  public Where isNull() { return Where.of("isNull", apiName, true); }
  public Where isNotNull() { return Where.not(isNull()); }
}
