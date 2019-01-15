package cc.lib.monopoly;

import java.util.List;

public class PlayerUser extends Player {

    @Override
    public MoveType chooseMove(Monopoly game, List<MoveType> options) {
        return ((UIMonopoly)game).showChooseMoveMenu(this, options);
    }

    @Override
    public Card chooseCard(Monopoly game, List<Card> cards) {
        return ((UIMonopoly)game).showChooseCardMenu(this, cards);
    }
}
