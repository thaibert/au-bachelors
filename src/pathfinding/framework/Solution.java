package pathfinding.framework;

import graph.*;
import java.util.*;

public class Solution {

    private List<Vertex> shortestPath;
    private List<Edge> visited;
    private Vertex meetingNode;
    private int scannedVertices;
    
    public Solution(List<Vertex> path, List<Edge> visited, Vertex meetingNode, int scannedVertices){
        this.shortestPath = path;
        this.visited = visited;
        this.meetingNode = meetingNode;
        this.scannedVertices = scannedVertices;
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

    public int getAmountOfScannedVertices(){
        return scannedVertices;
    }
}

