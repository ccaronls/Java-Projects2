package cc.app.bspline;

import java.awt.event.MouseEvent;

import cc.lib.game.*;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

public class BSplineExample extends AWTKeyboardAnimationApplet {
    
    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        AWTFrame frame = new AWTFrame("BSpline test");
        AWTKeyboardAnimationApplet app = new BSplineExample();
        frame.add(app);
        app.init();
        frame.centerToScreen(800, 600);
        app.start();
        app.setMillisecondsPerFrame(20);
    }   
    
    int [] xPoints = new int[256];
    int [] yPoints = new int[256];
    int numPts = 0;    
    
    void addPoint(float x, float y) {
        //System.out.println("Add pt " + x + "," + y);
        xPoints[numPts] = Math.round(x);
        yPoints[numPts++] = Math.round(y);
    }
    
    BSplineExample() {
    }
    
    MutableVector2D[] graph = new MutableVector2D[256];

    
    @Override
    protected void doInitialization() {
    }

    enum Mode {
        BSPLINE,
        GRAPH,
        BEIZER,
        CUSTOM,
    }
    
    Mode mode = Mode.CUSTOM;
    void drawHelp(AGraphics g) {
        g.setColor(g.BLACK);
        String txt = null;
        switch (mode) {
            case BSPLINE:
                txt = "BSPLINE Mode"
                    + "\nMouse + button - create control points"
                    + "\nC - Clear points"
                    + "\nG - Switch to Graph mode"
                    + "\nZ - Switch to Beizer mode"
                    + "\nQ - Switch to Custom mode";
                break;
                
            case GRAPH:
                txt = "GRAPH Mode"
                    + "\nMouse + click - stretch graph"
                    + "\nMouse + drag - add noise to graph pts"
                    + "\n+/- change minima/maxima"
                    + "\nB - Switch to BSpline mode"
                    + "\nZ - Switch to Beizer mode"
                    + "\nQ - Switch to Custom mode";
                break;
                
            case BEIZER:
                txt = "BEIZER Mode"
                    + "\nMouse + click - add up to four points"
                    + "\nMouse + drag - drag on a point to move"
                    + "\nC - Clear points"
                    + "\nG - Switch to graph mode"
                    + "\nB - Switch to B-Spline mode"
                    + "\nQ - Switch to Custom mode";
                
            case CUSTOM:
            	txt = "CUSTOM Mode"
                    + "\nMouse + button - create control points"
                    + "\nC - Clear points"
                    + "\nB - Switch to BSpline mode"
                    + "\nZ - Switch to Beizer mode"
                    + "\ng/G gamma: " + gamma + ""
                    + String.format("\nDist: %5.1f / %5.1f (%c%d%%)", distActual, distCurve, (distVariance > 0 ? '+' : '-'), Math.abs(distVariance));
                
        }
        
        g.drawJustifiedString(10, 10, txt);
    }
    
    void drawBSpline(AGraphics g) {
        g.setColor(g.RED);
        for (int i=0; i<numPts; i++) {
            g.drawDisk(xPoints[i], yPoints[i], 3);
            g.drawString("" + i, xPoints[i], yPoints[i]);
        }
        
        if (numPts > 3) {
            
            MutableVector2D [] P = {
                    new MutableVector2D(xPoints[0], yPoints[0]),
                    new MutableVector2D(xPoints[1], yPoints[1]),
                    new MutableVector2D(xPoints[2], yPoints[2]),
                    new MutableVector2D(xPoints[3], yPoints[3])
            };
            
            //float [] Px = { xPoints[0], xPoints[1], xPoints[2], xPoints[3] };
            //float [] Py = { yPoints[0], yPoints[1], yPoints[2], yPoints[3] };
            
            g.setColor(g.BLUE);
            g.begin();
            int i = 3;
            while (true) {
                //bsp(g, Px[0], Py[0], Px[1], Py[1], Px[2], Py[2], Px[3], Py[3], 10);
                bsp(g, P, 10);

                if (i++>=numPts)
                    break;
                
                P[0].set(P[1]); P[1].set(P[2]); P[2].set(P[3]); P[3].set(xPoints[i], yPoints[i]);
                //Px[0] = Px[1]; Px[1] = Px[2]; Px[2] = Px[3]; Px[3] = xPoints[i];
                //Py[0] = Py[1]; Py[1] = Py[2]; Py[2] = Py[3]; Py[3] = yPoints[i];
                
            } 
            g.drawLineStrip();
        }
        if (this.getKeyboardReset('c'))
            numPts=0;
        if (getKeyboardReset('g'))
            mode = Mode.GRAPH;
        if (getKeyboardReset('z'))
            mode = Mode.BEIZER;
    }

    int graphMinMaxBoxCount= 2; 
    
    
    void drawGraph(AGraphics g) {

        g.setColor(g.RED);
        
        g.begin();
        for (int i=0; i<graph.length; i++) {
            g.vertex(graph[i]);
        }
        g.drawLineStrip();

        int gr = 0;
        int boxWidth = graph.length/graphMinMaxBoxCount;
        g.setColor(g.CYAN);
        numPts=0;
        boolean toggle = false;
        for (int i=0; i<graphMinMaxBoxCount; i++) {
            MutableVector2D boxMin = new MutableVector2D(graph[gr]);
            MutableVector2D boxMax = new MutableVector2D(graph[gr]);
            for (int ii=0; ii<boxWidth; ii++) {
                boxMin.minEq(graph[gr]);
                boxMax.maxEq(graph[gr]);
                gr++;
            }
            drawRect(g, boxMin, boxMax, 1);
            if (numPts == 0) {
                addPoint(boxMin.X(), boxMin.Y());
                addPoint(boxMin.X(), boxMax.Y());
            }
                
            if (toggle) {
                addPoint(boxMax.X(), boxMin.Y());
                addPoint(boxMax.X(), boxMax.Y());
            } else {
                addPoint(boxMax.X(), boxMax.Y());
                addPoint(boxMax.X(), boxMin.Y());
            }
            toggle = !toggle;
        }

        drawBSpline(g);
        
        if (this.getKeyboardReset('-') && graphMinMaxBoxCount > 0)
            graphMinMaxBoxCount-=1;
        else if (getKeyboardReset('=') && graphMinMaxBoxCount < 100)
            graphMinMaxBoxCount+=1;
        
        if (getKeyboardReset('b'))
            mode = Mode.BSPLINE;
        if (getKeyboardReset('z'))
            mode = Mode.BEIZER;
        if (getKeyboardReset('q'))
        	mode = Mode.CUSTOM;
        
        if (dragging) {
            g.setColor(g.BLUE);
            drawRect(g, boxStart, boxEnd, 2);
        }
        
    }
    
    int pickedPoint = -1;
    
    void drawBeizer(AGraphics g) {
        g.setLineWidth(3);
        g.setColor(g.BLUE);
        if (numPts >= 4) {
            g.drawBeizerCurve(xPoints[0], yPoints[0], xPoints[1], yPoints[1], xPoints[2], yPoints[2], xPoints[3], yPoints[3], 100);
        }
        for (int i=0; i<this.numPts && i<4; i++) {
            g.setColor(g.YELLOW);
            if (dragging) {
                if (i == pickedPoint) {
                    g.setColor(g.GREEN);
                    xPoints[i] = getMouseX();
                    yPoints[i] = getMouseY();
                }
            } else {
                pickedPoint = -1;
                if (Utils.isPointInsideCircle(getMouseX(), getMouseY(), xPoints[i], yPoints[i], 5)) {
                    g.setColor(g.RED);
                    pickedPoint = i;
                }
            }
            g.drawDisk(xPoints[i], yPoints[i], 4);
        }
        
    }
    
    float gamma = 1;
    float distActual=0;
    float distCurve=0;
    int distVariance = 0;
    int iterations=10;
    
    void drawCustom(AGraphics g) {
    	g.setLineWidth(3);
    	g.setColor(g.BLUE);
    	
    	distActual = 0;
    	distCurve = 0;
    	
    	float dx=0, dy=0;
    	if (numPts >= 2) {
    		for (int i=0; i<numPts-1; i++) {
    			float [] d = drawCustomCurve(g, xPoints, yPoints, dx, dy, i, 10, gamma);
    			dx = d[0];
    			dy = d[1];
				float x = xPoints[i+1]-xPoints[i];
				float y = yPoints[i+1]-yPoints[i];
				distActual += Math.sqrt(x*x+y*y);
    			distCurve += d[2];
    		}
    	}
    	
    	if (distActual > 0)
    		distVariance = Math.round((distCurve-distActual) / distActual * iterations);
    	
    	for (int i=0; i<numPts; i++) {
            g.setColor(g.ORANGE);
            if (dragging) {
                if (i == pickedPoint) {
                    g.setColor(g.GREEN);
                    xPoints[i] = getMouseX();
                    yPoints[i] = getMouseY();
                }
            } else {
                pickedPoint = -1;
                if (Utils.isPointInsideCircle(getMouseX(), getMouseY(), xPoints[i], yPoints[i], 5)) {
                    g.setColor(g.RED);
                    pickedPoint = i;
                }
            }
            g.drawDisk(xPoints[i], yPoints[i], 4);
        }
    	
    	if (this.getKeyboardReset('c'))
            numPts=0;
        if (getKeyboardReset('z'))
            mode = Mode.BEIZER;
        if (getKeyboardReset('g') && gamma < 10)
        	gamma += 0.25f;
        if (getKeyboardReset('G') && gamma > 1)
        	gamma -= 0.25f;
        	
    }
    

    private float [] drawCustomCurve(AGraphics g, int[] xPoints, int [] yPoints, float dx0, float dy0, int start, int iterations, float gamma) {
    	float x0 = xPoints[start];
    	float y0 = yPoints[start];
    	float dist=0;
    		
    	float x1 = xPoints[start+1];
    	float y1 = yPoints[start+1];
    	float dx1=(x1-x0)/gamma, dy1=(y1-y0)/gamma;
    	if (false && start+2 < xPoints.length) {
    		dx1 = //x1+
    				(xPoints[start+2] - xPoints[start+1])/gamma;
    		dy1 = //y1+
    				(yPoints[start+2] - yPoints[start+1])/gamma;
    	}
    	
    	float dt = 1.0f/iterations;
    	float t = 0;
    	g.begin();
    	float lx=0, ly=0;
    	for (int i=0; i<=iterations; i++) {
    		float x = derive(x0, x1, dx0, dx1, t);
    		float y = derive(y0, y1, dy0, dy1, t);
    		t += dt;
    		g.vertex(x, y);
    		if (i > 0) {
    			float dx = x-lx;
    			float dy = y-ly;
    			dist += Math.sqrt(dx*dx+dy*dy);
    		}
    		lx=x;
    		ly=y;
    		
    	}    
    	g.drawLineStrip();
    	return new float [] { dx1, dy1, dist };
	}

    float derive(float p0, float p1, float dp0, float dp1, float t) {
	    float a0 = 2*p0 - 2*p1 + dp0 + dp1;
		float a1 = -3*p0 + 3*p1 - 2*dp0 - dp1;
		float a2 = 0 + 0 + dp0 + 0;
		float a3 = p0 + 0 + 0 + 0;
		return a0 * (t*t*t) + a1 * (t*t) + a2*t + a3;
    }
	void drawRect(AGraphics g, Vector2D a, Vector2D b, int thickness) {
        Vector2D min = a.min(b,  new MutableVector2D());
        Vector2D max = a.max(b,  new MutableVector2D());
        float w = max.X() - min.X();
        float h = max.Y() - min.Y();
        g.drawRect(min.X(), min.Y(), w, h, thickness);
    }
    
    @Override
    protected void drawFrame(AWTGraphics g) {
        g.ortho();
        g.clearScreen(g.WHITE);
        switch (mode) {
            case BSPLINE:
                drawBSpline(g); break;
            case GRAPH:
                drawGraph(g); break;
            case BEIZER:
                drawBeizer(g); break;
            case CUSTOM:
            	drawCustom(g); break;
                
        }
        drawHelp(g);
    }

    static class Point {
        
        public Point(Vector2D v) {
            this.x = v.X();
            this.y = v.Y();
        }

        final float x, y;
    }
    
    private void bsp(AGraphics g, Vector2D [] pts, int divisions) {
        float scale = 1.0f/6;
        /*
    a[0] = (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) / 6.0;
    b[0] = (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) / 6.0;
    
    a[1] = (3 * p0.x - 6 * p1.x + 3 * p2.x) / 6.0;
    b[1] = (3 * p0.y - 6 * p1.y + 3 * p2.y) / 6.0;
    
    a[2] = (-3 * p0.x + 3 * p2.x) / 6.0;
    b[2] = (-3 * p0.y + 3 * p2.y) / 6.0;

    a[3] = (p0.x + 4 * p1.x + p2.x) / 6.0;
    b[3] = (p0.y + 4 * p1.y + p2.y) / 6.0;         */
        
        Point p0 = new Point(pts[0]);
        Point p1 = new Point(pts[1]);
        Point p2 = new Point(pts[2]);
        Point p3 = new Point(pts[3]);
        
        Vector2D V0 = new MutableVector2D(    -p0.x + 3 * p1.x - 3 * p2.x +     p3.x,     -p0.y + 3 * p1.y - 3 * p2.y + p3.y).scaleEq(scale);
        Vector2D V1 = new MutableVector2D( 3 * p0.x - 6 * p1.x + 3 * p2.x           ,  3 * p0.y - 6 * p1.y + 3 * p2.y       ).scaleEq(scale);
        Vector2D V2 = new MutableVector2D(-3 * p0.x +            3 * p2.x           , -3 * p0.y +            3 * p2.y       ).scaleEq(scale);
        Vector2D V3 = new MutableVector2D(     p0.x + 4 * p1.x +     p2.x           ,      p0.y + 4 * p1.y +     p2.y       ).scaleEq(scale);
        
        g.vertex(V3);
        
        for (int i=1; i<=divisions; i++) {
            //float t0 = 1;
            float t = (float)i/divisions;
            //float t2 = t1*t1;
            //float t3 = t2*t1;

            //float x = t0*V0.X() + t1*V1.X() + t2*V2.X() + t3*V3.X();
            //float y = t0*V0.Y() + t1*V1.Y() + t2*V2.Y() + t3*V3.Y();
            
            Vector2D V = V0.scale(t).add(V1).scale(t).add(V2).scale(t).add(V3); 
                    
                    
                    //V2.add(V1.add(V0.scale(t)).scale(t)).scale(t)).add(V3);
            
            float x = (V2.X() + t * (V1.X() + t * V0.X()))*t+V3.X();
            float y = (V2.Y() + t * (V1.Y() + t * V0.Y()))*t+V3.Y();
            
            g.vertex(V);
            
        }
        
    }
    
    
    
    private void bsp_X(AGraphics g, float x0, float y0, float x1, float y1, float x2,
            float y2, float x3, float y3, final int divisions) {
        float [] a = new float[5];
        float [] b = new float[5];
        a[0] = (-x0 + 3 * x1 - 3 * x2 + x3) / 6.0f;
        a[1] = (3 * x0 - 6 * x1 + 3 * x2) / 6.0f;
        a[2] = (-3 * x0 + 3 * x2) / 6.0f;
        a[3] = (x0 + 4 * x1 + x2) / 6.0f;
        b[0] = (-y0 + 3 * y1 - 3 * y2 + y3) / 6.0f;
        b[1] = (3 * y0 - 6 * y1 + 3 * y2) / 6.0f;
        b[2] = (-3 * y0 + 3 * y2) / 6.0f;
        b[3] = (y0 + 4 * y1 + y2) / 6.0f;

        g.vertex(a[3], b[3]);
                ;
        for (int i = 1; i <= divisions - 1; i++)
        { 
            float t = (float)i / (float)divisions;
            g.vertex((a[2] + t * (a[1] + t * a[0]))*t+a[3], (b[2] + t * (b[1] + t * b[0]))*t+b[3]);
        }
        
    }

    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        float startX = 10;
        float endX = width-10;
        float y = height/2;
        
        float dx = (endX-startX) / graph.length;
        float x = startX;
        
        for (int i=0; i<graph.length; i++) {
            graph[i] = new MutableVector2D(x, y);
            x += dx;
        }
        
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        if (mode == Mode.BSPLINE || mode == Mode.CUSTOM) {
            super.mouseClicked(evt);
            addPoint(evt.getX(), evt.getY());
        }
    }

    boolean dragging = false;
    MutableVector2D boxStart = new MutableVector2D();
    MutableVector2D boxEnd   = new MutableVector2D();
    
    @Override
    public void mousePressed(MouseEvent evt) {
        super.mousePressed(evt);
        Vector2D P = new Vector2D(evt.getX(), evt.getY());
        boxStart.set(P);
        boxEnd.set(P);
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
        // TODO Auto-generated method stub
        super.mouseReleased(evt);
        if (dragging) {
            if (mode == Mode.GRAPH) {
                Vector2D min = boxStart.min(boxEnd, new MutableVector2D());
                Vector2D max = boxStart.max(boxEnd, new MutableVector2D());
                addGraphNoise(min, max);
            }
            
            
        }
        dragging = false;
    }

    private void addGraphNoise(Vector2D min, Vector2D max) {
        for (int i=0; i<graph.length; i++) {
            if (graph[i].X() >= min.X() && graph[i].X() < max.X()) {
                graph[i].setY(min.Y() + Utils.randFloat(max.Y()-min.Y()));
            }
        }
        
    }

    @Override
    public void mouseDragged(MouseEvent ev) {
        super.mouseDragged(ev);
        dragging = true;
        boxEnd.set(ev.getX(), ev.getY());
    }
    
    
}
