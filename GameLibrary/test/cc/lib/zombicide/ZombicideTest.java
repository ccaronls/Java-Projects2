package cc.lib.zombicide;

import junit.framework.TestCase;

import cc.lib.game.Utils;

public class ZombicideTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Utils.setDebugEnabled();
    }

    public void testLevels() {

        ZSkillLevel.ULTRA_RED_MODE = false;
        for (int i=0; i<1000; i++) {
            if (i <= ZSkillLevel.Color.BLUE.maxPts)
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZSkillLevel.Color.BLUE);
            else if (i <=ZSkillLevel.Color.YELLOW.maxPts)
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZSkillLevel.Color.YELLOW);
            else if (i <= ZSkillLevel.Color.ORANGE.maxPts)
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZSkillLevel.Color.ORANGE);
            else
                assertTrue(ZSkillLevel.getLevel(i).getColor() == ZSkillLevel.Color.RED);
        }

        for (int i=0; i<100; i++) {
            System.out.println("For exp " + i + " level is " + ZSkillLevel.getLevel(i) + " and next level in " + ZSkillLevel.getLevel(i).getPtsToNextLevel(i));
        }
    }

    public void testUltraRed() {
        ZGame game = new ZGame();
        game.setUsers(new TestUser(ZPlayerName.Ann));
        game.loadQuest(ZQuests.The_Abomination);

        ZCharacter ann = null;
        while (ann == null) {
            game.runGame();
            ann = game.getCurrentCharacter();
        }

        ZSkillLevel.ULTRA_RED_MODE = true;
        ZState state = game.getState();

        for (int i=0; i<100; i++) {
            game.addExperience(ann, Utils.rand() % 20 + 1);
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

}
