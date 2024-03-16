(ns app.graphspec
  (:require [dictim.graph.core :as g]
            [dictim.d2.compile :as c]
            [clojure.data.json :as json]))


;; Please see https://github.com/judepayne/dictim.cookbook
;; for more commentary on the code in this namespace.


(def ^{:private true} comparators
  {:equals =
   :not-equals not=
   :contains some
   :doesnt-contain (complement some)
   :> >
   :< <
   :<= <=
   :>= >=})


;; *****************************************
;; *              Conditions               *
;; *****************************************

(defn- get*
  "A generalized version of get/ get-in.
   If k is a keyword/ string, performs a normal get from the map m, otherwise
   if k is a vector of keywords/ strings performs a get-in."
  [m k]
  (cond
    (keyword? k)         (k m)
    (string? k)          (get m k)
    (and (vector? k)
         (every? #(or (string? %) (keyword? %)) k))
    (get-in m k)
    
    :else (throw (Exception. (str "Key must be a keyword, string or vector of either.")))))


(defn- contains-vectors?
  "Returns true if coll contains one or more vectors."
  [coll]
  (some vector? coll))


(defmacro ^{:private true} single-condition
  "Returns code that tests whether the condition is true for the item
   specified by sym."
  [sym condition]
  `(let [[comparator# k# v#] ~condition
         v-found# (get* ~sym k#)
         comp# (comparator# comparators)]

     (cond
       (and (not (coll? v-found#))
            (or (= :contains comparator#) (= :doesnt-contain comparator#)))
       (throw (Exception. (str ":contains and :doesnt-contain can only be used on collections. "
                               "No collection was found under the key " k#
                               " for the item " ~sym)))

       (coll? v-found#)
       (comp# (conj #{} v#) v-found#)

       :else
       (comp# v-found# v#))))


(defmacro ^{:private true} condition
  "Returns code that tests whether the condition/s is/are true for the item
   specified by sym."
  [sym condition]
  `(if (contains-vectors? ~condition)
     (if (= (first ~condition) :or)
       (some identity (map #(single-condition ~sym %) (rest ~condition)))
       (every? identity (map #(single-condition ~sym %) (rest ~condition))))
     (single-condition ~sym ~condition)))

;; *****************************************
;; *            validation                 *
;; *****************************************

(defn- valid-single-condition?
  [condition]
  (and
   ;; is a vector
   (vector? condition)

   ;; the first item is a comparator
   ;; (in either keyword or name/string form).
   (or (some #{(first condition)} (keys comparators))
       (some #{(first condition)} (map name (keys comparators))))

   ;; has 3 elements
   (= 3 (count condition))))


(defn- valid-condition?
  [condition]
  (if (and (vector? condition) (contains-vectors? condition))
    (and
     (let [comp (first condition)]
       (some #{comp} [:or :and]))
     (every? valid-single-condition? (rest condition)))
    (valid-single-condition? condition)))


(defn- valid-style? [style]
  (map? style))


(defn- valid-label? [lbl]
  ;; simple validation for label instructions. TODO improve
  (or (map? lbl)
      (and (vector? lbl)
           (every? map? lbl))))


;; I don't like the next 3 functions but error messages coming from clojure.spec are no good.
;; and I couldn't think of a better way to check over all possible errors in a graph spec
;; in one pass without stopping.
(defn- specs-errors
  [spec-type specs]
  (let [counts (map count specs)]
    (as-> nil errors      
      ;; count check
      (if-not (every? #(or (= 1 %) (= 2 %)) counts)
        (conj errors (str "Each spec in "
                          spec-type "s " specs
                          " must be either a one element spec or a two element (conditional) spec."))
        errors)
      ;; only 1 one-element spec is allowed
      (if-not (< (count (filter #(= 1 %) counts))  2)
        (conj errors (str "Any spec can only have a single one element spec. The spec " spec-type "s " specs
                          "breaks this rule."))
        errors)
      ;; the one element spec should be a valid label or style instruction
      (let [else-spec (first (filter #(= 1 (count %)) specs))]
        (if (nil? else-spec)
          errors
          (if-not (case spec-type
                    :label (valid-label? (first else-spec))
                    :style (valid-style? (first else-spec)))
            (conj errors (str "The spec " spec-type "s " specs "is not valid."))
            errors)))
      ;; two element specs are valid
      (let [conditional-specs (filter #(= 2 (count %)) specs)
            invalids? (reduce
                       (fn [acc spec]
                         (let [[style-or-label condition] spec]
                           (if-not (and
                                    (case spec-type
                                      :label (valid-label? style-or-label)
                                      :style (valid-style? style-or-label))
                                    (valid-condition? condition))
                             (conj acc (str "The spec " spec " isn't valid.")))))
                       nil
                       conditional-specs)]
        (if invalids?
          (concat errors invalids?)
          errors)))))


(defn- specs-map-errors
  [m]
  (let [vs (conj [(:labels m)] (:styles m))]
    (if (or (empty? vs)
            (every? #(or (nil? %) (vector? %)) vs))
      
      (let [label-errs (specs-errors :label (:labels m))
            style-errs (specs-errors :style (:styles m))
            combined (concat label-errs style-errs)]
        (if (empty? combined)
          nil
          combined))

      (list (str "Every label or style spec should be a vector in this specs map: " m)))))


(defn graph-spec-errors
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
            (if-not (map? node-specs)
              (conj errors "The value of the 'node-specs' key should be a map.")
              (if-not (every? #{:labels :styles} (keys node-specs))
                (conj errors "The map under 'node-specs' can only contain keys 'labels' or 'styles'")
                (let [sme (specs-map-errors node-specs)]
                  (if sme
                    (concat sme errors)
                    errors))))
            errors)
          ;; edge specs map check
          (if-let [edge-specs (:edge-specs spec)]
            (if-not (map? edge-specs)
              (conj errors "The value of the 'edge-specs' key should be a map.")
              (if-not (every? #{:labels :styles} (keys edge-specs))
                (conj errors "The map under 'edge-specs' can only contain keys 'labels' or 'styles'")
                (let [sme (specs-map-errors edge-specs)]
                  (if sme
                    (concat sme errors)
                    errors))))
            errors))]
    (when errs
      (reverse errs))))


;; *****************************************
;; *                Specs                  *
;; *****************************************


(defn- put-last
  [pred coll]
  "Puts the first item in coll that satisfies pred to the end."
  (let [splits (split-with (complement pred) coll)]
    (cond
      (empty? (first splits))
      (conj (into [] (rest (second splits))) (first coll))

      (empty? (second splits))
      coll

      :else
      (let [front (into [] (concat (first splits) (rest (second splits))))]
        (conj front (first (second splits)))))))


(defn- without-condition-spec?
  [spec]
  (= 1 (count spec)))


(defn- convert-without-condition-spec
  [spec]
  (if (without-condition-spec? spec)
    (list :else (first spec))
    spec))


(defn- prep-specs
  "If there's an :else clause, ensure it's at the end."
  [spec-type specs]
  (->> (put-last without-condition-spec? specs)
       (map reverse)
       (map convert-without-condition-spec)))


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

    :else nil))


(defmacro ^{:private true} specs
  "Convert specs into functions which are matched against the value stored
  in sym. spec-type must be either :label or :style."
  [sym spec-type specs]
  `(let [sp# (prep-specs ~spec-type ~specs)
         as-fns# (reduce
                  (fn [acc# [condition# res#]]
                    (let [res1# (if (= :label ~spec-type)
                                  (prep-label ~sym res#) res#)]
                      (if (valid-condition? condition#)
                        (conj acc# [(condition ~sym condition#) res1#])
                        (conj acc# [condition# res1#]))))
                  []
                  sp#)]
     as-fns#))


(defn- first-true
  "Returns the application of f on the first value whose resolved spec is true."
  ([resolved-spec] (first-true identity resolved-spec))
  ([f resolved-spec]
   (reduce
    (fn [acc [resolved? v]]
      (if resolved? (reduced (f v)) acc))
    nil
    resolved-spec)))


(defn- add-entries [& entries]
  (reduce (fn [acc [k v]]
            (if-not v
              acc
              (assoc acc k v)))
          nil
          entries))


(defmacro ^{:private true} spec-fn
  "Converts a node->attrs/ edge-attrs expression that use data to 
  express specs and conditions into a function."
  [m]
  `(fn [item#]
     (let [label# (:labels ~m)
           style# (:styles ~m)
           lbl# (when label# [:label (first-true (specs item# :label label#))])
           stl# (when style# [:style (first-true (specs item# :style style#))])]
       (add-entries lbl# stl#))))


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
   :node-specs {:labels [[{:key :owner} [:equals :dept "Equities"]][{:key :name}]]}
   :edge-specs {:labels [[{:key :data-type}]]}
   :container->attrs {"Securities" {:style.fill "green"}}})



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
      (let [error-msg (apply str (interpose
                                  "\n - "
                                  (cons "Errors found during diagram spec validation:" errors)))]
        (throw (Exception. error-msg)))))

  (let [nodes (-> diag :nodes)
        edges (-> diag :edges)
        node->key (-> diag :node->key)
        node-fn (if (-> diag :node-specs) (spec-fn (-> diag :node-specs)) (constantly nil))
        edge-fn (if (-> diag :edge-specs) (spec-fn (-> diag :edge-specs)) (constantly nil))
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


(def ^{:private true} path "samples/in.d2")


(defn out [diag]
  (spit path (graph-spec->d2 diag)))


;; serialization/ deserialization of diagram specs


(defn serialize-diagram
  "Serializes a diagram spec to json."
  [diagram-spec]
  (json/write-str diagram-spec))


;; cheshire seems to single quotes from single-quoted strings.
(defn- single-quote-hex-color [maybe-color]
  (if (and (string? maybe-color) (clojure.string/starts-with? maybe-color "#"))
    (str "'" maybe-color "'")
    maybe-color))


(defn- convert-element [type element]
  (cond
    (and (= type :labels) (map? element))
    (into {} (map (fn [[k v]] [k (keyword v)]) element))

    (vector? element)
    (conj (mapv keyword (take 2 element)) (last element))

    :else element))


(defn- convert-specs [type specs]
  (mapv #(mapv (fn [spec] (convert-element type spec)) %) specs))


(defn value-fn [[k v]]
  (cond
    (or (= k :labels) (= k :styles))
    [k (convert-specs k v)]

    (or (= k :node->container) (= k :node->key))
    [k (keyword v)]

    (or (= k :container->parent) (= k :container->attrs))
    [k (into {}
            (map (fn [[k v]] [(name k) v]) v))]

    ;;cheshire seems to eliminate single-quote strings inside double-quoted strings
    :else [k (single-quote-hex-color v)]))


(defn fix-maps
  [m f]
  (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m))


(defn fix-diagram-specs [m] (fix-maps m value-fn))
