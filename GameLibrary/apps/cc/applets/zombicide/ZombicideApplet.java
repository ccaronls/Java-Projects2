package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTButton;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTPanel;
import cc.lib.swing.AWTToggleButton;
import cc.lib.ui.IButton;
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
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
        ZombicideApplet app = instance = new ZombicideApplet() {
            @Override
            <T extends Enum<T>> List<T> getEnumListProperty(String property, Class className, List<T> defaultList) {
                return frame.getEnumListProperty(property, className, defaultList);
            }

            @Override
            String getStringProperty(String property, String defaultValue) {
                return frame.getStringProperty(property, defaultValue);
            }

            @Override
            void setStringProperty(String s, String v) {
                frame.setProperty(s, v);
            }

            @Override
            <T extends Enum<T>> void setEnumListProperty(String s, Collection<T> l) {
                frame.setEnumListProperty(s, l);
            }
        };
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
        ASSIGN,
        SUMMARY,
        OBJECTIVES;

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
            case OBJECTIVES: {
                boardComp.setOverlay(game.getQuest().getObjectivesOverlay(game));
                break;
            }
            case SUMMARY: {
                boardComp.setOverlay(game.getGameSummaryTable());
                break;
            }
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
                    btn.addMouseListener(new MouseListener() {
                        @Override
                        public void mouseClicked(MouseEvent e) {}
                        @Override
                        public void mousePressed(MouseEvent e) {}
                        @Override
                        public void mouseReleased(MouseEvent e) {}
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            boardComp.setOverlay(name.name());
                        }
                        @Override
                        public void mouseExited(MouseEvent e) {
                            boardComp.setOverlay(null);
                        }
                    });
                }
                menu.add(new AWTButton("KEEP", e1 -> {
                    ZUser user = new ZAppletUser();
                    for (Map.Entry<ZPlayerName, AWTToggleButton> entry : buttons.entrySet()) {
                        if (entry.getValue().isSelected())
                            user.addCharacter(entry.getKey().create());
                    }
                    game.setUsers(user);
                    game.reload();
                    setEnumListProperty("players", Utils.filter(buttons.keySet(), object -> buttons.get(object).isSelected()));
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
        setStringProperty("quest", qu.name());
        boardComp.repaint();
        initHomeMenu();
    };

    @Override
    public void init() {
        setLayout(new BorderLayout());
        add(charComp = new CharacterComponent(), BorderLayout.SOUTH);
        ZUser user = new ZAppletUser();
        List<ZPlayerName> players = getEnumListProperty("players", ZPlayerName.class, Utils.toList(ZPlayerName.Baldric, ZPlayerName.Clovis));
        for (ZPlayerName pl : players) {
            user.addCharacter(pl.create());
        }
        game.setUsers(user);
        game.loadQuest(ZQuests.valueOf(getStringProperty("quest", ZQuests.Tutorial.name())));
        menu.setLayout(new GridLayout(0, 1));
        menuContainer.setLayout(new GridBagLayout());
        menuContainer.setPreferredSize(new Dimension(150, 300));
        menuContainer.add(menu);
        add(menuContainer, BorderLayout.LINE_START);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
        initHomeMenu();
    }

    <T extends Enum<T>> List<T> getEnumListProperty(String property, Class enumClass, List<T> defaultList) {
        return defaultList;
    }

    String getStringProperty(String property, String defaultValue) {
        return defaultValue;
    }

    void setStringProperty(String s, String v) {}
    <T extends Enum<T>> void setEnumListProperty(String s, Collection<T> l) {}

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
        ZButton(IButton obj) {
            super(obj);//obj instanceof ZMove ? Utils.getPrettyString(((ZMove) obj).getButtonString()) : Utils.getPrettyString(obj.toString()));
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


    void initMenu() {
        menu.removeAll();
        switch (uiMode) {
            case NONE:
                break;
            case PICK_MENU: {

                for (Object o : options) {
                    menu.add(new ZButton((IButton)o));
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
        menu.add(new AWTButton(MenuItem.SUMMARY.name(), this));
        menu.add(new AWTButton(MenuItem.OBJECTIVES.name(), this));
        menu.add(new AWTButton(MenuItem.QUIT.name(), this));
        menuContainer.validate();
    }

    void setResult(Object result) {
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
        boardComp.requestFocus();
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
