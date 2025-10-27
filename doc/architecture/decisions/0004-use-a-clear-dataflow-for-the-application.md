# 4. use a clear dataflow for the application

Date: 2025-09-02

## Status

Accepted

## Context

To analyze a project and visualize the results we have to take care of multiple processing steps. To keep the process 
structured and easy to understand or extend a clear dataflow is necessary.

## Decision

The analysis produces a cg.json file and is executed in the following way:

``` mermaid
graph
    A[Command Line]--1. CLI parameters-->B[Main.kt]
    B --2. Analysis 
    Parameters--> C[AnalysisPipeline]
    C --3. FileReports--> B
    B --4. FileReports--> D[ProcessingPipeline]
    D --5. cg.json with
    ProjectReportDto --> E[File System]
```

After loading a new cg.json file into the visualization, the data is processed in the following way:

``` mermaid
graph
    A[load cg.json file]--ProjectReport-->B[ProjectNodeConverter]
    B --GraphNodes--> C[GraphState]
    C --5. Reduce to new GraphState--> C
    C --1./6. GraphState,
    GraphStateAction--> D[CytoscapeStateService]
    D --4. GraphStateAction
    (Change Request)--> C
    D --2. Updated Cytoscape Elements--> E[Cytoscape instance]
    E --3. User Interaction Event--> D
```
Step explanation:
1. The initial GraphState is passed to the CytoscapeStateService, which initializes the cytoscape instance with the elements from the GraphState.
2. The cytoscape instance is updated with the new elements according to the GraphStateAction. This includes:
    * Adding new nodes or removing nodes no longer visible
    * Layouting the graph
    * Recalculating the edges that need to be displayed according to the edge filter
3. The user interacts with the graph and triggers an event, like expanding or collapsing a node, removing a node from the graph or changing the edge filter
4. A GraphStateAction representing the user interaction gets emitted
5. The GraphState gets changed according to the new GraphStateAction
6. The updated GraphState gets passed to the CytoscapeStateService again together with the GraphStateAction that triggered the change

## Consequences

-