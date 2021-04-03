package pathfinding.framework;

import graph.*;
import java.util.*;

public class Solution {

    private List<Vertex> shortestPath;
    private List<Edge> visited;
    private Vertex meetingNode;
    
    public Solution(List<Vertex> path, List<Edge> visited, Vertex meetingNode){
        this.shortestPath = path;
        this.visited = visited;
        this.meetingNode = meetingNode;
    }

	public List<Vertex> getShortestPath() {
		return shortestPath;
	}

    public List<Edge> getVisited() {
        return visited;
    }
    
    public Vertex getMeetingNode() {
        return meetingNode;
    }
}

