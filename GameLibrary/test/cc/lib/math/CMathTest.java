package cc.lib.math;

import junit.framework.TestCase;

public class CMathTest extends TestCase {

	public void testNormalDistribution() {
		for (int i=0; i<10; i++) {
			System.out.println(CMath.normalDistribution(i, 5));
		}
	}
	
}
