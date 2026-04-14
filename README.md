# Swagger Vs Code REST API Audit Tool

This is a simple tool I created to audit APIS in the codebase. The aim is to compare what API calls are in the spring boot code compared to a set of swagger docs the project team have created (Our project team define the APIs they need in swagger docs)
1. So that I can identify gaps in the documentation at the end of each sprint 
2. Identify potentially missing code (call in the swagger spec but missed in the code base) so we can discuss this with the team
3. Provide some stats on progress of APIs to our management team at the end of the sprint

I created this for personal use but happy for people to copy and adapt. ITS NOT PRODUCTION CODE. Its just used for personal reporting.

To use, place your swagger docs in a folder and unzip the springboot jar and follow the instructions below. 


See below for details



# Environment

 - Clojure
 - Leiningen to build the jar (see https://leiningen.org)
 - I run this in the clojure REPL


# To Use
1. untar/unzip the springboot jar folder.
2. set the env variable to point UNZIPPED_BOOT_APP_PATH to the unzipped folder (don't add a trailing /)
3. Set APP_BASE_PACKAGE to be java package you want to scan to find Spring boot api calls under (e.g. com.myproject)
4. set APP_BASE_URL to be the the context path found in swagger spec but not in the spring code (for example, your external API for a call in swagger  would be /mycontext/a/b but the swingboot would have /a/b. So set APP_BASE_URL to /mycontext )
3. ensure that you have hateoas lib in your maven rep:  ~/.m2/repository/org/springframework/hateoas/spring-hateoas/2.5.2/spring-hateoas-2.5.2.jar (mvn install if you dont have it with  mvn dependency:get -Dartifact=org.springframework.hateoas:spring-hateoas:2.5.2:jar). TODO: add it to project.cli
3. download/clone your folder of swaggers specs.
4. start the clojure REPL (lien repl)
5. Run some clojure code to get some data out about the REST calls in your code base/swagger docs. Samples below

# Sample queries to run

1. printout all the REST APIs in the code
```
(require '[api-report.reporting.sample-reports :as reporting])
(reporting/spring-calls-report)
```

2. printout all the REST API calls found in all the swagger docs where are the swagger docs are found in the folder passed in
```
(require '[api-report.reporting.sample-reports :as reporting])
(reporting/swagger-calls-report "<full path to folder holding the swagger specs>")
```

3. springboot apis in the code but not in swagger
```
(require '[api-report.reporting.sample-reports :as reporting])
(spit "missing-from-swagger.csv" (reporting/apis-missing-from-swagger-as-csv "<full path to folder holding the swagger specs>"))
```

4. swagger APIS not in Code
```
(require '[api-report.reporting.sample-reports :as reporting])
(spit "missing-in-code.csv" (reporting/swagger-urls-missing-from-code-as-csv "<full path to folder holding the swagger specs>"))
```

There are some more reports in sample-reports if you want to check them out

# TODO

1. Add this to the spring rest code to ignore rest calls if we know its missing from the swagger spec for a specific reason. Altough this shouldn't happen!!!!!!!!!!

(defn ignore-in-audit? [method]
    (has-annotation? [IgnoreInSwaggerAudit] method))


2. include org.springframework.hateoas:spring-hateoas:2.5.2:jar code to get this in build process

