# LTSVisualiser
This tool allows visualising a graph encoded as a `.aut` file, providing indicatiors for the graph structure and real-time complexity reduction techniques.
The tool is effective for graphs up to 100.000 edges.

## Graph Controls
Assuming a graph is loaded, the camera can be rotated using both the right mouse button and the middle mouse button.
While the right mouse button has other uses as well, not all mouses with a scroll wheel support clicking it.
The camera can be moved by holding the shift-button, and dragging the right button.
Note that there is a button in the UI that centers the camera on specific nodes.
The camera distance can be changed by scrolling. 

### Graph manipulation
Nodes can be manually moved using the right mouse button, and be anchored in place using the left mouse button.
Clicking with left while holding the right button is supported, the node will be anchored where it is held by the mouse.
Edges can not be manipulated.

### Colors
The initial state is indicated with a green border and a green fill.
Deadlock states are indicated with a red border.
Anchored nodes are filled grey.

### Hovering graph elements
When an edge is hovered, all edges with the same label as this edge will be colored blue.
The label of the hovered element will be shown below the mouse pointer.
Left-clicking an edge will toggle its corresponding button in the _Action labels_ list, as we will discuss later.

When a node is hovered, all nodes that are confluent to this node will be colored blue.
This logically only applies for graphs with tau-transitions.

## User Interface
Graphs are loaded using the _Load Graph_ button.

### Load mu-formula
The tool also allows loading a property as a modal mu-formula, which applies a coloring on the graph.
In short, any edge that makes the property unreachable is colored red.
Any edge where the property becomes unavoidable is colored green.
The small `X` button resets the colors set by this function.

This function _does_ work in combination with clustering.

### Label indicator
The box below the three load buttons shows the size of the currently loaded primary graph, and the name of the graph element that is hovered.

### Sliders
The sliders below that control the force-directed layout.

The *Simulation step size* gives a trade-off between convergence speed and stability.
If any part of the graph starts vibrating, the simulation step size should be reduced until the vibration disappears.

*Attraction* controls the strength with which edges pull their connected nodes together.

*Repulsion* controls the strength with which all nodes repel each other.

*Natural length* applies a base distance modifier to the algorithm, which influences the scale of the graph.

*Handle repulsion* controls the strength on which edges repel each other. 
This repulsion only applies to edges connected to the same nodes, and is mostly effective to split overlapping edges. 
These four sliders may be set to you own preference for this survey.

The *Heuristic Effect* allows the introduction of an heuristic to the forces computation.
A high value reduces the time of each iteration, but also makes the force calculation less accurate.
This is hardly a problem for our application, and a value of 1.00 results in a sufficiently accurate stable situation.
This value is set automatically for large graphs, and there is no need to change it manually.

### Action labels
This is an alphabetically sorted list of all action labels in the graph.
When clicked on the name of the action, the action in the graph is colored according to the current color.

Each action has an `I` button, to Ignore / Internalize the action.
This marks the action as internal, and activates clustering on confluence.

Each action has an `C` button, to Cluster / Collapse all edges of this action.
When collapsing an edge, the two nodes it connects are combined into one node.

### Display Options will pop up a window with some additional options.
The first option allows you to set the edge representation to different shapes.

The *3D view* allows toggling between a 2D and 3D visualisation.
The graph is always flattened in the direction of the camera.

### Graph painting
The tool allows you to paint any graph element one of the preset colors, as long as _Activate Painting_ is enabled.
The color of a single edge can be set by left-clicking, and reset by right-clicking.
All colors can be reset using the small `X` button.

*Always use layout of primary graph* can be toggled on to make the force directed layout only affect the primary graph, rather than the graph that is currently visualized.
Enabling this allows you to switch between the different visualisations without the graph changing its layout, but also results in less optimal clustering results.

Additionally, there are two buttons to write the current performance measurements to the log files.
This is also done automatically for this survey, and do not have to be used.
