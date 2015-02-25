package cc.game.soc.swing;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public abstract class MyRadioButtonGroup<T> extends ButtonGroup implements ActionListener {

    Container holder;
    HashMap<JRadioButton, T> extra = new HashMap<JRadioButton, T>();
    
    MyRadioButtonGroup(Container holder) {
        this.holder = holder;
    }
    
    void addButton(String text, T extra) {
        add(new JRadioButton(text), extra);
    }
    
    void add(JRadioButton button, T extra) {
        super.add(button);
        if (this.extra.size() == 0)
            button.setSelected(true);
        else
            button.setSelected(false);
        holder.add(button);
        button.addActionListener(this);
        this.extra.put(button, extra);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        T extra = this.extra.get(arg0.getSource());
        onChange(extra);
    }

    protected abstract void onChange(T extra);
    
}
