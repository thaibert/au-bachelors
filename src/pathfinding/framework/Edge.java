package pathfinding.framework;

import graph.*;

public class Edge {

    private Vertex start;
    private Vertex end;
    private double dist;
    
    public Edge(Vertex start, Vertex end, double dist){
        this.start = start;
        this.end = end;
        this.dist = dist;
    }

	public Vertex getStart() {
		return start;
	}

    public Vertex getEnd() {
        return end;
    }

    public double getDist() {
        return dist;
    }
    
}

