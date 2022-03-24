package cc.lib.zombicide;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.APGraphics;
import cc.lib.game.TestGraphics;
import cc.lib.game.Utils;
import cc.lib.zombicide.ui.UIZombicide;

public class ZombicideTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Utils.setDebugEnabled();
    }

    public void testQuests() throws Exception {
        UIZombicide game = new HeadlessUIZombicide();
        APGraphics g = new TestGraphics();
        for (ZQuests q : ZQuests.values()) {
            System.out.println("Testing Quest: " + q);
            game.loadQuest(q);
            assertEquals(q, game.getQuest().getQuest());
            game.getQuest().getPercentComplete(game);
            game.getQuest().getTiles();
            for (ZIcon ic : ZIcon.values()) {
                ic.imageIds = new int[8];
            }
            game.boardRenderer.draw(g, 500, 300);
        }
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
        game.setUsers(new ZTestUser(ZPlayerName.Ann));
        game.loadQuest(ZQuests.The_Abomination);

        ZCharacter ann = null;
        while (ann == null) {
            game.runGame();
            ann = game.getCurrentCharacter().character;
        }

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
            System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.type.minDamageToDestroy, z.type.attackPriority));
        }
        System.out.println("----------------------------------------------------");

        for (int i=1; i<=3; i++) {
            List<ZZombie> meleeList = new ArrayList<>(zombies);
            Collections.sort(meleeList, new ZGame.MarksmanComparator(1));
            System.out.println("MELEE SORTING " + i);
            System.out.println("----------------------------------------------------");
            for (ZZombie z : meleeList) {
                System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.type.minDamageToDestroy, z.type.attackPriority));
            }
        }

        System.out.println("----------------------------------------------------");

        for (int i=1; i<=3; i++) {
            List<ZZombie> rangedList = new ArrayList<>(zombies);
            Collections.sort(rangedList, new ZGame.RangedComparator());
            System.out.println("RANGED SORTING " + i);
            System.out.println("----------------------------------------------------");
            for (ZZombie z : rangedList) {
                System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.type.minDamageToDestroy, z.type.attackPriority));
            }
        }

        for (int i=1; i<=3; i++) {
            List<ZZombie> marksmanList = new ArrayList<>(zombies);
            Collections.sort(marksmanList, new ZGame.MarksmanComparator(1));
            System.out.println("MARKSMAN SORTING " + i);
            System.out.println("----------------------------------------------------");
            for (ZZombie z : marksmanList) {
                System.out.println(String.format("%-20s hits:%d  priority:%d", z.getType(), z.type.minDamageToDestroy, z.type.attackPriority));
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
        assertTrue(game.getQuest().isWolfBurg());
        ZSpawnCard.drawSpawnCard(game.getQuest().isWolfBurg(), true, ZDifficulty.HARD);
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
