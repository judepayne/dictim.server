# The dictim api

## API Reference

The dictim syntax is covered [here](https://github.com/judepayne/dictim/wiki/Dictim-Syntax) under the dictim syntax.

The key difference with dictim.server to dictim is that dictim.server accepts the json-ic form of dictim.

You should send a POST request to the `/dictim` route with your dictim.

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
