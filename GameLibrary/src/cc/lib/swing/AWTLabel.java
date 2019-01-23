package cc.lib.swing;

import javax.swing.*;
import java.awt.*;

import cc.lib.game.Utils;

public class AWTLabel extends JLabel {

    /**
     *
     * @param text
     * @param justify 0 == left, 1 == center, 2 == right
     * @param size
     * @param bold
     */
    public AWTLabel(String text, int justify, float size, boolean bold) {
        super(text);
        switch (justify) {
            case 0:
                setHorizontalAlignment(SwingConstants.LEFT);
                break;
            case 1:
                setHorizontalAlignment(SwingConstants.CENTER);
                break;
            case 2:
                setHorizontalAlignment(SwingConstants.RIGHT);
                break;
            default:
                Utils.unhandledCase(justify);
        }
        Font font = getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size);
        setFont(font);
    }

}
