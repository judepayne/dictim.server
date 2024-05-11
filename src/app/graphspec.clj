(ns app.graphspec
  (:require [dictim.graph.core :as g]
            [dictim.d2.compile :as c]
            [clojure.data.json :as json]
            [dictim.template :as tp]
            [dictim.tests :as t]))


;; *****************************************
;; *            validation                 *
;; *****************************************


(defn- valid-style? [style]
  (map? style))


(defn- valid-label? [lbl]
  ;; simple validation for label instructions. TODO improve
  (or (map? lbl)
      (and (vector? lbl)
           (every? map? lbl))))


(defn- spec-errors
  [spec]
  (let [sp (partition 2 spec)
        valid-pair? (fn [acc [t o]]
                      (let [acc* (if (t/valid-test? t) acc (conj acc (str t " is not a valid test.")))
                            acc** (if (or (valid-style? o) (valid-label? o))
                                    acc* (conj acc* (str o " is not valid.")))]
                        acc**))]
    (reduce valid-pair? nil sp)))


(defn- graph-spec-errors
  "Checks that the diagram spec is valid. Returns true if it is and throws
   an exception with the validation errors found if not."
  [spec]
  (let [errs
        (as-> nil errors
          ;; map check
          (if-not (map? spec)
            (conj errors "The diagram spec must be a map.")
            errors)
          ;; check for incorrect keys
          (if-not (every? #{:nodes :edges :node->key :node->container
                            :container->parent :node-specs
                            :edge-specs :container->attrs} (keys spec))
            (conj errors "The diagram spec contains unrecognized keys.")
            errors)         
          ;; mandatory keys check
          (if-not (:nodes spec)
            (conj errors "The diagram spec must include a 'nodes' key.")
            errors)
          (if-not (:node->key spec)
            (conj errors "The diagram spec must include a 'node->key' key.")
            errors)
          ;; edge format check
          (if-let [edges (:edges spec)]
            (if-not (every? (fn [edge] (and (:src edge) (:dest edge))) edges)
              (conj errors "Every edge should include 'src' and 'dest' items.")
              errors)
            errors)
          ;; container->parent check
          (if-let [container->parent (:container-parent spec)]
            (if-not (map? container->parent)
              (conj errors "The value of the 'container->parent' key should be a map.")
              errors)
            errors)
          ;; container->parent check
          (if-let [container->attrs (:container-attrs spec)]
            (if-not (map? container->attrs)
              (conj errors "The value of the 'container->attrs' key should be a map.")
              errors)
            errors)
          ;; node-specs map check
          (if-let [node-specs (:node-specs spec)]
            (apply conj errors (spec-errors node-specs))
            errors)
          ;; edge-specs map check
          (if-let [edge-specs (:edge-specs spec)]
            (apply conj errors (spec-errors edge-specs))
            errors))]
    (when errs
      (reverse errs))))


;; *****************************************
;; *                Specs                  *
;; *****************************************


(defn- prep-label-component
  "Preps a single part of a label instruction."
  [item label-instruction-component]
  (let [k (:key label-instruction-component)
        tag (when (:show-key? label-instruction-component)
              (str (name k) ": "))]
    (if k

      (cond
        (and (vector? k)
             (every? #(or (string? %) (keyword? %)) k))
        (str tag (get-in item k))

        (or (string? k) (keyword? k))    (str tag (get item k nil))

        :else                            nil)
      
      (throw (Exception. (str "Label spec instruction: " label-instruction-component
                              " does not specify a key!"))))))


(defn- prep-label
  [item label-instruction]
  (cond
    (and (vector? label-instruction)
         (every? map? label-instruction))

    (apply str (interpose "\n" (map #(prep-label-component item %) label-instruction)))  

    (map? label-instruction)            (prep-label-component item label-instruction)

    :else label-instruction))


;; example diaagram spec
(def ex-diagram
  {:nodes '({:id "app12872",
             :name "Trade pad",
             :owner "Lakshmi",
             :dept "Finance",
             :functions ("Position Keeping" "Quoting"),
             :tco 1200000,
             :process "p.112"}
            {:id "app12873",
             :name "Data Source",
             :owner "India",
             :dept "Securities",
             :functions ("Booking" "Order Mgt"),
             :tco 1100000,
             :process "p.114"}
            {:id "app12874",
             :name "Crypto Bot",
             :owner "Joesph",
             :dept "Equities",
             :functions ("Accounting" "Booking"),
             :tco 500000,
             :process "p.112"}
            {:id "app12875",
             :name "Data Solar",
             :owner "Deepak",
             :dept "Securities",
             :functions ("Position Keeping" "Data Master"),
             :tco 1000000,
             :process "p.114"}
            {:id "app12876",
             :name "Data Solar",
             :owner "Lakshmi",
             :dept "Risk",
             :functions ("Accounting" "Data Master"),
             :tco 1700000,
             :process "p.114"})
   :edges '({:src "app12874", :dest "app12875", :data-type "security reference"}
            {:src "app12874", :dest "app12876", :data-type "quotes"}
            {:src "app12875", :dest "app12875", :data-type "instructions"}
            {:src "app12874", :dest "app12872", :data-type "instructions"}
            {:src "app12875", :dest "app12874", :data-type "client master"}
            {:src "app12875", :dest "app12874", :data-type "allocations"})
   :node->key :id
   :node->container :dept
   :container->parent {"Finance" "2LOD" "Risk" "2LOD" "Securities" "FO" "Equities" "FO"}
   :node-specs [["=" :dept "Equities"] {:label "myZZZZlabel" :style.fill ""}]
   :edge-specs [:else {:label {:key :data-type}}]
   :container->attrs {"Securities" {:style.fill "green"}}})


(defn spec-fn [tests]
  (fn [elem]
    (let [out ((t/test-fn tests) elem)]
      (if-let [lbl (:label out)]
        (let [new-lbl (prep-label elem lbl)]
          (assoc out :label new-lbl))
        out))))


;; *****************************************
;; *             Public API                *
;; *****************************************

(defn graph-spec->d2
  "Takes a diagram spec and produces d2.
   A diagram spec is a map which must have keys:
     [:data :nodes] a sequence of nodes (each of which is an arbitrary map).
     [:data :edges] a sequence of edges ( ditto ).
     :node->key     the key used to extract a unique value (the id) from each node.
   and optionally can have keys:
     :node-specs  a map with spec entries under :labels and :styles keys.
     :edge-specs  ditto
     :node->container a key applied to each node to determine which container it is in
     :container->parent a map with containers mapped to their parent containers.
     :container->attrs a map of the container (name) to a map of d2 styling elements,
       e.g. {\"my container\" {:style.fill \"pink\"} ...}}"
  [diag & {:keys [validate?] :or {validate? true}}]

  (when validate?
    (when-let [errors (graph-spec-errors diag)]
      (let [error-msg
            (apply str (interpose
                        "\n - "
                        (cons "Errors found during diagram spec validation:" errors)))]
        (throw (Exception. error-msg)))))

  (let [nodes (-> diag :nodes)
        edges (-> diag :edges)
        node->key (-> diag :node->key)
        node-fn (if (-> diag :node-specs)
                  (spec-fn (-> diag :node-specs)) (constantly nil))
        edge-fn (if (-> diag :edge-specs)
                  (spec-fn (-> diag :edge-specs)) (constantly nil))
        node->container (-> diag :node->container)
        container->parent (-> diag :container->parent)
        container->attrs (-> diag :container->attrs)
        directives (-> diag :directives)
        dictim-fn-params (cond->
                             {:node->key node->key
                              :node->attrs node-fn
                              :edge->attrs edge-fn
                              :cluster->attrs container->attrs}
                             node->container (assoc :node->cluster node->container)
                             container->parent (assoc :cluster->parent container->parent))
        dictim (g/graph->dictim nodes edges dictim-fn-params)
        dictim' (if directives (cons directives dictim) dictim)]
    (apply c/d2 dictim')))

#_((def ^{:private true} path "samples/in.d2")


   (defn out [diag]
     (spit path (graph-spec->d2 diag))))


;; *****************************************
;; *        deserialization cleanup        *
;; *****************************************

;; a few fixes to get the diagram spec deserialized into the form we need.

(defn serialize-diagram
  "Serializes a diagram spec to json."
  [diagram-spec]
  (json/write-str diagram-spec))


;; cheshire seems to remove single quotes from single-quoted strings.
(defn- single-quote-hex-color [maybe-color]
  (if (and (string? maybe-color) (clojure.string/starts-with? maybe-color "#"))
    (str "'" maybe-color "'")
    maybe-color))


(defn- fix-labels [m]
  (if (map? m)
    (clojure.walk/postwalk
     (fn [form]
       (if (string? form)
         (keyword form)
         form))
     m)
    m))


(defn- value-fn [[k v]]
  (cond
    (= k :label)
    [k (fix-labels v)]

    (or (= k :node->container) (= k :node->key))
    [k (keyword v)]

    (or (= k :container->parent) (= k :container->attrs))
    [k (into {}
             (map (fn [[k v]] [(name k) v]) v))]

    ;;cheshire seems to eliminate single-quote strings inside double-quoted strings
    :else [k (single-quote-hex-color v)]))


(defn- fix-test
  [t]
  (if (and (vector? t) (= 3 (count t)))
    (let [[a b c] t]
      [a (keyword b) c])
    t))


(defn- fix-maps
  [m f]
  (clojure.walk/postwalk
   (fn [x]
     (cond
       (= "else" x)  (keyword x)
       
       (map? x) (into {} (map f x))

       (t/valid-test? x) (fix-test x)
       
       :else x))
   m))


(defn fix-diagram-specs [m] (fix-maps m value-fn))
