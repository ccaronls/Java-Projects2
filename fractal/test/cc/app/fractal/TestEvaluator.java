package cc.app.fractal;

import java.util.HashMap;

import cc.app.fractal.evaluator.Evaluator;
import cc.lib.math.ComplexNumber;
import junit.framework.TestCase;

public class TestEvaluator extends TestCase {

    ComplexNumber a = new ComplexNumber(1,1);
    ComplexNumber b = new ComplexNumber(2,2);
    ComplexNumber c = new ComplexNumber(3,3);
    ComplexNumber d = new ComplexNumber(0.1,0.02);

    ComplexNumber t0 = new ComplexNumber();
    ComplexNumber t1 = new ComplexNumber();
    ComplexNumber t2 = new ComplexNumber();

    HashMap<String, ComplexNumber> vars = new HashMap<String, ComplexNumber>();
    Evaluator e = new Evaluator();

    public void testAddition() throws Exception {
        a.add(b, t0);
        String expr = a.toString() + " + " + b.toString();
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testSubtraction() throws Exception {
        a.sub(b, t0);
        String expr = a.toString() + " - " + b.toString();
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testMultiplication() throws Exception {
        a.multiply(b, t0);
        String expr = a.toString() + " * " + b.toString();
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }
    
    public void testDivision() throws Exception {
        a.divide(b, t0);
        String expr = a.toString() + " / " + b.toString();
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testPow() throws Exception {
        a.powi(2, t0);
        String expr = a.toString() + " ^2 ";// + b.toString();
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }
    
    public void testPow2() throws Exception {
        a.powi(3, t0);
        String expr = a.toString() + " ^3 ";// + b.toString();
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testChain1() throws Exception {
        String expr = a.toString() + " ^2 + " + b.toString();
        a.powi(2, t1);
        t1.add(b, t0);
        System.out.println("expr = " + expr);
        e.parse(expr);
        ComplexNumber x = e.evaluate();
        System.out.println("result: " + x + "/" + t0);
        assertEquals(x, t0);
    }

    public void testChain2() throws Exception {
        Evaluator e = new Evaluator();
        String expr = a.toString() + " ^2 + " + b.toString() + " ^2";
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
        String expr = a.toString() + " ^2 + " + b.toString() + " ^3";
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
}
