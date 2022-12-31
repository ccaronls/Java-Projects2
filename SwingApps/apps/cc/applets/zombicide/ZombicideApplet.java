package cc.applets.zombicide;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
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
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDifficulty;
import cc.lib.zombicide.ZGame;
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
        ZGame.DEBUG = true;
        frame = new AWTFrame("Zombicide");
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

            @Override
            public void setIntProperty(String s, int value) {
                frame.setProperty(s, String.valueOf(value));
            }

            @Override
            public int getIntProperty(String s, int defaultValue) {
                return frame.getIntProperty(s, defaultValue);
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
    AWTPanel menu = new AWTPanel() {
        @Override
        public Component add(Component comp) {
            comp.setMinimumSize(new Dimension(140, 40));
            comp.setMaximumSize(new Dimension(140, 400));
            ((JComponent)comp).setAlignmentX(LEFT_ALIGNMENT);
            return super.add(comp);
        }
    };
    AWTPanel menuContainer = new AWTPanel();
    BoardComponent boardComp;
    CharacterComponent charComp;
    File gameFile = null;
    static AWTFrame frame;

    public void onAllImagesLoaded() {
        UIZBoardRenderer renderer = new UIZBoardRenderer(boardComp) {
            @Override
            protected void drawActor(AGraphics g, ZActor actor, GColor outline) {
                if (actor.isAlive() && actor.getOutlineImageId() > 0) {
                    // for AWT to need to render the outline in white fist otherwise the tinting looks messed up
                    g.drawImage(actor.getOutlineImageId(), actor.getRect());
                }
                super.drawActor(g, actor, outline);
            }
        };

        game = new UIZombicide(new UIZCharacterRenderer(charComp), renderer) {

            @Override
            public boolean runGame() {
                boolean changed = false;
                try {
                    changed = super.runGame();
                    charComp.repaint();
                    boardComp.repaint();
                    if (isGameRunning() && changed && gameFile != null) {
                        FileUtils.backupFile(gameFile, 100);
                        game.trySaveToFile(gameFile);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopGameThread();
                    initHomeMenu();
                    boardRenderer.setOverlay("Error: " + e.getMessage());
                }

                if (isGameOver()) {
                    stopGameThread();
                    initHomeMenu();
                }

                return changed;
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

            @Override
            public ZUser getThisUser() {
                return user;
            }
        };
        try {
            game.loadQuest(ZQuests.valueOf(getStringProperty("quest", ZQuests.Tutorial.name())));
        } catch (Exception e) {
            e.printStackTrace();
            game.loadQuest(ZQuests.Tutorial);
        }
        user.setColor(0);
        List<ZPlayerName> players = getEnumListProperty("players", ZPlayerName.class, Utils.toList(ZPlayerName.Baldric, ZPlayerName.Clovis));
        for (ZPlayerName pl : players) {
            game.addCharacter(pl);
            user.addCharacter(pl);
        }
        game.setUsers(user);
        game.setDifficulty(ZDifficulty.valueOf(getStringProperty("difficulty", ZDifficulty.MEDIUM.name())));
        initHomeMenu();
    }

    enum MenuItem {
        START,
        RESUME,
        QUIT,
        CANCEL,
        LOAD,
        ASSIGN,
        SUMMARY,
        DIFFICULTY,
        UNDO,
        OBJECTIVES;

        boolean isHomeButton(ZombicideApplet instance) {
            switch (this) {
                case LOAD:
                case START:
                case ASSIGN:
                case DIFFICULTY:
                case UNDO:
                    return true;
                case RESUME:
                    return instance.gameFile != null && instance.gameFile.exists();
            }
            return false;
        }

    }

    void initHomeMenu() {
        List<MenuItem> items = Utils.filter(MenuItem.values(), object -> object.isHomeButton(this));
        setMenuItems(items);
        frame.setTitle("Zombicide: " + game.getQuest().getName());
    }

    synchronized void setMenuItems(List<MenuItem> items) {
        if (SwingUtilities.isEventDispatchThread()) {
            menu.removeAll();
            for (MenuItem i : items) {
                menu.add(new AWTButton(i.name(), this));
            }
            menuContainer.revalidate();
        } else {
            EventQueue.invokeLater(() -> setMenuItems(items));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuItem item = MenuItem.valueOf(e.getActionCommand());
        game.boardRenderer.setOverlay(null);
        switch (item) {
            case START:
                game.reload();
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
            case CANCEL:
                if (game.isGameRunning()) {
                    game.setResult(null);
                } else {
                    initHomeMenu();
                }
                break;
            case OBJECTIVES: {
                game.showObjectivesOverlay();
                break;
            }
            case SUMMARY: {
                game.showSummaryOverlay();
                break;
            }
            case LOAD: {
                menu.removeAll();
                for (ZQuests q : ZQuests.values()) {
                    menu.add(new AWTButton(q, action -> {
                        game.loadQuest(q);
                        setStringProperty("quest", q.name());
                        boardComp.repaint();
                        initHomeMenu();
                    }));
                }
                menu.add(new AWTButton(MenuItem.CANCEL.name(), this));
                menuContainer.revalidate();
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
                    game.clearCharacters();
                    game.clearUsersCharacters();
                    for (Map.Entry<ZPlayerName, AWTToggleButton> entry : buttons.entrySet()) {
                        if (entry.getValue().isSelected()) {
                            game.addCharacter(entry.getKey());
                            user.addCharacter(entry.getKey());
                        }
                    }
                    game.reload();
                    setEnumListProperty("players", Utils.filter(buttons.keySet(), object -> buttons.get(object).isSelected()));
                    initHomeMenu();
                    boardComp.repaint();
                }));
                menu.add(new AWTButton(MenuItem.CANCEL.name(), this));
                menuContainer.revalidate();
                break;
            }
            case DIFFICULTY:
                ZDifficulty difficulty = (ZDifficulty) JOptionPane.showInputDialog(this, "Set Difficulty", "DIFFICULTY", JOptionPane.PLAIN_MESSAGE, null,
                        ZDifficulty.values(), game.getDifficulty());
                if (difficulty != null) {
                    game.setDifficulty(difficulty);
                    setStringProperty("difficulty", difficulty.name());
                }
                break;
            case UNDO:
                if (FileUtils.restoreFile(gameFile)) {
                    game.tryLoadFromFile(gameFile);
                    boardComp.repaint();
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
        // For applets:all fonts are: [Arial, Dialog, DialogInput, Monospaced, SansSerif, Serif]

        setLayout(new BorderLayout());
        JScrollPane charScrollContainer = new JScrollPane();
        JScrollPane menuScrollContainer = new JScrollPane();
        charScrollContainer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        menuScrollContainer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        charScrollContainer.getViewport().add(charComp = new CharacterComponent());
        charScrollContainer.setPreferredSize(new Dimension(400, 200));
        charScrollContainer.setMaximumSize(new Dimension(10000, 200));
        add(charScrollContainer, BorderLayout.SOUTH);

        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menuContainer.setLayout(new GridBagLayout());
        menuScrollContainer.setPreferredSize(new Dimension(150, 400));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuContainer.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {
                if (game != null) {
                    game.boardRenderer.setHighlightedActor(null);
                    game.characterRenderer.redraw();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {}
        });
        menuContainer.setMinimumSize(new Dimension(150, 400));

        menuScrollContainer.getViewport().add(menuContainer);
        add(menuScrollContainer, BorderLayout.LINE_START);
        add(boardComp = new BoardComponent(), BorderLayout.CENTER);
        frame.addWindowListener(boardComp);
        charComp.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {
                if (game != null && game.getCurrentCharacter() != null)
                    setCharacterSkillsOverlay(game.getCurrentCharacter().getCharacter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                boardComp.renderer.setOverlay(null);
            }
        });
    }

    void setCharacterSkillsOverlay(ZCharacter c) {
        boardComp.renderer.setOverlay(c.getSkillsTable());
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

    void initMenu(UIZombicide.UIMode mode, List _options) {
        menu.removeAll();
        List options = new ArrayList(_options);
        boardComp.initKeysPresses(options);
        switch (mode) {
            case NONE:
                break;
            case PICK_CHARACTER:
            case PICK_MENU: {
                for (Object o : options) {
                    menu.add(new ZButton((IButton)o));
                }

                break;
            }
            case PICK_ZONE:
            case PICK_SPAWN:
            case PICK_ZOMBIE:
            case PICK_DOOR:
                break;
        }

        JComponent sep = new JSeparator();
        //sep.setMaximumSize(new Dimension(140, 32));
        //Dimension d = sep.getPreferredSize();
        //d.height = 32;
        //sep.setPreferredSize(d);
        menu.add(sep, null);
        menu.add(new AWTButton(MenuItem.CANCEL.name(), this));
        menu.add(new AWTButton(MenuItem.SUMMARY.name(), this));
        menu.add(new AWTButton(MenuItem.OBJECTIVES.name(), this));
        menu.add(new AWTButton(MenuItem.DIFFICULTY.name(), this));
        menu.add(new AWTButton(MenuItem.QUIT.name(), this));
        menuContainer.revalidate();
    }

}
