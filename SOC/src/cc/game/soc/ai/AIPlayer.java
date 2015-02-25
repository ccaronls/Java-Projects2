package cc.game.soc.ai;

import java.util.*;

import cc.game.soc.core.*;
import cc.lib.game.Utils;

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
public class AIPlayer extends Player {

    public static boolean DEBUG_ENABLED = false;
    
    LinkedList<AINode> moveList = new LinkedList<AINode>();
	IEvaluator evaluator;
	
	private boolean canceled = false;
	private int autoCancelTime = 0;
	
	private final List<MoveType> skipMoves = new ArrayList<MoveType>();
	
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
	
	private AINode extractMove(SOC soc, NodeType expected) {
	    Utils.assertTrue(!moveList.isEmpty(), "Empty Move List!");
        AINode n = moveList.removeFirst();
        soc.logDebug("Player " + getPlayerNum() + " Extracted move: " + n + " moveList size=" + moveList.size());
	    Utils.assertTrue(n.type == expected, "Expected type %s but found %s", expected, n.type);
	    return n;
	}
	
	private void doEvaluation(AINode node, Player p, Board b, SOC soc) {
	    if (canceled)
	        return;
	    // TODO: deep copy soc and the board instead of trying to maintain state which could be error prone
	    int oldRoadLen = p.getRoadLength();
	    int oldPts = p.getPoints();
	    p.setRoadLength(b.computeMaxRouteLengthForPlayer(p.getPlayerNum(), soc.getRules().isEnableRoadBlock()));
	    int oldMaxRoadLenPlayer = soc.getLongestRoadPlayerNum();
        int oldMaxArmySizePlayer = soc.getLargestArmyPlayerNum();
        soc.setLongestRoadPlayer(SOC.computeLongestRoadPlayer(soc));
        soc.setLargestArmyPlayer(SOC.computeLargestArmyPlayer(soc));
	    p.setPoints(SOC.computePointsForPlayer(p, b, soc));
	    node.value = evaluator.evaluate(new PlayerTemp(p), b, soc, node.debug);
	    onEvaluation(node);
	    soc.setLargestArmyPlayer(oldMaxArmySizePlayer);
	    soc.setLongestRoadPlayer(oldMaxRoadLenPlayer);
	    p.setRoadLength(oldRoadLen);
	    p.setPoints(oldPts);
	}
	
	private AINode doSearchTree(SOC soc, AINode root) {	 
        AINode optimal = root.findOptimalMoves();
        if (DEBUG_ENABLED)
            root.debugDumpTree(soc);
	    evaluator.onOptimalPath(soc, optimal);
	    canceled = false;
	    return optimal;
	}
	
	@Override
	public GiveUpCardOption chooseCardToGiveUp(SOC soc, List<GiveUpCardOption> options, int numCardsToSurrender) {
	    Utils.assertTrue(moveList.isEmpty(), "Expected empty move list but found: %s", moveList);
	    moveList.clear();
	    PlayerTemp temp = new PlayerTemp(this);
	    AINode root = new AINode();
	    onBeginNewDescisionTree(soc);
	    for (GiveUpCardOption op : options) {
	    	temp.removeCard(op.getType());
            AINode n = root.attach(NodeType.GIVEUP_CARD, op.ordinal());
            doEvaluation(n, temp, soc.getBoard(), soc);
            temp.addCard(op.getType());
	    	/*
	        ResourceType t = op.getResourceType();
	        if (t != null) {
	            temp.incrementResource(t, -1);
	            AINode n = root.attach(NodeType.GIVEUP_RESOURCE_CARD, op.ordinal());
	            doEvaluation(n, temp, soc.getBoard(), soc);
	            temp.incrementResource(t, 1);
	            continue;
	        }

	        DevelopmentCardType d = op.getDevelopmentCardType();
	        Utils.assertTrue(d != null);
	        Card removed = temp.removeCard(d);
	        assert(!removed.isUsed());
	        AINode n = root.attach(NodeType.GIVEUP_DEVELOPMENT_CARD, op.ordinal());
	        doEvaluation(n, temp, soc.getBoard(), soc);
	        temp.addCard(removed);*/
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
		skipMoves.clear();
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

	@Override
	public Vertex chooseCityVertex(SOC soc, List<Integer> vertexIndices) {
	    Utils.assertTrue(moveList != null, "Empty move list");
	    return soc.getBoard().getVertex(extractMove(soc, NodeType.CITY_CHOICE).index);
	}
	
	@Override
	public Vertex chooseCityWallVertex(SOC soc, List<Integer> vertexIndices) {
	    Utils.assertTrue(moveList != null, "Empty move list");
	    return soc.getBoard().getVertex(extractMove(soc, NodeType.CITY_WALL_CHOICE).index);
	}

	@Override
	public Vertex chooseKnightVertex(SOC soc, List<Integer> vertexIndices) {
		Utils.assertTrue(moveList != null, "Empty move list");
	    return soc.getBoard().getVertex(extractMove(soc, NodeType.KNIGHT_CHOICE).index);
	}

	@Override
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
	    
	    System.out.println("Choose move...");
        if (moveList.size() > 0 && moveList.get(0).type == NodeType.MOVE_CHOICE) {
            System.out.println("case 0");
            return MoveType.values()[extractMove(soc, NodeType.MOVE_CHOICE).index];
        }

        moveList.clear();
        if (moves == null) {
//            System.out.println("case 1");
            moves = SOC.computeMoveOptions(this, soc.getBoard(), soc);
        }
        
        Utils.assertTrue(moves.size()>0, "Empty move list");
	    if (moves.size() == 1) {
//            System.out.println("case 2");
//	        Utils.assertTrue(moves.get(0) == MoveType.CONTINUE, "Expected CONTINUE but found: %s", moves.get(0));
	    	MoveType move = moves.get(0);
	        moveList.clear();
	        return move; // dont do anything just return
	    }

	    {

	        // this is tricky, we want to build a tree to determine the path for the greatest increase in value
	        Board b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        long start = System.currentTimeMillis();
	        buildChooseMoveTreeR(root, soc, this, b, moves, 0, new HashSet<Integer>());
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
	        System.out.println("Optimal Path: " + moveList);
	    }

        System.out.println("case 4");
	    return MoveType.values()[extractMove(soc, NodeType.MOVE_CHOICE).index];	
	}
	
	/*
	 * This method appends to any root a sequence of moves determined 'optimial' by
	 * the current evaluator.  The tree is constructed using a variation on min-max.
	 */
	private void buildChooseMoveTreeR(AINode root, SOC soc, Player p, Board b, List<MoveType> moves, int depth, Set<Integer> visitedRoutes) {
	    
		final boolean seafarers = soc.getRules().isEnableSeafarersExpansion();
		Collections.sort(moves); // sort the moves in ascending order
		moves.removeAll(skipMoves);
		
	    for (MoveType move : moves) {
	        if (canceled)
	            return;
            AINode t = root.attach(NodeType.MOVE_CHOICE, move.ordinal());
            switch (move) {
                case YEAR_OF_PLENTY_CARD:
                {
                	skipMoves.add(MoveType.YEAR_OF_PLENTY_CARD);
                	Card used = p.removeCard(DevelopmentCardType.YearOfPlenty);
                	assert(used != null);
                	assert(used.isUsable());
                    List<Card> disabled = p.setCardsUsable(false);
                    for (int i=0; i<ResourceType.values().length && !canceled; i++) {
                        ResourceType r = ResourceType.values()[i];
                        AINode n = t.attach(NodeType.RESOURCE_CHOICE, r.ordinal());
                        p.incrementResource(r, 1);
                        for (int ii=0; ii<=i; ii++) {
                            ResourceType r2 = ResourceType.values()[ii];
                            AINode n2 = n.attach(NodeType.RESOURCE_CHOICE,r2.ordinal());
                            p.incrementResource(r2,1);
                            List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);   
                            buildChooseMoveTreeR(n2, soc, p, b, moves2, depth+1, visitedRoutes);
                            p.incrementResource(r2, -1);
                        }
                        p.incrementResource(r, -1);
                    }
                    for (Card card : disabled) {
                    	card.setUsable(true);
                    }
                    p.addCard(used);
                }
                
                break;
                
                case TRADE:
                {
                	skipMoves.add(MoveType.TRADE);
                    List<Trade> trades = SOC.computeTradeOptions(p, b);
                    for (Trade trade : trades) {
                    	if (canceled)
                    		break;
                        AINode n = t.attach(trade);
                        p.incrementResource(trade.getType(), -trade.getAmount());
                        for (ResourceType r : ResourceType.values()) {
                            if (r == trade.getType())
                                continue; // dont need to trade N->1 for the same type!
                            AINode n2 = n.attach(NodeType.RESOURCE_CHOICE, r.ordinal());
                            p.incrementResource(r, 1);
                            List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                            buildChooseMoveTreeR(n2, soc, p, b, moves2, depth+1, visitedRoutes);
                            p.incrementResource(r, -1);
                        }
                        p.incrementResource(trade.getType(), trade.getAmount());
                    }
                }
                break;
                
                case BUILD_ROAD: 
                {
                    List<Integer> roadOptions = SOC.computeRoadOptions(p.getPlayerNum(), b);
                    roadOptions.removeAll(visitedRoutes);
                    for (int roadIndex : roadOptions) {
                    	visitedRoutes.add(roadIndex);
                        b.setPlayerForEdge(roadIndex, p.getPlayerNum());
                        p.adjustResourcesForBuildable(BuildableType.Road, -1);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                        AINode n = t.attach(NodeType.ROAD_CHOICE, roadIndex);
                        buildChooseMoveTreeR(n, soc, p, b, moves2, depth+1, visitedRoutes);
                        //doEvaluation(n, p, b, soc);
                        p.adjustResourcesForBuildable(BuildableType.Road, 1);
                        b.setPlayerForEdge(roadIndex, 0);
                    }
                    break;            
                }        
                
                case BUILD_SHIP:
                {
                	List<Integer> ships = SOC.computeShipOptions(p.getPlayerNum(), b);
                	ships.removeAll(visitedRoutes);
                	for (int index : ships) {
                		visitedRoutes.add(index);
                		p.adjustResourcesForBuildable(BuildableType.Ship, -1);
                		Route r = b.setPlayerForEdge(index, p.getPlayerNum());
                		r.setShip(true);
                		r.setLocked(true);
                		//doEvaluation(t.attach(NodeType.SHIP_CHOICE, index), p, b, soc);
                		AINode n = t.attach(NodeType.SHIP_CHOICE, index);
                		List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                        buildChooseMoveTreeR(n, soc, p, b, moves2, depth+1, visitedRoutes);
                		b.setPlayerForEdge(index, 0);
                		p.adjustResourcesForBuildable(BuildableType.Ship, 1);
                		r.setShip(false);
                		r.setLocked(false);
                	}
                	break;
                }
                
                case MOVE_SHIP: {
                	skipMoves.add(MoveType.MOVE_SHIP);
                	List<Integer> movableShips = SOC.computeMovableShipOptions(p.getPlayerNum(), b);
                	for (int index : movableShips) {
                		b.setPlayerForEdge(index, 0).setShip(false);
                		List<Integer> newShip = SOC.computeShipOptions(p.getPlayerNum(), b);
                		newShip.remove((Object)index);
                		newShip.removeAll(visitedRoutes);
                		AINode n = t.attach(NodeType.SHIP_MOVE_CHOICE, index);
                		for (int index2 : newShip) {
                			visitedRoutes.add(index2);
                			b.setPlayerForEdge(index2, p.getPlayerNum()).setShip(true);
                			AINode n2 = n.attach(NodeType.SHIP_CHOICE, index2);
                    		List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                			buildChooseMoveTreeR(n2, soc, p, b, moves2, depth+1, visitedRoutes);
                			b.setPlayerForEdge(index2, 0).setShip(false);
                		}
                		b.setPlayerForEdge(index, p.getPlayerNum()).setShip(true);
                	}
    				break;
                }
                    
                case ROAD_BUILDING_CARD:
                {
                	skipMoves.add(MoveType.ROAD_BUILDING_CARD);
                    List<Integer> roadOptions = SOC.computeRoadOptions(p.getPlayerNum(), b);
                    List<Integer> shipOptions = seafarers ? SOC.computeShipOptions(p.getPlayerNum(), b) : new ArrayList<Integer>();
                    
                    Card used = p.removeCard(DevelopmentCardType.RoadBuilding);
                	assert(used != null);
                	assert(used.isUsable());
                    List<Card> disabled = p.setCardsUsable(false);
                    // add the road options with the ONLY NEW road options and the ship options
                    for (int roadIndex : roadOptions) {
                    	b.setPlayerForEdge(roadIndex, p.getPlayerNum());
                    	AINode n = t.attach(NodeType.ROAD_CHOICE, roadIndex);
                    	List<Integer> roadOptions2 = SOC.computeRoadOptions(p.getPlayerNum(), b);
                    	roadOptions2.removeAll(roadOptions);
                    	for (int roadIndex2 : roadOptions2) {
                    		visitedRoutes.add(roadIndex2);
                    		b.setPlayerForEdge(roadIndex2, p.getPlayerNum());
                    		AINode n2 = n.attach(NodeType.ROAD_CHOICE, roadIndex2);
                    		//doEvaluation(n2, p, b, soc);
                    		List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                    		buildChooseMoveTreeR(n2, soc, p, b, moves2, depth++, visitedRoutes);
                    		b.setPlayerForEdge(roadIndex2, 0);
                    	}
                    	
                    	for (int shipIndex : shipOptions) {
                    		visitedRoutes.add(shipIndex);
                    		Route edge = b.getRoute(shipIndex);
                    		edge.setShip(true);
                    		b.setPlayerForRoute(edge, p.getPlayerNum());
                    		AINode n2 = n.attach(NodeType.SHIP_CHOICE, shipIndex);
                    		List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                    		buildChooseMoveTreeR(n2, soc, p, b, moves2, depth++, visitedRoutes);
                    		edge.setShip(false);
                    		b.setPlayerForRoute(edge, 0);
                    	}
                    	
                    	b.setPlayerForEdge(roadIndex, 0);
                    }
                    
                    for (int shipIndex : shipOptions) {
                		visitedRoutes.add(shipIndex);
                    	b.setPlayerForEdge(shipIndex, p.getPlayerNum()).setShip(true);
                    	AINode n2 = t.attach(NodeType.SHIP_CHOICE, shipIndex);
                    	List<Integer> shipOptions2 = SOC.computeShipOptions(p.getPlayerNum(), b);
                    	shipOptions2.removeAll(shipOptions);
                    	for (int shipIndex2 : shipOptions2) {
                    		visitedRoutes.add(shipIndex2);
                    		b.setPlayerForEdge(shipIndex2, p.getPlayerNum()).setShip(true);
                    		n2.attach(NodeType.SHIP_CHOICE, shipIndex2);
                    		List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                    		buildChooseMoveTreeR(n2, soc, p, b, moves2, depth++, visitedRoutes);
                    		b.setPlayerForEdge(shipIndex2, 0).setShip(false);
                    	}
                    	b.setPlayerForEdge(shipIndex, 0).setShip(false);
                    }
                    p.addCard(used);
                    for (Card card : disabled) {
                    	card.setUsable(true);
                    }
                    break;            
                }
                           
                case BUILD_SETTLEMENT:
                    {
                        // find all possible settlement moves and attach them to the tree
                        List<Integer> settlements = SOC.computeSettlementOptions(soc, p.getPlayerNum(), b);
                        for (int index: settlements) {
                            if (canceled)
                                break;
                            Vertex v = b.getVertex(index);
                            assert(v.getPlayer() == 0 && !v.isCity());
                            v.setPlayer(p.getPlayerNum());
                            p.adjustResourcesForBuildable(BuildableType.Settlement, -1);
                            if (soc.isRoadBlockEnabled() && b.checkForBlockingRoads(index, p.getPlayerNum()) > 0) {
                                Board temp = b.deepCopy();
                                soc.updatePlayerRoadsBlocked(temp, index);
                                AINode n = t.attach(NodeType.SETTLEMENT_CHOICE, index);
                                List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                        		buildChooseMoveTreeR(n, soc, p, temp, moves2, depth++, visitedRoutes);
                            } else {
                            	AINode n = t.attach(NodeType.SETTLEMENT_CHOICE, index);
                            	List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                        		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
                            }
                            p.adjustResourcesForBuildable(BuildableType.Settlement, 1);
                            v.setPlayer(0);
                        }
                    }
                    break;
    
                case BUILD_CITY:
                    {
                        // find all possible city moves and add them to the tree
                        List<Integer> cities = SOC.computeCityOptions(p.getPlayerNum(), b);
                        for (int index: cities) {
                            if (canceled)
                                break;
                            Vertex v = b.getVertex(index);
                            v.setType(VertexType.CITY);
                            p.adjustResourcesForBuildable(BuildableType.City, -1);
                            AINode n = t.attach(NodeType.CITY_CHOICE, index);
                            List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                    		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
                            p.adjustResourcesForBuildable(BuildableType.City, 1);
                            v.setType(VertexType.SETTLEMENT);
                        }
                    }
                    break;
                    
                case DRAW_DEVELOPMENT: {
                    p.adjustResourcesForBuildable(BuildableType.Development, -1);
                    Card added = new Card(DevelopmentCardType.Soldier, CardStatus.UNUSABLE);
                    p.addCard(added);
                    this.doEvaluation(t, p, b, soc);
                    // We stop here since the result is random we re evaluate if we decide to walk this tree
                    //List<MoveType> moves2 = SOC.computeMoveOptions(p, b);
                    //reduceList(moves2, new MoveType [] { MoveType.YEAR_OF_PLENTY_CARD, MoveType.TRADE, MoveType.ROAD_BUILDING_CARD, MoveType.BUILD_ROAD, MoveType.BUILD_SETTLEMENT, MoveType.BUILD_CITY });
                    //buildChooseMoveTreeR(t, soc, p, b, moves2, depth+1);
                    p.adjustResourcesForBuildable(BuildableType.Development, 1);
                    p.removeCard(added);
                    break;
                }
    
                case MONOPOLY_CARD: {
                	skipMoves.remove(MoveType.MONOPOLY_CARD);
                	Card used = p.removeCard(DevelopmentCardType.Monopoly);
                	assert(used != null);
                	assert(used.isUsable());
                	doEvaluation(t, p, b, soc);
                	t.value += evaluateMonopolyCard(soc, p, b);
                	p.addCard(used);//unUseDevelopmentCard(DevelopmentCardType.Monopoly);
                	break;
                }
                    
                case SOLDIER_CARD:
                    {
                    	skipMoves.remove(MoveType.SOLDIER_CARD);
                    	Card used = p.getCard(DevelopmentCardType.Soldier);
                    	assert(used != null);
                    	assert(used.isUsable());
                    	used.setUsed(true);
                        List<Integer> robberCells = SOC.computeRobberOptions(soc, b);
                        List<Integer> pirateCells = SOC.computePirateOptions(soc, b);
                        int oldRobber = b.getRobberTile();
                        for (int index : robberCells) {
                            AINode n = t.attach(NodeType.ROBBER_CHOICE, index);
                            b.setRobber(index);
                            this.doEvaluation(n, p, b, soc);
                            b.setRobber(oldRobber);
                        }
                        int oldPirate = b.getPirateTile();
                        for (int index : pirateCells) {
                        	AINode n = t.attach(NodeType.ROBBER_CHOICE, index);
                        	b.setPirate(index);
                            this.doEvaluation(n, p, b, soc);
                            b.setPirate(oldPirate);
                        }
                        used.setUsed(false);
                    }
                    break;
                    
                case BUILD_CITY_WALL: {
                    // find all possible city moves and add them to the tree
                    List<Integer> cities = SOC.computeCityWallOptions(p.getPlayerNum(), b);
                    for (int index: cities) {
                        if (canceled)
                            break;
                        Vertex v = b.getVertex(index);
                        v.setType(VertexType.WALLED_CITY);
                        p.adjustResourcesForBuildable(BuildableType.CityWall, -1);
                        AINode n = t.attach(NodeType.CITY_WALL_CHOICE, index);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
                        p.adjustResourcesForBuildable(BuildableType.CityWall, 1);
                        v.setType(VertexType.CITY);
                    }
                    break;
                }
                case IMPROVE_CITY_POLITICS: {
                	addImproveCityNodes(DevelopmentArea.Politics, p, soc, b, root, depth, visitedRoutes);
                	break;
                }
                case IMPROVE_CITY_SCIENCE: {
                	addImproveCityNodes(DevelopmentArea.Science, p, soc, b, root, depth, visitedRoutes);
                	break;
                }
                case IMPROVE_CITY_TRADE: {
                	addImproveCityNodes(DevelopmentArea.Trade, p, soc, b, root, depth, visitedRoutes);
                	break;
                }
                case PROMOTE_KNIGHT: {
                	p.adjustResourcesForBuildable(BuildableType.PromoteKnight, -1);
                	List<Integer> verts = SOC.computePromoteKnightOptions(p, b);
                	for (int vIndex : verts) {
                		Vertex v = b.getVertex(vIndex);
                		assert(v.isKnight());
                		VertexType type = v.getType();
                		v.promoteKnight();
                		AINode n = t.attach(NodeType.KNIGHT_CHOICE, vIndex);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
                		v.setType(type);
                	}
                	p.adjustResourcesForBuildable(BuildableType.PromoteKnight, 1);
                	break;
                }
                case HIRE_KNIGHT: {
                	p.adjustResourcesForBuildable(BuildableType.Knight, -1);
                	List<Integer> verts = SOC.computeNewKnightVertexOptions(p.getPlayerNum(), b);
                	for (int vIndex : verts) {
                		Vertex v = b.getVertex(vIndex);
                		assert(v.getType() == VertexType.OPEN);
                		v.setType(VertexType.BASIC_KNIGHT_INACTIVE);
                		AINode n = t.attach(NodeType.KNIGHT_CHOICE, vIndex);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
                		v.setType(VertexType.OPEN);
                	}
                	p.adjustResourcesForBuildable(BuildableType.Knight, 1);
                	break;
                }
                case MOVE_KNIGHT: {
                	List<Integer> verts = SOC.computeMovableKnights(p.getPlayerNum(), b);
                	for (int vIndex : verts) {
                		Vertex v = b.getVertex(vIndex);
                		assert(v.isKnight());
                		assert(v.getPlayer() == p.getPlayerNum());
                	}
                	break;
                }
                	
                case ACTIVATE_KNIGHT: {
                	p.adjustResourcesForBuildable(BuildableType.ActivateKnight, -1);
                	List<Integer> verts = SOC.computeActivateKnightOptions(p.getPlayerNum(), b);
                	for (int vIndex : verts) {
                		Vertex v = b.getVertex(vIndex);
                		assert(v.isKnight());
                		v.activateKnight();
                		AINode n = t.attach(NodeType.KNIGHT_CHOICE, vIndex);
                        List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
                		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
                		v.deactivateKnight();
                	}
                	p.adjustResourcesForBuildable(BuildableType.ActivateKnight, 1);
                	break;
                }
                case PLAY_PROGRESS_CARD: 
                    break;
                    
                case ROLL_DICE:
                	if (!moveList.isEmpty()) {
                		soc.logError("move list not empty!: " + moveList);
                	}
                	// fallthrough
                case CONTINUE:
                    this.doEvaluation(t, p, b, soc);
                    break;
				
            }
        }
    }
	
	private void addImproveCityNodes(DevelopmentArea area, Player p, SOC soc, Board b, AINode root, int depth, Set<Integer> visitedRoutes) {
		int devel = p.getCityDevelopment(area);
    	p.setCityDevelopment(area, devel++);
    	p.removeCards(area.commodity, devel+1);
    	AINode n = root.attach(NodeType.IMPROVE_CITY, area.ordinal());
    	List<MoveType> moves2 = SOC.computeMoveOptions(p, b, soc);
		buildChooseMoveTreeR(n, soc, p, b, moves2, depth++, visitedRoutes);
    	p.setCityDevelopment(area, devel);
    	p.addCards(area.commodity, devel+1);		
	}
	
	@Override
	public Player choosePlayerNumToTakeCardFrom(SOC soc, List<Player> playerOptions) {
	    //assert(moveList == null);
	    // choose the player with the most cards or the player whom has the most points?
	    AINode root = new AINode();
	    onBeginNewDescisionTree(soc);
	    for (int index=0; index<playerOptions.size() && !canceled; index++) {
	        Player p = playerOptions.get(index);
	        AINode n = root.attach(NodeType.TAKE_PLAYER_CARD, index);
	        this.doEvaluation(n, p, soc.getBoard(), soc);
	    }
        onDescisionTreeComplete();
	    AINode optimal = this.doSearchTree(soc, root);
	    return playerOptions.get(optimal.index);
	}

	@Override
	public ResourceType chooseResource(SOC soc) {
	    if (moveList.isEmpty() || moveList.getFirst().type != NodeType.RESOURCE_CHOICE) {
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
	    return ResourceType.values()[extractMove(soc, NodeType.RESOURCE_CHOICE).index];
	}

	
	
	@Override
	public Route chooseRoute(SOC soc, List<Integer> roadIndices, List<Integer> shipIndices) {
		
		if (roadIndices.size() > 0 && shipIndices.size() > 0) {

			Board b = soc.getBoard();
			AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        for (int rIndex : roadIndices) {
	        	AINode n = root.attach(NodeType.ROAD_CHOICE, rIndex);
	        	b.setPlayerForEdge(rIndex, getPlayerNum());
	        	doEvaluation(n, this, b, soc);
	        	b.setPlayerForEdge(rIndex, 0);
	        }
	        
	        for (int sIndex : shipIndices) {
	        	AINode n = root.attach(NodeType.SHIP_CHOICE, sIndex);
	        	b.setPlayerForEdge(sIndex, getPlayerNum()).setShip(true);
	        	doEvaluation(n, this, b, soc);
	        	b.setPlayerForEdge(sIndex, 0).setShip(false);
	        }
			onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
			if (optimal.type == NodeType.SHIP_CHOICE) {
				Route r = b.getRoute(optimal.index);
				r.setShip(true);
				return r;
			} else {
				return b.getRoute(optimal.index);
			}
	        
		} else if (roadIndices.size() > 0) {
			return chooseRoadEdge(soc, roadIndices);
		}
		return chooseShipEdge(soc, shipIndices);
	}
	
	private Route chooseRoadEdge(SOC soc, List<Integer> edgeIndices) {
	    if (moveList.isEmpty()) {
	        Board b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        for (int index : edgeIndices) {
	            if (canceled)
	                break;
	            b.setPlayerForEdge(index, getPlayerNum());
	            AINode n = root.attach(NodeType.ROAD_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	            b.setPlayerForEdge(index, 0);
	        }
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getRoute(optimal.index);
	    }
	    return soc.getBoard().getRoute(extractMove(soc, NodeType.ROAD_CHOICE).index);	
	}
	
	private Route chooseShipEdge(SOC soc, List<Integer> edgeIndices) {
		if (moveList.isEmpty()) {
	        Board b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        for (int index : edgeIndices) {
	            if (canceled)
	                break;
	            Route edge = b.getRoute(index);
	            //edge.setPlayer(getPlayerNum());
	            b.setPlayerForRoute(edge, getPlayerNum());
	            assert(!edge.isShip());
	            edge.setShip(true);
	            AINode n = root.attach(NodeType.SHIP_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	            //edge.setPlayer(0);
	            b.setPlayerForRoute(edge, 0);
	            edge.setShip(false);
	        }
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getRoute(optimal.index);
	    }
	    return soc.getBoard().getRoute(extractMove(soc, NodeType.SHIP_CHOICE).index);
	}

	@Override
	public Route chooseShipToMove(SOC soc, List<Integer> edgeIndices) {
	    return soc.getBoard().getRoute(extractMove(soc, NodeType.SHIP_MOVE_CHOICE).index);
	}

	@Override
	public Tile chooseRobberCell(SOC soc, List<Integer> cellIndices) {
	    if (moveList.isEmpty()) {
	        Board b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        int oldRobberCell = b.getRobberTile();
	        int oldPirateCell = b.getPirateTile();
	        for (int index : cellIndices) {
	            if (canceled)
	                break;
	            if (b.getTile(index).isWater()) {
	            	b.setPirate(index);
	            } else {
	            	b.setRobber(index);
	            }
	            AINode n = root.attach(NodeType.ROBBER_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	            b.setPirate(oldPirateCell);
	            b.setRobber(oldRobberCell);
	        }
	        b.setRobber(oldRobberCell);
	        b.setPirate(oldPirateCell);
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getTile(optimal.index);
	    }
	    int cell = extractMove(soc, NodeType.ROBBER_CHOICE).index;
	    //assert(moveList == null);
	    return soc.getBoard().getTile(cell);
	}

	@Override
	public Vertex chooseSettlementVertex(SOC soc, List<Integer> vertexIndices) {
	    if (moveList.isEmpty()) {
	        Board b = soc.getBoard();
	        AINode root = new AINode();
	        onBeginNewDescisionTree(soc);
	        for (int index : vertexIndices) {
	            if (canceled)
	                break;
	            Vertex v = b.getVertex(index);
	            assert(v.getPlayer() == 0 && !v.isCity());
	            v.setPlayer(getPlayerNum());
	            v.setType(VertexType.SETTLEMENT);
	            AINode n = root.attach(NodeType.SETTLEMENT_CHOICE, index);
	            this.doEvaluation(n, this, b, soc);
	            v.setPlayer(0);
	            v.setType(VertexType.OPEN);
	        }
	        onDescisionTreeComplete();
	        AINode optimal = this.doSearchTree(soc, root);
	        return b.getVertex(optimal.index);
	    }
	    return soc.getBoard().getVertex(extractMove(soc, NodeType.SETTLEMENT_CHOICE).index);	
	}

	@Override
	public Trade chooseTradeOption(SOC soc, List<Trade> trades) {
	    Trade t = extractMove(soc, NodeType.TRADE_CHOICE).trade;
	    Utils.assertTrue(trades == null || trades.contains(t));
	    return t;
	}
	
	@Override
	public DevelopmentArea chooseDevelopmentArea(SOC soc) {
		assert(moveList.isEmpty());
		PlayerTemp p = new PlayerTemp(this);
		AINode root = new AINode();
        onBeginNewDescisionTree(soc);
		for (DevelopmentArea d : DevelopmentArea.values()) {
			AINode t = root.attach(NodeType.DEVELOPMENT_AREA_CHOICE, d.ordinal());
			t.value = evaluateDevelopmentAreaChoice(soc, p, soc.getBoard(), d);
		}
		onDescisionTreeComplete();
        AINode optimal = this.doSearchTree(soc, root);
        return DevelopmentArea.values()[optimal.index];
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
    
	/**
	 * Simplistic method to score using the monopoly card.  Here, we count how many cards are in the other players' hands
	 * to determine to odds that we will get a resource we need
	 * 
	 * @param soc
	 * @param p
	 * @param b
	 * @return
	 */
	protected float evaluateMonopolyCard(SOC soc, Player p, Board b) {
		float totalCards = 0;
		for (Player player : soc.getPlayers()) {
			if (player.getPlayerNum() != p.getPlayerNum()) {
				totalCards += player.getTotalResourceCount(); 
			}
		}
		float aveNum = totalCards / (soc.getNumPlayers()-1);
		return aveNum - p.getTotalResourceCount(); // if we have less cards than average, then this is good, else bad
	}
	
	protected float evaluateDevelopmentAreaChoice(SOC soc, Player p, Board b, DevelopmentArea area) {
		// choose the area for which we have the least likelyhood of getting
		return 10 - p.getCityDevelopment(area);
	}
	
}
