package cc.lib.monopoly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerUser extends Player {

    static {
        addAllFields(PlayerUser.class);
    }

    private Map<Card, Integer> sellableCards = new HashMap<>();

    @Override
    public MoveType chooseMove(Monopoly game, List<MoveType> options) {
        return ((UIMonopoly)game).showChooseMoveMenu(this, options);
    }

    @Override
    public Card chooseCard(Monopoly game, List<Card> cards, CardChoiceType type) {
        return ((UIMonopoly)game).showChooseCardMenu(this, cards, type);
    }

    @Override
    public Trade chooseTrade(Monopoly game, List<Trade> trades) {
        return ((UIMonopoly)game).showChooseTradeMenu(this, trades);
    }

    @Override
    public boolean markCardsForSale(Monopoly game, List<Card> sellable) {
        return ((UIMonopoly)game).showMarkSellableMenu(this, sellable);
    }

    @Override
    public List<Trade> getTrades() {
        List<Trade> trades = new ArrayList<>();
        for (Map.Entry<Card, Integer> e : sellableCards.entrySet()) {
            trades.add(new Trade(e.getKey(), e.getValue(), this));
        }
        return trades;
    }

    public void setSellableCard(Card card, int amount) {
        if (amount <= 0)
            sellableCards.remove(card);
        else
            sellableCards.put(card, amount);
    }

    public int getSellableCardCost(Card  card) {
        Integer amt = sellableCards.get(card);
        if (amt != null)
            return amt;
        return 0;
    }

    @Override
    protected void removeCard(Card card) {
        super.removeCard(card);
        sellableCards.remove(card);
    }
}
