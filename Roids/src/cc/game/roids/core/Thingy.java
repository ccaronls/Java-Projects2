package cc.game.roids.core;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.math.*;

public abstract class Thingy implements Serializable {

    protected MutableVector2D position = new MutableVector2D();
    protected MutableVector2D velocity = new MutableVector2D();
    protected float orientation;
    protected float angVelocity;
    private float mass=1, invMass=1;
    private float inertia, invInertia;
    protected float radius = 0;
    private transient LinkedList<History> history = new LinkedList();
    private transient int historyPos = 0;
        
    
    protected void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(position);
        out.writeObject(velocity);
        out.writeFloat(orientation);
        out.writeFloat(angVelocity);
        out.writeFloat(radius);
        out.writeFloat(mass);
    }
        
    protected void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        position.set((Vector2D)in.readObject());
        velocity.set((Vector2D)in.readObject());
        orientation = in.readFloat();
        angVelocity = in.readFloat();
        radius = in.readFloat();
        setMass(in.readFloat());
        history = new LinkedList();
    }
        
    private void readObjectNoData() throws ObjectStreamException {
        
    }

    
    private static class History {
        final Vector2D pos;
        final Vector2D vec;
        final float orientation, angVelocity;
        
        public History(Vector2D pos, Vector2D vec, float orientation, float angVelocity) {
            super();
            this.pos = new Vector2D(pos);
            this.vec = new Vector2D(vec);
            this.orientation = orientation;
            this.angVelocity = angVelocity;
        }
        
        
    }
    
    void capture() {
        if (history == null)
            history = new LinkedList();
        History h = new History(position, velocity, orientation, angVelocity);
        while (history.size() > historyPos)
            history.removeLast();
        history.addLast(h);
        if (history.size() > 100)
            history.removeFirst();
        historyPos = history.size();
    }
    
    void historyRewind() {
        if (historyPos > 0) {
            History h = history.get(--historyPos);
            this.position.set(h.pos);
            this.velocity.set(h.vec);
            this.orientation = h.orientation;
            this.angVelocity = h.angVelocity;
        }
    }
    
    void historyForward() {
        if (historyPos < history.size()) {
            History h = history.get(historyPos++);
            this.position.set(h.pos);
            this.velocity.set(h.vec);
            this.orientation = h.orientation;
            this.angVelocity = h.angVelocity;
        }
    }
    
    final void setInertia(float i) {
        this.inertia = i;
        if (inertia >= 1)
            this.invInertia = 1.0f / i;
        else
            this.invInertia = 1;
    }
    
    final void setMass(float m) {
        this.mass = m;
        if (mass >= 1)
            this.invMass = 1.0f / m;
        else
            this.invMass = 1;
        computeInertia();
    }
    
    final float getX() {
        return position.getX();
    }

    final float getY() {
        return position.getY();
    }
    
    final void setPosition(float x, float y) {
        position.set(x,y);
    }
    
    final void setVelocity(float dx, float dy) {
        velocity.set(dx, dy);
    }

    abstract void computeInertia();
    
    final public float getOrientation() {
        return orientation;
    }
    
    final public void setOrientation(float orientation) {
        this.orientation = orientation;
        while (orientation < 0)
            orientation += 359;
        while (orientation > 359)
            orientation -= 359;
    }
    
    final MutableVector2D getPosition() {
        return position;
    }
    
    final MutableVector2D getVelocity() {
        return velocity;
    }
    
    final float getAngVelocity() {
        return this.angVelocity;
    }
    
    final void setAngVelocity(float angV) {
        this.angVelocity = angV;
    }
    
    final void updatePhysics(float t, float dt) {
    	MutableVector2D dv = Vector2D.newTemp(velocity);
        dv.scaleEq(dt);
        position.addEq(dv);
        setOrientation(orientation += angVelocity * dt);
    }
    
    final void clampPosition(int maxX, int maxY) {
        float x = position.getX();
        float y = position.getY();
        while (x < -maxX) {
            x += (maxX*2);
        }
        while (x > maxX) {
            x -= maxX*2;
        }
        while (y < -maxY) {
            y += (maxY*2);
        }
        while (y > maxY) {
            y -= maxY*2;
        }
        position.set(x,y);
    }

    /*
    void updatePhysics(float t, float dt) {
        State x = new State(); // create states for x,y, and rotation
        State y = new State();
        State r = new State();
        this.integrate(x, t, dt);
        this.integrate(y, t, dt);
        this.integrate(r, t, dt);
    }*/
    
    abstract void update(Roids roids, float curTimeSeconds, float deltaTimeSeconds);
    
    abstract void draw(AGraphics g);

    final float getMass() {
        return mass;
    }
    
    final float getInvMass() {
        return invMass;
    }
    
    final float getInertia() {
        return inertia;
    }
    
    final float getInvInertia() {
        return invInertia;
    }
    
    final float getRadius() {
        return radius;
    }
    
    void setRadius(float radius) {
        this.radius = radius;
    }
    
    static class State
    {
         float x;          // position
         float v;          // velocity
    };
    
    static class Derivative
    {
         float dx;          // derivative of position: velocity
         float dv;          // derivative of velocity: acceleration
    };
    
    float acceleration(State state, float t) {
        return 0;
    }
    
    Derivative evaluate(State initial, float t, float dt, Derivative d)
    {
         State state = new State();
         state.x = initial.x + d.dx*dt;
         state.v = initial.v + d.dv*dt;

         Derivative output = new Derivative();
         output.dx = state.v;
         output.dv = acceleration(state, t+dt);
         return output;
    }
    
    void integrate(State state, float t, float dt)
    {
         Derivative a = evaluate(state, t, 0.0f, new Derivative());
         Derivative b = evaluate(state, t, dt*0.5f, a);
         Derivative c = evaluate(state, t, dt*0.5f, b);
         Derivative d = evaluate(state, t, dt, c);

         float dxdt = 1.0f/6.0f * (a.dx + 2.0f*(b.dx + c.dx) + d.dx);
         float dvdt = 1.0f/6.0f * (a.dv + 2.0f*(b.dv + c.dv) + d.dv);

         state.x = state.x + dxdt * dt;
         state.v = state.v + dvdt * dt;
    }
    
    /*
    static class State {
        final Vector2D x = new float[2]; // position
        final Vector2D v = new float[2]; // velocity
    }
    
    static class Derivative {
        final Vector2D dx = new float[2]; // derivative of position
        final Vector2D dv = new float[2]; // derivative of velocity
    }

    void acceleration(State state, float t, Vector2D result) {
        Vector2D.add(collisionForce, result, result);
        Vector2D.assign(0, 0, collisionForce);
    }
    
    private Derivative evaluate(final State initial, float t, float dt, Derivative d)
    {
         State state = new State();
         Vector2D tmp = new float[2];
         //state.x = initial.x + d.dx*dt;
         Vector2D.scale(d.dx, dt, tmp);
         Vector2D.add(initial.x, tmp, state.x);
         //state.v = initial.v + d.dv*dt;
         Vector2D.scale(d.dv, dt, tmp);
         Vector2D.add(initial.v, tmp, state.v);

         Derivative output = new Derivative();
         //output.dx = state.v;
         Vector2D.copy(state.v, output.dx);
         //output.dv = acceleration(state, t+dt);
         acceleration(state, t+dt, output.dv);
         return output;
    }
    
    private void integrate(State initial, float t, float dt)
    {
        State state = new State();
         Derivative a = evaluate(state, t, 0.0f, new Derivative());
         Derivative b = evaluate(state, t, dt*0.5f, a);
         Derivative c = evaluate(state, t, dt*0.5f, b);
         Derivative d = evaluate(state, t, dt, c);

         //final float dxdt = 1.0f/6.0f * (a.dx + 2.0f*(b.dx + c.dx) + d.dx);
         Vector2D dxdt = new float[2];
         Vector2D.add(b.dx, c.dx, dxdt);
         Vector2D.scale(dxdt, 2);
         Vector2D.add(a.dx, dxdt, dxdt);
         Vector2D.add(d.dx, dxdt, dxdt);
         Vector2D.scale(dxdt, 1.0f/6.0f);
         
         //final float dvdt = 1.0f/6.0f * (a.dv + 2.0f*(b.dv + c.dv) + d.dv);
         Vector2D dvdt = new float[2];
         Vector2D.add(b.dv, c.dv, dvdt);
         Vector2D.scale(dvdt, 2);
         Vector2D.add(a.dv, dvdt, dvdt);
         Vector2D.add(d.dv, dvdt, dvdt);
         Vector2D.scale(dvdt, 1.0f/6.0f);

         //state.x = state.x + dxdt * dt;
         //state.v = state.v + dvdt * dt;
         Vector2D.scale(dxdt, dt);
         Vector2D.add(state.x, dxdt, state.x);
         Vector2D.scale(dvdt, dt);
         Vector2D.add(state.v, dvdt, state.v);
    }*/
}
