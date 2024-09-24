package cc.experiments;

import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;

public class ProjectionTest extends AWTComponent {

    public static void main(String[] args) {
        Utils.setDebugEnabled();
        new ProjectionTest();
    }

    final AWTFrame frame;

    ProjectionTest() {
        frame = new AWTFrame("Projection Test") {

            @Override
            protected void onWindowClosing() {
                try {
                    //app.figures.saveToFile(app.figuresFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        frame.add(this);
        frame.centerToScreen(600,600);
//        File file = new File(FileUtils.getOrCreateSettingsDirectory(ProjectionTest.class), "gui.properties");
  //      frame.setPropertiesFile(file);
    }


    @Override
    protected void init(AWTGraphics g) {
        setMouseEnabled(true);
    }


    @Override
    protected void paint(AWTGraphics g) {

        GRectangle rect = new GRectangle(new GDimension(4, 4)).withCenter(new Vector2D(-1, -1));
        g.ortho(rect);//-5,0,-5,0);

        g.setColor(GColor.RED);
        g.drawRect(-1, -1, 1, 1);
        g.drawFilledRect(0, 0, 1, 1);

    }
}
