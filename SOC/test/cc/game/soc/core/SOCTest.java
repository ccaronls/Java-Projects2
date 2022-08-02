package cc.game.soc.core;

import junit.framework.TestCase;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.game.soc.ui.NetCommon;
import cc.game.soc.ui.UIPlayer;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.net.GameClient;
import cc.lib.utils.Profiler;
import cc.lib.utils.Reflector;

public class SOCTest extends TestCase {

	public void testAssert() {
		try {
			assert(false);
			fail();
		} catch (Throwable t) {}
	}

	private long startTimeMs = 0;

	@Override
    protected void setUp() throws Exception {
        //LoggerFactory.setFileLogger(new File("testresult/" + getName() + "log.txt"), false);
	    System.out.println("Begin TEST: " + getName() + " working dir=" + new File(".").getAbsolutePath());
        System.out.println("----------------------------------------------");
        Profiler.ENABLED = true;
        PlayerBot.DEBUG_ENABLED = true;
        Utils.setDebugEnabled();
        startTimeMs = System.currentTimeMillis();
    }



    @Override
    protected void tearDown() throws Exception {
        System.out.println("----------------------------------------------");
        System.out.println("End TEST: " + getName() + " in " + (0.001f * (System.currentTimeMillis()-startTimeMs)) + " secs");
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
		assertTrue(soc.deepEquals(s));
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

    public void testScenarios2() throws Exception {

	    for (int i=28; 1<100; i++) {
	        Utils.setRandomSeed(i);
	        try {
                testScenarios();
            } finally {
	            System.out.println("RANDOM SEED = " + i);
            }
        }


    }

    public void testFailedGame() throws Exception {
	    SOC soc = new SOC();

	    int iter = 0;
	    int seed = 101;
	    try {

	        for ( ; seed<1000; seed++) {
                soc.loadFromFile(new File("testresult/failedgame.txt"));
                Utils.setRandomSeed(seed);
                for (iter = 0; iter < 100 && !soc.isGameOver(); iter++) {
                    System.out.print(String.format("ITER %5d ", iter));
                    soc.runGame();
                }

                //break;
            }

            assertTrue(soc.isGameOver());

        } catch (Throwable e) {
	        PlayerBot.dumpStats();

//	        soc.saveToFile(new File("testresult/failedgame2.txt"));
            System.out.println("TEST Failed iteration (" + iter + ") seed = " + seed);
            throw e;
        }
    }

    public void testScenarios() throws Exception {
        //Utils.setRandomSeed(58);
        SOC soc = new SOC();

        File dir = new File("assets/scenarios");
        File [] files = dir.listFiles();

        List<String> passedFiles = new ArrayList<>();

        try {
            int iteration = 0;
            for (File scenario : files) {
                try {
                    if (scenario.getName().toLowerCase().equals("catan for two.txt"))
                        continue;
                    soc.loadFromFile(scenario);

                    GColor [] colors = {
                        GColor.RED,
                        GColor.BLUE,
                        GColor.GREEN,
                        GColor.CYAN,
                        GColor.YELLOW,
                        GColor.MAGENTA
                    };

                    for (int i=0; i<soc.getRules().getMaxPlayers(); i++) {
                        soc.addPlayer(new UIPlayer(colors[i]));
                    }

                    for (int i=0; i<5; i++) {
                        soc.initGame();
                        for (iteration = 0; iteration < 10000 && !soc.isGameOver(); iteration++) {
                            System.out.print("ITER(" + iteration + ")");

                            soc.runGame();
                        }

                        for (int p=0; p<soc.getNumPlayers(); p++) {
                            System.out.println("Player " + (p+1) + ": " + soc.getPlayerByPlayerNum(p+1));
                        }

                        assertTrue(soc.isGameOver());
                        passedFiles.add(scenario.getName() + " in " + iteration + " iterations");
                        PlayerBot.clearStats();
                    }
                    soc.clear();
                    soc.getBoard().clear();
                } catch (Throwable t) {
                    System.err.println("TEST Failed iteration (" + iteration + ") in file: " + scenario);
                    PlayerBot.dumpStats();
                    soc.saveToFile(new File("testresult/failedgame.txt"));
                    throw t;
                }
            }
        } finally {
            System.out.println("Passed Files:\n" + passedFiles);
        }
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
	
	public void testDiff() throws Exception {

        SOC a = new SOC();
        SOC b = new SOC();
        assertTrue(a.deepEquals(b));


        String diff = a.diff(b);
        System.out.println("diff:\n" + diff);

        b.addPlayer(new PlayerTemp(1));
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));


        b.getPlayerByPlayerNum(1).adjustResourcesForBuildable(BuildableType.City, 1);
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));


        b.getPlayerByPlayerNum(1).adjustResourcesForBuildable(BuildableType.City, -1);
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));


        b.addPlayer(new UIPlayer());
//        b.getPlayerByPlayerNum(1).adjustResourcesForBuildable(BuildableType.City, 1);
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));


        b.getPlayerByPlayerNum(2).setCityDevelopment(DevelopmentArea.Politics, 1);
//        b.getPlayerByPlayerNum(1).adjustResourcesForBuildable(BuildableType.City, 1);
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));


        ((UIPlayer)b.getPlayerByPlayerNum(2)).setColor(new GColor(128, 0, 0, 128));
        diff = a.diff(b);
        System.out.println("diff:\n"+diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));

    }

    public void testDiffUIPlayer() throws Exception {
	    UIPlayer p1 = new UIPlayer();
	    UIPlayer p2 = new UIPlayer();

	    String diff = p1.diff(p2);
        System.out.println("diff:\n" + diff);

        p2.setColor(new GColor(128, 255, 0, 0));
        diff = p1.diff(p2);
        System.out.println("diff:\n" + diff);
        p1.deserialize(diff);
        assertTrue(p1.deepEquals(p2));
    }

    public void testDiffBoard() throws Exception {
	    SOC a = new SOC();
	    SOC b = new SOC();
	    assertTrue(a.deepEquals(b));
	    assertEquals(a.toString(), b.toString());

	    b.addPlayer(new PlayerTemp(1));
	    b.addPlayer(new PlayerTemp(2));
	    b.addPlayer(new PlayerTemp(3));

	    String diff = a.diff(b);
	    a.deserialize(diff);
        assertTrue(a.deepEquals(b));
	    assertEquals(3, a.getNumPlayers());

	    Board x = b.getBoard();
	    int vIndex = x.getNumVerts()/2;
	    x.getVertex(vIndex).setPlayerAndType(1, VertexType.CITY);

        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));
        assertEquals(a.getBoard().getVertex(vIndex).getPlayer(), 1);
        assertEquals(a.getBoard().getVertex(vIndex).getType(), VertexType.CITY);

        int eIndex = x.getNumRoutes()/2;
        x.setPlayerForRoute(x.getRoute(eIndex), 1, RouteType.ROAD);
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));
        assertEquals(a.getBoard().getRoute(eIndex).getPlayer(), 1);
        assertEquals(a.getBoard().getRoute(eIndex).getType(), RouteType.ROAD);

        int tIndex = x.getNumTiles()/2;
        x.getTile(tIndex).setType(TileType.RANDOM_PORT_OR_WATER);
        diff = a.diff(b);
        System.out.println("diff:\n" + diff);
        a.deserialize(diff);
        assertTrue(a.deepEquals(b));
        assertEquals(a.getBoard().getTile(tIndex).getType(), TileType.RANDOM_PORT_OR_WATER);
    }

    public void testSOCDiff() throws Exception {

	    SOC soc = new SOC();
	    soc.addPlayer(new PlayerRandom());
        soc.addPlayer(new PlayerRandom());
        soc.addPlayer(new PlayerRandom());
        SOC copy = new SOC();
        soc.initGame();
        copy.copyFrom(soc);

        long undiffedBytes = 0;
        long bytesDiffed = 0;
        long compressBytesDiffed = 0;
        int packets = 0;

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        EncryptionOutputStream out = new EncryptionOutputStream(b, NetCommon.getCypher());
        HuffmanEncoding enc = new HuffmanEncoding();

        for (int i=0; i<10000; i++) {
            if (soc.isGameOver())
                break;

            soc.runGame();
            packets++;
            //bytesDiffed += soc.toString().length(); <--- This way averages 70K!!!

            undiffedBytes += soc.toString().length();
            String diff = copy.diff(soc);
            enc.importCounts(diff);
            bytesDiffed += diff.length(); // <--- This way averages 4K. We can get that down if we can omit state/dice state etc.
            out.write(diff.getBytes());
            compressBytesDiffed += b.size();
            b.reset();
            copy.merge(diff);
            //assertEquals(copy.toString(), soc.toString());
            //assertTrue(copy.deepEquals(soc));
        }

        enc.printEncodingAsCode(System.out);

        System.out.println("num packets=" + packets + "\n  Avg undiffed packet size: " + ((double)undiffedBytes/packets) + "\n  Avg diff packet size: " + ((double)bytesDiffed/packets) + "\n  Avg diff compress size: " + ((double)compressBytesDiffed/packets));

    }

    public void testMethodMatching() throws Exception {

	    SOC soc = new SOC();

	    Method m = GameClient.searchMethods(soc, "onVertexChosen",
                new Class[] { Integer.class, Player.VertexChoice.class, Integer.class, null},
                new Object[] { new Integer(0), Player.VertexChoice.CITY, null }
        );

	    List<Dice> dice = Arrays.asList(new Dice(1, DiceType.WhiteBlack), new Dice(2, DiceType.RedYellow));
	    m = GameClient.searchMethods(soc, "onDiceRolled", new Class[] { List.class }, new Object[] { dice }
        );
    }

    public void testIsSubclassAnnonymos() throws Exception {
	    SOC soc = new SOC();

	    SOC annon = new SOC() {
            @NotNull
            @Override
            public String getString(@NotNull String format, @NotNull Object... args) {
                return "";
            }
        };

	    boolean isSubclass = Reflector.isSubclassOf(annon.getClass(), soc.getClass()
        );
    }

    public void testUIPlayerDiffColors() throws Exception {

	    SOC soc = new SOC();
	    soc.addPlayer(new UIPlayer());
        soc.addPlayer(new UIPlayer());
        soc.addPlayer(new UIPlayer());

        SOC b = soc.deepCopy();

        assertFalse(b.getPlayerByPlayerNum(1) == soc.getPlayerByPlayerNum(1));
        assertFalse(b.getPlayerByPlayerNum(2) == soc.getPlayerByPlayerNum(2));
        assertFalse(b.getPlayerByPlayerNum(3) == soc.getPlayerByPlayerNum(3));

        assertEquals(b.getPlayerByPlayerNum(1), soc.getPlayerByPlayerNum(1));
        assertEquals(b.getPlayerByPlayerNum(2), soc.getPlayerByPlayerNum(2));
        assertEquals(b.getPlayerByPlayerNum(3), soc.getPlayerByPlayerNum(3));

        String diff = soc.diff(b);
        System.out.println("diff:\n" + diff);

        ((UIPlayer)b.getPlayerByPlayerNum(1)).setColor(GColor.TRANSPARENT);

        diff = soc.diff(b);
        System.out.println("diff:\n" + diff);

        soc.merge(diff);
        diff = soc.diff(b);
        System.out.println("diff:\n" + diff);
        //assertEquals(soc, b);

    }

    public void testLoadScenarios() throws Exception {
        File root = new File(".");
        List<File> files = new ArrayList<>();
        File path = new File(root, "SOC/assets/scenarios");
        if (!path.exists()) {
            throw new Exception("Failed to find path: " + path.getCanonicalPath());
        }
        files.addAll(Arrays.asList(new File(root, "SOC/assets/scenarios").listFiles()));

        SOC soc = new SOC();
        for (File file : files) {
            soc.loadFromFile(file);
        }
    }

    public void testSerializeEnum() throws Exception {
	    EventCard card = new EventCard();
	    System.out.println(Reflector.serializeObject(card));
    }
}
