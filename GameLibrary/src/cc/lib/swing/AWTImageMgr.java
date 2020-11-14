package cc.lib.swing;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.ReplicateScaleFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.CMath;
import cc.lib.utils.GException;

public final class AWTImageMgr {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static class ScaledImage {
        final Image image;
        final int w, h;

        public ScaledImage(Image image, int w, int h) {
            this.image = image;
            this.w = w;
            this.h = h;
        }
    }

    static class Meta {
        private Image source;
        private final int copies;

        final LinkedList<ScaledImage> scaledVersion = new LinkedList<>();

        void setSource(Image source) {
            this.source = source;
            scaledVersion.clear();
        }

        Meta(Image source, int maxCopies) {
            this.source = source;
            this.copies = maxCopies;
        }
    }

	private List<Meta> images = new ArrayList<>(); // loaded images

	/**
	 * 
	 */
	public AWTImageMgr() {
		//this.tracker = new MediaTracker(comp);
	}
	
	/* Returns an ImageIcon, or null if the path was invalid. 
	private static ImageIcon createImageIcon(String path) {
	    URL imgURL = Utils.class.getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}*/

	/* */
	private Image loadImageFromFile(String name) throws Exception {
		try (InputStream in = new FileInputStream(new File(name))) {
			byte [] buffer = new byte[in.available()];
			in.read(buffer);
			return new ImageIcon(buffer).getImage();
		}
	}

    private Image loadImageFromSearchPaths(String name) throws Exception {
	    for (String path : paths) {

            try (InputStream in = new FileInputStream(new File(path, name))) {
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                return new ImageIcon(buffer).getImage();
            } catch (FileNotFoundException e) {
                log.debug("Not found in search path '" + path + "':" + e.getMessage());
            } catch (IOException e) {
                throw e;
            }
        }
        throw new FileNotFoundException(name);
    }

    /* */
	private Image loadImageFromResource(String name) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
			byte [] buffer = new byte[in.available()];
			in.read(buffer);
			return new ImageIcon(buffer).getImage();
		} catch (NullPointerException e) {
			throw new FileNotFoundException(name);
		}
	}

	private Image loadImageFromApplet(Component comp, String name) {
		if (comp instanceof Applet) {
			try {
				URL url = new URL(name);
				return ((Applet)comp).getImage(url);
			} catch (Exception e) {
				System.err.println("Not found via Applet: " + e.getMessage());
				return null;
			}
		}
		return null;
	}

	private final List<String> paths = new ArrayList<>();

    public void addSearchPath(String s) {
        paths.add(s);
    }

    public synchronized int loadImage(String fileOrResourceName, Color transparent) {
        return loadImage(fileOrResourceName, transparent, 2);
    }

    /**
     *
     * @param fileOrResourceName
     * @param transparent
     * @return
     */
	public synchronized int loadImage(String fileOrResourceName, Color transparent, int maxCopies) {
        int id = images.size();
		Image image = null;
		log.debug("Loading image %d : %s ...", id, fileOrResourceName);
		try {
            try {
                image = this.loadImageFromFile(fileOrResourceName);
                log.debug("Image '" + fileOrResourceName + "' loaded from file");
            } catch (FileNotFoundException e) {
                try {
                    image = this.loadImageFromSearchPaths(fileOrResourceName);
                    log.debug("Image '" + fileOrResourceName + "' loaded from search paths");
                } catch (FileNotFoundException ee) {
                    image = this.loadImageFromResource(fileOrResourceName);
                    log.debug("Image '" + fileOrResourceName + "' loaded from resources");
                }
            }
        } catch (FileNotFoundException e) {
		    log.error("File '" + fileOrResourceName + "' Not found on file paths or resources");
        } catch (Exception e) {
		    log.error(e.getClass().getSimpleName() + ":" + e.getMessage());
            throw new GException("Cannot load image '" + fileOrResourceName + "'");
        }

		if (transparent != null) {
			image = transform(image, new AWTTransparencyFilter(transparent));
		}			
		return addImage(image, maxCopies);
	}

	/**
	 * Return an array 'num_cells' in length that is filled with ids to subimages
	 * of source where each subimage is width x height in dimension.  When
	 * celled is true, then each subimage is assumed to be bordered by 1 pixel
	 * border and the border is ommited.
	 * 
	 * @param source
	 * @param width
	 * @param height
	 * @param num_cells_x
	 * @param num_cells
	 * @param celled
	 * @return
	 */
	public synchronized int [] loadImageCells(Image source, int width, int height, int num_cells_x, int num_cells, boolean celled) {
		
		final int cellDelta = celled ? 1 : 0;
		
		int x=cellDelta;
		int y=cellDelta;
		int [] result = new int[num_cells];

		int nx = 0;
		for (int i=0; i<num_cells; i++) {
			result[i] = newSubImage(source, x, y, width, height);
			if (++nx == num_cells_x) {
				nx = 0;
				x=celled ? 1 : 0;
				y+=height + cellDelta;
			} else {
				x += width + cellDelta;
			}			
		}
		
		return result;
	}
	
	/**
	 * Convenience method
	 * @param file
	 * @param width
	 * @param height
	 * @param num_cells_x
	 * @param num_cells
	 * @param celled
	 * @return
	 */
	public synchronized int [] loadImageCells(String file, int width, int height, int num_cells_x, int num_cells, boolean celled, Color transparentColor) {
		return loadImageCells(loadImage(file, transparentColor), width, height, num_cells_x, num_cells, celled);
	}
		

	/**
	 * Convenience method, use getSourceImage(sourceId) as source Image.
	 * 
	 * @param sourceId
	 * @param width width of each sub image
	 * @param height height of each subimage
	 * @param numx number of cells on each row
	 * @param num number of cells total
	 * @param celled true of each cell has a 1 pixel border
	 * @return
	 */
	public synchronized int [] loadImageCells(int sourceId, int width, int height, int numx, int num, boolean celled) {
		return loadImageCells(this.getSourceImage(sourceId), width, height, numx, num, celled);
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public int loadImage(String fileName) {
		return loadImage(fileName, null);
	}
	
	/**
	 * 
	 * @param id
	 * @param color
	 */
	public void setTransparent(int id, Color color) {
	    Meta meta = images.get(id);
		Image image = meta.source;
		image = transform(image, new AWTTransparencyFilter(color));
		meta.setSource(image);
	}
	
	/**
	 * Get an image resized if neccessary to the specified dimension.
	 * The resize op only happens when the dimensions change.
	 * @param id
	 * @param width
	 * @param height
	 * @return
	 */
	public synchronized Image getImage(int id, int width, int height, Component comp) {
	    Meta meta = images.get(id);
	    for (ScaledImage si : meta.scaledVersion) {
	        int dw = Math.abs(width-si.w);
	        int dh = Math.abs(height-si.h);
	        if (dw <= 1 && dh <= 1) {
	            return si.image;
            }
        }
		Image image = meta.source;
		int curW = image.getWidth(comp);
		int curH = image.getHeight(comp);
		if (width >= 8 && width <= 1024*8 && height >= 8 && height <= 1024*8) {
			//log.debug("Resizing image [" + id + "] from " + curW + ", " + curH + " too " + width + ", " + height);
		    log.debug("Resizing image [%d] from %d, %d too %d, %d", id, curW, curH, width, height);

			image = transform(image, new ReplicateScaleFilter(width,height));
			meta.scaledVersion.addFirst(new ScaledImage(image, width, height));
			if (meta.scaledVersion.size() > meta.copies) {
			    meta.scaledVersion.removeLast();
            }
		}
		return image;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public Image getImage(int id) {
	    Meta meta = images.get(id);
	    if (meta.scaledVersion.size() == 0)
	        return meta.source;
	    return meta.scaledVersion.getFirst().image;
	}
	
	/**
	 * Render an image at the specified location and dimension
	 * @param g
	 * @param id
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public void drawImage(Graphics g, Component comp, int id, int x, int y, int w, int h) {
	    try {
    		Image image = getImage(id, w, h, comp);
    		g.drawImage(image, x, y, comp);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public final Image getSourceImage(int id) {
		return images.get(id).source;
	}
	
	/**
	 * 
	 * @param source
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	public int newSubImage(Image source, int x, int y, int w, int h) {
		Image cropped = transform(source, new CropImageFilter(x,y,w,h));
		return addImage(cropped);
	}
	
	/**
	 *
	 * @param id
	 * @return
	 */
	public int getWidth(int id) {
	    Meta meta = images.get(id);
	    if (meta.scaledVersion.size() > 0) {
	        return meta.scaledVersion.getFirst().w;
        }
		return meta.source.getWidth(null);
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public int getHeight(int id) {
        Meta meta = images.get(id);
        if (meta.scaledVersion.size() > 0) {
            return meta.scaledVersion.getFirst().h;
        }
        return meta.source.getHeight(null);
	}

    public int addImage(Image image) {
	    return addImage(image, 2);
    }

    /**
     *
     * @param image
     * @return
     */
	public int addImage(Image image, int maxCopies) {
		int id = 0;
		for ( ; id<images.size(); id++) {
			if (images.get(id).source == null) {
			    images.get(id).source = image;
				return id;
			}
		}
		images.add(new Meta(image, maxCopies));
		return id;
	}
	
	/**
	 * 
	 * @param id
	 */
	public void deleteImage(int id) {
	    Meta meta = images.get(id);
	    meta.source = null;
	    meta.scaledVersion.clear();
	}
	
	
	public void deleteAll() {
	    images.clear();
	}
	

	/*
	 * 
	 */
	public synchronized Image transform(Image image, ImageFilter filter) {
		ImageProducer p = new FilteredImageSource(image.getSource(), filter);
		Image newImage = Toolkit.getDefaultToolkit().createImage(p);
		waitForIt(newImage);
		return newImage;
	}

    /**
     * Only 0, 90, 180 and 270 supported at this time
     *
     * @param sourceId
     * @param degrees
     * @param comp
     * @return
     */
	public int newRotatedImage(int sourceId, int degrees, Component comp) {
	    
	    Image image = getImage(sourceId);
	    if (image == null || degrees == 0)
	        return sourceId;
	    int srcWid = image.getWidth(comp);
	    int srcHgt = image.getHeight(comp);
	    int dstWid = srcWid;
	    int dstHgt = srcHgt;
	    switch (degrees) {
	        case 0: 
	            return sourceId;
	        case 180:
	            break;
	        case 90: case 270: 
	            dstWid = srcHgt; dstHgt = srcWid; 
	            break;
	        default:
	            dstWid = dstHgt = Math.max(srcWid,  srcHgt);
	            break; // make destination a square
	    }

        //dstWid = dstHgt = Math.max(srcWid,  srcHgt);
	    int [] pixels = new int[srcWid * srcHgt];
	    
	    PixelGrabber grabber = new PixelGrabber(image, 0, 0, srcWid, srcHgt, pixels, 0, srcWid);
	    try {
	        grabber.grabPixels();
	    } catch (Exception e) {
	        throw new GException("Failed to grab pixels");
	    }

	    int [] rotated = new int[dstWid * dstHgt];
	    
	    final float x0 = srcWid / 2;
	    final float y0 = srcHgt / 2;
	    final float cosa = CMath.cosine(degrees);
	    final float sina = CMath.sine(degrees);
	    
	    for (int x1=0; x1<srcWid; x1++) {
	        for (int y1=0; y1<srcHgt; y1++) {
	            // rotation
	            // x2 = cos(a) * (x1-x0) - sin(a) * (y1-y0) + x0
	            // y2 = sin(a) * (x1-x0) + cos(a) * (y1 - y0) + y0
	            // where x0,y0 are center of rotation
	            float x2 = cosa * (x1-x0) - sina * (y1-y0) + dstWid/2 - 1;
	            float y2 = sina * (x1-x0) + cosa * (y1-y0) + dstHgt/2 ;
	            
                final int src = x1+y1*srcWid;
	            final int dst = Math.round(x2 + y2*dstWid);
	            if (dst >= 0 && dst < rotated.length)
	                rotated[dst] = pixels[src];
	        }
	    }
	    
	    return newImage(rotated, dstWid, dstHgt);
	    //BufferedImage newImage = new BufferedImage(dstWid, dstHgt, BufferedImage.TYPE_INT_ARGB);
	    //Image rotated = transform(newImage, new AWTRotationImageFilter(pixels, degrees, srcWid, srcHgt, dstWid, dstHgt));
	    //return addImage(rotated);
	}
	
	public synchronized int newImage(int [] pixels, int w, int h) {
	    Image img = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, pixels, 0, w));
	    return addImage(img);
	}

	/*
	 * 
	 */
	private void waitForIt(Image image) {
	    Utils.waitNoThrow(this, 100);
	}


}
