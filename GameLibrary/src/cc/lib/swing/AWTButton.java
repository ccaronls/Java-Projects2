package cc.lib.swing;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.utils.FileUtils;

public class AWTButton extends JButton implements ActionListener {

    public static AWTButton createWithImage(String fileOrResource) throws Exception {
        try (InputStream in = FileUtils.openFileOrResource(fileOrResource)) {
            Image img = ImageIO.read(in);
            return new AWTButton(img);
        }
    }

    private Object data;

    public AWTButton(Image icon) {
        super(new ImageIcon(icon));
        addActionListener(this);
    }

    public AWTButton(IButton source) {
        super("<html>" + source.getLabel() + "</html>");
        addActionListener(this);
        String ttt = source.getTooltipText();
        if (ttt != null) {
            if (ttt.length() >= 64) {
                ttt = Utils.wrapTextWithNewlines(ttt, 64);
                ttt = String.format("<html>%s</html>", ttt.replaceAll("[\n]+", "<br/>"));
            }
            setToolTipText(ttt);
        }
    }

    public AWTButton(IButton source, ActionListener listener) {
        this(source.getLabel(), listener);
        setToolTipText(source.getTooltipText());
    }

    public AWTButton setTooltip(String text, int maxChars) {
        setToolTipText(Utils.wrapTextWithNewlines(text, maxChars));
        return this;
    }

    public AWTButton(String label, boolean selected) {
        this(label);
        setSelected(selected);
    }

    public AWTButton(String label) {
        super(label.indexOf('\n') >= 0 ? "<html>" + label.replaceAll("[\n]", "<br/>") + "</html>" : label);
        addActionListener(this);
    }

    public AWTButton(String label, ActionListener listener) {
        super(label);
        addActionListener(listener);
    }

    public AWTButton(String label, Object data, ActionListener listener) {
        super(label);
        addActionListener(listener);
        this.data = data;
    }

    public void actionPerformed(ActionEvent e) {
        onAction();
    }

    protected void onAction() {
        System.err.println("Unhandled action");
    }

    public void toggleSelected() {
        setSelected(!isSelected());
    }

    public boolean isSelected() {
        return super.isSelected();
    }

    public <T> T getData() {
        return (T)data;
    }
}
