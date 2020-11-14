package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTButton;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTPanel;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Grid;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZZombieType;

public class ZombicideApplet extends JApplet implements ActionListener, ZTiles {

    static final Logger log = LoggerFactory.getLogger(ZombicideApplet.class);

    static ZombicideApplet instance = null;

    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        //mode = 0;
        AWTFrame frame = new AWTFrame("Zombicide");
        ZombicideApplet app = instance = new ZombicideApplet();
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
        PICK_DOOR,
        PICK_MENU,
        PICK_ZOMBIE
    }

    enum MenuItem {
        START,
        RESUME,
        QUIT,
        BACK
    }

    ZGame game = new ZGame();
    List options;
    String message = "";
    Object monitor = new Object();
    Object result = null;
    UIMode uiMode = UIMode.NONE;
    AWTPanel menu = new AWTPanel();

    Grid.Pos highlightedCell = null;
    Object highlightedResult = null;
    ZActor highlightedActor = null;

    BoardComponent boardComp;
    CharacterComponent charComp;
    boolean gameRunning = false;
    LinkedList<String> messages = new LinkedList<>();
    File gameFile = null;

    ZombicideApplet() {
        setLayout(new BorderLayout());
        add(charComp = new CharacterComponent(), BorderLayout.SOUTH);
        //menu.setMinimumSize(new Dimension(200, 0));
        //menu.setLayout(new AWTButtonLayout(menu));
        menu.setLayout(new GridLayout(0, 1));
        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new GridBagLayout());
        menuContainer.setPreferredSize(new Dimension(150, 300));
        menuContainer.add(menu);
        add(menuContainer, BorderLayout.LINE_START);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
        File settings = FileUtils.getOrCreateSettingsDirectory(ZombicideApplet.class);
        gameFile = new File(settings, "savegame.txt");
        if (gameFile.exists())
            setMenuItems(MenuItem.START, MenuItem.RESUME);
        else
            setMenuItems(MenuItem.START);
    }

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
            if (game == null || game.board == null)
                return;
            ZBoard board = game.board;
            highlightedActor = null;
            highlightedCell = null;
            highlightedResult = null;

            Grid.Pos cellPos = board.drawDebug(g, getMouseX(), getMouseY());

            if (gameRunning) {
                //game.getQuest().drawTiles(g, ZombicideApplet.this);

                int highlightedZone = board.drawZones(g, getMouseX(), getMouseY());
                highlightedActor = board.drawActors(g, getMouseX(), getMouseY());

                g.setColor(GColor.BLACK);
                g.drawJustifiedString(getWidth()-10, getHeight()-10, Justify.RIGHT, Justify.BOTTOM, message);
                switch (uiMode) {
                    case PICK_ZOMBIE:
                    case PICK_CHARACTER: {
                        if (options.contains(highlightedActor)) {
                            highlightedResult = highlightedActor;
                        }
                        break;
                    }
                    case PICK_ZONE: {
                        if (highlightedZone >= 0 && options.contains(highlightedZone)) {
                            highlightedResult = highlightedZone;
                            g.setColor(GColor.YELLOW);
                            board.drawZoneOutline(g, highlightedZone);
                        } else if (cellPos != null) {
                            ZCell cell = board.getCell(cellPos);
                            for (int i = 0; i < options.size(); i++) {
                                if (cell.getZoneIndex() == (Integer)options.get(i)) {
                                    highlightedCell = cellPos;
                                    highlightedResult = cell.getZoneIndex();
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case PICK_DOOR: {
                        for (ZDoor door : (List<ZDoor>)options) {
                            ZCell cell = board.getCell(door.cellPos);
                            GRectangle doorRect = cell.getWallRect(door.dir).grownBy(10);
                            if (doorRect.contains(mouseX, mouseY)) {
                                g.setColor(GColor.RED);
                                highlightedResult = door;
                            } else {
                                g.setColor(GColor.CYAN);
                            }
                            g.drawRect(doorRect, 1);
                        }
                        break;
                    }
                }
                if (highlightedCell != null) {
                    ZCell cell = board.getCell(highlightedCell);
                    g.setColor(GColor.RED.withAlpha(32));
                    board.drawZoneOutline(g, cell.getZoneIndex());
                }
                if (highlightedActor != null) {
                    g.setColor(GColor.YELLOW);
                    g.drawRect(highlightedActor.getRect());
                    charComp.repaint();
                }

            } else {
                if (cellPos != null) {
                    ZCell cell = board.getCell(cellPos);
                    g.setColor(GColor.RED.withAlpha(32));
                    board.drawZoneOutline(g, cell.getZoneIndex());
                    g.setColor(GColor.RED);
                    g.drawRect(cell.getRect());
                }
            }
        }

        @Override

        protected void onMousePressed(int mouseX, int mouseY) {
            if (gameRunning) {
                setResult(highlightedResult);
            }
        }
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
            g.getGraphics().setFont(Font.decode(Font.MONOSPACED).deriveFont(Font.BOLD));
            if (highlightedActor != null) {
                g.setColor(GColor.BLACK);
                highlightedActor.drawInfo(g, getWidth(), getHeight());
            } else if (game.getCurrentCharacter() != null) {
                String txt = game.getCurrentCharacter().getDebugString();
                g.setColor(GColor.BLACK);
                g.drawString(txt, 0, 0);
            }

            g.setColor(GColor.BLACK);
            int y = 0;
            for (String msg : messages) {
                g.drawJustifiedString(g.getViewportWidth(), y, Justify.RIGHT, Justify.TOP, msg);
                g.setColor(GColor.TRANSLUSCENT_BLACK);
                y += g.getTextHeight();
            }
        }

    }



    synchronized void startGameThread() {
        if (gameRunning)
            return;

        gameRunning = true;
        new Thread(()-> {
            try {
                while (gameRunning) {
                    game.runGame();
                    if (gameFile != null)
                        game.trySaveToFile(gameFile);
                    charComp.repaint();
                    boardComp.repaint();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            gameRunning = false;
            log.debug("Game thread stopped");
            setMenuItems(MenuItem.START);

        }).start();
    }

    synchronized void setMenuItems(MenuItem ... items) {
        menu.removeAll();
        for (MenuItem i : items) {
            menu.add(new AWTButton(i.name(), this));
        }
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        switch (MenuItem.valueOf(e.getActionCommand())) {
            case START:
                startGameThread();
                break;
            case RESUME:
                if(game.tryLoadFromFile(gameFile)) {
                    startGameThread();
                }
                break;
            case QUIT:
                gameRunning = false;
                setResult(null);
                break;
            case BACK:
                game.goBack();
                setResult(null);
                break;
            default:
                log.error("Unhandled action: " + e.getActionCommand());
        }
    }

    @Override
    public void init() {
        ZUser user = new ZAppletUser();
        user.addCharacter(ZPlayerName.Baldric.create());
        user.addCharacter(ZPlayerName.Clovis.create());
        game.setUsers(user);
        game.loadQuest(ZQuests.Tutorial);
    }

    <T> T waitForUser() {
        initMenu();
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (Exception e) {

            }
        }
        return (T)result;
    }

    class ZButton extends AWTButton {
        Object obj;
        ZButton(Object obj) {
            super(obj instanceof ZMove ? ((ZMove) obj).type.name() : obj.toString());
            this.obj = obj;
        }

        @Override
        protected void onAction() {
            new Thread() {
                public void run() {
                    setResult(obj);
                }
            }.start();
        }
    }

    synchronized void initMenu() {
        menu.removeAll();
        switch (uiMode) {
            case NONE:
                break;
            case PICK_MENU: {
                for (Object o : options) {
                    menu.add(new ZButton(o));
                }

                break;
            }
            case PICK_ZONE:
            case PICK_ZOMBIE:
            case PICK_CHARACTER:
            case PICK_DOOR:
        }

        JComponent sep = new JSeparator();
        Dimension d = sep.getPreferredSize();
        d.height = 32;
        sep.setPreferredSize(d);
        menu.add(sep);
        if (game.canGoBack()) {
            menu.add(new AWTButton(MenuItem.BACK.name(), this));
        }
        menu.add(new AWTButton(MenuItem.QUIT.name(), this));
        validate();
    }

    synchronized void setResult(Object result) {
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    boolean loaded = false;

    void loadImages(AWTGraphics g) {
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

    int [] tiles = new int[0];

    @Override
    public int[] loadTiles(AGraphics _g, String[] names, int[] orientations) {
        AWTGraphics g = (AWTGraphics)_g;
        for (int t : tiles) {
            g.deleteImage(t);
        }

        tiles = new int[names.length];

        for (int i=0; i<names.length; i++) {
            switch (names[i]) {
                case "4V":
                    tiles[i] = g.loadImage("ztile1.png", orientations[i]);
                    break;
                case "9R":
                    tiles[i] = g.loadImage("ztile8.png", orientations[i]);
                    break;
            }

        }
        return tiles;
    }

    void addMessage(String msg) {
        messages.addFirst(msg);
        while (messages.size() > 32) {
            messages.removeLast();
        }
        charComp.repaint();
    }

    public ZCharacter pickCharacter(String message, List<ZCharacter> characters) {
        this.message = message;
        uiMode = ZombicideApplet.UIMode.PICK_CHARACTER;
        options = characters;
        return waitForUser();
    }

    public Integer pickZone(String message, List<Integer> zones) {
        this.message = message;
        uiMode = ZombicideApplet.UIMode.PICK_ZONE;
        options = zones;
        return waitForUser();
    }

    public <T> T pickMenu(String message, List<T> moves) {
        this.message = message;
        uiMode = ZombicideApplet.UIMode.PICK_MENU;
        options = moves;
        return waitForUser();
    }

    public ZDoor pickDoor(String message, List<ZDoor> doors) {
        this.message = message;
        uiMode = ZombicideApplet.UIMode.PICK_DOOR;
        options = doors;
        return waitForUser();
    }

}
