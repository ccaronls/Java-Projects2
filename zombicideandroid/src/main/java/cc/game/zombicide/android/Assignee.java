package cc.game.zombicide.android;

import cc.lib.game.GColor;
import cc.lib.utils.Reflector;
import cc.lib.zombicide.ZPlayerName;

public class Assignee extends Reflector<Assignee> implements Comparable<Assignee> {
    static {
        addAllFields(Assignee.class);
    }
    final ZPlayerName name;
    String userName = "";
    GColor color;
    boolean checked;
    @Omit
    boolean isAssingedToMe;

    @Omit
    final ZombicideActivity.CharLock lock;

    public Assignee() {
        name = null;
        lock = null;
    }

    public Assignee(ZPlayerName name, String userName, GColor color, boolean checked) {
        this.name = name;
        this.userName = userName;
        this.color = color;
        this.checked = checked;
        lock = null;
    }

    Assignee(ZombicideActivity.CharLock cl) {
        name = cl.player;
        lock = cl;
    }

    @Override
    public int compareTo(Assignee o) {
        return name.compareTo(o.name);
    }

    boolean isUnlocked() {
        return (color == null || isAssingedToMe) && lock.isUnlocked();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignee assignee = (Assignee) o;
        return name == assignee.name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}