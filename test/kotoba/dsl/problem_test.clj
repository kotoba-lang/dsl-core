(ns kotoba.dsl.problem-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [kotoba.compiler.core :as compiler]
            [kotoba.compiler.ir :as ir]))

(def source (slurp "src/kotoba/dsl/problem.kotoba"))
(defn call [kir function & args] (ir/execute kir function (vec args)))
(defn dstr [value] ["string" value])
(defn dkw [value] ["keyword" value])
(defn dmap [entries]
  ["map" (->> entries (sort-by (comp str key)) (mapv (fn [[key value]] [key value])))])
(defn dvec [& values] ["vector" (vec values)])

(deftest reference-preserves-validation-contract
  (let [kir (:kir (compiler/compile-source source :js-kotoba-v1))
        error (call kir 'problem :sc :error :chart/bad-initial
                    (dstr "door") "bad initial")
        warning (call kir 'problem-field :torch :warn :module/empty
                      (dvec (dkw :path) (dstr "")) "empty")
        problems (dvec error warning)]
    (is (= [[:set :keyword] [:error :warn]] (call kir 'severities)))
    (is (= :sc/severity (call kir 'domain-key :sc :severity)))
    (is (= (dmap {:sc/code (dkw :chart/bad-initial)
                  :sc/id (dstr "door")
                  :sc/msg (dstr "bad initial")
                  :sc/severity (dkw :error)}) error))
    (is (= :error (call kir 'severity :sc error)))
    (is (true? (call kir 'error? :sc error)))
    (is (true? (call kir 'warning? :torch warning)))
    (is (= (dvec error) (call kir 'errors :sc problems)))
    (is (= (dvec warning) (call kir 'warnings :torch problems)))
    (is (false? (call kir 'valid? :sc problems)))
    (is (true? (call kir 'valid? :other problems)))
    (testing "invalid severity and malformed problem documents fail closed"
      (is (thrown? clojure.lang.ExceptionInfo
                   (call kir 'problem :sc :info :note (dstr "x") "msg")))
      (is (thrown? clojure.lang.ExceptionInfo
                   (call kir 'severity :sc (dmap {:sc/code (dkw :bad)})))))
    (is (= #{} (set (:effects kir))))))

(defn compiler-root []
  (nth (iterate #(.getParent ^java.nio.file.Path %)
                (java.nio.file.Path/of (.toURI (io/resource "kotoba/compiler/core.clj")))) 4))
(defn base64 [value] (.encodeToString (java.util.Base64/getEncoder) value))

(deftest restricted-javascript-and-typed-wasm-have-observable-conformance
  (let [javascript (compiler/compile-source source :js-kotoba-v1)
        wasm (compiler/compile-source source :wasm32-browser-kotoba-v1)
        js64 (base64 (.getBytes ^String (:source javascript) "UTF-8"))
        wasm64 (base64 ^bytes (:bytes wasm))
        probe (shell/sh
               "node" "--input-type=module" "-e"
               (str "import(process.argv[1]).then(async host=>{"
                    "const j=await import('data:text/javascript;base64," js64 "');"
                    "const w=await host.instantiateKotoba(Buffer.from(process.argv[2],'base64'));"
                    "const run=(x,doc)=>{const s=doc(['string','door']);"
                    "const p=x.problem(':sc',':error',':chart/bad-initial',s,'bad initial');"
                    "if(x.severity(':sc',p)!==':error'||x['error?'](':sc',p)!==true)throw Error('problem');"
                    "const ps=doc(['vector',[p]]);if(x['valid?'](':sc',ps)!==false)throw Error('valid');"
                    "if(x.errors(':sc',ps)[1].length!==1)throw Error('filter');"
                    "let rejected=false;try{x.problem(':sc',':info',':note',s,'bad')}catch(e){rejected=true}"
                    "if(!rejected)throw Error('reject');};"
                    "run(j.instantiateKotoba({}),x=>x);run(w.instance.exports,w.typedValues.document);"
                    "}).catch(e=>{console.error(e);process.exit(99)})")
               (.toString (.toUri (.resolve (compiler-root) "runtime/browser-host.mjs"))) wasm64)]
    (is (zero? (:exit probe)) (:err probe))))

(deftest production-source-authority
  (is (= ["src/kotoba/dsl/problem.kotoba"]
         (->> (file-seq (io/file "src")) (filter #(.isFile %)) (map str) sort vec))))
