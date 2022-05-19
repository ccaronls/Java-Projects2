package cc.lib.utils;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ClassTest extends TestCase {

	public void test() {
		List<String> l = new ArrayList<String>();
		
		System.out.println("l.class = " + l.getClass() + "\nl.canonical = " + l.getClass().getCanonicalName() + "\nl.local = " + l.getClass().isLocalClass()
				+"\nl.array = " + l.getClass().isArray() + "\nl.enum = " + l.getClass().isEnum() + "\nl.primitve = " + l.getClass().isPrimitive());
	}
	
}
