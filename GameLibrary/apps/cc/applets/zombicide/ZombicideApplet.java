package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JSeparator;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
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
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
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
        PICK_DOOR,
        PICK_MENU,
        PICK_ZOMBIE
    }

    enum MenuItem {
        START,
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

    int [] highlightedCell = null;
    Integer highlightedResult = null;
    ZActor highlightedActor = null;
    ZDoor highlightedDoor = null;

    BoardComponent boardComp;
    CharacterComponent charComp;
    boolean gameRunning = false;

    ZombicideApplet() {
        setLayout(new BorderLayout());
        add(charComp = new CharacterComponent(), BorderLayout.SOUTH);
        //menu.setPreferredSize(new Dimension(100, 300));
        menu.setLayout(new AWTButtonLayout(menu));
        add(menu, BorderLayout.WEST);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
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
            highlightedDoor = null;

            int [] cellPos = board.drawDebug(g, getMouseX(), getMouseY());
            ZActor actor = board.drawActors(g, getMouseX(), getMouseY());

            if (gameRunning) {
                g.setColor(GColor.CYAN);
                g.drawJustifiedString(getWidth()-10, getHeight()-10, Justify.RIGHT, Justify.BOTTOM, message);
                switch (uiMode) {
                    case PICK_ZOMBIE:
                    case PICK_CHARACTER: {
                        if (actor != null) {
                            for (int i = 0; i < options.size(); i++) {
                                if (actor == options.get(i)) {
                                    highlightedActor = actor;
                                    highlightedResult = i;
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case PICK_ZONE: {
                        if (cellPos != null) {
                            ZCell cell = board.getCell(cellPos);
                            for (int i = 0; i < options.size(); i++) {
                                if (cell.getZoneIndex() == (Integer)options.get(i)) {
                                    highlightedCell = cellPos;
                                    highlightedResult = i;
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case PICK_DOOR: {
                        g.begin();
                        int index = 0;
                        g.setColor(GColor.CYAN);
                        for (ZDoor door : (List<ZDoor>)options) {
                            g.setName(index++);
                            ZCell cell = board.getCell(door.cellPos);
                            GRectangle doorRect = cell.getWallRect(door.dir).grow(10);
                            g.drawRect(doorRect, 1);
                        }
                        g.end();
                        int picked = g.pickRects(mouseX, mouseY);
                        if (picked >= 0) {
                            highlightedDoor = (ZDoor)options.get(picked);
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
                if (actor != null) {
                    g.setColor(GColor.YELLOW);
                    g.drawRect(actor.getRect());
                    charComp.repaint();
                }
            }
        }

        @Override
        protected void onMousePressed(int mouseX, int mouseY) {
            if (gameRunning) {
                setResult(highlightedResult);
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
            g.getGraphics().setFont(Font.decode(Font.MONOSPACED).deriveFont(Font.BOLD));
            if (highlightedActor != null) {
                g.setColor(GColor.BLACK);
                highlightedActor.drawInfo(g, getWidth(), getHeight());
            } else if (game.getCurrentCharacter() != null) {
                String txt = game.getCurrentCharacter().getDebugString();
                g.setColor(GColor.BLACK);
                g.drawString(txt, 0, 0);
            }
        }

    }

    void startGameThread() {
        if (gameRunning)
            return;

        gameRunning = true;
        new Thread(()-> {
            try {
                while (gameRunning) {
                    game.runGame();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            gameRunning = false;
            setMenuItems(MenuItem.START);
        }).start();
    }

    void stopGameThread() {
        gameRunning = false;
        setResult(null);
        setMenuItems(MenuItem.START);
    }

    void setMenuItems(MenuItem ... items) {
        menu.removeAll();
        for (MenuItem i : items) {
            menu.add(new AWTButton(i.name(), this));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (MenuItem.valueOf(e.getActionCommand())) {
            case START:
                startGameThread();
                break;
            case QUIT:
                stopGameThread();
                break;
            default:
                log.error("Unhandled action: " + e.getActionCommand());
        }
    }

    @Override
    public void init() {
        ZUser user = new ZUser() {

            @Override
            public void showMessage(String s) {
                log.info(s);
            }

            @Override
            public Integer chooseCharacter(List<ZCharacter> characters) {
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

            @Override
            public ZEquipSlot chooseSlotToOrganize(ZGame zGame, ZCharacter cur, List<ZEquipSlot> slots) {
                message = "Choose Slot to Organize";
                uiMode = UIMode.PICK_MENU;
                options = slots;
                return waitForUser();
            }

            @Override
            public ZEquipment chooseEquipment(ZGame zGame, ZCharacter cur, List<ZEquipment> equipOptions) {
                message = "Choose Equipment to Organize";
                uiMode = UIMode.PICK_MENU;
                options = equipOptions;
                return waitForUser();
            }

            @Override
            public ZEquipSlot chooseSlotForEquip(ZGame zGame, ZCharacter cur, List<ZEquipSlot> equipableSlots) {
                message = "Choose Slot to Equip Item";
                uiMode = UIMode.PICK_MENU;
                options = equipableSlots;
                return waitForUser();
            }

            @Override
            public Integer chooseZoneToIgnite(ZGame zGame, ZCharacter cur, List<Integer> zones) {
                message = "Choose zone to Ignite";
                uiMode = UIMode.PICK_ZONE;
                options = zones;
                return waitForUser();
            }

            @Override
            public Integer chooseZoneToWalk(ZGame zGame, ZCharacter cur, List<Integer> zones) {
                message = "Choose zone to Walk";
                uiMode = UIMode.PICK_ZONE;
                options = zones;
                return waitForUser();
            }

            @Override
            public ZDoor chooseDoorToToggle(ZGame zGame, ZCharacter cur, List<ZDoor> doors) {
                message = "Choose door to open or close";
                uiMode = UIMode.PICK_DOOR;
                options = doors;
                return waitForUser();
            }

            @Override
            public ZEquipSlot chooseWeaponSlot(ZGame zGame, ZCharacter c, List<ZEquipSlot> weapons) {
                message = "Choose weapon from slot";
                uiMode = UIMode.PICK_MENU;
                options = weapons;
                return waitForUser();
            }

            @Override
            public ZCharacter chooseTradeCharacter(ZGame zGame, ZCharacter c, List<ZCharacter> list) {
                message = "Choose Character for Trade";
                uiMode = UIMode.PICK_CHARACTER;
                options = list;
                return waitForUser();
            }

            @Override
            public Integer chooseZoneForAttack(ZGame zGame, ZCharacter c, List<Integer> zones) {
                return null;
            }
        };
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
            setResult(obj);
        }
    }

    void initMenu() {
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
        menu.add(new AWTButton(MenuItem.QUIT.name(), this));
        validate();
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
