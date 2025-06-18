package cc.applets.roids

import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
import java.awt.event.KeyEvent
import java.io.File

/**
 * Created by chriscaron on 10/30/17.
 */
class JavaRoidsApplet : AWTKeyboardAnimationApplet() {
	var jr: JavaRoids = object : JavaRoids() {

		override val frameNumber: Int
			get() = getFrameNumber()
		override val screenWidth: Int
			get() = getScreenWidth()
		override val screenHeight: Int
			get() = getScreenHeight()

	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		jr.initGraphics(g)
		//g.ortho();
		g.ortho((-width / 2).toFloat(), (width / 2).toFloat(), (height / 2).toFloat(), (-height / 2).toFloat())
	}

	//---------------------------------------------------------------
	override fun keyTyped(e: KeyEvent) {
		val key = e.keyCode
		when (key) {
			KeyEvent.VK_SPACE -> {}
			KeyEvent.VK_S -> jr.player_v.assign(0f, 0f)
		}
	}

	//---------------------------------------------------------------
	override fun onKeyPressed(e: KeyEvent) {
		val key = e.keyCode
		when (key) {
			KeyEvent.VK_LEFT -> jr.pressButton(JavaRoids.PLAYER_BUTTON_LEFT)
			KeyEvent.VK_RIGHT -> jr.pressButton(JavaRoids.PLAYER_BUTTON_RIGHT)
			KeyEvent.VK_UP -> jr.pressButton(JavaRoids.PLAYER_BUTTON_THRUST)
			KeyEvent.VK_DOWN -> {}
			KeyEvent.VK_SHIFT -> jr.pressButton(JavaRoids.PLAYER_BUTTON_SHIELD)
			KeyEvent.VK_Z, KeyEvent.VK_SPACE -> jr.pressButton(JavaRoids.PLAYER_BUTTON_SHOOT)
			KeyEvent.VK_ALT -> {}
			KeyEvent.VK_S -> jr.player_v.zeroEq()
		}
	}

	override fun doInitialization() {
		jr.doInitialization()
	}

	override fun drawFrame(g: AGraphics) {
		jr.drawFrame(g)
	}

	//---------------------------------------------------------------
	override fun onKeyReleased(e: KeyEvent) {
		val key = e.keyCode
		when (key) {
			KeyEvent.VK_LEFT -> jr.releaseButton(JavaRoids.PLAYER_BUTTON_LEFT)
			KeyEvent.VK_RIGHT -> jr.releaseButton(JavaRoids.PLAYER_BUTTON_RIGHT)
			KeyEvent.VK_UP -> jr.releaseButton(JavaRoids.PLAYER_BUTTON_THRUST)
			KeyEvent.VK_DOWN -> {}
			KeyEvent.VK_SHIFT -> jr.releaseButton(JavaRoids.PLAYER_BUTTON_SHIELD)
			KeyEvent.VK_Z, KeyEvent.VK_SPACE -> jr.releaseButton(JavaRoids.PLAYER_BUTTON_SHOOT)
			KeyEvent.VK_ALT -> {}
		}
	} /*
    //---------------------------------------------------------------
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        mouse_dx = mouse_x - x;
        mouse_dy = mouse_y - y;
        mouse_x = x;
        mouse_y = y;
    }

    //---------------------------------------------------------------
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    //---------------------------------------------------------------
    public void onMousePressed(MouseEvent e) {
        int button=e.getButton();
        switch (button) {
            case MouseEvent.BUTTON1:
                switch (game_state) {
                    case STATE_INTRO:
                        break;

                    case STATE_PLAY:
                        break;
                }
                break;

            case MouseEvent.BUTTON2:
                break;

            case MouseEvent.BUTTON3:
                if (game_state == STATE_PLAY)
                {

                }
                break;
        }
    }

    //---------------------------------------------------------------
    public void mouseReleased(MouseEvent e) {
        if (game_state != STATE_PLAY)
            return;

        int button=e.getButton();
        switch (button) {
            case MouseEvent.BUTTON1:
                break;

            case MouseEvent.BUTTON2:
                break;

            case MouseEvent.BUTTON3:
                break;
        }
    }*/

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame = AWTFrame("JavaRoids Debug Mode")
			val app: AWTKeyboardAnimationApplet = JavaRoidsApplet()
			frame.add(app)
			app.init()
			val settings = FileUtils.getOrCreateSettingsDirectory(JavaRoidsApplet::class.java)
			if (!frame.loadFromFile(File(settings, "gui.properties"))) {
				frame.centerToScreen(620, 620)
			}
			app.start()
			app.setMillisecondsPerFrame(33)
		}
	}
}