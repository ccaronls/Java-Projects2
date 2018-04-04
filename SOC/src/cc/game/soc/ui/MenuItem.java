package cc.game.soc.ui;

/**
 * Created by chriscaron on 2/27/18.
 */

public final class MenuItem {

    public interface Action {
        void onAction(MenuItem item, Object extra);
    };

    public final String title;
    public final String helpText;
    public final Action action;

    public MenuItem(String title, String helpText, Action action) {
        this.title = title;
        this.helpText = helpText;
        this.action = action;
    }

    @Override
    public final String toString() {
        return title;
    }
}
