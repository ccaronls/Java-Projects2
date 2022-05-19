package cc.lib.swing;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public abstract class AWTRadioButtonGroup<T> extends ButtonGroup implements ActionListener {

    Container holder;
    HashMap<JRadioButton, T> extra = new HashMap<JRadioButton, T>();
    JRadioButton checked = null;

    public AWTRadioButtonGroup(Container holder) {
        this.holder = holder;
    }
    
    public void addButton(String text, T extra) {
        add(new JRadioButton(text), extra);
    }
    
    private void add(JRadioButton button, T extra) {
        super.add(button);
        if (this.extra.size() == 0) {
            checked = button;
            button.setSelected(true);
        } else {
            button.setSelected(false);
        }
        holder.add(button);
        button.addActionListener(this);
        this.extra.put(button, extra);
        buttons.add(button);
    }

    private boolean ignore = false;

    @Override
    public void actionPerformed(ActionEvent arg0) {
        checked = (JRadioButton)arg0.getSource();
        if (!ignore) {
            T extra = this.extra.get(arg0.getSource());
            onChange(extra);
        }
    }

    public void setChecked(int index) {
        ignore = true;
        setSelected(buttons.get(index).getModel(), true);
        ignore = false;
    }

    public T getChecked() {
        return extra.get(checked);
    }

    protected abstract void onChange(T extra);
    
}
