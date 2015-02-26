package cc.fantasy.swing;

import javax.swing.SwingConstants;

public enum Justify {

    TOP(SwingConstants.TOP),
    LEFT(SwingConstants.LEFT),
    RIGHT(SwingConstants.RIGHT),
    BOTTOM(SwingConstants.BOTTOM),
    CENTER(SwingConstants.CENTER);
    
    private final int justify;
    
    private Justify(int j) {
        justify = j;
    }
    
    int getJustify() {
        return justify;
    }
    
}
