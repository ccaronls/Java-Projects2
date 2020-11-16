package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTButton;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTPanel;
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZCharacter;
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
    Object monitor = new Object();
    Object result = null;
    UIMode uiMode = UIMode.NONE;
    AWTPanel menu = new AWTPanel();

    BoardComponent boardComp;
    CharacterComponent charComp;
    boolean gameRunning = false;
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
    public void actionPerformed(ActionEvent e) {
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
        uiMode = UIMode.NONE;
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
        validate();
    }

    void setResult(Object result) {
        if (gameRunning) {
            this.result = result;
            synchronized (monitor) {
                monitor.notify();
            }
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
