package cc.lib.zombicide;

public class ZZombie extends ZActor {

    static {
        addAllFields(ZZombie.class);
    }

    @Override
    protected int getActionsPerTurn() {
        return type.actionsPerTurn;
    }

    public ZZombie() {
        this(null, -1);
    }

    ZZombie(ZZombieType type, int zone) {
        super(zone);
        this.type = type;
    }

    final ZZombieType type;

    @Override
    protected int getImageId() {
        return type.imageId;
    }

    @Override
    public String name() {
        return type.name();
    }
}

