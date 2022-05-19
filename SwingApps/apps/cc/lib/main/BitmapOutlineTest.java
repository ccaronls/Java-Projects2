package cc.lib.main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTImage;
import cc.lib.swing.BlendComposite;
import cc.lib.utils.FileUtils;

public class BitmapOutlineTest extends AWTComponent {

    public static void main(String[] args) {
        Utils.setDebugEnabled();
        new BitmapOutlineTest();
    }

    final AWTFrame frame;

    BitmapOutlineTest() {
        frame = new AWTFrame("Image Rotate Test") {

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
        File file = new File(FileUtils.getOrCreateSettingsDirectory(BitmapOutlineTest.class), "gui.properties");
        frame.setPropertiesFile(file);
    }

    int abomId = 0;
    int outlineid = 0;

    @Override
    protected void init(AWTGraphics g) {
        setMouseEnabled(true);
        abomId = g.loadImage("zabomination.png");
        curBComposite = frame.getIntProperty("curB",0);
        curAComposite = frame.getIntProperty("curA", 0);
        curAComposite2 = frame.getIntProperty("curA2", 0);
        build(g);
    }

    void build(AWTGraphics g) {

        AWTImage img = (AWTImage)g.getImage(abomId);

        float aspect = img.getAspect();
        int width = Math.round(img.getWidth());
        int height = Math.round(img.getHeight());

        // Create a new bitmap that same size as original
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D G = image.createGraphics();
        Composite orig = G.getComposite();

        // render the original image ontop of newimaage, scaled up and in complete white
        //G.setColor(Color.WHITE);
        //G.fillRect(0, 0, image.getWidth(), image.getHeight());
        G.setComposite(allBlend[curBComposite]);
        AffineTransform M = new AffineTransform();
        M.setToIdentity();
        M.translate(width/2, height/2);
        M.scale(1.1f, 1.05f);
        M.translate(-width/2, -height/2);
        G.drawImage(img.getImage(), M, this);
        //G.drawImage(img.getImage(), 0, 0, image.getWidth(), image.getHeight(), this);

        G.setComposite(allAlpha[curAComposite2]);
        G.setColor(Color.WHITE);
        G.fillRect(0, 0, width, height);

        G.setComposite(allAlpha[curAComposite]);
        G.drawImage(img.getImage(), 0,0, img.getImage().getWidth(this), img.getImage().getHeight(this), this);

        G.setComposite(orig);

        outlineid = g.addImage(image);

        /*
        Image filtered = g.getImageMgr().transform(image, new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int rgb) {
                GColor c = GColor.fromRGB(rgb);

                float red=  c.getRed();
                float grn = c.getGreen();
                float blu = c.getBlue();
                float alpha = c.getAlpha();

                if (red == 1 && grn == 1 && blu == 1)
                    return 0;

                return -1;
            }
        });

        targetid = g.addImage(filtered);
*/
        repaint();
    }

    int idx = 0;
    int curBComposite = 0;
    int curAComposite = 0;
    int curAComposite2 = 0;
    int curOutlineColor = 0;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {

        if (outlineid == 0) {
            build(g);
        }

        g.setBackgroundColor(GColor.LIGHT_GRAY);
        g.clearScreen();
        Vector2D center = new Vector2D(getWidth()/2, getHeight()/2);
        g.clearScreen();
        AImage img = g.getImage(outlineid);
        GRectangle rect = new GRectangle(img).withCenter(center);
        g.drawImage(outlineid, center);
        //g.setTransparencyFilter(.5f);
        //g.drawImage(abomId, center);
        //g.removeFilter();
        g.setColor(outlineColors[curOutlineColor]);
        //g.setAlphaComposite(1, AlphaComposite.SRC_IN);
        g.setComposite(BlendComposite.Multiply);
        g.drawFilledRect(rect);

        g.setColor(GColor.RED);
        rect.drawOutlined(g);

        String txt = String.format("Alpha Compisite: %s\nAlpha Compisite2: %s\nBlendComposite: %s",
                alphaRule[allAlpha[curAComposite].getRule()],
                alphaRule[allAlpha[curAComposite2].getRule()],
                allBlend[curBComposite].getMode());

        g.drawJustifiedString(10, 10, txt);

        // Freeze, with Dst out gives desired effect
    }

    @Override
    public synchronized void keyReleased(KeyEvent evt) {
        //curComposite = (curComposite+1) % all.length;
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_A:
                curAComposite = (curAComposite+1) % allAlpha.length;
                frame.setProperty("curA", curAComposite);
                break;
            case KeyEvent.VK_B:
                curBComposite = (curBComposite+1) % allBlend.length;
                frame.setProperty("curB", curBComposite);
                break;
            case KeyEvent.VK_C:
                curAComposite2 = (curAComposite2+1) % allAlpha.length;
                frame.setProperty("curA2", curAComposite2);
                break;
            case KeyEvent.VK_SPACE:
                curOutlineColor = (curOutlineColor+1) % outlineColors.length;
                break;

        }
        outlineid = 0;
        repaint();
    }

    GColor [] outlineColors = {
            GColor.YELLOW, GColor.GREEN, GColor.RED
    };

    BlendComposite [] allBlend = {
            BlendComposite.Add,
            BlendComposite.Average,
            BlendComposite.Blue,
            BlendComposite.Color,
            BlendComposite.ColorBurn,
            BlendComposite.ColorDodge,
            BlendComposite.Darken,
            BlendComposite.Difference,
            BlendComposite.Exclusion,
            BlendComposite.Freeze,
            BlendComposite.Glow,
            BlendComposite.Green,
            BlendComposite.HardLight,
            BlendComposite.Heat,
            BlendComposite.Hue,
            BlendComposite.InverseColorBurn,
            BlendComposite.InverseColorDodge,
            BlendComposite.Lighten,
            BlendComposite.Luminosity,
            BlendComposite.Multiply,
            BlendComposite.Negation,
            BlendComposite.Overlay,
            BlendComposite.Red,
            BlendComposite.Reflect,
            BlendComposite.Saturation,
            BlendComposite.Screen,
            BlendComposite.SoftBurn,
            BlendComposite.SoftDodge,
            BlendComposite.SoftLight,
            BlendComposite.Stamp,
            BlendComposite.Subtract
    };

    AlphaComposite [] allAlpha = {
            AlphaComposite.Clear,
            AlphaComposite.Dst,
            AlphaComposite.DstAtop,
            AlphaComposite.DstIn,
            AlphaComposite.DstOut,
            AlphaComposite.DstOver,
            AlphaComposite.Src,
            AlphaComposite.SrcAtop,
            AlphaComposite.SrcIn,
            AlphaComposite.SrcOut,
            AlphaComposite.SrcOver,
            AlphaComposite.Xor
    };

    String [] alphaRule = {
            "???",
        "Clear",
        "SRC",
        "SrcOver",
        "DstOver",
        "SrcIn",
        "DstIn",
        "SrcOut",
        "DstOUt",
        "Dst",
        "SrcAtop",
        "DstAtop",
        "Xor",
    };
}
