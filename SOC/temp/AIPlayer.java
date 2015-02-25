package cc.game.soc.ai;

import java.util.*;

import cc.game.soc.core.*;
import cc.game.soc.core.IConfig.Key;

/**
 * AIPlayer is a player type that uses a decision tree in combination with
 * a generalized 'Evaluator' to determine the best sequence of moves, or 
 * optimal path through the tree.
 * 
 * Actual tree construction is complex and subject to repeated optimization
 * but in general it has the form of a min-max descision tree; really more
 * of a n-max since counter moves are not calculated, but n number of
 * leaves is choosen such that they represent the highest valued moves.
 * 
 * While tree construction details are hidden, the evaluator interface allows for
 * great control over AI.  The evaluator simply needs to return a 'score' for a
 * particular game arrangement.  In general, only leaf nodes are evaluated, so this
 * method can be complex as it is called log(n) times, with n being the number of
 * nodes in the tree.
 * 
 * Because the evaluation time has a difficult to determine upper bound, we have
 * a cancel method to stop the recursion.
 * 
 * There are cases when an outcome is random, for instance placing the robber and taking
 * a card from another player.  In these situations the tree construction is stopped.
 * The SOC game runner will reconstruct a new tree on the following move.  Other cases
 * is after a monopoly card is played
 * 
 * @author ccaron
 *
 */
public class AIPlayer extends SOCPlayer {

    List<AINode> moveList = new ArrayList<AINode>(32);
	IEvaluator evaluator;
	
	//final private boolean doOptimization1 = false;
	final private boolean doOptimization2 = true;
	
	final private int maxChildren = 3;
	final private int maxDepth = 5;
	
	private boolean canceled = false;
	private int autoCancelTime = 0;
	
	/**
	 * Create an AI with AIEvaluator tuned with custom properties
	 * 
	 * @param properties
	 */
	public AIPlayer(Properties properties) {
	    this(new AIEvaluator(properties));
	}
	
	/**
	 * Create an AI with default Evaluator interface 
	 */
	public AIPlayer() {
	    this(new AIEvaluator());
	}
	
	/**
	 * Create an AI with custom AI interface
	 * 
	 * @param evaluator
	 */
	public AIPlayer(IEvaluator evaluator) {
	    this.evaluator = evaluator;
	}
	
	/**
	 * Set the evaluator for this AI
	 * 
	 * @param evaluator
	 */
	public void setEvaluator(IEvaluator evaluator) {
	    this.evaluator = evaluator;
	}
	
	public void cancelSearch() {
	    this.canceled = true;
	    this.mAutoCancelTrigger = false;
	    synchronized (autoCancelMonitor) {
	        autoCancelMonitor.notify();
	    }
	}
	
	public void setAutoCancelTime(int maxProcessTimeMS) {
	    this.autoCancelTime = maxProcessTimeMS;
	}
	
	private AINode extractMove(NodeType expected) {
	    //debug("Extract move: %s\n", getNodeTypeString((*moves)->type));
	    assert(!moveList.isEmpty());
	    assert(moveList.get(0).type == expected);
	    AINode n = moveList.remove(0);
	    return n;
	}
	
	private void doEvaluation(AINode node, SOCPlayer p, SOCBoard b, SOC soc) {
	    if (canceled)
	        return;
	    int oldRoadLen = p.getRoadLength();
	    int oldPts = p.getPoints();
	    p.setRoadLength(b.computeMaxRoadLengthForPlayer(p.getPlayerNum()));
	    int oldMaxRoadLenPlayer = soc.getLongestRoadPlayerNum();
        int oldMaxArmySizePlayer = soc.getLargestArmyPlayerNum();
        soc.setLongestRoadPlayer(SOC.computeLongestRoadPlayer(soc));
        soc.setLargestArmyPlayer(SOC.computeLargestArmyPlayer(soc));
	    p.setPoints(SOC.computePointsForPlayer(p, b, soc));
	    node.value = evaluator.evaluate(new PlayerTemp(p), b, soc);
	    onEvaluation(node);
	    soc.setLargestArmyPlayer(oldMaxArmySizePlayer);
	    soc.setLongestRoadPlayer(oldMaxRoadLenPlayer);
	    p.setRoadLength(oldRoadLen);
	    p.setPoints(oldPts);
	}
	
	private AINode doSearchTree(SOC soc, AINode root) {	 
	    AINode optimal = root.findOptimalMoves();
	    evaluator.onOptimalPath(soc, optimal);
	    canceled = false;
	    return optimal;
	}
	
	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseCardToGiveUp(cc.game.soc.core.SOC, java.util.List)
	 */
	public GiveUpCardOption chooseCardToGiveUp(SOC soc, List<GiveUpCardOption> options, int numCardsToSurrender) {
	    
	    assert(moveList.isEmpty());
	    PlayerTemp temp = new PlayerTemp(this);
	    AINode root = new AINode();
	    onBeginNewDescisionTree(soc);
	    for (GiveUpCardOption op : options) {
	        if (canceled)
	            break;
	        ResourceType t = op.getResourceType();
	        if (t != null) {
	            temp.incrementResource(t, -1);
	            AINode n = root.attach(NodeType.GIVEUP_RESOURCE_CARD, op.ordinal());
	            doEvaluation(n, temp, soc.getBoard(), soc);
	            temp.incrementResource(t, 1);
	            continue;
	        }

	        DevelopmentCardType d = op.getDevelopmentCardType();
	        assert(d != null);
	        temp.removeDevelopmentCard(d, DevelopmentCard.UNUSED);
	        AINode n = root.attach(NodeType.GIVEUP_DEVELOPMENT_CARD, op.ordinal());
	        doEvaluation(n, temp, soc.getBoard(), soc);
	        temp.addDevelopmentCard(d, DevelopmentCard.UNUSED);
	    }
        onDescisionTreeComplete();
	    AINode optimal = doSearchTree(soc, root);
	    return GiveUpCardOption.values()[optimal.index];
	}

	private boolean mAutoCancelTrigger = false;
	private final Object autoCancelMonitor = new Object();
	
	private void onDescisionTreeComplete() {
	    cancelSearch();
	    evaluator.onDescisionTreeComplete();
	}
	
	private void onBeginNewDescisionTree(final SOC soc) {
        evaluator.onBeginNewDescisionTree();
        // start a thread to auto cancel if this feature is enabled
	    if (this.autoCancelTime > 0) {
	        mAutoCancelTrigger = true;
	        new Thread(new Runnable() {
	            public void run() {
	                try {
	                    int num = autoCancelTime / 5000;
	                    for (int i=0; mAutoCancelTrigger && i<num; i++) {
	                        int seconds = (num-i)*5;
	                        soc.logDebug("AIPlayer : " + getPlayerNum() + " will auto cancel in " + seconds + " seconds");
    	                    synchronized (autoCancelMonitor) {
    	                        autoCancelMonitor.wait(5000);
    	                    }
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	                if (mAutoCancelTrigger) {
	                    soc.logDebug("AUTO CANCEL AFTER " + (autoCancelTime)/1000 + " seconds");
	                    cancelSearch();
	                }
	            }
	        }).start();
	    }
    }

    /* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseCityVertex(cc.game.soc.core.SOC, java.util.List)
	 */
	public SOCVertex chooseCityVertex(SOC soc, List<Integer> vertexIndices) {
	    assert(moveList != null);
	    return soc.getBoard().getVertex(extractMove(NodeType.CITY_CHOICE).index);
	}

	private void reduceList(List<MoveType> moves, MoveType [] removed) {
	    moves.removeAll(Arrays.asList(removed));
	}
	
	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseMove(cc.game.soc.core.SOC, java.util.List)
	 */
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
	    
        if (moveList.size() > 0 && moveList.get(0).type == NodeType.MOVE_CHOICE) {
            return MoveType.values()[extractMove(NodeType.MOVE_CHOICE).index];
        }

        moveList.clear();
        if (moves == null) {
            moves = SOC.computeMoveOptions(this, soc.getBoard());
        }
        
        assert(moves.size()>0);
	    if (moves.size() == 1) {
	        assert(moves.get(0) == MoveType.CONTINUE);
	        moveList.clear();
	        return MoveType.CONTINUE; // dont do anything just return
	    }

	    // if we have a monopoly card or a victory card, play these right away
	    // since they are
	    for (MoveType move: moves) {
	        if (move == MoveType.VICTORY_CARD ||
	            move == MoveType.MONOPOLY_CARD)
	            return move;
	    }
	    
	    {
	        // this is tricky, we want to build a tree to determine the path for the greatest increase in value
	        SOCBoard b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        long start = System.currentTimeMillis();
	        buildChooseMoveTreeR(root, soc, this, b, moves, 0);
	        long end = System.currentTimeMillis();
	        if (soc.isDebugEnabled())
	            soc.logDebug(String.format("Descision tree built in %3.2f seconds", (float)(end-start) / 1000));
	        onNewSearchTree(root);
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);

	        // now traverse from the optimal node back to the root
	        // and construct our moves
	        // assert(optimal != null && optimal.isTerminatingNode());
	        
	        moveList.clear();
	        moveList.addAll(optimal.getPath());
	    }

	    return MoveType.values()[extractMove(NodeType.MOVE_CHOICE).index];	
	}
	
	/*
	 * This method appends to any root a sequence of moves determined 'optimial' by
	 * the current evaluator.  The tree is constructed using a variation on min-max.
	 */
	private void buildChooseMoveTreeR(AINode root, SOC soc, SOCPlayer p, SOCBoard b, List<MoveType> moves, int depth) {
        if (this.doOptimization2 && depth > maxDepth)
            return;
	    
	    for (MoveType move : moves) {
	        if (canceled)
	            return;
            AINode t = root.attach(NodeType.MOVE_CHOICE, move.ordinal());
            switch (move) {
                case YEAR_OF_PLENTY_CARD:
                {
                    p.useDevelopmentCard(DevelopmentCardType.YearOfPlenty);
                    for (int i=0; i<ResourceType.values().length && !canceled; i++) {
                        ResourceType r = ResourceType.values()[i];
                        AINode n = t.attach(NodeType.RESOURCE_CHOICE, r.ordinal());
                        p.incrementResource(r, 1);
                        for (int ii=0; ii<=i; ii++) {
                            ResourceType r2 = ResourceType.values()[ii];
                            AINode n2 = n.attach(NodeType.RESOURCE_CHOICE,r2.ordinal());
                            p.incrementResource(r2,1);
                            List<MoveType> moves2 = SOC.computeMoveOptions(p, b);                            
                            buildChooseMoveTreeR(n2, soc, p, b, moves2, depth+1);
                            p.incrementResource(r2, -1);
                        }
                        p.incrementResource(r, -1);
                    }
                    p.unUseDevelopmentCard(DevelopmentCardType.YearOfPlenty);
                }
                break;
                
                case TRADE:
                {
                    List<SOCTrade> trades = SOC.computeTradeOptions(p, b);
                    Iterator<SOCTrade> it2 = trades.iterator();
                    while (it2.hasNext() && !canceled) {
                        SOCTrade trade = it2.next();
                        AINode n = t.attach(trade);
                        p.incrementResource(trade.getType(), -trade.getAmount());
                        for (ResourceType r : ResourceType.values()) {
                            if (r == trade.getType())
                                continue; // dont need to trade N->1 for the same type!
                            AINode n2 = n.attach(NodeType.RESOURCE_CHOICE, r.ordinal());
                            p.incrementResource(r, 1);
                            //this.doEvaluation(n2, p, b, soc);
                            List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                            reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD });
                            buildChooseMoveTreeR(n2, soc, p, b, moves2, depth+1);
                            p.incrementResource(r, -1);
                        }
                        p.incrementResource(trade.getType(), trade.getAmount());
                    }
                }
                break;
                
            case ROAD_BUILDING_CARD:
            {
                List<Integer> roadOptions = SOC.computeRoadOptions(p, b);
                p.useDevelopmentCard(DevelopmentCardType.RoadBuilding);

                if (this.doOptimization2 && roadOptions.size() > this.maxChildren) {
                    for (int index: roadOptions) {
                        AINode n = t.attach(NodeType.ROAD_CHOICE, index);
                        SOCEdge e1 = b.getEdge(index);
                        assert(e1.getPlayer() == 0);
                        e1.setPlayer(p.getPlayerNum());
                        this.doEvaluation(n, p, b, soc);
                        e1.setPlayer(0);
                    }
                    
                    t.sortChildren();
                    
                    AINode cur = t.left;
                    for (int i=0; i<this.maxChildren && cur!=null; i++, cur=cur.next) {
                        SOCEdge e1 = b.getEdge(cur.index);
                        assert(e1.getPlayer() == 0);
                        e1.setPlayer(p.getPlayerNum());
                        // choose the second road
                        for (int index2: roadOptions) {
                            if (index2 == cur.index)
                                continue;
                            AINode n2 = cur.attach(NodeType.ROAD_CHOICE, index2);
                            SOCEdge e2 = b.getEdge(index2);
                            assert(e2.getPlayer() == 0);
                            e2.setPlayer(p.getPlayerNum());
                            this.doEvaluation(n2, p, b, soc);
                            e2.setPlayer(0);
                        }
                        
                        cur.sortChildren();
                        
                        AINode cur2 = cur.left;
                        for (int ii=0; ii<maxChildren && cur2 != null; ii++, cur2=cur2.next) {
                            SOCEdge e2 = b.getEdge(cur2.index);
                            assert(e2.getPlayer() == 0);
                            e2.setPlayer(p.getPlayerNum());
                            List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                            reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE });
                            buildChooseMoveTreeR(cur2, soc, p, b, moves2, depth+1);
                            assert(e2.getPlayer() == p.getPlayerNum());
                            e2.setPlayer(0);
                        }
                        
                        e1.setPlayer(0);
                    }
                    
                } else 
                //*/
                {
                    for (int index: roadOptions) {
                        if (canceled)
                            break;
                        AINode n = t.attach(NodeType.ROAD_CHOICE, index);
                        SOCEdge e1 = b.getEdge(index);
                        assert(e1.getPlayer() == 0);
                        e1.setPlayer(p.getPlayerNum());
                        for (int index2: roadOptions) {
                            if (index2 == index)
                                continue; // optimization, dont consider the prev index and avaoid a cell to computeRoadOptions
                            AINode n2 = n.attach(NodeType.ROAD_CHOICE, index2);
                            if (canceled)
                                break;
                            SOCEdge e2 = b.getEdge(index2);
                            assert(e2.getPlayer() == 0);
                            e2.setPlayer(p.getPlayerNum());
                            // evaluate and stop
                            this.doEvaluation(n2, p, b, soc);
                            //List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                            //reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE });
                            //buildChooseMoveTreeR(n2, soc, p, b, moves2, depth+1);
                            assert(e2.getPlayer() == p.getPlayerNum());
                            e2.setPlayer(0);
                        }
                        e1.setPlayer(0);
                    }

                }
                p.unUseDevelopmentCard(DevelopmentCardType.RoadBuilding);
                break;            
            }
                       
            case BUILD_ROAD: 
            {
                List<Integer> roadOptions = SOC.computeRoadOptions(p, b);
                if (this.doOptimization2 && roadOptions.size() > this.maxChildren) {
                    // reduce the children to only those with the highet value
                    for (int roadIndex : roadOptions) {
                        if (canceled)
                            break;
                        AINode n = t.attach(NodeType.ROAD_CHOICE, roadIndex);
                        SOCEdge e = b.getEdge(roadIndex);
                        assert(e.getPlayer() == 0);
                        e.setPlayer(p.getPlayerNum());
                        p.adjustResourcesForBuildable(BuildableType.Road, -1);
                        this.doEvaluation(n, p, b, soc);
                        p.adjustResourcesForBuildable(BuildableType.Road, 1);
                        e.setPlayer(0);
                    }
                    
                    // sort in decreasing order by evaluation 'value'
                    t.sortChildren();
                    //new Comparator<AINode>() {
                      //  public int compare(AINode arg0, AINode arg1) { return arg1.value - arg0.value >= 0 ? 1 : -1; }
                    //});
                    
                    // now only operate on the most valued
                    AINode c = t.left;
                    for (int i=0; i<maxChildren && c != null; i++) {
                        assert(c != null);
                        SOCEdge e = b.getEdge(c.index);
                        assert(e.getPlayer() == 0);
                        e.setPlayer(p.getPlayerNum());
                        p.adjustResourcesForBuildable(BuildableType.Road, -1);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                        reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE, MoveType.ROAD_BUILDING_CARD });
                        buildChooseMoveTreeR(c, soc, p, b, moves2, depth+1);
                        p.adjustResourcesForBuildable(BuildableType.Road, 1);
                        e.setPlayer(0);
                        c = c.next;
                    }
                    
                    // detach anything remaining
                    if (c != null) {
                        c.detatch();
                    }
                    
                } else {
                
                    for (int roadIndex : roadOptions) {
                        AINode n = t.attach(NodeType.ROAD_CHOICE, roadIndex);
                        SOCEdge e = b.getEdge(roadIndex);
                        assert(e.getPlayer() == 0);
                        e.setPlayer(p.getPlayerNum());
                        p.adjustResourcesForBuildable(BuildableType.Road, -1);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                        reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE, MoveType.ROAD_BUILDING_CARD });
                        buildChooseMoveTreeR(n, soc, p, b, moves2, depth+1);
                        p.adjustResourcesForBuildable(BuildableType.Road, 1);
                        e.setPlayer(0);
                    }
                }
                break;            
            }
            
            case BUILD_SETTLEMENT:
                {
                    // find all possible settlement moves and attach them to the tree
                    List<Integer> settlements = SOC.computeSettlementOptions(p, b);
                    for (int index: settlements) {
                        if (canceled)
                            break;
                        AINode n = t.attach(NodeType.SETTLEMENT_CHOICE, index);
                        if (soc.getConfig().getInt(Key.ENABLE_ROAD_BLOCK) != 0 && b.checkForBlockingRoads(index, p.getPlayerNum()) > 0) {
                            SOCBoard temp = new SOCBoard(b);
                            soc.updatePlayerRoadsBlocked(temp, index);
                            b = temp;
                        }
                        b.getVertex(index).setPlayer(p.getPlayerNum());
                        p.adjustResourcesForBuildable(BuildableType.Settlement, -1);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                        reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE, MoveType.ROAD_BUILDING_CARD, MoveType.BUILD_ROAD });
                        buildChooseMoveTreeR(n, soc, p, b, moves2, depth+1);
                        p.adjustResourcesForBuildable(BuildableType.Settlement, 1);
                        b.getVertex(index).setPlayer(0);
                    }
                }
                break;

            case BUILD_CITY:
                {
                    // find all possible city moves and add them to the tree
                    List<Integer> cities = SOC.computeCityOptions(p, b);
                    for (int index: cities) {
                        if (canceled)
                            break;
                        AINode n = t.attach(NodeType.CITY_CHOICE, index);
                        SOCVertex v = b.getVertex(index);
                        assert(v.getPlayer() == p.getPlayerNum());
                        v.setIsCity(true);
                        p.adjustResourcesForBuildable(BuildableType.City, -1);
                        //this.doEvaluation(n, p, b, soc);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                        reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE, MoveType.ROAD_BUILDING_CARD, MoveType.BUILD_ROAD, MoveType.BUILD_SETTLEMENT });
                        buildChooseMoveTreeR(n, soc, p, b, moves2, depth+1);
                        p.adjustResourcesForBuildable(BuildableType.City, 1);
                        v.setIsCity(false);
                    }
                }
                break;
            case DRAW_DEVELOPMENT: {
                p.adjustResourcesForBuildable(BuildableType.Development, -1);
                p.addDevelopmentCard(DevelopmentCardType.Soldier, DevelopmentCard.NOT_USABLE);
                this.doEvaluation(t, p, b, soc);
                // We stop here since the result is random we re evaluate if we decide to walk this tree
                //List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                //reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE, MoveType.ROAD_BUILDING_CARD, MoveType.BUILD_ROAD, MoveType.BUILD_SETTLEMENT, MoveType.BUILD_CITY });
                //buildChooseMoveTreeR(t, soc, p, b, moves2, depth+1);
                p.adjustResourcesForBuildable(BuildableType.Development, 1);
                p.removeDevelopmentCard(DevelopmentCardType.Soldier, DevelopmentCard.NOT_USABLE);
                break;
            }

            case VICTORY_CARD:
            case MONOPOLY_CARD:
                assert(false); // && "Should never get here");
                break;

            case SOLDIER_CARD:
                {
                    p.useDevelopmentCard(DevelopmentCardType.Soldier);
                    List<Integer> cells = SOC.computeRobberOptions(b);
                    int oldRobber = b.getRobberCell();
                    for (int index : cells) {
                        AINode n = t.attach(NodeType.ROBBER_CHOICE, index);
                        b.setRobber(index);
                        this.doEvaluation(n, p, b, soc);
                        b.setRobber(oldRobber);
                    }
                    t.sortChildren();
                    // reduce to top five and stop 
                    p.unUseDevelopmentCard(DevelopmentCardType.Soldier);
                }
                break;
                
            case CONTINUE:
                this.doEvaluation(t, p, b, soc);
                break;
            }
        }
    }

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#choosePlayerNumToTakeCardFrom(cc.game.soc.core.SOC, java.util.List)
	 */
	public SOCPlayer choosePlayerNumToTakeCardFrom(SOC soc, List<SOCPlayer> playerOptions) {
	    //assert(moveList == null);
	    // choose the player with the most cards or the player whom has the most points?
	    AINode root = new AINode();
	    onBeginNewDescisionTree(soc);
	    for (int index=0; index<playerOptions.size() && !canceled; index++) {
	        SOCPlayer p = playerOptions.get(index);
	        AINode n = root.attach(NodeType.TAKE_PLAYER_CARD, index);
	        this.doEvaluation(n, p, soc.getBoard(), soc);
	    }
        onDescisionTreeComplete();
	    AINode optimal = this.doSearchTree(soc, root);
	    return playerOptions.get(optimal.index);
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseResource(cc.game.soc.core.SOC)
	 */
	public ResourceType chooseResource(SOC soc) {
	    if (moveList.isEmpty()) {
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        PlayerTemp temp = new PlayerTemp(this);
	        for (ResourceType t : ResourceType.values()) {	   
	            if (canceled)
	                break;
	            temp.incrementResource(t, 1);
	            AINode n = root.attach(NodeType.RESOURCE_CHOICE, t.ordinal());
	            this.doEvaluation(n, temp, soc.getBoard(), soc);
	            temp.incrementResource(t, -1);
	        }
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return ResourceType.values()[optimal.index];
	    }
	    return ResourceType.values()[extractMove(NodeType.RESOURCE_CHOICE).index];
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseRoadEdge(cc.game.soc.core.SOC, java.util.List)
	 */
	public SOCEdge chooseRoadEdge(SOC soc, List<Integer> edgeIndices) {
	    if (moveList.isEmpty()) {
	        SOCBoard b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        for (int index : edgeIndices) {
	            if (canceled)
	                break;
	            SOCEdge edge = b.getEdge(index);
	            edge.setPlayer(getPlayerNum());
	            AINode n = root.attach(NodeType.ROAD_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	            edge.setPlayer(0);
	        }
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getEdge(optimal.index);
	    }
	    return soc.getBoard().getEdge(extractMove(NodeType.ROAD_CHOICE).index);	
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseRobberCell(cc.game.soc.core.SOC, java.util.List)
	 */
	public SOCCell chooseRobberCell(SOC soc, List<Integer> cellIndices) {
	    if (moveList.isEmpty()) {
	        SOCBoard b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        int oldRobberCell = b.getRobberCell();
	        for (int index : cellIndices) {
	            if (canceled)
	                break;
	            b.setRobber(index);
	            AINode n = root.attach(NodeType.ROBBER_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	        }
	        b.setRobber(oldRobberCell);
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getCell(optimal.index);
	    }
	    int cell = extractMove(NodeType.ROBBER_CHOICE).index;
	    //assert(moveList == null);
	    return soc.getBoard().getCell(cell);
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseSettlementVertex(cc.game.soc.core.SOC, java.util.List)
	 */
	public SOCVertex chooseSettlementVertex(SOC soc, List<Integer> vertexIndices) {
	    if (moveList.isEmpty()) {
	        SOCBoard b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        for (int index : vertexIndices) {
	            if (canceled)
	                break;
	            SOCVertex v = b.getVertex(index);
	            v.setPlayer(getPlayerNum());
	            AINode n = root.attach(NodeType.SETTLEMENT_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	            v.setPlayer(0);
	        }
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getVertex(optimal.index);
	    }
	    return soc.getBoard().getVertex(extractMove(NodeType.SETTLEMENT_CHOICE).index);	
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#chooseTradeOption(cc.game.soc.core.SOC, java.util.List)
	 */
	public SOCTrade chooseTradeOption(SOC soc, List<SOCTrade> trades) {
	    assert(moveList.size() > 0);
	    SOCTrade t = extractMove(NodeType.TRADE_CHOICE).trade;
	    assert(trades == null || trades.contains(t));
	    return t;
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#rollDice()
	 */
	public boolean rollDice() {
		return true;
	}

	/**
	 * Called when a new search tree has bee constructed
	 * base method does nothing
	 * @param node
	 */
	public void onNewSearchTree(AINode node) {
	    
	}
	
	/**
	 * Called each time a node is evaluated.
	 * base method does nothing.
	 * @param node
	 */
	public void onEvaluation(AINode node) {
	    
	}
    
}
