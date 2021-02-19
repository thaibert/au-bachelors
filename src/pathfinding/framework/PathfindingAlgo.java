package pathfinding.framework;

import java.util.*;
import graph.*;

public interface PathfindingAlgo {

    /** 
     * TODO description of interface 
     */
    public Solution shortestPath(Graph graph, Vertex start, Vertex goal);

}
