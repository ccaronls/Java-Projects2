package cc.lib.main;

import java.awt.event.KeyEvent;
import java.io.File;

import cc.lib.game.AGraphics;
import cc.lib.game.Figure2;
import cc.lib.game.Figures;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.game.Figure2.Part;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.StopWatch;

@SuppressWarnings("serial")
public class AnimationMaker extends AWTKeyboardAnimationApplet {

    public static void main(String[] args) {
        Utils.DEBUG_ENABLED = true;
        final AnimationMaker app = new AnimationMaker();
        AWTFrame frame = new AWTFrame("Animation Maker") {
        	
        	@Override
			protected void onWindowClosing() {
        		try {
        			//app.figures.saveToFile(app.figuresFile);
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
			}

        };
        frame.add(app);
        frame.centerToScreen(600,500);
        app.init();
        app.start();
        app.focusGained(null);
    }
    
    // the current rendered figure
    Figure2 transition = new Figure2();
    Figure2 current = null;
    
    Figures figures = new Figures();
    File figuresFile = new File("figures2.dat");
    
    @Override
    protected void doInitialization() {
    	try {
    		figures.loadFromFile(figuresFile);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	if (figures.getCount() <= 0)
    		figures.add(new Figure2("anim0"));

    	current = figures.get(0);
    }

    long transitionTimeMS = 3000;
    boolean transitioning = false;
    int transitionTarget = 0;
    int menu = 0;
    
    StopWatch sw = new StopWatch();
    Figure2.Value selectedValue = Figure2.Value.BODY_ANGLE;

    float alpha = 1;
    
    Figure2.Render figureRender = new Figure2.Render() {
		
		@Override
		public void drawPart(AGraphics g, Part part, float length, float thickness, float radius, boolean farSide) {
			if (!farSide)
				g.setColor(g.LIGHT_GRAY.setAlpha(alpha));
			else
				g.setColor(g.LIGHT_GRAY.darkened(0.2f));
			g.begin();
			switch (part) {
			case BODY: {
				float x = length/2 - thickness/2;
				g.drawDisk(0, -x, thickness/2);
				g.drawDisk(0,  x, thickness/2);
				g.begin();
	        	g.vertex(-thickness/2, -x);
	        	g.vertex(thickness/2, -x);
	        	g.vertex(thickness/2, x);
	        	g.vertex(-thickness/2, x);
	        	g.drawTriangleFan();
				
	        	break;
			}
			case FOOT:
				g.begin();
	        	g.vertex(-thickness/2, 0);
	        	g.vertex(-thickness/2,length);
	        	g.vertex(thickness/2, length);
	        	g.vertex(thickness/2, 0);
	        	g.drawTriangleFan();
	        	break;
			case LOWER_ARM:
			case LOWER_LEG:
			case UPPER_ARM:
			case UPPER_LEG:
				g.drawDisk(0, 0, thickness/2);
				// fallthrough
			case NECK:
				g.begin();
				g.vertex(-thickness/2, 0);
              	g.vertex(-thickness/2,length);
              	g.vertex(thickness/2, length);
              	g.vertex(thickness/2, 0);
              	g.drawTriangleFan();
	        	break;
			case HAND:
			case HEAD:
	        	g.drawDisk(0, 0, radius);
				break;
			}
			
			g.setColor(g.RED);
			g.setLineWidth(3);
			switch (selectedValue) {
			case ARM_THICKNESS:
			case ARM_OFFSET:
				if (part == Part.UPPER_ARM || part == Part.LOWER_ARM) {
					g.drawLineLoop();
				}
				break;
			case BODY_ANGLE:
			case BODY_LENGTH:
			case BODY_THICKNESS:
				if (part == Part.BODY) {
					g.drawLineLoop();
				}
				break;
			case R_FOOT_ANGLE:
				if (part == Part.FOOT && !farSide) {
					g.drawLineLoop();
				}
				break;
			case L_FOOT_ANGLE:
				if (part == Part.FOOT && farSide) {
					g.drawLineLoop();
					alpha = 0.5f;
				}
				break;
			case FOOT_LENGTH:
			case FOOT_THICKNESS:
				if (part == Part.FOOT) {
					g.drawLineLoop();
				}
				break;
			case HAND_RADIUS:
				if (part == Part.HAND) {
					g.drawCircle(0, 0, radius);
				}
				break;
			case HEAD_ANGLE:
			case HEAD_RADIUS:
				if (part == Part.HEAD) {
					g.drawCircle(0, 0, radius);
				}
				break;
			case LL_ARM_ANGLE:
				if (part == Part.LOWER_ARM && farSide) {
					g.drawLineLoop();
					alpha = 0.5f;
				}
				break;
			case LL_LEG_ANGLE:
				if (part == Part.LOWER_LEG && farSide) {
					g.drawLineLoop();
					alpha = 0.5f;
				}
				break;
			case LU_ARM_ANGLE:
				if (part == Part.UPPER_ARM && farSide) {
					g.drawLineLoop();
					alpha = 0.5f;
				}
				break;
			case LU_LEG_ANGLE:
				if (part == Part.UPPER_LEG && farSide) {
					g.drawLineLoop();
					alpha = 0.5f;
				}
				break;
			case U_ARM_LENGTH:
				if (part == Part.UPPER_ARM) {
					g.drawLineLoop();
				}
				break;
			case L_ARM_LENGTH:
				if (part == Part.LOWER_ARM) {
					g.drawLineLoop();
				}
				break;
			case L_HAND_ANGLE:
				if (part == Part.HAND && farSide) {
					g.drawCircle(0, 0, radius);
					alpha = 0.5f;
				}
				break;
			case U_LEG_LENGTH:
			case U_LEG_THICKNESS:
				if (part == Part.UPPER_LEG) {
					g.drawLineLoop();
				}
				break;

			case L_LEG_LENGTH:
			case L_LEG_THICKNESS:
				if (part == Part.LOWER_LEG) {
					g.drawLineLoop();
				}
				break;
			case NECK_ANGLE:
			case NECK_LENGTH:
			case NECK_RADIUS:
				if (part == Part.NECK) {
					g.drawLineLoop();
				}
				break;
			case RL_ARM_ANGLE:
				if (part == Part.LOWER_ARM && !farSide) {
					g.drawLineLoop();
				}
				break;
			case RL_LEG_ANGLE:
				if (part == Part.LOWER_LEG && !farSide) {
					g.drawLineLoop();
				}
				break;
			case RU_ARM_ANGLE:
				if (part == Part.UPPER_ARM && !farSide) {
					g.drawLineLoop();
				}
				break;
			case RU_LEG_ANGLE:
				if (part == Part.UPPER_LEG && !farSide) {
					g.drawLineLoop();
				}
				break;
			case R_HAND_ANGLE:
				if (part == Part.HAND && !farSide) {
					g.drawCircle(0, 0, radius);
				}
				break;
			
			}
		}
	};
    
    int menuWidth = 0;
    
    @Override
    protected void drawFrame(AWTGraphics g) {
    	final int dom = 15;
    	alpha = 1;
    	g.ortho(-dom, dom, -dom, dom);
    	g.clearScreen(g.BLACK.lightened(0.1f));
    	
    	g.clearMinMax();
    	current.draw(g, figureRender);
    	Vector2D min = g.getMinBoundingRect();
    	Vector2D max = g.getMaxBoundingRect();
    	
    	g.setColor(g.WHITE);
    	g.drawRect(min.X(), min.Y(), max.X()-min.X(), max.Y()-min.Y());
    	
    	g.ortho();
    
    	int x = 10;
    	int y = 10;
    	
    	for (Figure2.Value value : Figure2.Value.values()) {
    		float wid = g.getTextWidth(value.name());
    		if (wid > menuWidth)
    			menuWidth = Math.round(wid);
    		if (value == selectedValue) {
    			g.setColor(g.BLUE);
    			g.drawFilledRect(x, y, menuWidth, g.getTextHeight());
    		}
    		g.setColor(g.WHITE);
    		g.drawJustifiedString(x, y, value.name());
    		y += g.getTextHeight();
    	}
    	
    	
    	if (menu == 0) {
    		g.setColor(g.BLUE);
    		g.drawRect(10, 10, menuWidth, y-10);
    	}
    	
    	g.setColor(g.RED);
    	String instructions = "+/- : Adjust Limb\n"
    						+ "a/z : Adjust Limb\n"
    						+ "W   : Write\n"
    						+ "L   : Load\n"
    			            + "N   : New Figure\n";
    	if (transitioning) {
    		instructions += "S   : Stop\n"
    				      + "R   : Restart\n";
    		if (sw.isPaused()) {
    			instructions += "P   - Unpause\n";
    		} else {
    			instructions += "P   - Pause\n";
    		}
    	}
    	else
    		instructions += "T - Transition";
    	g.drawJustifiedString(10, g.getViewportHeight()-20, Justify.LEFT, Justify.BOTTOM, instructions);

		x = g.getViewportWidth()-20;
		y = 10;
		
    	for (int i=0; i<figures.getCount(); i++) {
    		if (i == transitionTarget) {
    			g.setColor(g.BLUE);
    			g.drawFilledRect(x-40, y, 40, g.getTextHeight());
    		}
    		g.setColor(g.WHITE);
    		g.drawJustifiedString(x, y, Justify.RIGHT, figures.get(i).getName());
    		y += g.getTextHeight();
    	}
    	
    	g.setColor(g.WHITE);
    	g.drawJustifiedString(this.getWidth()/2, 10, Justify.CENTER, current.getName());
    	
    	if (menu == 1) {
    		g.setColor(g.BLUE);
    		g.drawRect(g.getViewportWidth()-60, 10, 40, y-10);
    	}
    	
    	if (getKeyboardReset('w')) {
    		try {
    			figures.saveToFile(figuresFile);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	
    	if (getKeyboardReset('l')) {
    		current = figures.get(transitionTarget);
    	}
    	if (getKeyboardReset('n')) {
    		Figure2 newFigure = current.deepCopy();
    		newFigure.setName("anim" + figures.getCount());
    		figures.add(newFigure);
    	}
    	
    	if (transitioning) {
    		sw.capture();
    		float dt = sw.getTime();
    		if (dt > transitionTimeMS) {
    			dt = transitionTimeMS;
    			transitioning = false;
    		}
    		transition.transitionTo(figures.get(transitionTarget), dt/transitionTimeMS, current);
    		if (getKeyboardReset('s')) {
    			transitioning = false;
    		}
    		if (!transitioning) {
    			current = figures.get(transitionTarget);    			
    		} else if (getKeyboardReset('p')) {
    			if (sw.isPaused())
    				sw.unpause();
    			else
    				sw.pause();
    		}
    	} else {
    		if (getKeyboardReset('t')) {
    			transitioning = true;
    			sw.start();
    			transition = current.deepCopy();
    			current = current.deepCopy();
    			current.setName(transition.getName() + " -> " + figures.get(transitionTarget).getName());
    		}
    	}
    	
    	if (this.getKeyboard('=') || getKeyboard('a')) {
    		adjustFig(1);
    	} else if (this.getKeyboard('-') || getKeyboard('z')) {
    		adjustFig(-1);
    	}
    }

    private void adjustFig(float i) {
    	float min = -360;
    	float max = 360;
    	switch (selectedValue) {
		case ARM_OFFSET:
		case ARM_THICKNESS:
		case BODY_LENGTH:
		case BODY_THICKNESS:
		case FOOT_LENGTH:
		case FOOT_THICKNESS:
		case HAND_RADIUS:
		case HEAD_RADIUS:
		case L_ARM_LENGTH:
		case L_LEG_LENGTH:
		case L_LEG_THICKNESS:
		case NECK_LENGTH:
		case NECK_RADIUS:
		case U_ARM_LENGTH:
		case U_LEG_LENGTH:
		case U_LEG_THICKNESS:
			i = i/10;
			min = 0.1f;
			max = 10;
			break;
    	}
    	
    	float value = current.getValue(selectedValue)+i;
    	value += i;
    	value = Math.max(min,  value);
    	value = Math.min(max, value);
    	current.setValue(selectedValue, value);
/*    	
    	
    	
    	switch (selectedValue) {
    		case BODY_ANGLE: current.setBodyAngle(current.getBodyAngle()+i); break;
    		case HEAD_ANGLE: current.setHeadAngle(current.getHeadAngle()+i); break;
    		case LU_ARM_ANGLE: current.getUpperArmsAngle()[0] += i; break;
    		case RU_ARM_ANGLE: current.getUpperArmsAngle()[1] += i; break;
    		case LL_ARM_ANGLE: current.getLowerArmsAngle()[0] += i; break;
    		case RL_ARM_ANGLE: current.getLowerArmsAngle()[1] += i; break;
    		case LU_LEG_ANGLE: current.getUpperLegsAngle()[0] += i; break;
    		case RU_LEG_ANGLE: current.getUpperLegsAngle()[1] += i; break;
    		case LL_LEG_ANGLE: current.getLowerLegsAngle()[0] += i; break;
    		case RL_LEG_ANGLE: current.getLowerLegsAngle()[1] += i; break;
    		case L_FOOT_ANGLE: current.getFootAngle()[0] += i; break;
    		case R_FOOT_ANGLE: current.getFootAngle()[1] += i; break;    	
    	}*/
	}

	@Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        // TODO Auto-generated method stub
        
    }

	@Override
	public void keyPressed(KeyEvent ev) {
		switch (ev.getKeyCode()) {
		case KeyEvent.VK_DOWN:
			if (menu == 0) {
				int n = (selectedValue.ordinal() + 1) % Figure2.Value.values().length;
				selectedValue = Figure2.Value.values()[n];
			} else {
				transitionTarget = (transitionTarget+1) % figures.getCount();
			}
			break;
		case KeyEvent.VK_UP:
			if (menu == 0) {
				int n = (selectedValue.ordinal() + Figure2.Value.values().length - 1) % Figure2.Value.values().length;
				selectedValue = Figure2.Value.values()[n];
			} else {
				transitionTarget = (transitionTarget+figures.getCount()-1) % figures.getCount();
			}
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:
			menu = (menu+1) % 2;
			break;
		default:
			super.keyPressed(ev);
		}
	}

}
