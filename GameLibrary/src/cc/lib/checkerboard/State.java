package cc.lib.checkerboard;

import java.util.List;

import cc.lib.utils.Reflector;

public final class State extends Reflector<State> {

    static {
        addAllFields(State.class);
    }

    final int index;
    final List<Move> moves;

    public State() {
        this(-1, null);
    }

    public State(int index, List<Move> moves) {
        if (moves != null)
            if (index < 0 || index >= moves.size())
                throw new AssertionError();
        this.index = index;
        this.moves = moves;
    }

    Move getMove() {
        return moves.get(index);
    }
}

