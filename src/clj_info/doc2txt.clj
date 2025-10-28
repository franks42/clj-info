;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (https://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.doc2txt
  "Namespace dedicated to generation of text-formatted documentation for
  vars, namespaces and types."
  (:require [clojure.string :as s])
  (:use [clj-info.doc2map :only [get-docs-map]]))

;; text-docs generation

(defn doc2txt
  "Generates and returns string with text-page for the docs-map info obtained for identifier-string w."
  [w]
  (let [m0  (get-docs-map w)
        m1  (if (:special-form m0)
             (assoc m0 :url
                         (if (contains? m0 :url)
                           (str "https://clojure.org/" (:url m0))
                           (str "https://clojure.org/special_forms#" (:name m0))))
             m0)
        m (if-let [n (:fqname m1)]
            (if (re-find #"^clojure" (str n))
              (assoc m1 :clojuredocs-ref (str "https://clojuredocs.org/" n))
              m1)
            (if (:special-form m1)
              (assoc m1 :clojuredocs-ref (str "https://clojuredocs.org/clojure.core/" (:name m1)))
              m1))

        title (if m
                (str  (or (:fqname m)(:name m))
                      "   -   "
                      (when (:private m) "Private ")
                      (:object-type-str m))
                (str "Sorry, no doc-info for \"" w "\""))

        message (if m
          (str

            (when (:protocol m)
              (str  \newline "Protocol: "
                    (s/replace-first (str (:protocol m)) "#'" "")))

            (when (:forms m)
              (str \newline (doall (apply str (map pr-str (:forms m))))))

            (when (:arglists m)
              (str  \newline 
                (if (string? (:arglists m)) 
                  (:arglists m) 
                  (doall (apply str (map pr-str (:arglists m)))))))

            (when (:doc m)
              (str  ;\newline "Documentation:"
                    \newline "  " (:doc m)))

            (when-let [all-sigs (:sigs m)]
              (when (pos? (count all-sigs))
                (let [sigs-names (map (fn [s] (name (first s))) (:sigs m))]
                  (str \newline "Interface Signatures:"
                    (apply str (map (fn [x] (str \newline "  " x)) sigs-names))))))

            (when-let [all-extenders (:extenders m)]
              (when (pos? (count all-extenders))
                (str \newline "Extenders:"
                  (apply str (map (fn [x] (str \newline "  "
                                    (if (= (type x) java.lang.Class)
                                      (.getName x)
                                      (str x))))
                                  all-extenders)))))

            (when-let [fqvs (:all-other-fqv m)]
              (when (pos? (count fqvs))
                (str \newline "Alternative Vars:"
                  (apply str (map (fn [x] (str \newline "  " x)) fqvs)))))

            (when (or (:url m)(:clojuredocs-ref m))
              (str \newline "Refs: "
                (when (:url m) (str \newline "  " (:url m)))
                (when (:clojuredocs-ref m) (str \newline "  " (:clojuredocs-ref m))))))
          "")
          ]
    {:title title :message message}))


