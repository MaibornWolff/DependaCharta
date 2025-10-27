# Cycle Detection Algorithm

## Steps:
1) Find Strongly Connected Components (Clusters) using [Tarjan](https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm)
2) For Each Cluster with more than one node: [Depth-First Search](https://en.wikipedia.org/wiki/Depth-first_search)

### Depth-First Search
1) Iterate through all nodes
   1) Keep track of which nodes and edges we visited
   2) Find all cycles which are reachable from the current node with a depth first search => when visiting a node which is already in the list of visitedNodes we know that we found a cycle
   3) Add traversed nodes to the list of nodes, as all cycles which are reachable from them have been found
#### Restrictions on Depth-First Search
Cycle Detection stops after traversing a certain number of edges.
This is because we think that in highly tangled cluster of nodes, we don't need to find all cycles, by the following reasons:
* most times large cycles contain smaller cycles, which will get detected.
* the most important part of the information is, that the cluster contains any cycles and not how many.

Stopping the cycle detection depends on the number of edges inside a cluster.
* After 10 edges when total number of edges < 100
* After 8 edges when total number of edges < 200
* After 6 edges when total number of edges < 400
* After 4 edges in every other case