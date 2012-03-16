;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clj-info.doc-info-EN
  "")
  
(def doc-info-map
{
;;---------------------maximum width ruler------------------------------
  "clojure.core/symbol" {:doc
  "Returns a Symbol with the given namespace and name.
  (symbol name): name can be string or symbol.
  (symbol ns name): ns and name must both be string.
  A symbol string, begins with a non-numeric character 
  and can contain alphanumeric characters and *, +, !, -, _, and ?.
  (see \"http://clojure.org/reader\" for details).
  Note that function does not validate input strings for ns and name, 
  and may return improper symbols with undefined behavior for 
  non-conformant ns and name."}
  
 "clojure.core/keyword" {:doc
  "Returns a Keyword with the given namespace and name.  Do not use :
  in the keyword strings, it will be added automatically.
  (keyword name): name can be string, symbol or keyword.
  (keyword ns name): ns and name must both be string.
  A keyword string, like a symbol, begins with a non-numeric character
  and can contain alphanumeric characters and *, +, !, -, _, and ?.
  (see \"http://clojure.org/reader\" for details).
  Note that function does not validate input strings for ns and name,
  and may return improper keywords with undefined behavior for
  non-conformant ns and name."}
  
 "FQN-goes-here" {:doc
  "New docs are right here.
  Keep indent at 2 space on the left to ensure correct formatting, also
  try not to extend the lines beyond the \"maximum width ruler\" 
  otherwise the formatting gets ugly."}
  
 "Example Phrase" {:object-type-str "General Information" :doc
  "New docs are right here.
  Keep indent at 2 space on the left to ensure correct formatting, also
  try not to extend the lines beyond the \"maximum width ruler\" 
  otherwise the formatting gets ugly."}
;;---------------------maximum width ruler------------------------------
})

