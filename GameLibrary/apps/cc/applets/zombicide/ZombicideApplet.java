package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTButton;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTPanel;
import cc.lib.swing.AWTToggleButton;
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZUser;

public class ZombicideApplet extends JApplet implements ActionListener {

    static final Logger log = LoggerFactory.getLogger(ZombicideApplet.class);

    static ZombicideApplet instance = null;

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        //mode = 0;
        AWTFrame frame = new AWTFrame("Zombicide");
        ZombicideApplet app = instance = new ZombicideApplet();
        File settings = FileUtils.getOrCreateSettingsDirectory(ZombicideApplet.class);
        frame.setPropertiesFile(new File(settings, "application.properties"));
        app.gameFile = new File(settings, "savegame.txt");
        frame.add(app);
        app.init();
        app.start();
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
        BACK,
        LOAD,
        ASSIGN;

        boolean isHomeButton() {
            switch (this) {
                case LOAD:
                case START:
                case ASSIGN:
                    return true;
                case RESUME:
                    return instance.gameFile != null && instance.gameFile.exists();
            }
            return false;
        }
    }

    ZGame game = new ZGame();
    List options = Collections.emptyList();
    Object monitor = new Object();
    Object result = null;
    UIMode uiMode = UIMode.NONE;
    AWTPanel menu = new AWTPanel();
    JPanel menuContainer = new JPanel();

    BoardComponent boardComp;
    CharacterComponent charComp;
    boolean gameRunning = false;
    File gameFile = null;

    ZombicideApplet() {
    }

    synchronized void startGameThread() {
        if (gameRunning)
            return;

        gameRunning = true;
        new Thread(()-> {
            try {
                charComp.repaint();
                boardComp.repaint();
                while (gameRunning && !game.isGameOver()) {
                    game.runGame();
                    if (gameRunning && gameFile != null) {
                        FileUtils.backupFile(gameFile, 5);
                        game.trySaveToFile(gameFile);
                    }
                    charComp.repaint();
                    boardComp.repaint();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            gameRunning = false;
            log.debug("Game thread stopped");
            initHomeMenu();

        }).start();
    }

    void initHomeMenu() {
        List<MenuItem> items = Utils.filterItems(new Utils.Filter<MenuItem>() {
            @Override
            public boolean keep(MenuItem object) {
                return object.isHomeButton();
            }
        }, MenuItem.values());
        setMenuItems(items);
    }

    synchronized void setMenuItems(List<MenuItem> items) {
        if (SwingUtilities.isEventDispatchThread()) {
            menu.removeAll();
            for (MenuItem i : items) {
                menu.add(new AWTButton(i.name(), this));
            }
            menuContainer.validate();
        } else {
            EventQueue.invokeLater(() -> setMenuItems(items));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (MenuItem.valueOf(e.getActionCommand())) {
            case START:
                game.reload();
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
                if (gameRunning) {
                    game.goBack();
                    setResult(null);
                } else {
                    initHomeMenu();
                }
                break;
            case LOAD: {
                menu.removeAll();
                for (ZQuests q : ZQuests.values()) {
                    menu.add(new AWTButton(q.name(), questLoader));
                }
                menu.add(new AWTButton(MenuItem.BACK.name(), this));
                menuContainer.validate();
                break;
            }
            case ASSIGN: {
                menu.removeAll();
                Map<ZPlayerName, AWTToggleButton> buttons = new HashMap<>();
                for (ZPlayerName name : ZPlayerName.values()) {
                    AWTToggleButton btn = new AWTToggleButton(name.name());
                    buttons.put(name, btn);
                    menu.add(btn);
                }
                menu.add(new AWTButton("KEEP", e1 -> {
                    ZUser user = new ZAppletUser();
                    for (Map.Entry<ZPlayerName, AWTToggleButton> entry : buttons.entrySet()) {
                        if (entry.getValue().isSelected())
                            user.addCharacter(entry.getKey().create());
                    }
                    game.setUsers(user);
                    game.reload();
                    initHomeMenu();
                    boardComp.repaint();
                }));
                menu.add(new AWTButton(MenuItem.BACK.name(), this));
                menuContainer.validate();
                break;
            }
            default:
                log.error("Unhandled action: " + e.getActionCommand());
        }
    }

    ActionListener questLoader = e -> {
        ZQuests qu = ZQuests.valueOf(e.getActionCommand());
        game.loadQuest(qu);
        boardComp.repaint();
        initHomeMenu();
    };

    @Override
    public void init() {
        setLayout(new BorderLayout());
        add(charComp = new CharacterComponent(), BorderLayout.SOUTH);
        //menu.setMinimumSize(new Dimension(200, 0));
        //menu.setLayout(new AWTButtonLayout(menu));
        menu.setLayout(new GridLayout(0, 1));
        menuContainer.setLayout(new GridBagLayout());
        menuContainer.setPreferredSize(new Dimension(150, 300));
        menuContainer.add(menu);
        add(menuContainer, BorderLayout.LINE_START);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
        initHomeMenu();

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
        uiMode = UIMode.NONE;
        return (T)result;
    }

    class ZButton extends AWTButton {
        Object obj;
        ZButton(Object obj) {
            super(obj instanceof ZMove ? ((ZMove) obj).getButtonString() : obj.toString());
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

        @Override
        public Action getAction() {
            return super.getAction();
        }
    }

    void addDirectionArrowButtons() {
        ZCharacter cur = game.getCurrentCharacter();
        if (cur == null)
            return;
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        JPanel mid = new JPanel();
        mid.setLayout(new GridLayout(0, 1));
        top.add(mid, BorderLayout.CENTER);
        ZButton button = new ZButton(ZMove.newWalkDirMove(ZDir.WEST));
        top.add(button, BorderLayout.WEST);
        if (game.board.canMove(cur, ZDir.WEST)) {
            InputMap inputMap = top.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), button.getAction());
        } else {
            button.setEnabled(false);
        }
        button = new ZButton(ZMove.newWalkDirMove(ZDir.NORTH));
        mid.add(button);
        if (game.board.canMove(cur, ZDir.NORTH)) {
            InputMap inputMap = top.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), button.getAction());
        } else {
            button.setEnabled(false);
        }
        button = new ZButton(ZMove.newWalkDirMove(ZDir.EAST));
        top.add(button, BorderLayout.EAST);
        if (game.board.canMove(cur, ZDir.EAST)) {
            InputMap inputMap = top.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), button.getAction());
        } else {
            button.setEnabled(false);
        }
        button = new ZButton(ZMove.newWalkDirMove(ZDir.SOUTH));
        mid.add(button);
        if (game.board.canMove(cur, ZDir.SOUTH)) {
            InputMap inputMap = top.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), button.getAction());
        } else {
            button.setEnabled(false);
        }
        menu.add(top);
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
        Dimension d = sep.getPreferredSize();
        d.height = 32;
        sep.setPreferredSize(d);
        menu.add(sep);
        if (game.canGoBack()) {
            menu.add(new AWTButton(MenuItem.BACK.name(), this));
        }
        menu.add(new AWTButton(MenuItem.QUIT.name(), this));
        menuContainer.validate();
    }

    void setResult(Object result) {
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public ZCharacter pickCharacter(String message, List<ZCharacter> characters) {
        boardComp.message = message;
        options = characters;
        uiMode = ZombicideApplet.UIMode.PICK_CHARACTER;
        return waitForUser();
    }

    public Integer pickZone(String message, List<Integer> zones) {
        boardComp.message = message;
        options = zones;
        uiMode = ZombicideApplet.UIMode.PICK_ZONE;
        return waitForUser();
    }

    public <T> T pickMenu(String message, List<T> moves) {
        boardComp.message = message;
        options = moves;
        uiMode = ZombicideApplet.UIMode.PICK_MENU;
        return waitForUser();
    }

    public ZDoor pickDoor(String message, List<ZDoor> doors) {
        boardComp.message = message;
        options = doors;
        uiMode = ZombicideApplet.UIMode.PICK_DOOR;
        return waitForUser();
    }

}
