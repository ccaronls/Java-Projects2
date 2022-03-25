package cc.app.fractal;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.HashMap;

import cc.app.fractal.evaluator.Evaluator;
import cc.lib.math.ComplexNumber;

public class TestEvaluator extends TestCase {

    ComplexNumber a = new ComplexNumber(1,1);
    ComplexNumber b = new ComplexNumber(2,2);
    ComplexNumber c = new ComplexNumber(3,3);
    ComplexNumber d = new ComplexNumber(0.1,0.02);

    ComplexNumber t1 = new ComplexNumber();
    ComplexNumber t2 = new ComplexNumber();

    HashMap<String, ComplexNumber> vars = new HashMap<String, ComplexNumber>();
    Evaluator e = new Evaluator();

    public void testAddition() throws Exception {
        ComplexNumber t0 = a.add(b);
        String expr = a + " + " + b;
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testSubtraction() throws Exception {
    	ComplexNumber t0 = a.sub(b);
        String expr = a + " - " + b;
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testMultiplication() throws Exception {
    	ComplexNumber t0 = a.multiply(b);
        String expr = a + " * " + b;
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }
    
    public void testDivision() throws Exception {
    	ComplexNumber t0 = a.divide(b);
        String expr = a + " / " + b;
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testPow() throws Exception {
    	ComplexNumber t0 = a.powi(2);
        String expr = a + " ^2 ";// + b;
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }
    
    public void testPow2() throws Exception {
    	ComplexNumber t0 = a.powi(3);
        String expr = a + " ^3 ";// + b;
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testPowi() throws Exception {
        ComplexNumber t = new ComplexNumber(2314231.7563546,235243.234543);
        ComplexNumber t0 = t.multiply(t);
        ComplexNumber t1 = t.powi(2);
        assertEquals(t0, t1);
    }

/*
    public void testChain1() throws Exception {
        String expr = a + " ^2 + " + b;
        ComplexNumber t0 = a.powi(2);
        t1.add(b, t0);
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testChain2() throws Exception {
        Evaluator e = new Evaluator();
        String expr = a + " ^2 + " + b + " ^2";
        a.powi(2, t1);
        b.powi(2, t2);
        t1.add(t2, t0);
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testChain3() throws Exception {
        String expr = a + " ^2 + " + b + " ^3";
        a.powi(2, t1);
        b.powi(3, t2);
        t1.add(t2, t0);
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void test1() throws Exception {
        String expr = "([1,1] + [2,2]) / [3,3] * [0.1,0.02]";
        

        a.add(b, t0);
        t0.divide(c, t1);
        t1.multiply(d, t0);
        
        e.parse(expr);
        t1 = e.evaluate();
        
        assertEquals(t0,t1);
    }
    
    
    public void xtest() throws Exception {
        //e.parse("[1,-1] + [-2,2] - [3,-3]");
        String [] tests = { "[1,1] + [2,2] / [3,3] * [0.1,0.02]", "[0.01,0.23] ^ 3" }; 
        for (int i=0; i<tests.length; i++) {
            e.parse(tests[i]);
            e.debugDump();
            ComplexNumber c = e.evaluate();
            System.out.println(c);
        }
    }
    */

    public void testFractal() throws Exception {
        AFractal mandelbrot = new AFractal.Mandelbrot();
        AFractal custom = new AFractal.Custom("Z^2 + Z0");

        double x = -1;
        double y = -1;

        double d = 0.01;

        while (y < 1) {
            x = -1;
            while (x < 1) {
                int a = mandelbrot.processPixel(x, y, 256);
                int b = custom.processPixel(x, y, 256);
                Assert.assertEquals("Pixel at " + x + "," + y + " invalid", a, b);
                x += d;
            }
            y += d;
        }

    }
}
