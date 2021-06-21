package graphics;

public class Projection {
    public static int[] convertToXAndY(ProjectionConstants pc, double lat, double lon){
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);

        // https://wiki.openstreetmap.org/wiki/Mercator#Java
        // https://stackoverflow.com/a/14330009
        double y = Math.log(Math.tan(Math.PI / 4 + lat / 2));
        double x = lon;

        x = x - pc.MIN_X; // ensure no negative coords
        y = y - pc.MIN_Y; // ensure no negative coords

        int imgX = (int) (pc.widthPadding + (x * pc.globalRatio));
        int imgY = (int) (pc.image_height - pc.heightPadding - (y * pc.globalRatio));
        return new int[]{imgX, imgY};
    }
}

class ProjectionConstants {
    public final double MIN_X, MIN_Y;
    public final double widthPadding, heightPadding;
    public final double image_height;
    public final double globalRatio;
    public ProjectionConstants(double MIN_X, double MIN_Y, double widthPadding, double heightPadding, int image_height,
            double globalRatio) {
        this.MIN_X = MIN_X;
        this.MIN_Y = MIN_Y;
        this.widthPadding = widthPadding;
        this.heightPadding = heightPadding;
        this.image_height = image_height;
        this.globalRatio = globalRatio;
    }
}
