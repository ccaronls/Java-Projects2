package cc.game.soc.swing2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;

import javax.swing.ToolTipManager;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneLayout;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import cc.game.soc.android.R;
import cc.game.soc.core.*;
import cc.game.soc.ui.*;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.net.GameServer;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTRadioButtonGroup;
import cc.lib.swing.AWTUtils;
import cc.lib.swing.ButtonLayout;
import cc.lib.swing.EZFrame;
import cc.lib.swing.EZPanel;
import cc.lib.swing.ImageColorFilter;
import cc.lib.swing.ImageMgr;
import cc.lib.swing.JPanelStack;
import cc.lib.swing.JWrapLabel;
import cc.lib.utils.FileUtils;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;

public class GUI implements ActionListener, MenuItem.Action {

	final static String PROP_AI_TUNING_ENABLED = "aituning.enable";
	final static String PROP_SCENARIOS_DIR = "scenariosDirectory";
	final static String PROP_BOARDS_DIR = "boardsDirectory";
	
    final Logger log = LoggerFactory.getLogger(GUI.class);
    
    static final File HOME_FOLDER = new File(System.getProperty("user.home") + "/.soc");
    static final File AI_TUNING_FILE = new File("assets/aituning.properties");

    public final MenuItem QUIT = new MenuItem("Quit", "Quit current game", this);
    public final MenuItem BACK = new MenuItem("Back", null, this);
    public final MenuItem EXIT = new MenuItem("Exit", "Exit the Application", this);
    public final MenuItem NEW_GAME = new MenuItem("New Game", "Start a new game", this);
    public final MenuItem RESTORE = new MenuItem("Restore", "Restore previously saved game", this);
    public final MenuItem CONFIG_BOARD = new MenuItem("Configure Board", "Open configure board mode", this);
    public final MenuItem CONFIG_SETTINGS = new MenuItem("Config Settings", "Configure game settings", this);
    public final MenuItem CHOOSE_NUM_PLAYERS = new MenuItem("--", null, this);
    public final MenuItem CHOOSE_COLOR = new MenuItem("--", null, this);
    public final MenuItem START = new MenuItem("Start", "Start the game", this);
    public final MenuItem RESTART = new MenuItem("Restart", "Start the game", this);
    public final MenuItem START_MULTIPLAYER = new MenuItem("Start MP", "Start game and wait for players to join", this);
    public final MenuItem GEN_HEX_BOARD = new MenuItem("New Hexagon", "Generate a hexagon shaped board", this);
    public final MenuItem GEN_HEX_BOARD_SMALL = new MenuItem("Small", "Generate a small hexagon shaped board", this);
    public final MenuItem GEN_HEX_BOARD_MEDIUM = new MenuItem("Medium", "Generate a medium hexagon shaped board", this);
    public final MenuItem GEN_HEX_BOARD_LARGE = new MenuItem("Large", "Generate a large hexagon shaped board", this);
    public final MenuItem GEN_RECT_BOARD = new MenuItem("New Rectangle", "Generate a rectangular shaped board", this);
    public final MenuItem GEN_RECT_BOARD_SMALL = new MenuItem("Small", "Generate a small rectangular shaped board", this);
    public final MenuItem GEN_RECT_BOARD_MEDIUM = new MenuItem("Medium", "Generate a medium rectangular shaped board", this);
    public final MenuItem GEN_RECT_BOARD_LARGE = new MenuItem("Large", "Generate a large shaped board", this);
    public final MenuItem TRIM_BOARD = new MenuItem("Trim Board", "Remove empty tiles", this);
    public final MenuItem ASSIGN_RANDOM = new MenuItem("Assign Random", "Assign values to the random tiles", this);
    public final MenuItem SAVE_BOARD_AS_DEFAULT = new MenuItem("Save as Default", "Save current board as default board", this);
    public final MenuItem LOAD_DEFAULT = new MenuItem("Load Default", "Load the default board", this);
    public final MenuItem SAVE_BOARD = new MenuItem("Save", "Overwrite board changes", this);
    public final MenuItem SAVE_BOARD_AS = new MenuItem("Save as New", "Save as a new board", this);
    public final MenuItem LOAD_BOARD = new MenuItem("Load Board", "Load a board", this);
    public final MenuItem SAVE_SCENARIO = new MenuItem("Save Scenario", "Save the current board and game configuration as a scenario", this);
    public final MenuItem LOAD_SCENARIO = new MenuItem("Load Scenario", "Load a current scenario board and game configuration", this);

    // Debugging
    public final MenuItem COMPUTE_DISTANCES = new MenuItem("Compute Distances", "Dump distance info to the console", this);
    public final MenuItem LOAD_DEBUG = new MenuItem("Load Debug", "Load the Debugging Board", this);
    public final MenuItem SAVE_DEBUG = new MenuItem("Save Debug", "Save existing board as Debug", this);
    public final MenuItem AITUNING_NEXT_OPTIMAL_INDEX = new MenuItem("Next Path", "Select the next path for the optimal path", this);
    public final MenuItem AITUNING_PREV_OPTIMAL_INDEX = new MenuItem("Prev Path", "Select the previous path for the optimal path", this);
    public final MenuItem AITUNING_ACCEPT_OPTIMAL = new MenuItem("Accept", "Accept the current path", this);
    public final MenuItem AITUNING_REFRESH = new MenuItem("Refresh", "Refresh aiTuning properties from test area", this);
    public final MenuItem DEBUG_BOARD = new MenuItem("Debug Board", "Open board debugging screen", this);
    public final MenuItem RESET_BOARD = new MenuItem("Reset Board", "Clear current board of structures and routes", this);
    public final MenuItem RESET_BOARD_ISLANDS = new MenuItem("Reset Islands", "Remove island ", this);
    public final MenuItem REWIND_GAME = new MenuItem("Rewind Game", "Rewind the game to previous state", this);
    public final MenuItem SHOW_RULES = new MenuItem("Rules", "Display the Rules", this);
    public final MenuItem BUILDABLES_POPUP = new MenuItem("Buildables", "Show the buildables popup", this);

	public static void main(String [] args) throws Exception {
        UIManager.LookAndFeelInfo [] all = UIManager.getInstalledLookAndFeels();
        HashMap<String, String> lafs = new HashMap<>();
        for (int i=0; i<all.length; i++) {
            lafs.put(all[i].getName(), all[i].getClassName());
        }
        UIManager.setLookAndFeel(lafs.get("Metal"));
        EZFrame frame = new EZFrame();
        frame.setTitle("Senators of Coran");
		try {
			PlayerBot.DEBUG_ENABLED = true;
			Utils.setDebugEnabled(true);
            AGraphics.DEBUG_ENABLED = true;
			UIProperties props = new UIProperties();
			if (!HOME_FOLDER.exists()) {
				if (!HOME_FOLDER.mkdir()) {
					throw new RuntimeException("Cannot create home folder: " + HOME_FOLDER);
				}
			} else if (!HOME_FOLDER.isDirectory()) {
				throw new RuntimeException("Not a directory: " + HOME_FOLDER);
			}
			File propsFile = new File(HOME_FOLDER, "gui.properties");
            props.load(propsFile.getAbsolutePath());
            new GUI(frame, props);
            System.out.println(props.toString().replace(",", "\n"));
            if (!frame.loadFromFile(propsFile)) {
                frame.centerToScreen(640, 480);
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private enum MenuState {

	    MENU_START,
        MENU_START_MP,
	    MENU_CHOOSE_NUM_PLAYERS,
	    MENU_CHOOSE_COLOR,
	    MENU_GAME_SETUP,
	    MENU_PLAY_GAME,
	    MENU_CONFIG_BOARD,
	    MENU_CHOOSE_DEFAULT_BOARD_SIZE,
	    MENU_CHOOSE_CUSTOM_BOARD_SIZE, 
	    MENU_DEBUGGING,
        MENU_REWIND,
	}

    private JmDNS jmdns = null;
    private final UISOC soc;
	private final JPanel menu = new JPanel();

	private final SOCComponent consoleComponent = new SOCComponent() {
        @Override
        protected void init(AWTGraphics g) {
            setMouseEnabled(true);
        }

        @Override
        public void onMouseWheel(int rotation) {
            console.scroll(rotation);
        }
    };
	private final UIConsoleRenderer console = new UIConsoleRenderer(consoleComponent);
	private final SOCComponent boardComp = new SOCComponent() {

        @Override
        protected Object [][] getImagesToLoad() {
            Object [][] assets = {
                    { "desert.GIF", GColor.WHITE },
                    { "water.GIF",  GColor.WHITE },
                    { "gold.GIF", null },
                    { "undiscoveredtile.GIF", null },
                    { "foresthex.GIF", null },
                    { "hillshex.GIF", null },
                    { "mountainshex.GIF", null },
                    { "pastureshex.GIF", null },
                    { "fieldshex.GIF", null },
                    { "knight_basic_inactive.GIF", null },
                    { "knight_basic_active.GIF", null },
                    { "knight_strong_inactive.GIF", null },
                    { "knight_strong_active.GIF", null },
                    { "knight_mighty_inactive.GIF", null },
                    { "knight_mighty_active.GIF", null },
                    { "cardFrame.GIF", GColor.WHITE },
            };
            return assets;
        }

        @Override
        protected void init(final AWTGraphics g) {
            super.init(g);
            setMouseEnabled(true);
            setMinimumSize(256, 256);
            boardRenderer.renderFlag = getProps().getIntProperty("renderFlag", 0);
        }

        @Override
        protected void onImagesLoaded(int [] ids) {
            boardRenderer.initImages(ids[0], ids[1], ids[2], ids[3],
                    ids[4], ids[5], ids[6], ids[7], ids[8], ids[9],
                    ids[10], ids[11], ids[12], ids[13], ids[14], ids[15]);

        }

        @Override
        protected void onMouseWheel(int rotation) {
            if (boardRenderer.getPickHandler() != null && (boardRenderer.getPickHandler() instanceof MyCustomPickHandler)) {
                ((MyCustomPickHandler)boardRenderer.getPickHandler()).onMouseWheel(rotation);
            }
        }
    };
    private final SOCComponent barbarianComp = new SOCComponent() {

        @Override
        protected Object [][] getImagesToLoad() {
            Object[][] assets = {
                    {"barbarians_tile.GIF", null},
                    {"barbarians_piece.GIF", null},
            };
            return assets;
        }
        @Override
        protected void onImagesLoaded(int [] ids) {
            barbarianRenderer.initAssets(ids[0], ids[1]);
        }
    };

    class DiceComponent extends SOCComponent {

        @Override
        protected void init(final AWTGraphics g) {
            setMinimumSize(30, 30);
            setPreferredSize(60, 30);
            new Thread() {
                public void run() {
                    int [] ids = new int[4];
                    ids[0] = g.loadImage("dicesideship2.GIF");
                    progress = 0.25f;
                    int cityId = g.loadImage("dicesidecity2.GIF");
                    ImageMgr mgr = g.getImageMgr();
                    Image i = mgr.getImage(cityId);
                    ids[1] = mgr.addImage(mgr.transform(i, new ImageColorFilter(Color.WHITE, Color.RED)));
                    progress = 0.5f;
                    ids[2] = mgr.addImage(mgr.transform(i, new ImageColorFilter(Color.WHITE, Color.GREEN)));
                    progress = 0.75f;
                    ids[3] = mgr.addImage(mgr.transform(i, new ImageColorFilter(Color.WHITE, Color.BLUE)));

                    mgr.deleteImage(cityId);

                    diceRenderers.initImages(ids[0], ids[1], ids[2], ids[3]);
                    progress = 1;
                }
            }.start();
        }
    };

    private final DiceComponent diceComponent = new DiceComponent();

    private final SOCComponent [] playerComponents = {
            new SOCComponent(),
            new SOCComponent(),
            new SOCComponent(),
            new SOCComponent(),
            new SOCComponent(),
            new SOCComponent(),
            new SOCComponent(),
            new SOCComponent()
    };
    private final DiceComponent eventCardComp = new DiceComponent();
    private final UIBarbarianRenderer barbarianRenderer = new UIBarbarianRenderer(barbarianComp);
    private final UIPlayerRenderer [] playerRenderers = {
            new UIPlayerRenderer(playerComponents[0]),
            new UIPlayerRenderer(playerComponents[1]),
            new UIPlayerRenderer(playerComponents[2]),
            new UIPlayerRenderer(playerComponents[3]),
            new UIPlayerRenderer(playerComponents[4]),
            new UIPlayerRenderer(playerComponents[5]),
            new UIPlayerRenderer(playerComponents[6]),
            new UIPlayerRenderer(playerComponents[7]),
    };
    private final UIBoardRenderer boardRenderer = new UIBoardRenderer(boardComp);
    
    private final UIDiceRenderer diceRenderers = new UIDiceRenderer(diceComponent, true);
    private final UIEventCardRenderer eventCardRenderer = new UIEventCardRenderer(eventCardComp);

	private final Stack<MenuState> menuStack = new Stack<>();
	private UIPlayerUser localPlayer;
	private final EZFrame frame;
	private JFrame popup;
	private final JPanel westBorderPanel = new JPanel();
	private final JPanel cntrBorderPanel = new JPanel();
	
	private final JPanel eastGridPanel = new JPanel();
	private final JPanel westGridPanel = new JPanel();
	private JSpinner playerChooser;
	private final JPanelStack middleLeftPanel = new JPanelStack();
	private final JWrapLabel helpText = new JWrapLabel();
    private final Properties aiTuning = new Properties();
	
	final static class ColorString {
		final GColor color;
		final String name;
		
		ColorString(GColor color, String name) {
			this.color = color;
			this.name = name;
		}
	}
	
	private final ColorString [] playerColors;

    private File defaultBoardFile;
    private final File saveGameFile;
    private final File saveRulesFile;
    private final File debugBoard;
    
    private final JLabel boardNameLabel = new JLabel("Untitled");
    
    Board getBoard() {
    	return soc.getBoard();
    }
    
    Rules getRules() {
    	return soc.getRules();
    }
    
    private void clearMenu() {
    	menu.removeAll();
    }
    private final UIProperties props;

    private final Map<Integer, String> stringTable;

	public GUI(final EZFrame frame, final UIProperties props) throws IOException {
		this.frame = frame;
		this.props = props;
		this.stringTable = Utils.buildStringsTable(R.string.class, "../SOCAndroid/res/values/strings.xml");
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
		soc = new UISOC(playerRenderers, boardRenderer, diceRenderers, console, eventCardRenderer, barbarianRenderer) {
            @Override
            protected void addMenuItem(MenuItem item, String title, String helpText, Object object) {
                GUI.this.addMenuItem(getMenuOpButton(item, title, helpText, object));
            }

            @Override
            public void clearMenu() {
                menu.removeAll();
            }

            @Override
            public void redraw() {
                frame.repaint();
            }

            @Override
            protected String getServerName() {
                return System.getProperty("user.name");
            }

            @Override
            protected void showOkPopup(String title, String message) {
                JOptionPane.showMessageDialog(frame, message, title, JOptionPane.PLAIN_MESSAGE);
            }

            @Override
            public void completeMenu() {

                {
                    JComponent sep = new JSeparator();
                    Dimension d = sep.getPreferredSize();
                    d.height = 32;
                    sep.setPreferredSize(d);
                    menu.add(sep);
                }
                if (canCancel()) {
                    addMenuItem(CANCEL);
                } else {
                    menu.add(new JLabel(""));
                }
                boolean aiTuningEnabled = getProps().getBooleanProperty(PROP_AI_TUNING_ENABLED, false);
                JToggleButton tuneAI = new JToggleButton("AI Tuning");
                tuneAI.setSelected(aiTuningEnabled);
                tuneAI.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getProps().setProperty(PROP_AI_TUNING_ENABLED, ((JToggleButton)e.getSource()).isSelected());
                    }
                });
                menu.add(tuneAI);
                menu.add(getMenuOpButton(SHOW_RULES));
                menu.add(getMenuOpButton(BUILDABLES_POPUP));
                menu.add(getMenuOpButton(REWIND_GAME));
                menu.add(getMenuOpButton(QUIT));
                helpText.setText(soc.getHelpText());
                frame.validate();
            }

            @Override
            public BotNode chooseOptimalPath(BotNode optimal, final List<BotNode> leafs) {

                if (getProps().getBooleanProperty(PROP_AI_TUNING_ENABLED, false) == false)
                    return optimal;

                if (!soc.isRunning())
                    return optimal;

                final int [] leftPanelOffset = new int[1];

                final HashMap<String, Double> maxValues= new HashMap<String, Double>();
                int maxKeyWidth = 0;
                for (BotNode n : leafs) {
                    for (String key : n.getKeys()) {
                        maxKeyWidth = Math.max(maxKeyWidth, key.length());
                        double v = n.getValue(key);
                        if (maxValues.containsKey(key)) {
                            maxValues.put(key, Math.max(v, maxValues.get(key)));
                        } else {
                            maxValues.put(key,  v);
                        }
                    }
                }

                clearMenu();

                //menu.add(getMenuOpButton(MenuItem.NEXT_OPTIMAL_INDEX));
                //menu.add(getMenuOpButton(MenuItem.PREV_OPTIMAL_INDEX));
                menu.add(getMenuOpButton(AITUNING_ACCEPT_OPTIMAL));
                final OpButton refresh = getMenuOpButton(AITUNING_REFRESH);
                menu.add(refresh);

                if (optimal != null)
                    optimalIndex = leafs.indexOf(optimal);
                else
                    optimal = leafs.get(optimalIndex);
                optimalOptions = leafs;

                APGraphics g = boardComp.getAPGraphics();

                final int fontHeight = g.getTextHeight();
                int ypos = -leftPanelOffset[0] * fontHeight;
                final NodeRect [] nodeRects = new NodeRect[leafs.size()];
                initNodeRectsArray(g, leafs, nodeRects, ypos);

                final int padding = 2;

                final int maxKeyWidthf = maxKeyWidth;
                String optimalInfo = getBotNodeDetails(optimal, maxKeyWidth, maxValues);
                final JTextArea nodeArea = new JTextArea();
                nodeArea.addKeyListener(new KeyListener() {

                    @Override
                    public void keyTyped(KeyEvent e) {}

                    @Override
                    public void keyReleased(KeyEvent e) {}

                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                            refresh.doClick();
                            e.consume();
                        }
                    }
                });
                JPanel area = middleLeftPanel.push();
                JScrollPane pane = new JScrollPane();
                pane.getViewport().add(nodeArea);
                area.add(pane);

                nodeArea.setFont(Font.decode("courier-plain-10"));
                nodeArea.setText(optimalInfo.toString());
                completeMenu();

                MyCustomPickHandler handler = new MyCustomPickHandler() {

                    int lastHighlighted = -1;

                    @Override
                    public void onMouseWheel(int clicks) {
                        leftPanelOffset[0] = Math.max(0, leftPanelOffset[0]+clicks);
                        int ypos = -leftPanelOffset[0] * fontHeight;
                        initNodeRectsArray(g, leafs, nodeRects, ypos);
                        boardComp.repaint();
                    }

                    @Override
                    public void onPick(UIBoardRenderer bc, int pickedValue) {
                        BotNode n = leafs.get(pickedValue);

                        if (n.getData() instanceof Vertex) {
                            Vertex v = (Vertex)n.getData();
                            v.setPlayerAndType(getCurPlayerNum(), VertexType.SETTLEMENT);
                            IDistances d = getBoard().computeDistances(getRules(), getCurPlayerNum());
                            console.addText(GColor.BLACK, d.toString());
                            v.setOpen();
                        }

                        if (n.getData() instanceof Route) {
                            Route r = (Route)n.getData();
                            //r.setType(RouteType.SHIP);
                            getBoard().setPlayerForRoute(r, getCurPlayerNum(), RouteType.SHIP);
                            IDistances d = getBoard().computeDistances(getRules(), getCurPlayerNum());
                            console.addText(GColor.BLACK, d.toString());
                            getBoard().setRouteOpen(r);
                        }

                        // rewrite the aituning properties (to the text pane, user must visually inspect and commit) such that the picked botnode becomes the most dominant.
                        // there will be cases when this is not possible, in which case, algorithm will need additional factors introduced to give favor to the node we want to 'win'
                        final BotNode best = leafs.get(0);
                        final double delta = best.getValue() - n.getValue();
                        if (delta > 0) {
                            double deltaPos = 0;
                            double deltaNeg = 0;
                            Properties propsToChange = new Properties();
                            for (String key : best.getKeys()) {
                                double factor = AITuning.getInstance().getScalingFactor(key);
                                double b = factor * best.getValue(key);
                                double t = factor * n.getValue(key);
                                double dt = b-t;
                                if (dt > 0) {
                                    deltaPos += dt;
                                } else if (dt < 0) {
                                    // capture those variable where 'n' beats 'best'; these will be scaled up to make 'n' win
                                    deltaNeg -= dt;
                                    propsToChange.setProperty(key, "");
                                } // else ignore
                            }
                            if (deltaNeg > 0 && deltaPos > 0 && propsToChange.size() > 0) {
                                String str = "";
                                double newScale = deltaPos / deltaNeg;
                                newScale = Math.floor(newScale+1); // we want > than, not =

                                for (Object key : propsToChange.keySet()) {
                                    double f = AITuning.getInstance().getScalingFactor((String)key);
                                    double fnew = f*newScale;
                                    str += String.format("%-20s %f\n", key, fnew);
                                    propsToChange.setProperty((String)key, String.valueOf(fnew));
                                }
                                int opt = JOptionPane.showConfirmDialog(frame, "Confirm changes\n\n" + str);
                                if (opt == JOptionPane.YES_OPTION) {
                                    aiTuning.putAll(propsToChange);
                                    try {
                                        FileOutputStream out = new FileOutputStream(AI_TUNING_FILE);
                                        try {
                                            aiTuning.store(out, "Generated by SOC Swing Utility");
                                        } finally {
                                            out.close();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                console.addText(GColor.BLACK, "Node has no max values");
                            }
                        }
                        soc.notifyWaitObj();
                    }

                    @Override
                    public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {

                        if (lastHighlighted != highlightedIndex) {
                            BotNode node = leafs.get(highlightedIndex);
                            String info = getBotNodeDetails(node, maxKeyWidthf, maxValues);
                            nodeArea.setText(info);
                        }

                        lastHighlighted = highlightedIndex;

                        BotNode node = leafs.get(highlightedIndex);
                        onDrawPickable(bc, g, highlightedIndex);
                        NodeRect nr = nodeRects[highlightedIndex];
                        String text = node.getDescription();
                        while (node.getParent() != null) {
                            node = node.getParent();
                            text = node.getDescription() + " => " + text;
                        }
                        text = "" + highlightedIndex + ": " + text;
;
                        node = leafs.get(highlightedIndex);
                        String info = String.format("%.6f\n", node.getValue()) + text;
                        g.pushMatrix();
                        g.setIdentity();
                        g.setColor(GColor.RED);
                        g.drawJustifiedStringOnBackground(nr.r.x, nr.r.y, Justify.LEFT, Justify.TOP, nr.s, GColor.TRANSLUSCENT_BLACK, 2);
                        g.setColor(GColor.YELLOW);
                        g.drawWrapStringOnBackground(nr.r.x+nr.r.w, nr.r.y, g.getViewportWidth()/2, info, GColor.TRANSLUSCENT_BLACK, 2);
                        g.popMatrix();

                        g.setColor(GColor.BLACK);
                        MutableVector2D v = new MutableVector2D();
                        g.begin();
                        while (node.getParent() != null) {
                            v.set(node.getBoardPosition(getBoard()));
                            if (!v.isZero()) {
                                g.vertex(v);
                            }
                            node = node.getParent();
                        }
                        g.drawPoints(15);
                    }

                    @Override
                    public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
                        NodeRect nr = nodeRects[index];
                        g.setColor(GColor.YELLOW);
                        g.pushMatrix();
                        g.setIdentity();
                        g.drawJustifiedStringOnBackground(nr.r.x, nr.r.y, Justify.LEFT, Justify.TOP, nr.s, GColor.TRANSLUSCENT_BLACK, 2);
                        g.popMatrix();

                    }

                    @Override
                    public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {}

                    @Override
                    public boolean isPickableIndex(UIBoardRenderer bc, int index) {
                        return true;
                    }

                    @Override
                    public PickMode getPickMode() {
                        return PickMode.PM_CUSTOM;
                    }

                    @Override
                    public int getNumElements() {
                        return nodeRects.length;
                    }

                    @Override
                    public int pickElement(UIBoardRenderer b, APGraphics g, int x, int y) {
                        for (int i=0; i<nodeRects.length; i++) {
                            int dy = i * g.getTextHeight();
                            NodeRect nr = nodeRects[i];
                            if (nodeRects[i].r.contains(x, y))
                                return i;
                        }
                        return -1;
                    }

                };
                boardRenderer.setPickHandler(handler);
                BotNode result = waitForReturnValue(null);
                middleLeftPanel.pop();
                boardRenderer.setPickHandler(null);
                return result;
            }

            @Override
            protected void onShouldSaveGame() {
                FileUtils.backupFile(saveGameFile.getAbsolutePath(), 20);
                soc.trySaveToFile(saveGameFile);
            }

            @Override
            protected void onRunError(Throwable e) {
                super.onRunError(e);
                quitToMainMenu();
            }

            @Override
            public String getString(int resourceId, Object... args) {
                String s = stringTable.get(resourceId);
                if (s == null) {
                    log.error("Unknown string resource '" + resourceId + "'");
                }
                try {
                    return String.format(s, args);
                } catch (MissingFormatArgumentException e) {
                    log.error("Missing format argument for line:\n" + s);
                    throw e;
                }
            }

            @Override
            public boolean isAITuningEnabled() {
                return getProps().getBooleanProperty(PROP_AI_TUNING_ENABLED, false);
            }

            @Override
            protected String showChoicePopup(String title, List<String> choices) {
                int index = frame.showItemChooserDialog(title, "If you cancel from this dialog you will be disconnected from game", choices.toArray(new String[choices.size()]));
                if (index >= 0) {
                    return choices.get(index);
                }
                return null;
            }
        };
		
		String boardFilename = props.getProperty("gui.defaultBoardFilename", "soc_def_board.txt");
        defaultBoardFile = new File(HOME_FOLDER, boardFilename);
        if (!defaultBoardFile.exists()) {
        	defaultBoardFile = new File(boardFilename);
        }
        saveGameFile = new File(HOME_FOLDER, props.getProperty("gui.saveGameFileName", "socsavegame.txt"));
        saveRulesFile = new File(HOME_FOLDER, props.getProperty("gui.saveRulesFileName", "socrules.txt"));
        debugBoard = new File("boards/debug_board.txt");
        
        if (saveRulesFile.exists()) {
        	getRules().loadFromFile(saveRulesFile);
        }
        
        menuStack.push(MenuState.MENU_START);
		if (!loadBoard(defaultBoardFile)) {
			getBoard().generateDefaultBoard();
			saveBoard(defaultBoardFile);
		}
        playerColors = new ColorString[] {
        		new ColorString(GColor.RED, "Red"),
        		new ColorString(GColor.GREEN.darkened(0.5f), "Green"),
        		new ColorString(GColor.BLUE,"Blue"),
        		new ColorString(GColor.ORANGE.darkened(0.1f), "Orange"),
        		new ColorString(GColor.MAGENTA, "Magenta"),
        		new ColorString(GColor.PINK, "Pink")
        };

        playerChooser = new JSpinner(new SpinnerNumberModel(props.getIntProperty("debug.playerNum",  1), 1, playerColors.length, 1));
        playerChooser.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				props.setProperty("debug.playerNum",  (Integer)playerChooser.getValue());
			}
		});
		
        String scenario = getProps().getProperty("scenario");
        if (scenario != null) {
        	try {
        		loadGame(new File(scenario));

        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        
        try {
        	FileInputStream in = new FileInputStream(AI_TUNING_FILE);
        	try {
        		aiTuning.load(in);
        	} finally {
        		in.close();
        	}
        } catch (FileNotFoundException e) {
        	AI_TUNING_FILE.createNewFile();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        AITuning.setInstance(new AITuning() {
			
			@Override
			public double getScalingFactor(String property) {
				if (!aiTuning.containsKey(property)) {
					aiTuning.setProperty(property, "1.0");
					return 1.0;
				}
				
				return Double.valueOf(aiTuning.getProperty(property));
			}
		});
        
        // menu
        menu.setLayout(new ButtonLayout());

        cntrBorderPanel.setLayout(new BorderLayout());
        westBorderPanel.setLayout(new BorderLayout());
        
        eastGridPanel.setLayout(new GridLayout(0,1));
        //eastGridPanel.setBorder(BorderFactory.createLineBorder(GColor.CYAN, 2));
        westGridPanel.setLayout(new GridLayout(0,1));
        //westGridPanel.setBorder(BorderFactory.createLineBorder(GColor.CYAN, 2));
        
        JPanel boardPanel = new JPanel(new BorderLayout());
        boardPanel.add(boardComp, BorderLayout.CENTER);
        boardPanel.add(boardNameLabel, BorderLayout.NORTH);
        cntrBorderPanel.add(boardPanel);
        setupDimensions(
                props.getIntProperty("gui.w", 640),
                props.getIntProperty("gui.h", 480)
        );
        frame.setLayout(new BorderLayout());
        frame.add(cntrBorderPanel, BorderLayout.CENTER);
        
        frame.add(eastGridPanel, BorderLayout.EAST);
        frame.add(westGridPanel, BorderLayout.WEST);
        cntrBorderPanel.add(consoleComponent, BorderLayout.SOUTH);
        helpText.setBorder(BorderFactory.createLineBorder(helpText.getBackground(), 5));
        frame.setBackground(GColor.LIGHT_GRAY);

		initMenu();
	}

	private void setupDimensions(int w, int h) {
        int boardW = w*2/3;
        boardComp.setPreferredSize(boardW, h*3/4);
        Dimension sideDim = new Dimension(w/6, h);
        eastGridPanel.setPreferredSize(sideDim);
        westGridPanel.setPreferredSize(sideDim);
        consoleComponent.setPreferredSize(boardW, h/5);
	}
	
    enum LayoutType {
        LAYOUT_DEFAULT, // just buttons on left the board on the right
        LAYOUT_INGAME,    // buttons on lower left, playerinfo on upper left, board upper center, console lower center 
        LAYOUT_CONFIGURE,  // 2 menus of buttons on left
    }
    
    private LayoutType currentLayoutType = null;
    
    @SuppressWarnings("serial")
    private void initLayout(LayoutType type) {

        if (currentLayoutType != null && currentLayoutType == type)
            return; // nothing to do
        
        currentLayoutType = type;
        if (console != null)
        	console.clear();
        switch (type) {
            case LAYOUT_DEFAULT:
            {
                eastGridPanel.removeAll();
                westGridPanel.removeAll();
                JPanel buttons = new JPanel();
                buttons.add(menu);
                westGridPanel.add(new JSeparator());
                westGridPanel.add(buttons);
                break;
            }
            case LAYOUT_INGAME:
            {
                eastGridPanel.removeAll();
                westGridPanel.removeAll();

                // NEW WAY
                // basically, the user is always on the left and the remaining players are always on the right

                int userPlayerIndex = -1;
                for (int i=0; i<soc.getNumPlayers(); i++) {
                    UIPlayer p = (UIPlayer)soc.getPlayerByPlayerNum(i+1);
                    playerRenderers[i].setPlayer(i+1);
                    if (p instanceof  UIPlayerUser) {
                        userPlayerIndex = i;
                    }
                }

                playerComponents[userPlayerIndex].setMouseEnabled(false);
                if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                    westGridPanel.add(barbarianComp);
                    playerComponents[userPlayerIndex].setMouseEnabled(true);
                }
                westGridPanel.add(playerComponents[userPlayerIndex]);

                for (int i=0; i<soc.getNumPlayers(); i++) {
                    if (i == userPlayerIndex)
                        continue;
                    eastGridPanel.add(playerComponents[i]);
                }

                middleLeftPanel.removeAll();
                JPanel diceHelpPanel = middleLeftPanel.push();
                diceHelpPanel.setLayout(new BorderLayout());
                diceHelpPanel.add(helpText, BorderLayout.SOUTH);
                if (getRules().isEnableEventCards()) {
                    diceHelpPanel.add(eventCardComp, BorderLayout.CENTER);
                } else {
                    diceHelpPanel.add(diceComponent, BorderLayout.CENTER);
                }
                westGridPanel.add(middleLeftPanel);
                JScrollPane menuPanel = new JScrollPane();
                menuPanel.setLayout(new ScrollPaneLayout());
                menuPanel.getViewport().add(menu);
                westGridPanel.add(menuPanel);

                if (!soc.isRunning()) {
                    menuStack.push(MenuState.MENU_GAME_SETUP);
                }
                break;
            }
            case LAYOUT_CONFIGURE:
                initConfigBoardLayout();
                //cntrBorderPanel.remove(console);
                break;
        }
        
        frame.validate();
        frame.repaint();
    }
    
    private void initConfigBoardLayout() {
    	eastGridPanel.removeAll();
        westGridPanel.removeAll();
        JPanel buttons = new JPanel();
        buttons.add(menu);
        JPanel chooser = new JPanel();
        chooser.setLayout(new GridLayout(0,1));
        
        AWTRadioButtonGroup<Object> grp = new AWTRadioButtonGroup<Object>(chooser) {
            @Override
            protected void onChange(Object extra) {
            	if (extra instanceof TileType) {
            		final TileType tt = (TileType)extra;
            		boardRenderer.setPickHandler(new PickHandler() {
						
            			@Override
            			public PickMode getPickMode() {
            				return PickMode.PM_TILE;
            			}
            			
						@Override
						public void onPick(UIBoardRenderer bc, int pickedValue) {
							Tile t = bc.getBoard().getTile(pickedValue);
							t.setType(tt);
							if (t.getResource() == null) {
								t.setDieNum(0);
							}
						}
						
						@Override
						public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
							g.setColor(GColor.YELLOW);
							bc.drawTileOutline(g, getBoard().getTile(highlightedIndex), 2);
						}
						
						@Override
						public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
							g.setColor(GColor.YELLOW);
							bc.drawTileOutline(g, getBoard().getTile(index), 2);
						}
						
						@Override
						public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {}
						
						@Override
						public boolean isPickableIndex(UIBoardRenderer bc, int index) {
							return true;
						}
					});
            	} else if (extra instanceof PickHandler){
            		boardRenderer.setPickHandler((PickHandler)extra);
            	}
            }
        };
        for (TileType c : TileType.values()) {
            grp.addButton(formatString(c.getName(soc)), c);
        }
        grp.addButton("Islands", new PickHandler() {
			
			@Override
			public void onPick(UIBoardRenderer bc, int pickedValue) {
    			int islandNum = getBoard().getTile(pickedValue).getIslandNum();
    			if (getBoard().getTile(pickedValue).getIslandNum() > 0) {
    				getBoard().removeIsland(islandNum);
    			} else {
                	islandNum = getBoard().createIsland(pickedValue);
                	console.addText(GColor.BLACK, "Found island: " + islandNum);
    			}
			}
			
			@Override
			public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
				bc.drawIslandOutlined(g, highlightedIndex);
			}
			
			@Override
			public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
			}
			
			@Override
			public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {
			}
			
			@Override
			public boolean isPickableIndex(UIBoardRenderer bc, int index) {
				return !bc.getBoard().getTile(index).isWater();
			}
			
			@Override
			public PickMode getPickMode() {
				return PickMode.PM_TILE;
			}
		});
        grp.addButton("Pirate Route", new PickHandler() {
			
        	List<Integer> indices = computePirateRouteTiles();
        	
        	private List<Integer> computePirateRouteTiles() {
        		int tIndex = getBoard().getPirateRouteStartTile();
        		if (tIndex < 0) {
        			return getBoard().getTilesOfType(TileType.WATER);
        		}
        		
        		final Tile start = getBoard().getTile(tIndex); 
        		Tile tile = start;
        		
        		while (tile.getPirateRouteNext() >= 0) {
        			tile = getBoard().getTile(tile.getPirateRouteNext());
        			if (tile == start) {
        				// the route is in a loop, so no options
        				return Collections.emptyList();
        			}
        		}
        		
        		List<Integer> result = new ArrayList<>();
        		for (int index : getBoard().getTilesAdjacentToTile(tile)) {
        			Tile tt = getBoard().getTile(index);
        			if (index == getBoard().getPirateRouteStartTile() || (tt.isWater() && tt.getPirateRouteNext() < 0))
        				result.add(getBoard().getTileIndex(tt));
        		}
        		return result;
        	}
        	
        	
			@Override
			public void onPick(UIBoardRenderer bc, int pickedValue) {
				bc.getBoard().addPirateRoute(pickedValue);
				indices = computePirateRouteTiles();
			}
			
			@Override
			public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
				g.setColor(GColor.BLACK);
				bc.drawTileOutline(g, getBoard().getTile(highlightedIndex), 2);
			}
			
			@Override
			public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
				g.setColor(GColor.RED);
				bc.drawTileOutline(g, getBoard().getTile(index), 2);
			}
			
			@Override
			public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {
				g.setColor(GColor.BLACK);
    			int t = getBoard().getPirateRouteStartTile();
    			ArrayList<IVector2D> tiles = new ArrayList<>();
    			while (t >= 0) {
    				Tile tile = getBoard().getTile(t);
    				tiles.add(tile);
    				bc.drawTileOutline(g, tile, 2);
    				t = tile.getPirateRouteNext();
    				if (t == getBoard().getPirateRouteStartTile()) {
    					tiles.add(getBoard().getTile(t));
    					break;
    				}
    			}
                g.begin();
                g.vertexList(tiles);
                g.drawLineStrip(2);
			}
			
			@Override
			public boolean isPickableIndex(UIBoardRenderer bc, int index) {
				return indices.contains(index);
			}
			
			@Override
			public PickMode getPickMode() {
				indices = computePirateRouteTiles();
				return PickMode.PM_TILE;
			}
		});
        grp.addButton("Close routes", new PickHandler() {
			
			@Override
			public void onPick(UIBoardRenderer bc, int pickedValue) {
				Route r = getBoard().getRoute(pickedValue);
				if (r.isClosed()) {
					r.setClosed(false);
				} else {
					r.setClosed(true);
				}
			}
			
			@Override
			public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
				Route rt = getBoard().getRoute(highlightedIndex);
				if (rt.isClosed())
					g.setColor(GColor.BLACK);
				else
					g.setColor(GColor.WHITE);
				bc.drawRoad(g, rt, true);
			}
			
			@Override
			public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
				Route rt = getBoard().getRoute(index);
				if (rt.isClosed())
					g.setColor(GColor.BLACK.withAlpha(120));
				else
					g.setColor(GColor.WHITE.withAlpha(120));
				bc.drawRoad(g, rt, false);
			}
			
			@Override
			public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isPickableIndex(UIBoardRenderer bc, int index) {
				return true;
			}
			
			@Override
			public PickMode getPickMode() {
				return PickMode.PM_EDGE;
			}
		});
        grp.addButton("Pirate Fortress", new PickHandler() {
			
        	List<Integer> indices = new ArrayList<Integer>();
        	
			@Override
			public void onPick(UIBoardRenderer bc, int pickedValue) {
				Vertex v = bc.getBoard().getVertex(pickedValue);
				if (v.getType() == VertexType.OPEN) {
					v.setPirateFortress();
				} else {
					v.setOpen();
				}
			}
			
			@Override
			public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
				Vertex v = getBoard().getVertex(highlightedIndex);
				g.setColor(GColor.BLACK);
				bc.drawSettlement(g, v, 0, true);
			}
			
			@Override
			public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
				Vertex v = getBoard().getVertex(index);
				if (v.getType() == VertexType.PIRATE_FORTRESS) {
					g.setColor(GColor.BLACK);
				} else {
					g.setColor(GColor.BLACK.withAlpha(120));
				}
				bc.drawSettlement(g, v, 0, true);
			}
			
			@Override
			public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {
				
			}
			
			@Override
			public boolean isPickableIndex(UIBoardRenderer bc, int index) {
				return indices.contains(index);
			}
			
			@Override
			public PickMode getPickMode() {
				indices.clear();
				for (int i=0; i<getBoard().getNumAvailableVerts(); i++) {
					Vertex v = getBoard().getVertex(i);
					if (v.canPlaceStructure() && v.getType() == VertexType.OPEN) {
						indices.add(i);
					}
				}
				return PickMode.PM_VERTEX;
			}
		});
        grp.addButton("Settlements", new PickHandler() {

			@Override
			public PickMode getPickMode() {
				return PickMode.PM_VERTEX;
			}

			@Override
			public void onPick(UIBoardRenderer bc, int pickedValue) {
				Vertex v = bc.getBoard().getVertex(pickedValue);
				if (v.getType() == VertexType.OPEN) {
					v.setOpenSettlement();
				} else {
					v.setOpen();
				}
			}

			@Override
			public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
				Vertex v= bc.getBoard().getVertex(index);
				g.setColor(GColor.TRANSLUSCENT_BLACK);
				bc.drawSettlement(g, v, 0, false);
			}

			@Override
			public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {
				int index = 1;
				for (int vIndex : bc.getBoard().getVertIndicesOfType(0, VertexType.OPEN_SETTLEMENT)) {
					Vertex v = bc.getBoard().getVertex(vIndex);
					g.setColor(GColor.LIGHT_GRAY);
					bc.drawSettlement(g, v, 0, false);
					MutableVector2D mv = g.transform(v);
					g.setColor(GColor.YELLOW);
					String text = String.valueOf(index++);
                    g.drawJustifiedString(mv.X(), mv.Y(), Justify.CENTER, Justify.CENTER, text);
				}
			}

			@Override
			public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
				Vertex v= bc.getBoard().getVertex(highlightedIndex);
				bc.drawSettlement(g, v, 0, true);
			}

			@Override
			public boolean isPickableIndex(UIBoardRenderer bc, int index) {
				Vertex v = bc.getBoard().getVertex(index);
				if (v.getType() != VertexType.OPEN && v.getType() != VertexType.SETTLEMENT)
					return false;
				// TODO Auto-generated method stub
				return v.getPlayer() == 0 && v.canPlaceStructure();
			}
        	
        });
        eastGridPanel.add(chooser);
        westGridPanel.add(buttons);		
	}

	private String formatString(String str) {
        String formatted = "";
        str = str.toLowerCase();
        String [] split = str.split("_");
        for (int i=0; i<split.length; i++) {
            String s = split[i];
            String delim = (i>0 && i%2 == 0 ? "\n" : " ");
            formatted += Character.toUpperCase(s.charAt(0)) + s.substring(1) + delim;
        }
        return formatted.trim();
    }
    
    enum DebugPick {
    	OPEN_SETTLEMENT(PickMode.PM_VERTEX, VertexType.OPEN_SETTLEMENT, null, null),
    	SETTLEMENT(PickMode.PM_VERTEX, VertexType.SETTLEMENT, null, null),
    	CITY(PickMode.PM_VERTEX, VertexType.CITY, null, null),
    	CITY_WALL(PickMode.PM_VERTEX, VertexType.WALLED_CITY, null, null),
    	KNIGHT(PickMode.PM_VERTEX, VertexType.BASIC_KNIGHT_ACTIVE, null, null),
    	ROAD(PickMode.PM_EDGE, null, RouteType.ROAD, null),
    	SHIP(PickMode.PM_EDGE, null, RouteType.SHIP, null),
    	WARSHIP(PickMode.PM_EDGE, null, RouteType.WARSHIP, null),
    	MERCHANT(PickMode.PM_TILE, null, null, null),
    	ROBBER(PickMode.PM_TILE, null, null, null),
    	PIRATE(PickMode.PM_TILE, null, null, null),
    	FORTRESS(PickMode.PM_VERTEX, VertexType.PIRATE_FORTRESS, null, null),
    	PATH(PickMode.PM_VERTEX, VertexType.OPEN, null, null),
    	;
    	
    	DebugPick(PickMode mode, VertexType vType, RouteType rType, TileType tType) {
    		this.mode = mode;
    		this.vType = vType;
    		this.rType = rType;
    		this.tType = tType;
    	}
    	
    	final PickMode mode;
    	final VertexType vType;
    	final RouteType rType;
    	final TileType tType;
    }

    private void addMenuItem(Component op) {
        //menu.add(Box.createHorizontalGlue());
        menu.add(op);
        //menu.add(Box.createHorizontalGlue());
    }
    
    
	@SuppressWarnings("serial")
    private void initMenu() {
		if (getBoard().getName().length() == 0) {
			boardNameLabel.setText("Untitled");
		} else {
			boardNameLabel.setText(getBoard().getName());
		}
        log.debug("MenuStack: " + menuStack);
        clearMenu();
		
		if (menuStack.size() > 0) {
    		switch (menuStack.peek()) {
                case MENU_START:
                    initLayout(LayoutType.LAYOUT_DEFAULT);
                    addMenuItem(getMenuOpButton(NEW_GAME));
                    addMenuItem(getMenuOpButton(RESTORE));
                    addMenuItem(getMenuOpButton(CONFIG_BOARD));
                    addMenuItem(getMenuOpButton(CONFIG_SETTINGS));
                    addMenuItem(getMenuOpButton(DEBUG_BOARD));
                    addMenuItem(getMenuOpButton(SAVE_SCENARIO));
                    addMenuItem(getMenuOpButton(LOAD_SCENARIO));
                    addMenuItem(getMenuOpButton(EXIT));
                    break;

                case MENU_CHOOSE_NUM_PLAYERS:
                    initLayout(LayoutType.LAYOUT_DEFAULT);
                    for (int i = getRules().getMinPlayers(); i <= getRules().getMaxPlayers(); i++) {
//    			for (int i=0; i<playerColors.length; i++) {
                        addMenuItem(getMenuOpButton(CHOOSE_NUM_PLAYERS, String.valueOf(i), null, i));
                    }
                    addMenuItem(getMenuOpButton(QUIT, "Back", "Go back to previous menu"));
                    break;

                case MENU_CHOOSE_COLOR:
                    initLayout(LayoutType.LAYOUT_DEFAULT);
                    for (ColorString cs : playerColors) {
                        addMenuItem(getMenuOpButton(CHOOSE_COLOR, cs.name, null, cs.color));
                    }
                    addMenuItem(getMenuOpButton(BACK));
                    break;

                case MENU_GAME_SETUP:
                    initLayout(LayoutType.LAYOUT_INGAME);
                    addMenuItem(getMenuOpButton(START));
                    addMenuItem(getMenuOpButton(START_MULTIPLAYER));
                    addMenuItem(getMenuOpButton(CONFIG_BOARD));
                    addMenuItem(getMenuOpButton(CONFIG_SETTINGS));
                    addMenuItem(getMenuOpButton(BACK));
                    break;

                case MENU_REWIND:
                    addMenuItem(getMenuOpButton(RESTART));
                    addMenuItem(getMenuOpButton(REWIND_GAME));
                    break;

                case MENU_PLAY_GAME:
                    initLayout(LayoutType.LAYOUT_INGAME);
                    break;

                case MENU_DEBUGGING:
                {
                    buildDebugLayout();
                    break;
                }

                case MENU_CONFIG_BOARD:
                    initLayout(LayoutType.LAYOUT_CONFIGURE);
                    addMenuItem(getMenuOpButton(LOAD_DEFAULT));
                    addMenuItem(getMenuOpButton(LOAD_BOARD));
                    addMenuItem(getMenuOpButton(GEN_HEX_BOARD));
                    addMenuItem(getMenuOpButton(GEN_RECT_BOARD));
                    addMenuItem(getMenuOpButton(TRIM_BOARD));
                    addMenuItem(getMenuOpButton(ASSIGN_RANDOM));
                    addMenuItem(getMenuOpButton(SAVE_BOARD_AS_DEFAULT));
                    if (getBoard().getName() != null) {
                        if (new File(getBoard().getName()).isFile())
                            addMenuItem(getMenuOpButton(SAVE_BOARD));
                    }
                    addMenuItem(getMenuOpButton(SAVE_BOARD_AS));
                    addMenuItem(getMenuOpButton(BACK));
                    break;

                case MENU_CHOOSE_DEFAULT_BOARD_SIZE:
                    initLayout(LayoutType.LAYOUT_CONFIGURE);
                    addMenuItem(getMenuOpButton(GEN_HEX_BOARD_SMALL));
                    addMenuItem(getMenuOpButton(GEN_HEX_BOARD_MEDIUM));
                    addMenuItem(getMenuOpButton(GEN_HEX_BOARD_LARGE));
                    addMenuItem(getMenuOpButton(BACK));
                    break;

                case MENU_CHOOSE_CUSTOM_BOARD_SIZE:
                    initLayout(LayoutType.LAYOUT_CONFIGURE);
                    addMenuItem(getMenuOpButton(GEN_RECT_BOARD_SMALL));
                    addMenuItem(getMenuOpButton(GEN_RECT_BOARD_MEDIUM));
                    addMenuItem(getMenuOpButton(GEN_RECT_BOARD_LARGE));
                    addMenuItem(getMenuOpButton(BACK));
                    break;

                default:
                    log.error("Unhandled case : " + menuStack.peek());
    		}
	    }
		frame.validate();
        frame.repaint();
	}
	
	private void buildDebugLayout() {
		initLayout(LayoutType.LAYOUT_DEFAULT);
	    for (final RenderFlag f : RenderFlag.values()) {
	        addMenuItem(
	            new MyToggleButton(f.name(), boardRenderer.getRenderFlag(f)) {

                    void onChecked() {
                       boardRenderer.setRenderFlag(f, true);
                    }
                    void onUnchecked() {
                        boardRenderer.setRenderFlag(f, false);
                    }
		        }
	        );
	    }

	    JPanel choiceButtons = new JPanel();
        choiceButtons.setLayout(new BoxLayout(choiceButtons, BoxLayout.Y_AXIS));
        AWTRadioButtonGroup<DebugPick> pickChoice = new AWTRadioButtonGroup<DebugPick>(choiceButtons) {
            protected void onChange(final DebugPick mode) {
            	boardRenderer.setPickHandler(new PickHandler() {
					
            		int vertex0 = -1;
            		int vertex1 = -1;
            		IDistances d = null;
            		
					@Override
					public void onPick(UIBoardRenderer bc, int pickedValue) {
						Vertex v;
						Route r;
						switch (mode) {
							case CITY:
							case CITY_WALL:
							case SETTLEMENT:
								v = getBoard().getVertex(pickedValue);
								if (v.getPlayer() == 0) {
									v.setPlayerAndType(getCurPlayerNum(), mode.vType);
								} else {
									v.setOpen();
								}
								break;
							case OPEN_SETTLEMENT:
								v = getBoard().getVertex(pickedValue);
								if (v.getType() == VertexType.OPEN_SETTLEMENT) {
									v.setOpen();
								} else {
									v.setOpenSettlement();
								}
								break;
							case KNIGHT:
								v = getBoard().getVertex(pickedValue);
								switch (v.getType()) {
									default:
										v.setPlayerAndType(getCurPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE);
										break;
									case BASIC_KNIGHT_INACTIVE:
										v.setPlayerAndType(getCurPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE);
										break;
									case BASIC_KNIGHT_ACTIVE:
										v.setPlayerAndType(getCurPlayerNum(), VertexType.STRONG_KNIGHT_INACTIVE);
										break;
									case STRONG_KNIGHT_INACTIVE:
										v.setPlayerAndType(getCurPlayerNum(), VertexType.STRONG_KNIGHT_ACTIVE);
										break;
									case STRONG_KNIGHT_ACTIVE:
										v.setPlayerAndType(getCurPlayerNum(), VertexType.MIGHTY_KNIGHT_INACTIVE);
										break;
									case MIGHTY_KNIGHT_INACTIVE:
										v.setPlayerAndType(getCurPlayerNum(), VertexType.MIGHTY_KNIGHT_ACTIVE);
										break;
									case MIGHTY_KNIGHT_ACTIVE:
										v.setOpen();
										break;
								}
								break;
							case FORTRESS:
								v = getBoard().getVertex(pickedValue);
								if (v.getType() == VertexType.PIRATE_FORTRESS) {
									v.setPirateHealth(v.getPirateHealth()-1);
									if (v.getPirateHealth() <= 0)
										v.setOpen();
								} else {
									v.setOpen();
									v.setPirateFortress();
									v.setPirateHealth(3);
								}
								break;
							case ROAD:
							case SHIP:
							case WARSHIP:
								r = getBoard().getRoute(pickedValue);
								if (r.getPlayer() == 0) {
									r.setType(mode.rType);
									getBoard().setPlayerForRoute(r, getCurPlayerNum(), mode.rType);
								} else {
									getBoard().setRouteOpen(r);
								}
								break;
							case MERCHANT:
								if (getBoard().getMerchantPlayer() == getCurPlayerNum() && getBoard().getMerchantTileIndex() == pickedValue) {
									getBoard().setMerchant(-1, 0);
								} else {
									getBoard().setMerchant(pickedValue, getCurPlayerNum());
								}
								break;
							case ROBBER:
								if (getBoard().getRobberTileIndex() == pickedValue)
									getBoard().setRobber(-1);
								else
									getBoard().setRobber(pickedValue);
								break;
							case PIRATE:
								if (getBoard().getPirateTileIndex() == pickedValue)
									getBoard().setPirate(-1);
								else
									getBoard().setPirate(pickedValue);
								break;
							
							case PATH: {
								if (pickedValue == vertex0) {
									vertex0 = -1;
								} else if (pickedValue == vertex1) {
									vertex1 = -1;
								} else if (vertex0 < 0) {
									vertex0 = pickedValue;
								} else {
									vertex1 = pickedValue;
								}
								break;
							}
						}
					}
					
					@Override
					public void onHighlighted(UIBoardRenderer bc, APGraphics g, int highlightedIndex) {
						switch (mode.mode) {
							case PM_EDGE: {
								g.setColor(getPlayerColor(getCurPlayerNum()));
								Route e = getBoard().getRoute(highlightedIndex);
								bc.drawEdge(g, e, mode.rType, e.getPlayer(), true);
								break;
							}
							case PM_TILE: {
								g.setColor(GColor.YELLOW);
								bc.drawTileOutline(g, getBoard().getTile(highlightedIndex), RenderConstants.thinLineThickness);
								break;
							}
							case PM_VERTEX: {
								Vertex v = getBoard().getVertex(highlightedIndex);
								if (mode == DebugPick.PATH) {
									g.setColor(GColor.BLACK);
									g.begin();
									g.vertex(v);
									g.drawPoints(10);
								} else {
									g.setColor(getPlayerColor(getCurPlayerNum()));
									bc.drawVertex(g, v, mode.vType, v.getPlayer(), true);
								}
								break;
							}
							case PM_CUSTOM:
							case PM_NONE:
								break;
						}
					}
					
					@Override
					public void onDrawPickable(UIBoardRenderer bc, APGraphics g, int index) {
						switch (mode.mode) {
							case PM_EDGE: {
								g.setColor(getPlayerColor(getCurPlayerNum()).withAlpha(100));
								Route e = getBoard().getRoute(index);
								bc.drawEdge(g, e, e.getType(), e.getPlayer(), true);
								break;
							}
							case PM_TILE: {
								break;
							}
							case PM_VERTEX: {
								g.setColor(getPlayerColor(getCurPlayerNum()).withAlpha(100));
								Vertex v = getBoard().getVertex(index);
								bc.drawVertex(g, v, v.getType(), v.getPlayer(), true);
								break;
							}
							case PM_CUSTOM:
							case PM_NONE:
								break;
						}
					}
					
					@Override
					public void onDrawOverlay(UIBoardRenderer bc, APGraphics g) {
						g.setColor(GColor.YELLOW);
						g.begin();
						if (vertex0 >= 0) {
							g.vertex(getBoard().getVertex(vertex0));
						}
						if (vertex1 >= 0) {
							g.vertex(getBoard().getVertex(vertex1));
						}
						g.drawPoints(10);
						if (vertex0 >= 0 && vertex1 >= 0) {
							if (d == null) {
								d = getBoard().computeDistances(getRules(), getCurPlayerNum());
							}
                            g.begin();
							List<Integer> path = d.getShortestPath(vertex0, vertex1);
							for (int i=0; i<path.size(); i++) {
								g.vertex(getBoard().getVertex(path.get(i)));
							}
							g.drawLineStrip(5);
							IVector2D v = getBoard().getVertex(path.get(0));
                            g.drawWrapStringOnBackground(v.getX(), v.getY(), g.getViewportWidth()/2, "Dist from " + vertex0 + "->" + vertex1 + " = " + d.getDist(vertex0, vertex1), GColor.TRANSLUSCENT_BLACK, 5);
						}
					}
					
					@Override
					public boolean isPickableIndex(UIBoardRenderer bc, int index) {
						return true;
					}
					
					@Override
					public PickMode getPickMode() {
						return mode.mode;
					}
				});
            }
        };
        //choiceButtons.add(new JLabel("PICK CHOICE"));
        for (DebugPick pm : DebugPick.values()) {
        	pickChoice.addButton(pm.name(), pm);
        }
	    
        choiceButtons.add(new EZPanel(new JLabel("Player:"), playerChooser));
        choiceButtons.add(getMenuOpButton(RESET_BOARD));
        choiceButtons.add(getMenuOpButton(RESET_BOARD_ISLANDS));
        choiceButtons.add(getMenuOpButton(COMPUTE_DISTANCES));
        choiceButtons.add(getMenuOpButton(LOAD_DEBUG));
        choiceButtons.add(getMenuOpButton(SAVE_DEBUG));
        eastGridPanel.removeAll();
        //eastGridPanel.add(new JPanel());
        eastGridPanel.add(choiceButtons);
        
	    addMenuItem(getMenuOpButton(BACK));
	}

	@Override
    public void onAction(MenuItem op, Object extra) {

        if (op == BACK) {
            if (menuStack.size() > 0) {
                menuStack.pop();
                initMenu();
            } else {
                ((JButton)extra).setEnabled(false);
            }
            boardRenderer.setPickHandler(null);
        } else if (op == EXIT) {
            synchronized (soc) {
                System.exit(0);
            }
        } else if (op == DEBUG_BOARD) {
            menuStack.push(MenuState.MENU_DEBUGGING);
            initMenu();
        } else if (op == RESET_BOARD) {
            getBoard().reset();
        } else if (op == RESET_BOARD_ISLANDS) {
            getBoard().clearIslands();
        } else if (op == NEW_GAME) {
            Board b = soc.getBoard();
            b.tryRefreshFromFile();
            soc.initGame();
            menuStack.push(MenuState.MENU_GAME_SETUP);
            menuStack.push(MenuState.MENU_CHOOSE_COLOR);
            menuStack.push(MenuState.MENU_CHOOSE_NUM_PLAYERS);
            initMenu();
        } else if (op == RESTORE) {
            try {
                soc.stopRunning();
                loadGame(saveGameFile);
                menuStack.push(MenuState.MENU_PLAY_GAME);
                soc.startGameThread();
                initMenu();
            } catch (Exception e) {
                e.printStackTrace();
                ((JButton)extra).setEnabled(false);
                e.printStackTrace();
            }
        } else if (op == CONFIG_BOARD) {
            menuStack.push(MenuState.MENU_CONFIG_BOARD);
            initMenu();
        } else if (op == CONFIG_SETTINGS) {
            showConfigureGameSettingsPopup(getRules().deepCopy(), true);
        } else if (op ==CHOOSE_NUM_PLAYERS) {
            initPlayers((Integer) extra);
            menuStack.pop();
            initMenu();
        } else if (op == CHOOSE_COLOR) {
            // reload the board
            setPlayerColor((GColor) extra);
            menuStack.pop();
            initMenu();
        } else if (op == START) {
            if (getBoard().isReady()) {
                getBoard().assignRandom();
                menuStack.clear();
                menuStack.push(MenuState.MENU_START);
                menuStack.push(MenuState.MENU_PLAY_GAME);
                initMenu();
                clearSaves();
                soc.startGameThread();
            } else {
                log.error("Board not ready");
            }
        } else if (op == RESTART) {
            menuStack.pop();
            initMenu();
            soc.startGameThread();
        } else if (op == START_MULTIPLAYER) {
            if (getBoard().isReady()) {
                try {
                    soc.server.listen();
                } catch (Exception e) {
                    showOkPopup("ERROR", "Failed to start server. " + e.getClass().getSimpleName() + ":" + e.getMessage());
                    return;
                }

                try {
                    jmdns = JmDNS.create(InetAddress.getLocalHost());

                    // Register a service
                    ServiceInfo serviceInfo = ServiceInfo.create(NetCommon.DNS_SERVICE_ID,
                            "Senators of Katan", NetCommon.PORT,
                            "name=" + System.getProperty("user.name") + ",numplayers=" + soc.getNumPlayers());
                    jmdns.registerService(serviceInfo);

                    // Unregister all services
                    jmdns.unregisterAllServices();
                    console.addText(GColor.BLACK, "Broadcasting on Bonjour");
                } catch (Exception e) {
                    soc.server.stop();
                    showOkPopup("ERROR", "Failed to register Bonjour service. "+ e.getClass().getSimpleName() + ":" + e.getMessage());
                    return;
                }

                getBoard().assignRandom();
                menuStack.clear();
                menuStack.clear();
                menuStack.push(MenuState.MENU_START);
                menuStack.push(MenuState.MENU_PLAY_GAME);
                initMenu();
            } else {
                log.error("Board not ready");
            }
        } else if (op == GEN_HEX_BOARD) {
            menuStack.push(MenuState.MENU_CHOOSE_DEFAULT_BOARD_SIZE);
            initMenu();
        } else if (op == GEN_HEX_BOARD_SMALL) {
            getBoard().generateHexBoard(4, TileType.WATER);
            menuStack.pop();
            initMenu();
        } else if (op == GEN_HEX_BOARD_MEDIUM) {
            getBoard().generateHexBoard(5, TileType.WATER);
            menuStack.pop();
            initMenu();
        } else if (op == GEN_HEX_BOARD_LARGE) {
            getBoard().generateHexBoard(6, TileType.WATER);
            menuStack.pop();
            initMenu();
        } else if (op == GEN_RECT_BOARD) {
            menuStack.push(MenuState.MENU_CHOOSE_CUSTOM_BOARD_SIZE);
            initMenu();
        } else if (op == GEN_RECT_BOARD_SMALL) {
            getBoard().generateRectBoard(6, TileType.WATER);
            menuStack.pop();
            initMenu();
        } else if (op == GEN_RECT_BOARD_MEDIUM) {
            getBoard().generateRectBoard(8, TileType.WATER);
            menuStack.pop();
            initMenu();
        } else if (op == GEN_RECT_BOARD_LARGE) {
            getBoard().generateRectBoard(10, TileType.WATER);
            menuStack.pop();
            initMenu();
        } else if (op == TRIM_BOARD) {
            getBoard().trim();
        } else if (op == ASSIGN_RANDOM) {
            getBoard().assignRandom();
        } else if (op == SAVE_BOARD_AS_DEFAULT) {
            saveBoard(defaultBoardFile);
        } else if (op == LOAD_DEFAULT) {
            if (loadBoard(defaultBoardFile)) {
                frame.repaint();
            }
        } else if (op == SAVE_BOARD) {
            saveBoard(new File(getBoard().getName()));
        } else if (op == SAVE_BOARD_AS) {
            JFileChooser chooser = new JFileChooser();
            File baseDir = new File(getProps().getProperty(PROP_BOARDS_DIR, "assets/boards"));
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                showOkPopup("ERROR", "Failed to ceate directory tree '" + baseDir + "'");
            } else if (!baseDir.isDirectory()) {
                showOkPopup("ERROR", "Not a directory '" + baseDir + "'");
            } else {
                chooser.setCurrentDirectory(baseDir);
                chooser.setDialogTitle("Save Board");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(getExtensionFilter("txt", true));
                int result = chooser.showSaveDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    baseDir = file.getParentFile();
                    String fileName = file.getAbsolutePath();
                    if (!fileName.endsWith(".txt"))
                        fileName += ".txt";
                    saveBoard(new File(fileName));
                }
            }
        } else if (op == LOAD_BOARD) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(getExtensionFilter("txt", true));
            File boardsDir = new File(getProps().getProperty(PROP_BOARDS_DIR, "assets/boards"));
            if (!boardsDir.isDirectory()) {
                showOkPopup("ERROR", "Boards directory missing");
            } else if (boardsDir.list().length == 0) {
                showOkPopup("ERROR", "No Boards in boards directory");
            } else {
                chooser.setCurrentDirectory(boardsDir);
                chooser.setDialogTitle("Load Board");
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int result = chooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    loadBoard(file);
                }
            }
        } else if (op == REWIND_GAME) {
            soc.stopRunning();
            try {
                FileUtils.restoreFile(saveGameFile.getAbsolutePath());
                loadGame(saveGameFile);
                //soc.startGameThread();
                clearMenu();
                if (getCurrentMenu() != MenuState.MENU_REWIND) {
                    menuStack.push(MenuState.MENU_REWIND);
                }
                initMenu();
                soc.redraw();
            } catch (Exception e) {
                ((JButton)extra).setEnabled(false);
            }
        } else if (op == SHOW_RULES) {
            showConfigureGameSettingsPopup(getRules(), false);
        } else if (op == QUIT) {
            quitToMainMenu();
        } else if (op == BUILDABLES_POPUP) {
            Vector<String> columnNames = new Vector<String>();
            columnNames.add("Buildable");
            for (ResourceType r : ResourceType.values()) {
                columnNames.add(r.name());

            }
            Vector<Vector<Object>> rowData = new Vector<Vector<Object>>();
            for (BuildableType b : BuildableType.values()) {
                if (b.isAvailable(getSOC())) {
                    Vector<Object> row = new Vector<Object>();
                    row.add(b.name());
                    for (ResourceType r : ResourceType.values())
                        row.add(String.valueOf(b.getCost(r)));
                    rowData.add(row);
                }
            }
            JTable table = new JTable(rowData, columnNames);
            table.getColumnModel().getColumn(0).setMinWidth(100);
            JScrollPane view = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            view.getViewport().add(table);
            this.showOkPopup("BUILDABLE", view);
        } else if (op == SAVE_SCENARIO) {

            String txt = FileUtils.stripExtension(new File(getBoard().getName()).getName());
            if (txt.length() == 0)
                txt = "MyScenario";
            final JTextField nameField = new JTextField(txt);
            EZPanel panel = new EZPanel(new GridLayout(0, 1), new JLabel("Enter Scenario name"), nameField);
            showPopup("Save Current Board and Rules as Scenario", panel, new PopupButton[]{
                    new PopupButton("Cancel"),
                    new PopupButton("Save") {
                        @Override
                        public boolean doAction() {
                            getBoard().setName(nameField.getText());
                            final JFileChooser chooser = new JFileChooser();
                            File scenarioDir = new File(getProps().getProperty(PROP_SCENARIOS_DIR, "assets/scenarios"));
                            if (!scenarioDir.exists()) {
                                if (!scenarioDir.mkdirs()) {
                                    showOkPopup("ERROR", "Failed to create directory '" + scenarioDir + "'");
                                    return true;
                                }
                            } else if (!scenarioDir.isDirectory()) {
                                showOkPopup("ERROR", "Not a directory '" + scenarioDir + "'");
                                return true;
                            }
                            chooser.setSelectedFile(new File(scenarioDir, nameField.getText()));
                            chooser.setCurrentDirectory(scenarioDir);
                            chooser.setDialogTitle("Save Scenario");
                            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            chooser.setFileFilter(getExtensionFilter("txt", true));
                            int result = chooser.showSaveDialog(frame);
                            if (result == JFileChooser.APPROVE_OPTION) {
                                File file = chooser.getSelectedFile();
                                String fileName = file.getAbsolutePath();
                                if (!fileName.endsWith(".txt"))
                                    fileName += ".txt";
                                try {
                                    Map<String, Double> aituning = new HashMap<>();
                                    for (Object key : aiTuning.keySet()) {
                                        aituning.put((String)key, Double.valueOf(aiTuning.getProperty((String)key)));
                                    }
                                    Scenario scenario = new Scenario(soc, aituning);
                                    scenario.saveToFile(new File(fileName));

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            return true;
                        }

                    }
            });
        } else if (op == LOAD_SCENARIO) {
            File scenariosDir = new File(getProps().getProperty(PROP_SCENARIOS_DIR, "assets/scenarios"));
            if (!scenariosDir.isDirectory()) {
                showOkPopup("ERROR", "Cant find scenarios directory '" + scenariosDir + "'");
            } else {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(scenariosDir);
                chooser.setDialogTitle("Load Scenario");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(getExtensionFilter("txt", true));
                int result = chooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        loadGame(file);
                        getProps().setProperty("scenario", file.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (op == COMPUTE_DISTANCES) {
            long t = System.currentTimeMillis();
            IDistances distances = getBoard().computeDistances(getRules(), getCurPlayerNum());
            long dt = System.currentTimeMillis() - t;
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<getBoard().getNumAvailableVerts()-1; i++) {
                for (int ii=i+1; ii<getBoard().getNumAvailableVerts(); ii++) {
                    int dist = distances.getDist(i, ii);
                    if (dist != IDistances.DISTANCE_INFINITY) {
                        buf.append(String.format("DIST %-3d -> %-3d = %d\n", i, ii, dist));
                    }
                }
            }
            System.out.println("got Distances in " + dt + " MSecs:\n" + buf);

        } else if (op == LOAD_DEBUG) {
            loadBoard(debugBoard);
        } else if (op == SAVE_DEBUG) {
            saveBoard(debugBoard);
        } else if (op == AITUNING_NEXT_OPTIMAL_INDEX) {
            optimalIndex = (optimalIndex + 1) % optimalOptions.size();
        } else if (op == AITUNING_PREV_OPTIMAL_INDEX) {
            optimalIndex = (optimalOptions.size() + optimalIndex - 1) % optimalOptions.size();
        } else if (op == AITUNING_ACCEPT_OPTIMAL) {
            soc.setReturnValue(optimalOptions.get(optimalIndex));
        } else if (op == AITUNING_REFRESH) {
            try {
                // yikes! is there a better way to do this?
                JTextArea area = (JTextArea)(((JScrollPane)middleLeftPanel.top().getComponent(0)).getViewport().getView());
                String txt = area.getText();
                String [] lines = txt.split("[\n]");
                for (int i=1; i<lines.length; i++) {
                    String line = lines[i];
                    String [] parts = line.split("[ ]+");
                    aiTuning.setProperty(parts[0], parts[1]);
                }
                FileOutputStream out = new FileOutputStream(AI_TUNING_FILE);
                try {
                    aiTuning.store(out, "Generated by SOC Swing Utility");
                } finally {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
		}
		soc.notifyWaitObj();
	}
	
	private void loadGame(File file) throws IOException {
		soc.loadFromFile(file);
		soc.getBoard().setName(file.getAbsolutePath());
		initMenu();
	}
    
    public synchronized void closePopup() {
        if (popup != null) {
            synchronized (popup) {
                popup.notify();
            }
            popup.setVisible(false);
            popup = null;        
        }
        frame.setEnabled(true);
        frame.setVisible(true);
    }

    public void quitToMainMenu() {
        soc.stopRunning();
        //soc.clear();
        boardRenderer.setPickHandler(null);
        console.clear();
        menuStack.clear();
        menuStack.push(MenuState.MENU_START);
        initMenu();
    }
    
    private OpButton getMenuOpButton(MenuItem op) {
        return getMenuOpButton(op, op.title, op.helpText, null);
    }

    private OpButton getMenuOpButton(MenuItem op, String txt, String helpText) {
		return getMenuOpButton(op, txt, helpText, null);
	}

	private OpButton getMenuOpButton(MenuItem op, String text, String helpText, Object extra) {
		OpButton button = new OpButton(op, text, extra);
		button.addActionListener(this);
		button.setToolTipText(helpText);
		return button;
	}
	
	private void initPlayers(int numPlayers) {
        soc.clear();
        Player [] players = new Player[numPlayers];
        players[0] = localPlayer = new UIPlayerUser();
        localPlayer.setColor(playerColors[0].color);
        for (int i=1; i<numPlayers; i++) {
			UIPlayer p = new UIPlayer();
			p.setColor(playerColors[i].color);
			p.setPlayerNum(i+1);
            players[i] = p;			
		}
		
		// now shuffle the player nums
        for (int i=0; i<numPlayers; i++) {
        	players[i].setPlayerNum(i+1);
        	soc.addPlayer(players[i]);
        }
	}
	
	private void setPlayerColor(GColor color) {
		log.debug("setPlayerColor too: " + color);
		// swap the players colors
		assert(color != null);
		GColor temp = localPlayer.getColor();
		localPlayer.setColor(color);
		for (int i=1; i<=soc.getNumPlayers(); i++) {
			if (i == localPlayer.getPlayerNum())
				continue;
			UIPlayer p = getGUIPlayer(i);
			if (p.getColor().equals(color)) {
				p.setColor(temp);
				break;
			}
		}
	}
	
	private boolean saveBoard(File file) {
		try {
			getBoard().setName(file.getAbsolutePath());
			getBoard().saveToFile(file);
			boardNameLabel.setText(file.getName());
			initMenu();
		} catch (IOException e) {
			log.error(e.getMessage());
			return false;
		}
		return true;
	}
	
	private boolean loadBoard(File file) {
		try {
			Board b = new Board();
			b.loadFromFile(file);
			getBoard().copyFrom(b);
			boardNameLabel.setText(file.getName());
			return true;
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
	}
	
	public UIPlayer getGUIPlayer(int playerNum) {
		return (UIPlayer)soc.getPlayerByPlayerNum(playerNum);
	}
	
	public SOC getSOC() {
		return soc;
	}

    public GColor getPlayerColor(int playerNum) {
		if (playerNum < 1)
			return GColor.GRAY;
		UIPlayer p = getGUIPlayer(playerNum);
		if (p == null)
			return playerColors[playerNum-1].color;
		return p.getColor();
	}
    
	public int getCurPlayerNum() {
		if (getCurrentMenu() == MenuState.MENU_DEBUGGING)
			return (Integer)playerChooser.getValue();
        return soc.getCurPlayerNum();
    }

	private int optimalIndex = 0;
	private List<BotNode> optimalOptions = null;
	
	private String getBotNodeDetails(BotNode node, int maxKeyWidth, Map<String,Double> maxValues) {
		StringBuffer info = new StringBuffer();
		info.append(String.format("%-" + Math.max(5, maxKeyWidth) + "s FACTOR  VALUE MAX TOT\n", "PROPERTY"));
		for (String key : node.getKeys()) {
			double factor = AITuning.getInstance().getScalingFactor(key);
			double value =  node.getValue(key);
			int percentMax = (int)(100 * value / maxValues.get(key));
			int percentTot = (int)(100 * factor * value / node.getValue());
			info.append(String.format("%-" + maxKeyWidth + "s %1.4f %1.4f %3d %3d\n", key, factor, value, percentMax, percentTot));
		}		
		return info.toString();
	}
			
	interface MyCustomPickHandler extends CustomPickHandler {
	    void onMouseWheel(int rotation);
    };

	static final class NodeRect {
		final GRectangle r;
		final String s;

		public NodeRect(GRectangle r, String s) {
			this.r = r;
			this.s = s;
		}
	}
	
	private void initNodeRectsArray(AGraphics g, Collection<BotNode> leafs, NodeRect [] nodeRects, int ypos) {
		int index = 0;
		final int fontHeight = g.getTextHeight();
		final int padding = 2;

		// need to setup the same transform as UIBoardRenderer
        float width = g.getViewportWidth();
        float height = g.getViewportHeight();
        float dim = Math.min(width, height);
        g.pushMatrix();
        g.setIdentity();
        g.translate(width/2, height/2);
        g.scale(dim, dim);
        g.translate(-0.5f, -0.5f);

		for (BotNode node : leafs) {
		    // find the best node from the root that suits us.
            // start moving up the tree and pick the top most that is an edge, vertex or tile node
            BotNode n = node;
            BotNode best = node;
            String desc = n.getDescription();
            while (n.getParent() != null) {
                n = n.getParent();
                if ((n instanceof BotNodeVertex) ||
                        (n instanceof BotNodeRoute) ||
                        (n instanceof BotNodeTile)) {
                    best = n;
                }
                if (n.getParent() != null)
                    desc = n.getDescription();
            }
		    n=best;

			MutableVector2D v = new MutableVector2D(n.getBoardPosition(getBoard()));
			if (true || v.isZero()) {

			    // this method seems easier
				String s = String.valueOf(index) +  " " + desc;
				GRectangle r = new GRectangle(padding, ypos, (int)g.getTextWidth(s), fontHeight);
				nodeRects[index] = new NodeRect(r, s);
				ypos += fontHeight+padding*2+1;
				
			} else {
				g.transform(v);
				String s = String.valueOf(index);
				width = g.getTextWidth(s);
				GRectangle r = new GRectangle(v.X() - width/2, v.Y() - fontHeight/2, width, fontHeight);
				for (int i=0; i<index; i++) {
					if (nodeRects[i].r.isIntersectingWidth(r)) {
						r.x = nodeRects[i].r.x;
						r.y = nodeRects[i].r.y + nodeRects[i].r.h + padding;
						break;
					}
				}
				nodeRects[index] = new NodeRect(r, s);
			}
			index++;
		}

		g.popMatrix();
	}

	public void showPopup(JFrame pop) {
        if (popup != null)
            popup.setVisible(false);
        popup = pop;
        popup.setUndecorated(true);
        frame.setEnabled(false);
        popup.setMinimumSize(new Dimension(160,120));
        popup.pack();
        int x = frame.getX() + frame.getWidth()/2 - popup.getWidth()/2;
        int y = frame.getY() + frame.getHeight()/2 - popup.getHeight()/2;
        popup.setLocation(x, y);
        popup.setResizable(false);
        popup.setVisible(true);
    }
    
    public void showPopup(String title, JComponent view, PopupButton[] button) {
        JFrame frame = new JFrame();
        frame.setTitle(title);
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        container.setLayout(new BorderLayout());
        container.add(new JLabel(title), BorderLayout.NORTH);
        container.add(view, BorderLayout.CENTER);
        Container buttons = new Container();
        container.add(buttons, BorderLayout.SOUTH);
        buttons.setLayout(new GridLayout(1, 0));
        for (int i=0; i<button.length; i++) {
            if (button[i] != null) {
                buttons.add(button[i]);
                button[i].addActionListener(this);
            } else {
                buttons.add(new JLabel());
            }
        }
        frame.setContentPane(container);
        showPopup(frame);
    }
    
    public void showPopup(String name, String msg, 
            PopupButton leftButton,
            PopupButton middleButton,
            PopupButton rightButton) {
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setTitle(name);
        JTextArea label = new JTextArea(msg);
        label.setLineWrap(true);

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        container.setLayout(new BorderLayout());
        container.add(new JLabel(name), BorderLayout.NORTH);
        container.add(label, BorderLayout.CENTER);
        Container buttons = new Container();
        container.add(buttons, BorderLayout.SOUTH);
        buttons.setLayout(new GridLayout(1, 0));
        if (leftButton != null) {
            buttons.add(leftButton);
            leftButton.addActionListener(this);
        }
        else
            buttons.add(new JLabel());
        if (middleButton!= null) {
            buttons.add(middleButton);
            middleButton.addActionListener(this);
        }
        else
            buttons.add(new JLabel());
        if (rightButton != null) {
            buttons.add(rightButton);
            rightButton.addActionListener(this);
        }
        else
            buttons.add(new JLabel());
        
        frame.setContentPane(container);
        showPopup(frame);
    }
    
    public void showPopup(String name, String msg, PopupButton leftButton, PopupButton rightButton) {
    	showPopup(name, msg, leftButton, null, rightButton);
    }

    public void showPopup(String name, String msg, PopupButton middleButton) {
    	showPopup(name, msg, null, middleButton, null);
    }
    
    public void showOkPopup(String name, JComponent view) {
        PopupButton button = new PopupButton("OK");
        showPopup(name, view, new PopupButton[] { null, button, null});
    }
    
    public void showOkPopup(String name, String msg) {
        PopupButton button = new PopupButton("OK");
        showPopup(name, msg, button);
    }

    public void showConfigureGameSettingsPopup(final Rules rules, boolean editable) {
        final JPanel view = new JPanel();
        JScrollPane panel = new JScrollPane();
        panel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //panel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.setPreferredSize(new Dimension(1000, 800));
        panel.getViewport().add(view);
        view.setLayout(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        cons.fill = GridBagConstraints.BOTH;
        cons.anchor = GridBagConstraints.WEST;

        final HashMap<JComponent, Field> components = new HashMap<JComponent, Field>();
        final int numCols = 10;
        try {
            Rules.Variation var = null;
            Field [] fields = Rules.class.getDeclaredFields();
        	for (Field f : fields) {
        		Annotation [] anno = f.getAnnotations();
        		for (Annotation a : anno) {
        			if (a.annotationType().equals(Rules.Rule.class)) {
    					cons.gridx=0;
        				f.setAccessible(true);
                        Rules.Rule ruleVar = (Rules.Rule)a;
                        if (var != ruleVar.variation()) {
                            var = ruleVar.variation();
        					cons.fill=HORIZONTAL;
        					cons.gridwidth=numCols;
        					view.add(new JLabel(getSOC().getString(var.stringId)), cons);
        					cons.gridy++;
        					view.add(new JSeparator(), cons);
        					cons.gridy++;
        					cons.fill=NONE;
        				}

        				
        				cons.gridx=0;
        				cons.gridwidth=1;
        				if (f.getType().equals(boolean.class)) {
        					if (editable) {
        						JCheckBox button = new JCheckBox("", f.getBoolean(rules));
            			        view.add(button, cons);
            			        components.put(button,  f);
        					} else {
        						view.add(new JLabel(f.getBoolean(rules) ? "Enabled" : "Disabled"), cons);
        					}
        				} else if (f.getType().equals(int.class)) {
        					if (editable) {
            			        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(f.getInt(rules), ruleVar.minValue(), ruleVar.maxValue(), 1));
            			        view.add(spinner, cons);
            			        components.put(spinner, f);
        					} else {
        						view.add(new JLabel("" + f.getInt(rules)), cons);
        					}
        				} else {
        					System.err.println("Dont know how to handle field type:" + f.getType());
        				}
        				cons.gridx=1;
        				cons.gridwidth = numCols-1;
        				String txt = "<html><div WIDTH=900>" + getSOC().getString(ruleVar.stringId()) + "</div></html>";
        				JLabel label = new JLabel(txt);
    			        view.add(label, cons);
    			        cons.gridy++;
        				break;
        			}
        		}
        	}
        	
        	PopupButton [] buttons = new PopupButton[4];
            buttons[0] = new PopupButton("View\nDefaults") {
                public boolean doAction() {
                    new Thread(new Runnable() {
                        public void run() {
                            showConfigureGameSettingsPopup(new Rules(), true);
                        }
                    }).start();
                    return false;
                }
            };
            
            buttons[1] = new PopupButton("Save\nAs Default") {
                public boolean doAction() {
                	try {

                		for (JComponent c : components.keySet()) {
                			Field f = components.get(c);
                			if (c instanceof JToggleButton) {
                				boolean value = ((JToggleButton)c).isSelected();
                				f.setBoolean(rules, value);
                			} else if (c instanceof JSpinner) {
                				int value = (Integer)((JSpinner)c).getValue();
                				f.setInt(rules, value);
                			}
                		}
                		
                		rules.saveToFile(saveRulesFile.getAbsoluteFile());
                		GUI.this.getRules().copyFrom(rules);
                	} catch (Exception e) {
                		e.printStackTrace();
                	}
                    return true;
                }
            };
            
            buttons[2] = new PopupButton("Keep") {
            	public boolean doAction() {

            		
            		try {
            			// TODO: fix cut-paste code
                		for (JComponent c : components.keySet()) {
                			Field f = components.get(c);
                			if (c instanceof JToggleButton) {
                				boolean value = ((JToggleButton)c).isSelected();
                				f.setBoolean(rules, value);
                			} else if (c instanceof JSpinner) {
                				int value = (Integer)((JSpinner)c).getValue();
                				f.setInt(rules, value);
                			}
                		}
    
                		getRules().copyFrom(rules);
            		} catch (Exception e) {
            			e.printStackTrace();
            		}
            		return true;
            	}
            };
            
            buttons[3] = new PopupButton("Cancel");
            
            this.showPopup("CONFIGURE GAME SETTINGS", panel, buttons);
        	
        	
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        
    }
    
    
    // END POPUPS
    
    // OVERRIDES
    
    @Override
	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() instanceof OpButton) {
            OpButton button = (OpButton) e.getSource();
            MenuItem op = button.item;
            op.action.onAction(op, button.extra);
        } else if (e.getSource() instanceof PopupButton) {
	        PopupButton button = (PopupButton)e.getSource();
	        if (button.doAction()) {
	            closePopup();
            }
        }
		frame.repaint();
	}

    public MenuState getCurrentMenu() {
    	if (menuStack.size() == 0)
    		return MenuState.MENU_START;
    	return menuStack.peek();
    }
    
    UIProperties getProps() {
    	return props;
    }

	private void clearSaves() {
		try {
    		FileUtils.deleteDirContents(HOME_FOLDER, "playerAI*");
    		FileUtils.deleteDirContents(HOME_FOLDER, "socsave*");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    static FileFilter getExtensionFilter(final String ext, final boolean acceptDirectories) {

        return new FileFilter() {

            public boolean accept(File file) {
                if (file.isDirectory() && acceptDirectories)
                    return true;
                return file.getName().endsWith(ext);
            }

            public String getDescription() {
                return "SOC Board Files";
            }

        };
    }
}

