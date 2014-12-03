(ns fence.core
  (:refer-clojure :exclude [..])
  (:require [clojure.walk :refer [prewalk]]
            [clojure.core.match :refer [match]]))

(defmacro dot
  "Alternative macro version of Clojurescript special form `.`. Will
  expand to `aget` forms which are resistant to *renaming symbols* feature
  of Google Closure compiler in `advanced optimizations` level."
  ([obj attr|method]
     (cond
      (list? attr|method)
      `(dot ~obj ~(first attr|method) ~@(rest attr|method))
      (symbol? attr|method)
      (let [the-name (name attr|method)]
        (if (.startsWith the-name "-")
          `(aget ~obj ~(apply str (rest (seq the-name))))
          `(let [obj# ~obj
                 method# (aget obj# ~the-name)]
             (.call method# obj#))))
      :else
      (throw (Exception. "Invalid dot form."))))
  ([obj method & args]
     (let [the-name (name method)]
       (if (.startsWith the-name "-")
         (throw (Exception. "Invalid dot form."))
         `(let [obj# ~obj
                method# (aget obj# ~the-name)]
            (.call method# obj# ~@args))))))

(defmacro ..
  "Effortless dot form macro. Use it instead of core library's `..`
  macro to avoid manual extern files."
  ([x form] `(dot ~x ~form))
  ([x form & more] `(.. (dot ~x ~form) ~@more)))

(defn attr? [sym]
  (and (symbol? sym)
       (.startsWith (name sym) ".-")))

(defn method? [sym]
  (and (symbol? sym)
       (.startsWith (name sym) ".")))
