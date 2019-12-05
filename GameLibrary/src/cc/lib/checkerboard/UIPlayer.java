package cc.lib.checkerboard;

import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;

public class UIPlayer extends AIPlayer {

    static {
        addAllFields(UIPlayer.class);
    }

    public enum Type {
        RANDOM, USER, AI
    }

    private final Type type;

    public UIPlayer() {
        this(Type.USER);
    }

    public UIPlayer(Type type) {
        this.type = type;
    }

    @Override
    public Piece choosePieceToMove(Game game, List<Piece> pieces) {
        switch (type) {
            case RANDOM:
                return pieces.get(Utils.rand() % pieces.size());
            case USER:
                return ((UIGame)game).choosePieceToMove(pieces);
            case AI:
                return super.choosePieceToMove(game, pieces);
        }
        throw new AssertionError("Unhandled case: " +type);
    }

    @Override
    public Move chooseMoveForPiece(Game game, List<Move> moves) {
        switch (type) {
            case RANDOM:
                return moves.get(Utils.rand() % moves.size());
            case USER:
                return ((UIGame)game).chooseMoveForPiece(moves);
            case AI:
                return super.chooseMoveForPiece(game, moves);
        }
        throw new AssertionError("Unhandled case: " +type);
    }

    public void drawStatus(AGraphics g, float w, float h) {
        switch (type) {
            case AI:
                if (isThinking()) {
                    g.setColor(getColor().color);
                    g.drawJustifiedStringOnBackground(w / 2, h / 2, Justify.CENTER, Justify.CENTER, "Thinking", GColor.TRANSLUSCENT_BLACK, 3, 8);
                }
        }
    }

    public final Type getType() {
        return type;
    }
}
