package cc.lib.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.IVector2D;
import cc.lib.game.Renderable;
import cc.lib.game.Utils;


/**
 * Created by chriscaron on 2/21/18.
 */

public abstract class AWTComponent extends JComponent implements Renderable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {


    enum VKKey {
        VK_UNKNOWN(-1),
        VK_0(KeyEvent.VK_0),
        VK_1(KeyEvent.VK_1),
        VK_2(KeyEvent.VK_2),
        VK_3(KeyEvent.VK_3),
        VK_4(KeyEvent.VK_4),
        VK_5(KeyEvent.VK_5),
        VK_6(KeyEvent.VK_6),
        VK_7(KeyEvent.VK_7),
        VK_8(KeyEvent.VK_8),
        VK_9(KeyEvent.VK_9),
        VK_A(KeyEvent.VK_A),
        VK_B(KeyEvent.VK_B),
        VK_C(KeyEvent.VK_C),
        VK_D(KeyEvent.VK_D),
        VK_E(KeyEvent.VK_E),
        VK_F(KeyEvent.VK_F),
        VK_G(KeyEvent.VK_G),
        VK_H(KeyEvent.VK_H),
        VK_I(KeyEvent.VK_I),
        VK_J(KeyEvent.VK_J),
        VK_K(KeyEvent.VK_K),
        VK_L(KeyEvent.VK_L),
        VK_M(KeyEvent.VK_M),
        VK_N(KeyEvent.VK_N),
        VK_O(KeyEvent.VK_O),
        VK_P(KeyEvent.VK_P),
        VK_Q(KeyEvent.VK_Q),
        VK_R(KeyEvent.VK_R),
        VK_S(KeyEvent.VK_S),
        VK_T(KeyEvent.VK_T),
        VK_U(KeyEvent.VK_U),
        VK_V(KeyEvent.VK_V),
        VK_W(KeyEvent.VK_W),
        VK_X(KeyEvent.VK_X),
        VK_Y(KeyEvent.VK_Y),
        VK_Z(KeyEvent.VK_Z),
        VK_ACCEPT(KeyEvent.VK_ACCEPT),
        VK_ADD(KeyEvent.VK_ADD),
        VK_AGAIN(KeyEvent.VK_AGAIN),
        VK_ALL_CANDIDATES(KeyEvent.VK_ALL_CANDIDATES),
        VK_ALPHANUMERIC(KeyEvent.VK_ALPHANUMERIC),
        VK_ALT(KeyEvent.VK_ALT),
        VK_ALT_GRAPH(KeyEvent.VK_ALT_GRAPH),
        VK_AMPERSAND(KeyEvent.VK_AMPERSAND),
        VK_ASTERISK(KeyEvent.VK_ASTERISK),
        VK_AT(KeyEvent.VK_AT),
        VK_BACK_QUOTE(KeyEvent.VK_BACK_QUOTE),
        VK_BACK_SLASH(KeyEvent.VK_BACK_SLASH),
        VK_BACK_SPACE(KeyEvent.VK_BACK_SPACE),
        VK_BEGIN(KeyEvent.VK_BEGIN),
        VK_BRACELEFT(KeyEvent.VK_BRACELEFT),
        VK_BRACERIGHT(KeyEvent.VK_BRACERIGHT),
        VK_CANCEL(KeyEvent.VK_CANCEL),
        VK_CAPS_LOCK(KeyEvent.VK_CAPS_LOCK),
        VK_CIRCUMFLEX(KeyEvent.VK_CIRCUMFLEX),
        VK_CLEAR(KeyEvent.VK_CLEAR),
        VK_CLOSE_BRACKET(KeyEvent.VK_CLOSE_BRACKET),
        VK_CODE_INPUT(KeyEvent.VK_CODE_INPUT),
        VK_COLON(KeyEvent.VK_COLON),
        VK_COMMA(KeyEvent.VK_COMMA),
        VK_COMPOSE(KeyEvent.VK_COMPOSE),
        VK_CONTEXT_MENU(KeyEvent.VK_CONTEXT_MENU),
        VK_CONTROL(KeyEvent.VK_CONTROL),
        VK_CONVERT(KeyEvent.VK_CONVERT),
        VK_COPY(KeyEvent.VK_COPY),
        VK_CUT(KeyEvent.VK_CUT),
        VK_DEAD_ABOVEDOT(KeyEvent.VK_DEAD_ABOVEDOT),
        VK_DEAD_ABOVERING(KeyEvent.VK_DEAD_ABOVERING),
        VK_DEAD_ACUTE(KeyEvent.VK_DEAD_ACUTE),
        VK_DEAD_BREVE(KeyEvent.VK_DEAD_BREVE),
        VK_DEAD_CARON(KeyEvent.VK_DEAD_CARON),
        VK_DEAD_CEDILLA(KeyEvent.VK_DEAD_CEDILLA),
        VK_DEAD_CIRCUMFLEX(KeyEvent.VK_DEAD_CIRCUMFLEX),
        VK_DEAD_DIAERESIS(KeyEvent.VK_DEAD_DIAERESIS),
        VK_DEAD_DOUBLEACUTE(KeyEvent.VK_DEAD_DOUBLEACUTE),
        VK_DEAD_GRAVE(KeyEvent.VK_DEAD_GRAVE),
        VK_DEAD_IOTA(KeyEvent.VK_DEAD_IOTA),
        VK_DEAD_MACRON(KeyEvent.VK_DEAD_MACRON),
        VK_DEAD_OGONEK(KeyEvent.VK_DEAD_OGONEK),
        VK_DEAD_SEMIVOICED_SOUND(KeyEvent.VK_DEAD_SEMIVOICED_SOUND),
        VK_DEAD_TILDE(KeyEvent.VK_DEAD_TILDE),
        VK_DEAD_VOICED_SOUND(KeyEvent.VK_DEAD_VOICED_SOUND),
        VK_DECIMAL(KeyEvent.VK_DECIMAL),
        VK_DELETE(KeyEvent.VK_DELETE),
        VK_DIVIDE(KeyEvent.VK_DIVIDE),
        VK_DOLLAR(KeyEvent.VK_DOLLAR),
        VK_DOWN(KeyEvent.VK_DOWN),
        VK_END(KeyEvent.VK_END),
        VK_ENTER(KeyEvent.VK_ENTER),
        VK_EQUALS(KeyEvent.VK_EQUALS),
        VK_ESCAPE(KeyEvent.VK_ESCAPE),
        VK_EURO_SIGN(KeyEvent.VK_EURO_SIGN),
        VK_EXCLAMATION_MARK(KeyEvent.VK_EXCLAMATION_MARK),
        VK_F1(KeyEvent.VK_F1),
        VK_F10(KeyEvent.VK_F10),
        VK_F11(KeyEvent.VK_F11),
        VK_F12(KeyEvent.VK_F12),
        VK_F13(KeyEvent.VK_F13),
        VK_F14(KeyEvent.VK_F14),
        VK_F15(KeyEvent.VK_F15),
        VK_F16(KeyEvent.VK_F16),
        VK_F17(KeyEvent.VK_F17),
        VK_F18(KeyEvent.VK_F18),
        VK_F19(KeyEvent.VK_F19),
        VK_F2(KeyEvent.VK_F2),
        VK_F20(KeyEvent.VK_F20),
        VK_F21(KeyEvent.VK_F21),
        VK_F22(KeyEvent.VK_F22),
        VK_F23(KeyEvent.VK_F23),
        VK_F24(KeyEvent.VK_F24),
        VK_F3(KeyEvent.VK_F3),
        VK_F4(KeyEvent.VK_F4),
        VK_F5(KeyEvent.VK_F5),
        VK_F6(KeyEvent.VK_F6),
        VK_F7(KeyEvent.VK_F7),
        VK_F8(KeyEvent.VK_F8),
        VK_F9(KeyEvent.VK_F9),
        VK_FINAL(KeyEvent.VK_FINAL),
        VK_FIND(KeyEvent.VK_FIND),
        VK_FULL_WIDTH(KeyEvent.VK_FULL_WIDTH),
        VK_GREATER(KeyEvent.VK_GREATER),
        VK_HALF_WIDTH(KeyEvent.VK_HALF_WIDTH),
        VK_HELP(KeyEvent.VK_HELP),
        VK_HIRAGANA(KeyEvent.VK_HIRAGANA),
        VK_HOME(KeyEvent.VK_HOME),
        VK_INPUT_METHOD_ON_OFF(KeyEvent.VK_INPUT_METHOD_ON_OFF),
        VK_INSERT(KeyEvent.VK_INSERT),
        VK_INVERTED_EXCLAMATION_MARK(KeyEvent.VK_INVERTED_EXCLAMATION_MARK),
        VK_JAPANESE_HIRAGANA(KeyEvent.VK_JAPANESE_HIRAGANA),
        VK_JAPANESE_KATAKANA(KeyEvent.VK_JAPANESE_KATAKANA),
        VK_JAPANESE_ROMAN(KeyEvent.VK_JAPANESE_ROMAN),
        VK_KANA(KeyEvent.VK_KANA),
        VK_KANA_LOCK(KeyEvent.VK_KANA_LOCK),
        VK_KANJI(KeyEvent.VK_KANJI),
        VK_KATAKANA(KeyEvent.VK_KATAKANA),
        VK_KP_DOWN(KeyEvent.VK_KP_DOWN),
        VK_KP_LEFT(KeyEvent.VK_KP_LEFT),
        VK_KP_RIGHT(KeyEvent.VK_KP_RIGHT),
        VK_KP_UP(KeyEvent.VK_KP_UP),
        VK_LEFT(KeyEvent.VK_LEFT),
        VK_LEFT_PARENTHESIS(KeyEvent.VK_LEFT_PARENTHESIS),
        VK_LESS(KeyEvent.VK_LESS),
        VK_META(KeyEvent.VK_META),
        VK_MINUS(KeyEvent.VK_MINUS),
        VK_MODECHANGE(KeyEvent.VK_MODECHANGE),
        VK_MULTIPLY(KeyEvent.VK_MULTIPLY),
        VK_NONCONVERT(KeyEvent.VK_NONCONVERT),
        VK_NUM_LOCK(KeyEvent.VK_NUM_LOCK),
        VK_NUMBER_SIGN(KeyEvent.VK_NUMBER_SIGN),
        VK_NUMPAD0(KeyEvent.VK_NUMPAD0),
        VK_NUMPAD1(KeyEvent.VK_NUMPAD1),
        VK_NUMPAD2(KeyEvent.VK_NUMPAD2),
        VK_NUMPAD3(KeyEvent.VK_NUMPAD3),
        VK_NUMPAD4(KeyEvent.VK_NUMPAD4),
        VK_NUMPAD5(KeyEvent.VK_NUMPAD5),
        VK_NUMPAD6(KeyEvent.VK_NUMPAD6),
        VK_NUMPAD7(KeyEvent.VK_NUMPAD7),
        VK_NUMPAD8(KeyEvent.VK_NUMPAD8),
        VK_NUMPAD9(KeyEvent.VK_NUMPAD9),
        VK_OPEN_BRACKET(KeyEvent.VK_OPEN_BRACKET),
        VK_PAGE_DOWN(KeyEvent.VK_PAGE_DOWN),
        VK_PAGE_UP(KeyEvent.VK_PAGE_UP),
        VK_PASTE(KeyEvent.VK_PASTE),
        VK_PAUSE(KeyEvent.VK_PAUSE),
        VK_PERIOD(KeyEvent.VK_PERIOD),
        VK_PLUS(KeyEvent.VK_PLUS),
        VK_PREVIOUS_CANDIDATE(KeyEvent.VK_PREVIOUS_CANDIDATE),
        VK_PRINTSCREEN(KeyEvent.VK_PRINTSCREEN),
        VK_PROPS(KeyEvent.VK_PROPS),
        VK_QUOTE(KeyEvent.VK_QUOTE),
        VK_QUOTEDBL(KeyEvent.VK_QUOTEDBL),
        VK_RIGHT(KeyEvent.VK_RIGHT),
        VK_RIGHT_PARENTHESIS(KeyEvent.VK_RIGHT_PARENTHESIS),
        VK_ROMAN_CHARACTERS(KeyEvent.VK_ROMAN_CHARACTERS),
        VK_SCROLL_LOCK(KeyEvent.VK_SCROLL_LOCK),
        VK_SEMICOLON(KeyEvent.VK_SEMICOLON),
        VK_SEPARATER(KeyEvent.VK_SEPARATER),
        VK_SEPARATOR(KeyEvent.VK_SEPARATOR),
        VK_SHIFT(KeyEvent.VK_SHIFT),
        VK_SLASH(KeyEvent.VK_SLASH),
        VK_SPACE(KeyEvent.VK_SPACE),
        VK_STOP(KeyEvent.VK_STOP),
        VK_SUBTRACT(KeyEvent.VK_SUBTRACT),
        VK_TAB(KeyEvent.VK_TAB),
        VK_UNDEFINED(KeyEvent.VK_UNDEFINED),
        VK_UNDERSCORE(KeyEvent.VK_UNDERSCORE),
        VK_UNDO(KeyEvent.VK_UNDO),
        VK_UP(KeyEvent.VK_UP),
        VK_WINDOWS(KeyEvent.VK_WINDOWS),
        ;

        VKKey(int code) {
            this.code = code;
        }

        static VKKey lookup(int code) {
            for (VKKey k : values()) {
                if (code == k.code)
                    return k;
            }
            return VK_UNKNOWN;
        }

        private final int code;
        private static VKKey [] lookup;
    }


    private AWTGraphics G = null;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean focused = false;
    private int padding = 5;
    private int scrollAmount = -1;
    private int scrollStartY = 0;

    public AWTComponent() {
    }

    public void setMouseEnabled(boolean enabled) {
        if (enabled) {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            setFocusable(true);
            addKeyListener(this);
        } else {
            removeMouseListener(this);
            removeMouseMotionListener(this);
            removeMouseWheelListener(this);
            setFocusable(false);
            removeKeyListener(this);
            mouseX = mouseY = -1;
        }
    }

    @Override
    public final synchronized void paint(Graphics g) {
        try {
            if (getWidth() > 0 && getHeight() > 0) {
                Color c = g.getColor();
                g.setColor(getBackground());//AWTUtils.toColor(GColor.TRANSPARENT));
                g.fillRect(0, 0, super.getWidth(), super.getHeight());
                g.setColor(c);
                //g.setClip(padding,padding, getWidth()+1, getHeight()+1);
                if (G == null) {
                    if (g instanceof Graphics2D)
                        G = new AWTGraphics2(((Graphics2D) g), this);
                    else
                        G = new AWTGraphics(g, this);
                    init(G);
                    repaint();
                } else {
                    float progress = getInitProgress();
                    G.setGraphics(g);
                    G.initViewport(getWidth(), getHeight());
                    G.ortho();
                    if (progress >= 1) {
                        int matStack = G.getMatrixStackSize();
                        if (scrollAmount < 0)
                            scrollAmount = G.getTextHeight();
                        if (scrollStartY != 0) {
                            G.pushMatrix();
                            G.translate(0, scrollStartY);
                            paint(G, mouseX, mouseY);
                            G.popMatrix();
                        } else {
                            try {
                                paint(G, mouseX, mouseY);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (G.getMatrixStackSize() != matStack) {
                            throw new AssertionError("Matrix stack not zero");
                        }
                    } else {
                        Font f = g.getFont();
                        G.clearScreen(GColor.CYAN);
                        G.setColor(GColor.WHITE);
                        G.setTextHeight(getHeight() / 10);
                        float x = getWidth() / 2;
                        float y = getHeight() / 3;
                        String txt = "INITIALIZING";
                        float tw = G.getTextWidth(txt);
                        float th = G.getTextHeight();
                        while (tw > getWidth() && G.getTextHeight() > 8) {
                            G.setTextHeight(G.getTextHeight() - 2);
                        }
                        G.drawJustifiedString(x - tw / 2, y, txt);
                        y += th;
                        G.drawFilledRect(x - tw / 2, y, tw * progress, th);
                        g.setFont(f);
                        repaint();
                    }
                }
                //g.setClip(0, 0, super.getWidth(), super.getHeight());
                if (focused) {
                    //                System.out.println("AWT " + toString() + " has focus!");
                    g.setColor(Color.BLUE);
                    g.drawRect(0, 0, super.getWidth() - 2, super.getHeight() - 2);
                }
            } else {
                repaint();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void paint(AWTGraphics g, int mouseX, int mouseY);

    protected void init(AWTGraphics g) {
    }

    /**
     * Return value between 0-1 that is the progress of init flow
     * @return
     */
    protected float getInitProgress() {
        return 1;
    }

    @Override
    public final synchronized void mouseClicked(MouseEvent e) {
        //Utils.println("mouseClicked");
        onClick();
    }

    @Override
    public final synchronized void mousePressed(MouseEvent e) {
        //Utils.println("mousePressed");
        grabFocus();
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        repaint();
    }

    @Override
    public final synchronized void mouseReleased(MouseEvent e) {
        //Utils.println("mouseReleased");
        if (dragging) {
            onDragStopped();
            dragging = false;
        }
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        repaint();
    }

    @Override
    public final synchronized void mouseEntered(MouseEvent e) {
//        grabFocus();
        focused = true;
        repaint();
    }

    @Override
    public final synchronized void mouseExited(MouseEvent e) {
        focused = false;
        repaint();
    }

    boolean dragging = false;

    @Override
    public final synchronized void mouseWheelMoved(MouseWheelEvent e) {
        onMouseWheel(e.getWheelRotation());
    }

    protected void onMouseWheel(int rotation) {
        Dimension d = getMinimumSize();
        int maxScroll = getHeight() - d.height;
        if (maxScroll < 0) {
            scrollStartY = Utils.clamp(scrollStartY - rotation * scrollAmount, maxScroll, 0);
            repaint();
        }
    }

    @Override
    public synchronized boolean lostFocus(java.awt.Event ev, Object obj) {
        repaint();
        return super.lostFocus(ev, obj);
    }

    @Override
    public final synchronized void mouseDragged(MouseEvent e) {
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        if (!dragging) {
            onDragStarted(mouseX, mouseY);
            dragging = true;
        }
        //Utils.println("mouseDragged");
        repaint();
    }

    @Override
    public final synchronized void mouseMoved(MouseEvent e) {
        //Utils.println("mouseMoved");
        mouseX = e.getX()-padding;
        mouseY = e.getY()-padding;
        repaint();
    }

    @Override
    public final synchronized void keyTyped(KeyEvent evt) {
        onKeyTyped(VKKey.lookup(evt.getKeyCode()));
    }

    @Override
    public final synchronized void keyPressed(KeyEvent evt) {
        onKeyPressed(VKKey.lookup(evt.getKeyCode()));
    }

    @Override
    public final synchronized void keyReleased(KeyEvent evt) {
        onKeyReleased(VKKey.lookup(evt.getKeyCode()));
    }

    protected void onKeyTyped(VKKey key) {
        System.out.println("Key Typed'" + key + "'");
    }

    protected void onKeyPressed(VKKey key) { System.out.println("Key Pressed'" + key + "'"); }

    protected void onKeyReleased(VKKey key) { System.out.println("Key Released'" + key + "'"); }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    protected void onDragStarted(int x, int y) {}

    protected void onDragStopped() {}

    protected void onClick() {}

    public final void repaint() {
        super.repaint();
    }

    public final int getX() {
        return super.getX() + padding;
    }

    public final int getY() {
        return super.getY() + padding;
    }

    public final int getWidth() {
        return super.getWidth()-padding*2;
    }

    public final int getHeight() {
        return super.getHeight()-padding*2;
    }

    @Override
    public int getViewportWidth() {
        return getWidth();
    }

    @Override
    public int getViewportHeight() {
        return getHeight();
    }

    public void setMinimumSize(int w, int h) {
        super.setMinimumSize(new Dimension(w, h));
    }

    public void setMinimumSize(GDimension dim) {
        setMinimumSize(Math.round(dim.width), Math.round(dim.height));
    }

    public void setPreferredSize(int w, int h) {
        super.setPreferredSize(new Dimension(w, h));
    }

    @Override
    public Dimension getMinimumSize() {
        return super.getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return super.getMaximumSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return super.getPreferredSize();
    }

    public final APGraphics getAPGraphics() {
        return G;
    }

    public final void setPadding(int padding) {
        this.padding = padding;
    }

    public final void setBackground(GColor color) {
        super.setBackground(AWTUtils.toColor(color));
    }

    /**
     * Get the position in viewport coords
     * @return
     */
    protected IVector2D getMousePos() {
        return getMousePos(getMouseX(), getMouseY());
    }

    protected IVector2D getMousePos(int mx, int my) {
        return getAPGraphics().screenToViewport(mx, my);
    }

}
