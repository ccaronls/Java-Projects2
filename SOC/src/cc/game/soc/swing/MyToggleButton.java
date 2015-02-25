package cc.game.soc.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
abstract class MyToggleButton extends JCheckBox implements ActionListener {

    MyToggleButton(String text, boolean on) {
        super(text, on);
        addActionListener(this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isSelected()) {
            onChecked();
        } else {
            onUnchecked();
        }
    }

    abstract void onChecked();
    abstract void onUnchecked();
}
