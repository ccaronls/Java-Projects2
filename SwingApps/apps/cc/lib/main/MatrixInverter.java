package cc.lib.main;

import cc.lib.math.Matrix4x4;

public class MatrixInverter {

	public static void main(String [] args) {
		
		Matrix4x4 M = new Matrix4x4(0, 0, 0, 1,
				 					1, 1, 1, 1,
				 					0, 0, 1, 0,
				 					3, 2, 1, 0);
		
		M.invert();
		
		System.out.println(M);
		
	}
	
	
}
