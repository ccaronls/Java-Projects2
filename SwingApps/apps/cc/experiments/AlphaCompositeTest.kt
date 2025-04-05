package cc.experiments

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import java.awt.AlphaComposite
import java.awt.BorderLayout
import javax.swing.JSlider
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AlphaCompositeTest internal constructor() : AWTComponent(), ChangeListener {
	override fun stateChanged(e: ChangeEvent) {
		repaint()
	}

	var slider: JSlider
	override var initProgress = 0f
	override fun init(g: AWTGraphics) {
		idx = g.loadImage("zabomination.png")
		initProgress = 1f
		repaint()
	}

	var idx = 0
	var modes = intArrayOf(
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
	)
	var names = arrayOf(
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
	)

	init {
		val frame: AWTFrame = object : AWTFrame("Alpha Composite Test") {
			override fun onWindowClosing() {
				try {
					//app.figures.saveToFile(app.figuresFile);
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		slider = JSlider()
		slider.addChangeListener(this)
		slider.minimum = 0
		slider.maximum = 100
		slider.value = 50
		frame.add(this)
		frame.add(slider, BorderLayout.SOUTH)
		frame.centerToScreen(600, 600)
	}

	override fun paint(g: AWTGraphics) {
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
// */
		val rect = GRectangle(0f, 0f, width.toFloat(), height.toFloat()) //.scaledBy(.5f);
		val src = g.getImage(idx)
		var srcRect = rect.fit(src!!)
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
// */
		//GRectangle rect = new GRectangle(0,0,getWidth(), getHeight());
		rect.scale(.25f)
		rect.left = 0f
		rect.top = 0f
		srcRect = rect.fit(src)
		var mode = 0
		g.pushMatrix()
		for (i in 0..2) {
			g.pushMatrix()
			for (ii in 0..3) {
				g.color = GColor.BLUE
				//((Graphics2D)g.getGraphics()).setXORMode(Color.RED);
				//g.setAlphaCompisite(0.01f * slider.getValue(), modes[mode]);
				srcRect.drawFilled(g)
				g.setAlphaComposite(0.01f * slider.value, modes[mode])
				g.drawImage(idx, srcRect)

				//srcRect.drawFilled(g);
				g.setAlphaComposite(1f, AlphaComposite.XOR)
				//g.setXorMode(GColor.TRANSPARENT);
				//g.drawImage(idx, srcRect);
				srcRect.drawFilled(g)
				g.removeFilter()
				g.setXorMode(null)
				g.color = GColor.BLACK
				g.drawJustifiedString(rect.width / 2, rect.height + 5, Justify.CENTER, Justify.TOP, names[mode])
				g.translate((width / 4).toFloat(), 0f)
				mode++
			}
			g.popMatrix()
			g.translate(0f, (height / 3).toFloat())
		}
		g.popMatrix()

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

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			AlphaCompositeTest()
		}
	}
}
