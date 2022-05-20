package cc.lib.swing;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cc.lib.game.Utils;


public class AWTNumberPicker extends AWTPanel implements ChangeListener {

    public interface Listener {
        void onValueChanged(int oldValue, int newValue);
    };

    private SpinnerNumberModel model;
    private Listener listener;

    public AWTNumberPicker(int rows, int cols) {
        super(rows, cols);
    }

    public AWTNumberPicker() {
        super();
    }

    public int getValue() {
        return (Integer)model.getValue();
    }

    public void setValue(int value) {
        ignore = true;
        model.setValue(value);
        ignore = false;
    }

    private boolean ignore = false;

    public final void stateChanged(ChangeEvent e) {
        if (!ignore && listener != null)
            listener.onValueChanged(0, (Integer)model.getValue());
    }

    public static class Builder {

        private int min=Integer.MIN_VALUE;
        private int max=Integer.MAX_VALUE;
        private int value = 0;
        private int step = 1;
        private String label = null;

        public AWTNumberPicker build(Listener listener) {
            AWTNumberPicker panel;
            if (label != null) {
                panel = new AWTNumberPicker(0, 1);
                panel.add(new JLabel(label));
            } else {
                panel = new AWTNumberPicker();
            }
            value = Utils.clamp(value, min, max);
            panel.model = new SpinnerNumberModel(value, min, max, step);
            JSpinner spinner = new JSpinner(panel.model);
            spinner.addChangeListener(panel);
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
