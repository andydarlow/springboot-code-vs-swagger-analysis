(ns api-report.restcalls.spring-rest-calls
  "Used to find all the URL calls in the Spring boot code using reflection."
   (:require [clojure.string :as str] ))

;; -------- definitions --------------------------------------

(def rest-method-annotations
  "list of all the REST method annotations to look for on Methods the code"
  [org.springframework.web.bind.annotation.GetMapping
   org.springframework.web.bind.annotation.PutMapping
   org.springframework.web.bind.annotation.PostMapping
   org.springframework.web.bind.annotation.DeleteMapping])

(def rest-class-mapping 
  "Used to read the URL from the class. This will be prepended to the URL read from the method to get the full URL path for the REST call"
  [org.springframework.web.bind.annotation.RequestMapping])

(def default-base-search-package
  "Default root package for class scanning. Override at runtime by passing
   :base-package to get-apis-definitions, or via the APP_BASE_PACKAGE env var."
  (or (System/getenv "APP_BASE_PACKAGE") "com.mypackage))

(def default-application-base-url
  "Default URL prefix prepended to every discovered endpoint. Override at runtime
   by passing :base-url to get-apis-definitions, or via the APP_BASE_URL env var.
   NOTE: could extract this from the @SpringBootApplication annotation."
  (or (System/getenv "APP_BASE_URL") "/mycontext"))

;; --------- functions --------------------------------------------

(defn has-rest-annotation?
  "True if the method/class carries any of the given REST annotations
   (e.g. GetMapping, PostMapping)."
  [rest-annotations method]
  (some #(seq (.getAnnotationsByType method %)) rest-annotations))

(defn get-annotations-on
  "Return all annotations of any of rest-annotations present on the method or class."
  [rest-annotations reflected-object]
  (mapcat #(.getAnnotationsByType reflected-object %) rest-annotations))

(defn get-application-classes
  "Search the classpath for all classes under base-package (and subpackages).
   Returns an empty seq (with a warning to *err*) if the classpath cannot
   be enumerated."
  [base-package]
  (try
    (as-> (.getContextClassLoader (Thread/currentThread)) p
      (com.google.common.reflect.ClassPath/from p)
      (.getTopLevelClassesRecursive p base-package)
      (map #(.load %) p))
    (catch Exception e
      (binding [*out* *err*]
        (println (str "WARN: failed to scan package " base-package
                      " — " (.getMessage e))))
      [])))


(defn get-all-rest-annotated-class-methods
  "Return every method declared on classes (under base-package) whose declaring
   class carries one of rest-annotations."
  [base-package rest-annotations]
  (->> (get-application-classes base-package)
       (filter (partial has-rest-annotation? rest-annotations))
       (mapcat #(.getMethods %))))

(defn find-methods-with-annotations 
  "find all the methods passed in (from a class) that have REST annotations on them"
  [rest-annotations methods]
  (filter #(has-rest-annotation? rest-annotations %) methods))


(defn rest-annotation-name [rest-annotation]
  (.getSimpleName (first (.getInterfaces (.getClass rest-annotation)))))

(defn request-mapping-annotation
  "get the request mapping path from the class supporting the method"
  [method]
  (first (get-annotations-on rest-class-mapping (.getDeclaringClass method))))


(defn path-in-mapping-annotation
  "extracts the path out of the RequestMapping annotation"
  [mapping-annotation]
  (when mapping-annotation
    (or (first (.path mapping-annotation)) (first (.value mapping-annotation)))))


(defn remove-text
  "Remove every occurrence of text-to-remove from subject. Used to strip 'Mapping'
   from a REST annotation name to derive the HTTP verb."
  [subject text-to-remove]
  (str/replace subject text-to-remove ""))


(defn concat-url-fragments
  "Join two URL fragments, inserting a single '/' between them when neither
   side already provides one. Tolerates nil/blank on either side."
  [url-fragment-left url-fragment-right]
  (let [left  (or url-fragment-left "")
        right (or url-fragment-right "")]
    (cond
      (str/blank? right) left
      (str/blank? left)  right
      (and (not (str/ends-with? left "/"))
           (not (str/starts-with? right "/")))
      (str left "/" right)
      :else
      (str left right))))



(defn url-concat
  "merge a list of URL fragments (psrts of a URL) together to form full path" 
  [& fragments]
   (reduce concat-url-fragments fragments))


(defn extract-rest-url
  "Build a {:path :verb :class :method} map describing the REST call on method.
   application-base-url is prepended to the class- and method-level path fragments."
  [application-base-url rest-method-annotations method]
  (let [rest-annotation (first (get-annotations-on rest-method-annotations method))
        class-path      (path-in-mapping-annotation (request-mapping-annotation method))
        method-path     (path-in-mapping-annotation rest-annotation)]
    {:path   (url-concat application-base-url class-path method-path)
     :verb   (remove-text (rest-annotation-name rest-annotation) "Mapping")
     :class  (.getName (.getDeclaringClass method))
     :method (.getName method)}))


(defn get-apis-definitions
  "Find all REST calls in the application code and return a seq of
   {:path :verb :class :method} maps. Options:
     :base-package — root package to scan (default default-base-search-package)
     :base-url     — URL prefix prepended to every path (default default-application-base-url)"
  ([] (get-apis-definitions {}))
  ([{:keys [base-package base-url]
     :or   {base-package default-base-search-package
            base-url     default-application-base-url}}]
   (->> (get-all-rest-annotated-class-methods base-package rest-class-mapping)
        (find-methods-with-annotations rest-method-annotations)
        (map (partial extract-rest-url base-url rest-method-annotations)))))
