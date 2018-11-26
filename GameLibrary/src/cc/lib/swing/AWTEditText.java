package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class AWTEditText extends JTextField implements ActionListener {

    public AWTEditText(String text, int maxLength) {
        super(text, maxLength);
        addActionListener(this);
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
        onTextChanged(getText());
    }

    protected abstract void onTextChanged(String newText);

    public void setText(String text) {
        super.setText(text);
    }
}
