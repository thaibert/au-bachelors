package graph;

public class Neighbor {
    public final Vertex v;
    public final double distance;

    public Neighbor(Vertex v, double distance) {
        this.v = v;
        this.distance = distance;
    }

    public String toString() {
        return v.toString() + "," + distance;
    }
}
