(ns app.core (:require [io.pedestal.http :as http]
                       [io.pedestal.http.body-params :as body-params]
                       [environ.core :refer [env]]
                       [app.graphspec :as graph]
                       [io.pedestal.interceptor.helpers :as interceptor]
                       [clojure.java.shell :as sh]
                       [dictim.d2.compile :as c]
                       [clojure.java.io :as io])
    (:gen-class))


;; TODO Trap when json is badly formed.

;; Shell out to d2 executable
(defn- format-error [s err]
  (apply str
         err "\n"
         (interleave
          (map
           (fn [idx s]
             (format "%3d: %s" idx s))
           (range)
           (clojure.string/split-lines s))
          (repeat "\n"))))


(def path-to-d2 "d2")


(defmacro try-read
  "A macro that wraps an expr that can fail in a try .. catch"
  [expr]
  `(try ~expr
        (catch Exception e# false)))


(def layout-engine (or (try-read (slurp (io/resource "LAYOUT_ENGINE"))) "dagre"))


(def port (or (try-read (Integer. (slurp (io/resource "PORT")))) 5001))


(def ssl-port (or (try-read (Integer. (slurp (io/resource "SSLPORT")))) 5002))


(def theme (str (or (try-read (slurp (io/resource "THEME"))) 0)))


;; correct command line is:   echo "x -> y: hello" | d2 --layout tala -
(defn d2->svg
  "Takes a string of d2, and returns a string containing SVG."
  [d2 & {:keys [path layout] :or {path path-to-d2
                                  layout layout-engine}}]
  (let [{:keys [out err]} (sh/sh path "--layout" layout "--theme" theme "-" :in d2)]
    (or
     out
     (throw (IllegalArgumentException. ^String (str "d2 engine error: "(format-error d2 err)))))))


(defn graph->d2-handler
  [{:keys [headers json-params path-params body] :as request}]

  (if json-params
    (let [spec (graph/fix-diagram-specs json-params)]
      
      (try
        (let [d2 (graph/graph-spec->d2 spec)
              svg (let [svg (d2->svg d2)]
                    (if (or (nil? svg) (= "" svg))
                      (throw (Exception. "The d2 engine returned nothing."))
                      svg))]
          {:status 200
           :headers {"Content-Type" "image/svg+xml"}
           :body svg})
        (catch Exception e
          {:status 400
           :body (.getMessage e)})))
    {:status 400
     :body "No json in body, or invalid json."}))


(defn dictim-handler
  [{:keys [headers json-params path-params body] :as request}]
  (if json-params
    (try
      (let [d2 (apply c/d2 json-params)
            svg (d2->svg d2)]
        {:status 200
           :headers {"Content-Type" "image/svg+xml"}
           :body svg})
      (catch Exception e
        {:status 400
         :body (.getMessage e)}))
    {:status 400
     :body "No json in body, or invalid json."}))


(def routes #{["/graph" :post
               [(body-params/body-params) graph->d2-handler]
               :route-name :graph->d2]

              ["/dictim" :post
               [(body-params/body-params) dictim-handler]
               :route-name :dictim]})


(def service-map
  (-> {::http/routes routes
       ::http/host "0.0.0.0"
       ::http/join? false
       ::http/port port
       ::http/type :jetty
       ::http/container-options
       {:h2 true
        :ssl? true
        :ssl-port ssl-port
        :keystore "resources/jetty-keystore"
        :key-password "dictim.server"
        :security-provider "Conscrypt"}}))


(defn -main [] (-> service-map http/create-server http/start)) ; Server Instance
