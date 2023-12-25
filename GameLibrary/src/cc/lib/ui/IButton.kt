package cc.lib.ui;

/**
 * AWTButton and other buttons can look for these types to add tooltip 'mouse hover' text to buttons
 *
 *
 */
public interface IButton {

    String getTooltipText();

    String getLabel();

    default int getZOrder() { return 0; }

    default boolean isEnabled() { return true; }
}
