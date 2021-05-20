package graph;

public class BoundingBox {

    public static BoundingBox Aarhus = new BoundingBox(56.1269, 56.1846, 10.1365, 10.2580);
    public static BoundingBox GreaterAarhus = new BoundingBox(56.0953, 56.2296, 10.0393, 10.2974);
    public static BoundingBox Christiansbjerg = new BoundingBox(56.1561, 56.1874, 10.1674, 10.2070);
    public static BoundingBox AarhusSilkeborg = new BoundingBox(56.0328000, 56.300000, 9.4003000, 10.3546000);
    public static BoundingBox Denmark = new BoundingBox(54.525, 57.799, 8.010, 12.838);
    public static BoundingBox intersection = new BoundingBox(56.1484, 56.1486, 9.85, 9.86); //This is the one from the meeting to test something
    public static BoundingBox Europa = new BoundingBox(29.735139, 81.47299000000001, -34.492960000000004, 46.75348);
    public static BoundingBox Iceland = new BoundingBox(62.845530000000004, 67.50085, -25.740850000000002, -12.41708);
    public static BoundingBox ReachTest = new BoundingBox(56.1474, 56.1497, 9.8977, 9.9194);

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
