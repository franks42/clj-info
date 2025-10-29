;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (https://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.doc2html
  (:require [clojure.string :as s]
            [hiccup2.core :as h]
            [clojure.repl]
            [clj-info.doc2map :refer [get-docs-map]]
            [clj-info.clojuredocs :as cljdocs]))

;; html-docs generation from info returned from docs-map

(def ^:dynamic *clj-source-home* (str (System/getProperty "user.home") "/Development/Clojure/clojure/src/clj/clojure"))

(def ^:dynamic *clj-info-ccs*
  (str (h/html
   [:style {:type "text/css"}
    " body {
          font-family: arial, helvetica, sans-serif;
          font-size: 0.8em;
        }
        h1 { font-size: 2em; }
        h2 { font-size: 1.5em; }
        a { text-decoration: none; }
        .cljdoc-entry { border-style: dashed; padding: 1em; }
        .cljdoc-heading { color: black; display: inline; }
        .cljdoc-heading-name { color: black; display: inline; }
        .cljdoc-heading-type { color: blue; display: inline; text-align: right; font-size: 0.75em; font-style: italic; }
        .cljdoc-sub-heading { color: black; }
        .cljdoc-type-macro { display: inline; color: red; }
        .cljdoc-type-sf { color: orange; display: inline; }
        .cljdoc-type-fn { color: green; display: inline; }
        .cljdoc-doc-body { color: blue; font-family: Inconsolata, \"Andale Mono\", Courier; }
        .cljdoc-source-body { color: blue; font-family: Inconsolata, \"Andale Mono\", Courier; }
      "])))

(def ^:dynamic *clj-body-js*
  (str (h/html
   [:script {:type "text/javascript"}
    "window.onload = function() { scrollTo(0,0);}"])))

(defn ul [coll] 
  [:ul (for [x (map str coll)] [:li x])])

(defn- format-clojuredocs-example
  "Format a single ClojureDocs example for HTML display."
  [idx example]
  [:div.cljdoc-example
   [:h5 (str "Example " (inc idx) ":")]
   [:pre.cljdoc-doc-body (if (string? example)
                           example
                           (:body example example))]])

(defn- format-clojuredocs-note
  "Format a single ClojureDocs note for HTML display."
  [idx note]
  [:div.cljdoc-note
   [:h5 (str "Note " (inc idx) ":")]
   [:pre.cljdoc-doc-body (if (string? note)
                           note
                           (:body note note))]])

(defn doc2html
  "Generates html-page for the docs-map info obtained for word w.
  
  Options (optional map as second argument):
    :include-clojuredocs - If true, includes ClojureDocs examples, see-alsos,
                          and notes (default: false)"
  ([w]
   (doc2html w {}))
  ([w opts]
   (let [m0  (get-docs-map w opts)
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

        page (if m 
               [:div
                [:h1.cljdoc-heading
                 (if (:clojuredocs-ref m)
                   [:div.cljdoc-heading-name [:a {:href (:clojuredocs-ref m)} (str (or (:fqname m) (:name m)))]]
                   [:div.cljdoc-heading-name (str (or (:fqname m) (:name m)))])
                 " - "
                 [:div.cljdoc-heading-type (when (:private m) "Private ")
                  (cond (:macro m) [:div.cljdoc-type-macro (:object-type-str m)]
                        (:special-form m) [:div.cljdoc-type-sf (:object-type-str m)]
                        (:function m) [:div.cljdoc-type-fn (:object-type-str m)]
                        :else (:object-type-str m))]]

                (when (:protocol m) 
                  [:div.cljdoc-sub-heading [:h4 (str "Protocol: " (s/replace-first (str (:protocol m)) "#'" ""))]])

                (when (:forms m) 
                  [:div.cljdoc-sub-heading 
                   [:h4 "Forms"]
                   (ul (:forms m))])

                (when (:arglists m) 
                  [:div.cljdoc-sub-heading 
                   [:h4 "Arity"]
                   (ul (:arglists m))])

                (when (:doc m) 
                  [:div.cljdoc-sub-heading 
                   [:h4 "Documentation"]
                   [:div.cljdoc-doc-body [:pre (str "  " (:doc m))]]])

                (when-let [examples (:clojuredocs-examples m)]
                  [:div.cljdoc-sub-heading
                   [:h4 "ClojureDocs Examples"]
                   (map-indexed format-clojuredocs-example examples)])

                (when-let [see-alsos (:clojuredocs-see-alsos m)]
                  [:div.cljdoc-sub-heading
                   [:h4 "See Also"]
                   [:ul (for [sa see-alsos]
                          [:li (if (map? sa)
                                 (str (:ns sa) "/" (:name sa))
                                 (str sa))])]])

                (when-let [notes (:clojuredocs-notes m)]
                  [:div.cljdoc-sub-heading
                   [:h4 "ClojureDocs Notes"]
                   (map-indexed format-clojuredocs-note notes)])

                (when (:sigs m)
                  [:div
                   [:h4 "Interface Signatures:"]
                   [:ul (for [x (map (fn [y] [:a {:href (str "cljdoc://?" (name y))} (name y)]) (map first (:sigs m)))] [:li x])]])

                (when (:extenders m) 
                  [:div
                   [:h4 "Extenders:"]
                   [:ul (for [x (map (fn [e] [:a {:href (str "cljdoc://?" e)} e])
                                     (map (fn [ee] (if (= (type ee) java.lang.Class) (.getName ee) (str ee))) (:extenders m)))] 
                          [:li x])]])

                (when-let [fqvs (:all-other-fqv m)]
                  (when (pos? (count fqvs)) 
                    [:div
                     [:h4 "Alternative Vars:"]
                     (ul (map str fqvs))]))

                (when (or (:url m) (:clojuredocs-ref m)) 
                  [:div
                   [:h4 "Refs: "]
                   (when (:url m) [:div [:a {:href (:url m)} (:url m)]])
                   (when (:clojuredocs-ref m) [:div [:a {:href (:clojuredocs-ref m)} (:clojuredocs-ref m)]])])

                (when (:file m) 
                  [:div
                   [:h4 "Source File: "]
                   [:a {:href (str "txmt://open/?url=file:"
                                   (s/replace-first (:file m) #"^clojure" *clj-source-home*)
                                   (if (:line m) (str "&line=" (:line m)) ""))}
                    (:file m)]])

                (when-let [source-str (and (not (:namespace m))
                                           (clojure.repl/source-fn (symbol w)))]
                  [:div
                   [:h4.cljdoc-source-heading "Source: "]
                   [:div.cljdoc-source-body [:pre source-str]]])]

               [:h2 (str "Sorry, no doc-info for \"" w "\"")])]

    (str (h/html [:html [:head *clj-info-ccs*] [:body *clj-body-js* [:div {:class "cljdoc-entry"} page]]])))))

(defn doc2simple-html
  "Generates simple html-page for the docs-map info obtained for word w.
  
  Options (optional map as second argument):
    :include-clojuredocs - If true, includes ClojureDocs examples, see-alsos,
                          and notes (default: false)"
  ([w]
   (doc2simple-html w {}))
  ([w opts]
   (let [m0  (get-docs-map w opts)
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

        page (if m 
               [:div
                [:h2
                 (if (:clojuredocs-ref m)
                   [:a {:href (:clojuredocs-ref m)} (str (or (:fqname m) (:name m)))]
                   (str (or (:fqname m) (:name m))))
                 " - "
                 (when (:private m) "Private ")
                 (cond (:macro m) (:object-type-str m)
                       (:special-form m) (:object-type-str m)
                       (:function m) (:object-type-str m)
                       :else (:object-type-str m))]

                (when (:protocol m) 
                  [:h3 (str "Protocol: " (s/replace-first (str (:protocol m)) "#'" ""))])

                (when (:forms m) 
                  [:div
                   [:h3 "Forms"]
                   (ul (:forms m))])

                (when (:arglists m) 
                  [:div
                   [:h3 "Arity"]
                   (ul (:arglists m))])

                (when (:doc m) 
                  [:div
                   [:h3 "Documentation"]
                   [:pre (str "  " (:doc m))]])

                (when-let [examples (:clojuredocs-examples m)]
                  [:div
                   [:h3 "ClojureDocs Examples"]
                   (map-indexed format-clojuredocs-example examples)])

                (when-let [see-alsos (:clojuredocs-see-alsos m)]
                  [:div
                   [:h3 "See Also"]
                   [:ul (for [sa see-alsos]
                          [:li (if (map? sa)
                                 (str (:ns sa) "/" (:name sa))
                                 (str sa))])]])

                (when-let [notes (:clojuredocs-notes m)]
                  [:div
                   [:h3 "ClojureDocs Notes"]
                   (map-indexed format-clojuredocs-note notes)])

                (when (:sigs m) 
                  [:div
                   [:h3 "Interface Signatures:"]
                   [:ul (for [x (map (fn [y] [:a {:href (str "cljdoc://?" (name y))} (name y)]) (map first (:sigs m)))] [:li x])]])

                (when (:extenders m) 
                  [:div
                   [:h3 "Extenders:"]
                   [:ul (for [x (map (fn [e] [:a {:href (str "cljdoc://?" e)} e])
                                     (map (fn [ee] (if (= (type ee) java.lang.Class) (.getName ee) (str ee))) (:extenders m)))] 
                          [:li x])]])

                (when-let [fqvs (:all-other-fqv m)]
                  (when (pos? (count fqvs)) 
                    [:div
                     [:h3 "Alternative Vars:"]
                     (ul (map str fqvs))]))]

               [:h2 (str "Sorry, no doc-info for \"" w "\"")])]

    (println w m)
    (str (h/html [:html page])))))
