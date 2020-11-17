package cc.lib.zombicide;

import junit.framework.TestCase;

public class ZombicideTest extends TestCase {

    public void testLevels() {

        for (int i=0; i<1000; i++) {
            if (i <= ZSkillLevel.BLUE.maxPts)
                assertTrue(ZSkillLevel.getLevel(i) == ZSkillLevel.BLUE);
            else if (i <=ZSkillLevel.YELLOW.maxPts)
                assertTrue(ZSkillLevel.getLevel(i) == ZSkillLevel.YELLOW);
            else if (i <= ZSkillLevel.ORANGE.maxPts)
                assertTrue(ZSkillLevel.getLevel(i) == ZSkillLevel.ORANGE);
            else
                assertTrue(ZSkillLevel.getLevel(i) == ZSkillLevel.RED);
        }

        for (int i=0; i<100; i++) {
            System.out.println("For exp " + i + " level is " + ZSkillLevel.getLevel(i) + " and next level in " + ZSkillLevel.getLevel(i).getPtsToNextLevel(i));
        }


    }

}
