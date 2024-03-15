
### The dictim api

The dictim api accepts the json-ic form of dictim which is covered [here](https://github.com/judepayne/dictim/wiki/Dictim-Syntax).

You should send a POST request to the `/dictim` route with your dictim.

For example

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
