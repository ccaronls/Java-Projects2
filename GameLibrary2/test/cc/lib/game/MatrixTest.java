package cc.lib.game;

import junit.framework.TestCase;

import cc.lib.math.Matrix3x3;

public class MatrixTest extends TestCase {

    public void test() {
        Matrix3x3 A = new Matrix3x3();
        Matrix3x3 B = new Matrix3x3();
        Matrix3x3 C = new Matrix3x3();

        A.setTranslationMatrix(5, 10);
        System.out.println("A=" + A);

        B.setRotationMatrix(45);
        System.out.println("B=" + B);

        Matrix3x3.multiply(A, B, C);
        System.out.println("A X B =" + C);

        Matrix3x3.multiply(B, A, C);
        System.out.println("B X A =" + C);


    }


}
