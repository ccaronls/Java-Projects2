package cc.lib.swing;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public abstract class AWTToggleButton extends JToggleButton implements ActionListener {

    private boolean ignore = false;

    public AWTToggleButton(String text) {
        super(text);
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!ignore) {
            onToggle(isSelected());
        }
    }

    @Override
    public void setSelected(boolean selected) {
        ignore = true;
        super.setSelected(selected);
        ignore = false;
    }

    protected abstract void onToggle(boolean on);
}
