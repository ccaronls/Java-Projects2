package cc.lib.swing;

import javax.swing.JLabel;

@SuppressWarnings("serial")
public class AWTWrapLabel extends JLabel {

    public AWTWrapLabel() {
        super();
    }

    public AWTWrapLabel(String text) {
        setText(text);
    }
    

    @Override
    public void setText(String arg0) {
    	super.setText("<html>" + arg0.replace("\n", "</br>") + "</html>");
    }
    
    
    
}
