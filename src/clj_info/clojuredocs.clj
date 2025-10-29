;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (https://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.clojuredocs
  "ClojureDocs content integration with hybrid file+memory caching.
  
  Provides access to ClojureDocs examples, see-alsos, and comments with:
  - File-based cache (~/.clj-info/cache/) for persistence across sessions
  - In-memory cache for fast access within current session
  - Configurable TTL (default 1 hour)
  - Graceful fallback on network errors"
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clj-info.platform :as platform]))

;; Configuration
(def ^:dynamic *config*
  "Configuration map for ClojureDocs integration.
  
  Keys:
  - :cache-ttl-ms - Time-to-live for cache in milliseconds (default 1 hour)
  - :cache-dir - Directory for file cache (default ~/.clj-info/cache)
  - :export-url - URL to ClojureDocs export EDN file"
  {:cache-ttl-ms (* 60 60 1000) ; 1 hour
   :cache-dir (str (System/getProperty "user.home") "/.clj-info/cache")
   :export-url "https://github.com/clojure-emacs/clojuredocs-export-edn/raw/refs/heads/master/exports/export.compact.min.edn"})

;; In-memory cache (fast for current session)
(defonce ^:private memory-cache 
  (atom {:data nil :timestamp 0}))

;; File paths
(defn- cache-file-path []
  (str (:cache-dir *config*) "/clojuredocs-export.edn"))

(defn- timestamp-file-path []
  (str (:cache-dir *config*) "/clojuredocs-timestamp.txt"))

;; File cache operations
(defn- ensure-cache-dir!
  "Ensure cache directory exists."
  []
  (let [dir (io/file (:cache-dir *config*))]
    (when-not (.exists dir)
      (.mkdirs dir))))

(defn- read-timestamp-file
  "Read timestamp from file, returns 0 if file doesn't exist."
  []
  (try
    (if-let [f (io/file (timestamp-file-path))]
      (if (.exists f)
        (Long/parseLong (slurp f))
        0)
      0)
    (catch Exception _
      0)))

(defn- write-timestamp-file!
  "Write timestamp to file."
  [timestamp]
  (try
    (ensure-cache-dir!)
    (spit (timestamp-file-path) (str timestamp))
    (catch Exception e
      (println "Warning: Could not write timestamp file:" (.getMessage e)))))

(defn- read-cache-file
  "Read cached ClojureDocs data from file, returns nil if not available."
  []
  (try
    (when-let [f (io/file (cache-file-path))]
      (when (.exists f)
        (with-open [r (io/reader f)]
          (edn/read (java.io.PushbackReader. r)))))
    (catch Exception e
      (println "Warning: Could not read cache file:" (.getMessage e))
      nil)))

(defn- write-cache-file!
  "Write ClojureDocs data to cache file."
  [data]
  (try
    (ensure-cache-dir!)
    (with-open [w (io/writer (cache-file-path))]
      (binding [*print-length* nil
                *print-level* nil]
        (.write w (pr-str data))))
    (catch Exception e
      (println "Warning: Could not write cache file:" (.getMessage e)))))

;; HTTP client abstraction
(defn- http-get
  "Platform-agnostic HTTP GET request."
  [url]
  (try
    (if platform/bb?
      ;; Babashka: use built-in http-client
      ((requiring-resolve 'babashka.http-client/get) url {:throw false})
      ;; JVM: use babashka.http-client as library
      ((requiring-resolve 'babashka.http-client/get) url {:throw false}))
    (catch Exception e
      (println "Warning: HTTP request failed:" (.getMessage e))
      {:status 0 :body nil})))

;; Network operations
(defn- fetch-clojuredocs-export
  "Fetch ClojureDocs export from GitHub. Returns data map or nil on failure."
  []
  (println "Fetching ClojureDocs export from" (:export-url *config*))
  (try
    (let [response (http-get (:export-url *config*))]
      (if (= 200 (:status response))
        (do
          (println "Successfully fetched ClojureDocs export (" 
                   (count (:body response)) "bytes)")
          (edn/read-string (:body response)))
        (do
          (println "Warning: Failed to fetch ClojureDocs export, status:" 
                   (:status response))
          nil)))
    (catch Exception e
      (println "Warning: Could not parse ClojureDocs export:" (.getMessage e))
      nil)))

;; Cache management
(defn- cache-expired?
  "Check if cache with given timestamp has expired."
  [timestamp]
  (let [now (System/currentTimeMillis)
        age (- now timestamp)
        ttl (:cache-ttl-ms *config*)]
    (> age ttl)))

(defn- get-from-memory-cache
  "Get data from in-memory cache if fresh, otherwise nil."
  []
  (let [{:keys [data timestamp]} @memory-cache]
    (when (and data (not (cache-expired? timestamp)))
      data)))

(defn- update-memory-cache!
  "Update in-memory cache with new data."
  [data]
  (reset! memory-cache {:data data :timestamp (System/currentTimeMillis)}))

(defn- get-from-file-cache
  "Get data from file cache if fresh, otherwise nil."
  []
  (let [timestamp (read-timestamp-file)]
    (when-not (cache-expired? timestamp)
      (when-let [data (read-cache-file)]
        ;; Also update memory cache while we're at it
        (update-memory-cache! data)
        data))))

(defn- update-file-cache!
  "Update file cache with new data."
  [data]
  (let [timestamp (System/currentTimeMillis)]
    (write-cache-file! data)
    (write-timestamp-file! timestamp)))

(defn- get-cached-data
  "Get ClojureDocs data with hybrid caching strategy.
  
  Cache lookup order:
  1. In-memory cache (fastest)
  2. File cache (fast, survives restarts)
  3. Network fetch (slow, requires internet)
  4. Stale file cache as fallback (if network fails)
  
  Returns data map or nil if unavailable."
  []
  (or
   ;; 1. Try in-memory cache first
   (get-from-memory-cache)
   
   ;; 2. Try file cache
   (get-from-file-cache)
   
   ;; 3. Fetch from network and update both caches
   (when-let [fresh-data (fetch-clojuredocs-export)]
     (update-memory-cache! fresh-data)
     (update-file-cache! fresh-data)
     fresh-data)
   
   ;; 4. Fallback to stale file cache if network failed
   (do
     (println "Warning: Using stale cache as fallback")
     (when-let [stale-data (read-cache-file)]
       (update-memory-cache! stale-data)
       stale-data))))

;; Public API
(defn get-clojuredocs-content
  "Get ClojureDocs content for a fully-qualified name.
  
  Args:
    fqn - Fully qualified name as string (e.g., 'clojure.core/map')
    content-type - :examples, :see-alsos, or :notes
  
  Returns:
    Vector of content for the requested type, or empty vector if none found.
    
  Examples:
    (get-clojuredocs-content \"clojure.core/map\" :examples)
    (get-clojuredocs-content \"clojure.core/reduce\" :see-alsos)
    (get-clojuredocs-content \"clojure.core/filter\" :notes)"
  [fqn content-type]
  (when-let [data (get-cached-data)]
    (when-let [entry (get data (keyword fqn))]
      (vec (get entry content-type [])))))

(defn format-examples
  "Format ClojureDocs examples as a readable string.
  
  Args:
    examples - Vector of example maps with :body key
    
  Returns:
    Formatted string with numbered examples, or nil if no examples."
  [examples]
  (when (seq examples)
    (str/join "\n\n" 
              (map-indexed 
               (fn [i example]
                 (str "Example " (inc i) ":\n" 
                      (if (string? example)
                        example
                        (:body example example))))
               examples))))

(defn format-see-alsos
  "Format see-alsos as a readable string.
  
  Args:
    see-alsos - Vector of related function names
    
  Returns:
    Comma-separated string of related functions, or nil if none."
  [see-alsos]
  (when (seq see-alsos)
    (str "See also: " 
         (str/join ", " 
                   (map (fn [sa]
                          (if (map? sa)
                            (str (:ns sa) "/" (:name sa))
                            (str sa)))
                        see-alsos)))))

(defn format-notes
  "Format notes/comments as a readable string.
  
  Args:
    notes - Vector of note maps with :body key
    
  Returns:
    Formatted string with numbered notes, or nil if no notes."
  [notes]
  (when (seq notes)
    (str/join "\n\n" 
              (map-indexed
               (fn [i note]
                 (str "Note " (inc i) ":\n" 
                      (if (string? note)
                        note
                        (:body note note))))
               notes))))

(defn enrich-doc-map
  "Enrich a documentation map with ClojureDocs content.
  
  Args:
    doc-map - Documentation map from doc2map/get-docs-map
    
  Returns:
    Enhanced doc-map with :clojuredocs-examples, :clojuredocs-see-alsos,
    and :clojuredocs-notes keys added if content is available."
  [doc-map]
  (if-let [fqn (:fqname doc-map)]
    (let [examples (get-clojuredocs-content fqn :examples)
          see-alsos (get-clojuredocs-content fqn :see-alsos)
          notes (get-clojuredocs-content fqn :notes)]
      (cond-> doc-map
        (seq examples) (assoc :clojuredocs-examples examples)
        (seq see-alsos) (assoc :clojuredocs-see-alsos see-alsos)
        (seq notes) (assoc :clojuredocs-notes notes)))
    doc-map))

(defn clear-cache!
  "Clear both in-memory and file caches. Useful for forcing a refresh."
  []
  (reset! memory-cache {:data nil :timestamp 0})
  (try
    (when-let [f (io/file (cache-file-path))]
      (when (.exists f)
        (.delete f)))
    (when-let [f (io/file (timestamp-file-path))]
      (when (.exists f)
        (.delete f)))
    (println "Cache cleared successfully")
    (catch Exception e
      (println "Warning: Could not clear cache files:" (.getMessage e)))))

(defn cache-info
  "Get information about the current cache state.
  
  Returns:
    Map with :memory-cached?, :file-cached?, :file-timestamp, :file-age-ms,
    :cache-expired?, and :cache-file-exists? keys."
  []
  (let [mem-ts (:timestamp @memory-cache)
        file-ts (read-timestamp-file)
        now (System/currentTimeMillis)
        cache-file (io/file (cache-file-path))]
    {:memory-cached? (and (some? (:data @memory-cache)) 
                          (pos? mem-ts))
     :memory-age-ms (when (pos? mem-ts) (- now mem-ts))
     :file-cached? (pos? file-ts)
     :file-timestamp file-ts
     :file-age-ms (when (pos? file-ts) (- now file-ts))
     :cache-expired? (cache-expired? file-ts)
     :cache-file-exists? (.exists cache-file)
     :cache-file-size (when (.exists cache-file) (.length cache-file))
     :cache-ttl-ms (:cache-ttl-ms *config*)
     :cache-dir (:cache-dir *config*)}))
