(ns puppetlabs.puppetdb.http.factsets
  (:require [puppetlabs.puppetdb.http.query :as http-q]
            [puppetlabs.puppetdb.query.paging :as paging]
            [puppetlabs.puppetdb.query-eng :refer [produce-streaming-body]]
            [net.cgrand.moustache :refer [app]]
            [puppetlabs.puppetdb.middleware :refer [verify-accepts-json validate-query-params
                                                    wrap-with-paging-options]]))

(defn query-app
  [version]
  (app
   [&]
   {:get (comp (fn [{:keys [params globals paging-options] :as request}]
                 (produce-streaming-body
                  :factsets
                  version
                  (params "query")
                  paging-options
                  (:scf-read-db globals)))
               http-q/restrict-query-to-active-nodes)}))

(defn build-factset-app
  [query-app]
  (app
   []
   (verify-accepts-json query-app)))

(defn factset-app
  [version]
  (build-factset-app
   (-> (query-app version)
       (validate-query-params
        {:optional (cons "query" paging/query-params)})
       wrap-with-paging-options)))
