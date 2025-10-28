;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (https://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info
  "Set of functions and macros to present the online/real-time documentation
  in different formats."
  (:require [clojure.repl]
            [clojure.java.shell])
  (:require [clj-info.doc2txt :refer [doc2txt]]
            [clj-info.doc2map :refer [get-docs-map]]
            ; [clj-info.doc2html :refer [doc2html]] ; Temporarily disabled while fixing Hiccup 2 syntax
            [clj-info.doc2rich :refer [doc->rich]]
            [clj-info.doc2md :refer [doc->md]]
            [clj-info.doc2data :refer [doc->edn doc->json doc->edn-str doc->json-str doc->data]]
            [clj-info.platform :as platform]
            ; [clojure.java.browse :refer [browse-url]] ; Commented out for Babashka compatibility
            ))

(def clj-info-version "0.5.1")

;;;;;;;;;;;;;;;

;; text-doc - generate (enhanced) text-based docs

(defn tdoc*
  "Function that writes text-formatted documentation for a var,
  namespace, or special form given its name. (generates more info
  than clojure.core/doc)
  Name n is string or (quoted) symbol.
  Output is written to stdout, or file f."
  ([] (tdoc* "tdoc*"))
  ([w] (let [m (doc2txt w)]
          (println "----------------------------------------------------------------------")
          (print (:title m) (:message m))(symbol "")))
  ([w f & f-opts] (if f
                    (let [m (doc2txt w)]
                      (apply spit f (str (:title m) (:message m)) f-opts))
                    (tdoc* w (str (System/getProperty "user.home")
                                   "/.cljsh_output_dir/cljsh_output.txt")))))


(defmacro tdoc
  "Prints documentation for a var, namespace, or special form given
  its name. (generates more info than clojure.core/doc)
  Name n is string, symbol, or quoted symbol."
  ([] (tdoc* "tdoc"))
  ([n]
  (cond (string? n) `(tdoc* ~n)
        (symbol? n) `(tdoc* ~(str n))
        (= (type n) clojure.lang.Cons) `(tdoc* ~(str (second n))))))


(defn clj-doc*
  "Function that writes text-formatted documentation for a var,
  namespace, or special form given its name. (generates more info
  than clojure.core/doc)
  Name n is string or (quoted) symbol.
  Output is written to stdout, or file f."
  ([] (clj-doc* "clj-doc*"))
  ([w] (let [m (doc2txt w)]
          (println "----------------------------------------------------------------------")
          (print (:title m) (:message m))(symbol "")))
  ([w f & f-opts] (if f
                    (let [m (doc2txt w)]
                      (apply spit f (str (:title m) (:message m)) f-opts))
                    (clj-doc* w (str (System/getProperty "user.home")
                                   "/.cljsh_output_dir/cljsh_output.txt")))))


(defmacro clj-doc
  "Prints documentation for a var, namespace, or special form given
  its name. (generates more info than clojure.core/doc)
  Name n is string, symbol, or quoted symbol."
  ([] (clj-doc* "clj-doc"))
  ([n]
  (cond (string? n) `(clj-doc* ~n)
        (symbol? n) `(clj-doc* ~(str n))
        (= (type n) clojure.lang.Cons) `(clj-doc* ~(str (second n))))))


;; browser-doc - generates html-formated docs

(defn bdoc*
  "Function that writes html-formatted doc-info for identifier w (string)
  to (default) file f or stdout if f explicit nil."
  ([w]
    (let [d (str (System/getProperty "user.home") "/.cljsh_output_dir")]
      (:exit (clojure.java.shell/sh "bash" "-c"
        (str "if [ ! -d " d " ]; then mkdir "d ";fi"))))
    (let [f (str (System/getProperty "user.home")
                  "/.cljsh_output_dir/cljsh_output.html")]
      (bdoc* w f)
      ; TODO: Add back browse-url functionality for JVM
      (println "HTML file generated at:" f)
      (symbol "")))
  ([_w _f & _f-opts]
   (println "HTML documentation temporarily disabled while fixing Hiccup 2 syntax")
   (symbol "")))


(defmacro bdoc
  "Macro that writes html-formatted docs-map info for identifier w.
  Identifier can be given as string, symbol, or quoted symbol."
  [w]
  (cond (string? w) `(bdoc* ~w)
        (symbol? w) `(bdoc* ~(str w))
        (= (type w) clojure.lang.Cons) `(bdoc* ~(str (second w)))))


;; easy-doc (clojure.core/doc replacement)

(defn edoc*
  "Prints documentation for a var or special form given its name.
  Function \"edoc*\" is functional equivalent of \"clojure.repl/doc\" macro,
  except that the name-identifier can be a (quoted-)symbol or string.
  (makes it easier to use at the repl, especially for novice users)"
  [name-str-or-sym]
  (let [name-sym (symbol name-str-or-sym)]
    (if-let [special-name ('{& fn catch try finally try} name-sym)]
    ;; Handle special forms with platform compatibility
      (when-let [special-doc-info (platform/clojure-repl-special-doc special-name)]
        (platform/clojure-repl-print-doc special-doc-info))
      (cond
        (platform/clojure-repl-special-doc-map name-sym)
        (when-let [special-doc-info (platform/clojure-repl-special-doc name-sym)]
          (platform/clojure-repl-print-doc special-doc-info))
        
        (find-ns name-sym)
        (when-let [ns-doc-info (platform/clojure-repl-namespace-doc (find-ns name-sym))]
          (platform/clojure-repl-print-doc ns-doc-info))
        
        (resolve name-sym)
        (platform/clojure-repl-print-doc (meta (resolve name-sym)))))))

(defmacro edoc
  "Prints documentation for a var or special form given its name.
  Macro \"edoc\" is the equivalent of the \"clojure.repl/doc\" macro,
  except name-identifier can be a symbol, quoted-symbol or string.
  (makes it easier to use at the repl, especially for novice users)"
  [w]
  (cond (string? w) `(edoc* ~w)
        (symbol? w) `(edoc* ~(str w))
        (= (type w) clojure.lang.Cons) `(edoc* ~(str (second w)))))


;; generic single entry point for help and documentation

(def ^:dynamic *info-fn-map* {:text tdoc* :html bdoc*})
(def ^:dynamic *info-output-choice* :text)
(defn add-info-fn-map
  "Convenience function to add additional handlers
  that present doc-info in different formats across namespaces."
  [k f]
  (def ^:dynamic *info-fn-map* (assoc *info-fn-map* k f)))


(defn info*
  ""
  [n]
  (if (keyword? n)
    (if (n (set (keys *info-fn-map*)))
      (def ^:dynamic *info-output-choice* n)
      (info* (str n)))
    ((*info-output-choice* *info-fn-map*) n)))


(defmacro info
  "info or help returns documentation and usage information about the name n."
  [n]
  (cond (string? n) `(info* ~n)
        (keyword? n) `(info* ~n)
        (symbol? n) `(info* ~(str n))
        (= (type n) clojure.lang.Cons) `(info* ~(str (second n)))))

(defmacro help [n] `(clj-info/info ~n))

;;
