;; A babashka script to call the webserver and check status is 200
;; a babashka script to test the webserver
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


(when-not
    (=
     (-> (http/post "http://localhost:5001/conversions/dictim-to-d2"
                    {:headers {:content-type "application/json"
                               "Accept" "text/plain"}
                     :body
                     (json/generate-string
                      '({:shape "sequence_diagram"}
                        ["alice" "->" "bob" "What does it mean?"]
                        ["bob" "->" "alice" "The ability to play bridge or\ngolf as if they were games."]
                        [:comment "This is a comment"]))})
         :body)
     "shape: sequence_diagram
alice -> bob: What does it mean?
bob -> alice: The ability to play bridge or
golf as if they were games.
# This is a comment")
  (throw (Exception. "Failed to convert dictim to d2!")))


(when-not
    (= (->
        (http/post "http://localhost:5001/dictim"
                   {:headers {:content-type "application/json"
                              "Accept" "image/svg+xml"}
                    :body
                    (json/generate-string
                     '({:shape "sequence_diagram"}
                       ["alice" "->" "bob" "What does it mean?"]
                       ["bob" "->" "alice" "The ability to play bridge of\ngolf as if they were games."]
                       [:comment "This is a comment"]))})
        :status)
       200)
  (throw (Exception. "Failed to convert dictim to svg!")))


(when-not
 (=
  (-> (http/post "http://localhost:5001/conversions/d2-to-dictim"
                 {:headers {:content-type "text/plain"
                            "Accept" "application/json"}
                  :body
                  "shape: sequence_diagram
                     alice -> bob: What does it mean?
                     bob -> alice: The ability to play bridge or golf as if they were games.
                     # This is a comment"})
      :body)
  "[{\"shape\":\"sequence_diagram\"},[\"alice\",\"->\",\"bob\",\"What does it mean?\"],[\"bob\",\"->\",\"alice\",\"The ability to play bridge or golf as if they were games.\"],[\"comment\",\"This is a comment\"]]")
  (throw (Exception. "Failed to convert d2 to dictim")))
