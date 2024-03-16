# The Graph API

## API reference

As the name suggests this api models the boxes and arrows as a graph, where the boxes are nodes and the arrows edges.

You should send a POST request to the `/graph` route with a json map with the following keys.

*Manatory keys:*

| key | expanation |
| --- | ---------- |
| `"nodes"` | a vector of maps, each map being a representation of one node. The maps don't have to be homogenous (i.e. have the same keys) but often are if you representing multiple instances of the same type of thing. |
| `"edges"` | a vector of maps, each map being a representation of one edge. Each edge must have `"src"` and `"dest"` keys and any other keys you need to model the edge. Again, they don't need to be homogenous but often are. the `"src"` and `"dest:"` keys are how you tie edges to nodes. The value of each of these keys must be the value of one of (the same one across all nodes) keys in the maps representing the nodes. The pointer to that (node) key is.. |
| `"node->key"` | this indicates the key to use in node maps to uniquely represent that node. The value returned must be unique for each node. Often this could be some sort of `"id"` key. |

*Optional keys:*

| key | expanation |
| --- | ---------- |
| `"node-specs"` & `"edge-specs"` | map nodes and edges to the visual styles that should be used to display that node or edge. The values of these two keys are a mini dsl (domain specific language) that requires further explanation. Please see below for more details. |
| `"node->container"` | this indicates the use to use in node maps to map a node to its containing container (if it has/ needs one). |
| `"container->parent"` | this should be a map of containers to their parent container (if needed) and is how you create hierarchies of containers within containers in the diagram. |
| `"container->attrs"` | a map of containers to the visual styles that should be used to display that container. |

An example of container->attrs:

```json
  "container->attrs": {
    "Securities": {
      "style.fill": "green"
    }
  }
```

is used to map a single container "Securities" to styling instructions that indicates that the fill color of that container should be green. Please see [this page](https://d2lang.com/tour/style) for details on styling options. You can also add a `"label"` entry to this map, if you wish for the container to be labelled differently to its name.


### node-specs, edge-specs mini dsl

It would be powerful to be able to conditionally style nodes and edges depending on their content. That's what the mini dsl for these two keys allows you to do!


Here's the overall form of either the node->attrs or edge->attrs entry; both are the same:

```json
{"labels": ..label-specs..,
 "styles": ..style-specs..}
```

Both the "labels" and "styles" keys are optional.

label-sepcs and style-specs are very similar both not exactly the same. Let's look at the anatomy of an example label-specs and then explain the differences for a style spec.

#### label specs

Here's an example label specs collection..

```json
[[{"key":"owner"},["equals","dept","Equities"]],[{"key":"name"}]]
```
a vector of individual label specs, where each spec is made of just one part - a label instruction or two parts - a label instruction and a condition. Let's look at instructions first. From the example, this label spec has no conditional part, just a label instruction which tells dictim.server how to build the label from each node or edge.

```json
[{"key":"name"}]
```
When there's no condition in a label spec, it's like the `else` clause in an `if .. if-else .. if-else .. else` statement. Only one one part/ non conditional label spec is allowed in each label specs collections.

In the above case, the instruction tells dictim.server to use the value of the "name" key for each node or edge as the label.
This is the simplest type of label instruction. There are two other forms..

Rather than a single key, we can replace with a vector of keys, e.g.:

```json
[{"key":["name","owner"]}]
```

This instruction would tell dictim.server to create a label by combining the values found under the "name" and "owner" keys (and splicing them together with `\newlines`).

Finally an instruction can be specified as a vector of maps, e.g.:

```json
[{"key":"name"},{"key":"owner"}]
```

which accomplishes the same as the vector of keys.

Let's look at conditions. In the example, the first label spec has the following condition:

```json
["equals","dept","Equities"]
```

Each condition has three parts; the comparator, the key (in the node or edge) and the value (to be compared to).
Possible comparators are:


```code
"equals"
"not-equals"
"contains"
"doesnt-contain"
">"
"<"
">="
"<="
```

`"contains"` and `"doesnt-contain"` can be used when the value is a collection (a sequence or set). The greater-than/ less-than comparators can be used when the value is numeric.

Conditions can be combined with an "and" or an "or". For example `["and" [">","tco",1000000] ["equals","dept","Equities"]]` is a valid condition, but conditions cannot currently be arbitrarily nested, so `["or" ["equals","owner,"Joe"] ["and" [">","tco",1000000] ["equals","dept","Equities"]]]` is not valid.

Style specs differ from label specs only in the detail of the instruction. A style spec is a simple map of d2 styling attributes and values, which are described [here](https://d2lang.com/tour/style).

To sum up, the format of a spec collection is:

```code
[[*else-instruction*][*instruction* *condition*][*instruction* *condition*]...]
```

where only one *else-instruction* is allowed. The order of the specs determines in which each is attempted to be matched against a node or label, but the *else-instruction* can be placed anywhere since it is automatically moved to the end before matching.


### Return values

Successful requests will result into a 200 response with a Content-Type of `image/svg+xml` and the svg of the image in the body.

Unsuccessful requests will result in a 400 response with the error message as the body.

A 401 Internal Server Error generally means that the json sent was invalid.



## Tutorial


Let's now build this example diagram one step at a time, by hand. This is of course not normally how you'd do it; rather you'd have code that generates the json diagram spec from data you have and pass the spec to dictim.server. Building by hand though is a good way to understand what all the keys in the diagram spec do.


<img src="images/graphtutorial1.svg" width="850">


is an architecture diagram that was built from this diagram spec..

<details><summary>Full diagram spec</summary>

```json
{
  "nodes":[
	    {
	      "id":"app12872",
	      "name":"Trade pad",
	      "owner":"Lakshmi",
	      "dept":"Finance",
	      "functions":[
		"Position Keeping",
		"Quoting"
	      ],
	      "tco":1200000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12873",
	      "name":"Data Source",
	      "owner":"India",
	      "dept":"Securities",
	      "functions":[
		"Booking",
		"Order Mgt"
	      ],
	      "tco":1100000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12874",
	      "name":"Crypto Bot",
	      "owner":"Joesph",
	      "dept":"Equities",
	      "functions":[
		"Accounting",
		"Booking"
	      ],
	      "tco":500000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12875",
	      "name":"Data Solar",
	      "owner":"Deepak",
	      "dept":"Securities",
	      "functions":[
		"Position Keeping",
		"Data Master"
	      ],
	      "tco":1000000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12876",
	      "name":"Data Solar",
	      "owner":"Lakshmi",
	      "dept":"Risk",
	      "functions":[
		"Accounting",
		"Data Master"
	      ],
	      "tco":1700000,
	      "process":"p.114"
	    }
	  ],
	  "edges":[
	    {
	      "src":"app12874",
	      "dest":"app12875",
	      "data-type":"security reference"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12876",
	      "data-type":"quotes"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12875",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12872",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"client master"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"allocations"
	    }
	  ],
	  "node->key":"id",
	  "node->container":"dept",
	  "container->parent":{
	    "Finance":"2LOD",
	    "Risk":"2LOD",
	    "Securities":"FO",
	    "Equities":"FO",
	    "FO":"Company",
	    "2LOD":"Company"
	  },
	  "node-specs":{
	    "labels":[
	      [
		{
		  "key":"owner"
		},
		[
		  "equals",
		  "dept",
		  "Equities"
		]
	      ],
	      [
		{
		  "key":"name"
		}
	      ]
	    ]
	  },
	  "edge-specs":{
	    "labels":[
	      [
		{
		  "key":"data-type"
		}
	      ]
	    ]
	  },
	  "container->attrs":{
	    "Securities":{
	      "style.fill":"'#d6edd5'"
	    }
	  }
	}
```
</details>


The `"nodes"` key is a vector of nodes, each one being an arbitrary map of keys and values. Let look at one.

```json
{
  "id":"app12872",
  "name":"Trade pad",
  "owner":"Lakshmi",
  "dept":"Finance",
  "functions":["Position Keeping", "Quoting"],
  "tco":1200000,
  "process":"p.112"
}
```

Each node describes a box in the diagram. It's helpful to put more facts rather than less about the concept represented by the box; even some of those facts are not immediately represented in the diagram (e.g. the label of the box), because we're working with dynamic diagrams, we might choose to use one of those facts later e.g. in the label or to determine some styling aspect of how the box is represented.

We can make a diagram out of just this one node. Let's talk to dictim server with `curl`

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"nodes":[
		     {
		       "id":"app12872",
		       "name":"Trade pad",
		       "owner":"Lakshmi",
		       "dept":"Finance",
		       "functions":[
			 "Position Keeping",
			 "Quoting"
		       ],
		       "tco":1200000,
		       "process":"p.112"
		     }],
            "node->key": "id"
		 }' \
  http://localhost:5001/graph > out.svg
```

and have the result written to a file called `out.svg`


<img src="images/graphtutorial2.svg" width="400">


As well as the `"node"` itself, we must have a `"node->key"` entry in the diagram spec or dictim.server will return an error.

The `"node->key"` entry serves two purposes:
- specifies the default label if no other label is specified by a `"node-specs"` entry.
- uniquely identifies the node, which we'll need later when adding edges that have a source and destination.

> Each node in a diagram spec must have one key in it's map which is unique.


Let's now add in a second node and an edge between the two.

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"nodes":[
		     {
		       "id":"app12872",
		       "name":"Trade pad",
		       "owner":"Lakshmi",
		       "dept":"Finance",
		       "functions":[
			 "Position Keeping",
			 "Quoting"
		       ],
		       "tco":1200000,
		       "process":"p.112"
		     },
		     {
		       "id":"app12874",
		       "name":"Crypto Bot",
		       "owner":"Joesph",
		       "dept":"Equities",
		       "functions":[
			 "Accounting",
			 "Booking"
		       ],
		       "tco":500000,
		       "process":"p.112"
		     }],
	    "edges":[
	              {
			"src":"app12874",
			"dest":"app12872",
			"data-type":"instructions"
		      }],
	    "node->key": "id",
	    "node-specs":{
			   "labels":[
				      [
					{
					  "key":"name"
					}
				      ]
				    ]
			 }
		 }' \
  http://localhost:5001/graph > out.svg
```

Each `"edge"` is also just a map. Unlike a `"node"` there are two keys that must always be present; `"src"` and `"dest"`, the source and destination of the edge. The value of these two keys must refer to the unique value of a node that we just talked about.

Beyond those two keys, the other entries making up an `"edge"` is arbitrary and can be as many as you want. As for `"nodes"` it's worth including more entries in each edge in case you need to use them later in your dynamic, evolving diagram, but in this example we've just added one `"data-type"` which represents the type of data flowing between two applications in our architecture diagram.

We've also added a `"node-specs"` entry into the diagram spec, and inside it a simple unconditional label instruction, that tells dictim.server to use the `"name"` key's value from each node as the label of that node.

Here's the diagram now.

<img src="images/graphtutorial3.svg" width="400">

 Let's add back in the other nodes, edges and an edge-spec that controls the labels used for the edges.

The edge-spec:

```json
"edge-specs":{
  "labels":[
    [
      {
	"key":"data-type"
      }
    ]
  ]
}
```

<details>
<summary>Full curl command</summary>

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
  "nodes":[
	    {
	      "id":"app12872",
	      "name":"Trade pad",
	      "owner":"Lakshmi",
	      "dept":"Finance",
	      "functions":[
		"Position Keeping",
		"Quoting"
	      ],
	      "tco":1200000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12873",
	      "name":"Data Source",
	      "owner":"India",
	      "dept":"Securities",
	      "functions":[
		"Booking",
		"Order Mgt"
	      ],
	      "tco":1100000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12874",
	      "name":"Crypto Bot",
	      "owner":"Joesph",
	      "dept":"Equities",
	      "functions":[
		"Accounting",
		"Booking"
	      ],
	      "tco":500000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12875",
	      "name":"Data Solar",
	      "owner":"Deepak",
	      "dept":"Securities",
	      "functions":[
		"Position Keeping",
		"Data Master"
	      ],
	      "tco":1000000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12876",
	      "name":"Data Solar",
	      "owner":"Lakshmi",
	      "dept":"Risk",
	      "functions":[
		"Accounting",
		"Data Master"
	      ],
	      "tco":1700000,
	      "process":"p.114"
	    }
	  ],
	  "edges":[
	    {
	      "src":"app12874",
	      "dest":"app12875",
	      "data-type":"security reference"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12876",
	      "data-type":"quotes"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12875",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12872",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"client master"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"allocations"
	    }
	  ],
	  "node->key":"id",
	  "node-specs":{
	    "labels":[
	      [
		{
		  "key":"owner"
		},
		[
		  "equals",
		  "dept",
		  "Equities"
		]
	      ],
	      [
		{
		  "key":"name"
		}
	      ]
	    ]
	  },
	  "edge-specs":{
	    "labels":[
	      [
		{
		  "key":"data-type"
		}
	      ]
	    ]
	  }
	}' \
  http://localhost:5001/graph > out.svg
```
</details>


This is the diagram now

<img src="images/graphtutorial4.svg" width="850">

Notice that there's no node labelled "Crypto Bot" any longer. That's because we also added in a second *conditional* label instruction into the `"node-specs"`

```json
[
  {
    "key":"owner"
   },
   [
     "equals",
     "dept",
     "Equities"
   ]
]
```

The condition part of this label instructions tell dictim.server that this instruction applies only to nodes where the value of the `"dept"` key is equals to "Equities", and when that is true to use the `"owner"` key from the node map rather than the default "name".

All that's left to do now is put the container instructions back into the diagram spec that handle how nodes are positioned into containers, how those containers are themselves positioned inside other containers, and how all the containers should be styled.


All this is handled with 3 additional keys. The first is `"node->container"` which indicates which key in each node is used to group the nodes into containers.

```json
	  "node->container":"dept",
```

The next is `"container->parent"` which provides the hierarchy of containers by mapping each container to its parent container.

```json
	  "container->parent":{
	    "Finance":"2LOD",
	    "Risk":"2LOD",
	    "Securities":"FO",
	    "Equities":"FO",
	    "FO":"Company",
	    "2LOD":"Company"
	  },
```

and finally there is `"container->attrs"` which is a map of containers to their styles.

```json
	  "container->attrs":{
	    "Securities":{
	      "style.fill":"'#d6edd5'"
	    }
	  }
```
Please note that in dictim, hex colors (e.g. '#d6edd5') must be single quoted because d2 interprets `#` as a special character indicating a comment.

Now we're back at the original diagram!

<details>
<summary>Full curl command</summary>
```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
  "nodes":[
	    {
	      "id":"app12872",
	      "name":"Trade pad",
	      "owner":"Lakshmi",
	      "dept":"Finance",
	      "functions":[
		"Position Keeping",
		"Quoting"
	      ],
	      "tco":1200000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12873",
	      "name":"Data Source",
	      "owner":"India",
	      "dept":"Securities",
	      "functions":[
		"Booking",
		"Order Mgt"
	      ],
	      "tco":1100000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12874",
	      "name":"Crypto Bot",
	      "owner":"Joesph",
	      "dept":"Equities",
	      "functions":[
		"Accounting",
		"Booking"
	      ],
	      "tco":500000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12875",
	      "name":"Data Solar",
	      "owner":"Deepak",
	      "dept":"Securities",
	      "functions":[
		"Position Keeping",
		"Data Master"
	      ],
	      "tco":1000000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12876",
	      "name":"Data Solar",
	      "owner":"Lakshmi",
	      "dept":"Risk",
	      "functions":[
		"Accounting",
		"Data Master"
	      ],
	      "tco":1700000,
	      "process":"p.114"
	    }
	  ],
	  "edges":[
	    {
	      "src":"app12874",
	      "dest":"app12875",
	      "data-type":"security reference"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12876",
	      "data-type":"quotes"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12875",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12872",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"client master"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"allocations"
	    }
	  ],
	  "node->key":"id",
	  "node->container":"dept",
	  "container->parent":{
	    "Finance":"2LOD",
	    "Risk":"2LOD",
	    "Securities":"FO",
	    "Equities":"FO",
	    "FO":"Company",
	    "2LOD":"Company"
	  },
	  "node-specs":{
	    "labels":[
	      [
		{
		  "key":"owner"
		},
		[
		  "equals",
		  "dept",
		  "Equities"
		]
	      ],
	      [
		{
		  "key":"name"
		}
	      ]
	    ]
	  },
	  "edge-specs":{
	    "labels":[
	      [
		{
		  "key":"data-type"
		}
	      ]
	    ]
	  },
	  "container->attrs":{
	    "Securities":{
	      "style.fill":"'#d6edd5'"
	    }
	  }
	}' \
  http://localhost:5001/graph > out.svg
```
</details>


As a bonus and final step, let's add a conditional styling instruction under `"node-specs"` which styles nodes (application in our example) that contain the function 'Accounting' with a different fill and border radius.

```json
"styles":[
      [
        {
          "fill":"'#f4a261'",
          "border-radius":8
	  
        },
        [
          "contains",
          "functions",
          "Accounting"
        ]
      ]
    ]

```

<img src="images/graphtutorial5.svg" width="850">

<details>
<summary>Full curl command</summary>
```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
  "nodes":[
	    {
	      "id":"app12872",
	      "name":"Trade pad",
	      "owner":"Lakshmi",
	      "dept":"Finance",
	      "functions":[
		"Position Keeping",
		"Quoting"
	      ],
	      "tco":1200000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12873",
	      "name":"Data Source",
	      "owner":"India",
	      "dept":"Securities",
	      "functions":[
		"Booking",
		"Order Mgt"
	      ],
	      "tco":1100000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12874",
	      "name":"Crypto Bot",
	      "owner":"Joesph",
	      "dept":"Equities",
	      "functions":[
		"Accounting",
		"Booking"
	      ],
	      "tco":500000,
	      "process":"p.112"
	    },
	    {
	      "id":"app12875",
	      "name":"Data Solar",
	      "owner":"Deepak",
	      "dept":"Securities",
	      "functions":[
		"Position Keeping",
		"Data Master"
	      ],
	      "tco":1000000,
	      "process":"p.114"
	    },
	    {
	      "id":"app12876",
	      "name":"Data Solar",
	      "owner":"Lakshmi",
	      "dept":"Risk",
	      "functions":[
		"Accounting",
		"Data Master"
	      ],
	      "tco":1700000,
	      "process":"p.114"
	    }
	  ],
	  "edges":[
	    {
	      "src":"app12874",
	      "dest":"app12875",
	      "data-type":"security reference"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12876",
	      "data-type":"quotes"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12875",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12874",
	      "dest":"app12872",
	      "data-type":"instructions"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"client master"
	    },
	    {
	      "src":"app12875",
	      "dest":"app12874",
	      "data-type":"allocations"
	    }
	  ],
	  "node->key":"id",
	  "node->container":"dept",
	  "container->parent":{
	    "Finance":"2LOD",
	    "Risk":"2LOD",
	    "Securities":"FO",
	    "Equities":"FO",
	    "FO":"Company",
	    "2LOD":"Company"
	  },
	  "node-specs":{
	    "labels":[
	      [
		{
		  "key":"owner"
		},
		[
		  "equals",
		  "dept",
		  "Equities"
		]
	      ],
	      [
		{
		  "key":"name"
		}
	      ]
	    ],
	    "styles":[
	      [
		{
		  "fill":"'#f4a261'",
		  "border-radius":8
		},
		[
		  "contains",
		  "functions",
		  "Accounting"
		]
	      ]
	    ]
	  },
	  "edge-specs":{
	    "labels":[
	      [
		{
		  "key":"data-type"
		}
	      ]
	    ]
	  },
	  "container->attrs":{
	    "Securities":{
	      "style.fill":"'#d6edd5'"
	    }
	  }
	}' \
  http://localhost:5001/graph > out.svg
```bash
</details>