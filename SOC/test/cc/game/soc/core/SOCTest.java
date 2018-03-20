package cc.game.soc.core;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;

import cc.lib.game.Utils;
import cc.lib.utils.Profiler;

public class SOCTest extends TestCase {

	public void testAssert() {
		try {
			assert(false);
			fail();
		} catch (Throwable t) {}
	}

	@Override
    protected void setUp() throws Exception {
	    System.out.println("Begin TEST: " + getName());
        System.out.println("----------------------------------------------");
        Profiler.ENABLED = true;
        PlayerBot.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
    }



    @Override
    protected void tearDown() throws Exception {
        System.out.println("----------------------------------------------");
        System.out.println("End TEST: " + getName());
        Profiler.dumpTimes(System.out);

    }



    public void testSOC() throws Exception
	{
		final FileWriter log = new FileWriter("testresult/soctestlog.txt");
		SOC soc = new SOC();
		Board b = soc.getBoard();
		b.generateHexBoard(4, TileType.WATER);
		for (int i=0; i<b.getNumTiles(); i++) {
		    b.getTile(i).setType(TileType.RANDOM_RESOURCE_OR_DESERT);
		}
		b.trim();
		b.assignRandom();
		for (int i=0; i<4; i++)
			soc.addPlayer(new PlayerRandom());
		//for (int i=0; i<100000; i++)
		for (int i=0; i<100 && !soc.isGameOver(); i++)
			soc.runGame();
		log.close();
		//soc.serialize(System.out);
		File file = new File("soctest.sav");
		soc.saveToFile(file);
		SOC s = new SOC();
		s.loadFromFile(file);
		assertEquals(soc, s);
		for (int i=0; i<100 && !s.isGameOver(); i++)
			s.runGame();
		
	}
	
    public void testAIGame() throws Exception
    {
        SOC soc = new SOC();
        Board b = soc.getBoard();
        b.generateHexBoard(4, TileType.WATER);
        for (int i=0; i<b.getNumTiles(); i++) {
            b.getTile(i).setType(TileType.RANDOM_RESOURCE_OR_DESERT);
        }
        b.trim();
        b.assignRandom();
        for (int i=0; i<2; i++)
            soc.addPlayer(new PlayerBot());
        //for (int i=0; i<100000; i++)
        for (int i=0; i<10000 && !soc.isGameOver(); i++)
            soc.runGame();
        soc.save("testresult/soctestresult.txt");
    }
    
	public void testRestoreGame() throws Exception {
		//assertTrue(new SOC(new Board()).load("soctestresult.txt"));
	}
	
	public void testCopy() throws Exception {
	    Board b = new Board();
	    //b.load("boards/testboard.txt");
	    b.generateDefaultBoard();
	    Board copy = b.deepCopy();
	    assertEquals(b.getNumTiles(), copy.getNumTiles());
        assertEquals(b.getNumVerts(), copy.getNumVerts());
        assertEquals(b.getNumRoutes(), copy.getNumRoutes());
        assertEquals(b.getTileHeight(), copy.getTileHeight());
        assertEquals(b.getTileWidth(), copy.getTileWidth());
        assertEquals(b.getName(), copy.getName());
        
        for (int i=0; i<b.getNumTiles(); i++) {
            assertEquals(b.getTile(i), copy.getTile(i));
            assertEquals(b.getTileIndex(copy.getTile(i)), i);
        }
        for (int i=0; i<b.getNumVerts(); i++) {
            assertEquals(b.getVertex(i), copy.getVertex(i));
            assertEquals(b.getVertexIndex(copy.getVertex(i)), i);
        }
        for (int i=0; i<b.getNumRoutes(); i++) {
            assertEquals(b.getRoute(i), copy.getRoute(i));
            assertEquals(b.getRouteIndex(copy.getRoute(i)), i);
        }
	}
	
	
}
