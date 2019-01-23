package cc.lib.swing;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class AWTButton extends JButton implements ActionListener {

    public AWTButton(String label, boolean selected) {
        this(label);
        setSelected(selected);
    }

    public AWTButton(String label) {
        super(label);
        addActionListener(this);
    }

    public AWTButton(String label, ActionListener listener) {
        super(label);
        addActionListener(listener);
    }

    public void actionPerformed(ActionEvent e) {
        onAction();
    }

    protected void onAction() {
        System.err.println("Unhandled action");
    }

    public void toggleSelected() {
        setSelected(!isSelected());
    }

    public boolean isSelected() {
        return super.isSelected();
    }
}
