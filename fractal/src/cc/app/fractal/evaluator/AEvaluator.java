package cc.app.fractal.evaluator;

import cc.lib.math.ComplexNumber;

public abstract class AEvaluator {

    final ComplexNumber Z0 = new ComplexNumber();
    final ComplexNumber Zi = new ComplexNumber();
    final ComplexNumber C  = new ComplexNumber();
    final ComplexNumber t0 = new ComplexNumber();
//    private final ComplexNumber t1 = new ComplexNumber();

    Node root;
    String expression = "";

    public abstract void parse(String expression) throws Exception;
    
    public abstract void parse(ComplexNumber constant, String expression) throws Exception;

    public void reset(ComplexNumber Zi, ComplexNumber Z0, ComplexNumber C) {
        this.Zi.copy(Zi);
        this.Z0.copy(Z0);
        this.C.copy(C);
    }

    public ComplexNumber evaluate() {
        if (root != null)
            Zi.copy(evaluateR(root));
        return Zi;
    }
    
    public ComplexNumber evaluate(String expression) throws Exception {
      	this.expression = expression;
        parse(expression);
        return evaluate();
    }

    ComplexNumber evaluateR(Node n) {
        switch (n.type) {
        case TYPE_ADD:
            return evaluateR(n.left).add(evaluateR(n.right), t0);
        case TYPE_SUB:
            return evaluateR(n.left).sub(evaluateR(n.right), t0);
        case TYPE_MULT:
            return evaluateR(n.left).multiply(evaluateR(n.right), t0);
        case TYPE_DIV:
            return evaluateR(n.left).divide(evaluateR(n.right), t0);
        case TYPE_POWI:
            return evaluateR(n.left).powi(n.right.numi, t0);
        case TYPE_POWD:
            return evaluateR(n.left).powd(n.right.numd, t0);
        case TYPE_POWC:
            return evaluateR(n.left).powc(evaluateR(n.right), t0);
        case TYPE_SQRT:
            return evaluateR(n.left).sqrt(t0);
        case TYPE_SIN:
            return evaluateR(n.left).sine(t0);
        case TYPE_COS:
            return evaluateR(n.left).cosine(t0);
        case TYPE_TAN:
            return evaluateR(n.left).tangent(t0);
        case TYPE_SINH:
            return evaluateR(n.left).sineh(t0);
        case TYPE_COSH:
            return evaluateR(n.left).cosineh(t0);
        case TYPE_TANH:
            return evaluateR(n.left).tangenth(t0);
        case TYPE_ASIN:
            return evaluateR(n.left).asine(t0);
        case TYPE_ACOS:
            return evaluateR(n.left).acosine(t0);
        case TYPE_ATAN:
            return evaluateR(n.left).atangent(t0);
        case TYPE_ASINH:
            return evaluateR(n.left).asineh(t0);
        case TYPE_ACOSH:
            return evaluateR(n.left).acosineh(t0);
        case TYPE_ATANH:
            return evaluateR(n.left).atangenth(t0);
        case TYPE_LN:
            return evaluateR(n.left).ln(t0);
        case TYPE_EXP:
            return evaluateR(n.left).exp(t0);
        case TYPE_CONSTANT:
            return n.numc;
        case TYPE_Z0:
            return Z0;
        case TYPE_Zi:
            return Zi;
        case TYPE_NEGATE:
            return evaluateR(n.left).negate(t0);
        }
        throw new RuntimeException("Unhandled type");
    }

	public String getExpression() {
	  	return this.expression;
	}
    public void debugDump() {
        debugDumpR(0, root);
    }

    void debugDumpR(int level, Node n) {
        if (n == null)
            return;

        for (int i=0; i<level; i++)
            System.out.print("  ");

        System.out.print(n.type.name());
        switch (n.type) {
        case TYPE_CONSTANT:
            System.out.println(" " + n.stringValue); break;
            
        case TYPE_Z0:
            System.out.println(" Z0"); break;
            
        case TYPE_Zi:
            System.out.println(" Zi"); break;
            
        default:
            System.out.println();
        }
        debugDumpR(level+1, n.left);
        debugDumpR(level+1, n.right);
    }
    
    public String getCompiledExpression() {
        StringBuffer buf = new StringBuffer();
        getCompiledExpressionR(buf, root);
        return buf.toString();
    }
    
    void getCompiledExpressionR(StringBuffer buffer, Node n) {
        if (n == null)
            return;

        switch (n.type)
        {
        case TYPE_SQRT:
        case TYPE_SIN:
        case TYPE_COS:
        case TYPE_TAN:
        case TYPE_SINH:
        case TYPE_COSH:
        case TYPE_TANH:
        case TYPE_ASIN:
        case TYPE_ACOS:
        case TYPE_ATAN:
        case TYPE_ASINH:
        case TYPE_ACOSH:
        case TYPE_ATANH:
        case TYPE_LN:
        case TYPE_EXP:
            buffer.append(n.type.symbol);
            buffer.append("(");
            getCompiledExpressionR(buffer, n.left);
            getCompiledExpressionR(buffer, n.right);
            buffer.append(")");
            break;       
        case TYPE_CONSTANT:
            getCompiledExpressionR(buffer, n.left);
            buffer.append(n.numc.toString());
            getCompiledExpressionR(buffer, n.right);
            break;
        case TYPE_POWI:
        case TYPE_POWD:
        case TYPE_POWC:
        case TYPE_ADD:
        case TYPE_SUB:
        case TYPE_MULT:
        case TYPE_DIV:
        case TYPE_Z0:
        case TYPE_Zi:
            getCompiledExpressionR(buffer, n.left);
            buffer.append(n.type.symbol);
            getCompiledExpressionR(buffer, n.right);
            break;
        case TYPE_NEGATE:
            buffer.append(n.type.symbol);
            getCompiledExpressionR(buffer, n.left);
            getCompiledExpressionR(buffer, n.right);
            break;
        }
    }

// We really dont need this, Z0 and Zi are an extension of CONSTANT
// Should be something like: FUNC, NUM, COMPLEX_NUM only
    enum Type {
        TYPE_ADD("+"), // complex + complex
        TYPE_SUB("-"), // complex - complex
        TYPE_MULT("*"), // complex * complex
        TYPE_DIV("//"),  // complex / complex
        TYPE_CONSTANT(""), // Complex constant [a+bi]
        TYPE_POWI("^"), // complex ^ int
        TYPE_POWD("^"), // complex ^ double
        TYPE_POWC("^"), // complex ^ complex
        TYPE_SQRT("sqrt"), // sqrt (complex)
        TYPE_SIN("sin"), // sin (complex)
        TYPE_COS("cos"), // cos (complex)
        TYPE_TAN("tan"), // tan (complex)
        TYPE_ASIN("asin"), // sin (complex)
        TYPE_ACOS("acos"), // cos (complex)
        TYPE_ATAN("atan"), // tan (complex)
        TYPE_SINH("sinh"), // sinh (complex)
        TYPE_COSH("cosh"), // cosh (complex)
        TYPE_TANH("tanh"),        
        TYPE_ASINH("asinh"), // sinh (complex)
        TYPE_ACOSH("acosh"), // cosh (complex)
        TYPE_ATANH("atanh"),
        TYPE_LN("ln"),  // ln (complex)
        TYPE_EXP("e^"), // exp (complex)
        TYPE_Z0("Z0"),
        TYPE_Zi("Zi"),
        TYPE_NEGATE("-"), // -constant
        
        ;
        
        private Type(String symbol) {
            this.symbol = symbol;
        }
        
        final String symbol;
    }
    
    // TODO: Merge with the functions of Type, make type one of constant or func, move
    //   this enum to an external file with the evaluators as abstract function.
    //   Should be able to add functions with out running javacc on this file.
    enum Function {
        sqrt(Type.TYPE_SQRT),
        sin(Type.TYPE_SIN),
        sinh(Type.TYPE_SINH),
        asin(Type.TYPE_ASIN),
        asinh(Type.TYPE_ASINH),
        cos(Type.TYPE_COS),
        cosh(Type.TYPE_COSH),
        acos(Type.TYPE_ACOS),
        acosh(Type.TYPE_ACOSH),
        tan(Type.TYPE_TAN),
        tanh(Type.TYPE_TANH),
        atan(Type.TYPE_ATAN),
        atanh(Type.TYPE_ATANH),
        ln(Type.TYPE_LN),
        exp(Type.TYPE_EXP),
        ;
        
        Function(Type type) {
            this.type = type;
        }
        
        final Type type;
    }
    
    Type lookupFunc(String name) {
        try {
            return Function.valueOf(name).type;
        } catch (Exception e) {
            throw new RuntimeException("Unknown function '" + name + "' must be one of : " + java.util.Arrays.asList(Function.values()));
        }
    }

    final static int CONSTANT_COMPLEX = 0;
    final static int CONSTANT_DOUBLE  = 1;
    final static int CONSTANT_INT     = 2;

    static class Node {
        Type type;
        ComplexNumber numc;
        int numi;
        double numd;
        int constantType = -1;
        String stringValue;

        Node(Type type, Node left, Node right) {
            this.type = type;
            this.left = left;
            this.right = right;
        }

        Node(double constant, String value) {
            type = Type.TYPE_CONSTANT;
            constantType = CONSTANT_DOUBLE;
            numd = constant;
            numc = new ComplexNumber(numd, 0);
            this.stringValue = value == null ? String.valueOf(numd) : value + " {" + numd + "}";
        }
  
        Node(String constant) {
            stringValue = constant;
            type = Type.TYPE_CONSTANT;
            try {
               numi = Integer.parseInt(constant);
               numd = numi;
               constantType = CONSTANT_INT;
               stringValue += " {" + numi + "}";
            } catch (NumberFormatException e) {
               numd = Double.parseDouble(constant);
               constantType = CONSTANT_DOUBLE;
               stringValue += " {" + numd + "}";
            }
            numc = new ComplexNumber(numd, 0);
        }

        Node(Type type) {
            this.type = type;
        }

        Node(ComplexNumber constant) {
            this.type = Type.TYPE_CONSTANT;
            if (constant.isReal()) {
                this.constantType = CONSTANT_DOUBLE;
                this.numd = constant.getReal();
                stringValue = "{" + numd + "}";
            } else {
                this.constantType = CONSTANT_COMPLEX;
                stringValue = constant.toString();
            }
            this.numc = constant;
        }

        Node(ComplexNumber constant, String name) {
            this.type = Type.TYPE_CONSTANT;
            if (constant.isReal()) {
                this.constantType = CONSTANT_DOUBLE;
                this.numd = constant.getReal();
                stringValue = name + " {" + numd + "}";
                
            } else {
                this.constantType = CONSTANT_COMPLEX;
                stringValue = name + " " + constant;
            }
            this.numc = constant;
        }

        Node left, right;

    }
}