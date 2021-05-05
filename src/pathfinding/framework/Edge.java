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

    public String toString() {
        return start.toString() + end.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true; 
        }
        if (obj == null) {
            return false;
        }
        final Edge other = (Edge) obj;
        if ((start != other.start) || (end != other.end)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 37; // I saw on https://stackoverflow.com/questions/2265503/why-do-i-need-to-override-the-equals-and-hashcode-methods-in-java 
                              // That they used prime in auto generated code, so now we do as well ¯\_(ツ)_/¯
        int longHash = Double.hashCode(start.getLatitude());
        int latHash = Double.hashCode(end.getLatitude());

        return (prime + longHash + latHash); 
    }

}

