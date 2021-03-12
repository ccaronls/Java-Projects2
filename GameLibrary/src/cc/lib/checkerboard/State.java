package cc.lib.checkerboard;

import java.util.List;

import cc.lib.utils.Reflector;

public final class State extends Reflector<State> {

    static {
        addAllFields(State.class);
    }

    final int index;
    final List<Move> moves;
    List<int []> enpassants;

    public State() {
        this(-1, null);
    }

    public State(int index, List<Move> moves) {
        if (moves != null)
            if (index < 0 || index >= moves.size())
                throw new cc.lib.utils.GException("Logic Error: index not in range [0-" + moves.size() + ")");
        this.index = index;
        this.moves = moves;
    }

    @Override
    public String toString() {
        return "State{" + getMove() + "}";
    }

    Move getMove() {
        return moves.get(index);
    }
}

