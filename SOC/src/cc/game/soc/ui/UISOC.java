package cc.game.soc.ui;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

import cc.game.soc.core.Card;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.Player;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Tile;
import cc.game.soc.core.Trade;
import cc.game.soc.core.Vertex;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.math.MutableVector2D;

/**
 * Created by chriscaron on 2/22/18.
 */

public abstract class UISOC extends SOC {

    public UIBoard getUIBoard() {
        return (UIBoard)getBoard();
    }

    Object returnValue = null;
    final Object waitObj = new Object();

    public void setReturnValue(Object o) {
        returnValue = o;
        synchronized (waitObj) {
            waitObj.notify();
        }
    }

    public abstract void clearMenu();

    public abstract MoveType chooseMoveMenu(Collection<MoveType> moves);

    public abstract Player.RouteChoiceType chooseRouteType();

    public GColor getPlayerColor(int playerNum) {
        return ((UIPlayer)getPlayerByPlayerNum(playerNum)).getColor();
    }

    public Vertex chooseVertex(final Collection<Integer> vertexIndices, final int playerNum, final Player.VertexChoice choice) {
        clearMenu();
        getUIBoard().setPickHandler(new UIBoard.PickHandler() {

            @Override
            public void onPick(UIBoard b, int pickedValue) {
                b.setPickHandler(null);
                setReturnValue(getBoard().getVertex(pickedValue));
            }

            @Override
            public void onHighlighted(UIBoard b, APGraphics g, int highlightedIndex) {
                Vertex v = getBoard().getVertex(highlightedIndex);
                g.setColor(getPlayerColor(getCurPlayerNum()));
                switch (choice) {
                    case SETTLEMENT:
                        b.drawSettlement(g, v, v.getPlayer(), true);
                        break;
                    case CITY:
                        b.drawCity(g, v, v.getPlayer(), true);
                        break;
                    case CITY_WALL:
                        b.drawWalledCity(g, v, v.getPlayer(), true);
                        break;
                    case KNIGHT_DESERTER:
                    case KNIGHT_DISPLACED:
                    case KNIGHT_MOVE_POSITION:
                    case KNIGHT_TO_MOVE:
                    case OPPONENT_KNIGHT_TO_DISPLACE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), v.getType().isKnightActive(), false);
                        g.setColor(GColor.RED);
                        b.drawCircle(g, v);
                        break;
                    case KNIGHT_TO_ACTIVATE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), true, true);
                        break;
                    case KNIGHT_TO_PROMOTE:
                        b.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel() + 1, v.getType().isKnightActive(), true);
                        break;
                    case NEW_KNIGHT:
                        b.drawKnight(g, v, v.getPlayer(), 1, false, true);
                        g.setColor(GColor.RED);
                        b.drawCircle(g, v);
                        break;
                    case POLITICS_METROPOLIS:
                        b.drawMetropolisPolitics(g, v, v.getPlayer(), true);
                        break;
                    case SCIENCE_METROPOLIS:
                        b.drawMetropolisScience(g, v, v.getPlayer(), true);
                        break;
                    case TRADE_METROPOLIS:
                        b.drawMetropolisTrade(g, v, v.getPlayer(), true);
                        break;
                    case PIRATE_FORTRESS:
                        b.drawPirateFortress(g, v, true);
                        break;
                    case OPPONENT_STRUCTURE_TO_ATTACK:
                        b.drawVertex(g, v, v.getType(), v.getPlayer(), true);
                        break;
                }

            }

            @Override
            public void onDrawPickable(UIBoard b, APGraphics g, int index) {
                Vertex v = getBoard().getVertex(index);
                GColor color = getPlayerColor(getCurPlayerNum()).setAlpha(120);
                g.setColor(color);
                switch (choice) {
                    case SETTLEMENT:
                        b.drawSettlement(g, v, 0, false);
                        break;
                    case CITY:
                        b.drawCity(g, v, 0, false);
                        break;
                    case CITY_WALL:
                        b.drawWalledCity(g, v, 0, false);
                        break;
                    case KNIGHT_DESERTER:
                    case KNIGHT_DISPLACED:
                    case KNIGHT_MOVE_POSITION:
                    case KNIGHT_TO_MOVE:
                    case OPPONENT_KNIGHT_TO_DISPLACE:
                        b.drawKnight(g, v, 0, v.getType().getKnightLevel(), v.getType().isKnightActive(), false);
                        g.setColor(GColor.YELLOW);
                        b.drawCircle(g, v);
                        g.setColor(color);
                        break;
                    case KNIGHT_TO_ACTIVATE:
                        b.drawKnight(g, v, 0, v.getType().getKnightLevel(), true, false);
                        break;
                    case KNIGHT_TO_PROMOTE:
                        b.drawKnight(g, v, 0, v.getType().getKnightLevel() + 1, v.getType().isKnightActive(), false);
                        break;
                    case NEW_KNIGHT:
                        b.drawKnight(g, v, 0, 1, false, false);
                        g.setColor(GColor.YELLOW);
                        b.drawCircle(g, v);
                        g.setColor(color);
                        break;
                    case POLITICS_METROPOLIS:
                        b.drawMetropolisPolitics(g, v, 0, false);
                        break;
                    case SCIENCE_METROPOLIS:
                        b.drawMetropolisScience(g, v, 0, false);
                        break;
                    case TRADE_METROPOLIS:
                        b.drawMetropolisTrade(g, v, 0, false);
                        break;
                    case PIRATE_FORTRESS:
                        b.drawPirateFortress(g, v, false);
                        break;
                    case OPPONENT_STRUCTURE_TO_ATTACK:
                        g.setColor(getPlayerColor(v.getPlayer()).setAlpha(120));
                        b.drawVertex(g, v, v.getType(), 0, false);
                        break;
                }
            }

            @Override
            public void onDrawOverlay(UIBoard b, APGraphics g) {
            }

            @Override
            public boolean isPickableIndex(UIBoard b, int index) {
                return vertexIndices.contains(index);
            }

            @Override
            public UIBoard.PickMode getPickMode() {
                return UIBoard.PickMode.PM_VERTEX;
            }
        });
        completeMenu();
        return waitForReturnValue(null);
    }

    public Route chooseRoute(final Collection<Integer> edges, final Player.RouteChoice choice) {
        clearMenu();
        getUIBoard().setPickHandler(new UIBoard.PickHandler() {

            @Override
            public void onPick(UIBoard b, int pickedValue) {
                b.setPickHandler(null);
                setReturnValue(getBoard().getRoute(pickedValue));
            }

            @Override
            public void onHighlighted(UIBoard b, APGraphics g, int highlightedIndex) {
                Route route = getBoard().getRoute(highlightedIndex);
                g.setColor(getPlayerColor(getCurPlayerNum()));
                switch (choice) {
                    case OPPONENT_ROAD_TO_ATTACK:
                        g.setColor(getPlayerColor(route.getPlayer()));
                        b.drawEdge(g, route, route.getType(), 0, true);
                        break;
                    case ROAD:
                    case ROUTE_DIPLOMAT:
                        b.drawRoad(g, route, true);
                        break;
                    case SHIP:
                        b.drawShip(g, route, true);
                        break;
                    case SHIP_TO_MOVE:
                        b.drawEdge(g, route, route.getType(), getCurPlayerNum(), true);
                        break;
                    case UPGRADE_SHIP:
                        b.drawWarShip(g, route, true);
                        break;
                }
            }

            @Override
            public void onDrawPickable(UIBoard b, APGraphics g, int index) {
                Route route = getBoard().getRoute(index);
                g.setColor(getPlayerColor(getCurPlayerNum()).setAlpha(120));
                switch (choice) {
                    case OPPONENT_ROAD_TO_ATTACK:
                        g.setColor(getPlayerColor(route.getPlayer()).setAlpha(120));
                        b.drawEdge(g, route, route.getType(), 0, false);
                        break;
                    case ROAD:
                    case ROUTE_DIPLOMAT:
                        b.drawRoad(g, route, false);
                        break;
                    case SHIP:
                        b.drawShip(g, route, false);
                        break;
                    case SHIP_TO_MOVE:
                        b.drawEdge(g, route, route.getType(), getCurPlayerNum(), true);
                        break;
                    case UPGRADE_SHIP:
                        b.drawWarShip(g, route, false);
                        break;
                }
            }

            @Override
            public void onDrawOverlay(UIBoard b, APGraphics g) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isPickableIndex(UIBoard b, int index) {
                return edges.contains(index);
            }

            @Override
            public UIBoard.PickMode getPickMode() {
                return UIBoard.PickMode.PM_EDGE;
            }
        });
        completeMenu();
        return waitForReturnValue(null);
    }

    public Tile chooseTile(final Collection<Integer> tiles, final Player.TileChoice choice) {
        clearMenu();
        final Tile robberTile = getBoard().getRobberTile();
        final int merchantTileIndex = getBoard().getMerchantTileIndex();
        final int merchantTilePlayer = getBoard().getMerchantPlayer();
        getBoard().setRobber(-1);
        getBoard().setMerchant(-1, 0);
        getUIBoard().setPickHandler(new UIBoard.PickHandler() {

            @Override
            public void onPick(UIBoard b, int pickedValue) {
                b.setPickHandler(null);
                b.setRobberTile(robberTile);
                b.setMerchant(merchantTileIndex, merchantTilePlayer);
                setReturnValue(getBoard().getTile(pickedValue));
            }

            @Override
            public void onHighlighted(UIBoard b, APGraphics g, int highlightedIndex) {
                Tile t = b.getTile(highlightedIndex);
                switch (choice) {
                    case INVENTOR:
                        g.setColor(GColor.YELLOW);
                        b.drawTileOutline(g, t, 4);
                        break;
                    case MERCHANT:
                        b.drawMerchant(g, t, getCurPlayerNum());
                        break;
                    case ROBBER:
                    case PIRATE:
                        if (t.isWater())
                            b.drawPirate(g, t);
                        else
                            b.drawRobber(g, t);
                        break;
                }
            }

            @Override
            public void onDrawPickable(UIBoard b, APGraphics g, int index) {
                Tile t = b.getTile(index);
                //g.setColor(Color.RED);
                //bc.drawTileOutline(g, t, 1)
                MutableVector2D v = g.transform(t);
                g.setColor(GColor.RED);
                g.drawFilledCircle(v.Xi(), v.Yi(), UIBoard.TILE_CELL_NUM_RADIUS+10);
                if (t.getDieNum() > 0)
                    b.drawCellProductionValue(g, v.Xi(), v.Yi(), t.getDieNum(), UIBoard.TILE_CELL_NUM_RADIUS);//drawTileOutline(g, cell, borderThickness);
            }

            @Override
            public void onDrawOverlay(UIBoard b, APGraphics g) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isPickableIndex(UIBoard b, int index) {
                return tiles.contains(index);
            }

            @Override
            public UIBoard.PickMode getPickMode() {
                return UIBoard.PickMode.PM_TILE;
            }
        });
        completeMenu();
        return waitForReturnValue(null);        

    }

    public abstract Trade chooseTradeMenu(Collection<Trade> trades);

    public abstract Player choosePlayerMenu(Collection<Integer> players, Player.PlayerChoice mode);

    public abstract Card chooseCardMenu(Collection<Card> cards);

    public abstract <T extends Enum<T>> T chooseEnum(List<T> ts);

    @SuppressWarnings("unchecked")
    public <T> T waitForReturnValue(T defaultValue) {
        returnValue = defaultValue;
        try {
            synchronized (waitObj) {
                waitObj.wait();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        return (T)returnValue;
    }

    private void completeMenu() {
/*
        menu.add(new JSeparator());

        if (soc.canCancel()) {
            menu.add(getMenuOpButton(MenuOp.CANCEL));
        } else {
            menu.add(new JLabel(""));
        }
        boolean aiTuningEnabled = getProps().getBooleanProperty(PROP_AI_TUNING_ENABLED, false);
        JToggleButton tuneAI = new JToggleButton("AI Tuning");
        tuneAI.setSelected(aiTuningEnabled);
        tuneAI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getProps().setProperty(PROP_AI_TUNING_ENABLED, ((JToggleButton)e.getSource()).isSelected());
            }
        });
        menu.add(tuneAI);
        menu.add(getMenuOpButton(MenuOp.SHOW_RULES));
        menu.add(getMenuOpButton(MenuOp.BUILDABLES_POPUP));
        menu.add(getMenuOpButton(MenuOp.REWIND_GAME));
        menu.add(getMenuOpButton(MenuOp.QUIT));
        helpText.setText(soc.getHelpText());
        frame.validate();*/
    }

    public boolean getSetDiceMenu(Dice[] die, int num) {
        clearMenu();
        /*
        diceComps[0].setDicePickerEnabled(true);
        diceComps[1].setDicePickerEnabled(true);
        menu.add(getMenuOpButton(MenuOp.SET_DICE));
        completeMenu();
        int [] result = (int[])waitForReturnValue(null);
        if (result != null) {
            for (int i=0; i<result.length; i++) {
                die[i].setNum(result[i]);
            }
            return true;
        }*/
        return false;
    }
}
