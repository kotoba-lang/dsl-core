# kotoba DSL core

[![CI](https://github.com/kotoba-lang/dsl-core/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/dsl-core/actions/workflows/ci.yml)

Shared, dependency-free `.cljc` helpers for small portable kotoba DSL libraries.

Current surface:

- `kotoba.dsl.problem` — namespaced validation problem maps and
  `errors` / `warnings` / `valid?` helpers.

This repo is intentionally small. Domain libraries such as `statechart`,
`states`, `sigma`, `policy`, and `torch` keep their own model and execution
semantics while sharing stable validation result conventions.

```sh
clojure -M:test
```
