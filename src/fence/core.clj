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

(defn interop-form-for-dot
  [attr|method]
  (->> attr|method name rest
       (apply str) symbol))

(defn expand-interop-form
  "Expands regular Clojurescript interop forms to Fence's equivalent."
  [form]
  (match form
         (['. & xs] :seq)
         `(fence.core/dot ~@xs)

         ([(:or '.. 'clojure.core/..) & xs] :seq)
         `(fence.core/.. ~@xs)

         ([(attr :guard attr?) obj] :seq)
         `(fence.core/dot ~obj ~(interop-form-for-dot attr))

         ([(method :guard method?) obj & xs] :seq)
         `(fence.core/dot ~obj ~(interop-form-for-dot method) ~@xs)

         :else form))

(defn expand-all-interop-forms
  "Expands regular Clojurescript interop forms to Fence's equivalent."
  [form]
  (prewalk expand-interop-form form))

(defmacro +++
  "The fence macro. Works the same as Clojure's `do` special form
  except that it will prevent symbols in interop forms inside from
  getting renamed by Closure compiler."
  [& body]
  (expand-all-interop-forms `(do ~@body)))
