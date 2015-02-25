package cc.app.fractal;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import cc.lib.game.Justify;
import cc.lib.math.ComplexNumber;
import cc.lib.swing.AWTGraphics;

/**
 * Responsible for rendering, zooming, undo/redo, publishing fractal generation progress to a listener
 * @author chriscaron
 *
 */
public class FractalComponent extends JComponent implements MouseListener, MouseMotionListener { 

    private final int WIDTH = 512;
    private final int HEIGHT = 512;

    private final Dimension dim = new Dimension(WIDTH, HEIGHT);
    
    private final int [] fractalPixels = new int[WIDTH * HEIGHT];
    private final int defaultZoom;// = 2;
    
    private final MediaTracker tracker = new MediaTracker(this);
    private int trackerId = 0;
    
    private final ColorTable colorTable;
    
    private int mx0, my0, mx1, my1;
    
    private boolean dragging = false;
    private GeneratorThread generator;
    private boolean showWatermark = false;
    
    private final ComplexNumber C = new ComplexNumber();
    
    private class GeneratorThread {
        boolean generating = false;
        GeneratorThread() {
            new Thread(new Runnable() {
                public void run() {
                    generating = true;
                    updateFractal();
                    generating = false;
                    repaint();
                }
            }).start();
        }
    }

    public interface FractalListener {
        
        /**
         * Update progress.  
         * @param progress value between 0-100
         */
        void onProgress(int progress);
        
        void onDone();
    }
    
    private FractalListener fractalListener = new DefaultProgressListener();
    
    class DefaultProgressListener implements FractalListener {

        @Override
        public void onProgress(int progress) {}

        @Override
        public void onDone() {}

    };
    
    public void setFractalListener(FractalListener listener) {
        this.fractalListener = listener == null ? new DefaultProgressListener() : listener;
    }
    
    public FractalListener getFractalListener() {
        return this.fractalListener;
    }
    
    class FractalImage {
        Image image;
        final double left;
        final double right;
        final double top;
        final double bottom;
        ColorTable.Scale scale;
        
        FractalImage(double left, double right, double top, double bottom, ColorTable.Scale scale) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.scale = scale;
        }
        
        void reset() {
            scale = null;
        }
    };
    
    // store images for undo/redo ops
    private final FractalImage [] images = new FractalImage[256];
    private int numImages = 0;
    
    public FractalComponent(ColorTable colorTable, int defaultZoom) {
        this.colorTable = colorTable;
        addMouseListener(this);
        addMouseMotionListener(this);
        this.defaultZoom = defaultZoom;
        
        images[numImages++] = new FractalImage(-defaultZoom, defaultZoom, -defaultZoom, defaultZoom, colorTable.currentScale);

    }

    boolean canUndo() {
        return numImages > 1;
    }
    
    void undo() {
        if (canUndo()) {
            numImages--;
            startNewFractal(false);
        }            
    }
    
    void zoom(double percent) {
        FractalImage fi = getLastFractalImage();
        
        double left = fi.left;
        double right = fi.right;
        double top = fi.top;
        double bottom = fi.bottom;
        
        double cx = (left+right)/2;
        double cy = (top+bottom)/2;
        
        double width  = Math.abs(right-left);
        double height = Math.abs(bottom - top);
        
        double newWidth  = width*percent;
        double newHeight = height*percent;

        left    = cx-newWidth/2;
        right   = cx+newWidth/2;
        top     = cy+newHeight/2;
        bottom  = cy-newHeight/2;
        
        zoomRect(left, right, top, bottom);
        
    }

    boolean canRedo() {
        return numImages < images.length-1 && images[numImages] != null;
    }
    
    void redo() {
        if (canRedo()) {
            numImages++;
            startNewFractal(false);
        }
    }

    // TODO: This method should not be visible
    FractalImage getLastFractalImage() {
        return images[numImages-1];
    }
    
    int [] getFractalBitmap() {
        int [] bitmap = new int[WIDTH * HEIGHT];
        for (int i=0; i<bitmap.length; i++) {
            bitmap[i] = colorTable.getColorTable()[fractalPixels[i]];
        }
        return bitmap;
    }
    
    Image deleteImage(Image image) {
    	if (image != null)
    		tracker.removeImage(image);
    	return null;
    }
    
    void cancel() {
        if (generator != null)
            generator.generating = false;
    }

    void updateFractal() {
        Image newImage = null;
        if (getLastFractalImage().scale != colorTable.currentScale) {
            newImage = createImage(new MemoryImageSource(WIDTH, HEIGHT, getFractalBitmap(), 0, WIDTH));
            if (showWatermark) {
                Image i = createImage(WIDTH, HEIGHT);
                i.getGraphics().drawImage(newImage, 0, 0, null);
                attachWatermark(i);
                newImage = i;
            }
            getLastFractalImage().scale = colorTable.currentScale;
        }
        if (getLastFractalImage().image == null && newImage == null) {
            generateFractal();
            newImage = createImage(new MemoryImageSource(WIDTH, HEIGHT, getFractalBitmap(), 0, WIDTH));
            if (showWatermark) {
                Image i = createImage(WIDTH, HEIGHT);
                i.getGraphics().drawImage(newImage, 0, 0, null);
                attachWatermark(i);
                newImage = i;
            }
        } else {
            fractalImage = getLastFractalImage().image;
        }
        if (newImage != null) {
            tracker.addImage(newImage, trackerId);
            int errors = 0;
            while (true) {
                try {
                    tracker.waitForID(trackerId);
                    break;
                } catch (OutOfMemoryError e) {
                    System.gc();
                    if (++errors > 100)
                        break;
                } catch (Exception e) {
                    if (++errors > 100)
                        break;
                }

            }
            trackerId++;
            images[numImages-1].image = fractalImage = newImage;
            getLastFractalImage().image = deleteImage(getLastFractalImage().image);
            getLastFractalImage().image = newImage;
        }
    }
    
    void generateFractal() {
        
        System.out.println("Generating " + fractal.getName());
        try {
            generator.generating = true;
            FractalImage fi = getLastFractalImage();
            final double xStep = (fi.right - fi.left) / WIDTH;
            final double yStep = (fi.bottom - fi.top) / HEIGHT;
            double y = fi.top;
            fractalListener.onProgress(0);
            fractal.setup(C);
            for (int j=0; j<HEIGHT && generator.generating; j++) {
                double x = fi.left;
                for (int i=0; i<WIDTH && generator.generating; i++) {
                    int index = fractal.processPixel(x, y, ColorTable.LENGTH);
                    assert(index >= 0 && index < 256);
                    fractalPixels[i + j * WIDTH] = index;
                    x += xStep;
                }
                y += yStep;
                int progress = j * 100 / HEIGHT + 1;
                fractalListener.onProgress(progress);
            }
            fractalListener.onDone();
        } catch (Exception e) {
            System.err.println("Error: " + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            generator.generating = false;
        }
    }
    
    private AFractal fractal;
    
    public void setFractal(ComplexNumber C, AFractal frac) {
        this.fractal = frac;
        this.C.copy(C);
    }
    
    public void setConstant(ComplexNumber C) {
        this.C.copy(C);
    }
    
    public ComplexNumber getConstant() {
        return this.C;
    }
    
    public Dimension getMaximumSize() {
        return dim;
    }

    @Override
    public Dimension getMinimumSize() {
        return dim;
    }
    
    @Override
    public Dimension getPreferredSize() {
        return dim;
    }

    private Image fractalImage = null;
    
    @Override
    public void paint(Graphics g) {
        if (fractalImage == null) {
            if (generator != null)
                generator.generating = false;
            generator = new GeneratorThread();
        } else {
            g.drawImage(fractalImage, 0, 0, null);
        }
        synchronized (this) {
            notify();
        }
        if (dragging) {
            g.setColor(Color.WHITE);            
            g.drawRect(mx0, my0, mx1-mx0, my1-my0);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        mx0 = mx1 = e.getX();
        my0 = my1 = e.getY();
    }

    public void mouseReleased(MouseEvent e) {
        if (dragging) {
            FractalImage fi = getLastFractalImage();
            dragging = false;
            
            double left = fi.left;
            double right = fi.right;
            double top = fi.top;
            double bottom = fi.bottom;
            
            double width = right - left;
            double height = bottom - top;
            left = left + (double)mx0 / WIDTH * width;
            right = left + (double)(mx1-mx0) / WIDTH * width;
            top  = top + (double)my0 / HEIGHT * height;
            bottom = top + (double)(my1-my0) / HEIGHT * height;
            zoomRect(left, right, top, bottom);
        }
    }
    
    public void zoomRect(double left, double right, double top, double bottom) {
        System.out.println("Zoom to [" + left + ", " + top + "] x [" + right + ", " + bottom + "]");
        images[numImages++] = new FractalImage(left, right, top, bottom, colorTable.currentScale);
        startNewFractal(false);
    }

    public void mouseDragged(MouseEvent e) {
        if (numImages < images.length-1) {
            dragging = true;
            mx1 = e.getX();
            my1 = e.getY();
            int minx = Math.min(mx0, mx1);
            int miny = Math.min(my0, my1);
            int maxx = Math.max(mx0, mx1);
            int maxy = Math.max(my0, my1);
            mx0 = minx;
            my0 = miny;
            mx1 = maxx;
            my1 = maxy;
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    void startNewFractal(boolean resetColorTable) {
        if (resetColorTable)
            this.getLastFractalImage().scale = null;
        if (generator != null)
            generator.generating = false; // cancel
        this.fractalImage = null;
        repaint();
    }
    
    void attachWatermark(Image im) {
    	
    	final int padding = 10;
    	
        Graphics g = im.getGraphics();
        AWTGraphics G = new AWTGraphics(g, this);

        FractalImage f = getLastFractalImage();
        String str = String.format("%s\nRect [%5.3f, %5.3f x %5.3f, %5.3f]", fractal.getDescription(), f.left, f.top, f.right, f.bottom);
        //G.drawJustifiedString(G.getScreenHeight()-padding, G.getScreenWidth()-padding, Justify.RIGHT, Justify.BOTTOM, str);
        String [] lines = G.generateWrappedLines(str, G.getScreenWidth());
        int width = 0;
        for (String l : lines) {
        	width = (int)Math.max(width, G.getTextWidth(l));
        }
        width += padding;
        int height = lines.length*G.getTextHeight() + padding;
        G.setColor(G.BLACK.setAlpha(0.5f));
        G.drawFilledRect(G.getScreenWidth() -width -5, G.getScreenHeight() -height -2, width, height);

        G.setColor(G.WHITE);
        G.drawJustifiedString(G.getScreenHeight()-padding, G.getScreenWidth()-padding, Justify.RIGHT, Justify.BOTTOM, str);

        //g.drawString(String.format("Rect [%5.3f, %5.3f x %5.3f, %5.3f]", f.left, f.top, f.right, f.bottom), x, y);
    }
    
    /**
     * save image to file
     * @param file where to save image
     * @param format jpg, gif, png, etc.
     * @throws IOException
     */
    void saveImage(File file, String format) throws IOException {
        System.out.println("Writing fractal to file: " + file + " using format " + format);
        BufferedImage im = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        im.setRGB(0, 0, WIDTH, HEIGHT, getFractalBitmap(), 0, WIDTH);
        if (showWatermark)
            attachWatermark(im);
        ImageIO.write(im, format, file);
    }

    /**
     * delete all images and history.  If resetZoom is true, then zoom out to default zoom.
     * @param resetZoom
     */
    void reset(boolean resetZoom) {
        // clear everything
        double left = -defaultZoom;
        double right = defaultZoom;
        double top = defaultZoom;
        double bottom = -defaultZoom;
        for (int i=images.length-1; i>=0; i--) {
        	if (images[i]!=null) {
        	    if (!resetZoom && i==numImages) {
        	        left    = images[i].left;
                    right   = images[i].right;
                    top     = images[i].top;
                    bottom  = images[i].bottom;
                    resetZoom = true;
        	    }
        		images[i].image = this.deleteImage(images[i].image);
        	}
        }
        images[0] = new FractalImage(left, right, top, bottom, colorTable.currentScale);
        numImages = 1;
    }

    ColorTable getColorTable() {
        return this.colorTable;
    }

    public void setShowWatermark(boolean show) {
        if (showWatermark == show)
            return;

        this.showWatermark = show;
        startNewFractal(true);
    }

}
