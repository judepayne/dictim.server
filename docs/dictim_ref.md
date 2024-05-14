# The dictim api

## API Reference

The dictim syntax - a 'datafied' version of the d2 language is made up of plain Clojure data structures; vectors and maps which very readily translate to json. In the design of the dictim syntax, we've avoided anything in Clojure that doesn't directly translate to json, for example maps (which become json objects) which have keys or maps as their keys.

Therefore, the syntax of dictim in Clojure and the syntax of the 'json-ified' version of dictim is the same; it's just a format change.

The dictim syntax is covered [here](https://github.com/judepayne/dictim/wiki/Dictim-Syntax) and there are further details about its 'json-fied' form on that page.

For dictim templates, see lower down.

### Return values

Successful requests will result into a 200 response with a Content-Type of `image/svg+xml` and the svg of the image in the body.

Unsuccessful requests will result in a 400 response with the error message as the body.

A 401 Internal Server Error generally means that the json sent was invalid.

## Tutorial

A quick example:

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '[
	    {
	      "shape": "sequence_diagram"
	    },
	    [
	      "alice",
	      "->",
	      "bob",
	      "What does it mean?"
	    ],
	    [
	      "bob",
	      "->",
	      "alice",
	      "The ability to play bridge or\ngolf as if they were games."
	    ]
	  ]' \
  http://localhost:5001/dictim/json
```

will produce the svg for this image.

![Example sequence diagram](../images/seq_example.svg)

### For edn

Here's the quivalent on the above, but posting edn rather than json

````bash
curl --header "Content-Type: application/edn" \
  --request POST \
  --data '[{"shape" "sequence_diagram"}
           ["alice" "->" "bob" "What does it mean?"]
           ["bob"
            "->"
            "alice"
            "The ability to play bridge or\ngolf as if they were games."]
           ["comment" "This is a comment"]]' \
  http://localhost:5001/dictim/edn
````


### More complex sequence diagram example


````bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '@test.json' \
  http://localhost:5001/dictim/json
````

will produce the svg for this image.

![Example sequence diagram](../images/seq_example2.svg)

And, of course it's not just sequence diagrams that can be created... All of d2/ dictim is available through this api!


`test.json` contents:

````json
[
  [
    "convs",
    "Office Conversations",
    [
      "conv1",
      "Office conversation 1",
      {
        "shape": "sequence_diagram"
      },
      [
        "list",
        "bob",
        "alice"
      ],
      [
        "alice",
        "Alice",
        {
          "shape": "person",
          "style": {
            "fill": "orange"
          }
        }
      ],
      [
        "bob.\"In the eyes of my (dog), I'm a man.\""
      ],
      [
        "awkward small talk",
        [
          "alice",
          "->",
          "bob",
          "um, hi"
        ],
        [
          "bob",
          "->",
          "alice",
          "oh, hello"
        ],
        [
          "icebreaker attempt",
          [
            "alice",
            "->",
            "bob",
            "what did you have for lunch?"
          ]
        ],
        [
          "fail",
          {
            "style": {
              "fill": "green"
            }
          },
          [
            "bob",
            "->",
            "alice",
            "that's personal"
          ]
        ]
      ]
    ],
    [
      "conv2",
      "Office conversation 2",
      {
        "shape": "sequence_diagram"
      },
      [
        "list",
        "simon",
        "trev"
      ],
      [
        "simon",
        "Simon",
        {
          "shape": "person"
        }
      ],
      [
        "trev",
        "Trevor"
      ],
      [
        "failed conversation",
        [
          "simon",
          "->",
          "trev",
          "seen the football"
        ],
        [
          "trev",
          "->",
          "simon",
          "no, I was at my gran's"
        ],
        [
          "Carry on anyway",
          [
            "simon",
            "->",
            "trev",
            "mate, you missed a classic"
          ]
        ]
      ]
    ],
    [
      "conv1",
      "->",
      "conv2",
      "spot the difference?"
    ]
  ]
]
````

### Templates


Templates are a feature of dictim that allow you to separately pass the 'data part' of the dictim from the 'styling instructions part'. They are covered on the dictim wiki [here](https://github.com/judepayne/dictim/wiki/Template).

In dictim.server a separate route is available for templaes `/dictim-template`. To use this route, post a json object with the dictim, template and optionally directives under separate keys like so..


````bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
  "dictim": [
    [
      "Process View",
      [
        "p113",
        [
          "app14149",
          "Solar Wind"
        ],
        [
          "app14027",
          "Leventine Sky"
        ]
      ],
      [
        "p114",
        [
          "app14181",
          "eBed"
        ],
        [
          "app14029",
          "Storm"
        ]
      ],
      [
        "p113",
        "->",
        "p114",
        "various flows"
      ]
    ]
  ],
  "template": [
    [
      "and",
      [
        "=",
        "element-type",
        "shape"
      ],
      [
        "or",
        [
          "=",
          "key",
          "app14181"
        ],
        [
          "=",
          "key",
          "app14027"
        ]
      ]
    ],
    {
      "class": "lemony"
    },
    [
      "and",
      [
        "=",
        "element-type",
        "shape"
      ]
    ],
    {
      "style": {
        "fill": "aliceblue"
      }
    }
  ],
  "directives": {
    "direction": "right",
    "classes": {
      "lemony": {
        "style": {
          "fill": "lightblue",
          "border-radius": 5
        }
      }
    }
  }
}' \
  http://localhost:5001/dictim-template/json
````

#### Edn templates


````bash
curl --header "Content-Type: application/edn" \
  --request POST \
  --data '{"dictim"
            [["Process View"
              ["p113" ["app14149" "Solar Wind"] ["app14027" "Leventine Sky"]]
              ["p114" ["app14181" "eBed"] ["app14029" "Storm"]]
              ["p113" "->" "p114" "various flows"]]],
           "template"
            [["and"
              ["=" "element-type" "shape"]
              ["or" ["=" "key" "app14181"] ["=" "key" "app14027"]]]
             {"class" "lemony"}
             ["and" ["=" "element-type" "shape"]]
             {"style" {"fill" "aliceblue"}}],
           "directives"
            {"direction" "right",
             "classes"
             {"lemony" {"style" {"fill" "lightblue", "border-radius" 5}}}}}' \
  http://localhost:5001/dictim-template/edn
````