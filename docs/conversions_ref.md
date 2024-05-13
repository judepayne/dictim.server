# The Conversions api

## API Reference

### dictim to d2


Let's convert json-fied dictim into d2

````bash
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
            ],
	    [
	    "comment",
	    "This is a comment"
	    ]
          ]' \
  http://localhost:5001/conversions/dictim-to-d2/json
````

results in an `"text/plain"` response of

````bash
shape: sequence_diagram
alice -> bob: What does it mean?
bob -> alice: The ability to play bridge or
golf as if they were games.
# This is a comment
````

For `application/edn`, use the `dictim-to-d2/edn` route instead


#### Return Values

Successful requests will result into a 200 response with a Content-Type of `text/plain` and the d2 equivalent of the posted dictim.

Unsuccessful requests will result in a 400 response with the error message as the body.

A 401 Internal Server Error generally means that the dictim sent was invalid (json).


### d2 to dictim

Let's post a d2 string that we want to have converted to (json-fied) dictim

````bash
curl --header "Content-Type: text/plain" \
  --request POST \
  --data 'hello: jude
          x -> y: a connection' \
  http://localhost:5001/conversions/d2-to-dictim
````

results in an `"application/json"` response of

````json
[["hello","jude"],["x","->","y","a connection"]]
````


#### Return Values


Successful requests will result into a 200 response with a Content-Type of `application/json` and the json-fied dictim equivalent of the d2 posted in the request.

Unsuccessful requests will result in a 400 response with the error message as the body.

A 401 Internal Server Error generally means that the d2 sent was invalid, or empty.