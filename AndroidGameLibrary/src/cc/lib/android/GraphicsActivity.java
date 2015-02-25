package cc.lib.android;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceHolder;

/**
 * Convenience activity for full screen graphics activities like games.  NO layout required.
 * Simply override the basic methods
 * 
 * @author chriscaron
 *
 */
public abstract class GraphicsActivity extends Activity {

	private BaseRenderer renderer;
	
	@Override
	protected final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GLSurfaceView view = new GLSurfaceView(this) {
			
			@Override
		    public void surfaceDestroyed(SurfaceHolder holder) {
				if (renderer != null) {
					synchronized (renderer) {
						renderer.shutDown();
					}
				}
				renderer = null;
			}
			
		};
		view.setRenderer(renderer = new BaseRenderer(view) {
			
			@Override
			protected void init(GL10Graphics g) {
				GraphicsActivity.this.init(g);
			}
			
			@Override
			protected void drawFrame(GL10Graphics g) {
				GraphicsActivity.this.drawFrame(g);
			}
		});
		setContentView(view);
	}

	@Override
	protected final void onResume() {
		super.onResume();
		if (renderer != null) {
			renderer.setPaused(false);
		}
	}

	@Override
	protected final void onPause() {
		super.onPause();
		if (renderer != null) {
			renderer.setPaused(true);
		}
	}

	@Override
	protected final void onDestroy() {
		shutdown();
		super.onDestroy();
	}
	
	protected BaseRenderer getRenderer() {
		return renderer;
	}

	protected abstract void init(GL10Graphics g);
	
	protected abstract void drawFrame(GL10Graphics g);
	
	protected abstract void shutdown();
}
