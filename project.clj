(def spring-app-classpath (str (get (System/getenv) "UNZIPPED_BOOT_APP_PATH" "<Springboot-app-unzipped>") "/BOOT-INF/classes"))
(def spring-hateoas-path (get (System/getenv) "HATEOAS_JAR_PATH" "~/.m2/repository/org/springframework/hateoas/spring-hateoas/2.5.2/spring-hateoas-2.5.2.jar"))


(defproject restcalls "0.1.0-SNAPSHOT"
  :description "tool to find differences between wahts in Spring boot code vs what's in the swagger spec"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.4"],
                 [clj-commons/pomegranate "1.2.25"]
                 [com.google.guava/guava "33.5.0-jre"]
                 [clj-commons/clj-yaml "1.0.29"]
                 [org.springframework/spring-web "6.2.12"]
                 [org.springframework.hateoas/spring-hateoas "2.5.2"]
                 [org.springframework.boot/spring-boot-dependencies "3.5.13" :extension "pom"]
                 [org.springframework.boot/spring-boot-starter "3.5.13"]
                 [org.springframework.boot/spring-boot-starter-data-jpa "3.5.13"]
                 [org.springframework/spring-webmvc "6.2.12"]
                 [org.springframework.security/spring-security-core "6.5.9"]
                 [org.springframework.security/spring-security-oauth2-resource-server "6.5.9"]
                 [org.springframework.security/spring-security-oauth2-client "6.5.9"]
                 [jakarta.validation/jakarta.validation-api "3.1.0"]
                 [com.fasterxml.jackson.core/jackson-databind "2.19.4"]
                 [org.springframework.data/spring-data-commons "3.5.10"]
                 [jakarta.xml.bind/jakarta.xml.bind-api "4.0.2"]
                 [jakarta.servlet/jakarta.servlet-api "6.0.0"]
                 [org.reflections/reflections "0.10.2"]]
  :resource-paths #=(eval (vector spring-app-classpath spring-hateoas-path)))
