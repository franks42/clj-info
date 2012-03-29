;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info
  "Set of functions and macros to present the online/real-time documentation
  in different formats."
  (:require [clojure.repl])
  (:use [clj-info.doc2txt :only [doc2txt]]
        [clj-info.doc2html :only [doc2html]]
        [clojure.java.browse]))


;;;;;;;;;;;;;;;

;; text-doc - generate (enhanced) text-based docs

(defn tdoc*
  "Function that writes text-formatted documentation for a var,
  namespace, or special form given its name. (generates more info
  than clojure.core/doc)
  Name n is string or (quoted) symbol.
  Output is written to stdout, or file f."
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
  [n]
  (cond (string? n) `(tdoc* ~n)
        (symbol? n) `(tdoc* ~(str n))
        (= (type n) clojure.lang.Cons) `(tdoc* ~(str (second n)))))


;; browser-doc - generates html-formated docs

(defn bdoc*
  "Function that writes html-formatted doc-info for identifier w (string)
  to (default) file f or stdout if f explicit nil."
  ([w]
    (let [f (str (System/getProperty "user.home")
                  "/.cljsh_output_dir/cljsh_output.html")]
      (bdoc* w f)
      (browse-url (str "file://" f))
      (symbol "")))
  ([w f & f-opts]
    (if f (apply spit f (doc2html w) f-opts) (println (doc2html w)))))


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
    ;; using private functions from clojure.repl... not good...
    (#'clojure.repl/print-doc (#'clojure.repl/special-doc special-name))
    (cond
      (#'clojure.repl/special-doc-map name-sym)
        (#'clojure.repl/print-doc (#'clojure.repl/special-doc name-sym))
      (find-ns name-sym)
        (#'clojure.repl/print-doc (#'clojure.repl/namespace-doc (find-ns name-sym)))
      (resolve name-sym)
        (#'clojure.repl/print-doc (meta (resolve name-sym)))))))

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
