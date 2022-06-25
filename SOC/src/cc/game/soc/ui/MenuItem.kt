package cc.game.soc.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by chriscaron on 2/27/18.
 */

public final class MenuItem {

    public interface Action {
        void onAction(@NotNull MenuItem item, @Nullable Object extra);
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
