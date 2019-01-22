package cc.lib.swing;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public abstract class AWTButton extends JButton implements ActionListener {

    public AWTButton(String label) {
        super(label);
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        onAction();
    }

    protected abstract void onAction();

    public void toggleSelected() {
        setSelected(!isSelected());
    }
}
