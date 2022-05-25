package cc.applets.kaiser;

import cc.game.kaiser.core.Bid;
import cc.game.kaiser.core.Card;
import cc.game.kaiser.core.Kaiser;

public class SwingPlayerUser extends SwingPlayer { 

    KaiserApplet applet;
    
    public SwingPlayerUser() {
        applet = null;
    }
    
    protected SwingPlayerUser(String name, KaiserApplet applet) {
        super(name);
        this.applet = applet;
    }

    @Override
    public boolean isCardsShowing() {
        return true;
    }
    
    @Override
    public void onProcessRound(Kaiser k) {
        synchronized (applet) {
            try {
                applet.wait();
            } catch (Exception e) {}
        }
    }

    @Override
    public Card playTrick(Kaiser kaiser, Card[] options) {
        return applet.pickCard(options);
    }

    @Override
    public Bid makeBid(Kaiser kaiser, Bid[] options) {
        return applet.pickBid(options);
    }
    
}
