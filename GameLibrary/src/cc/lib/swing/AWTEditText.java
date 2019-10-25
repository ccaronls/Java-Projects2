package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class AWTEditText extends JTextField implements KeyListener {

    public AWTEditText(String text, int maxLength) {
        super(text, maxLength);
        addKeyListener(this);
    }

    @Override
    public final void keyTyped(KeyEvent e) {
        onTextChanged(getText());
    }

    @Override
    public final void keyPressed(KeyEvent e) {

    }

    @Override
    public final void keyReleased(KeyEvent e) {
        onTextChanged(getText());
    }

    protected abstract void onTextChanged(String newText);
}
