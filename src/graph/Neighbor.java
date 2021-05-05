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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true; 
        }
        if (obj == null) {
            return false;
        }
        final Neighbor other = (Neighbor) obj;
        if ((v != other.v) || (distance != other.distance)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 37; // I saw on https://stackoverflow.com/questions/2265503/why-do-i-need-to-override-the-equals-and-hashcode-methods-in-java 
                              // That they used prime in auto generated code, so now we do as well ¯\_(ツ)_/¯
        int longHash = Double.hashCode(v.getLatitude());
        int latHash = Double.hashCode(distance);

        return (prime + longHash + latHash); 
    }


}
