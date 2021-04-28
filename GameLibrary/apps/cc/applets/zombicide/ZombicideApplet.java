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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
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
import cc.lib.zombicide.ZDiffuculty;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZCharacterRenderer;
import cc.lib.zombicide.ui.UIZUser;
import cc.lib.zombicide.ui.UIZombicide;

public class ZombicideApplet extends AWTApplet implements ActionListener {

    static final Logger log = LoggerFactory.getLogger(ZombicideApplet.class);

    static ZombicideApplet instance = null;

    public ZombicideApplet() {
        instance = this;
    }

    @Override
    public URL getAbsoluteURL(String imagePath) throws MalformedURLException {
        return new URL("http://mac-book.local/~chriscaron/Zombicide/" + imagePath);
    }

    public static void main(String [] args) {
        Utils.setDebugEnabled();
        AWTFrame frame = new AWTFrame("Zombicide");
        ZombicideApplet app = new ZombicideApplet() {
            @Override
            public <T extends Enum<T>> List<T> getEnumListProperty(String property, Class className, List<T> defaultList) {
                return frame.getEnumListProperty(property, className, defaultList);
            }

            @Override
            public String getStringProperty(String property, String defaultValue) {
                return frame.getStringProperty(property, defaultValue);
            }

            @Override
            public void setStringProperty(String s, String v) {
                frame.setProperty(s, v);
            }

            @Override
            public <T extends Enum<T>> void setEnumListProperty(String s, Collection<T> l) {
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

    final ZUser user = new UIZUser();
    UIZombicide game;
    AWTPanel menu = new AWTPanel();
    JPanel menuContainer = new JPanel();

    BoardComponent boardComp;
    CharacterComponent charComp;
    File gameFile = null;

    public void onAllImagesLoaded() {
        game = new UIZombicide(new UIZCharacterRenderer(charComp), new UIZBoardRenderer(boardComp)) {

            @Override
            public void runGame() {
                try {
                    super.runGame();
                    charComp.repaint();
                    boardComp.repaint();
                    if (isGameRunning() && gameFile != null) {
                        FileUtils.backupFile(gameFile, 20);
                        game.trySaveToFile(gameFile);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (isGameOver()) {
                    initHomeMenu();
                }
            }

            @Override
            public <T> T waitForUser(Class<T> expectedType) {
                initMenu(getUiMode(), getOptions());
                return super.waitForUser(expectedType);
            }

            @Override
            public void setResult(Object result) {
                super.setResult(result);
                boardComp.requestFocus();
            }
        };
        List<ZPlayerName> players = getEnumListProperty("players", ZPlayerName.class, Utils.toList(ZPlayerName.Baldric, ZPlayerName.Clovis));
        for (ZPlayerName pl : players) {
            user.addCharacter(pl);
        }
        game.setUsers(user);
        try {
            game.loadQuest(ZQuests.valueOf(getStringProperty("quest", ZQuests.Tutorial.name())));
        } catch (Exception e) {
            e.printStackTrace();
            game.loadQuest(ZQuests.Tutorial);
        }
        game.setDifficulty(ZDiffuculty.valueOf(getStringProperty("difficulty", ZDiffuculty.MEDIUM.name())));
        initHomeMenu();
    }

    enum MenuItem {
        START,
        RESUME,
        QUIT,
        BACK,
        LOAD,
        ASSIGN,
        SUMMARY,
        DIFFICULTY,
        OBJECTIVES;

        boolean isHomeButton(ZombicideApplet instance) {
            switch (this) {
                case LOAD:
                case START:
                case ASSIGN:
                case DIFFICULTY:
                    return true;
                case RESUME:
                    return instance.gameFile != null && instance.gameFile.exists();
            }
            return false;
        }

    }

    void initHomeMenu() {
        List<MenuItem> items = Utils.filterItems(object -> object.isHomeButton(this), MenuItem.values());
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
        MenuItem item = MenuItem.valueOf(e.getActionCommand());
        switch (item) {
            case START:
                game.reload();
                game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                game.startGameThread();
                break;
            case RESUME:
                if(game.tryLoadFromFile(gameFile)) {
                    game.startGameThread();
                }
                break;
            case QUIT:
                game.stopGameThread();
                game.setResult(null);
                initHomeMenu();
                break;
            case BACK:
                if (game.isGameRunning()) {
                    game.goBack();
                    game.setResult(null);
                } else {
                    initHomeMenu();
                }
                break;
            case OBJECTIVES: {
                game.boardRenderer.setOverlay(game.getQuest().getObjectivesOverlay(game));
                break;
            }
            case SUMMARY: {
                game.boardRenderer.setOverlay(game.getGameSummaryTable());
                break;
            }
            case LOAD: {
                menu.removeAll();
                for (ZQuests q : ZQuests.values()) {
                    menu.add(new AWTButton(q.name().replace('_', ' '), e12 -> {
                        game.loadQuest(q);
                        setStringProperty("quest", q.name());
                        boardComp.repaint();
                        initHomeMenu();
                    }));
                }
                menu.add(new AWTButton(MenuItem.BACK.name(), this));
                menuContainer.validate();
                break;
            }
            case ASSIGN: {
                menu.removeAll();
                Map<ZPlayerName, AWTToggleButton> buttons = new HashMap<>();
                for (ZPlayerName player : ZPlayerName.values()) {
                    AWTToggleButton btn = new AWTToggleButton(player.name()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // override this since parent class has method that causes our layout to resize badly
                            onToggle(isSelected());
                        }
                    };
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
                            game.boardRenderer.setOverlay(player);
                        }
                        @Override
                        public void mouseExited(MouseEvent e) {
                            game.boardRenderer.setOverlay(null);
                        }
                    });
                }
                menu.add(new AWTButton("KEEP", e1 -> {
                    user.clear();
                    for (Map.Entry<ZPlayerName, AWTToggleButton> entry : buttons.entrySet()) {
                        if (entry.getValue().isSelected())
                            user.addCharacter(entry.getKey());
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
            case DIFFICULTY:
                ZDiffuculty difficulty = (ZDiffuculty) JOptionPane.showInputDialog(this, "Set Difficulty", "DIFFICULTY", JOptionPane.PLAIN_MESSAGE, null,
                        ZDiffuculty.values(), game.getDifficulty());
                if (difficulty != null) {
                    game.setDifficulty(difficulty);
                    setStringProperty("difficulty", difficulty.name());
                }
                break;
            default:
                log.error("Unhandled action: " + e.getActionCommand());
        }
    }

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
        charContainer.setPreferredSize(new Dimension(400, 200));
        charContainer.setMaximumSize(new Dimension(10000, 200));
        add(charContainer, BorderLayout.SOUTH);

        menu.setLayout(new GridLayout(0, 1));
        menuContainer.setLayout(new GridBagLayout());
        menuContainer.setPreferredSize(new Dimension(150, 400));
        menuContainer.add(menu);
        add(menuContainer, BorderLayout.LINE_START);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
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
                    game.setResult(obj);
                }
            }.start();
        }
    }

    void initMenu(UIZombicide.UIMode mode, List options) {
        menu.removeAll();
        switch (mode) {
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
        menu.add(new AWTButton(MenuItem.DIFFICULTY.name(), this));
        menu.add(new AWTButton(MenuItem.QUIT.name(), this));
        menuContainer.validate();
    }

}
