# Distribution artifacts

Prebuilt jars for `coalliance-matchkey`. These are committed into the
repository so consortium members can pull the jar directly without setting
up a Maven build environment. The jar is regenerated and re-committed
whenever a release is cut.

## Files

- **`matchkey-0.1.0-SNAPSHOT.jar`** — thin jar (no marc4j bundled).
  Requires marc4j 2.9.6 on the classpath.

## CLI usage

```bash
java -cp matchkey-0.1.0-SNAPSHOT.jar:marc4j-2.9.6.jar \
     org.coalliance.matchkey.cli.MatchKeyCli file.marc
```

Emits one `recordId<TAB>matchkey` line per record on stdout.

## Library usage

```java
MatchKeyGenerator gen = new MatchKeyGenerator(null);   // or pass a MARC filename hint
String matchkey = gen.generate(marc4jRecord);
```

Or, matching the CoAlliance indexer's CLI convention:

```java
MatchKeyGenerator gen = MatchKeyGenerator.fromSystemProperty();
```

(reads the filename hint from system property `org.coalliance.indexing.fileName`).
