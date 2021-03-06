(ns puppetlabs.puppetdb.http.experimental.population-test
  (:require [cheshire.core :as json]
            [puppetlabs.puppetdb.scf.storage :as scf-store]
            [puppetlabs.puppetdb.http :as http]
            [clojure.test :refer :all]
            [clj-time.core :refer [now]]
            [puppetlabs.puppetdb.fixtures :refer :all]
            [puppetlabs.puppetdb.examples :refer :all]
            [puppetlabs.puppetdb.testutils :refer [get-request]]))

(use-fixtures :each with-test-db with-http-app)

(def c-t http/json-response-content-type)

(defn get-response
  ([]      (get-response nil))
  ([route] (*app* (get-request (str "/experimental/population/" route)))))

(defn is-response-equal
  "Test if the HTTP request is a success, and if the result is equal
to the result of the form supplied to this method."
  [response body]
  (is (= http/status-ok (:status response)))
  (is (= c-t (get-in response [:headers "Content-Type"])))
  (is (= (when-let [body (:body response)]
           (json/parse-string body true))
         body)))

(deftest collected-exported-resources
  (let [catalog (:basic catalogs)
        resources (:resources catalog)
        collector-catalog (assoc catalog :name "collector")
        exporter-catalog (-> catalog
                             (assoc :name "exporter")
                             (assoc :resources (into {} (for [[spec resource] resources]
                                                          [spec (assoc resource :exported true)]))))]
    (scf-store/add-certname! "collector")
    (scf-store/add-certname! "exporter")
    (scf-store/replace-catalog! collector-catalog (now))
    (scf-store/replace-catalog! exporter-catalog (now))

    (testing "should return a list of resources and who exports/collects them"
      (is-response-equal (get-response "exported-resources")
                         [{:type "Class" :title "foobar" :exporter "exporter" :collector "collector"}
                          {:type "File" :title "/etc/foobar" :exporter "exporter" :collector "collector"}
                          {:type "File" :title "/etc/foobar/baz" :exporter "exporter" :collector "collector"}]))))
