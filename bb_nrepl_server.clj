#!/usr/bin/env bb

;; Babashka nREPL server with clj-info pre-loaded
;; Usage: bb bb_nrepl_server.clj [port]
;; Default port: 7888

(require '[babashka.nrepl.server :as nrepl-server]
         '[clojure.string :as str])

;; Pre-load clj-info and related namespaces
(println "📚 Loading clj-info library...")
(require '[clj-info :as info]
         '[clj-info.doc2rich :as rich]
         '[clj-info.doc2md :as md] 
         '[clj-info.doc2data :as data]
         '[clj-info.doc2map :as doc-map]
         '[clj-info.platform :as platform])

(println "✅ clj-info loaded successfully!")
(println "📋 Available namespaces:")
(println "   [info :as info]     - Main API: (info map), (info* 'map)")
(println "   [rich :as rich]     - Rich formatting: (rich/doc->rich ...)")
(println "   [md :as md]         - Markdown: (md/doc->md ...)")  
(println "   [data :as data]     - JSON/EDN: (data/doc->json ...), (data/doc->edn ...)")
(println "   [doc-map :as doc-map] - Core docs: (doc-map/get-docs-map 'symbol)")
(println "   [platform :as platform] - Platform detection")

(def default-port 7888)
(def port (if (seq *command-line-args*)
            (Integer/parseInt (first *command-line-args*))
            default-port))

(println (str "🚀 Starting nREPL server on port " port "..."))
(println (str "💡 Connect with: lein repl :connect " port))
(println (str "💡 Or in VS Code: Jack-in to localhost:" port))
(println "")
(println "🧪 Quick test commands to try:")
(println "   (info map)")
(println "   (info* 'reduce)")  
(println "   (rich/doc->rich (doc-map/get-docs-map 'filter))")
(println "   (data/doc->json (doc-map/get-docs-map 'map))")
(println "   platform/bb? ; Should return true (it's a var, not a function!)")
(println "")
(println "🎯 Press Ctrl+C to stop the server")
(println "=" (str/join (repeat 50 "=")))

;; Start the nREPL server
(def server (nrepl-server/start-server! {:host "0.0.0.0" 
                                         :port port}))

;; Keep the server running
(deref (promise))