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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTApplet;
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

public class ZombicideApplet extends AWTApplet implements ActionListener {

    static final Logger log = LoggerFactory.getLogger(ZombicideApplet.class);

    static ZombicideApplet instance = null;

    @Override
    public URL getAbsoluteURL(String imagePath) throws MalformedURLException {
        return new URL("http://mac-book.local/~chriscaron/Zombicide/" + imagePath);
    }

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        AWTFrame frame = new AWTFrame("Zombicide");
        ZombicideApplet app = new ZombicideApplet() {
            @Override
            protected <T extends Enum<T>> List<T> getEnumListProperty(String property, Class className, List<T> defaultList) {
                return frame.getEnumListProperty(property, className, defaultList);
            }

            @Override
            protected String getStringProperty(String property, String defaultValue) {
                return frame.getStringProperty(property, defaultValue);
            }

            @Override
            protected void setStringProperty(String s, String v) {
                frame.setProperty(s, v);
            }

            @Override
            protected <T extends Enum<T>> void setEnumListProperty(String s, Collection<T> l) {
                frame.setEnumListProperty(s, l);
            }
        };
        File settings = FileUtils.getOrCreateSettingsDirectory(ZombicideApplet.class);
        frame.setPropertiesFile(new File(settings, "application.properties"));
        app.gameFile = new File(settings, "savegame.txt");
        frame.add(app);
        app.initApp();
        app.start();
        if (!frame.restoreFromProperties())
            frame.centerToScreen(800, 600);
    }

    public void onAllImagesLoaded() {
        game = new UIZGame();
        ZUser user = new ZAppletUser();
        List<ZPlayerName> players = getEnumListProperty("players", ZPlayerName.class, Utils.toList(ZPlayerName.Baldric, ZPlayerName.Clovis));
        for (ZPlayerName pl : players) {
            user.addCharacter(pl.create());
        }
        game.setUsers(user);
        try {
            game.loadQuest(ZQuests.valueOf(getStringProperty("quest", ZQuests.Tutorial.name())));
        } catch (Exception e) {
            e.printStackTrace();
            game.loadQuest(ZQuests.Tutorial);
        }
        initHomeMenu();
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

    public ZombicideApplet() {
        instance = this;
    }

    ZGame game;
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
                        FileUtils.backupFile(gameFile, 20);
                        game.trySaveToFile(gameFile);
                    }
                    charComp.repaint();
                    boardComp.repaint();
//                    if (gameRunning)
//                        Thread.sleep(5000);
                }
            } catch (Exception e) {
                log.error(e.getClass().getSimpleName() + " " + e.getMessage());
                for (StackTraceElement st : e.getStackTrace()) {
                    log.error(st.toString());
                }
                e.printStackTrace();
            }
            gameRunning = false;
            log.debug("Game thread stopped");
            initHomeMenu();

        }).start();
    }

    void initHomeMenu() {
        List<MenuItem> items = Utils.filterItems(object -> object.isHomeButton(), MenuItem.values());
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
                boardComp.setOverlay(game.getQuest().getObjectivesOverlay(game));
                charComp.clearMessages();
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
                for (ZPlayerName player : ZPlayerName.values()) {
                    AWTToggleButton btn = new AWTToggleButton(player.name());
                    buttons.put(player, btn);
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
                            boardComp.setOverlay(player.name());
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
    protected void initApp() {
        ToolTipManager.sharedInstance().setDismissDelay(30*1000);
        ToolTipManager.sharedInstance().setInitialDelay(0);
//        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
//        for (Font f : fonts) {
//            log.debug("Font: %s:%s", f.getName(), f.getAttributes());
//        }
        //log.info("Fonts=" + Arrays.toString(fonts));

        // For applets:all fonts are: [Arial, Dialog, DialogInput, Monospaced, SansSerif, Serif]

        setLayout(new BorderLayout());
        JScrollPane charContainer = new JScrollPane();
        charContainer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        charContainer.getViewport().add(charComp = new CharacterComponent());
        add(charContainer, BorderLayout.SOUTH);
        menu.setLayout(new GridLayout(0, 1));
        menuContainer.setLayout(new GridBagLayout());
        menuContainer.setPreferredSize(new Dimension(150, 300));
        menuContainer.add(menu);
        add(menuContainer, BorderLayout.LINE_START);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
    }

    <T> T waitForUser(Class<T> expectedType) {
        initMenu();
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (Exception e) {

            }
        }
        uiMode = UIMode.NONE;
        if (result != null && expectedType.isAssignableFrom(result.getClass()))
            return (T)result;
        return null;
    }

    class ZButton extends AWTButton {
        Object obj;
        ZButton(IButton obj) {
            super(obj);
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
        synchronized (boardComp) {
            boardComp.message = message;
            options = characters;
            uiMode = ZombicideApplet.UIMode.PICK_CHARACTER;
            boardComp.repaint();
        }
        return waitForUser(ZCharacter.class);
    }

    public Integer pickZone(String message, List<Integer> zones) {
        synchronized (boardComp) {
            boardComp.message = message;
            options = zones;
            uiMode = ZombicideApplet.UIMode.PICK_ZONE;
            boardComp.repaint();
        }
        return waitForUser(Integer.class);
    }

    public <T> T pickMenu(String message, Class expectedType, List<T> moves) {
        synchronized (boardComp) {
            boardComp.message = message;
            options = moves;
            uiMode = ZombicideApplet.UIMode.PICK_MENU;
        }
        return (T) waitForUser(expectedType);
    }

    public ZDoor pickDoor(String message, List<ZDoor> doors) {
        synchronized (boardComp) {
            boardComp.message = message;
            options = doors;
            uiMode = ZombicideApplet.UIMode.PICK_DOOR;
            boardComp.repaint();
        }
        return waitForUser(ZDoor.class);
    }

}
