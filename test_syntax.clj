(ns test-syntax
  (:require [clojure.string :as s]
            [clj-info.doc2map :refer [get-docs-map]]
            [clj-info.platform :as platform]))

(defn doc2html
  "generates html-page for the docs-map info obtained for word w"
  [w]
  (let [m0  (get-docs-map w)
        m1  (if (:special-form m0)
              (assoc m0 :url
                     (if (contains? m0 :url)
                       (str "https://clojure.org/" (:url m0))
                       (str "https://clojure.org/special_forms#" (:name m0))))
              m0)
        m (if-let [n (:fqname m1)]
            (if (re-find #"^clojure" n)
              (assoc m1 :clojuredocs-ref (str "https://clojuredocs.org/" n))
              m1)
            (if (:special-form m1)
              (assoc m1 :clojuredocs-ref (str "https://clojuredocs.org/clojure.core/" (:name m1)))
              m1))

        page	(if m [:div
                    [:h1 (str (or (:fqname m) (:name m)))]
                    (when (:doc m) [:pre (platform/html-escape (str "  " (:doc m)))])]

                 [:h2 (str "Sorry, no doc-info for \"" w "\")])]
    [:html [:head] [:body [:div page]]]))

(println "Syntax test passed")