package cc.lib.main;

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

public class AlphaCompositeTest extends AWTComponent implements ChangeListener {

    public static void main(String[] args) {
        Utils.setDebugEnabled();
        new AlphaCompositeTest();
    }

    AlphaCompositeTest() {

        AWTFrame frame = new AWTFrame("Alpha Composite Test") {

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

        idx = g.loadImage("zabomination.png");

        progress = 1;
        repaint();
    }

    int idx = 0;

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
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
/*
        GRectangle r = new GRectangle(0,0,getWidth(),getHeight()).scaledBy(.5f);
        g.setColor(GColor.RED);//.inverted());//.withAlpha(.5f));
        r.drawFilled(g);
        g.setColor(GColor.RED);//.inverted());//.withAlpha(.5f));
        g.setXorMode(GColor.TRANSPARENT);
        r.scale(.8f);
        r.drawFilled(g);
        //r.drawFilled(g);
        if (true)
            return;
//*/

        GRectangle rect = new GRectangle(0,0,getWidth(),getHeight());//.scaledBy(.5f);
        AImage src = g.getImage(idx);
        GRectangle srcRect = rect.fit(src);
//        Graphics2D g2d = ((Graphics2D)g.getGraphics());
/*
        g.setColor(GColor.BLUE);
        //g.setXorMode(GColor.YELLOW);
        g.setXorMode(GColor.TRANSPARENT);
        //g.setAlphaCompisite(1, AlphaComposite.SRC_IN);
        //g.drawImage(idx, srcRect);
        srcRect.drawFilled(g);
        //g.removeFilter();
        //g.setAlphaCompisite(1, AlphaComposite.DST_IN);
        //g.setColor(GColor.TRANSPARENT);
        //g.removeFilter();
        //g.setXorMode(GColor.TRANSPARENT);
        //g.setColor(GColor.BLUE.inverted());
        //g.setXorMode(GColor.YELLOW);
        //g.setXorMode(GColor.TRANSPARENT);
        g.setAlphaCompisite(1, AlphaComposite.SRC_OUT);
        g.drawImage(idx, srcRect);
        //srcRect.drawFilled(g);
        //g.setXorMode(null);
        //g.setColor(GColor.TRANSPARENT);
        //g.setXorMode(GColor.TRANSPARENT);
        //g.setAlphaCompisite(1, AlphaComposite.DST_IN);
        //((Graphics2D)g.getGraphics()).setXORMode(Color.BLUE);//new Color(0,0,0,0));
        //g.setColor(GColor.BLUE);
        //g.setXorMode(GColor.BLUE);
        //g.setXorMode(GColor.BLACK);
        //g.drawImage(idx, srcRect);
        //srcRect.drawFilled(g);
        g.setXorMode(null);
        g.removeFilter();
        if (true)
            return;
//*/
        //GRectangle rect = new GRectangle(0,0,getWidth(), getHeight());
        rect.scale(.25f);
        rect.x = 0;
        rect.y = 0;

        srcRect = rect.fit(src);


        int mode = 0;

        g.pushMatrix();
        for (int i=0; i<3; i++) {
            g.pushMatrix();
            for (int ii=0; ii<4; ii++) {
                g.setColor(GColor.BLUE);
                //((Graphics2D)g.getGraphics()).setXORMode(Color.RED);
                //g.setAlphaCompisite(0.01f * slider.getValue(), modes[mode]);
                srcRect.drawFilled(g);
                g.setAlphaCompisite(0.01f * slider.getValue(), modes[mode]);
                g.drawImage(idx, srcRect);

                //srcRect.drawFilled(g);
                g.setAlphaCompisite(1, AlphaComposite.XOR);
                //g.setXorMode(GColor.TRANSPARENT);
                //g.drawImage(idx, srcRect);
                srcRect.drawFilled(g);

                g.removeFilter();
                g.setXorMode(null);
                g.setColor(GColor.BLACK);
                g.drawJustifiedString(rect.w/2, rect.h+5, Justify.CENTER, Justify.TOP, names[mode]);
                g.translate(getWidth()/4, 0);
                mode++;
            }
            g.popMatrix();
            g.translate(0, getHeight()/3);
        }
        g.popMatrix();

/*
        Vector2D center = new Vector2D(getWidth()/2, getHeight()/2);
        g.clearScreen();
        g.drawImage(images.get(idx), center);
        idx = (idx+1) % images.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        wait(100);
                    }
                    repaint();
                } catch (Exception e) {}
            }
        }).start();*/
    }
}
