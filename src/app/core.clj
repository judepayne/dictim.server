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


;; Cheshire deserialization seems to lose single quotes around hex colors. restore them.

(defn- css-hex-color? [c]
  (re-matches #"#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$" c))


(defn- fix-hex [form]
  (clojure.walk/postwalk
   (fn [x]
     (if
       (and (string? x) (css-hex-color? x))
       (str "'" x "'")
       x))
   form))


(defn- bad [msg]
  {:status 400
   :body msg})


(def ^:private bad-json (bad "No json in body, or invalid json."))


(def ^:private bad-edn (bad "No edn in body, or invalid edn."))


(defn- good [content-type content]
  {:status 200
   :headers {"Content-Type" content-type}
   :body content})


(def ^:private good-svg (partial good "image/svg+xml"))


(def ^:private good-text (partial good "text/plain"))


(def ^:private good-json (partial good "application/json"))


(def ^:private good-edn (partial good "application/edn"))

;; the handlers

(defn graph->d2-json
  [{:keys [json-params] :as request}]
  (if json-params
    (try
      (let [d2 (graph/graph-spec->d2 (fix-hex json-params))
            svg (let [svg (d2->svg d2)]
                  (if (or (nil? svg) (= "" svg))
                    (throw (Exception. "The d2 engine returned nothing."))
                    svg))]
        (good-svg svg))
      (catch Exception e (-> e .getMessage bad)))
    bad-json))


(defn graph->d2-edn
  [{:keys [edn-params] :as request}]
  (if edn-params
    (try
      (let [d2 (graph/graph-spec->d2 (fix-hex edn-params))
            svg (let [svg (d2->svg d2)]
                  (if (or (nil? svg) (= "" svg))
                    (throw (Exception. "The d2 engine returned nothing."))
                    svg))]
        (good-svg svg))
      (catch Exception e (-> e .getMessage bad)))
    bad-edn))


(defn dictim-json
  [{:keys [json-params] :as request}]
  (if json-params
    (try
      (let [d2 (apply c/d2 (fix-hex json-params))]
        (good-svg (d2->svg d2)))
      (catch Exception e (-> e .getMessage bad)))
    bad-json))


(defn dictim-edn
  [{:keys [edn-params] :as request}]
  (if edn-params
    (try
      (let [d2 (apply c/d2 (fix-hex edn-params))]
        (good-svg (d2->svg d2)))
      (catch Exception e (-> e .getMessage bad)))
    bad-json))


(defn dictim-template-json
  [{:keys [json-params] :as request}]
  (if json-params
    (try
      (let [jp (fix-hex json-params)
            dictim (or (:dictim jp) (get jp "dictim"))
            directives (or (:directives jp) (get jp "directives"))
            template (or (:template jp) (get jp "template"))
            dictim (tp/add-styles dictim template directives)
            d2 (apply c/d2 dictim)]
        (good-svg (d2->svg d2)))
      (catch Exception e (-> e .getMessage bad)))
    bad-json))


(defn dictim-template-edn
  [{:keys [edn-params] :as request}]
  (if edn-params
    (try
      (let [jp (fix-hex edn-params)
            dictim (or (:dictim jp) (get jp "dictim"))
            directives (or (:directives jp) (get jp "directives"))
            template (or (:template jp) (get jp "template"))
            dictim (tp/add-styles dictim template directives)
            d2 (apply c/d2 dictim)]
        (good-svg (d2->svg d2)))
      (catch Exception e (-> e .getMessage bad)))
    bad-json))


(defn dictim->d2-json
  [{:keys [json-params] :as request}]
  (if json-params
    (try
      (good-text (apply c/d2 (fix-hex json-params)))
      (catch Exception e (-> e .getMessage bad)))
    bad-json))


(defn dictim->d2-edn
  [{:keys [edn-params] :as request}]
  (if edn-params
    (try
      (good-text (apply c/d2 (fix-hex edn-params)))
      (catch Exception e (-> e .getMessage bad)))
    bad-edn))


(defn d2->dictim-json
  [{:keys [body] :as request}]
  (if-let [d2 (slurp body)]
    (try
      (good-json (j/to-json (p/dictim d2)))
      (catch Exception e (-> e .getMessage bad)))
    (bad "No d2 in body.")))


(defn d2->dictim-edn
  [{:keys [body] :as request}]
  (if-let [d2 (slurp body)]
    (try
      (good-edn (pr-str (p/dictim d2)))
      (catch Exception e (-> e .getMessage bad)))
    (bad "No d2 in body.")))


;; Does not convert map keys to keywords for json
(def tweaked-body-params
  (body-params/body-params
   (body-params/default-parser-map :json-options {:key-fn identity})))


(def routes #{["/graph/json" :post
               [tweaked-body-params
                graph->d2-json]
               :route-name :graph->d2-json]

              ["/graph/edn" :post
               [(body-params/body-params)
                graph->d2-edn]
               :route-name :graph->d2-edn]

              ["/dictim/json" :post
               [tweaked-body-params
                dictim-json]
               :route-name :dictim-json]

              ["/dictim/edn" :post
               [tweaked-body-params
                dictim-edn]
               :route-name :dictim-edn]

              ["/dictim-template/json" :post
               [tweaked-body-params
                dictim-template-json]
               :route-name :dictim-template-json]

              ["/dictim-template/edn" :post
               [tweaked-body-params
                dictim-template-edn]
               :route-name :dictim-template-edn]

              ["/conversions/dictim-to-d2/json" :post
               [tweaked-body-params
                dictim->d2-json]
               :route-name :dictim->d2-json]

              ["/conversions/dictim-to-d2/edn" :post
               [tweaked-body-params
                dictim->d2-edn]
               :route-name :dictim->d2-edn]

              ["/conversions/d2-to-dictim/json" :post
               d2->dictim-json
               :route-name :d2->dictim-json]

              ["/conversions/d2-to-dictim/edn" :post
               d2->dictim-edn
               :route-name :d2->dictim-edn]})


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
