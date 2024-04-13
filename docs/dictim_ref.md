# The dictim api

## API Reference

The dictim syntax - a 'datafied' version of the d2 language is made up of plain Clojure data structures; vectors and maps which very readily translate to json. In the design of the dictim syntax, we've avoided anything in Clojure that doesn't directly translate to json, for example maps (which become json objects) which have keys or maps as their keys.

Therefore, the syntax of dictim in Clojure and the syntax of the 'json-ified' version of dictim is the same; it's just a format change.

The dictim syntax is covered [here](https://github.com/judepayne/dictim/wiki/Dictim-Syntax) and there are further details about its 'json-fied' form on that page.

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
  http://localhost:5001/dictim
```

will produce the svg for this image.

![Example sequence diagram](images/seq_example.svg)


A more complex sequence diagram example


````bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '@test.json' \
  http://localhost:5001/dictim
````

will produce the svg for this image.

![Example sequence diagram](images/seq_example2.svg)

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