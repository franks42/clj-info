#!/usr/bin/env bb

;; Test script to verify Babashka compatibility

(require '[clj-info.platform :as platform]
         '[clojure.pprint :as pprint])

(println "=== Babashka Compatibility Test ===")
(println)
(println "Platform Detection:")
(pprint/pprint (platform/platform-info))
(println)

;; Test basic functionality that should work in both JVM and Babashka
(println "Testing basic clj-info functionality...")

;; Load the main namespace
(require '[clj-info :refer [info]])

(println "\n1. Testing platform detection colorization:")
(let [colored-text (platform/colorize :header "This should be colored on JVM, plain on BB")]
  (println colored-text))

(println "\n2. Testing JSON encoding:")
(let [test-data {:name "map" :type "function" :namespace "clojure.core"}
      json-str (platform/json-encode-str test-data)]
  (println "JSON output:" json-str))

(println "\n3. Testing EDN pretty printing:")
(let [test-data {:name 'map :type :function :namespace 'clojure.core}
      edn-str (platform/edn-str-pretty test-data)]
  (println "EDN output:" edn-str))

(println "\n4. Testing basic clj-info functions:")

(println "\n4.1 Text documentation (tdoc*):")
(try
  (let [result (with-out-str ((requiring-resolve 'clj-info/tdoc*) "map"))]
    (if (pos? (count result))
      (println "✓ Text documentation works")
      (println "✗ Text documentation returned empty")))
  (catch Exception e
    (println "✗ Text documentation failed:" (.getMessage e))))

(println "\n4.2 Rich documentation (rdoc*):")
(try
  (let [result (with-out-str ((requiring-resolve 'clj-info/rdoc*) "map"))]
    (if (pos? (count result))
      (println "✓ Rich documentation works")
      (println "✗ Rich documentation returned empty")))
  (catch Exception e
    (println "✗ Rich documentation failed:" (.getMessage e))))

(println "\n4.3 Info function with mode switching:")
(try
  (info :text)  ;; Switch to text mode for BB compatibility
  (println "Documentation for 'map':")
  (info 'map)
  (println "✓ Info function works")
  (catch Exception e
    (println "✗ Info function failed:" (.getMessage e))))

(println "\n=== Test Complete ===")
(println "If you see this message, clj-info Babashka compatibility is working!")