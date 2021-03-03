package graph;

public class BoundingBox {

    public static BoundingBox Aarhus = new BoundingBox(56.1269, 56.1846, 10.1365, 10.2580);
    public static BoundingBox GreaterAarhus = new BoundingBox(56.0953, 56.2296, 10.0393, 10.2974);
    public static BoundingBox Christiansbjerg = new BoundingBox(56.1561, 56.1874, 10.1674, 10.2070);
    public static BoundingBox AarhusSilkeborg = new BoundingBox(56.0337, 56.2794, 9.4807, 10.259);
    public static BoundingBox Denmark = new BoundingBox(54.525, 57.799, 8.010, 12.838);
    
    public final double SOUTH;
    public final double NORTH;
    public final double WEST;
    public final double EAST;


    BoundingBox(double SOUTH, double NORTH, double WEST, double EAST) {
        this.SOUTH = SOUTH;
        this.NORTH = NORTH;
        this.WEST = WEST;
        this.EAST = EAST;
    }
    
}
