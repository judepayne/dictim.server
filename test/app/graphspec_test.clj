(ns app.graphspec-test
  (:require [clojure.test :refer :all]
            [app.graphspec :as g]))


(def req-json
  "{\"nodes\":[{\"id\":\"app12872\",\"name\":\"Trade pad\",\"owner\":\"Lakshmi\",\"dept\":\"Finance\",\"functions\":[\"Position Keeping\",\"Quoting\"],\"tco\":1200000,\"process\":\"p.112\"},{\"id\":\"app12873\",\"name\":\"Data Source\",\"owner\":\"India\",\"dept\":\"Securities\",\"functions\":[\"Booking\",\"Order Mgt\"],\"tco\":1100000,\"process\":\"p.114\"},{\"id\":\"app12874\",\"name\":\"Crypto Bot\",\"owner\":\"Joesph\",\"dept\":\"Equities\",\"functions\":[\"Accounting\",\"Booking\"],\"tco\":500000,\"process\":\"p.112\"},{\"id\":\"app12875\",\"name\":\"Data Solar\",\"owner\":\"Deepak\",\"dept\":\"Securities\",\"functions\":[\"Position Keeping\",\"Data Master\"],\"tco\":1000000,\"process\":\"p.114\"},{\"id\":\"app12876\",\"name\":\"Data Solar\",\"owner\":\"Lakshmi\",\"dept\":\"Risk\",\"functions\":[\"Accounting\",\"Data Master\"],\"tco\":1700000,\"process\":\"p.114\"}],\"edges\":[{\"src\":\"app12874\",\"dest\":\"app12875\",\"data-type\":\"security reference\"},{\"src\":\"app12874\",\"dest\":\"app12876\",\"data-type\":\"quotes\"},{\"src\":\"app12875\",\"dest\":\"app12875\",\"data-type\":\"instructions\"},{\"src\":\"app12874\",\"dest\":\"app12872\",\"data-type\":\"instructions\"},{\"src\":\"app12875\",\"dest\":\"app12874\",\"data-type\":\"client master\"},{\"src\":\"app12875\",\"dest\":\"app12874\",\"data-type\":\"allocations\"}],\"node->key\":\"id\",\"node->container\":\"dept\",\"container->parent\":{\"Finance\":\"2LOD\",\"Risk\":\"2LOD\",\"Securities\":\"FO\",\"Equities\":\"FO\",\"FO\":\"Company\",\"2LOD\":\"Company\"},\"node-specs\":[[\"=\",\"dept\",\"Equities\"],{\"label\":{\"key\":\"owner\"}},[\"contains\",\"functions\",\"Accounting\"],{\"style.fill\":\"'#f4a261'\",\"border-radius\":8},\"else\",{\"label\":{\"key\":\"name\"}}],\"edge-specs\":[\"else\",{\"label\":{\"key\":\"data-type\"}}],\"container->attrs\":{\"Securities\":{\"style.fill\":\"'#d6edd5'\"}}}")


(def diag-spec
  {:nodes
   [{:id "app12872",
     :name "Trade pad",
     :owner "Lakshmi",
     :dept "Finance",
     :functions ["Position Keeping" "Quoting"],
     :tco 1200000,
     :process "p.112"}
    {:id "app12873",
     :name "Data Source",
     :owner "India",
     :dept "Securities",
     :functions ["Booking" "Order Mgt"],
     :tco 1100000,
     :process "p.114"}
    {:id "app12874",
     :name "Crypto Bot",
     :owner "Joesph",
     :dept "Equities",
     :functions ["Accounting" "Booking"],
     :tco 500000,
     :process "p.112"}
    {:id "app12875",
     :name "Data Solar",
     :owner "Deepak",
     :dept "Securities",
     :functions ["Position Keeping" "Data Master"],
     :tco 1000000,
     :process "p.114"}
    {:id "app12876",
     :name "Data Solar",
     :owner "Lakshmi",
     :dept "Risk",
     :functions ["Accounting" "Data Master"],
     :tco 1700000,
     :process "p.114"}],
   :edges
   [{:src "app12874",
     :dest "app12875",
     :data-type "security reference"}
    {:src "app12874", :dest "app12876", :data-type "quotes"}
    {:src "app12875", :dest "app12875", :data-type "instructions"}
    {:src "app12874", :dest "app12872", :data-type "instructions"}
    {:src "app12875", :dest "app12874", :data-type "client master"}
    {:src "app12875", :dest "app12874", :data-type "allocations"}],
   :node->key :id,
   :node->container :dept,
   :container->parent
   {"Finance" "2LOD",
    "Risk" "2LOD",
    "Securities" "FO",
    "Equities" "FO",
    "FO" "Company",
    "2LOD" "Company"},
   :node-specs [["=" :dept "Equities"] {:label {:key :owner}}
                ["contains" :functions "Accounting"] {:style.fill "'#f4a261'", :border-radius 8}
                :else {:label {:key :name}}]
   :edge-specs [:else {:label {:key :data-type}}]
   :container->attrs {"Securities" {:style.fill "'#d6edd5'"}}})


(def d2
  "Company:   {\n  FO:   {\n    Securities:   {\n      style.fill: '#d6edd5'\n      app12873: Data Source\n      app12875: Data Solar\n    }\n    Equities:   {\n      app12874: Joesph\n    }\n  }\n  2LOD:   {\n    Finance:   {\n      app12872: Trade pad\n    }\n    Risk:   {\n      app12876:  {\n        style.fill: '#f4a261'\n        border-radius: 8\n      }\n    }\n  }\n}\nCompany.FO.Equities.app12874 -> Company.FO.Securities.app12875: security reference\nCompany.FO.Equities.app12874 -> Company.2LOD.Risk.app12876: quotes\nCompany.FO.Securities.app12875 -> Company.FO.Securities.app12875: instructions\nCompany.FO.Equities.app12874 -> Company.2LOD.Finance.app12872: instructions\nCompany.FO.Securities.app12875 -> Company.FO.Equities.app12874: client master\nCompany.FO.Securities.app12875 -> Company.FO.Equities.app12874: allocations")


(deftest valid-d2
  (testing "I can produce valid d2."
    (is (= (g/graph-spec->d2 diag-spec)
           d2))))

(def diag-spec-2
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
    "Equities" "FO",
    "FO" "Company",
    "2LOD" "Company"},
   "node-specs"
   [["contains" "functions" "Accounting"]
    {"label" {"key" "owner"},
     "style.fill" "'#f4a261'",
     "style.border-radius" 8}
    ["=" "dept" "Equities"]
    {"label" {"key" "owner"}}
    "else"
    {"label" {"key" "name"}}],
   "edge-specs" ["else" {"label" {"key" "data-type"}}],
   "container->attrs" {"Securities" {"style.fill" "'#d6edd5'"}}})


(def d2-2
  "Company:   {\n  FO:   {\n    Securities:   {\n      style.fill: '#d6edd5'\n      app12873: Data Source\n      app12875: Data Solar\n    }\n    Equities:   {\n      app12874: Joesph  {\n        style.fill: '#f4a261'\n        style.border-radius: 8\n      }\n    }\n  }\n  2LOD:   {\n    Finance:   {\n      app12872: Trade pad\n    }\n    Risk:   {\n      app12876: Lakshmi  {\n        style.fill: '#f4a261'\n        style.border-radius: 8\n      }\n    }\n  }\n}\nCompany.FO.Equities.app12874 -> Company.FO.Securities.app12875: security reference\nCompany.FO.Equities.app12874 -> Company.2LOD.Risk.app12876: quotes\nCompany.FO.Securities.app12875 -> Company.FO.Securities.app12875: instructions\nCompany.FO.Equities.app12874 -> Company.2LOD.Finance.app12872: instructions\nCompany.FO.Securities.app12875 -> Company.FO.Equities.app12874: client master\nCompany.FO.Securities.app12875 -> Company.FO.Equities.app12874: allocations")


(deftest valid-d2-dekeywordized
  (testing "I can produce valid d2."
    (is (= (g/graph-spec->d2 diag-spec-2)
           d2-2))))
