package pathfinding.framework;

import graph.*;
import java.util.*;

public class Solution {

    private List<Vertex> shortestPath;
    private List<Edge> visited;
    
    public Solution(List<Vertex> path, List<Edge> visited){
        this.shortestPath = path;
        this.visited = visited;
    }

	public List<Vertex> getShortestPath() {
		return shortestPath;
	}

    public List<Edge> getVisited() {
        return visited;
    }
    
}

