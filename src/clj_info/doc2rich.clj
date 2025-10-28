(ns clj-info.doc2rich
  "Rich terminal formatting for documentation info using ANSI colors and tables."
  (:require [clj-info.doc2map :refer [get-docs-map]]
            [clj-info.platform :as platform]
            [clojure.string :as str]))

;; Use platform/colorize for ANSI-safe coloring; it will gracefully
;; no-op when running under Babashka or when io.aviso.ansi is not present.
;; colorize usage: (platform/colorize :header "text")

(defn- make-box
  "Create a bordered box around content."
  [content & {:keys [width padding] :or {width 80 padding 1}}]
  (let [pad-str (apply str (repeat padding " "))
  border-line (platform/colorize :border (apply str (repeat width "─")))
  top-border (platform/colorize :border (str "┌" border-line "┐"))
  bottom-border (platform/colorize :border (str "└" border-line "┘"))
        format-line (fn [line]
                     (let [content-width (- width (* 2 padding) 2)
                           truncated (if (> (count line) content-width)
                                     (str (subs line 0 (- content-width 3)) "...")
                                     line)
                           padded (str truncated 
                                     (apply str (repeat (- content-width (count truncated)) " ")))]
                       (str (platform/colorize :border "│") pad-str padded pad-str (platform/colorize :border "│"))))]
    (str top-border "\n"
         (->> (str/split-lines content)
              (map format-line)
              (str/join "\n"))
         "\n" bottom-border)))

(defn- format-table
  "Format key-value pairs as a table."
  [pairs & {:keys [max-key-width] :or {max-key-width 15}}]
  (when (seq pairs)
    (let [key-width (min max-key-width 
                        (apply max (map #(count (str (first %))) pairs)))]
      (->> pairs
           (map (fn [[k v]]
                  (let [key-str (str k)
                        key-padded (str key-str 
                                      (apply str (repeat (- key-width (count key-str)) " ")))
                        formatted-key (platform/colorize :keyword key-padded)
                        separator (platform/colorize :muted " : ")
                        formatted-val (if (string? v)
                                      (platform/colorize :value v)
                                      (platform/colorize :code (pr-str v)))]
                    (str "  " formatted-key separator formatted-val))))
           (str/join "\n")))))

(defn- format-arglists
  "Format arglists with proper syntax highlighting."
  [arglists]
    (when arglists
    (->> arglists
      (map #(platform/colorize :code (str "(" (str/join " " (map name %)) ")")))
      (str/join "\n  "))))

(defn- format-section
  "Format a section with header and content."
  [title content]
    (when (and content (not (str/blank? (str content))))
    (str "\n" (platform/colorize :subheader (str "▸ " title)) "\n"
         (if (string? content)
           (str "  " content)
           content))))

(defn- format-doc-string
  "Format docstring with proper line wrapping and indentation."
  [doc-str]
  (when doc-str
    (->> (str/split-lines doc-str)
         (map #(str "  " %))
         (str/join "\n"))))

(defn- get-icon
  "Get appropriate icon for different types of documentation targets."
  [doc-map]
  (cond
    (:function doc-map) "🔧"
    (:macro doc-map) "🔮"
    (:special-form doc-map) "⚡"
    (:namespace doc-map) "📦"
    (:java-class doc-map) "🏛️"
    (:protocol doc-map) "🔌"
    (:protocol-member-fn doc-map) "🔗"
    :else "📖"))

(defn doc->rich
  "Convert documentation info to rich terminal format with colors and formatting."
  [x]
  (let [base-doc-map (get-docs-map x)
        ;; Add ClojureDocs URL similar to other formatters
        doc-map (if-let [n (:fqname base-doc-map)]
                  (if (re-find #"^clojure" (str n))
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/" n))
                    base-doc-map)
                  (if (:special-form base-doc-map)
                    (assoc base-doc-map :clojuredocs-ref (str "https://clojuredocs.org/clojure.core/" (:name base-doc-map)))
                    base-doc-map))
        {:keys [name fqname ns arglists doc url file line column object-type-str]} doc-map
        
        ;; Header with function/object name and type
        icon (get-icon doc-map)
        display-name (or fqname name (str x))
        type-info (when object-type-str (str " [" object-type-str "]"))
        header-content (str (platform/colorize :header (str icon " " display-name))
                           (when type-info 
                             (platform/colorize :muted type-info)))
        
        ;; Metadata table
        meta-pairs (cond-> []
                    ns (conj ["Namespace" (str ns)])
                    file (conj ["File" file])
                    line (conj ["Line" line])
                    column (conj ["Column" column])
                    url (conj ["Javadoc" url])
                    (:clojuredocs-ref doc-map) (conj ["ClojureDocs" (:clojuredocs-ref doc-map)]))
        
        ;; Build the rich output
        sections [(make-box header-content :width 70)
                 
                 (when arglists
                   (format-section "Arguments" 
                                 (str "\n  " (format-arglists arglists))))
                 
                 (when doc
                   (format-section "Documentation"
                                 (format-doc-string doc)))
                 
                 (when (seq meta-pairs)
                   (format-section "Source Information"
                                 (format-table meta-pairs)))]]
    
    (if (empty? doc-map)
      (platform/colorize :muted (str "No documentation found for: " x))
      (->> sections
           (remove nil?)
           (str/join "\n")))))