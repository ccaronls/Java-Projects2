package cc.lib.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;

import javax.swing.Icon;

import cc.lib.game.AImage;

public class AWTImage extends AImage implements Icon {

    final Image image;
    final Component comp;
    int [] pixels = null;
    
    AWTImage(Image image, Component comp) {
        this.image = image;
        this.comp = comp;
    }
    
    @Override
    public final float getWidth() {
        return image.getWidth(comp);
    }

    @Override
    public final float getHeight() {
        return image.getHeight(comp);
    }

    @Override
    public final int[] getPixels() {
        if (pixels != null)
            return pixels;
        int w = image.getWidth(comp);
        int h = image.getHeight(comp);
        pixels = new int[w * h];
        PixelGrabber pg = new PixelGrabber(image, 0, 0, w, h, pixels, 0, w);  
        try {
            if (!pg.grabPixels()) {
                throw new RuntimeException("Failed to grabPixels"); 
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to grabPixels", e);
        }
        return pixels;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(image, x, y, null);
    }

    @Override
    public int getIconWidth() {
        return image.getWidth(null);
    }

    @Override
    public int getIconHeight() {
        return image.getHeight(null);
    }

    public AWTImage transform(ImageFilter filter) {
        ImageProducer p = new FilteredImageSource(image.getSource(), filter);
        return new AWTImage(Toolkit.getDefaultToolkit().createImage(p), comp);
    }
}
