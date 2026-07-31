# coalliance-matchkey

A reference Java implementation of the **Gold Rush MARC matchKey algorithm** used
by the Colorado Alliance of Research Libraries (CoAlliance) to identify common
bibliographic records across heterogeneous library catalogs.

This repository exists so that other libraries — inside the consortium and out —
can generate matchKeys that interoperate with the Gold Rush system, or port the
algorithm to other languages. The canonical algorithm specification lives in
[`docs/CoAlliance_Match_Key.md`](docs/CoAlliance_Match_Key.md).

## What is a matchKey?

Due to the wide variety of cataloging sources and the long bibliographic history
of most collections, no single identifier (OCLC, ISBN, …) is reliable for
matching records across libraries. The matchKey distills a MARC bibliographic
record down to a single fixed-shape string by combining title, publication year,
pagination, edition, publisher, author, and a handful of other normalized
elements. Two records that produce the same matchKey are treated as describing
the same work.

## Algorithm version

The current algorithm version is **`_v07312026`**. The version string is
embedded in every generated matchKey just before the format character, so
consumers can tell at a glance which algorithm produced a given key.

Every matchKey is exactly **188 characters**. Through `_v03182026` an absent
title (no 245) or an absent publisher (no 264$b/260$b) emitted a zero-width
section instead of padding, so keys could be 188, 183, 93 or 88 characters.
Both cases now pad to full width. Keys carrying `_v03182026` are **not**
comparable to `_v07312026` keys for records lacking a 245 or a publisher. See
[`docs/CoAlliance_Match_Key.md`](docs/CoAlliance_Match_Key.md) for the field-by-field
specification and an annotated example.

## Status

Production. Since algorithm version `_v03182026`, this library **is** the
implementation the CoAlliance production indexer runs: `CoAllianceIndexUtil` in
MarcImporter delegates matchKey generation to `MatchKeyGenerator` from this repo.
The two were verified byte-identical across ~740k real MARC records before the
cutover. The algorithm documentation remains authoritative for *intent*; this
source is authoritative for *behaviour*.

## Relationship to the production indexer

The CoAlliance MarcImporter (not public) consumes this library directly — it
ships `coa_matchkey_v07312026.jar` and calls `MatchKeyGenerator`. This repository
is therefore the canonical implementation, not a copy. When the algorithm changes
— which happens rarely and is signalled by a version-string bump — this repo is
updated and MarcImporter picks up the new jar.

## Porting to other languages

The Java sources here are deliberately structured one element per file so that
a port to Python / Ruby / Go / Rust can be done class-for-class. The JUnit tests
under `src/test/java/org/coalliance/matchkey/` are the behavioural contract: each
extractor has a test that builds MARC records in code (via marc4j `MarcFactory`)
and asserts the exact matchKey section it produces, including the edge cases that
matter for index compatibility — every section is padded to its fixed width even
when its source field is absent, authors are cleaned twice so leading punctuation
is stripped, and Leader/06='m' marks a record electronic. A port is "done" when it
reproduces those same assertions.

Note for anyone who ported from `_v03182026`: that version emitted a zero-width
section for an empty publisher and for a record with no 245, so keys were not a
fixed length. That was a defect, not a contract — see
[issue #1](https://github.com/co-alliance/coalliance-matchkey/issues/1).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

## About CoAlliance

The [Colorado Alliance of Research Libraries](https://coalliance.org) is a
consortium of academic research libraries operating the Gold Rush family of
shared-collection-analysis tools.
