package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


public class NumberPicker extends EZPanel implements ChangeListener {

    public static interface Listener {
        void onValueChanged(int oldValue, int newValue);
    };

    private SpinnerNumberModel model;
    private Listener listener;

    public NumberPicker(int rows, int cols) {
        super(rows, cols);
    }

    public NumberPicker() {
        super();
    }

    public int getValue() {
        return (Integer)model.getValue();
    }

    public void setValue(int value) {
        model.setValue(value);
    }

    public final void stateChanged(ChangeEvent e) {
        listener.onValueChanged(0, (Integer)model.getValue());
    }

    static class Builder {

        int min=Integer.MIN_VALUE;
        int max=Integer.MAX_VALUE;
        int value = 0;
        int step = 1;
        String label = null;

        public NumberPicker build(Listener listener) {
            NumberPicker panel;
            if (label != null) {
                panel = new NumberPicker(0, 1);
                panel.add(new JLabel(label));
            } else {
                panel = new NumberPicker();
            }
            panel.model = new SpinnerNumberModel(value, min, max, step);
            JSpinner spinner = new JSpinner(panel.model);
            panel.add(spinner);
            panel.listener = listener;
            return panel;
        }

        public Builder setMin(int min) {
            this.min = min;
            return this;
        }

        public Builder setMax(int max) {
            this.max = max;
            return this;
        }

        public Builder setValue(int value) {
            this.value = value;
            return this;
        }

        public Builder setStep(int step) {
            this.step = step;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }
    }

}
