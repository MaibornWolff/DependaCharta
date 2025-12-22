# Visualization
## Standalone
Download the correct standalone version for your OS from [the latest release page](https://github.com/MaibornWolff/DependaCharta/releases). 
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
- **Show all feedback edges (default)**: Show all edges which travel from a lower level to a higher level (includes leaf level feedback edges which are red solid lines, and container level feedback edges which are red dotted lines)
- Show only leaf level feedback edges: Only show the edges which are part of a cycle but also travel from a lower level to a higher level
- Show only cycles: Only show edges which are part of a cycle. Regular cyclic edges are colored blue, while leaf level feedback edges are colored red
- Show all edges: Shows all edges. Regular edges are colored gray, cyclic edges are colored blue, leaf level feedback edges are colored red as solid lines, and container level feedback edges are colored red as dotted lines
- Show no edges: Hides all edges

## Directory Strucure and Dataflow
See [ADR Log](../doc/architecture/decisions)
