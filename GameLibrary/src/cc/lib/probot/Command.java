package cc.lib.probot;

public class Command {
        public final CommandType type;
        public int count;
        public int nesting=0;

        public Command(CommandType type, int count) {
            this.type = type;
            this.count = count;
        }
    }
