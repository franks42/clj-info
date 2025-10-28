(ns clj-info.doc2data
  "JSON and EDN formatting for documentation info, suitable for programmatic consumption."
  (:require [clj-info.doc2map :refer [get-docs-map]]
            [clj-info.platform :as platform]))

(defn- serialize-value
  "Serialize a value for JSON/EDN output, handling special types."
  [v]
  (cond
    (instance? clojure.lang.Var v) (str v)
    (instance? clojure.lang.Namespace v) (str (ns-name v))
    (instance? java.lang.Class v) (.getName v)
    (fn? v) (str v)
    (set? v) (vec v)  ; Convert sets to vectors for JSON compatibility
    (seq? v) (vec v)  ; Convert seqs to vectors
    :else v))

(defn- clean-doc-map
  "Clean the doc map for serialization, handling non-serializable values."
  [doc-map]
  (into {}
        (for [[k v] doc-map]
          [k (serialize-value v)])))

(defn- add-metadata
  "Add metadata about the output format and generation time."
  [doc-map format-type]
  (assoc doc-map
         :clj-info/format format-type
         :clj-info/generated-at (str (java.time.Instant/now))
         :clj-info/version (or (System/getProperty "clj-info.version") "0.5.0")))

(defn doc->edn
  "Convert documentation info to EDN format.
  
  EDN (Extensible Data Notation) is ideal for:
  - Clojure tooling and editor integration
  - Configuration files
  - Data exchange between Clojure systems
  - Preserving exact Clojure data types
  
  The output preserves:
  - All original data types where possible
  - Keywords, symbols, and collections
  - Metadata and structure information
  - Full fidelity to the original doc-map"
  [x]
  (let [base-doc-map (get-docs-map x)
        ;; Add ClojureDocs URL similar to other formatters
        doc-map (if-let [n (:fqname base-doc-map)]
                  (if (re-find #"^clojure" (str n))
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/" n))
                    base-doc-map)
                  (if (:special-form base-doc-map)
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/clojure.core/" (:name base-doc-map)))
                    base-doc-map))]
    (if (empty? doc-map)
      {:clj-info/error "No documentation found"
       :clj-info/target (str x)
       :clj-info/format :edn
       :clj-info/generated-at (str (java.time.Instant/now))}
      (-> doc-map
          clean-doc-map
          (add-metadata :edn)))))

(defn doc->json
  "Convert documentation info to JSON format.
  
  JSON format is ideal for:
  - REST APIs and web services
  - Integration with non-Clojure tools
  - Language-agnostic data exchange
  - Modern web applications and frontends
  
  The output provides:
  - Standard JSON-compatible data types
  - Nested structure for complex information
  - String representations for Clojure-specific types
  - Metadata about the documentation source"
  [x]
  (let [base-doc-map (get-docs-map x)
        ;; Add ClojureDocs URL similar to other formatters
        doc-map (if-let [n (:fqname base-doc-map)]
                  (if (re-find #"^clojure" (str n))
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/" n))
                    base-doc-map)
                  (if (:special-form base-doc-map)
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/clojure.core/" (:name base-doc-map)))
                    base-doc-map))]
    (if (empty? base-doc-map)
      {:clj-info/error "No documentation found"
       :clj-info/target (str x)
       :clj-info/format "json"
       :clj-info/generated-at (str (java.time.Instant/now))}
      (-> doc-map
          clean-doc-map
          (add-metadata "json")))))

(defn doc->edn-str
  "Convert documentation to EDN string format."
  [x & {:keys [pretty?] :or {pretty? true}}]
  (let [edn-data (doc->edn x)]
    (if pretty?
      (platform/edn-str-pretty edn-data)
      (pr-str edn-data))))

(defn doc->json-str
  "Convert documentation to JSON string format."
  [x & {:keys [pretty?] :or {pretty? true}}]
  (let [json-data (doc->json x)]
    (platform/json-encode-str json-data :pretty? pretty?)))

(defn doc->data
  "Generic function to convert documentation to data format.
  
  Supports multiple output formats:
  - :edn or :edn-str for EDN format
  - :json or :json-str for JSON format  
  - :map or :raw for raw Clojure map
  
  Options:
  - :pretty? - Enable pretty printing (default true)
  - :format - Output format (:edn, :json, :map)
  
  Examples:
    (doc->data 'map :format :json)
    (doc->data 'filter :format :edn :pretty? false)
    (doc->data 'reduce :format :map)"
  [x & {:keys [format pretty?] :or {format :edn pretty? true}}]
  (case format
    (:edn :edn-str) (doc->edn-str x :pretty? pretty?)
    (:json :json-str) (doc->json-str x :pretty? pretty?)
    (:map :raw) (doc->edn x)  ; Return raw map for :map format
    (throw (ex-info (str "Unsupported format: " format)
                    {:format format
                     :supported-formats [:edn :json :map :edn-str :json-str :raw]}))))