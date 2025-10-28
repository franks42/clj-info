;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (https://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.utils
  "Set of functions and macros to present the online/real-time documentation
  in different formats."
  (:require [clojure.repl]
            [clojure.java.shell])
  (:use [clojure.java.browse]))

;;;;

(defn clj-namespace? 
  "Predicate to test whether n is a clj-namespace object or not."
  [n]
  (= (type n) clojure.lang.Namespace))

(defn clj-find-ns 
  "find-ns alternative, where n can be a str, symbol, or namespace."
  [n]
  (cond
    (symbol? n) (find-ns n)
    (string? n) (find-ns (symbol n))
    (clj-namespace? n) n
    :true nil))

(defn clj-the-ns 
  "the-ns alternative, where n can be a str, symbol, or namespace."
  [n]
  (cond
    (symbol? n) (find-ns n)
    (string? n) (the-ns (symbol n))
    (clj-namespace? n) n
    :true (the-ns n)))

(defn clj-ns-resolve 
  "ns-resolve alternative, where n can be a str, symbol, or namespace,
  and s can be a string or symbol.
  Returns s when it's already a var or class.
  Returns nil when s refers to a namespace instead of throwing a fit.
  n defaults to *ns*."
  ([s] (clj-ns-resolve *ns* s))
  ([n s]
    (if (or (var? s) (class? s))
      s
      (when-not (clj-find-ns s)
        (ns-resolve (clj-the-ns n) (symbol s)))))
  ([n e s]
    (if (or (var? s) (class? s))
      s
      (when-not (clj-find-ns s)
        (ns-resolve (clj-the-ns n) e (symbol s))))))

(defn clj-resolve-name 
  "Combination of clj-ns-resolve and clj-find-ns as it tries to 
  resolve s to either a namespace, var or class."
  ([s] (clj-resolve-name *ns* s))
  ([n s]
    (if-let [a-ns (clj-find-ns s)]
      a-ns
      (clj-ns-resolve n s))))




;;;;
;; fqname

(defprotocol IFQNameable
  "Protocol to dispatch on the fqname by type."
  (-fqname [o] [o n] 
  "Returns a string with the fully qualified name of an existing object 
  that is either o or o will resolve to it.
  The optional namespace n may be used for resolution - defaults to *ns*.
  Returns nil when no real object exists or o cannot resolve to real object."))

(extend-type java.lang.Object
  ;; default value nil for all types that are not IFQNameable
  IFQNameable
  (-fqname ([o] nil)
          ([o n] nil)))

(extend-type clojure.lang.Namespace
  IFQNameable
  (-fqname ([o] (str o))
          ([o _] (-fqname o))))

(extend-type clojure.lang.Var
  IFQNameable
  (-fqname ([o] (subs (str o) 2))
          ([o _] (-fqname o))))

(extend-type java.lang.Class
  IFQNameable
  (-fqname ([o] (.getName o))
          ([o _] (-fqname o))))

(extend-type clojure.lang.Symbol
  IFQNameable
  (-fqname ([o] (-fqname o *ns*))
           ([o n] 
             (if-let [a-ns (clj-find-ns o)] 
                (-fqname a-ns)
                (when-let [v (ns-resolve n o)] (-fqname v))))))

(extend-type clojure.lang.Keyword
  IFQNameable
  (-fqname ([o] (if-let [n (namespace o)] (str n "/" (name o)) (name o)))
          ([o n] (-fqname o))))

(extend-type java.lang.String
  IFQNameable
  (-fqname ([o] (-fqname (symbol o)))
          ([o n] (-fqname (symbol o) n))))

(defn fqname 
  "fqname returns the fully qualified string-name of existing object o,
  or returns the fqn of the existing object that o resolves to.
  Returns nil if no fqn is found or applicable."
  ([o] (-fqname o))
  ([n o] (-fqname o n)))

(defn fqname-sym
  "fqname returns the fully qualified symbol-name of existing object o,
  or returns the fqn of the object o resolves to.
  Returns nil if no fqn is found or applicable."
  ([o] (when-let [s (fqname o)] (symbol s)))
  ([n o] (when-let [s (fqname n o)] (symbol s))))

;;;;

