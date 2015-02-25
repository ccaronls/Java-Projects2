package cc.game.soc.swing;

import java.awt.event.ActionListener;

import javax.swing.JTextField;

@SuppressWarnings("serial")
public class TextInputOp extends JTextField {

    private final OpTextField op;
    private final Object extra;
    
    TextInputOp(OpTextField op, Object extra, ActionListener listener) {
        this.op = op;
        this.extra = extra;
        addActionListener(listener);
    }

    public OpTextField getOp() {
        return op;
    }

    public Object getExtra() {
        return extra;
    }
    
    
    
    //public void actionPerformed(ActionEvent e) {
      //  GUI.getInstance().processOpTextField(op, getText(), extra);
    //}

    
    
}
