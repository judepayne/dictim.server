(ns app.graphspec
  (:require [dictim.graph.core :as g]
            [dictim.d2.compile :as c]
            [dictim.template :as tp]
            [dictim.tests :as t]))


;; *****************************************
;; *            validation                 *
;; *****************************************


(defn- get*
  "Like get but indifferent to whether k is a keyword or a string."
  [m k]
  (if (keyword? k)
    (or (k m) (get m (name k)))
    (or (get m k) (get m (keyword k)))))


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
          (if-not (every? #{:nodes "nodes" :edges "edges"
                            :node->key "node->key" :node->container "node->container"
                            :container->parent "container->parent" :node-specs "node-specs"
                            :edge-specs "edge-specs" :container->attrs "container->attrs"}
                          (keys spec))
            (conj errors "The diagram spec contains unrecognized keys.")
            errors)         
          ;; mandatory keys check
          (if-not (or (get spec "nodes") (:nodes spec))
            (conj errors "The diagram spec must include a 'nodes' key.")
            errors)
          (if-not (or (get spec "node->key") (:node->key spec))
            (conj errors "The diagram spec must include a 'node->key' key.")
            errors)
          ;; edge format check
          (if-let [edges (or (get spec "edges") (:edges spec))]
            (if-not (every? (fn [edge] (or (and (get edge "src") (get edge "dest"))
                                           (and (:src edge) (:dest edge))))
                            edges)
              (conj errors "Every edge should include 'src' and 'dest' items.")
              errors)
            errors)
          ;; container->parent check
          (if-let [container->parent (or (get spec "container->parent")
                                         (:container-parent spec))]
            (if-not (map? container->parent)
              (conj errors "The value of the 'container->parent' key should be a map.")
              errors)
            errors)
          ;; container->parent check
          (if-let [container->attrs (or (get spec "container->attrs")
                                        (:container-attrs spec))]
            (if-not (map? container->attrs)
              (conj errors "The value of the 'container->attrs' key should be a map.")
              errors)
            errors)
          ;; node-specs map check
          (if-let [node-specs (or (get spec "node-specs") (:node-specs spec))]
            (apply conj errors (spec-errors node-specs))
            errors)
          ;; edge-specs map check
          (if-let [edge-specs (or (get spec "edge-specs") (:edge-specs spec))]
            (apply conj errors (spec-errors edge-specs))
            errors))]
    (when errs
      (reverse errs))))


;; *****************************************
;; *                Specs                  *
;; *****************************************



(defn- prep-label-instruction [item inst]
  (assert (or (get inst "key") (:key inst))
          (str inst " is not a valid instruction for forming a label"))
  (let [k (or (get inst "key") (:key inst))
        component (fn [v]
                    (or (get item v) (get item (keyword v))))]
    (cond
      (vector? k)  (apply str
                          (interpose
                           "\n"
                           (map component k)))

      (or (string? k) (keyword? k))    (component k)

      :else (throw (IllegalArgumentException.
                    (str inst " the value under 'key' cannot be used to form a label"))))))


(defn- prep-label
  [item label-instruction]
  (cond
    (string? label-instruction)       label-instruction

    (map? label-instruction)          (prep-label-instruction item label-instruction)

    :else (throw (IllegalArgumentException.
                  (str label-instruction " is not a valid label instruction")))))


;; example dev data
(def ex-diag1
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
   :node-specs [["=" :dept "Equities"] {:label "myZZZZlabel" :style.fill ""}
                :else {:label {:key :name}}]
   :edge-specs [:else {:label {:key :data-type}}]
   :container->attrs {"Securities" {:style.fill "green"}}})


;; more example dev data
(def ex-diag2
  {"nodes"
   [{"id" "app12872",
     "name" "Trade pad",
     "owner" "Lakshmi",
     "dept" "Finance",
     "functions" ["Position Keeping" "Quoting"],
     "tco" 1200000,
     "process" "p.112"}
    {"id" "app12873",
     "name" "Data Source",
     "owner" "India",
     "dept" "Securities",
     "functions" ["Booking" "Order Mgt"],
     "tco" 1100000,
     "process" "p.114"}
    {"id" "app12874",
     "name" "Crypto Bot",
     "owner" "Joesph",
     "dept" "Equities",
     "functions" ["Accounting" "Booking"],
     "tco" 500000,
     "process" "p.112"}
    {"id" "app12875",
     "name" "Data Solar",
     "owner" "Deepak",
     "dept" "Securities",
     "functions" ["Position Keeping" "Data Master"],
     "tco" 1000000,
     "process" "p.114"}
    {"id" "app12876",
     "name" "Data Solar",
     "owner" "Lakshmi",
     "dept" "Risk",
     "functions" ["Accounting" "Data Master"],
     "tco" 1700000,
     "process" "p.114"}],
   "edges"
   [{"src" "app12874",
     "dest" "app12875",
     "data-type" "security reference"}
    {"src" "app12874", "dest" "app12876", "data-type" "quotes"}
    {"src" "app12875", "dest" "app12875", "data-type" "instructions"}
    {"src" "app12874", "dest" "app12872", "data-type" "instructions"}
    {"src" "app12875", "dest" "app12874", "data-type" "client master"}
    {"src" "app12875", "dest" "app12874", "data-type" "allocations"}],
   "node->key" "id",
   "node->container" "dept",
   "container->parent"
   {"Finance" "2LOD",
    "Risk" "2LOD",
    "Securities" "FO",
    "Equities" "FO"},
   "node-specs"
   [["=" "dept" "Equities"] {"label" "myZZZZlabel", "style.fill" ""}
    "else" {"label" {"key" "name"}}],
   "edge-specs" ["else" {"label" {"key" "data-type"}}],
   "container->attrs" {"Securities" {"style.fill" "green"}}})


(defn spec-fn [tests]
  (fn [elem]
    (let [out ((t/test-fn tests) elem)]
      (if-let [lbl (or (get out "label") (:label out))]
        (let [new-lbl (prep-label elem lbl)]
          (dissoc (assoc out :label new-lbl) "label"))
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
       e.g. {\"my container\" {:style.fill \"pink\"} ...}}
   This function makes no assumptions about whether the of the diagram spec are strings
   or keywords (both work). Similarly keywords and strings both work in specifying the
   nodes and edges data. The only assumption is that the spec is internally consistent.
   e.g. if a node is a map of keywords, then node-specs, node->container, etc will
   also  be specified using keywords."
  [diag & {:keys [validate?] :or {validate? true}}]

  (when validate?
    (when-let [errors (graph-spec-errors diag)]
      (let [error-msg
            (apply str (interpose
                        "\n - "
                        (cons "Errors found during diagram spec validation:" errors)))]
        (throw (Exception. error-msg)))))

  (let [nodes (get* diag :nodes)
        edges (get* diag :edges)
        node->key (let [nk (get* diag :node->key)]
                    (fn [n]
                      (or (get n nk) (get n (keyword nk)))))
        node-fn (if-let [ns (get* diag :node-specs)]
                  (spec-fn ns) (constantly nil))
        edge-fn (if-let [es (get* diag :edge-specs)]
                  (spec-fn es) (constantly nil))
        node->container (let [nc (get* diag :node->container)] #(get % nc))
        container->parent (let [cp (get* diag :container->parent)] #(get cp %))
        container->attrs (let [ca (get* diag :container->attrs)] #(get ca %))
        directives (get* diag :directives)
        dictim-fn-params (cond->
                             {:node->key node->key
                              :node->attrs node-fn
                              :edge->attrs edge-fn
                              :cluster->attrs container->attrs
                              :edge->src-key #(or (get % "src") (get % :src))
                              :edge->dest-key #(or (get % "dest") (get % :dest))}
                             node->container (assoc :node->cluster node->container)
                             container->parent (assoc :cluster->parent container->parent))
        dictim (g/graph->dictim nodes edges dictim-fn-params)
        dictim' (if directives (cons directives dictim) dictim)]
    (apply c/d2 dictim')))
