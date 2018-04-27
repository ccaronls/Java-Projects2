package cc.game.soc.core;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 4/2/18.
 */

public class Scenario extends Reflector<Scenario> {
    static {
        addAllFields(Scenario.class);
    }

    public Scenario() {}

    public Scenario(SOC soc, Map<String, Double> aituing) {
        this.mBoard = soc.getBoard();
        this.mRules = soc.getRules();
        this.aiTuning = aituing;
    }

    //members mirro those in SOC so we can save a scenario and load into an SOC without all the other
    // crap associated with SOC
    Board mBoard;
    Rules mRules;
    Map<String, Double> aiTuning;
}
