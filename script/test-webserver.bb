;; a babashka script to test the webserver
(require '[babashka.http-client :as http])
(require '[cheshire.core :as json])


;; test /graph/json
(when-not
    (=
     (-> (http/post "http://localhost:5001/graph/json"
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
         :status)
     200)
    (throw (Exception. "graph/json test status is not 200!")))


;; test /graph/edn
(when-not
    (=
     (-> (http/post "http://localhost:5001/graph/edn"
                    {:headers {:content-type "application/edn"
                               "Accept" "image/svg+xml"}
                     :body
                     "{\"nodes\" [{:id \"app12872\", :name \"Trade pad\", :owner \"Lakshmi\", :dept \"Finance\", :functions [\"Position Keeping\" \"Quoting\"], :tco 1200000, :process \"p.112\"}], \"node->key\" :id}"})
         :status)
     200)
    (throw (Exception. "/graph/edn test status is not 200!")))


;; test /conversions/dictim-to-d2/json
(when-not
    (=
     (-> (http/post "http://localhost:5001/conversions/dictim-to-d2/json"
                    {:headers {:content-type "application/json"
                               "Accept" "text/plain"}
                     :body
                     (json/generate-string
                      '({:shape "sequence_diagram"}
                        ["alice" "->" "bob" "What does it mean?"]
                        ["bob" "->" "alice" "The ability to play bridge or\ngolf as if they were games."]
                        ["# This is a comment"]))})
         :body)
     "shape: sequence_diagram
alice -> bob: What does it mean?
bob -> alice: The ability to play bridge or
golf as if they were games.
# This is a comment")
    (throw (Exception. "/dictim-to-d2/json failed to convert dictim to d2!")))


;; test /conversions/dictim-to-d2/edn
(when-not
    (=
     (-> (http/post "http://localhost:5001/conversions/dictim-to-d2/edn"
                    {:headers {:content-type "application/edn"
                               "Accept" "text/plain"}
                     :body
                     (pr-str
                      '({:shape "sequence_diagram"}
                        ["alice" "->" "bob" "What does it mean?"]
                        ["bob" "->" "alice" "The ability to play bridge or\ngolf as if they were games."]
                        ["# This is a comment"]))})
         :body)
     "shape: sequence_diagram\nalice -> bob: What does it mean?\nbob -> alice: The ability to play bridge or\ngolf as if they were games.\n# This is a comment")
    (throw (Exception. "/dictim-to-d2/edn failed to convert dictim to d2!")))


;; test /d2-to-dictim/json
(when-not
    (=
     (-> (http/post "http://localhost:5001/conversions/d2-to-dictim/json"
                    {:headers {:content-type "text/plain"
                               "Accept" "application/json"}
                     :body
                     "shape: sequence_diagram
                     alice -> bob: What does it mean?
                     bob -> alice: The ability to play bridge or golf as if they were games.
                     # This is a comment"})
         :body)
     "[{\"shape\":\"sequence_diagram\"},[\"alice\",\"->\",\"bob\",\"What does it mean?\"],[\"bob\",\"->\",\"alice\",\"The ability to play bridge or golf as if they were games.\"],\"# This is a comment\"]")
    (throw (Exception. "/d2-to-dictim/json failed to convert d2 to dictim")))


;; test /d2-to-dictim/edn
(when-not
    (=
     (-> (http/post "http://localhost:5001/conversions/d2-to-dictim/edn"
                    {:headers {:content-type "text/plain"
                               "Accept" "application/edn "}
                     :body
                     "shape: sequence_diagram
                     alice -> bob: What does it mean?
                     bob -> alice: The ability to play bridge or golf as if they were games.
                     # This is a comment"})
         :body
         clojure.edn/read-string)
     '({"shape" "sequence_diagram"}
 ["alice" "->" "bob" "What does it mean?"]
 ["bob"
  "->"
  "alice"
  "The ability to play bridge or golf as if they were games."]
 "# This is a comment"))
    (throw (Exception. "/d2-to-dictim/edn failed to convert d2 to dictim")))


;; test /d2-to-dictim/json
(when-not
    (=
     (try
       (-> (http/post "http://localhost:5001/conversions/d2-to-dictim/json"
                      {:headers {:content-type "text/plain"
                                 "Accept" "application/json"}
                       :body
                       "alice ->"})
           :body)
       (catch Exception e (:status (.data e))))
     400)
    (throw (Exception. "/d2-to-dictim/json bad d2 failed to produce an error")))


;; test /dictim/json
(when-not
    (= (->
        (http/post "http://localhost:5001/dictim/json"
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
    (throw (Exception. "/dictim/json failed to convert dictim to svg!")))


;; test /dictim/edn
(when-not
    (= (->
        (http/post "http://localhost:5001/dictim/edn"
                   {:headers {:content-type "application/edn"
                              "Accept" "image/svg+xml"}
                    :body
                    (pr-str
                     '({:shape "sequence_diagram"}
                       ["alice" "->" "bob" "What does it mean?"]
                       ["bob" "->" "alice" "The ability to play bridge of\ngolf as if they were games."]
                       [:comment "This is a comment"]))})
        :status)
       200)
    (throw (Exception. "/dictim/edn failed to convert dictim to svg!")))
