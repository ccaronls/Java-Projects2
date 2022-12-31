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

    private final static String HTML_PREFIX = "<html><center><p>";
    private final static String HTML_SUFFIX = "</p></center></html>";

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
        this(source, null);
        addActionListener(this);
    }

    public AWTButton(IButton source, ActionListener listener) {
        super(HTML_PREFIX + source.getLabel() + HTML_SUFFIX);
        setActionCommand(source.getLabel());
        if (listener != null)
            addActionListener(listener);
        String ttt = source.getTooltipText();
        if (ttt != null) {
            if (ttt.length() >= 64) {
                ttt = Utils.wrapTextWithNewlines(ttt, 64);
                ttt = String.format("<html>%s</html>", ttt.replaceAll("[\n]+", "<br/>"));
            }
            setToolTipText(ttt);
        }
    }

    public AWTButton setTooltip(String text, int maxChars) {
        setToolTipText(Utils.wrapTextWithNewlines(text, maxChars));
        return this;
    }

    public AWTButton(String label, boolean selected) {
        super(HTML_PREFIX + label + HTML_SUFFIX);
        setActionCommand(label);
        setSelected(selected);
    }

    public AWTButton(String label) {
        super(HTML_PREFIX + label + HTML_SUFFIX);
        setActionCommand(label);
        addActionListener(this);
    }

    public AWTButton(String label, ActionListener listener) {
        super(HTML_PREFIX + label + HTML_SUFFIX);
        setActionCommand(label);
        addActionListener(listener);
    }

    public AWTButton(String label, Object data, ActionListener listener) {
        super(HTML_PREFIX + label + HTML_SUFFIX);
        addActionListener(listener);
        setActionCommand(label);
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

    public <T> T getData() {
        return (T)data;
    }
}
