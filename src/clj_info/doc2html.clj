;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.doc2html
  (:require [clojure.string :as s]
	          [hiccup.core :as h]
	          [clojure.repl])
	(:use  [clj-info.doc2map :only [get-docs-map]]))

;; html-docs generation from info returned from docs-map

(def ^:dynamic *clj-source-home* (str (System/getProperty "user.home") "/Development/Clojure/clojure/src/clj/clojure"))

(def ^:dynamic *clj-info-ccs* 
  (h/html 
    [:style {:type"text/css"} 
      " body {
          font-family: arial, helvetica, sans-serif;
          font-size: 0.8em;
        }
        
        h1 {
          font-size: 2em;
        }
        
        h2 {
          font-size: 1.5em;
        }
        
        a {
          text-decoration: none;
        }
        .cljdoc-entry {
          border-style: dashed;
          padding: 1em;
        }
        .cljdoc-heading {
          color: black;
          display: inline;
        }
        .cljdoc-heading-name {
          color: black;
          display: inline;
        }
        .cljdoc-heading-type {
          color: blue;
          display: inline;
          text-align: right;
          font-size: 0.75em;
          font-style: italic;
        }
        .cljdoc-sub-heading {
          color: black;
        }
        .cljdoc-type-macro {
          display: inline;
          color: red;
        }
        .cljdoc-type-sf {
          color: orange;
          display: inline;
        }
        .cljdoc-type-fn {
          color: green;
          display: inline;
        }
        .cljdoc-doc-body {
          color: blue;
          font-family: Inconsolata, \"Andale Mono\", Courier;
        }
        .cljdoc-source-body {
          color: blue;
          font-family: Inconsolata, \"Andale Mono\", Courier;
        }
      "
    ]))

(def ^:dynamic *clj-body-js*
  (h/html 
    [:script {:type "text/javascript"} 
      (h/h "window.onload = function() { scrollTo(0,0);}" )
    ]))


(defn ul [coll] [:ul (for [x (map h/h (map str coll))] [:li x])])

(defn doc2html
  "generates html-page for the docs-map info obtained for word w"
  [w]
  (let [m0  (get-docs-map w)
        m1  (if (:special-form m0)
             (assoc m0 :url 
                         (if (contains? m0 :url)
                           (str "http://clojure.org/" (:url m0))
                           (str "http://clojure.org/special_forms#" (:name m0))))
             m0)
        m (if-let [n (:fqname m1)]
            (if (re-find #"^clojure" n)
              (assoc m1 :clojuredocs-ref (str "http://clojuredocs.org/clojure_core/" n))
              m1)
            (if (:special-form m1)
              (assoc m1 :clojuredocs-ref (str "http://clojuredocs.org/clojure_core/clojure.core/" (:name m1)))
              m1))
              
        page	(if m (h/html
        
            [:h1.cljdoc-heading
            (if (:clojuredocs-ref m)
              [:div.cljdoc-heading-name [:a {:href (:clojuredocs-ref m)} (str (or (:fqname m)(:name m)))]]
              [:div.cljdoc-heading-name (str (or (:fqname m)(:name m)))])
            " -       "
            [:div.cljdoc-heading-type (when (:private m) "Private ")
                  (cond (:macro m) [:div.cljdoc-type-macro (:object-type-str m) ]
                        (:special-form m) [:div.cljdoc-type-sf (:object-type-str m) ]
                        (:function m) [:div.cljdoc-type-fn (:object-type-str m) ]
                        :else (:object-type-str m))]]
                    
            (when (:protocol m) (h/html
              [:div.cljdoc-sub-heading [:h4 (str "Protocol: " (s/replace-first (str (:protocol m)) "#'" ""))]]))
             
            (when (:forms m) (h/html
              [:div.cljdoc-sub-heading [:h4 "Forms"]]
              [:ul (for [x (map h/h (map str (:forms m)))] [:li x])]))
             
            (when (:arglists m) (h/html
              [:div.cljdoc-sub-heading [:h4 "Arity"]]
              ;[:ul (for [x (map h/h (map str (:arglists m)))] [:li x])]))
              (ul (:arglists m))))

            (when (:doc m) (h/html
              [:div.cljdoc-sub-heading [:h4 "Documentation"]]
              [:div.cljdoc-doc-body [:pre (h/h (str "  " (:doc m)))]]))

            (when (:sigs m) (h/html
              [:h4 "Interface Signatures:"]
              ;[:ul (for [x (map (fn [y] h/h (name y)) (map first (:sigs m)))] [:li x])]))
              [:ul (for [x (map (fn [y] [:a {:href (str "cljdoc://?" (name y))} (name y)]) (map first (:sigs m)))] [:li x])]))
              
            (when (:extenders m) (h/html
              [:h4 "Extenders:"]
              ;[:ul (for [x (map h/h (map str (:extenders m)))] [:li x])]
              [:ul (for [x (map (fn [e] [:a {:href (str "cljdoc://?" e)} e]) 
                (map (fn [ee] (if (= (type ee) java.lang.Class) (.getName ee)(str ee))) (:extenders m)))] [:li x])]))
            
            (when-let [fqvs (:all-other-fqv m)]
              (when (pos? (count fqvs)) (h/html
                [:h4 "Alternative Vars:"]
                (ul (map str fqvs)))))
            
            (when (or (:url m)(:clojuredocs-ref m)) (h/html
              [:h4 "Refs: "]
              (when (:url m) [:a {:href (:url m)} (:url m)])
              [:p]
              (when (:clojuredocs-ref m) [:a {:href (:clojuredocs-ref m)} (:clojuredocs-ref m)])))

            (when (:file m) (h/html
              [:h4 "Source File: "
              [:a {:href (str "txmt://open/?url=file:" 
                (s/replace-first (:file m) #"^clojure" *clj-source-home* ) 
                (if (:line m) (str "&line=" (:line m)) ""))} 
                (:file m)]]))
                
            (when-let [source-str (and (not (:namespace m))
                                       (clojure.repl/source-fn (symbol w)))] 
              (h/html
              [:h4.cljdoc-source-heading "Source: "]
              [:div.cljdoc-source-body [:pre (h/h source-str)]])))
          
          (h/html [:h2 (str "Sorry, no doc-info for \"" w "\"")] ))

       ]
       ;;(h/html [:html [:head ] [:body [:script {:type "text/javascript"} "document.body.innerHTML = '';" ] page]])))
       ;;(h/html [:script {:type "text/javascript"} "document.body.innerHTML = '';" ] page)))
       ;;(h/html [:div {:class "cljdoc-entry"} page])))
       (h/html [:html [:head *clj-info-ccs* ] [:body *clj-body-js*  [:div {:class "cljdoc-entry"} page]]])))

