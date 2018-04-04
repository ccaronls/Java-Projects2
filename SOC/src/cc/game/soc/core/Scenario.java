package cc.game.soc.core;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 4/2/18.
 */

public class Scenario extends Reflector<Scenario> {
    static {
        addAllFields(Scenario.class);
    }

    //members mirro those in SOC so we can save a scenarion and load into an SOC without all the other
    // crap associated with SOC
    Board mBoard;
    Rules mRules;
}
