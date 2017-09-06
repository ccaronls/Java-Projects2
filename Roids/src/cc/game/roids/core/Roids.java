package cc.game.roids.core;

// must be careful to keep the core package free from android/awt ect. dependencies.

import java.io.*;
import java.util.*;

import cc.lib.game.*;
import cc.lib.math.*;

public class Roids {

    
    
    int WORLD_WIDTH = 100;
    int WORLD_HEIGHT = 100;
    
    int SCREEN_WIDTH = 100;
    int SCREEN_HEIGHT = 100; // this should be set based on aspect ratio
    
    List<Thingy> things = new ArrayList<Thingy>(256);
    List<Collision> collisions = new LinkedList<Collision>();
    
    //TestPolygon test = new TestPolygon();
    
    public Roids() {
        //things.add(makeRandomRock(20));
        //things.add(test);
        //things.add(new TestPolygon());
        //things.add(new TestPolygon());
    }
    
    Thingy makeRandomRock(float radius) {
        
        int numVerts = Utils.randRange(5, 10);
        
        MutableVector2D [] verts = new MutableVector2D[numVerts];
        
        float deg = 0;
        for (int i=0; i<numVerts; i++) {
            float l = Utils.randFloatX(radius/2) + radius/2; 
            verts[i] = new MutableVector2D(CMath.cosine(deg) * l, CMath.sine(deg) * l);
            deg += (360 / numVerts) + Utils.randRange(-10, 10);
        }
        
        Asteroid a = new Asteroid();
        a.setVertices(verts);
        
        return a;
    }
    
    float accumulator = 0;
    final float PHYSICS_TIME_STEP = 0.02f; //
    final float COEFF_RESTITUTION = 0.8f; // e
    final float MAX_ANG_VELOCITY = 50;
    final float MAX_VELOCITY = 50;
    
    int historySaveCounter = 0;
    
    public void updateAll(long curTime, long deltaTime) {

        somethingMoved = false;
        this.numHighlighed = 0;
        CMath.EPSILON = 0.01f;
        for (Thingy t: things) {
            t.update(this, (float)curTime/1000, (float)deltaTime/1000);
        }
        
        if (!pressed && !paused) {
            accumulator += (float)deltaTime/1000;
            
            while (accumulator >= PHYSICS_TIME_STEP) {
                somethingMoved = true;
                float T = (float)curTime / 1000 - accumulator;
                accumulator -= PHYSICS_TIME_STEP;
                historySaveCounter ++;
                for (Thingy t: things) {
                    t.updatePhysics(T, PHYSICS_TIME_STEP);
                    t.clampPosition(WORLD_WIDTH, WORLD_HEIGHT);
                    if (historySaveCounter % 10 == 0)
                        t.capture();
                }
                T += PHYSICS_TIME_STEP;
                computeCollisions();
                
                // process collisions
                for (Collision c: collisions) {
                    // backup source and target to their previous positions
                    c.source.updatePhysics(T, -PHYSICS_TIME_STEP);
                    c.target.updatePhysics(T, -PHYSICS_TIME_STEP);
                    //c.source.clampPosition(WORLD_WIDTH, WORLD_HEIGHT);
                    //c.target.clampPosition(WORLD_WIDTH, WORLD_HEIGHT);
                    collisionResponse(c);
                    c.source.updatePhysics(T, PHYSICS_TIME_STEP);
                    c.target.updatePhysics(T, PHYSICS_TIME_STEP);
                    c.source.clampPosition(WORLD_WIDTH, WORLD_HEIGHT);
                    c.target.clampPosition(WORLD_WIDTH, WORLD_HEIGHT);
                }
            }
        } else {
            computeCollisions();
        }
    }

    boolean somethingMoved = false;
    
    void computeCollisions() {
        if (somethingMoved) {
            // collision tests
            Collision c = new Collision();
            this.collisions.clear();
            for (int i=0; i<things.size()-1; i++) {
            //for (Thingy t : things) {
                //for (Thingy tt : things) {
                Thingy t = things.get(i);
                for (int ii=i+1; ii<things.size(); ii++) {
                    Thingy tt = things.get(ii);
                    if (collisionDetect(t, tt, c)) {
                        collisions.add(c);
                        c = new Collision();
                    }
                }
            }
        }
    }
    
    public boolean collisionDetectObjectSpecific(Thingy a, Thingy b, Collision c) {

        float d0 = a.getPosition().sub(b.getPosition(), MutableVector2D.newTemp()).magSquared();
        float d1 = a.getRadius() + b.getRadius();
        
        if (d0 > d1*d1)
            return false;
        
        if (a instanceof PolygonThingy) {
            if (b instanceof PolygonThingy) {
                if (PolygonThingy.collisionDetect((PolygonThingy)a, (PolygonThingy)b, c))
                    return true;
            }
        }
        return false;
    }
    
    public boolean collisionDetect(Thingy a, Thingy b, Collision c) {
        boolean collided = false;
        if (a != b) {
            if (collisionDetectObjectSpecific(a, b, c))
                return true;
            
            // since we are wrapping in the world, we have to see if we are on an edge and therefore overlapping against the 
            // other side.
            MutableVector2D ta = Vector2D.newTemp(a.getPosition());
            MutableVector2D tb = Vector2D.newTemp(b.getPosition());
            
            do { // just sumtin to break out uv
                
                boolean changed = false;
                if (a.getX() + a.getRadius() > WORLD_WIDTH) {
                    a.getPosition().setX(ta.getX() - WORLD_WIDTH*2);
                    changed = true;
                    
                } else if (a.getX() - a.getRadius() < -WORLD_WIDTH) {
                    a.getPosition().setX(ta.getX() + WORLD_WIDTH*2);
                    changed = true;
                }
                
                if (a.getY() + a.getRadius() > WORLD_HEIGHT) {
                    a.getPosition().setY(ta.getY() - WORLD_HEIGHT*2);
                    changed = true;
                } else if (a.getY() - a.getRadius() < -WORLD_HEIGHT) {
                    a.getPosition().setY(ta.getY() + WORLD_HEIGHT*2);
                    changed = true;
                }
                
                if (changed) {
                    if (collided = collisionDetectObjectSpecific(a, b, c))
                        break;
                    
                }

                changed = false;
                if (b.getX() + b.getRadius() > WORLD_WIDTH) {
                    b.getPosition().setX(tb.getX() - WORLD_WIDTH*2);
                    changed = true;
                    
                } else if (b.getX() - b.getRadius() < -WORLD_WIDTH) {
                    b.getPosition().setX(tb.getX() + WORLD_WIDTH*2);
                    changed = true;
                }
                
                if (b.getY() + b.getRadius() > WORLD_HEIGHT) {
                    b.getPosition().setY(tb.getY() - WORLD_HEIGHT*2);
                    changed = true;
                } else if (b.getY() - b.getRadius() < -WORLD_HEIGHT) {
                    b.getPosition().setY(tb.getY() + WORLD_HEIGHT*2);
                    changed = true;
                }
                
                if (changed) {
                    if (collided = collisionDetectObjectSpecific(a, b, c))
                        break;
                }

            } while (false);
            
            a.getPosition().set(ta);
            b.getPosition().set(tb);
        }
        return collided;
    }
    
    public void drawAll(AGraphics g) {
        g.clearScreen(g.BLACK);
        g.ortho(-100, 100, 100, -100);
        g.setIdentity();
        
        //test.highlighted = test.isPointInsidePolygon(mx, my);
        
        float energy = 0;
        for (Thingy t : things) {
            t.draw(g);
            //energy.addEq(t.getVelocity());
            energy += t.getVelocity().mag() + Math.abs(t.getAngVelocity() * CMath.DEG_TO_RAD);
        }
        
        for (Collision c: collisions) {
            c.draw(g);
        }
        
        //g.drawString("" + mx + "," + my, mx, my);
        g.ortho();
        g.setIdentity();
        g.setColor(g.WHITE);
        final int vTextSpacing = g.getTextHeight() + 3;
        int ty = 5;
        g.drawJustifiedString(5, ty, Justify.LEFT, Justify.TOP, "System Energy: " + energy);
        ty += vTextSpacing;
        
        if (energy > 1000) {
            paused = true;
        }
        
        if (isPaused()) {
            g.drawJustifiedString(5, ty, "PAUSED");
            ty += vTextSpacing;
        }
    }
    
    float mx=1000, my=1000, mdx=0, mdy=0;
    float sw, sh;
    boolean pressed = false;
    public boolean paused = false;
    int numHighlighed = 0;
    
    public void setScreenDimension(int width, int height) {
        this.sw = width;
        this.sh = height;
    }
    
    public void setPointer(int screenX, int screenY, int screenDX, int screenDY, boolean pressed) {
        mx = 200f/sw * screenX - 100;
        my = 100 - 200f/sh * screenY;
        this.pressed = pressed;
    }
    
    float getPointerX() {
        return mx;
    }
    
    float getPointerY() {
        return my;
    }
    
    MutableVector2D getPointer() {
        return Vector2D.newTemp(mx, my);
    }
    
    Vector2D getPointerDelta() {
        return Vector2D.newTemp(mdx,mdy);
    }
    
    boolean getPointerPressed() {
        return this.pressed;
    }

    public void toggleRotations() {
        for (Thingy t: things) {
            if (t instanceof TestPolygon) {
                ((TestPolygon)t).toggleRotations();
            }
        }
    }
    
    void collisionResponse(Collision c) {
        if (pauseOnCollision)
            paused = true;
        System.out.println("Process collision : " +c);
        Thingy t1 = c.source;
        Thingy t2 = c.target;
        Vector2D P = c.collisionPoint;
        Vector2D N = c.planeNormal; 
        // From: http://en.wikipedia.org/wiki/Collision_response
        
        System.out.println("Pre collision:" + "\n   src p=" + (c.sourcePos) + " v=" + (t1.getVelocity()) + " av=" + t1.getAngVelocity()
                                            + "\n   tgt p=" + (c.targetPos) + " v=" + (t2.getVelocity()) + " av=" + t2.getAngVelocity());
        
        
        // Ri is the radius vector from ti to point of collision
        Vector2D R1 = P.sub(c.sourcePos, c.R1);//Vector2D.newTemp());
        Vector2D R2 = P.sub(c.targetPos, c.R2);//Vector2D.newTemp());
        
        // Vpi is the relative velocity at the point of collision given both linear and angular velocities
        // Vpi = Vi + Wi X Ri
        Vector2D Vp1 = R1.cross(t1.getAngVelocity()*CMath.DEG_TO_RAD, c.Vp1).scaleEq(-1).addEq(t1.getVelocity());
        Vector2D Vp2 = R2.cross(t2.getAngVelocity()*CMath.DEG_TO_RAD, c.Vp2).scaleEq(-1).addEq(t2.getVelocity());
        // Vr = Vp2 - Vp1
        Vector2D Vr = Vp2.sub(Vp1, Vector2D.newTemp());
        // Step 1.
        //                 -(1 + e)Vr.N
        //Jr = -----------------------------------------------------------
        //     m1^-1 + m2^-1 + (I1^-1(R1 X N) X R1 + I2^-1(R2 X N) X R2).N

        float numer = Vr.dot(N) * -(1.0f + COEFF_RESTITUTION);
        
        MutableVector2D A = R1.cross(R1.cross(N), Vector2D.newTemp()).scaleEq(t1.getInvInertia());
        MutableVector2D B = R2.cross(R2.cross(N), Vector2D.newTemp()).scaleEq(t2.getInvInertia());
        
        float denom = t1.getInvMass() + t2.getInvMass() + (A.addEq(B)).dot(N); 
        
        if (Math.abs(denom) < 0.01) {
            // error out?
            System.out.println("Too small");
        }
        float jr = numer / denom;
        System.out.println("jr=" + jr);
        //jr = Utils.clamp(jr, -20, 20);
        // Step 2.
        // Compute Jr = jr * N
        Vector2D Jr = N.scale(jr, c.Jr);
        
        // Step 3.
        // compute refined velocity vectors
        //
        //  V'i = Vi -/+ N(jr/mi)
        MutableVector2D V1 = t1.getVelocity();
        MutableVector2D V2 = t2.getVelocity();

        V1.subEq(N.scale(jr*t1.getInvMass(), Vector2D.newTemp()));
        V2.addEq(N.scale(jr*t2.getInvMass(), Vector2D.newTemp()));
        
        if (V1.magSquared() > MAX_VELOCITY*MAX_VELOCITY) {
            V1.unitLengthEq().scaleEq(MAX_VELOCITY);
        }
        if (V2.magSquared() > MAX_VELOCITY*MAX_VELOCITY) {
            V2.unitLengthEq().scaleEq(MAX_VELOCITY);
        }
        
        // Step 4.
        // Compute new angular velocities 
        // W'i = Wi -/+ jr*Ii^-1(Ri X N)
        //float newAng1 = t1.getAngVelocity()*Utils.DEG_TO_RAD - t1.getInvInertia() * R1.cross(Jr);
        //float newAng2 = t2.getAngVelocity()*Utils.DEG_TO_RAD + t2.getInvInertia() * R2.cross(Jr);

        float newAng1 = t1.getAngVelocity()*CMath.DEG_TO_RAD - jr*t1.getInvInertia() * R1.cross(N);
        float newAng2 = t2.getAngVelocity()*CMath.DEG_TO_RAD + jr*t2.getInvInertia() * R2.cross(N);
        
        t1.setAngVelocity(Utils.clamp(newAng1 * CMath.RAD_TO_DEG, -MAX_ANG_VELOCITY, MAX_ANG_VELOCITY));
        t2.setAngVelocity(Utils.clamp(newAng2 * CMath.RAD_TO_DEG, -MAX_ANG_VELOCITY, MAX_ANG_VELOCITY));
        
        System.out.println("Post collision:" + "\n   src v=" + (t1.getVelocity()) + " av=" + t1.getAngVelocity()
                + "\n   tgt v=" + (t2.getVelocity()) + " av=" + t2.getAngVelocity());

    }

    public void clearThingys() {
        things.clear();
    }

    public void doTest1() {
        clearThingys();
        Thingy t = makeRect(20);
        t.setPosition(1, -22);
        t.setVelocity(0,1);
        things.add(t);
        t = makeRect(20);
        t.setPosition(-1, 22);
        things.add(t);
        t.setVelocity(0,-1);
    }

    public void doTest4() {
        clearThingys();
        Thingy t = makeRect(20);
        t.setPosition(0, -22);
        t.setVelocity(0,1);
        t.setOrientation(45);
        things.add(t);
        t = makeRect(20);
        t.setPosition(0, 22);
        things.add(t);
        t.setVelocity(0,-1);
        t.setOrientation(45);
    }

    public void doTest2() {
        clearThingys();
        Thingy t = makeRect(20);
        t.setPosition(1, -22);
        t.setAngVelocity(10);
        t.setVelocity(0,1);
        things.add(t);
        t = makeRect(20);
        t.setPosition(-1, 22);
        things.add(t);
        t.setVelocity(0,-1);
        t.setAngVelocity(10);
    }
    
    public void doTest3() {
        clearThingys();
        Thingy t = makeRect(20);
        t.setPosition(-15, -22);
        //t.setAngVelocity(10);
        t.setVelocity(0,3);
        things.add(t);
        t = makeRect(20);
        t.setPosition(15, -22);
        //t.setAngVelocity(10);
        t.setVelocity(0,3);
        things.add(t);
        t = makeRect(20);
        t.setPosition(-1, 22);
        things.add(t);
        t.setVelocity(0,-3);
        //t.setAngVelocity(10);
    }

    public void addTestThingy() {
        Thingy t = makePoly(Utils.randFloat(10) + 10);
        t.setVelocity(Utils.randFloatX(5), Utils.randFloatX(5));
        t.setAngVelocity(Utils.randFloatX(5));
        things.add(t);
    }

    public TestPolygon makeRect(float radius) {
        TestPolygon p = new TestPolygon();
        MutableVector2D [] verts = {
                new MutableVector2D(0,0),
                new MutableVector2D(1, 0),
                new MutableVector2D(1,1),
                new MutableVector2D(0,1)
        };
        p.setVertices(verts);
        p.setRadius(radius);
        return p;
    }
    
    public TestPolygon makePoly(float radius) {
        TestPolygon p = new TestPolygon();
        int numVerts = Utils.randRange(5, 10);
        
        MutableVector2D [] verts = new MutableVector2D[numVerts];

        for (int i=0; i<numVerts; i++) {
            float deg = (360f / numVerts) * i + Utils.randRange(-10, 10);
            float l = Utils.randFloat(radius/2) + radius/2; 
            verts[i] = new MutableVector2D(CMath.cosine(deg) * l, CMath.sine(deg) * l);
        }
        p.setVertices(verts);
        p.setOrientation(Utils.randFloat(359));
        return p;
    }

    public enum DragMode {
        POSITION,
        VELOCITY,
        ORIENTATION,
        ANGVELOCITY,
    }

    public DragMode dragMode = DragMode.POSITION;
    public boolean pauseOnCollision = false;
    
    public void togglePause() {
        paused = !paused;
    }
    
    public boolean isPaused() {
        return paused;
    }

    public void historyStep(int amt) {
        for (Thingy t: things) {
            if (amt < 0)
                t.historyRewind();
            else
                t.historyForward();
        }
    }

    public void saveStateToFile(String fileName) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(fileName));
            out.writeObject(things);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (out != null)
                out.close();
        } catch (Exception e) {}
    }

    public void restoreStateFromFile(String fileName) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(fileName));
            List<Thingy> things = (List<Thingy>)in.readObject(); 
            this.things = things;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (in != null)
                in.close();
        } catch (Exception e) {}
    }

	public void deleteSelected() {
		
	}
}
