package graphics;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

public class GraphicsUtils {

    public static BufferedImage zoom(BufferedImage img, double baseScale, double zoom) {
        // Using AffineTransform instead of Image.getScaledInstance is much faster!
        // https://web.archive.org/web/20130119065536/http://today.java.net:80/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html

        double intermediateScaling = 2; 
        // Read above: with AffineTransform, should not downscale more than half per scaling!
        // So scaling by 50% and then 50% is better than one of 25%

        double scale = zoom * baseScale;

        while (scale < 1) {
            BufferedImage bufferedScaled = new BufferedImage(
                (int) (1/intermediateScaling * img.getWidth()),
                (int) (1/intermediateScaling * img.getHeight()), 
                BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = bufferedScaled.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR); // Bilinear is pretty fast and pretty pretty :)

            AffineTransform at = AffineTransform.getScaleInstance(1/intermediateScaling, 1/intermediateScaling);
            g2d.drawImage(img, at, null);
            g2d.dispose();

            img = bufferedScaled;
            scale *= intermediateScaling;
        }
        return img;
    }
    
}
