package cc.lib.swing;
 
/*
    The KeyboardAnimation applet provides a generic framework for applets
    both display an animation and respond to keyboard events.  The animation
    runs only when the applet has the keyboard focus and can respond to 
    key presses.  The appearance of the applet changes, depending on whether
    it has the keyboard focus.  Note that each time a new frame of the 
    animation is to be displayed, it is drawn completely from scratch.
    When the applet has the keyboard focus, a cyan border is drawn around
    it.  When it does not have the keyboard focus, the border is in
    the applet's background color and a message "Click to activate" is
    displayed in the applet's foreground color and font.

    This class would be appropriate, for example, as a basis for a typical
    arcade game, such as Space Invaders.  (Except that the preformance
    won't be so good.)
    
    To use this framework, define a subclass of AWTAWTKeyboardAnimationApplet and
    override the drawFrame() method.  This method is responsible for drawing
    one frame of the animation.  If you need to some initialization at the
    time the applet is created, override the doInitialization() method.
    This method is called once when the applet is created.  (You should
    NOT override the standard applet methods, init(), start(), stop(), or
    destroy() unless you call the inherited versions from this class.  These
    routines perform important functions in this class.)
    
    In this class, the applet is already set up to "listen" for keyboard
    events.  To make your applet respond to keyboard events, you should
    override one or more of the methods keyPressed(), keyReleased(),
    and keyTyped().  (The applet also listens for MouseEvents, and you
    can override the mouse handling events if you want.  But if you do
    override mousePressed(), be sure to call requestFocus() in that
    routine.)
    
    To respond to key presses, you should have some instance variables
    that affect the image drawn.  Change these variables in the keyPressed,
    keyReleased, or keyTyped methods.
    
    (Alternatively, instead of defining a subclass, you could copy this
    file, change its name and the name of the class, and edit it.)
    
    (This applet is requires Java 1.1, since it uses Java 1.1 style event
    handling.)
    
    David Eck
    Department of Mathematics and Computer Science
    Hobart and William Smith Colleges
    Geneva, NY 14456
    eck@hws.edu
    
    October 2, 1998
    Small modifications 18 February 2000
*/

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

import javax.swing.JApplet;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Renderable;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public abstract class AWTKeyboardAnimationApplet extends JApplet implements
        KeyListener, FocusListener, MouseListener,
        MouseMotionListener, MouseWheelListener, Renderable {
    static final long serialVersionUID = 20003;

    static final Logger log = LoggerFactory.getLogger(AWTKeyboardAnimationApplet.class);

    /**
     * This routine is called once when the applet is first created.
     * You can override it to do initialzation of your instance
     * variables.  It's also a good place to call setFrameCount()
     * and setMillisecondsPerFrame(), if you want to customize these
     * values.  The parameters tell the size of the drawing area
     * at the time the applet is created.
     */
    protected abstract void doInitialization();

	/**
	 * This routine should be overridden in any subclass of AWTAWTKeyboardAnimationApplet.
	 * It is responsible for drawing one frame of the animation.  The frame
	 * is drawn to the graphics context g.  The parameters width and height
	 * give the size of the drawing area.  drawFrame() should begin by
	 * filling the drawing area with a background color (as is done in this
	 * version of drawFrame).  It should then draw the contents of the
	 * frame.  The routine can call getFrameNumber() to dermine which frame
	 * it is supposed to draw.  It can call getElapsedTime() to find out
	 * how long the animation has been running, in milliseconds.
	 * Note that this routine should not take a long time to execute!
	 * As an example, the elapsed number of seconds and the frame number
	 * are output.
	 * 
	 */
	protected abstract void drawFrame(AGraphics g);

	/**
	 * This function is called when the dimension changes
	 * 
	 * @param width
	 * @param height
	 */
	protected abstract void onDimensionsChanged(AGraphics g, int width, int height);
	
	
	
	/* EXAMPLE
	 g.setColor(Color.lightGray);
	 g.fillRect(0,0,width,height);
	 g.setColor(Color.black);
	 g.drawString("Elapsed Time:  " + (getElapsedTime()/1000),10,20);
	 g.drawString("Frame Number:  " + (getFrameNumber()),10,35);
	 }*/

	@Override
	public void keyTyped(KeyEvent evt) {
	}

	@Override
	public void keyPressed(KeyEvent evt) {
		int c = evt.getKeyChar();
		Utils.assertTrue(c >= 0);
		if (c >= 0 && c < keyboard.length) {
    		if (keyboard[c] == 0 || this.keyRepeat)
    			keyboard[c] = 1;
		}
	}

	@Override
	public void keyReleased(KeyEvent evt) {
        int c = evt.getKeyChar();
		if (c >= 0 && c < keyboard.length)
		    keyboard[evt.getKeyChar()] = 0;
	}

	/**
	 * Get the current frame number.  The frame number will be incremented
	 * each time a new frame is to be drawn.  The first frame number is 0.
	 * (If frameCount is greater than zero, and if frameNumber is greater than
	 * or equal to frameCount, then frameNumber returns to 0.)  For a keyboard
	 * applet, you are not too likely to need frame numbers, actually.
	 * 
	 * @return
	 */
	public final int getFrameNumber() {
		return frameNumber;
	}

	/**
	 * Set the current frame number.  This is the value returned by getFrameNumber().
	 * 
	 * @param frameNumber
	 */
	public final void setFrameNumber(int frameNumber) {
		if (frameNumber < 0)
			this.frameNumber = 0;
		else
			this.frameNumber = frameNumber;
	}

	/**
	 * return the total number of milliseconds that the animation has been
	 * running (not including the time when the applet is suspended by
	 * the system or when the applet does not have the keyboard focus).
	 * @return
	 */
	public final long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * If you want your animation to loop through a set of frames over
	 * and over, you should call this routine to set the frameCount to 
	 * the number of frames in the animation.  Frames will be numbered
	 * from 0 to frameCount - 1.  If you specify a value <= 0, the
	 * frameNumber will increase indefinitely without ever returning
	 * to zero.  The default value of frameCount is -1, meaning that
	 * by default frameNumber does NOT loop.
	 * 
	 * @param max
	 */
	public final void setFrameCount(int max) {
		if (max <= 0)
			this.frameCount = -1;
		else
			frameCount = max;
	}

	/**
	 * Set the approximate number of milliseconds to be used for each frame.
	 * For example, set time = 1000 if you want each frame to be displayed for
	 * about a second.  The time is only approximate, and the actual display
	 * time will probably be a bit longer.  The default value of 40 is
	 * probably OK for a game.
	 * 
	 * @param time
	 */
	public final void setMillisecondsPerFrame(int time) {
		millisecondsPerFrame = time;
	}
	
	/**
	 * Set the target FPS to run at.  There is no guarantee the app will run at the FPS, 
	 * but the app should not exceed to target FPS.
	 * @param fps value > 0
	 */
	public final void setTargetFPS(int fps) {
		if (fps <= 0)
			throw new IllegalArgumentException("Invalid fps value: " + fps);
		setMillisecondsPerFrame(1000/fps);
	}

	/**
	 * An applet must allow some time for the computer to do other tasks.
	 * In order to do this, the animation applet "sleeps" between frames.
	 * This routine sets a minimum sleep time that will be applied even if
	 * that will increase the display time for a frame above the value
	 * specified in setMillisecondsPerFrame().  The parameter is given in
	 * milliseconds.  The default value is 10.  You can set this to
	 * any positive value.
	 *     
	 * @param time
	 */
	public final void setMinimumSleepTime(int time) {
		if (time <= 0)
			minimumSleepTime = 1;
		else
			minimumSleepTime = time;
	}

	/**
	 * Set the color of the three-pixel border that surrounds the applet
	 * when the applet has the keyboard focus. The default color is cyan.
	 * 
	 * @param c
	 */
	public final void setFocusBorderColor(Color c) {
		focusBorderColor = c;
	}

	// This rest of this file is private stuff that you don't have to know about
	// when you write your own animations.

	private int frameNumber = 0;

	private int frameCount = -1;

	private int millisecondsPerFrame = 40;

	private int minimumSleepTime = 10;

	private long startTime;

	private long oldElapsedTime;

	private long elapsedTime;

	private Thread runner;

	private Image OSC;

	private AWTGraphics OSG;

	private enum Status {
	    GO,
	    SUSPEND,
	    TERMINATE;
	};

	private Status status = Status.GO;

	private int width = -1;

	private int height = -1;

	private boolean focussed = false; // set to true when the applet has the keyboard focus
	
	private int borderThickness = 3;

	private Color focusBorderColor = Color.cyan;
	
	private boolean initialized = false;

	public final void init() {
		setBackground(Color.gray); // Color used for border when applet doesn't have focus.
		setForeground(Color.red);
		setFont(new Font("SanSerif", Font.BOLD, 14));
		addFocusListener(this);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		doInitialization();
		initialized = true;
	}

	/*
	 *  (non-Javadoc)
	 * @see java.applet.Applet#start()
	 */
	synchronized public final void start() {
		// Called by the system when the applet is first started 
		// or restarted after being stopped.  This routine creates
		// or restarts the thread that runs the animation.  Also,
		// if focussed is true, then the animation will start.  So
		// we should start the timing mechanism by setting startTime == 1.
		if (runner == null || !runner.isAlive()) {
			runner = new Thread(new Runnable() {
			    public void run() {
			        runLoop();
			    }
			});
			runner.start();
		}
		status = Status.GO;
		if (focussed)
			startTime = -1; // signal to run() to compute startTime
		synchronized (runLock) {
		    runLock.notify();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see java.applet.Applet#stop()
	 */
	synchronized public final void stop() {
		// Called by the system to suspend the applet. Suspend the
		// animation thread by setting status to SUSPEND.
		// Also, update oldElapsedTime, which keeps track of the
		// total running time of the animation time between
		// calls to start() and stop().  Also, if focussed is
		// true, then the animation will change state from running
		// to paused, so we should record the elapsed time.
		if (focussed)
			oldElapsedTime += (getTimeMilis() - startTime);
		status = Status.SUSPEND;
		synchronized (runLock) {
		    runLock.notify();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see java.applet.Applet#destroy()
	 */
	public final void destroy() {
		// Called by the system when the applet is being permanently
		// destroyed.  This tells the animation thread to stop by
		// setting status to TERMINATE.
		if (runner != null && runner.isAlive()) {
			synchronized (runLock) {
				status = Status.TERMINATE;
				runLock.notify();
			}
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#update(java.awt.Graphics)
	 */
	public final void update(Graphics g) {
		// Called by system when applet needs to be redrawn.
		paint(g);
	}

	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	synchronized public final void paint(Graphics g) {
		// Draw the current frame on the applet drawing area.  If the 
		// applet has focus, draw a cyan border around the frame.  Otherwise,
		// draw a message telling the user to click on the applet to 
		// activate it.

		if (width != getSize().width || height != getSize().height) { // if size has changed, recreate frame
			doSetup();
			if (OSC != null && initialized) {
				drawFrame(OSG);
			}
		}

		if (OSC == null) { // if not enough memory for OSC, draw an error message
			g.setColor(getBackground());
			g.fillRect(0, 0, width, height);
			g.setColor(getForeground());
			g.drawString("Sorry, out of Memory!", 10, 25);
			return;
		}

		g.drawImage(OSC, borderThickness, borderThickness, this);

		if (focussed) // Draw a 3-pixel border.  If the applet has the
			g.setColor(focusBorderColor); //   focus, draw it in focusBorderColor; otherwise,
		else
			//   draw it in the background color.
			g.setColor(getBackground());
		g.drawRect(0, 0, width - 1, height - 1);
		g.drawRect(1, 1, width - 3, height - 3);
		g.drawRect(2, 2, width - 5, height - 5);

		if (!focussed) { // If the applet does not have the focus,
			g.setColor(getForeground()); //    print a message for the user.
			g.drawString("Click to activate", 10, height - 12);
		}

	} // end paint

	/*
	 * 
	 */
	private void doSetup() {
		// creates OSC and graphics context for OSC
		width = getSize().width;
		height = getSize().height;
		
		OSC = null; // free up any memory currently used by OSC before allocating new memory
		try {
			OSC = createImage(width - borderThickness*2, height -  borderThickness*2);
			if (OSG != null)
			    OSG = new AWTGraphics(OSG, OSC.getGraphics(), this);
			else
			    OSG = new AWTGraphics(OSC.getGraphics(), this);
			OSG.setColor(Color.BLACK);
			OSG.setFont(getDefaultFont());
			OSG.initViewport(width, height);
	        onDimensionsChanged(OSG, width, height);
		} catch (OutOfMemoryError e) {
			OSC = null;
			OSG = null;
		}
	}

	public final int getBorderThickness() {
		return this.borderThickness;
	}
	
	protected Font getDefaultFont() {
	    return new Font("Serif", Font.PLAIN, 12);
	}
	
	public final void setFont(Font font) {
	    if (OSG != null) {
	        OSG.setFont(font);
	    }
	}

	private int fps = 0;
	
	public final int getFPS() {
	    return fps;
	}
	
	public final long getTimeMilis() {
	    //return System.currentTimeMillis();
	    return System.nanoTime() / 1000000;
	}
	
	private Object runLock = this;//
	
	private void runLoop() {
		// Runs the animation.  The animation thread executes this routine.
		long lastFrameTime = 0;
		int framesThisSecond = 0;
		long fpsTime = 0;
		long lastFpsTime = 0;
		while (true) {
			synchronized (runLock) {
				while (status == Status.SUSPEND || !focussed) {
					try {
					    //System.out.println("status = " + status + " focussed=" + focussed);
					    runLock.wait(1000); // animation has been suspended; wait for it to be restarted
					} catch (InterruptedException e) {
					}
				}
				if (status == Status.TERMINATE) { // exit from run() routine and terminate animation thread
					return;
				}
				if (width != getSize().width || height != getSize().height) // check for applet size change
					doSetup();
				long thisTime = getTimeMilis();
				if (startTime == -1) {
					startTime = thisTime;
					lastFpsTime = thisTime;
					elapsedTime = oldElapsedTime;
				} else
					elapsedTime = oldElapsedTime
							+ (thisTime - startTime);
				if (frameCount >= 0 && frameNumber >= frameCount)
					frameNumber = 0;
				if (OSC != null) {
					try {
						drawFrame(OSG); // draw current fram to OSC
					} catch (Throwable t) {
						t.printStackTrace();
					}
					long t = getTimeMilis();
					long frameTime = t - lastFpsTime;
					lastFpsTime = t;
					fpsTime += frameTime;
					if (fpsTime > 1000) {
                        this.fps = framesThisSecond;
					    framesThisSecond = 1;
					    while (fpsTime > 1000)
					        fpsTime -= 1000;
					} else {
	                    framesThisSecond ++;
//	                    System.out.println("framesThisSecond=" + framesThisSecond);
					}
					if (AGraphics.DEBUG_ENABLED) {
					    OSG.setColor(Color.RED);
					    OSG.drawJustifiedString(getViewportWidth()-30, 0, Justify.RIGHT, Justify.TOP, "FPS: " + fps);
					}
				}
				frameNumber++;
			}
			long time = getTimeMilis();
			long sleepTime = (lastFrameTime + millisecondsPerFrame) - time;
			if (sleepTime < minimumSleepTime)
				sleepTime = minimumSleepTime;
			repaint(); // tell system to redraw the applet to display the new frame
			try {
				synchronized (runLock) {
				    runLock.wait(sleepTime);
				}
			} catch (InterruptedException e) {
			}
			lastFrameTime = getTimeMilis();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	synchronized public final void focusGained(FocusEvent evt) {
        log.info("focusGained");
		// The applet now has the input focus. Set focussed = true and repaint.
		// Also, if both (focussed && status == GO), the animation will start,
		// so we have to restart the timing utility by setting startTime = -1;
		grabFocus();
		onPauseChanged(false);
	}

	/*
	 *  (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	synchronized public final void focusLost(FocusEvent evt) {
        log.info("focusLost");
		// The applet has lostthe input focus. Set focussed = false and repaint.
		// Also, if both (focussed && status == GO) were previously true, then
		// the animation will be stopped at this point, so we should record the time.
		focussed = false;
		repaint(); // redraw without cyan border
		if (status == Status.GO)
			oldElapsedTime += (getTimeMilis() - startTime);
        synchronized (runLock) {
            runLock.notify();
        }
        onPauseChanged(true);
	}
	
	protected void onPauseChanged(boolean paused) {
		
	}

	private int eventToInt(int evtButton) {
		switch (evtButton) {
			case MouseEvent.BUTTON1:
				return 0;
			case MouseEvent.BUTTON2:
				return 1;
			case MouseEvent.BUTTON3:
				return 2;
		}
		return 4;
	}
	
	/*
	 *  (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public final void mousePressed(MouseEvent evt) {
		requestFocus();
		if (evt.isControlDown() && evt.getButton() == MouseEvent.BUTTON1)
			mouseButtons[eventToInt(MouseEvent.BUTTON3)] = 1;
		else
			mouseButtons[eventToInt(evt.getButton())] = 1;
		onMousePressed(evt);
	}

	protected void onMousePressed(MouseEvent ev) {}

	public void mouseEntered(MouseEvent evt) {
	} 

	public void mouseExited(MouseEvent evt) {
	} 

	synchronized public void mouseReleased(MouseEvent evt) {
		int index = 0;
		if (evt.isControlDown() && evt.getButton() == MouseEvent.BUTTON1)
			index = eventToInt(MouseEvent.BUTTON3);
		else
			index = eventToInt(evt.getButton());
		if (mouseButtons[index] == 1)
			mouseButtons[index] = 2;
	} 

	public void mouseClicked(MouseEvent evt) {
	}

	public void mouseWheelMoved(MouseWheelEvent ev) {
	}

	public void mouseMoved(MouseEvent ev) {
		mouseDX = ev.getX() - mouseX;
		mouseDY = ev.getY() - mouseY;
		mouseX = ev.getX();
		mouseY = ev.getY();
	}

	public void mouseDragged(MouseEvent ev) {
		mouseDX = ev.getX() - mouseX;
		mouseDY = ev.getY() - mouseY;
		mouseX = ev.getX();
		mouseY = ev.getY();
	}

	protected final int getMouseX() {
		return mouseX;
	}

	protected final int getMouseY() {
		return mouseY;
	}

	protected final int getMouseDX() {
		return mouseDX;
	}

	protected final int getMouseDY() {
		return mouseDY;
	}

	protected final boolean getMouseButtonClicked(int button) {
		boolean clicked = mouseButtons[button] != 0;
		mouseButtons[button] = 0;
		return clicked;
	}

	/**
	 * 
	 * @param button
	 * @return
	 */
	protected final boolean getMouseButtonPressed(int button) {
		return mouseButtons[button] == 1;
	}

	protected final boolean getKeyboard(char c) {
		return keyboard[c] == 1;
	}

	protected final boolean getKeyboardReset(char c) {
		boolean r = keyboard[c] == 1;
		if (r)
			keyboard[c] = -1;
		return r;
	}

	/**
	 * 
	 *
	 */
	protected final void clearScreen(GColor color) {
		if (OSG != null) {
			GColor old = OSG.getColor();
			OSG.setColor(color);
			this.OSG.drawFilledRect(0, 0, width, height);
			OSG.setColor(old);
		}
	}

	protected final void clearScreen() {
		clearScreen(GColor.BLACK);
	}

	@Override
	public final int getViewportWidth() {
		return width-borderThickness*2;
	}

	@Override
	public final int getViewportHeight() {
		return height-borderThickness*2;
	}
	
	/**
	 * Synonym of getViewportHeight
	 * @return
	 */
	public final int getScreenWidth() {
		return getViewportWidth();
	}
	
	/**
	 * Synonym for getViewportHeight
	 * @return
	 */
	public final int getScreenHeight() {
		return getViewportHeight();
	}
	
	public void grabFocus() {
		if (!focussed) {
    		focussed = true;
    		repaint(); // redraw with cyan border
    		if (status == Status.GO)
    			startTime = -1; // signal to run() to compute startTime
    		synchronized (runLock) {
    		    runLock.notify();
    		}		
		}
	}
	
	protected void setKeyRepeat(boolean repeat) {
		this.keyRepeat = repeat;
	}

	protected AWTKeyboardAnimationApplet() {
		Arrays.fill(keyboard, 0);
	}
	
	private boolean keyRepeat = false;
	
	private int mouseX = 0;

	private int mouseY = 0;

	private int mouseDX = 0;

	private int mouseDY = 0;

	private int[] mouseButtons = new int[10]; // 1==down, 2=released, 0==unset
	
	private int [] keyboard = new int[256];

} // end class AnimationApplet
