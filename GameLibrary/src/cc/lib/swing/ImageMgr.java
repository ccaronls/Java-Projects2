package cc.lib.swing;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.ReplicateScaleFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

import javax.swing.ImageIcon;

import cc.lib.game.Utils;
import cc.lib.math.CMath;

public class ImageMgr {

	private Component 		comp; // Image Observer
	private Vector<Image>	sourceImages = new Vector<Image>(); // loaded images
	private Vector<Image>	scaledImages = new Vector<Image>(); // scaled images
	private MediaTracker 	tracker; // used to wait for transforms, loading
	private int				trackerId = 0;
	
	/**
	 * 
	 * @param comp
	 */
	public ImageMgr(Component comp) {
		this.comp = comp;
		this.tracker = new MediaTracker(comp);
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
	private Image loadImageFromFile(String name) {
		InputStream in = null;
		try {
			in = new FileInputStream(new File(name));
			byte [] buffer = new byte[in.available()];
			in.read(buffer);
			return new ImageIcon(buffer).getImage();
		} catch (Exception e) {
			Utils.println(e.getMessage());
			return null;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception e) {}
		}
	}
	
	/* */
	private Image loadImageFromResource(String name) {
		InputStream in = null;
		try {
			in = getClass().getClassLoader().getResourceAsStream(name);
			byte [] buffer = new byte[in.available()];
			in.read(buffer);
			return new ImageIcon(buffer).getImage();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		} finally {
			try {
				in.close();
			} catch (Exception e) {}
		}
	}
	
	private Image loadImageFromApplet(String name) {
		if (comp instanceof Applet) {
			try {
				URL url = new URL(name);
				return ((Applet)comp).getImage(url);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				return null;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param name
	 * @param transparent
	 * @return
	 */
	public int loadImage(String fileOrResourceName, Color transparent) {
        int id = sourceImages.size();
		Image image = null;
		Utils.print("Loading image %d : %s ...", id, fileOrResourceName);
		if ((image = this.loadImageFromFile(fileOrResourceName))!=null) {
			Utils.print("From File...");
		} else if ((image = this.loadImageFromResource(fileOrResourceName))!=null) {
			Utils.print("From Resource...");
		} else if ((image = this.loadImageFromApplet(fileOrResourceName))!=null) {
			Utils.print("From Applet...");
		} else {
			throw new RuntimeException("Cannot load image");
		}		
		
		if (transparent != null) {
			image = transform(image, new TransparencyFilter(transparent));
		}			

		Utils.println("SUCCESS");
		sourceImages.add(image);
		scaledImages.add(image);
		return id;
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
	public int [] loadImageCells(Image source, int width, int height, int num_cells_x, int num_cells, boolean celled) {
		
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
	public int [] loadImageCells(String file, int width, int height, int num_cells_x, int num_cells, boolean celled, Color transparentColor) {
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
	public int [] loadImageCells(int sourceId, int width, int height, int numx, int num, boolean celled) {
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
		Image image = sourceImages.get(id);
		image = transform(image, new TransparencyFilter(color)); 
		sourceImages.set(id, image);
		scaledImages.set(id, image);
	}
	
	/**
	 * Get an image resized if neccessary to the specified dimension.
	 * The resize op only happens when the dimensions change.
	 * @param id
	 * @param width
	 * @param height
	 * @return
	 */
	public Image getImage(int id, int width, int height) {
		Image image = scaledImages.get(id);
		int curW = image.getWidth(comp);
		int curH = image.getHeight(comp);
		if (width >= 8 && width <= 1024 && height >= 8 && height <= 1024 && curW != width || curH != height) {
			//Utils.println("Resizing image [" + id + "] from " + curW + ", " + curH + " too " + width + ", " + height);
		    Utils.println("Resizing image [%d] from %d, %d too %d, %d", id, curW, curH, width, height);

			image = sourceImages.get(id);
			image = transform(image, new ReplicateScaleFilter(width,height));
			Image toDelete = scaledImages.get(id);
			if (toDelete != null) {
				tracker.removeImage(toDelete);
			}
			scaledImages.set(id, image); // make this our new scaled images
		}
		return image;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public Image getImage(int id) {
		Image image = scaledImages.get(id);
		if (image == null)
			image = sourceImages.get(id);
		Utils.assertTrue(image != null);
		return image;
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
	public void drawImage(Graphics g, int id, int x, int y, int w, int h) {
	    try {
    		Image image = getImage(id, w, h);
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
		return sourceImages.get(id);
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
		return this.scaledImages.get(id).getWidth(comp);
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public int getHeight(int id) {
		return this.scaledImages.get(id).getHeight(comp);
	}
	
	/**
	 * 
	 * @param image
	 * @return
	 */
	public int addImage(Image image) {
		int id = 0;
		for ( ; id<sourceImages.size(); id++) {
			if (sourceImages.get(id) == null) {
				sourceImages.set(id, image);
				scaledImages.set(id, image);
				return id;
			}
		}
		sourceImages.add(image);
		scaledImages.add(image);
		return id;
	}
	
	/**
	 * 
	 * @param id
	 */
	public void deleteImage(int id) {
		sourceImages.set(id, null);
		scaledImages.set(id, null);
	}
	
	
	public void deleteAll() {
		scaledImages.clear();
		sourceImages.clear();
	}
	

	/*
	 * 
	 */
	public Image transform(Image image, ImageFilter filter) {
		ImageProducer p = new FilteredImageSource(image.getSource(), filter);
		Image newImage = comp.createImage(p);//Toolkit.getDefaultToolkit().createImage(p);
		waitForIt(newImage);
		return newImage;
	}
	
	/**
	 * Only 0, 90, 180 and 270 supported at this time
	 * 
	 * @param id
	 * @param degrees
	 */
	public int newRotatedImage(int sourceId, int degrees) {
	    
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
	    
	    PixelGrabber grabber = new PixelGrabber(image, 0, 0, image.getWidth(comp), image.getHeight(comp), pixels, 0, srcWid);
	    try {
	        grabber.grabPixels();
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to grab pixels");
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
	    //Image rotated = transform(newImage, new RotationImageFilter(pixels, degrees, srcWid, srcHgt, dstWid, dstHgt));
	    //return addImage(rotated);
	}
	
	public int newImage(int [] pixels, int w, int h) {
	    Image img = comp.createImage(new MemoryImageSource(w, h, pixels, 0, w));
	    return addImage(img);
	}

	/*
	 * 
	 */
	private void waitForIt(Image image) {
		tracker.addImage(image, trackerId);
		
		for (int i=0; i<3; i++) {
			try {
				tracker.waitForID(trackerId);
				break;
			} catch (Exception e) {
				
			}
		}
		trackerId++;
	}

}
