(ns clj-info.doc2md
  "Markdown formatting for documentation info, suitable for files and modern tooling."
  (:require [clj-info.doc2map :refer [get-docs-map]]
            [clojure.string :as str]))

(defn- escape-markdown
  "Escape special Markdown characters in text."
  [text]
  (when text
    (-> text
        (str/replace #"\*" "\\*")
        (str/replace #"_" "\\_")
        (str/replace #"`" "\\`")
        (str/replace #"#" "\\#")
        (str/replace #"\[" "\\[")
        (str/replace #"\]" "\\]")
        (str/replace #"\(" "\\(")
        (str/replace #"\)" "\\)")
        (str/replace #"\|" "\\|"))))

(defn- format-code-block
  "Format code in a Markdown code block with optional language."
  [code & {:keys [lang] :or {lang "clojure"}}]
  (str "```" lang "\n" code "\n```"))

(defn- format-inline-code
  "Format text as inline Markdown code."
  [text]
  (str "`" text "`"))

(defn- format-arglists-md
  "Format arglists as Markdown code blocks."
  [arglists]
  (when arglists
    (let [formatted-args (->> arglists
                             (map #(str "(" (str/join " " (map name %)) ")"))
                             (str/join "\n"))]
      (format-code-block formatted-args))))

(defn- format-metadata-table
  "Format metadata as a Markdown table."
  [pairs]
  (when (seq pairs)
    (let [header "| Property | Value |\n|----------|-------|"
          rows (->> pairs
                   (map (fn [[k v]]
                          (str "| " (escape-markdown (str k)) 
                               " | " (format-inline-code (str v)) " |")))
                   (str/join "\n"))]
      (str header "\n" rows))))

(defn- get-type-badge
  "Get a badge/emoji for different types of documentation targets."
  [doc-map]
  (cond
    (:function doc-map) "🔧 Function"
    (:macro doc-map) "🔮 Macro"
    (:special-form doc-map) "⚡ Special Form"
    (:namespace doc-map) "📦 Namespace"
    (:java-class doc-map) "🏛️ Java Class"
    (:protocol doc-map) "🔌 Protocol"
    (:protocol-member-fn doc-map) "🔗 Protocol Method"
    (:var-def doc-map) "📋 Var"
    :else "📖 Documentation"))

(defn- format-doc-header
  "Format the main header for documentation."
  [doc-map]
  (let [{:keys [name fqname object-type-str]} doc-map
        display-name (or fqname name "Unknown")
        type-badge (get-type-badge doc-map)
        type-info (when object-type-str (str " _(" object-type-str ")_"))]
    (str "# " (escape-markdown display-name) "\n\n"
         "**" type-badge "**" (or type-info "") "\n")))

(defn- format-description
  "Format the description/docstring section."
  [doc]
  (when doc
    (str "\n## Description\n\n"
         (escape-markdown doc) "\n")))

(defn- format-usage
  "Format the usage/arguments section."
  [arglists]
  (when arglists
    (str "\n## Usage\n\n"
         (format-arglists-md arglists) "\n")))

(defn- format-source-info
  "Format source and metadata information."
  [doc-map]
  (let [{:keys [ns file line column url]} doc-map
        pairs (cond-> []
                ns (conj ["Namespace" (str ns)])
                file (conj ["File" file])
                line (conj ["Line" line])
                column (conj ["Column" column])
                url (conj ["Documentation URL" url]))]
    (when (seq pairs)
      (str "\n## Source Information\n\n"
           (format-metadata-table pairs) "\n"))))

(defn- format-examples
  "Format examples section (placeholder for future enhancement)."
  [doc-map]
  (when-let [examples (:examples doc-map)]
    (str "\n## Examples\n\n"
         (format-code-block examples) "\n")))

(defn- format-see-also
  "Format see-also and references section with ClojureDocs URLs."
  [doc-map]
  (let [see-also (:see-also doc-map)
        clojuredocs-ref (:clojuredocs-ref doc-map)
        javadoc-url (:url doc-map)
        has-refs (or see-also clojuredocs-ref javadoc-url)]
    (when has-refs
      (str "\n## References\n\n"
           ;; ClojureDocs reference
           (when clojuredocs-ref
             (str "- [ClojureDocs](" clojuredocs-ref ")\n"))
           ;; Javadoc URL  
           (when javadoc-url
             (str "- [Javadoc](" javadoc-url ")\n"))
           ;; See also items
           (when see-also
             (->> see-also
                  (map #(str "- " (format-inline-code (str %))))
                  (str/join "\n")
                  (str "\n### See Also\n\n")))
           "\n"))))

(defn doc->md
  "Convert documentation info to Markdown format.
  
  This function generates comprehensive Markdown documentation suitable for:
  - README files and documentation sites
  - GitHub/GitLab documentation
  - Static site generators (Jekyll, Hugo, etc.)
  - Modern documentation tools
  
  The output includes:
  - Proper Markdown headers and formatting
  - Code blocks with syntax highlighting
  - Tables for metadata
  - Escaped special characters
  - Type badges and visual indicators"
  [x]
  (let [base-doc-map (get-docs-map x)
        ;; Add ClojureDocs URL similar to doc2txt
        doc-map (if-let [n (:fqname base-doc-map)]
                  (if (re-find #"^clojure" (str n))
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/" n))
                    base-doc-map)
                  (if (:special-form base-doc-map)
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/clojure.core/" (:name base-doc-map)))
                    base-doc-map))]
    (if (empty? doc-map)
      (str "# Documentation Not Found\n\n"
           "_No documentation available for:_ " (format-inline-code (str x)) "\n")
      (let [{:keys [doc arglists]} doc-map
            sections [(format-doc-header doc-map)
                     (format-description doc)
                     (format-usage arglists)
                     (format-source-info doc-map)
                     (format-examples doc-map)
                     (format-see-also doc-map)]]
        (->> sections
             (remove nil?)
             (str/join "\n")
             str/trim)))))