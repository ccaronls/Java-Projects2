package cc.lib.swing;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

public class AWTCaret extends DefaultCaret {

        private final String mark;

        public AWTCaret() {
            this("<", 500);
        }

        public AWTCaret(String mark) {
            this(mark, 500);
        }

        public AWTCaret(String mark, int blinkRate) {
             this.mark = mark;
             if (blinkRate > 0)
                 setBlinkRate(blinkRate);
        }

        @Override
        protected synchronized void damage(Rectangle r) {
            if (r == null) {
                return;
            }

            JTextComponent comp = getComponent();
            FontMetrics fm = comp.getFontMetrics(comp.getFont());
            int textWidth = fm.stringWidth(">");
            int textHeight = fm.getHeight();
            x = r.x;
            y = r.y;
            width = textWidth;
            height = textHeight;
            repaint(); // calls getComponent().repaint(x, y, width, height)
        }

        @Override
        public void paint(Graphics g) {
            JTextComponent comp = getComponent();
            if (comp == null) {
                return;
            }

            int dot = getDot();
            Rectangle r = null;
            try {
                r = comp.modelToView(dot);
            } catch (BadLocationException e) {
                return;
            }
            if (r == null) {
                return;
            }

            if ((x != r.x) || (y != r.y)) {
                repaint(); // erase previous location of caret
                damage(r);
            }

            if (isVisible()) {
                FontMetrics fm = comp.getFontMetrics(comp.getFont());
                int textWidth = fm.stringWidth(">");
                int textHeight = fm.getHeight();

                g.setColor(comp.getCaretColor());
                g.drawString(mark, x, y + fm.getAscent());
            }
        }

    }
