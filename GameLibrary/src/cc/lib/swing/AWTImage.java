package cc.lib.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.PixelGrabber;

import cc.lib.game.AImage;

class AWTImage extends AImage {

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

}
