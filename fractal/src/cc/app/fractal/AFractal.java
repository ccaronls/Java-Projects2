package cc.app.fractal;

import cc.app.fractal.evaluator.Evaluator;
import cc.lib.math.ComplexNumber;

public abstract class AFractal {

    private final String name;
    protected final ComplexNumber zi = new ComplexNumber();
    protected final ComplexNumber c = new ComplexNumber();
    
    AFractal(String name) {
        this.name = name;
    }
    
    public abstract int processPixel(double x, double y, int length);

    public abstract String getDescription();

    public void setup(ComplexNumber c) throws Exception {
        this.c.copy(c);
    }
    
    public final String getName() {
        return name;
    }

    public final static class Mandelbrot extends AFractal {

        Mandelbrot() {
            super("Mandelbrot");
        }

        @Override
        public int processPixel(double x, double y, int length) {
            zi.set(x,y);
            c.set(x,y);
            for (int i=0; i<length; i++) {
                if (zi.mag() > 4)
                    return i;
                zi.multiplyEq(zi).addEq(c);
            }
            return 0;
        }

        @Override
        public String getDescription() {
            return "Mandelbrot Z^2 + Z0";
        }
        
        
    }
    
    public final static class Julia extends AFractal {
        
        Julia() {
            super("Julia");
        }

        @Override
        public int processPixel(double x, double y, int length) {
            zi.set(x,y);
            for (int i=0; i<length; i++) {
                if (zi.mag() > 4)
                    return i;
                zi.multiplyEq(zi).subEq(c);
            }
            return 0;
        }

        @Override
        public String getDescription() {
            return "Julia Z^2 - " + c;
        }
        
        
    }
    
    public final static class Custom extends AFractal {
        
        private final Evaluator evaluator = new Evaluator();
        private final String expression;
        
        /**
         * Generates Julia styles fractals
         * @param expression
         * @throws Exception
         */
        Custom(String expression) throws Exception {
            super("Custom");
            this.expression = expression;
            evaluator.parse(expression);
        }
        
        @Override
        public void setup(ComplexNumber C) throws Exception {
            super.setup(C);
            evaluator.parse(C, expression);
        }
        
        
        @Override
        public int processPixel(double x, double y, int length) {
            zi.set(x, y);
            evaluator.reset(zi, zi, c);
            for (int i=0; i<length; i++) {
                if (zi.mag() > 4)
                    return i;
                zi.copy(evaluator.evaluate());
            }
            return 0;
        }

        @Override
        public String getDescription() {
            return evaluator.getCompiledExpression();
        }
        
        
    }

}
