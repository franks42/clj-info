;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (https://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.doc2map
  "Set of functions to enhance the runtime available documentation for
  clojure. Protocol with interfaces to collect doc-info from
  clojure-entities. Functions to render doc-info in either text or
  html. More user-friendly \"clojure.repl/doc\"-like macros/functions
  to present doc-info in various representations"
  (:require [clj-info.doc-info-EN]
            [clojure.repl]
            [clojure.set]
            [clojure.string]
            [clj-info.platform :as platform]))


(defprotocol docsmap
  "A protocol for gathering of doc info from objects of different types."
  (docs-map [o] "Returns a map with doc info for the object o."))


(extend-type clojure.lang.PersistentArrayMap
  ; see if we're dealing with a protocol object
  docsmap
  (docs-map [o]
    (if (:on-interface o)
      (let [extender-info (when-not platform/bb?
                            ;; extenders function may not be available in Babashka
                            (try 
                              (when-let [extenders-fn (resolve 'extenders)]
                                (extenders-fn o))
                              (catch Exception _ nil)))]
        {:protocol-def true
         :object-type-str "Protocol"
         :sigs (:sigs o),
         :extenders extender-info})
      {:object-type-str "Var => clojure.lang.PersistentArrayMap"
       :var-def true})))


(extend-type clojure.lang.Atom
  docsmap
  (docs-map [atomtype]
    {:object-type-str (str atomtype " => " (type @atomtype))}))


(extend-type java.lang.Class
  docsmap
  (docs-map [jtype]
    (let [class-name (.getName jtype)
          javadoc-url (when-not platform/bb?
                        ;; Only try to use clojure.java.javadoc on JVM
                        (try
                          (require 'clojure.java.javadoc)
                          ((resolve 'clojure.java.javadoc/javadoc-url) class-name)
                          (catch Exception _ nil)))]
      {:name class-name
       :java-class true
       :object-type-str "java.lang.Class"
       :url javadoc-url})))


(extend-type clojure.lang.Namespace
  docsmap
  (docs-map [nspace]
    (assoc (meta nspace)
            :name (ns-name nspace)
            :object-type-str "Namespace"
            :namespace true)))


(defn all-other-fqv [v]
  (let [s (platform/var->symbol v)]
    (clojure.set/difference
      (clojure.set/select #(not (nil? %))
                          (set (for [n (all-ns)] (ns-resolve n s))))
      #{v})))


(extend-type clojure.lang.Var
  docsmap
  (docs-map [v]
    (let [m0 (meta v)
          m1 (assoc m0
                    :fqname (str (when-let [ns (:ns m0)]
                                    (str (ns-name ns) "/"))
                                 (:name m0))
                    :all-other-fqv (all-other-fqv v))
          m  (cond
                (:special-form m1)  (assoc m1 :object-type-str
                                                "Special Form")
                (:macro m1)         (assoc m1 :object-type-str
                                                "Macro")
                (not (:arglists m1)) m1
                :else               (assoc m1 :function true
                                              :object-type-str "Function"))]
      (cond
        (:protocol m) (assoc m :protocol-member-fn true
                               :object-type-str "Protocol Interface/Function")
        (and (not platform/bb?) 
             (extends? docsmap (type @v))) (merge m (docs-map @v))
        true (if (get m :object-type-str)
               m
               (assoc m :var-def true
                        :object-type-str (str "Var => " (or (type @v) "nil"))))))))


(extend-type clojure.lang.Symbol
  docsmap
  (docs-map [s]
    (let [;; Try to get special doc info on JVM only
          special-doc-info (when (not platform/bb?)
                            (try 
                              (when-let [special-doc-map-var (resolve 'clojure.repl/special-doc-map)]
                                (@special-doc-map-var s))
                              (catch Exception _ nil)))]
      (cond
        ;; Found special doc info
        special-doc-info
        (assoc special-doc-info
               :name (name s)
               :object-type-str "Special Form"
               :special-form true)
        
        ;; Check if it's a special symbol (basic hardcoded list for Babashka compatibility)
        (#{'. 'def 'do 'if 'let 'var 'quote 'try 'catch 'throw 'finally 'loop 'recur 'fn 'set!} s)
        {:name (name s)
         :object-type-str "Special Symbol" 
         :special-form true}
         
        ;; Is it a namespace?
        (find-ns s) 
        (docs-map (find-ns s))
        
        ;; Try to resolve as var/class
        (and (try (resolve s) (catch Exception _ nil))
             (extends? docsmap (type (resolve s))))
        (docs-map (resolve s))
        
        ;; Default case
        :else nil))))


(extend-type clojure.lang.MultiFn
  docsmap
  (docs-map [astring]
    {:object-type-str "Multimethod"}))

;; SCI compatibility - extend protocol for sci.lang.Var
(when platform/bb?
  (try
    (let [sci-var-class (Class/forName "sci.lang.Var")]
      (extend sci-var-class
        docsmap
        {:docs-map (fn [v]
                     ;; Same implementation as clojure.lang.Var but for SCI vars
                     (let [v-meta (meta v)
                           v-name (platform/var->symbol v)
                           arglist-fn (fn [arglists]
                                        (when arglists
                                          (clojure.string/join " " 
                                                               (map str arglists))))]
                       (merge v-meta
                              {:name v-name
                               :fqname (str v-name)
                               :var true
                               :arglists-str (arglist-fn (:arglists v-meta))
                               :object-type-str "Var"})))}))
    (catch Exception _ nil)))

;; Default implementation for any Object - provides fallback for function objects
(extend-type Object
  docsmap
  (docs-map [obj]
    {:name (str obj)
     :object-type-str (if (fn? obj) "Function" "Object") 
     :no-docs "No documentation available - try passing the symbol instead of the function"}))

;;

(defn merge-newdocs
  "Merge-in new/alternative docs from clj-info.doc-info-EN/doc-info-map."
  [m n]
  (if (empty? m)
    (if-let [n-m (get clj-info.doc-info-EN/doc-info-map (str n))]
      (merge {:fqname n :object-type-str "General Info"} n-m)
      m)
    (if-let [n-m (get clj-info.doc-info-EN/doc-info-map (str (or (:fqname m)(:name m))))]
      (merge m n-m)
      m)))


(defn get-docs-map
  "Entry function to obtain map with doc-info for object n."
  [n]
  (let [s (if (string? n) (symbol n) n)]
    (merge-newdocs (docs-map s) s)))


