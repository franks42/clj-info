; Minimal test of the problematic bracket structure

(defn test-brackets []
  (let [m {:name "test"}
        w "test-word"
        page (if m 
               [:div "has m"]
               [:h2 (str "Sorry, no doc-info for \"" w "\")])]
    [:html [:body page]]))