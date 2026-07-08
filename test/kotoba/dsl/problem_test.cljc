(ns kotoba.dsl.problem-test
  (:require [clojure.test :refer [deftest is]]
            [kotoba.dsl.problem :as problem]))

(deftest builds-domain-namespaced-problems
  (is (= {:sc/severity :error
          :sc/code :chart/bad-initial
          :sc/id "door"
          :sc/msg "bad initial"}
         (problem/problem :sc :error :chart/bad-initial "door" "bad initial")))
  (is (= {:torch/severity :warn
          :torch/code :module/empty
          :torch/path ""
          :torch/msg "empty"}
         (problem/problem :torch :path :warn :module/empty "" "empty"))))

(deftest filters-errors-and-warnings-by-domain
  (let [problems [(problem/problem :sc :error :bad "a" "bad")
                  (problem/problem :sc :warn :odd "b" "odd")]]
    (is (= 1 (count (problem/errors :sc problems))))
    (is (= 1 (count (problem/warnings :sc problems))))
    (is (false? (problem/valid? :sc problems)))
    (is (true? (problem/valid? :torch problems)))))

(deftest rejects-invalid-severity
  (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) #"Invalid problem severity"
                        (problem/problem :sc :info :note "x" "msg"))))
