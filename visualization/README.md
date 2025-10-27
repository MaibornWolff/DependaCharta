# Visualization
## Standalone
Download the correct standalone version for your OS from [the latest release page](https://NEW-GITHUB-URL/-/releases). 
### MAC OS
Extract, navigate into the extracted folder where the .app file is located in & run this command: <code>xattr -cr dependacharta-visualization.app</code> then run the application.
### WINDOWS
Extract & run the application.
## Installation
### Build it yourself
Prerequisites: Git and Node.js installed
- Clone the repository
- Navigation into visualization
- Run <code>npm i</code>
#### Run it in your browser
- Run <code>npm run start</code>
- The visualization now runs on localhost:4200 where you can choose which cg.json you want to load
#### Run it as an electron app
- Run <code>npm run start-electron</code>
- A window will open that shows the visualization

## Usage
- Packages are displayed in green, Files/Classes are displayed in black.
- If you see a package, you can expand it with a left click and also collapse it again with a left click
- By hovering on a node, you highlight all connected Edges and direct neighbors
- Right-clicking a node hides it
- To show all hidden nodes again you can use the 'Restore Nodes'-button
- For the edges there can be a label with a number, which describes the number of (leaf-to-leaf) edges which are aggregated to that visible edge (feature can be toggled with 'Toggle Edge Labels'-button at the top)

### Edge Filters
You can choose from the following filters to limit which edges are displayed:
- **Show only feedback edges (default)**: Only show the edges which are part of a cycle but also travel from a lower level to a higher level
- Show feedback and twisted edges: Only show all edges which travel from a lower level to a higher level (includes feedback edges which are a red solid line, and non-cyclic twisted edges which are a red dotted line)
- Show only cycles: Only show edges which are part of a cycle. Regular cyclic edges are colored blue, while feedback edges are colored red
- Show all edges: Shows all edges. Regular edges are colored gray, while cyclic edges are colored blue, feedback edges are colored red as a solid line and twisted edges are colored red as a dotted line
- Show no edges: Hides all edges

## Directory Strucure and Dataflow
See [ADR Log](../doc/architecture/decisions)
