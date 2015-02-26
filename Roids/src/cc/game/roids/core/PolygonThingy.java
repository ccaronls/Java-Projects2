package cc.game.roids.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import cc.lib.game.*;
import cc.lib.math.*;

public abstract class PolygonThingy extends Thingy {

    private static final long serialVersionUID = 7526472295622776147L;
    
    private MutableVector2D [] verts;
    private MutableVector2D [] boundingVerts;

    private int primary = 0; // farthest from origin
    private float surfaceArea = 1;
    
    protected void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(verts.length);
        for (Vector2D v: verts)
            out.writeObject(v);
        out.writeInt(boundingVerts.length);
        for (Vector2D v: boundingVerts)
            out.writeObject(v);
        super.writeObject(out);
    }
        
    protected void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        verts = new MutableVector2D[in.readInt()];
        for (int i=0; i<verts.length; i++)
            verts[i] = (MutableVector2D)in.readObject();
        boundingVerts = new MutableVector2D[in.readInt()];
        for (int i=0; i<boundingVerts.length; i++)
            boundingVerts[i] = (MutableVector2D)in.readObject();
        compute();
        super.readObject(in);
    }
    
    public void setVertices(MutableVector2D [] verts) {
        this.verts = verts;
        compute();
    }
    
    void computeInertia() {
        setInertia(getMass() * surfaceArea);
    }
    
    public void setRadius(float radius) {
        float scale = radius / this.radius;
        for (int i=0; i<verts.length; i++) {
            verts[i].scaleEq(scale);
        }
        for (int i=0; i<boundingVerts.length; i++) {
            boundingVerts[i].scaleEq(scale);
        }
        super.setRadius(radius);
        computeArea();
    }
    
    void renderBoundingVerts(AGraphics g) {
        g.begin();
        g.vertexArray(boundingVerts);
        g.drawLineLoop();
        g.end();
    }
    
    @Override
    void draw(AGraphics g) {
        g.pushMatrix();
        g.translate(getX(), getY());
        g.rotate(orientation);
        g.begin();
        g.vertexArray(verts);
        g.drawLineLoop();
        g.popMatrix();
    }
    
    /*
     * Determine if any of our points collide with target
     * 
     * @param target
     * @param info
     * @return
     */
    private boolean collisionTest(PolygonThingy target, Collision info) {
        // check if any of my points are inside target
        for (int i=0; i<this.boundingVerts.length; i++) {
        	MutableVector2D v = boundingVerts[i].rotate(orientation, Vector2D.newTemp()).addEq(position);
            if (target.isPointInsidePolygon(v)) {
                // collect collision info
                info.source = this;
                info.target = target;
                info.sourcePos.set(getPosition());
                info.targetPos.set(target.getPosition());
                info.collisionPoint.set(v);
                return computeBounceVectors(i, target, info);
            }
        }
        return false;
    }
    
    private boolean computeBounceVectors(int collisionPointIndex, PolygonThingy target, Collision info) {
     // we want to determine the type of collision:
        // edge/edge, edge/point, point/point
        
        //Utils.EPSILON = 0.01f;
        
        // create 2 matricies
        Matrix3x3 m0 = new Matrix3x3();
        Matrix3x3 m1 = new Matrix3x3();
        
        m0.setRotation(getOrientation());
        m0.translate(info.sourcePos);
        
        m1.setRotation(target.getOrientation());
        m1.translate(info.targetPos);

        // precompute all the points we need.  
        
        MutableVector2D[]  src = new MutableVector2D[3];

        final int prev = (collisionPointIndex-1+getNumBoundingVerts()) % getNumBoundingVerts();
        for (int i=0; i<3; i++) {
            int index = (prev+i) % getNumBoundingVerts();
            src[i] = Vector2D.newTemp(getXBoundingVert(index), getYBoundingVert(index));
            m0.transform(src[i]);
        }
        
        // now get the dest verts pre transformed
        MutableVector2D[] dst = new MutableVector2D[target.getNumBoundingVerts()];
        for (int i=0; i<target.getNumBoundingVerts(); i++) {
            dst[i] = Vector2D.newTemp(target.getXBoundingVert(i), target.getYBoundingVert(i));
            m1.transform(dst[i]);
        }
        
        int collisionIndex0 = -1;

        MutableVector2D e0= Vector2D.newTemp();
        MutableVector2D e1= Vector2D.newTemp();
        
        // see if the next point is also in the target
        if (target.isPointInsidePolygon(src[2])) {
            // here is a special case that is essentially and edge 2 edge collision
            System.out.println("Case S");
            src[1].sub(src[2], e0); // get the edge
            e0.norm(info.planeNormal).unitLengthEq();
            info.collisionPoint.addEq(e0.scaleEq(-0.5f));
            return true;
        }
        
        for (int i=0; i<src.length-1; i++) {
            // get the edge to test
            src[i+1].sub(src[i], e0);
            float x0 = src[i].getX();
            float y0 = src[i].getY();
            float x1 = src[i].getX() + e0.getX();
            float y1 = src[i].getY() + e0.getY();
            //int x0 = Math.round(src[i].getX());
            //int y0 = Math.round(src[i].getY());
            //int x1 = Math.round(src[i].getX() + e0.getX());
            //int y1 = Math.round(src[i].getY() + e0.getY());
            for (int ii=0; ii<dst.length; ii++) {
                int iii=(ii+1)%dst.length;
                dst[iii].sub(dst[ii], e1);
                float x2 = dst[ii].getX();
                float y2 = dst[ii].getY();
                float x3 = dst[ii].getX() + e1.getX();
                float y3 = dst[ii].getY() + e1.getY();
                //int x2 = Math.round(dst[ii].getX());
                //int y2 = Math.round(dst[ii].getY());
                //int x3 = Math.round(dst[ii].getX() + e1.getX());
                //int y3 = Math.round(dst[ii].getY() + e1.getY());
                switch (Utils.isLineSegsIntersecting(x0, y0, x1, y1, x2, y2, x3, y3)) {
                    case 0: break;
                    case 1: // intersecting
                        info.addSegment(Vector2D.newTemp(x0, y0).add(info.sourcePos), Vector2D.newTemp(x1, y1).add(info.sourcePos));
                        info.addSegment(Vector2D.newTemp(x2, y2).add(info.targetPos), Vector2D.newTemp(x3, y3).add(info.targetPos));
                        if (collisionIndex0 < 0) {
                            // found 1 set of intersections
                            System.out.print("Case A->");
                            collisionIndex0 = ii;
                        } else if (ii == collisionIndex0) {
                            // 2 of my edges are intersecting a single edge of target
                            System.out.println("1B");
                            
                            e1.unitLengthEq();
                            e1.norm(info.planeNormal);
                            return true;
                        } else {
                            System.out.println("1C");
                            
                            // this is a point to point collision, so the normal is just the normalized vector between the objects centers
                            info.targetPos.sub(info.sourcePos, info.planeNormal).unitLengthEq();                            
                            /*
                            Vector2D c0 = Vector2D.newTemp(x0, y0);
                            Vector2D c1 = Vector2D.newTemp(x3, y3);
                            info.addSegment(x0, y0, x3, y3);
                            Vector2D t = c0.sub(c1, Vector2D.newTemp());
                            float m = t.mag();
                            if (m > Utils.EPSILON) {
                                t.norm(info.planeNormal).scaleEq(1.0f / m);
                            } else {
                                // c0 and c1 are on top of each other, so need to choose a different norm
                                src[0].sub(src[1], e0).unitLengthEq();
                                src[2].sub(src[1], e1).unitLengthEq();
                                e0.subEq(e1).norm(info.planeNormal).unitLengthEq();
                            }
                            //info.collisionPoint.subEq(t.scaleEq(0.5f));
                            info.collisionPoint.set(c0).subEq(t.scaleEq(0.5f));
                            info.collisionPoint.sub(info.sourcePos, t);
                            if (info.planeNormal.dot(t) < 0)
                                info.planeNormal.scaleEq(-1);
                                */
                            return true;
                        }
                        // we have a point 
                        // bounce vector is the sum of the edges of the point
                        break;
                    case 2: {
                        info.addSegment(Vector2D.newTemp(x0, y0).add(info.sourcePos), Vector2D.newTemp(x1, y1).add(info.sourcePos));
                        System.out.println("Case 2");
                        // parallel and coincident
                        // here the bounce vectors are the normal to the edge (either).  Then we stop searching 
                        info.addSegment(Vector2D.newTemp(x2, y2).add(info.targetPos), Vector2D.newTemp(x3, y3).add(info.targetPos));
                        info.planeNormal.set(e0.normEq().unitLengthEq());
                        return true;
                    }
                }
            }
        }
        
        if (collisionIndex0 >= 0) {
            return true;
        }
        
        return false;
    }
    
    static boolean collisionDetect(PolygonThingy a, PolygonThingy b, Collision info) {
        return a.collisionTest(b, info) || b.collisionTest(a, info);        
    }

    boolean isPointInsidePolygon(float x, float y) {
        return isPointInsidePolygon(Vector2D.newTemp(x, y));
    }
    
    boolean isPointInsidePolygon(Vector2D v) {

        if (Utils.distSqPointPoint(getX(), getY(), v.getX(), v.getY()) > radius*radius)
            return false;

        int orient = 0;
  
        MutableVector2D v2 = Vector2D.newTemp();
        MutableVector2D v3 = Vector2D.newTemp();
        MutableVector2D dv0 = Vector2D.newTemp();
        MutableVector2D dv1 = Vector2D.newTemp();
        MutableVector2D n = Vector2D.newTemp();
        
        int numPts = boundingVerts.length;
        
        for (int i=0; i<numPts; i++) {
            boundingVerts[i].rotate(orientation, v2).addEq(position);
            v.sub(v2, dv0);
            
            int ii = (i+1) % numPts;
            boundingVerts[ii].rotate(orientation, v3).addEq(position);
            v3.sub(v2, dv1);
            
            dv1.norm(n);
            
            float dot = dv0.dot(n);
            
            if (Math.abs(dot) < Utils.EPSILON) {
                // ignore since this is 'on' the segment
            }
            else if (dot < 0) {
                // return false if orientation changes
                if (orient > 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = -1;
            } else {
                // return false if orientation changes
                if (orient < 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = 1;
            }
        }                
        
        return true;
    }

    /*
     * Internal.  Center, compute radius and find the bounding polygon.
     */
    private void compute() {
        
        final int numVerts = verts.length;
        
        // center the polygon
        MutableVector2D cntr = Vector2D.newTemp();
        
        for (int i=0; i<numVerts; i++) {
            cntr.addEq(verts[i]);
        }

        cntr.scaleEq(1.0f / numVerts);

        radius = 0;
        primary = -1;
        for (int i=0; i<numVerts; i++) {
            verts[i].subEq(cntr);
            
            float r = verts[i].magSquared();
            if (r > radius) {
                radius = r;
                primary = i;
            }
        }

        radius = (float)Math.sqrt(radius);
        
        // compute the bounding polygon using giftwrap algorithm
        
        // start at the primary (longest) vertex from the center since this must be on the bounding rect
        List<MutableVector2D> newV = new ArrayList<MutableVector2D>(32);
        
        newV.add(new MutableVector2D(verts[primary]));
        
        int start = primary;//(primary+1) % numVerts;
        
        MutableVector2D dv = Vector2D.newTemp();
        MutableVector2D vv = Vector2D.newTemp();
        try {
            do {
                verts[start].scale(-1, dv);
                float best = 0;
                int next = -1;
                for (int i=(start+1)%numVerts; i!=start; i = (i+1)%numVerts) {
                    verts[i].sub(verts[start], vv);
                    float angle = vv.angleBetween(dv);
                    if (angle > best) {
                        best = angle;
                        next = i;
                    } else {
                        break;
                    }
                }
                Utils.assertTrue(next >= 0);
                if (next != primary) {
                    newV.add(new MutableVector2D(verts[next]));
                }
                //Utils.assertTrue(xv.size() <= numVerts);
                start = next;
            } while (start != primary && newV.size() < numVerts);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // the bounding verts are a subset of the verts
        this.boundingVerts = new MutableVector2D[newV.size()];
        
        for (int i=0; i<newV.size(); i++) {
            boundingVerts[i] = newV.get(i);
        }
        computeArea();
    }
    
    void computeArea() {
        // compute the area of the bounding vertices since these are garanteed to be simple closed loop
        double area = 0;
        for (int i=0; i<getNumBoundingVerts(); i++) {
            int ii = (i+1) % getNumBoundingVerts();
            
            float base = boundingVerts[ii].mag();
            float height  = boundingVerts[i].norm(Vector2D.newTemp()).unitLengthEq().dot(boundingVerts[ii]);
            
            area += 0.5f * height * base;
        }
        this.surfaceArea = (float)area;
        computeInertia();
    }
    
    public int getNumVerts() {
        return verts.length;
    }
    
    public float getXVert(int index) {
        return verts[index].getX();
    }
    
    public float getYVert(int index) {
        return verts[index].getY();
    }
    
    public int getNumBoundingVerts() {
        return boundingVerts.length;
    }
    
    public float getXBoundingVert(int index) {
        return this.boundingVerts[index].getX();
    }

    public float getYBoundingVert(int index) {
        return this.boundingVerts[index].getY();
    }

    /**
     * The primary vertex is the vertex that is farthest from the origin.
     * @return
     */
    public int getPrimaryVert() {
        return this.primary;
    }
    
    final public float getSurfaceArea() {
        return this.surfaceArea;
    }
}
