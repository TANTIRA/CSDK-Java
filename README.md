# CSDK-Java

Converge SDK runtime for Java (`dev.converge:converge-sdk`).

This is the shared runtime that generated Converge ontology SDKs compile
against. Generated sources are produced per ontology by Converge
(`GET /v2/ontologies/{ontology}/osdk/java`) under `dev.converge.osdk.*`
and import `dev.converge.sdk.*`; they also work standalone because the
same runtime classes emit alongside them — the two stay in sync
(`ontology/api/app/osdk_java.py`).

```java
ConvergeClient c = Ontology.client("https://host/api", token);
List<Order> big = Order.objects(c).where(Order.Props.totalValue.gt(5.0)).take(10);
```

Requires Java 17+ and Jackson (`jackson-databind`). Build: `mvn package`.
