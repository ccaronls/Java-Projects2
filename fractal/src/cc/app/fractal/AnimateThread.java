package cc.app.fractal;

import javax.swing.JTextField;

import java.io.*;

import cc.app.fractal.FractalComponent.FractalListener;
import cc.lib.math.ComplexNumber;

public class AnimateThread implements Runnable {

    private final FractalComponent fractal;
    private final JTextField currentField;
    private final FractalViewer viewer;
    private int frames= 100;
    
    AnimateThread (FractalComponent fractal, FractalViewer viewer, JTextField currentField) {
        this.fractal = fractal;
        this.viewer = viewer;
        this.currentField = currentField;
    }
    
    @Override
    public void run() {

        //AFractal frac = fractal.getFractal();
        //frac.c.copy(start);
        ComplexNumber current = new ComplexNumber(start);
        fractal.setConstant(current);
        ComplexNumber step = end.sub(start, new ComplexNumber()).scaleEq(1.0/frames);
        FractalListener copy = fractal.getFractalListener();
        fractal.setFractalListener(null);
        
        final int FRAME_DELAY = 100; // 10 FPS
        try {
            for (int i=0; isAnimating() && i<=frames; i++) {
                while (isPaused()) {
                    try {
                        wait(1000);
                    } catch (Exception e) {}
                    if (!isAnimating())
                        break;
                }
                long t = System.currentTimeMillis();
                viewer.onProgress(i*100 / frames);
                currentField.setText(current.toString());
                fractal.getLastFractalImage().reset();
                fractal.reset(false);
                fractal.setConstant(current);
                fractal.generateFractal();
                if (saveFolder != null) {
                    File file = new File(saveFolder, String.format("anim%03d.png", (i+1)));
                    fractal.saveImage(file, "png");
                }
                fractal.updateFractal();
                fractal.repaint();
                long totalT = System.currentTimeMillis() - t;
                if (totalT < FRAME_DELAY) {
                    try {
                        synchronized (fractal) {
                            fractal.wait(FRAME_DELAY - totalT);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                //frac.c.addEq(step);
                current.addEq(step);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
        fractal.setFractalListener(copy);
        viewer.onAnimationDone(AnimateThread.this);

        System.out.println("DONE ANIMATING");
        state = READY;
        synchronized (this) {
            notify();
        }
    }

    private ComplexNumber start = new ComplexNumber();
    private ComplexNumber end   = new ComplexNumber();

    public void startAnimation(File saveFolder, ComplexNumber start, ComplexNumber end, int numFrames) {
        this.saveFolder = saveFolder;
        this.frames = numFrames;
        startAnimation(start, end);
    }

    
    public void startAnimation(ComplexNumber start, ComplexNumber end) {
        if (isAnimating()) {
            stop();
        } else {
            state = ANIMATING;
            this.start = start;
            this.end = end;
            new Thread(this).start();
        }
    }

    private final int READY = 0;
    private final int ANIMATING = 1;
    private final int PAUSED = 2;
    
    private int state = READY;
    private File saveFolder = null;

    public boolean isAnimating() {
        return state == ANIMATING || state == PAUSED;
    }
    
    public void pause() {
        if (state == ANIMATING)
            state = PAUSED;
    }
    
    public void resume() {
        if (state == PAUSED) {
            state = ANIMATING;
            synchronized (this) {
                notify();
            }
        }
    }
    
    public boolean isPaused() {
        return state == PAUSED;
    }
    
    public void stop() {
        state = READY;;
        synchronized (this) {
            notify();
        }
        fractal.cancel();
    }
}
