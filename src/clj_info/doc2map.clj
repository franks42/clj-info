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
	(:require  [clj-info.doc-info-EN]
	           [clojure.string :as s]
	           [clojure.repl]
	           [clojure.set]
	           [clj-info.platform :as platform]
	           [clj-info.clojuredocs :as cljdocs]))

;; Conditionally require clojure.java.javadoc only on JVM (not available in BB)
(when-not platform/bb?
  (require '[clojure.java.javadoc]))


(defprotocol docsmap
  "A protocol for gathering of doc info from objects of different types."
  (docs-map [o] "Returns a map with doc info for the object o."))


(extend-type clojure.lang.PersistentArrayMap
  ; see if we're dealing with a protocol object
  docsmap
  (docs-map [o]
    (if (:on-interface o)
      {:protocol-def true
       :object-type-str "Protocol"
       :sigs (:sigs o),
       :extenders (when-let [ext-fn (resolve 'extenders)]
                    (ext-fn o))}
      {:object-type-str "Var => clojure.lang.PersistentArrayMap"
       :var-def true})))


(extend-type clojure.lang.Atom
  docsmap
  (docs-map [atomtype]
    {:object-type-str (str atomtype " => " (type @atomtype))}))


(extend-type java.lang.Class
  docsmap
  (docs-map [jtype]
    {:name (.getName jtype)
     :java-class true
     :object-type-str "java.lang.Class"
     :url (when-not platform/bb?
            (when-let [javadoc-url (resolve 'clojure.java.javadoc/javadoc-url)]
              (@javadoc-url (.getName jtype))))}))


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
        (and (not platform/bb?) (extends? docsmap (type @v))) (merge m (docs-map @v))
        :else (if (get m :object-type-str)
               m
               (assoc m :var-def true
                        :object-type-str (str "Var => " (or (type @v) "nil"))))))))


(extend-type clojure.lang.Symbol
  docsmap
  (docs-map [s]
    (cond
      (platform/clojure-repl-special-doc-map s)
        (assoc (platform/clojure-repl-special-doc-map s)
                :name (name s)
                :object-type-str "Special Form"
                :special-form true)
      (special-symbol? s)
        {:name (name s)
         :object-type-str "Special Symbol"
         :special-form true}
     (find-ns s) (docs-map (find-ns s))
     (try (resolve s) (catch Exception _)) (when (extends? docsmap (type (resolve s)))
                    (docs-map (resolve s))))))


(extend-type clojure.lang.MultiFn
  docsmap
  (docs-map [astring]
    {:object-type-str "Multimethod"}))

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
  "Entry function to obtain map with doc-info for object n.
  
  Options (optional map as second argument):
    :include-clojuredocs - If true, enriches doc-map with ClojureDocs examples,
                          see-alsos, and notes (default: false)
  
  Examples:
    (get-docs-map 'map)
    (get-docs-map 'map {:include-clojuredocs true})"
  ([n]
   (get-docs-map n {}))
  ([n opts]
   (let [s (if (string? n) (symbol n) n)
         base-map (merge-newdocs (docs-map s) s)]
     (if (:include-clojuredocs opts)
       (cljdocs/enrich-doc-map base-map)
       base-map))))


