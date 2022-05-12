package cc.lib.monopoly;

public class Trade {
    private final Card card;
    private final int price;
    private final Player trader;

    public Trade(Card card, int price, Player trader) {
        this.card = card;
        this.price = price;
        this.trader = trader;
    }

    public Card getCard() {
        return card;
    }

    public int getPrice() {
        return price;
    }

    public Player getTrader() {
        return trader;
    }

    public String toString() {
        return card.property.name() + " $" + price;
    }
}
