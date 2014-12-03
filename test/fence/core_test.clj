(ns fence.core-test
  (:require [fence.core :refer [interop-form-for-dot expand-interop-form]]
            [clojure.test :refer :all]))

(deftest interop-form-for-dot-tests
  (= (interop-form-for-dot '.-abc) '-abc)
  (= (interop-form-for-dot '.xyz) 'xyz))

(deftest expand-interop-form-test
  (are [x y] (= (expand-interop-form x) y)
       '(. foo bar) '(fence.core/dot foo bar)

       '(.. foo bar) '(fence.core/.. foo bar)
       '(clojure.core/.. foo bar)  '(fence.core/.. foo bar)

       '(.-bar foo)  '(fence.core/dot foo -bar)
       '(.bar foo baz boo)  '(fence.core/dot foo bar baz boo)
       :not-a-list :not-a-list))
