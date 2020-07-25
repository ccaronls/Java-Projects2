package cc.lib.swing;

import java.awt.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.io.InputStream;

import cc.lib.utils.FileUtils;

public class AWTButton extends JButton implements ActionListener {

    public static AWTButton createWithImage(String fileOrResource) throws Exception {
        try (InputStream in = FileUtils.openFileOrResource(fileOrResource)) {
            Image img = ImageIO.read(in);
            return new AWTButton(img);
        }
    }

    public AWTButton(Image icon) {
        super(new ImageIcon(icon));
        addActionListener(this);
    }

    public AWTButton(String label, boolean selected) {
        this(label);
        setSelected(selected);
    }

    public AWTButton(String label) {
        super(label);
        addActionListener(this);
    }

    public AWTButton(String label, ActionListener listener) {
        super(label);
        addActionListener(listener);
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
}
