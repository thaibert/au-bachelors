package pathfinding.framework;

import graph.*;
import java.util.*;

public class Solution {

    private List<Vertex> shortestPath;
    private Map<Vertex, Vertex> visited;
    
    public Solution(List<Vertex> path, Map<Vertex, Vertex> visited){
        this.shortestPath = path;
        this.visited = visited;
    }

	public List<Vertex> getShortestPath() {
		return shortestPath;
	}

    public Map<Vertex, Vertex> getVisited() {
        return visited;
    }
    
}

