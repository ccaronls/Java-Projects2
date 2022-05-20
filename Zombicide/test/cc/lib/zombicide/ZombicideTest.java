package cc.lib.zombicide;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.game.APGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.TestGraphics;
import cc.lib.game.Utils;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Grid;
import cc.lib.zombicide.ui.UIZombicide;

public class ZombicideTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Utils.setDebugEnabled();
        for (ZZombieType z : ZZombieType.values()) {
            z.imageDims = new GDimension[] {
                    GDimension.EMPTY
            };
            z.imageOptions = new int[] { 0 };
            z.imageOutlineOptions = new int[] { 0 };
        }

        for (ZIcon i : ZIcon.values()) {
            i.imageIds = new int [] { 0 };
        }
    }

    public void testRunGame() {
        ZGame game = new ZGame();
        game.addUser(new ZTestUser());
        for (ZQuests q : ZQuests.values()) {
            System.out.println("Testing Quest: " + q);
            game.clearCharacters();
            game.loadQuest(q);
            for (ZPlayerName pl : Utils.toList(ZPlayerName.Ariane, ZPlayerName.Clovis, ZPlayerName.Ann, ZPlayerName.Nelly))
                game.addCharacter(pl);

            for (int i=0; i<1000; i++) {
                game.runGame();
                if (game.isGameOver())
                    break;
            }
        }
    }

    public void testRunGameWithGameDiffs() throws Exception {

        /*
AVG Diff Size:              26011
AVG Compressed Diff Size:   13349
Total Diffs Size:           524722374
Total Compressed Size:      269292144
Compression Ratio:          1.9485246253600328
Total Diffs:                20173
Total Time:                 214900
Total Diff Time:            9346
Total Compression Time:     28952
Total Decompression Time:   58186

AVG Diff Size:              23955
AVG Compressed Diff Size:   11718
Total Diffs Size:           483249741
Total Compressed Size:      236402304
Compression Ratio:          2.0441837191231436
Total Diffs:                20173
Total Time:                 227535
Total Diff Time:            14509
Total Compression Time:     25639
Total Decompression Time:   49343

         */
        final boolean COMPUTE_COUNTS = false;

        ZGame game = new HeadlessUIZombicide();
        game.addUser(new ZTestUser());
        long totalDiffSizeBytes = 0;
        long totalCompressedDiffSizeBytes = 0;
        long totalDiffTimeMS = 0;
        long numDiffs = 0;
        long totalCompressionTimeMS = 0;
        long totalDecompressionTimeMS = 0;
        LoggerFactory.logLevel = LoggerFactory.LogLevel.SILENT;
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,38723605,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,760928204,0,0,0,0,0,0,0,0,0,0,0,0,790973,24285073,0,7271806,4359237,1990584,1289033,1136347,1923453,1186936,605577,367121,868366,0,0,0,21309706,0,0,0,4868446,427663,2191494,426863,2368816,2411107,2059161,642876,1755727,11887,15305,6986105,460653,4912646,1835319,2319581,789813,2234146,3000699,2799966,254418,76858,3727594,0,5043,6467491,10,0,10,0,1413578,0,21068250,11527323,29288901,13948580,28916636,2989897,4918591,2169351,28742005,994152,1879472,38655539,8333837,19662857,19340855,6774298,4,12965080,12203750,11903970,15073054,3283530,4324690,2753174,3794159,5000722,7010098,0,7010098};
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,38723605,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,760928204,1,1,1,1,1,1,1,1,1,1,1,1,790974,24285074,1,7271807,4359238,1990585,1289034,1136348,1923454,1186937,605578,367122,868367,1,1,1,21309707,1,1,1,4868447,427664,2191495,426864,2368817,2411108,2059162,642877,1755728,11888,15306,6986106,460654,4912647,1835320,2319582,789814,2234147,3000700,2799967,254419,76859,3727595,1,5044,6467492,11,1,11,1,1413579,1,21068251,11527324,29288902,13948581,28916637,2989898,4918592,2169352,28742006,994153,1879473,38655540,8333838,19662858,19340856,6774299,5,12965081,12203751,11903971,15073055,3283531,4324691,2753175,3794160,5000723,7010099,1,7010099,1};
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,16083164,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,328200891,1,1,1,1,1,1,1,1,1,1,1,1,669045,9848700,1,1793781,1903988,898832,775395,703534,1552254,333574,363928,220394,381307,1,1,1,10373126,1,1,1,305679,73805,982953,8735,772464,385777,1635087,268748,928866,1115,14,1142828,59237,2669962,371534,1177625,667919,1501306,112755,2070630,214202,22,611035,1,5044,3227246,250095,1,250095,1,53870,1,6760076,4482197,13858764,6877520,13686098,1428841,2031316,1351126,12099834,42624,412846,10835467,3262179,5908641,9892008,3596512,5,6180675,4122276,6110429,6051791,51782,1338443,1333759,2145185,1301561,2874843,1,2874843,1};
//      int [] counts = {0,0,0,0,0,0,0,0,0,0,16081120,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,328187666,1,1,1,1,1,1,1,1,1,1,1,1,669045,9845694,1,1793774,1903980,898827,775391,703533,1552253,333574,363928,220394,381307,1,1,1,10373079,1,1,1,305679,73805,982934,8735,772464,385777,1635087,268748,928866,1115,14,1142821,59237,2669962,371534,1177625,667919,1501306,112755,2070630,214202,1,611035,1,5044,3225270,250095,1,250095,1,53870,1,6760034,4480221,13855779,6876532,13684021,1428841,2031316,1351126,12096842,42603,412846,10834413,3261191,5907606,9889983,3596512,5,6180654,4122248,6110368,6051770,51754,1338443,1333740,2145185,1300552,2873834,1,2873834,1};
        int [] counts = {0,0,0,0,0,0,0,0,0,0,16081334,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,328189620,1,1,1,1,1,1,1,1,1,1,1,1,669045,1368451,1,1793796,1903991,898845,775397,703550,1552274,333576,363929,220400,381312,1,1,1,10373126,1,1,1,305679,73805,982953,8735,772464,385777,946987,268748,928866,1115,14,1142828,59258,2669962,371534,1177625,667919,1501306,112755,2070630,214202,22,611035,1,5044,3205150,250095,1,250095,1,53870,1,5984270,693295,7500874,4989636,11774639,1428841,1360928,1351126,5701802,1,412846,7545296,1351642,5907679,8671090,3596512,3,5512768,3443670,5389202,5330564,9159,1338443,1333759,2145185,81605,2873928,1,2873928,1};

        HuffmanEncoding enc = COMPUTE_COUNTS ? new HuffmanEncoding() : new HuffmanEncoding(counts);
        if (COMPUTE_COUNTS)
            enc.initStandardCounts();
        long startTime = System.currentTimeMillis();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EncryptionOutputStream eout = new EncryptionOutputStream(out, enc);
        DataOutputStream dout = new DataOutputStream(eout);

        for (ZQuests q : ZQuests.values()) {
            System.out.println("Testing Quest: " + q);
            game.clearCharacters();
            game.loadQuest(q);
            for (ZPlayerName pl : Utils.toList(ZPlayerName.Ariane, ZPlayerName.Clovis, ZPlayerName.Ann, ZPlayerName.Nelly))
                game.addCharacter(pl);
            ZGame prev = game.deepCopy();
            assertEquals(game, prev);

            for (int i=0; i<1000; i++) {
                game.runGame();
                long t = System.currentTimeMillis();
                String diff = prev.diff(game);
                long dt = System.currentTimeMillis()-t;
                totalDiffSizeBytes += diff.length();

                totalDiffTimeMS += dt;
                numDiffs ++;

                if (COMPUTE_COUNTS) {
                    enc.importCounts(diff);
                } else {
                    t = System.currentTimeMillis();
                    dout.writeBytes(diff);
                    dout.flush();
                    dt = System.currentTimeMillis()-t;

                    totalCompressedDiffSizeBytes += out.size();
                    totalCompressionTimeMS += dt;
                    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                    EncryptionInputStream ein = new EncryptionInputStream(in, enc);
                    DataInputStream din = new DataInputStream(ein);

                    byte[] data = new byte[din.available()];
                    t = System.currentTimeMillis();
                    din.readFully(data);
                    dt = System.currentTimeMillis() - t;
                    totalDecompressionTimeMS += dt;
                    String diff2 = new String(data);
                    out.reset();

                    assertEquals(diff, diff2);
                }
                prev.merge(diff);
                //System.out.println("diff="+diff);
                assertEquals(game.toString(), prev.toString());
                assertEquals(game.getChecksum(), prev.getChecksum());

                if (game.isGameOver())
                    break;
            }
        }

        long totalTimeMS = System.currentTimeMillis()-startTime;

        if (COMPUTE_COUNTS)
            enc.printEncodingAsCode(System.out);

        System.out.println("AVG Diff Size:              " + totalDiffSizeBytes / numDiffs);
        System.out.println("AVG Compressed Diff Size:   " + totalCompressedDiffSizeBytes / numDiffs);
        System.out.println("Total Diffs Size:           " + totalDiffSizeBytes);
        System.out.println("Total Compressed Size:      " + totalCompressedDiffSizeBytes);
        System.out.println("Compression Ratio:          " + (double)totalDiffSizeBytes / totalCompressedDiffSizeBytes);
        System.out.println("Total Diffs:                " + numDiffs);
        System.out.println("Total Time:                 " + totalTimeMS);
        System.out.println("Total Diff Time:            " + totalDiffTimeMS);
        System.out.println("Total Compression Time:     " + totalCompressionTimeMS);
        System.out.println("Total Decompression Time:   " + totalDecompressionTimeMS);
    }

    public void testQuests() throws Exception {
        UIZombicide game = new HeadlessUIZombicide();
        APGraphics g = new TestGraphics();
        for (ZQuests q : ZQuests.values()) {
            System.out.println("Testing Quest: " + q);
            game.clearCharacters();
            game.loadQuest(q);
            ZBoard b = game.board;
            for (ZZone zone : b.getZones()) {
                for (ZDoor door : zone.doors) {
                    Grid.Pos pos = door.getCellPosStart();
                    ZCell cell = b.getCell(pos);
                    assertEquals(cell.zoneIndex, zone.getZoneIndex());
                    ZDoor otherSide = door.getOtherSide();
                    if (otherSide != null) {
                        pos = otherSide.getCellPosStart();
                        assertEquals(pos, door.getCellPosEnd());
                        assertEquals(otherSide.getCellPosEnd(), door.getCellPosStart());
                        cell = b.getCell(pos);
                    }
                }
            }
            assertEquals(q, game.getQuest().getQuest());
            game.getQuest().getPercentComplete(game);
            game.getQuest().getTiles();
            for (ZIcon ic : ZIcon.values()) {
                ic.imageIds = new int[8];
            }
            for (ZZombieType z : ZZombieType.values()) {
                z.imageOptions = new int[1];
            }
            game.boardRenderer.draw(g, 500, 300);
            for (ZZone zone : game.board.getZones()) {
                Assert.assertTrue("Zone: " + zone + " is invalid", zone.getZoneIndex() >= 0);
            }
        }
    }

    public void testEvilTwins() throws Exception {

        ZGame game = new ZGame();
        game.loadQuest(ZQuests.The_Evil_Twins);

        ZDoor gvd1 = game.board.findDoor(new Grid.Pos(0, 0), ZDir.DESCEND);
        assertNotNull(gvd1);

        ZDoor other = gvd1.getOtherSide();
        assertNotNull(other);

        assertEquals(new Grid.Pos(9, 6), other.getCellPosStart());

    }

    public void testLevels() {

        ZSkillLevel.ULTRA_RED_MODE = false;
        for (int i=0; i<1000; i++) {
            if (i <= ZColor.BLUE.maxPts)
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZColor.BLUE);
            else if (i <= ZColor.YELLOW.maxPts)
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZColor.YELLOW);
            else if (i <= ZColor.ORANGE.maxPts)
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZColor.ORANGE);
            else
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZColor.RED);
        }

        for (int i=0; i<100; i++) {
            System.out.println("For exp " + i + " level is " + ZSkillLevel.getLevel(i) + " and next level in " + ZSkillLevel.getLevel(i).getPtsToNextLevel(i));
        }
    }

    public void testUltraRed() {
        ZSkillLevel.ULTRA_RED_MODE = false;
        ZGame game = new ZGame();
        game.setUsers(new ZTestUser());
        game.clearCharacters();
        game.loadQuest(ZQuests.The_Abomination);
        game.addCharacter(ZPlayerName.Ann);

        ZCharacter ann = ZPlayerName.Ann.character;

        ZSkillLevel.ULTRA_RED_MODE = true;
        ZState state = game.getState();

        for (int i=0; i<1000; i++) {
            game.addExperience(ann, 1);
            while (game.getState() != state) {
                game.runGame();
            }
        }

        for (int i=0; i<ZSkillLevel.NUM_LEVELS; i++) {
            assertEquals(0, ann.getRemainingSkillsForLevel(i).size());
        }
    }

    public void testUltraLevelStr() {
        for (int i=0; i<150; i+=3) {
            System.out.println("Exp=" + i + "    -> " + ZSkillLevel.getLevel(i));
        }
    }

    public void testZombieSorting() {
        List<ZZombie> zombies = new ArrayList<>();

        for (int i=0; i<20; i++) {
            ZZombieType type = Utils.randItem(ZZombieType.values());
            zombies.add(new ZZombie(type, 0));
        }
        Utils.shuffle(zombies);

        System.out.println("----------------------------------------------------");
        for (ZZombie z : zombies) {
            System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.getType().minDamageToDestroy, z.getType().rangedPriority));
        }
        System.out.println("----------------------------------------------------");

        for (int i=1; i<=3; i++) {
            List<ZZombie> meleeList = new ArrayList<>(zombies);
            Collections.sort(meleeList, new ZGame.MarksmanComparator(1));
            System.out.println("MELEE SORTING " + i);
            System.out.println("----------------------------------------------------");
            for (ZZombie z : meleeList) {
                System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.getType().minDamageToDestroy, z.getType().rangedPriority));
            }
        }

        System.out.println("----------------------------------------------------");

        for (int i=1; i<=3; i++) {
            List<ZZombie> rangedList = new ArrayList<>(zombies);
            Collections.sort(rangedList, new ZGame.RangedComparator());
            System.out.println("RANGED SORTING " + i);
            System.out.println("----------------------------------------------------");
            for (ZZombie z : rangedList) {
                System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.getType().minDamageToDestroy, z.getType().rangedPriority));
            }
        }

        for (int i=1; i<=3; i++) {
            List<ZZombie> marksmanList = new ArrayList<>(zombies);
            Collections.sort(marksmanList, new ZGame.MarksmanComparator(1));
            System.out.println("MARKSMAN SORTING " + i);
            System.out.println("----------------------------------------------------");
            for (ZZombie z : marksmanList) {
                System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.getType().minDamageToDestroy, z.getType().rangedPriority));
            }
        }

    }

    public void testInitDice() {
        for (ZDifficulty d : ZDifficulty.values()) {
            int [] dice = ZGame.initDice(d);
            for (int n : dice) {
                assertTrue(n > 0 && n <= 6);
            }
        }
    }

    public void testWolfsberg() {
        ZGame game = new ZGame();
        game.setUsers(new ZTestUser(ZPlayerName.Ann));
        game.loadQuest(ZQuests.Welcome_to_Wulfsberg);
        assertTrue(game.getQuest().getQuest().isWolfBurg());
        ZSpawnCard.Companion.drawSpawnCard(game.getQuest().getQuest().isWolfBurg(), true, ZDifficulty.HARD);
    }

    public void testUltraExp() {
        ZSkillLevel skill = new ZSkillLevel(ZColor.BLUE, 0);
        int pts = 0;
        for (int i=0; i<10; i++) {
            int nextLvl = skill.getPtsToNextLevel(pts);
            assertTrue(nextLvl > 0);
            System.out.println("Cur level=" + skill);
            System.out.println("Next Level=" + nextLvl);
            pts += nextLvl;
            skill = skill.nextLevel();
        }
    }

}
