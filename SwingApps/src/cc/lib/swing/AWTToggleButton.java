package cc.lib.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import cc.lib.ui.IButton;

public class AWTToggleButton extends JCheckBox implements ActionListener {

    private boolean ignore = false;

    public AWTToggleButton(String text) {
        this(text, false);
    }

    public AWTToggleButton(IButton button) {
        this(button.getLabel(), false);
        setToolTipText(button.getTooltipText());
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
