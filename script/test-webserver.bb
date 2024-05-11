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
               "{\"nodes\":[
		     {
		       \"id\":\"app12872\",
		       \"name\":\"Trade pad\",
		       \"owner\":\"Lakshmi\",
		       \"dept\":\"Finance\",
		       \"functions\":[
			 \"Position Keeping\",
			 \"Quoting\"
		       ],
		       \"tco\":1200000,
		       \"process\":\"p.112\"
		     }],
            \"node->key\": \"id\"
		 }"})
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


(when-not
    (=
     (try
        (-> (http/post "http://localhost:5001/conversions/d2-to-dictim"
                         {:headers {:content-type "text/plain"
                                    "Accept" "application/json"}
                          :body
                          "alice ->"})
              :body)
        (catch Exception e (:status (.data e))))
     400)
  (throw (Exception. "bad d2 failed to produce an error")))
