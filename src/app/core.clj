(ns app.core (:require [io.pedestal.http :as http]
                       [io.pedestal.http.body-params :as body-params]
                       [environ.core :refer [env]]
                       [app.graphspec :as graph]
                       [io.pedestal.interceptor.helpers :as interceptor]
                       [clojure.java.shell :as sh]
                       [dictim.d2.compile :as c]
                       [dictim.d2.parse :as p]
                       [dictim.template :as tp]
                       [dictim.json :as j]
                       [clojure.java.io :as io])
    (:gen-class))


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
    (if (and (= out "") err)
      (throw (IllegalArgumentException. ^String (str "d2 engine error:\n"(format-error d2 err))))
      out)))

;; Pedestal automatically coerces map keys to keywords for application/json
;; and this leaves a (graph) diagram spec in an inconsistent state (see doctstring
;; of graph-spec->d2 about consistency). This fn undoes the coerce.
;; also deserialization seems to lose single quotes around hex colors. restore them.

(defn- css-hex-color? [c]
  (re-matches #"#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$" c))

(defn- fix-keyword-hex [form]
  (clojure.walk/postwalk
   (fn [x]
     (cond
       (keyword? x)
       (name x)

       (and (string? x) (css-hex-color? x))
       (str "'" x "'")
       
       :else x))
   form))

(defn- fix-hex [form]
  (clojure.walk/postwalk
   (fn [x]
     (if
       (and (string? x) (css-hex-color? x))
       (str "'" x "'")
       x))
   form))




(defn graph->d2-handler
  [{:keys [headers json-params path-params body] :as request}]

  (if json-params
    (try
      (let [d2 (graph/graph-spec->d2 (fix-keyword-hex json-params))
            svg (let [svg (d2->svg d2)]
                  (if (or (nil? svg) (= "" svg))
                    (throw (Exception. "The d2 engine returned nothing."))
                    svg))]
        {:status 200
         :headers {"Content-Type" "image/svg+xml"}
         :body svg})
      (catch Exception e
        {:status 400
         :body (.getMessage e)}))
    {:status 400
     :body "No json in body, or invalid json."}))


(defn dictim-handler
  [{:keys [headers json-params path-params body] :as request}]
  (if json-params
    (try
      (let [d2 (apply c/d2 (fix-hex json-params))
            svg (d2->svg d2)]
        {:status 200
           :headers {"Content-Type" "image/svg+xml"}
           :body svg})
      (catch Exception e
        {:status 400
         :body (.getMessage e)}))
    {:status 400
     :body "No json in body, or invalid json."}))


(defn dictim-template-handler
  [{:keys [headers json-params path-params body] :as request}]
  (if json-params
    (try
      (let [{dictim :dictim
             directives :directives
             template :template} (fix-hex json-params)
            dictim (tp/add-styles dictim template directives)
            d2 (apply c/d2 dictim)
            svg (d2->svg d2)]
        {:status 200
         :headers {"Content-Type" "image/svg+xml"}
         :body svg})
      (catch Exception e
        {:status 400
         :body (.getMessage e)}))
    {:status 400
     :body "No json in body, or invalid json."}))


(defn dictim->d2-handler
  [{:keys [headers json-params path-params body] :as request}]
  (if json-params
    (try
      (let [d2 (apply c/d2 (fix-hex json-params))]
        {:status 200
         :headers {"Content-Type" "text/plain"}
         :body d2})
      (catch Exception e
        {:status 400
         :body (.getMessage e)}))
    {:status 400
     :body "No json in body, or invalid json."}))


(defn d2->dictim-handler
  [{:keys [headers json-params path-params body] :as request}]
  (if-let [d2 (slurp (:body request))]
    (try
      (let [dict (j/to-json (p/dictim d2))]
        {:status 200
         :headers {"Content-Type" "text/plain"}
         :body dict})
      (catch Exception e
        {:status 400
         :body (.getMessage e)}))
    {:status 400
     :body "No d2 in body."}))


(def routes #{["/graph/json" :post
               [(body-params/body-params) graph->d2-handler]
               :route-name :graph->d2]

              ["/dictim/json" :post
               [(body-params/body-params) dictim-handler]
               :route-name :dictim]

              ["/dictim-template/json" :post
               [(body-params/body-params) dictim-template-handler]
               :route-name :dictim-template]

              ["/conversions/dictim-to-d2/json" :post
               [(body-params/body-params) dictim->d2-handler]
               :route-name :dictim->d2]

              ["/conversions/d2-to-dictim" :post
               d2->dictim-handler
               :route-name :d2->dictim]})


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
