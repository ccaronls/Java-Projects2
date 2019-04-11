package cc.lib.swing;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class AWTToggleButton extends JCheckBox implements ActionListener {

    private boolean ignore = false;

    public AWTToggleButton(String text) {
        this(text, false);
    }

    public AWTToggleButton(String text, boolean selected) {
        super(text);
        setSelected(selected);
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!ignore) {
            updateUI();
            onToggle(isSelected());
        }
    }

    @Override
    public void setSelected(boolean selected) {
        ignore = true;
        super.setSelected(selected);
        ignore = false;
    }

    protected void onToggle(boolean on) {}
}
