package cc.game.golf.swing;

import java.util.ArrayList;
import java.util.List;

import cc.game.golf.core.Card;
import cc.game.golf.core.DrawType;
import cc.game.golf.core.Golf;
import cc.game.golf.core.Player;

public class SwingPlayerUser extends Player {

    GolfSwing g;
    
    public SwingPlayerUser() {
        super();
    }

    public SwingPlayerUser(String name) {
        super(name);
    }

    public SwingPlayerUser(String name, GolfSwing g) {
        super(name);
        this.g = g;
    }

    @Override
    protected int turnOverCard(Golf golf, int row) {
        return g.pickCard(getRow(row));
//        if (card >= 0)
            //g.addTurnOverCardAnimation(this, row, card);
//        return card;
    }

    @Override
    protected DrawType chooseDrawPile(Golf golf) {
        List<Card> options = new ArrayList<Card>();
        options.add(golf.getTopOfDeck());
        options.add(golf.getTopOfDiscardPile());
        int p = g.pickCard(options);
        if (p == 0) {
            //g.addTurnOverCardAnimationStack();
            return DrawType.DTStack;
        }
        if (p == 1)
            return DrawType.DTDiscardPile;
        return DrawType.DTWaiting;
    }

    @Override
    protected Card chooseDiscardOrPlay(Golf golf, Card drawCard) {
        Card swapped = null;
        List<Card> all = new ArrayList<Card>(this.getNumCardsDealt() + 1);
        all.addAll(getAllCards());
        all.add(drawCard);
        //g.setExtraCard(drawCard);
        int p = g.pickCard(all);
        //g.setExtraCard(null);
        if (p >= 0) {
            swapped = all.get(p);
            //onChooseCardToSwap(swapped);
        }
        return swapped;
    }

    @Override
    protected Card chooseCardToSwap(Golf golf, Card drawCard) {
        Card swapped = null;
        List<Card> all = new ArrayList<Card>(this.getNumCardsDealt());
        all.addAll(getAllCards());
        int p = g.pickCard(all);
        if (p >= 0) {
            swapped = all.get(p);
            //onChooseCardToSwap(swapped);
        }
        return swapped;
    }

    public String getMessage() {
        return "I am not a robot!";
    }
    
    void setGolfSwing(GolfSwing g) {
        this.g = g;
    }
}