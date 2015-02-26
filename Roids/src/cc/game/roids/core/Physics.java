package cc.game.roids.core;

import cc.game.roids.core.Thingy.Derivative;
import cc.game.roids.core.Thingy.State;

public abstract class Physics {

    // constant
    final float mass;
    final float inverseMass;
    final State initial;
    
    Physics(float position, float velocity, float mass) {
        initial = new State();
        initial.x = position;
        initial.setVelocity(velocity);
        this.mass = mass;
        this.inverseMass = 1.0f / mass;
    }
    
    final class State
    {
         // primary
         float x;
         float m;

         // secondary
         private float v;

         void setVelocity(float v) {
             m = v*mass;
             this.v = v;
         }
         
         void setMomentum(float m) {
             this.m = m;
             v= m*inverseMass;
         }
         
         void recalculate()
         {
              m = m * inverseMass;
         }
    };

    final class Derivative
    {
         float dx;
         float force;
    };
    
    /*
    static class State
    {
         float x;          // position
         float v;          // velocity
    };
    
    static class Derivative
    {
         float dx;          // derivative of position: velocity
         float dv;          // derivative of velocity: acceleration
    };*/
    
    abstract float acceleration(State state, float t);
    
    private Derivative evaluate(State initial, float t, float dt, Derivative d)
    {
         State state = new State();
         state.x = initial.x + d.dx*dt;
         state.v = initial.v + d.force*dt;

         Derivative output = new Derivative();
         output.dx = state.v;
         output.force = acceleration(state, t+dt);
         return output;
    }
    
    private void integrate(State state, float t, float dt)
    {
         Derivative a = evaluate(state, t, 0.0f, new Derivative());
         Derivative b = evaluate(state, t, dt*0.5f, a);
         Derivative c = evaluate(state, t, dt*0.5f, b);
         Derivative d = evaluate(state, t, dt, c);

         float dxdt = 1.0f/6.0f * (a.dx + 2.0f*(b.dx + c.dx) + d.dx);
         float dvdt = 1.0f/6.0f * (a.force + 2.0f*(b.force + c.force) + d.force);

         state.x = state.x + dxdt * dt;
         state.v = state.v + dvdt * dt;
    }
    
    
}
