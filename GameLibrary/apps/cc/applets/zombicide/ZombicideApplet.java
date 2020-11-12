package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JPanel;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTButton;
import cc.lib.swing.AWTButtonLayout;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTPanel;
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZZombieType;

public class ZombicideApplet extends JApplet implements ActionListener {

    static final Logger log = LoggerFactory.getLogger(ZombicideApplet.class);

    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        //mode = 0;
        AWTFrame frame = new AWTFrame("Zombicide");
        ZombicideApplet app = new ZombicideApplet();
        frame.add(app);
        app.init();
        app.start();
        File settings = FileUtils.getOrCreateSettingsDirectory(ZombicideApplet.class);
        frame.setPropertiesFile(new File(settings, "application.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(800, 600);
    }

    enum UIMode {
        NONE,
        PICK_CHARACTER,
        PICK_ZONE,
        PICK_MENU,
        PICK_ZOMBIE
    }

    ZGame game = new ZGame();
    List options;
    String message = "";
    Object monitor = new Object();
    Object result = null;
    UIMode uiMode = UIMode.NONE;
    AWTPanel menu = new AWTPanel();

    int [] highlightedCell = null;
    ZActor highlightedActor = null;
    BoardComponent boardComp;
    CharacterComponent charComp;

    class BoardComponent extends AWTComponent {

        BoardComponent() {
            setPreferredSize(250, 250);
        }

        @Override
        protected void init(AWTGraphics g) {
            GDimension cellDim = game.board.initCellRects(g, g.getViewportWidth()-5, g.getViewportHeight()-5);
            loadImages(g);
            setMouseEnabled(true);
            //setMinimumSize(256, 256);
            setPreferredSize( (int)cellDim.width * game.board.getColumns(), (int)cellDim.height * game.board.getRows());
            //setMaximumSize( (int)cellDim.width * game.board.getColumns(), (int)cellDim.height * game.board.getRows());
        }

        @Override
        protected void onDimensionChanged(AWTGraphics g, int width, int height) {
            GDimension cellDim = game.board.initCellRects(g, width-5, height-5);
            int newWidth = (int)cellDim.width * game.board.getColumns();
            int newHeight = (int)cellDim.height * game.board.getRows();
            setPreferredSize(newWidth, newHeight);
        }

        @Override
        protected void paint(AWTGraphics g, int mouseX, int mouseY) {
            g.clearScreen(GColor.YELLOW);
            if (game == null || game.board == null)
                return;
            ZBoard board = game.board;
            highlightedCell = board.drawDebug(g, getMouseX(), getMouseY());
            highlightedActor = board.drawActors(g, getMouseX(), getMouseY());
            if (highlightedCell != null) {
                g.setColor(GColor.RED);
                g.drawRect(board.getCell(highlightedCell).getRect());
            }
            if (highlightedActor != null) {
                g.setColor(GColor.YELLOW);
                g.drawRect(highlightedActor.getRect());
                charComp.repaint();
            }
        }

        /*
            if (highlighted != null) {
                if (getKeyboardReset('a')) {
                    board.toggleDorOpen(highlighted, ZBoard.DIR_WEST);
                } else if (getKeyboardReset('w')) {
                    board.toggleDorOpen(highlighted, ZBoard.DIR_NORTH);
                } else if (getKeyboardReset('s')) {
                    board.toggleDorOpen(highlighted, ZBoard.DIR_SOUTH);
                } else if (getKeyboardReset('d')) {
                    board.toggleDorOpen(highlighted, ZBoard.DIR_EAST);
                }

                if (getMouseButtonClicked(0)) {
                    selected = highlighted;
                } else if (selected != null) {
                    g.setColor(GColor.RED);
                    if (board.canSeeCell(selected, highlighted)) {
                        g.setColor(GColor.GREEN);
                    }
                    g.drawRect(board.getCell(selected).getRect());
                }
            }

        }*/
    }

    class CharacterComponent extends AWTComponent {

        CharacterComponent() {
            setPreferredSize(300, 200);
        }

        @Override
        protected void init(AWTGraphics g) {
            setMouseEnabled(true);
            int minHeight = g.getTextHeight() * 12;
            setPreferredSize(minHeight*2, minHeight);
            setMinimumSize(minHeight*2, minHeight);
        }

        @Override
        public synchronized void paint(Graphics g) {
            super.paint(g);
        }

        @Override
        protected void paint(AWTGraphics g, int mouseX, int mouseY) {
            g.clearScreen();
            if (game == null)
                return;
            g.getGraphics().setFont(Font.decode(Font.MONOSPACED));
            if (highlightedActor != null) {
                g.setColor(GColor.BLACK);
                highlightedActor.drawInfo(g, getWidth(), getHeight());
            } else if (game.getCurrentCharacter() != null) {
                String txt = game.getCurrentCharacter().getDebugString();
                g.setColor(GColor.BLACK);
                g.drawString(txt, 0, 0);
            }
        }

        @Override
        public void repaint() {
            super.repaint();
        }

    }

    ZombicideApplet() {
        setLayout(new BorderLayout());
        JPanel center = new JPanel();
        //center.setLayout(new GridLayout(0, 1));
        center.setLayout(new BorderLayout());
        center.add(charComp = new CharacterComponent(), BorderLayout.SOUTH);
        //center.add(new AWTButton("whatapp"), BorderLayout.SOUTH);
        menu.setLayout(new AWTButtonLayout(menu));
        add(menu, BorderLayout.WEST);
        //add(new AWTPanel(0, 1, new AWTButton("Hello")), BorderLayout.EAST);
        center.add(boardComp = new BoardComponent(), BorderLayout.CENTER);
        add(center);
        menu.add(new AWTButton("START", this));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "START":
                break;
            default:
                log.error("Unhandled action: " + e.getActionCommand());
        }
    }

    @Override
    public void init() {
        ZUser user = new ZUser() {
            @Override
            public Integer chooseCharacter(List<Integer> characters) {
                message = "Pick character to play";
                uiMode = UIMode.PICK_CHARACTER;
                options = characters;
                return waitForUser();
            }

            @Override
            public Integer chooseZoneForBile(ZGame zGame, ZCharacter cur, List<Integer> zones) {
                message = "Pick a zone to place the Dragon Bile";
                uiMode = UIMode.PICK_ZONE;
                options = zones;
                return waitForUser();
            }

            @Override
            public ZMove chooseMove(ZGame zGame, ZCharacter cur, List<ZMove> moves) {
                message = "Choose Move";
                uiMode = UIMode.PICK_MENU;
                options = moves;
                return waitForUser();
            }

            @Override
            public ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions) {
                message = "Choose New Skill";
                uiMode = UIMode.PICK_MENU;
                options = skillOptions;
                return waitForUser();
            }
        };
        user.addCharacter(ZPlayerName.Baldric.create());
        user.addCharacter(ZPlayerName.Clovis.create());
        game.setUsers(user);
        game.loadQuest(ZQuests.Tutorial);
    }

    <T> T waitForUser() {
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (Exception e) {

            }
        }
        return (T)result;
    }

    void setResult(Object result) {
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    boolean loaded = false;

    void loadImages(AGraphics g) {
        if (loaded)
            return;
        ZZombieType.Abomination.imageId = g.loadImage("zabomination.png");
        ZZombieType.Necromancer.imageId = g.loadImage("znecro.png");
        ZZombieType.Walker1.imageId = g.loadImage("zwalker1.png");
        ZZombieType.Walker2.imageId = g.loadImage("zwalker2.png");
        ZZombieType.Walker3.imageId = g.loadImage("zwalker3.png");
        ZZombieType.Walker4.imageId = g.loadImage("zwalker4.png");
        ZZombieType.Walker5.imageId = g.loadImage("zwalker5.png");
        ZZombieType.Runner1.imageId = g.loadImage("zrunner1.png");
        ZZombieType.Runner2.imageId = g.loadImage("zrunner1.png");
        ZZombieType.Fatty1.imageId = g.loadImage("zfatty1.png");
        ZZombieType.Fatty2.imageId = g.loadImage("zfatty2.png");
        ZPlayerName.Clovis.imageId = g.loadImage("zchar_clovis.png");
        ZPlayerName.Baldric.imageId = g.loadImage("zchar_baldric.png");
        ZPlayerName.Ann.imageId = g.loadImage("zchar_ann.png");
        ZPlayerName.Nelly.imageId = g.loadImage("zchar_nelly.png");
        ZPlayerName.Samson.imageId = g.loadImage("zchar_samson.png");
        ZPlayerName.Silas.imageId = g.loadImage("zchar_silas.png");
        loaded = true;
    }

}
