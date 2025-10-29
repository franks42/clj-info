(defproject clj-info "0.6.0"
  :description "Enhanced clojure doc-info facility for use in REPL and beyond."
  :url "https://github.com/franks42/clj-info"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [hiccup "2.0.0"]
                 [io.aviso/pretty "1.4.4"]
                 [org.clojure/data.json "2.5.1"]
                 [org.babashka/http-client "0.4.22"]]
  :repositories [["clojars" {:url "https://repo.clojars.org/"
                             :creds :gpg}]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org/"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases false}]]
;;  	:dev-dependencies [[lein-marginalia "0.9.2"]
;;  	                   [codox "0.10.8"]]
  )
