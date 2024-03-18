(require '[babashka.http-client :as http])
(require '[cheshire.core :as json])

(def status
  (->
   (http/post "http://localhost:5001/graph"
              {:headers {:content-type "application/json"
                         "Accept" "image/svg+xml"}
               :body
               (json/generate-string
                {:nodes
                 [{:a "one"}]
                 :node->key :a})})
   :status))

(when-not (= status 200)
  (throw (Exception. "Status is not 200!")))
