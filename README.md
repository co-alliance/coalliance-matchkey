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

The current algorithm version is **`_v03182026`**. The version string is
embedded in every generated matchKey just before the format character, so
consumers can tell at a glance which algorithm produced a given key. See
[`docs/CoAlliance_Match_Key.md`](docs/CoAlliance_Match_Key.md) for the field-by-field
specification and an annotated example.

## Status

Early. The algorithm documentation is complete and authoritative. The Java
source is being extracted from the consortium's production indexer
(`CoAllianceIndexUtil` in MarcImporter) into this repo. Until the extraction is
finished, treat the documentation as the source of truth; consult MarcImporter
for the live reference implementation.

## Relationship to the production indexer

The production matchKey generator lives in the CoAlliance MarcImporter project
(not public) and is the canonical implementation. This repository is a
periodically-synced reference copy. When the algorithm changes — which happens
rarely and is signalled by a version-string bump — both the production indexer
and this repository are updated together.

## Porting to other languages

The Java sources here are deliberately structured one element per file so that
a port to Python / Ruby / Go / Rust can be done class-for-class. Test fixtures
under `src/test/resources/fixtures/` consist of MARC records paired with their
expected matchKey output; a port is "done" when it passes the same fixtures.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

## About CoAlliance

The [Colorado Alliance of Research Libraries](https://coalliance.org) is a
consortium of academic research libraries operating the Gold Rush family of
shared-collection-analysis tools.
