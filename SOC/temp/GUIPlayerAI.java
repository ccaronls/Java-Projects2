package cc.game.soc.swing;

import cc.game.soc.ai.AIEvaluator;

public class GUIPlayerAI extends GUIPlayer {

	public GUIPlayerAI() {
        //setAutoCancelTime(GUI.instance.getProps().getIntProperty("ai.maxProcessTimeMS", 30000));
        setEvaluator(new AIEvaluator());
	}
    
}
