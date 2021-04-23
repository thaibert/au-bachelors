package graph;

import java.io.Serializable;

public class Vertex implements Serializable {

    private double longitude;
    private double latitude;


    public Vertex(double latitude, double longitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
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
        final Vertex other = (Vertex) obj;
        if ((longitude != other.longitude) || (latitude != other.latitude)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 37; // I saw on https://stackoverflow.com/questions/2265503/why-do-i-need-to-override-the-equals-and-hashcode-methods-in-java 
                              // That they used prime in auto generated code, so now we do as well ¯\_(ツ)_/¯
        int longHash = Double.hashCode(longitude);
        int latHash = Double.hashCode(latitude);

        return (prime + longHash + latHash); 
    }

    public String toString() {
        return getLatitude() + "," + getLongitude();
    }


}
