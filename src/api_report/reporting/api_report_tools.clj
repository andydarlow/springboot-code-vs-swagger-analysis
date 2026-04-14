(ns  api-report.reporting.api-report-tools
  "utils for comparing APIS defined in the springboot code (using reflection) and APIs defined in the swagger spec."
  (:require [clojure.string :as string]))


(defn url-matches? 
  "same URL path (ignoring path parameters)?"
  [api-url swagger-url] 
  (let [param-free-api-url (string/replace api-url #"\{[^}]*\}" "_")
        param-free-swagger-url (string/replace swagger-url #"\{[^}]*\}" "_")]
    (= param-free-api-url param-free-swagger-url)))

(defn rest-verb-matches? 
  "same REST VERB (GET/POST etc)?"
  [verb-a, verb-b]
  (= (string/upper-case verb-a) (string/upper-case verb-b)))


(defn api-matches-swagger-url? 
  "IS this springboot API call and the swagger URL the same API (same path and verb)?"
  [swagger-url springbootapi-call]
  (and (url-matches? (:path springbootapi-call) (:path swagger-url))
       (rest-verb-matches?  (:verb springbootapi-call) (:verb swagger-url))))


(defn is-springboot-api-in-swagger-urls?
  "True if the springboot API call appears in the list of swagger URLs."
  [swagger-urls springboot-api-url]
  (boolean (some #(api-matches-swagger-url? % springboot-api-url) swagger-urls)))


(defn is-swagger-url-in-springboot-apis?
  "True if the swagger URL appears in the list of APIs found in the spring boot code."
  [springboot-api-urls swagger-url]
  (boolean (some #(api-matches-swagger-url? % swagger-url) springboot-api-urls)))


(defn swagger-apis-in-springboot-code
  "For each swagger URL, return {:swagger-url … :in-springboot bool}."
  [swagger-urls springboot-api-calls]
  (map #(hash-map :swagger-url %
                  :in-springboot (is-swagger-url-in-springboot-apis? springboot-api-calls %))
       swagger-urls))


(defn springboot-apis-in-swagger
  "For each springboot API call, return {:api … :in-swagger bool}."
  [swagger-urls api-calls]
  (map #(hash-map :api %
                  :in-swagger (is-springboot-api-in-swagger-urls? swagger-urls %))
       api-calls))

