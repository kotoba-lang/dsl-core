# ADR 0001: Validation problems are sovereign Kotoba

`src/kotoba/dsl/problem.kotoba` is the sole production language source. JVM
Clojure is a compiler/test host only and is not a production runtime.

The former open EDN API becomes a typed boundary: domains, field names,
severities, and codes are keywords; subjects and problem collections are
bounded canonical documents; messages are bounded strings. The public
`severities` value becomes a zero-argument typed-set getter. The common
five-argument `problem` constructor is retained. The former six-argument
subject-key overload becomes `problem-field`, with `[keyword, subject]` carried
as one canonical bounded document so the shared five-parameter native fuel ABI
is not weakened. Filtering is explicit bounded recursion over at most 32
problems.

Unsupported severities, missing domain severity fields, malformed canonical
documents, value-budget overflow, and recursion/fuel exhaustion fail closed.
The program declares no effects. Conformance covers observable values, typed
ABI, effect declarations, resource bounds, and rejection behavior across
reference execution, restricted JavaScript, and instantiated typed Wasm.
