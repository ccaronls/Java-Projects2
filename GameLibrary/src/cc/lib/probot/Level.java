package cc.lib.probot;

import cc.lib.utils.Reflector;

public class Level extends Reflector<Level> {
        public String label = "<UNNAMED>";
        public String info = "<EMPTY>";
        public Type [][] coins = { { Type.EM } };
        public Boolean [] lazers = { true, true, true };
        public int numJumps = 0;
        public int numTurns = -1;
        public int numLoops = -1;
    }
