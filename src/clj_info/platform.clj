;; Platform detection and graceful feature-fallbacks for clj-info
(ns clj-info.platform)

(def bb?
  "True when running under Babashka (native)."
  (boolean (System/getProperty "babashka.version")))

(defn- try-require
  "Try to require a namespace and return true on success, false on failure.
   Designed for optional runtime feature detection." [sym]
  (try (require sym) true (catch Throwable _ false)))

(def ansi-available (try-require 'io.aviso.ansi))
(def hiccup-available (try-require 'hiccup2.core))
(def clj-data-json-available (try-require 'clojure.data.json))
;; Babashka has Cheshire built-in (aliased as 'json), not babashka.json
(def bb-cheshire-available (and bb? (try-require 'cheshire.core)))

(defn colorize
  "Apply a named color/style to text if io.aviso.ansi is available.
   Otherwise return the text unchanged (graceful degradation)."
  [k text]
  (if ansi-available
    (let [sym (case k
                :header 'bold-cyan-font
                :subheader 'bold-blue-font
                :keyword 'bold-magenta-font
                :value 'green-font
                :code 'yellow-font
                :meta 'blue-font
                :border 'cyan-font
                :highlight 'bold-white-font
                :muted 'black-font
                :reset 'reset-font
                'reset)
          v (ns-resolve 'io.aviso.ansi sym)
          resetv (ns-resolve 'io.aviso.ansi 'reset-font)]
      (if v
        (str @v text (if resetv @resetv ""))
        text))
    text))

(defn json-encode-str
  "Encode data to JSON string using babashka.json when available in bb,
   otherwise clojure.data.json if present. Falls back to `pr-str` as last resort.
   Accepts :pretty? option (best-effort)."
  [data & {:keys [pretty?] :or {pretty? true}}]
  (cond
    bb-cheshire-available
    ;; Babashka has Cheshire built-in - use cheshire.core/generate-string
    ((requiring-resolve 'cheshire.core/generate-string) data {:pretty pretty?})

    clj-data-json-available
    ;; clojure.data.json/write-str
    ((requiring-resolve 'clojure.data.json/write-str) data :indent pretty?)

    :else
    (pr-str data)))

(defn edn-str-pretty
  "Return a pretty EDN string. Uses clojure.pprint if available; otherwise pr-str.
   Uses requiring-resolve to avoid compile-time namespace resolution warnings."
  [data]
  (if (try-require 'clojure.pprint)
    (let [p (requiring-resolve 'clojure.pprint/pprint)]
      (if p
        (with-out-str (p data))
        (pr-str data)))
    (pr-str data)))

(defn hiccup-available?
  [] hiccup-available)

(defn ansi-available?
  [] ansi-available)

(defn safe-private-var
  "Safely resolve a private var, returning nil if not available (e.g., in Babashka)."
  [var-symbol]
  (when-not bb?
    (try
      (var-get (resolve var-symbol))
      (catch Exception _ nil))))

(defn clojure-repl-print-doc
  "Platform-compatible version of clojure.repl/print-doc."
  [doc-map]
  (if bb?
    ;; Basic doc printing for Babashka
    (when (:doc doc-map)
      (println "-------------------------")
      (println (or (:name doc-map) (:fqname doc-map)))
      (when (:arglists doc-map)
        (println (:arglists doc-map)))
      (println (:doc doc-map)))
    ;; Use the real print-doc on JVM
    (when-let [print-doc-fn (safe-private-var 'clojure.repl/print-doc)]
      (print-doc-fn doc-map))))

(defn clojure-repl-special-doc
  "Platform-compatible version of clojure.repl/special-doc."
  [special-name]
  (when-not bb?
    (when-let [special-doc-fn (safe-private-var 'clojure.repl/special-doc)]
      (special-doc-fn special-name))))

(defn clojure-repl-special-doc-map
  "Platform-compatible version of clojure.repl/special-doc-map."
  [name-sym]
  (when-not bb?
    (when-let [special-doc-map (safe-private-var 'clojure.repl/special-doc-map)]
      (special-doc-map name-sym))))

(defn clojure-repl-namespace-doc
  "Platform-compatible version of clojure.repl/namespace-doc."
  [namespace]
  (when-not bb?
    (when-let [namespace-doc-fn (safe-private-var 'clojure.repl/namespace-doc)]
      (namespace-doc-fn namespace))))

(defn var->symbol
  "Get the symbol from a var in a platform-compatible way.
   In JVM Clojure, uses .sym field access. In Babashka, uses metadata."
  [v]
  (if bb?
    ;; Babashka: get symbol from metadata
    (symbol (str (:ns (meta v))) (str (:name (meta v))))
    ;; JVM: use direct field access
    (.sym v)))

(defn platform-info
  "Return a map with platform detection results for debugging."
  []
  {:babashka? bb?
   :ansi-available? ansi-available
   :hiccup-available? hiccup-available
   :clj-data-json-available? clj-data-json-available
   :bb-cheshire-available? bb-cheshire-available})