
(ns api-report.restcalls.swagger-rest-calls
  "Used to find all the URL calls in a swagger spec.
   list-swagger-URLs is the main function used in this file."
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defn filter-out-summary 
   "filter out the x-summary comment that describes the Path"
   [verbs] 
   (filter #(not= :x-summary %1) verbs))

(defn make-url 
   "make the absolute URL path from the path passed in (which is a key in the path)"
   [swagger url-path-key] 
   (str (:basePath swagger) "/" (name url-path-key)))

(defn urls-path-keys 
  "Gives the path of all the end points found in the swagger spec."
  [swagger] (keys (:paths swagger)))


(defn http-verbs 
  "Http verbs (GET/POST etc) found under the path in the swagger spec"
  [swagger urls-path-key]  
  (keys (get-in swagger [:paths urls-path-key])))

(defn make-URL-summaries
  "For one swagger path, return a seq of {:path :verb} for every HTTP verb defined under it."
  [swagger urls-path-key]
  (let [path  (make-url swagger urls-path-key)
        verbs (filter-out-summary (http-verbs swagger urls-path-key))]
    (map #(hash-map :path path :verb (string/upper-case (name %))) verbs)))

(defn extract-URLS
  "Extract the REST URI paths and their verbs from a parsed swagger spec."
  [swagger-spec]
  (mapcat #(make-URL-summaries swagger-spec %) (urls-path-keys swagger-spec)))

(defn read-swagger
  "Parse a swagger YAML file. Returns the parsed map, or nil after logging
   to *err* when the file is missing or malformed."
  [swagger-yml-file]
  (try
    (yaml/parse-string (slurp swagger-yml-file))
    (catch Exception e
      (binding [*out* *err*]
        (println (str "WARN: skipping swagger file " swagger-yml-file
                      " — " (.getMessage e))))
      nil)))


(defn get-yaml-file-paths
  "List all .yaml files under root-dir."
  [root-dir]
  (let [directory (io/file root-dir)]
    (->> (file-seq directory)
         (filter #(.isFile %))
         (map #(.getAbsolutePath %))
         (filter #(string/ends-with? % ".yaml")))))

(defn list-swagger-URLs
  "Return a seq of {:path :verb} for every endpoint found in every .yaml file
   under root-dir. Files that fail to parse are skipped with a warning.
   IMPORTANT: assumes every .yaml file is a swagger spec."
  [root-dir]
  (->> (get-yaml-file-paths root-dir)
       (keep read-swagger)
       (mapcat extract-URLS)))

