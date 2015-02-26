package cc.fantasy.swing;

import javax.swing.JTextField;

public class FTextField extends JTextField {

    FTextField() {
        this("", true);
    }
    
    FTextField(String text) {
        this(text, true);
    }
    
    FTextField(String initialText, boolean canEdit) {
        super(initialText);
        setEditable(canEdit);
        setColumns(20);
    }

    
}
