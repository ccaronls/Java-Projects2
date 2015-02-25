package cc.game.soc.ai;

import java.io.PrintWriter;

import cc.game.soc.core.Board;
import cc.game.soc.core.Player;
import cc.game.soc.core.SOC;

public interface IEvaluator {

    /**
     * Evaluate
     * @param p
     * @param b
     * @param soc
     * @return
     */
	float evaluate(Player p, Board b, SOC soc, PrintWriter debugOutput);

	/**
     * Called when all nodes in the tree are evaluated and the optimal path determined.	 
     * 
     * @param optimal the highest valued 'leaf'
	 */
	void onOptimalPath(SOC soc, AINode optimal);
	
    /**
     * Called when any of the AIPlayer choose methods gets called and the player
     * must construct a new descision tree.
     * 
     * @param node
     * @param soc
     */
    void onBeginNewDescisionTree();
    
    void onDescisionTreeComplete();
	
}
