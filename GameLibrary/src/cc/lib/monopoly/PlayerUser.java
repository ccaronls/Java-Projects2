package cc.lib.monopoly;

import java.util.List;

public class PlayerUser extends Player {

    @Override
    public MoveType chooseMove(Monopoly game, List<MoveType> options) {
        return super.chooseMove((UIMonopoly)game, options);
    }

    @Override
    public Card chooseCard(Monopoly game, List<Card> cards) {
        return super.chooseCard(game, cards);
    }
}
