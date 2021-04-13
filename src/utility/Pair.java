package utility;
import graph.*;
import java.util.*;


public class Pair implements Comparable<Pair>{
    public final Vertex v;
    public final double dist;

    public Pair(Vertex v, double dist) {
        this.v = v;
        this.dist = dist;
    }

    /*
    * I think this is covering all the possibilities, but maybe there's something i missed 
    */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true; 
        }
        if (obj == null) {
            return false;
        }
        final Pair other = (Pair) obj;
        if ((! v.equals(other.v)) || (dist != other.dist)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 37; // I saw on https://stackoverflow.com/questions/2265503/why-do-i-need-to-override-the-equals-and-hashcode-methods-in-java 
                              // That they used prime in auto generated code, so now we do as well ¯\_(ツ)_/¯
        int vHash = v.hashCode();
        int distHash = Double.hashCode(dist);

        return (prime + vHash + distHash); 
    }

    @Override
    public int compareTo(Pair arg0) {
        return Double.compare(this.dist, arg0.dist);
    }

}
