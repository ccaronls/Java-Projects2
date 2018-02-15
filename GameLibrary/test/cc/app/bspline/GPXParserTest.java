package cc.app.bspline;

import java.io.File;

import junit.framework.TestCase;

public class GPXParserTest extends TestCase {

	int num = 0;
	/*
	
	public void test() throws Exception {

		System.out.println(String.format("%5s %10s %10s %10s %20s %10s", "NUM", "LAT", "LON", "ELEV", "UTCTIME", "NAME"));
		
		GPXParser parser = new GPXParser() {
			
			@Override
			protected void onWaypoint(double lat, double lng, int elevationMeters, long utcTime, String name) {
				System.out.println(String.format("%5d %10.4f %10.4f %10d %20d %10s", num++, lat, lng, elevationMeters, utcTime, name));
			}
		};
		parser.parse(new File("elev_gps.gpx"));
	}
	
	public void testNeg() {
		try {
			GPXParser parser = new GPXParser() {

				@Override
				protected void onWaypoint(double lat, double lng,
						int elevationMeters, long utcTime, String name) {
					// TODO Auto-generated method stub
					
				}
				
			};
			parser.parse(new File("bad_elev_gps.gpx"));
			fail("Expected error");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}*/
	
}
