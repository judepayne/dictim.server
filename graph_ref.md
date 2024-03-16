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



## Tutorial


Let's now build this example diagram one step at a time, by hand. This is of course not normally how you'd do it; rather you'd have code that generates the json diagram spec from data you have and pass the spec to dictim.server. Building by hand though is a good way to understand what all the keys in the diagram spec do.


![](images/graphtutorial1.svg)







