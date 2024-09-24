package cc.experiments;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;

public class TintingTest extends AWTComponent implements ChangeListener {

    public static void main(String[] args) {
        Utils.setDebugEnabled();
        new TintingTest();
    }

    TintingTest() {

        AWTFrame frame = new AWTFrame("Tinting Test") {

            @Override
            protected void onWindowClosing() {
                try {
                    //app.figures.saveToFile(app.figuresFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        slider = new JSlider();
        slider.addChangeListener(this);
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(50);
        frame.add(this);
        frame.add(slider, BorderLayout.SOUTH);
        frame.centerToScreen(600,600);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        repaint();
    }

    JSlider slider;

    float progress = 0;

    @Override
    protected float getInitProgress() {
        return progress;
    }

    @Override
    protected void init(AWTGraphics g) {

        image = g.loadImage("zchar_benson.gif");
        progress = 0.5f;
        repaint();
        outline = g.loadImage("zchar_benson_outline.gif");

        progress = 1;
        repaint();
    }

    int image = 0;
    int outline = 0;

    int [] modes = {
            AlphaComposite.CLEAR,
            AlphaComposite.SRC,
            AlphaComposite.SRC_OVER,
            AlphaComposite.DST_OVER,
            AlphaComposite.SRC_IN,
            AlphaComposite.DST_IN,
            AlphaComposite.SRC_OUT,
            AlphaComposite.DST_OUT,
            AlphaComposite.DST,
            AlphaComposite.SRC_ATOP,
            AlphaComposite.DST_ATOP,
            AlphaComposite.XOR
    };

    String [] names = {
            "CLEAR",
            "SRC",
            "SRC_OVER",
            "DST_OVER",
            "SRC_IN",
            "DST_IN",
            "SRC_OUT",
            "DST_OUT",
            "DST",
            "SRC_ATOP",
            "DST_ATOP",
            "XOR"
    };

    @Override
    protected void paint(AWTGraphics g) {

        GRectangle rect = new GRectangle(0, 0, getWidth(), getHeight());
        AImage imagesrc = g.getImage(image);
        AImage outlineSrc = g.getImage(outline);

        rect.scale(.25f);
        rect.x = 0;
        rect.y = 0;

        GRectangle srcRect = rect.fit(imagesrc);

        int mode = 0;

        g.pushMatrix();
        for (int i=0; i<3; i++) {
            g.pushMatrix();

            for (int ii=0; ii<4; ii++) {

                g.removeFilter();

                //g.removeFilter();
                //g.setTransparencyFilter(.5f);
                //g.drawImage(image, srcRect);
                //
                //g.setColor(GColor.RED.withAlpha(0));
                g.setTintFilter(GColor.WHITE, GColor.RED);
                //g.setComposite(BlendComposite.Multiply);
                g.drawImage(outline, srcRect);
                g.removeFilter();
                //g.drawFilledRect(srcRect);

                g.removeFilter();
                g.setColor(GColor.BLACK);
                g.drawJustifiedString(rect.w/2, rect.h+5, Justify.CENTER, Justify.TOP, names[mode]);
                g.translate(getWidth()/4, 0);
                mode++;
            }
            g.popMatrix();
            g.translate(0, getHeight()/3);
        }
        g.popMatrix();

    }
}
