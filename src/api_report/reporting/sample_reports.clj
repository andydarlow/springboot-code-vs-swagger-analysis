(ns api-report.reporting.sample-reports
  "sample reports to generate some useful data to report about swagger vs code base API"
  (:require [clojure.pprint :as pp]
            [api-report.restcalls.spring-rest-calls :as spring-calls]
            [api-report.reporting.api-report-tools :as report-tools]
            [api-report.restcalls.swagger-rest-calls :as swagger-calls]
            [clojure.string :as string]))


(defn fetch-data
  "Fetch swagger URLs and Spring API definitions in one go so callers can pass
   them to multiple report functions without recomputing."
  [swagger-root-dir]
  {:swagger-urls (swagger-calls/list-swagger-URLs swagger-root-dir)
   :api-calls    (spring-calls/get-apis-definitions)})

(defn apis-missing-from-swagger
  "SpringBoot REST calls found in the code that are missing from the swagger spec."
  [{:keys [swagger-urls api-calls]}]
  (->> (report-tools/springboot-apis-in-swagger swagger-urls api-calls)
       (remove :in-swagger)))

(defn swagger-urls-missing-from-code
  "Swagger URLs that are missing from the SpringBoot code."
  [{:keys [swagger-urls api-calls]}]
  (->> (report-tools/swagger-apis-in-springboot-code swagger-urls api-calls)
       (remove :in-springboot)))

(defn apis-in-both
  "SpringBoot REST calls that are present in both the code and the swagger spec."
  [{:keys [swagger-urls api-calls]}]
  (->> (report-tools/springboot-apis-in-swagger swagger-urls api-calls)
       (filter :in-swagger)))
  

;;----------------------- utils for dumping above functions to CSV -------------------


(defn- csv-escape
  "Escape a single CSV field per RFC 4180 — quote when the value contains
   a comma, quote, CR or LF; double any internal quotes."
  [v]
  (let [s (str v)]
    (if (re-find #"[,\"\r\n]" s)
      (str \" (string/replace s "\"" "\"\"") \")
      s)))

(defn to-csv
  "Render data as CSV. fields is a list of key paths (passed to get-in) defining
   column order. The first row is a header derived from the last key in each path.
   Example: (to-csv data [[:api :verb] [:api :path]])"
  [data fields]
  (let [header (string/join "," (map #(csv-escape (name (last %))) fields))
        row    (fn [record] (string/join "," (map #(csv-escape (get-in record %)) fields)))]
    (string/join "\n" (cons header (map row data)))))
 
(defn apis-in-both-as-csv
  "SpringBoot REST calls present in both the code and the swagger spec, as CSV."
  [swagger-root-dir]
  (to-csv (apis-in-both (fetch-data swagger-root-dir)) [[:api :verb] [:api :path]]))

(defn swagger-urls-missing-from-code-as-csv
  "Swagger URLs missing from the SpringBoot code, as CSV."
  [swagger-root-dir]
  (to-csv (swagger-urls-missing-from-code (fetch-data swagger-root-dir))
          [[:swagger-url :verb] [:swagger-url :path]]))

(defn apis-missing-from-swagger-as-csv
  "SpringBoot REST calls missing from the swagger spec, as CSV."
  [swagger-root-dir]
  (to-csv (apis-missing-from-swagger (fetch-data swagger-root-dir))
          [[:api :verb] [:api :path]]))

;;------------------- call reporting functions below to get the data printed out on the terminal-------------------


(defn spring-calls-report
  "Simple report to dump out all the REST calls found in the spring boot code using reflection. "
  []
  (let [spring-rest-apis (spring-calls/get-apis-definitions)
        urls (filter #(not (clojure.string/blank? %)) (sort (set (map :path spring-rest-apis))))]
    (println (str "REST calls with blank paths:" " " (count urls)))
    (pp/print-table spring-rest-apis)))


(defn swagger-calls-report
  "Simple report to dump out all the REST calls found in the swagger spec under swagger-root-dir"
  [swagger-root-dir]
    (pp/print-table (swagger-calls/list-swagger-URLs swagger-root-dir)))


(defn compare-swagger-to-code-report
  "Generate a report of which swagger calls are missing from the code and which
   code calls are missing from the swagger spec. Reads swagger + code once."
  [swagger-root-dir]
  (let [data (fetch-data swagger-root-dir)
        api-missing-from-swagger       (apis-missing-from-swagger data)
        swagger-missing-from-api       (swagger-urls-missing-from-code data)
        in-both-swagger-and-springboot (apis-in-both data)]
    (println "APIs missing from swagger:" (count api-missing-from-swagger))
    (pp/print-table api-missing-from-swagger)
    (println "Swagger URLs missing from API:" (count swagger-missing-from-api))
    (pp/print-table swagger-missing-from-api)
    (println "APIs in both springboot and swagger:" (count in-both-swagger-and-springboot))
    (pp/print-table in-both-swagger-and-springboot)))



