package cc.lib.swing;

import java.awt.Graphics;

import cc.lib.game.*;

/**
 * Class to allow for OpenGL type rendering in 2D.
 * Good for rendereing in cartesian coordinates.
 * 
 * @author Chris Caron
 */
public final class AWTRenderer extends Renderer {

	/**
	 * 
	 * @param window
	 */
	public AWTRenderer(Renderable window) {
		super(window);
	} 
	
	/**
	 * draw points
	 * @param g
	 * @param size value between 1 and whatever
	 */
	public void drawPoints(Graphics g, int size) {
		if (size <= 1) {
			for (int i=0; i<getNumVerts(); i++) {
				g.drawRect(Math.round(getX(i)), Math.round(getY(i)), 1, 1);
			}
		} else {
			for (int i=0; i<getNumVerts(); i++) {
				g.drawOval(Math.round(getX(i)-size/2), Math.round(getY(i)-size/2), size, size);
			}
		}			
	}
	
	/**
	 * draw points
	 * @param g
	 * @param size value between 1 and whatever
	 */
	public void fillPoints(Graphics g, int size) {
		if (size <= 1) {
			for (int i=0; i<getNumVerts(); i++) {
				g.fillRect(Math.round(getX(i)), Math.round(getY(i)), 1, 1);
			}
		} else {
			for (int i=0; i<getNumVerts(); i++) {
				g.fillOval(Math.round(getX(i)-size/2), Math.round(getY(i)-size/2), size, size);
			}
		}			
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param size
	 * @return
	 */
	public int pickPoints(int x, int y, int size) {
		int picked = -1;
        int bestD = Integer.MAX_VALUE;
		for (int i=0; i<getNumVerts(); i++) {
			int dx = x - Math.round(getX(i));
			int dy = y - Math.round(getY(i));
			int d = Utils.fastLen(dx, dy);
			if (d <= size) {
			    if (picked < 0 || d < bestD) {
			        picked = getName(i);
			        bestD = d;
			    }
			}
		}
		return picked;
	}
	
	/**
	 * draw a series of lines defined by each
	 * consecutive pairs of pts
	 * @param g
	 */
	public void drawLines(Graphics g, int thickness) {
		for (int i=0; i<getNumVerts(); i+=2) {
		    if (i+1 < getNumVerts())
		        AWTUtils.drawLinef(g, getX(i), getY(i), getX(i+1), getY(i+1), thickness);	
		}
	}
	
	/**
	 * 
	 * @param g
	 * @param thickness
	 */
	public void drawLineStrip(Graphics g, int thickness) {
	    for (int i=0; i<getNumVerts()-1; i++) {
	        AWTUtils.drawLinef(g, getX(i), getY(i), getX(i+1), getY(i+1), thickness);
	    }
	}
	
	/**
	 * 
	 * @param thickness
	 * @param x
	 * @param y
	 * @return
	 */
	public int pickLines(int x, int y, int thickness) {
	    int picked = -1;
		for (int i=0; i<getNumVerts(); i+=2) {
			
			float x0 = getX(i);
			float y0 = getY(i);
			float x1 = getX(i+1);
			float y1 = getY(i+1);

			float d0 = Utils.distSqPointLine(x, y, x0, y0, x1, y1);
			if (d0 > thickness)
				continue;

			float dx = x1 - x0;
			float dy = y1 - y0;

			float dot_p_d1 = (x-x0)*dx + (y-y0)*dy;
			float dot_p_d2 = (x-x1)*-dx + (y-y1)*-dy;
			
			if (dot_p_d1 < 0 || dot_p_d2 < 0)
				continue;

			picked = getName(i);
		}
		return picked;
	}
	
	/**
	 * draw a series of ray emitting from pt[0]
	 * @param g
	 */
	public void drawRays(Graphics g) {
		for (int i=1; i<getNumVerts(); i++) {
			AWTUtils.drawLinef(g, getX(0), getY(0), getX(i), getY(i), 1);
		}
	}
	
	/**
	 * draw a polygon from the transformed points
	 * @param g
	 */
	public void drawLineLoop(Graphics g) {
	    if (getNumVerts() > 1) {
    	    for (int i=0; i<getNumVerts()-1; i++) {
    	        AWTUtils.drawLinef(g, getX(i), getY(i), getX(i+1), getY(i+1), 1);    
    	    }
    	    int lastIndex = getNumVerts()-1;
    	    AWTUtils.drawLinef(g, getX(lastIndex), getY(lastIndex), getX(0), getY(0), 1);
	    }
	}

	/**
	 * draw a polygon from the transformed points
	 * @param g
	 */
	public void drawLineLoop(Graphics g, int thickness) {
		//g.drawPolygon(x_pts, y_pts, num_pts);
		//num_pts = 0;
		if (thickness <= 1) {
			drawLineLoop(g);
			return;
		}
		if (getNumVerts() > 1) {
			for (int i=1; i<getNumVerts(); i++) {
				float x0 = getX(i-1);
				float y0 = getY(i-1);
				float x1 = getX(i);
				float y1 = getY(i);
				AWTUtils.drawLinef(g, x0, y0, x1, y1, thickness);
			}
			if (getNumVerts()>2) {
				float x0 = getX(getNumVerts()-1);
				float y0 = getY(getNumVerts()-1);
				float x1 = getX(0);
				float y1 = getY(0);
				AWTUtils.drawLinef(g, x0, y0, x1, y1, thickness);
			}
		}
	}

	/**
	 * draw a filled polygon from the transformed points
	 * @param g
	 */
	public void fillPolygon(Graphics g) {
	    this.drawTriangleFan(g);
	}
	
	/**
	 * 
	 * @param g
	 */
	public void drawTriangles(Graphics g) {		
		for (int i=0; i<=getNumVerts()-3; i+=3) {
			AWTUtils.drawTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
		}		
	}
	
	/**
	 * 
	 * @param g
	 */
	public void drawTriangleFan(Graphics g) {
        for (int i=1; i<getNumVerts()-1; i+=1) {
            //AWTUtils.drawTriangle(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
            AWTUtils.fillTrianglef(g, getX(0), getY(0), getX(i), getY(i), getX(i+1), getY(i+1));
        }       
        
    }
	
	/**
	 * 
	 * @param g
	 */
	public void drawTriangleStrip(Graphics g) {
		for (int i=0; i<=getNumVerts()-3; i+=1) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
		}
	}
	
	/**
	 * 
	 * @param g
	 */
	public void fillTriangles(Graphics g) {
		for (int i=0; i<=getNumVerts()-3; i+=3) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
		}		
	}

	/**
	 * 
	 * @param g
	 */
	public void fillTriangleStrip(Graphics g) {
		for (int i=0; i<=getNumVerts()-3; i+=1) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
		}
	}

	/**
	 * 
	 * @param g
	 */
	public void drawQuads(Graphics g) {
		for (int i=0; i<=getNumVerts()-4; i+=4) {
			AWTUtils.drawTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
			AWTUtils.drawTrianglef(g, getX(i+1), getY(i+1), getX(i+2), getY(i+2), getX(i+3), getY(i+3));
		}		
	}
	
	/**
	 * 
	 * @param g
	 */
	public void fillQuads(Graphics g) {
		for (int i=0; i<=getNumVerts()-4; i+=4) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
			AWTUtils.fillTrianglef(g, getX(i+1), getY(i+1), getX(i+2), getY(i+2), getX(i+3), getY(i+3));
		}		
	}

	/**
	 * 
	 * @param g
	 */
	public void drawQuadStrip(Graphics g) {
		for (int i=0; i<=getNumVerts()-2; i+=2) {
			AWTUtils.drawTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
			AWTUtils.drawTrianglef(g, getX(i+1), getY(i+1), getX(i+2), getY(i+2), getX(i+3), getY(i+3));
		}		
	}

	/**
	 * 
	 * @param g
	 */
	public void fillQuadStrip(Graphics g) {
		for (int i=0; i<=getNumVerts()-4; i+=2) {
			AWTUtils.fillTrianglef(g, getX(i), getY(i), getX(i+1), getY(i+1), getX(i+2), getY(i+2));
			AWTUtils.fillTrianglef(g, getX(i+1), getY(i+1), getX(i+2), getY(i+2), getX(i+3), getY(i+3));
		}		
	}

	/**
	 * 
	 * @param v
	 */
	public void translate(IVector2D v) {
		translate(v.getX(), v.getY());
	}

}
