package cc.fantasy.struts.util;

import junit.framework.TestCase;

public class PaginationTest extends TestCase {

	public void test() {
		Pagination p = new Pagination("getList", 20, 10, 40);
		System.out.println(p.generateHeader("NAME"));
		System.out.println(p.generateHeader("STATUS"));
		
		System.out.println(p.generatePagination());
	}
	
}
