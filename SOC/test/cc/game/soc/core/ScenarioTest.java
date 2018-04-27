package cc.game.soc.core;

import junit.framework.TestCase;

import java.io.File;

/**
 * Created by chriscaron on 4/27/18.
 */

public class ScenarioTest extends TestCase {

    public void testScenarios() throws Exception {

        File[] files = new File("SOC/assets/scenarios").listFiles();

        Scenario scenario = new Scenario();

        for (File file : files) {
            scenario.loadFromFile(file);
        }


    }

    public void testScenarios2() throws Exception {

        File[] files = new File[] {
                new File("SOC/assets/scenarios/basic.txt")
        };

        Scenario scenario = new Scenario();

        for (File file : files) {
            scenario.loadFromFile(file);
        }

        System.out.println("aituning=" + scenario.aiTuning);

    }


}
