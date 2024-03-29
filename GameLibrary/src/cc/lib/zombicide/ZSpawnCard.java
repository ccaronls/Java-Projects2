package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

public class ZSpawnCard extends Reflector<ZSpawnCard> {

    static Logger log = LoggerFactory.getLogger(ZSpawnCard.class);

    static {
        addAllFields(ZSpawnCard.class);
        addAllFields(Action.class);
    }

    public enum ActionType {
        NOTHING_IN_SIGHT,
        SPAWN,
        DOUBLE_SPAWN,
        EXTRA_ACTIVATION_STANDARD,
        EXTRA_ACTIVATION_NECROMANCER,
        EXTRA_ACTIVATION_WOLFSBURG
    }

    public static class Action extends Reflector<Action> {

        public final ActionType action;
        public final int count;
        public final ZZombieType type;

        public Action() {
            this(null, 0, null);
        }

        public Action(ActionType action, int count, ZZombieType type) {
            this.action = action;
            this.count = count;
            this.type = type;
        }

        @Override
        public String toString() {
            if (count > 0 || type != null) {
                return String.format("%s %d X %s", action, count, type);
            }
            return action.toString();
        }
    }

    public static Action NOTHING_IN_SIGHT = new Action(ActionType.NOTHING_IN_SIGHT, 0, null);
    public static Action DOUBLE_SPAWN = new Action(ActionType.DOUBLE_SPAWN, 0, null);
    public static Action EXTRA_ACTIVATION = new Action(ActionType.EXTRA_ACTIVATION_STANDARD, 0, null);
    public static Action NECROMANCER = new Action(ActionType.SPAWN, 1, ZZombieType.Necromancer);

    private final String name;
    private final boolean wolfburg;
    private final int easyCount;
    private final int mediumCount;
    private final int hardCount;
    private final Action [] actions;

    public ZSpawnCard() {
        this(null, false, 0, 0, 0);
    }

    private ZSpawnCard(String name, boolean wolfzburg, int easyCount, int mediumCount, int hardCount, Action...actions) {
        this.name = name;
        this.wolfburg = wolfzburg;
        this.easyCount = easyCount;
        this.mediumCount = mediumCount;
        this.hardCount = hardCount;
        this.actions = actions;
    }

    public final static ZSpawnCard [] cards = {
            new ZSpawnCard("Standard Zombie Invasion", false, 6, 4, 2,
                    NOTHING_IN_SIGHT,
                    new Action(ActionType.SPAWN, 2, ZZombieType.Runner),
                    new Action(ActionType.SPAWN, 5, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Fatty)),
            new ZSpawnCard("Standard Zombie Invasion", false, 10, 5, 2,
                    new Action(ActionType.SPAWN, 2, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 3, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 4, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 5, ZZombieType.Walker)),
            new ZSpawnCard("Standard Zombie Invasion", false, 0, 0, 1,
                    new Action(ActionType.SPAWN, 1, ZZombieType.Abomination),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Runner),
                    new Action(ActionType.SPAWN, 5, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Fatty)),
            new ZSpawnCard("Standard Zombie Invasion", false, 8, 6, 4,
                    new Action(ActionType.SPAWN, 1, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Fatty),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Runner),
                    new Action(ActionType.SPAWN, 8, ZZombieType.Walker)),
            new ZSpawnCard("Standard Zombie Invasion", false, 1, 2, 3,
                    new Action(ActionType.SPAWN, 2, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Abomination),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Walker)),
            new ZSpawnCard("Standard Zombie Invasion", false, 4, 4, 4,
                    new Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Runner),
                    new Action(ActionType.SPAWN, 3, ZZombieType.Runner)),
            new ZSpawnCard("Standard Zombie Invasion", false, 2, 4, 6,
                    new Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Runner),
                    new Action(ActionType.SPAWN, 6, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 2, ZZombieType.Runner)),
            new ZSpawnCard("Standard Zombie Invasion", false, 4, 4, 4,
                    new Action(ActionType.SPAWN, 1, ZZombieType.Runner),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
                    new Action(ActionType.SPAWN, 4, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 5, ZZombieType.Walker)),
            new ZSpawnCard("Standard Zombie Invasion", false, 8, 6, 4,
                    new Action(ActionType.SPAWN, 3, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Fatty),
                    new Action(ActionType.SPAWN, 4, ZZombieType.Walker),
                    new Action(ActionType.SPAWN, 6, ZZombieType.Walker)),

            new ZSpawnCard("Necromancer!", false, 1, 2, 4,
                    NECROMANCER, NECROMANCER, NECROMANCER, NECROMANCER),

            new ZSpawnCard("Double Spawn!", false, 0, 1, 2,
                    DOUBLE_SPAWN, DOUBLE_SPAWN, DOUBLE_SPAWN, DOUBLE_SPAWN),

            new ZSpawnCard("Extra Activation!", false, 0, 3, 6,
                    NOTHING_IN_SIGHT, EXTRA_ACTIVATION, EXTRA_ACTIVATION, EXTRA_ACTIVATION),

            new ZSpawnCard("Zombie Wolfz Invasion", true, 1, 2, 3,
                    NOTHING_IN_SIGHT,
                    new Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination),
                    new Action(ActionType.SPAWN, 1, ZZombieType.Wolfbomination)),
            new ZSpawnCard("Zombie Wolfz Invasion", true, 4, 6, 10,
                    new Action(ActionType.SPAWN, 2, ZZombieType.Wolfz),
                    new Action(ActionType.SPAWN, 3, ZZombieType.Wolfz),
                    new Action(ActionType.SPAWN, 4, ZZombieType.Wolfz),
                    new Action(ActionType.SPAWN, 5, ZZombieType.Wolfz)),
    };

    public static ZSpawnCard drawSpawnCard(boolean wolfburg, boolean canSpawnNecromancers, ZDifficulty difficulty) {
        int [] weights = new int[cards.length];

        for (int i=0; i<cards.length; i++) {
            ZSpawnCard card = cards[i];
            if (!wolfburg && card.wolfburg)
                continue;
            if (cards[i].actions[0] == NECROMANCER && !canSpawnNecromancers)
                continue; // dont spawn necromancers in spawn zones, only in buildings
            switch (difficulty) {
                case EASY:
                    weights[i] = card.easyCount;
                    break;
                case MEDIUM:
                    weights[i] = card.mediumCount;
                    break;
                case HARD:
                    weights[i] = card.hardCount;
                    break;
            }
        }

        for (int i=0; i<weights.length; i++) {
            if (weights[i] == 0)
                continue;
            log.debug("%2d %s", weights[i], Arrays.toString(cards[i].actions));
        }

        int cardIdx = Utils.chooseRandomFromSet(weights);
        return cards[cardIdx];
    }

    public String getName() {
        return name;
    }

    public Action getAction(ZColor color) {
        return actions[color.ordinal()];
    }

    @Override
    public String toString() {
        return "ZSpawnCard{" +
                "name='" + name + '\'' +
                ", wolfzburg=" + wolfburg +
                ", easyCount=" + easyCount +
                ", mediumCount=" + mediumCount +
                ", hardCount=" + hardCount +
                ", actions=" + Arrays.toString(actions) +
                '}';
    }
}
