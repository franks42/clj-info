;; Babashka-compatible clj-info proposal
(ns clj-info-bb
  "Babashka-compatible version of clj-info with core functionality"
  (:require [clojure.string :as str]))

;; Simplified doc map extraction (no Java interop)
(defn bb-get-docs-map [name-or-sym]
  "Extract documentation map without Java interop dependencies"
  (let [sym (if (string? name-or-sym) (symbol name-or-sym) name-or-sym)]
    (when-let [resolved (try (resolve sym) (catch Exception _ nil))]
      (when (var? resolved)
        (let [m (meta resolved)]
          (assoc m
                 :fqname (str (:ns m) "/" (:name m))
                 :object-type-str (cond 
                                   (:macro m) "Macro"
                                   (:arglists m) "Function"
                                   :else "Var")))))))

;; Simple text formatter (no ANSI colors)
(defn bb-doc->text [name-or-sym]
  "Simple text documentation format for Babashka"
  (let [doc-map (bb-get-docs-map name-or-sym)]
    (if doc-map
      (str (:fqname doc-map) " - " (:object-type-str doc-map) "\n"
           (when (:arglists doc-map) 
             (str "Args: " (pr-str (:arglists doc-map)) "\n"))
           (when (:doc doc-map)
             (str "  " (:doc doc-map))))
      (str "No documentation found for: " name-or-sym))))

;; Simple EDN output (using built-in pr-str)
(defn bb-doc->edn [name-or-sym]
  "EDN format using built-in serialization"
  (pr-str (bb-get-docs-map name-or-sym)))

;; Main bb-compatible function
(defn bb-tdoc [name-or-sym]
  "Babashka-compatible documentation function"
  (println (bb-doc->text name-or-sym)))

;; Example usage:
;; (bb-tdoc 'map)
;; (bb-tdoc "reduce")
;; (println (bb-doc->edn 'filter))