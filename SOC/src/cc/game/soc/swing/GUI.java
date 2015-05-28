package cc.game.soc.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import cc.game.soc.core.*;
import cc.game.soc.core.Player.RouteChoiceType;
import cc.game.soc.core.annotations.RuleVariable;
import cc.game.soc.swing.BoardComponent.PickMode;
import cc.game.soc.swing.BoardComponent.RenderFlag;
import cc.game.soc.swing.PlayerInfoComponent.CardLoc;
import cc.lib.game.Utils;
import cc.lib.swing.AWTUtils;
import cc.lib.swing.EZPanel;
import cc.lib.swing.ImageMgr;
import cc.lib.swing.JMultiColoredScrollConsole;
import cc.lib.swing.JWrapLabel;
import cc.lib.utils.FileUtils;

public class GUI implements ActionListener, ComponentListener, WindowListener, Runnable, BoardComponent.BoardListener {

    final Logger log = Logger.getLogger(GUI.class);
    
    static GUI instance;
    static final File homeFolder = new File(System.getProperty("user.home") + "/.soc");

	public static void main(String [] args)  {
		JFrame frame = new JFrame();
		try {
			
//			System.out.println(System.getProperties().toString().replace(",", "\n"));
			
			Utils.setDebugEnabled(true);
			GUIProperties props = new GUIProperties();
			if (!homeFolder.exists()) {
				if (!homeFolder.mkdir()) {
					throw new RuntimeException("Cannot create home folder: " + homeFolder);
				}
			} else if (!homeFolder.isDirectory()) {
				throw new RuntimeException("Not a directory: " + homeFolder);
			}
            props.load(new File(homeFolder, "gui.properties").getAbsolutePath());
			GUI gui = new GUI(frame, props);
			frame.addWindowListener(gui);
			frame.addComponentListener(gui);
            int w = props.getIntProperty("gui.w", 640);
            int h = props.getIntProperty("gui.h", 480);
			frame.setSize(w, h);
	        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	        int x,y;
	        x = dim.width / 2 - frame.getWidth() / 2;
	        y = dim.height / 2 - frame.getHeight() / 2;
            x = props.getIntProperty("gui.x", x);
            y = props.getIntProperty("gui.y", y);
	        frame.setLocation(x, y);
	        frame.setVisible(true);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private enum MenuState {

	    MENU_START,
	    MENU_CHOOSE_NUM_PLAYERS,
	    MENU_CHOOSE_COLOR,
	    MENU_GAME_SETUP,
	    MENU_PLAY_GAME,
	    MENU_CONFIG_BOARD,
	    MENU_CHOOSE_DEFAULT_BOARD_SIZE,
	    MENU_CHOOSE_CUSTOM_BOARD_SIZE, 
	    MENU_DEBUGGING,
	    
	}
	
    private SOCGUI soc;
	private final JPanel menu = new JPanel();
	private final JMultiColoredScrollConsole console;
	private final BoardComponent boardComp;
	BarbarianComponent barbarianComp;
	private ADiceComponent [] diceComps;// = new ADiceComponent[3];
	private JSpinner [] diceChoosers;
	private final GUIProperties props;
	final ImageMgr images;
	
	//private Board board;

	private Stack<MenuState> menuStack = new Stack<MenuState>();
	private GUIPlayerUser localPlayer;
	private boolean running;
	private Object returnValue = null;
	private Object waitObj = this;
	Container frame;
	private JFrame popup;
	private JPanel westBorderPanel = new JPanel();
	private JPanel cntrBorderPanel = new JPanel();
	
	private JPanel eastGridPanel = new JPanel();
	private JPanel westGridPanel = new JPanel();
	private JScrollPane consolePane;
	private JSpinner playerChooser;
	private JLabel helpText;
	PlayerInfoComponent [] playerComponents = new PlayerInfoComponent[8];
	
	final static class ColorString {
		final Color color;
		final String name;
		
		ColorString(Color color, String name) {
			this.color = color;
			this.name = name;
		}
	}
	
	private ColorString [] playerColors;

    private File defaultBoardFile;
    private final File saveGameFile;
    private final File saveRulesFile;
    
    private float diceSpinTimeSeconds;
    
//    private Rules rules;
    private JLabel boardNameLabel = new JLabel("Untitled");
    
    Board getBoard() {
    	return soc.getBoard();
    }
    
    Rules getRules() {
    	return soc.getRules();
    }
    
    private void clearMenu() {
    	menu.removeAll();
    }
    
	public GUI(Container frame, final GUIProperties props) throws IOException {
		instance = this;
		
		this.frame = frame;
		this.props = props;
		soc = new SOCGUI(this);
		
		String boardFilename = props.getProperty("gui.defaultBoardFilename", "soc_def_board.txt");
        defaultBoardFile = new File(homeFolder, boardFilename);
        if (!defaultBoardFile.exists()) {
        	defaultBoardFile = new File(boardFilename);
        }
        saveGameFile = new File(homeFolder, props.getProperty("gui.saveGameFileName", "socsavegame.txt"));
        saveRulesFile = new File(homeFolder, props.getProperty("gui.saveRulesFileName", "socrules.txt"));
        diceSpinTimeSeconds = props.getFloatProperty("gui.diceSpinTimeSeconds", 3);
        
        if (saveRulesFile.exists()) {
        	getRules().loadFromFile(saveRulesFile);
        }
        
        menuStack.push(MenuState.MENU_START);
		if (!loadBoard(defaultBoardFile.getAbsolutePath())) {
			//board.generateRectBoard(8);
			getBoard().generateDefaultBoard();
//			if (!getBoard().isFinalized())
//				menuStack.push(MenuState.MENU_CONFIG_BOARD);
//			else
				saveBoard(defaultBoardFile.getAbsolutePath());
		}
        playerColors = new ColorString[] {
        		new ColorString(Color.RED, "Red"),
        		new ColorString(AWTUtils.darken(Color.GREEN, 0.5f), "Green"),
        		new ColorString(Color.BLUE,"Blue"),
        		new ColorString(AWTUtils.darken(Color.ORANGE, 0.1f), "Orange"),
        		new ColorString(Color.MAGENTA, "Magenta")
        };

        playerChooser = new JSpinner(new SpinnerNumberModel(props.getIntProperty("debug.playerNum",  1), 1, playerColors.length, 1));
        playerChooser.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				props.setProperty("debug.playerNum",  (Integer)playerChooser.getValue());
			}
		});
		// dice component
        
    	images = new ImageMgr(frame);

		// board component
        boardComp = new BoardComponent(this, getBoard(), images);
        
        // console
        Color bkColor = props.getColorProperty("console.bkColor", Color.LIGHT_GRAY);
        if (bkColor == null)
            console = new JMultiColoredScrollConsole();
        else
            console = new JMultiColoredScrollConsole(bkColor);
        consolePane = new JScrollPane();
        consolePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        consolePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        consolePane.getViewport().add(console);
        consolePane.getVerticalScrollBar().setBlockIncrement(console.getTextHeight());
        consolePane.getVerticalScrollBar().setUnitIncrement(console.getTextHeight());
        consolePane.setPreferredSize(console.getMinimumSize());

        // menu
        menu.setLayout(new GridLayout(0 ,1));

        cntrBorderPanel.setLayout(new BorderLayout());
        westBorderPanel.setLayout(new BorderLayout());
        
        eastGridPanel.setLayout(new GridLayout(0,1));
        //eastGridPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        westGridPanel.setLayout(new GridLayout(0,1));
        //westGridPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        
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
        cntrBorderPanel.add(consolePane, BorderLayout.SOUTH); 
        helpText = new JLabel();
        helpText.setBorder(BorderFactory.createLineBorder(helpText.getBackground(), 5));
        //helpText.set
        
		initMenu();
	}

	private void setupDimensions(int w, int h) {
        Dimension boardDim = new Dimension(w*2/3, h*3/4);
        boardComp.setPreferredSize(boardDim);
        Dimension sideDim = new Dimension(w/6, h);
        eastGridPanel.setPreferredSize(sideDim);
        westGridPanel.setPreferredSize(sideDim);
        Dimension consoleDim = new Dimension(boardDim.width, h/5);
        consolePane.setPreferredSize(consoleDim);
	}
	
    enum LayoutType {
        LAYOUT_DEFAULT, // just buttons on left the board on the right
        LAYOUT_INGAME,    // buttons on lower left, playerinfo on upper left, board upper center, console lower center 
        LAYOUT_CONFIGURE,  // 2 menus of buttons on left
    }
    
	List<Animation> cardAnimations = new ArrayList<Animation>();
    
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
                //JScrollPane buttons = new JScrollPane(menu);
                buttons.add(menu);
                westGridPanel.add(new JSeparator());
                //westGridPanel.add(new JSeparator());
                westGridPanel.add(buttons);
                //cntrBorderPanel.remove(configButtonsPanel);
                //cntrBorderPanel.remove(consolePane);
                //console.clear();
                break;
            }
            case LAYOUT_INGAME:
            {
                eastGridPanel.removeAll();
                westGridPanel.removeAll();

                // NEW WAY
                // basically, the user is always on the left and the remaining players are always on the right

                GUIPlayer userPlayer = getGUIPlayer(1);
                for (Player p : soc.getPlayers()) {
                	if (p instanceof GUIPlayerUser) {
                		userPlayer = (GUIPlayer)p;
                		break;
                	}
                }
                
                if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                	westGridPanel.add(barbarianComp = new BarbarianComponent());
                	JScrollPane sp = new JScrollPane();
                	int num = userPlayer.getPlayerNum();
                	sp.getViewport().add(playerComponents[num] = new PlayerInfoComponentCAK(num, CardLoc.CL_UPPER_LEFT));
                	westGridPanel.add(sp);
                } else {
                	barbarianComp = null;
                	int num = userPlayer.getPlayerNum();
                	westGridPanel.add(playerComponents[num] = new PlayerInfoComponentCAK(num, CardLoc.CL_UPPER_LEFT));
                }
                
                CardLoc [] locs = { CardLoc.CL_UPPER_RIGHT, CardLoc.CL_MIDDLE_RIGHT, CardLoc.CL_LOWER_RIGHT };

                int index = 0;
                for (int i=1; i<=soc.getNumPlayers(); i++) {
                	if (i == userPlayer.getPlayerNum())
                		continue;
                	if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                		eastGridPanel.add(playerComponents[i] = new PlayerInfoComponentCAK(i, locs[index++]));
                	} else {
                		eastGridPanel.add(playerComponents[i] = new PlayerInfoComponentCAK(i, locs[index++]));
                	}
                }
                
                JPanel helpMenuDice = new JPanel();
                helpMenuDice.setLayout(new BorderLayout());
                JPanel dicePanel = null;
                if (getRules().isEnableCitiesAndKnightsExpansion()) {
                	
                	if (getRules().isEnableEventCards()) {
                		diceComps = new ADiceComponent[] {
                				null,
                    			new SixSideDiceComponent(Color.red, Color.yellow),
                    			new EventDiceComponent(),
                		};
                		dicePanel = new EZPanel(new FlowLayout(), new EventCardComponent(diceComps[1], diceComps[2]));
                	} else {
                    	diceComps = new ADiceComponent [] {
                    			new SixSideDiceComponent(Color.yellow, Color.red),
                    			new SixSideDiceComponent(Color.red, Color.yellow),
                    			new EventDiceComponent()
                    	};
                    	diceChoosers = new JSpinner[2];
                    	
                    	diceChoosers[0] = new JSpinner(new SpinnerNumberModel(1, 1, 6, 1));
                    	diceChoosers[1] = new JSpinner(new SpinnerNumberModel(1, 1, 6, 1));
                    	diceChoosers[0].addChangeListener(diceComps[0]);
                    	diceChoosers[1].addChangeListener(diceComps[1]);
                    	diceChoosers[0].setVisible(false);
                    	diceChoosers[1].setVisible(false);
                    	dicePanel = new EZPanel(new FlowLayout(), diceComps[0], diceChoosers[0], diceComps[1], diceChoosers[1], diceComps[2]);
                	}
                } else {
                    diceComps = new ADiceComponent[] {
                    		new SixSideDiceComponent(Color.white, Color.black),
                    		new SixSideDiceComponent(Color.white, Color.black)
                    };
                	if (getRules().isEnableEventCards()) {
                		dicePanel = new EZPanel(new EventCardComponent());
                	} else {
                        dicePanel = new EZPanel(new FlowLayout(), diceComps);
                	}
                }
                //dicePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
                helpMenuDice.add(dicePanel, BorderLayout.NORTH);
                helpMenuDice.add(helpText, BorderLayout.CENTER);
                westGridPanel.add(helpMenuDice);
                JScrollPane menuPanel = new JScrollPane();
                menuPanel.setLayout(new ScrollPaneLayout());// BoxLayout(menuPanel, BoxLayout.Y_AXIS));
                menuPanel.getViewport().add(menu);
                westGridPanel.add(menuPanel);
                break;
            }
            case LAYOUT_CONFIGURE:
                eastGridPanel.removeAll();
                westGridPanel.removeAll();
                JPanel buttons = new JPanel();
                buttons.add(menu);
                JPanel chooser = new JPanel();
                chooser.setLayout(new GridLayout(0,1));
                
                MyRadioButtonGroup<Object> grp = new MyRadioButtonGroup<Object>(chooser) {
                    @Override
                    protected void onChange(Object extra) {
                    	if (extra instanceof TileType) {
                    		boardComp.setPaintMode((TileType)extra);
                    	} else if (extra instanceof PickMode){
                    		boardComp.setPickMode((Integer)playerChooser.getValue(), (PickMode)extra, null);
                    	}
                    }
                };
                for (TileType c : TileType.values()) {
                    grp.addButton(formatString(c.name()), c);
                }
                grp.addButton("Islands", PickMode.PM_ISLAND);
                grp.addButton("Pirate Route", PickMode.PM_PIRATE_ROUTE);
                westGridPanel.add(chooser);
                westGridPanel.add(buttons);
                //cntrBorderPanel.remove(console);
                break;
        }
        
        frame.validate();
        frame.repaint();
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
    
	@SuppressWarnings("serial")
    private void initMenu() {
		if (getBoard().getName().length() == 0) {
			boardNameLabel.setText("Untitled");
		} else {
			boardNameLabel.setText(getBoard().getName());
		}
        log.debug("MenuStack: " + menuStack);
        //menu = new JPanel();
        //menu.setLayout(new GridLayout(0 ,1));
        clearMenu();
		
		if (menuStack.size() > 0) {
    		switch (menuStack.peek()) {
    		case MENU_START:
    		    initLayout(LayoutType.LAYOUT_DEFAULT);
    			menu.add(getMenuOpButton(MenuOp.NEW_GAME));
    			menu.add(getMenuOpButton(MenuOp.RESTORE));
    			menu.add(getMenuOpButton(MenuOp.CONFIG_BOARD));
                menu.add(getMenuOpButton(MenuOp.CONFIG_SETTINGS));
    			menu.add(getMenuOpButton(MenuOp.DEBUG_BOARD));
    			menu.add(getMenuOpButton(MenuOp.SAVE_SCENARIO));
    			menu.add(getMenuOpButton(MenuOp.LOAD_SCENARIO));
    			menu.add(getMenuOpButton(MenuOp.EXIT));
    			break;
    			
    		case MENU_CHOOSE_NUM_PLAYERS:
                initLayout(LayoutType.LAYOUT_DEFAULT);
    			for (int i=0; i<playerColors.length; i++) {
    				menu.add(getMenuOpButton(MenuOp.CHOOSE_NUM_PLAYERS, String.valueOf(i+2), null));
    			}
    			menu.add(getMenuOpButton(MenuOp.QUIT, "Back", "Go back to previous menu"));
    			break;
    			
    		case MENU_CHOOSE_COLOR:
                initLayout(LayoutType.LAYOUT_DEFAULT);
                for (ColorString cs : playerColors) {
    				menu.add(getMenuOpButton(MenuOp.CHOOSE_COLOR, cs.name, null, cs.color));
                }
                menu.add(getMenuOpButton(MenuOp.BACK));
    			break;
    			
    		case MENU_GAME_SETUP:
                initLayout(LayoutType.LAYOUT_INGAME);
    			menu.add(getMenuOpButton(MenuOp.START));
    			menu.add(getMenuOpButton(MenuOp.CONFIG_BOARD));
    			menu.add(getMenuOpButton(MenuOp.CONFIG_SETTINGS));
    			menu.add(getMenuOpButton(MenuOp.BACK));
    			break;
    			
    		case MENU_PLAY_GAME:
                initLayout(LayoutType.LAYOUT_INGAME);
    			break;
    			
    		case MENU_DEBUGGING:
    		{
                initLayout(LayoutType.LAYOUT_DEFAULT);
    		    for (final RenderFlag f : RenderFlag.values()) {
    		        menu.add(
    		            new MyToggleButton(f.name(), boardComp.getRenderFlag(f)) {

                            void onChecked() {
                               boardComp.setRenderFlag(f, true); 
                            }
                            void onUnchecked() {
                                boardComp.setRenderFlag(f, false);
                            }
        		        }
    		        );
    		    }

    		    JPanel choiceButtons = new JPanel();
                choiceButtons.setLayout(new BoxLayout(choiceButtons, BoxLayout.Y_AXIS));
                MyRadioButtonGroup<PickMode> pickChoice = new MyRadioButtonGroup<PickMode>(choiceButtons) {
                    protected void onChange(PickMode mode) {
                        boardComp.setPickMode(getCurPlayerNum(), mode, null);
                    }
                };
                //choiceButtons.add(new JLabel("PICK CHOICE"));
                for (PickMode pm : PickMode.values()) {
                	pickChoice.addButton(pm.name(), pm);
                }
    		    
                choiceButtons.add(new EZPanel(new JLabel("Player:"), playerChooser));
                choiceButtons.add(getMenuOpButton(MenuOp.RESET_BOARD));
                choiceButtons.add(getMenuOpButton(MenuOp.RESET_BOARD_ISLANDS));
                eastGridPanel.removeAll();
                //eastGridPanel.add(new JPanel());
                eastGridPanel.add(choiceButtons);
                
    		    menu.add(getMenuOpButton(MenuOp.BACK));
    		    break;
    		}
    		
    		case MENU_CONFIG_BOARD:
                initLayout(LayoutType.LAYOUT_CONFIGURE);
    			menu.add(getMenuOpButton(MenuOp.LOAD_DEFAULT));
    			menu.add(getMenuOpButton(MenuOp.LOAD_BOARD));
    			menu.add(getMenuOpButton(MenuOp.GEN_HEX_BOARD));
    			menu.add(getMenuOpButton(MenuOp.GEN_RECT_BOARD));
    			menu.add(getMenuOpButton(MenuOp.TRIM_BOARD));
    			menu.add(getMenuOpButton(MenuOp.SAVE_BOARD_AS_DEFAULT));
    			if (getBoard().getName() != null) {
    				if (new File(getBoard().getName()).isFile())
    					menu.add(getMenuOpButton(MenuOp.SAVE_BOARD));
    			}
    			menu.add(getMenuOpButton(MenuOp.SAVE_BOARD_AS));
    			menu.add(getMenuOpButton(MenuOp.BACK));
    			boardComp.setPickMode(0, PickMode.PM_CELLPAINT, null);
    			break;
    			
    		case MENU_CHOOSE_DEFAULT_BOARD_SIZE:
                initLayout(LayoutType.LAYOUT_CONFIGURE);
    			menu.add(getMenuOpButton(MenuOp.GEN_HEX_BOARD_SMALL));
    			menu.add(getMenuOpButton(MenuOp.GEN_HEX_BOARD_MEDIUM));
    			menu.add(getMenuOpButton(MenuOp.GEN_HEX_BOARD_LARGE));
    			menu.add(getMenuOpButton(MenuOp.BACK));
    			break;
                
    		case MENU_CHOOSE_CUSTOM_BOARD_SIZE:
                initLayout(LayoutType.LAYOUT_CONFIGURE);
    			menu.add(getMenuOpButton(MenuOp.GEN_RECT_BOARD_SMALL));
    			menu.add(getMenuOpButton(MenuOp.GEN_RECT_BOARD_MEDIUM));
    			menu.add(getMenuOpButton(MenuOp.GEN_RECT_BOARD_LARGE));
    			menu.add(getMenuOpButton(MenuOp.BACK));
    			break;
    			
    		default:
    			log.error("Unhandled case : " + menuStack.peek());
    
    		
    		}
	    }
		frame.validate();
        frame.repaint();
	}
	
	@SuppressWarnings("serial")
    private void processOp(MenuOp op, OpButton button) {
		
		switch (op) {
			case BACK:
				if (menuStack.size()>0) {
					menuStack.pop();
					initMenu();
				} else {
					button.setEnabled(false);
				}
				boardComp.setPickMode(0, PickMode.PM_NONE, null);
				break;

			case EXIT:
				synchronized (soc) {
					System.exit(0);
				}
				break;

			case DEBUG_BOARD:
				menuStack.push(MenuState.MENU_DEBUGGING);
//				try {
//					getBoard().load(new File(homeFolder, "debugboard.txt").getAbsolutePath());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				initMenu();
				break;

			case RESET_BOARD:
				getBoard().reset();
				break;

			case RESET_BOARD_ISLANDS:
				getBoard().clearIslands();
				break;

			case NEW_GAME:
				newGame();
				initMenu();
				break;

			case RESTORE: {
				try {
					menuStack.push(MenuState.MENU_PLAY_GAME);
					loadGame(saveGameFile);
					setDice(soc.getDice());
					new Thread(this).start();
				} catch (Exception e) {
					e.printStackTrace();
					menuStack.pop();
					button.setEnabled(false);
					e.printStackTrace();
				}
				break;
			}

			case CONFIG_BOARD:
				menuStack.push(MenuState.MENU_CONFIG_BOARD);
				initMenu();
				break;

			case CONFIG_SETTINGS:
				showConfigureGameSettingsPopup(getRules().deepCopy());
				break;

			case CHOOSE_NUM_PLAYERS:
				initPlayers(Integer.parseInt(button.getText()));
				menuStack.pop();
				initMenu();
				break;

			case CHOOSE_COLOR:
				returnValue = button.getText();
				setPlayerColor((Color)button.extra);
				menuStack.pop();
				initMenu();
				break;

			case START:			
				if (getBoard().isReady()) {
					getBoard().assignRandom();
					menuStack.clear();
					menuStack.push(MenuState.MENU_START);
					menuStack.push(MenuState.MENU_PLAY_GAME);
					initMenu();
					new Thread(this).start();
				} 
				break;

			case GEN_HEX_BOARD:
				menuStack.push(MenuState.MENU_CHOOSE_DEFAULT_BOARD_SIZE);
				initMenu();
				break;

			case GEN_HEX_BOARD_SMALL:
				getBoard().generateHexBoard(4, TileType.WATER);
				menuStack.pop();
				initMenu();
				break;

			case GEN_HEX_BOARD_MEDIUM:
				getBoard().generateHexBoard(5, TileType.WATER);
				menuStack.pop();
				initMenu();
				break;

			case GEN_HEX_BOARD_LARGE:
				getBoard().generateHexBoard(6, TileType.WATER);
				menuStack.pop();
				initMenu();
				break;

			case GEN_RECT_BOARD:
				menuStack.push(MenuState.MENU_CHOOSE_CUSTOM_BOARD_SIZE);
				initMenu();
				break;

			case GEN_RECT_BOARD_SMALL:
				getBoard().generateRectBoard(6, TileType.WATER);
				menuStack.pop();
				initMenu();
				break;

			case GEN_RECT_BOARD_MEDIUM:
				getBoard().generateRectBoard(8, TileType.WATER);
				menuStack.pop();
				initMenu();
				break;

			case GEN_RECT_BOARD_LARGE:
				getBoard().generateRectBoard(10, TileType.WATER);
				menuStack.pop();
				initMenu();
				break;

			case TRIM_BOARD:
				getBoard().trim();
				break;

			case SAVE_BOARD_AS_DEFAULT:
				saveBoard(defaultBoardFile.getAbsolutePath());
				break;

			case LOAD_DEFAULT:
				if (loadBoard(defaultBoardFile.getAbsolutePath())) {
					frame.repaint();
				}
				break;

			case SAVE_BOARD:
				saveBoard(getBoard().getName());
				break;

			case SAVE_BOARD_AS: {		
				JFileChooser chooser = new JFileChooser();
				File baseDir = new File(props.getProperty("boardsDirectory", "boards"));
				if (!baseDir.exists()) {
					if (!baseDir.mkdirs()) {
						showOkPopup("ERROR", "Failed to ceate directory tree '" + baseDir + "'");
						break;
					}
				} else if (!baseDir.isDirectory()) {
					showOkPopup("ERROR", "Not a directory '" + baseDir + "'");
					break;
				}
				chooser.setCurrentDirectory(baseDir);
				chooser.setDialogTitle("Save Board");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
				int result = chooser.showSaveDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					baseDir = file.getParentFile();
					String fileName = file.getAbsolutePath();
					if (!fileName.endsWith(".txt"))
						fileName += ".txt";
					saveBoard(fileName);
				}
				break;
			}
			case LOAD_BOARD: {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
				File boardsDir = new File(props.getProperty("boardsDirectory", "boards"));
				if (!boardsDir.isDirectory()) {
					showOkPopup("ERROR", "Boards directory missing");
					break;
				}
				if (boardsDir.list().length == 0) {
					showOkPopup("ERROR", "No Boards in boards directory");
					break;
				}
				chooser.setCurrentDirectory(boardsDir);
				chooser.setDialogTitle("Load Board");
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int result = chooser.showOpenDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					loadBoard(file.getAbsolutePath());
				}
				break;
			}

			case SET_PICKMODE:
				boardComp.setPickMode(getCurPlayerNum(), (PickMode)button.extra, null);
				break;

			case REWIND_GAME: {
				stopGameThread();
				FileUtils.restoreFile(saveGameFile.getAbsolutePath());
				try {
					loadGame(saveGameFile);
					setDice(soc.getDice());
					new Thread(this).start();
					frame.repaint();
				} catch (Exception e) {
					button.setEnabled(false);
				}
				break;
			}
				
				
			case QUIT:
				quitToMainMenu();
				break;

			case CANCEL:
				boardComp.setPickMode(0, PickMode.PM_NONE, null);
				soc.cancel();
				break;

			case CHOOSE_GIVEUP_CARD:
			case CHOOSE_MOVE:
			case CHOOSE_PLAYER:
			case CHOOSE_CARD:
			case CHOOSE_TRADE:
			case CHOOSE_ROAD:
			case CHOOSE_SHIP:
				returnValue = button.extra;
				break;

			case BUILDABLES_POPUP:
			{
				ArrayList<String> columnNamesList = new ArrayList<String>();
				columnNamesList.add("Buildable");
				for (ResourceType r : ResourceType.values()) {
					columnNamesList.add(r.name());
				}
				String [] columnNames = columnNamesList.toArray(new String[columnNamesList.size()]);
				Object [][] rowData = new Object[BuildableType.values().length][];
				for (BuildableType b : BuildableType.values()) {
					rowData[b.ordinal()] = new Object[columnNames.length];
					rowData[b.ordinal()][0] = b.name();
					for (ResourceType r: ResourceType.values())
						rowData[b.ordinal()][r.ordinal()+1] = b.getCost(r);
				}
				JTable table = new JTable(rowData, columnNames);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				JScrollPane view = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				view.getViewport().add(table);
				this.showOkPopup("BUILDABLE", view);
				break;
			}

			case POPUPBUTTON:
				if (((PopupButton)button).doAction()) {
					closePopup();
				}				
				break;

			case SAVE_SCENARIO: {

				String txt = FileUtils.stripExtension(new File(getBoard().getName()).getName());
				if (txt.length() == 0)
					txt = "MyScenario";
				final JTextField nameField = new JTextField(txt);
				EZPanel panel = new EZPanel(new GridLayout(0, 1), new JLabel("Enter Scenario name"), nameField);
				showPopup("Save Current Board and Rules as Scenario", panel, new PopupButton [] {
						new PopupButton("Cancel"),
						new PopupButton("Save") {
							@Override
							public boolean doAction() {
								getBoard().setName(nameField.getText());
								JFileChooser chooser = new JFileChooser();
								File scenarioDir = new File(props.getProperty("screnariosDirectory", "scenarios"));
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
								chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
								int result = chooser.showSaveDialog(frame);
								if (result == JFileChooser.APPROVE_OPTION) {
									File file = chooser.getSelectedFile();
									String fileName = file.getAbsolutePath();
									if (!fileName.endsWith(".txt"))
										fileName += ".txt";
									try {
										soc.saveToFile(new File(fileName));
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								return true;
							}

						}
				});


				break;
			}
			
			case LOAD_SCENARIO: {
				File scenariosDir = new File(props.getProperty("scenariosDirectory", "scenarios"));
				if (!scenariosDir.isDirectory()) {
					showOkPopup("ERROR", "Cant find scenarios directory '" + scenariosDir + "'");
				} else {
					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(scenariosDir);
					chooser.setDialogTitle("Load Scenario");
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
					int result = chooser.showOpenDialog(frame);
					if (result == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						try {
							loadGame(file);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				break;
			}

			case SET_DICE: {
				returnValue = new int[] { diceComps[0].getDie(), diceComps[1].getDie() };
				diceChoosers[0].setVisible(false);
				diceChoosers[1].setVisible(false);
				break;
			}
		
		}

        synchronized (waitObj) {
            waitObj.notify();
        }
	}	
	
	private void loadGame(File file) throws IOException {
		soc.loadFromFile(file);
		initMenu();
		boardComp.setBoard(getBoard());
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
    
    private void stopGameThread() {
    	running = false;
    	synchronized (waitObj) {
    		waitObj.notifyAll();
    	}
    }
    
    public void quitToMainMenu() {
        stopGameThread();
        console.clear();
        boardComp.setPickMode(0, PickMode.PM_NONE, null);
        menuStack.clear();
        menuStack.push(MenuState.MENU_START);
        initMenu();
    }
    
    private void newGame() {
    	soc.reset();
		menuStack.push(MenuState.MENU_GAME_SETUP);
		menuStack.push(MenuState.MENU_CHOOSE_COLOR);
		menuStack.push(MenuState.MENU_CHOOSE_NUM_PLAYERS);    	
    }
    
	private OpButton getMenuOpButton(MenuOp op) {
		return getMenuOpButton(op, op.txt, op.toolTipText);
	}

	public JMultiColoredScrollConsole getConsole() {
		return console;
	}
	
	private OpButton getMenuOpButton(MenuOp op, String txt, String helpText) {
		return getMenuOpButton(op, txt, helpText, null);
	}

	private OpButton getMenuOpButton(MenuOp op, String text, String helpText, Object extra) {
		OpButton button = new OpButton(op, text, extra);
		button.addActionListener(this);
		button.setActionCommand(op.name());
		button.setToolTipText(helpText);
		return button;
	}
	
	private void initPlayers(int numPlayers) {
        soc.clear();
        Player [] players = new Player[numPlayers];
        players[0] = localPlayer = new GUIPlayerUser();
        localPlayer.setColor(playerColors[0].color);
        for (int i=1; i<numPlayers; i++) {
			GUIPlayer p = new GUIPlayerUser();
			p.setColor(playerColors[i].color);
			p.setPlayerNum(i+1);
            players[i] = p;			
		}
		
		// now shuffle the player nums
        for (int i=0; i<numPlayers; i++) {
        	players[i].setPlayerNum(i+1);
        	soc.addPlayer(players[i]);
        }
        soc.initGame();
	}
	
	private void setPlayerColor(Color color) {
		log.debug("setPlayerColor too: " + color);
		// swap the players colors
		assert(color != null);
		Color temp = localPlayer.getColor();
		localPlayer.setColor(color);
		for (int i=1; i<=soc.getNumPlayers(); i++) {
			if (i == localPlayer.getPlayerNum())
				continue;
			GUIPlayer p = getGUIPlayer(i);
			if (p.getColor().equals(color)) {
				p.setColor(temp);
				break;
			}
		}
	}
	
	private boolean saveBoard(String fileName) {
		try {
			getBoard().setName(fileName);
			getBoard().save(fileName);
			boardNameLabel.setText(fileName);
			initMenu();
		} catch (IOException e) {
			log.error(e.getMessage());
			return false;
		}
		return true;
	}
	
	private boolean loadBoard(String fileName) {
		try {
			Board b = new Board();
			b.load(fileName);
			getBoard().copyFrom(b);
			boardNameLabel.setText(fileName);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
	}
	
	public GUIPlayer getGUIPlayer(int playerNum) {
		return (GUIPlayer)soc.getPlayerByPlayerNum(playerNum);
	}
	
	public SOC getSOC() {
		return soc;
	}
	
	private void completeMenu() {
		
	    menu.add(new JSeparator());
	    
		if (soc.canCancel()) {
            menu.add(getMenuOpButton(MenuOp.CANCEL));
        } else {
    	    menu.add(new JLabel(""));
        }
		menu.add(getMenuOpButton(MenuOp.BUILDABLES_POPUP));
		menu.add(getMenuOpButton(MenuOp.REWIND_GAME));
		menu.add(getMenuOpButton(MenuOp.QUIT));
		helpText.setText("<html>" + soc.getHelpText() + "</html>");
        frame.validate();
	}
	
	public GUIPlayer getCurGUIPlayer() {
		return (GUIPlayer)soc.getCurPlayer();
	}
    
	public Color getPlayerColor(int playerNum) {
		if (playerNum < 1)
			return Color.GRAY;
		GUIPlayer p = getGUIPlayer(playerNum);
		if (p == null)
			return playerColors[playerNum-1].color;
		return p.getColor();
	}
    
	public int getCurPlayerNum() {
		if (getCurrentMenu() == MenuState.MENU_DEBUGGING)
			return (Integer)playerChooser.getValue();
        return soc.getCurPlayerNum();
    }

    public String getChooseColorMenu() {
        menuStack.push(MenuState.MENU_CHOOSE_COLOR);
        initMenu();
        return waitForReturnValue(null);
    }
    
    public Vertex chooseVertex(List<Integer> vertices, int playerNum, PickMode mode) {
		clearMenu();
		boardComp.setPickMode(playerNum, mode, vertices);
		completeMenu();
		return waitForReturnValue(null);
    }
	
	public RouteChoiceType getChooseRouteType() {
		clearMenu();
		menu.add(getMenuOpButton(MenuOp.CHOOSE_ROAD, "Roads", "View road options", RouteChoiceType.ROAD_CHOICE));
		menu.add(getMenuOpButton(MenuOp.CHOOSE_SHIP, "Ships", "View ship options", RouteChoiceType.SHIP_CHOICE));
		completeMenu();
		return waitForReturnValue(null);
	}

	public Route getChooseRoadEdge(List<Integer> edges) {
		clearMenu();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_ROAD, edges);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Route getChooseShipEdge(List<Integer> edges) {
		clearMenu();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_SHIP, edges);
		completeMenu();
		return waitForReturnValue(null);
	}	

	public Tile getChooseRobberTile(List<Integer> cells) {
		clearMenu();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_ROBBER, cells);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Tile getChooseMerchantTile(List<Integer> tiles) {
		clearMenu();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_MERCHANT, tiles);
		completeMenu();
		return waitForReturnValue(null);
	}

	public Tile getChooseInventorTile(List<Integer> tiles) {
		clearMenu();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_CELL, tiles);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	// called from run thread
	public MoveType getChooseMoveMenu(List<MoveType> moves) {
		clearMenu();
		Iterator<MoveType> it = moves.iterator();
		while (it.hasNext()) {
			MoveType move = it.next();
			OpButton button = null;
			menu.add(button = getMenuOpButton(MenuOp.CHOOSE_MOVE, move.niceText, move.helpText, move));
			button.setToolTipText(move.helpText);
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	// called from run thread
	public <T extends Enum<T>> T getChooseEnum(List<T> choices) {
		clearMenu();
		Iterator<T> it = choices.iterator();
		while (it.hasNext()) {
			Enum<T> choice= it.next();
			menu.add(getMenuOpButton(MenuOp.CHOOSE_MOVE, choice.name(), null, choice));
		}
		completeMenu();
		return waitForReturnValue(null);
	}	
	
	public boolean getSetDiceMenu(int [] die, int num) {
		clearMenu();
		diceChoosers[0].setVisible(true);
		diceChoosers[1].setVisible(true);
		menu.add(getMenuOpButton(MenuOp.SET_DICE));
		completeMenu();
		int [] result = (int[])waitForReturnValue(null);
		if (result != null) {
			Utils.copyElems(die, result);
			return true;
		}
		return false;
	}
	
	public Player getChoosePlayerToTakeCardFromMenu(List<Integer> players) {
		clearMenu();
		for (int num : players) {
			Player player = getGUIPlayer(num);
			menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + player.getTotalCardsLeftInHand() + " Cards", null, player));
		}
		completeMenu();
		return waitForReturnValue(null);
	}

	public Player getChoosePlayerToGiftCardToMenu(List<Integer> players) {
		clearMenu();
		for (int num : players) {
			Player player = getGUIPlayer(num);
			menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + player.getTotalCardsLeftInHand() + " Cards", null, player));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Player getChoosePlayerKnightForDesertion(List<Integer> players) {
		clearMenu();
		for (int num : players) {
			Player player = getGUIPlayer(num);
			int numKnights = getBoard().getNumKnightsForPlayer(player.getPlayerNum());
			menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + numKnights + " Knights", null, player));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Player getChoosePlayerToSpyOn(List<Integer> players) {
		clearMenu();
		for (int num : players) {
			Player player = getGUIPlayer(num);
			menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + player.getUnusedCardCount(CardType.Progress) + " Progress Cards", null, player));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Card getChooseCardMenu(List<Card> cards) {
		clearMenu();
		for (Card type : cards) {
			menu.add(getMenuOpButton(MenuOp.CHOOSE_CARD, type.getName(), type.getHelpText(), type));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Trade getChooseTradeMenu(List<Trade> trades) {
		clearMenu();
		Iterator<Trade> it = trades.iterator();
		while (it.hasNext()) {
			Trade trade = it.next();	
			String str = trade.getType().name() + " X " + trade.getAmount();
			menu.add(getMenuOpButton(MenuOp.CHOOSE_TRADE, str, null, trade));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public void spinDice(int ... dieToSpin) {
		clearMenu();
		int delay = 10;
		long startTime = System.currentTimeMillis();
		while (true) {
			long curTime = System.currentTimeMillis();
			if (curTime - startTime > diceSpinTimeSeconds*1000)
				break;
			for (int i=0; i<dieToSpin.length; i++) {
				if (dieToSpin[i] > 0) {
					diceComps[i].setDie(Utils.rand() % 6 + 1);
				}
			}
			try {
				Thread.sleep(delay);
			} catch (Exception e) {}
			delay += 20;
		}
	}
	
	public void showCustomMenu(Component [] components) {
		clearMenu();
		for (int i=0; i<components.length; i++)
			menu.add(components[i]);
		completeMenu();
	}
	
	@SuppressWarnings("unchecked")
    public <T> T waitForReturnValue(T defaultValue) {
		returnValue = defaultValue;
		try {
			synchronized (waitObj) {
				waitObj.wait();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return (T)returnValue;	
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
    
    public void showPopup(String title, JComponent view, PopupButton [] button) {
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
        JLabel label = new JWrapLabel(msg);

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

    public void showConfigureGameSettingsPopup(final Rules rules) {
        final JPanel view = new JPanel();
        JScrollPane panel = new JScrollPane();
        panel.setMinimumSize(new Dimension(300, 300));
        panel.getViewport().add(view);
        view.setLayout(new GridLayout(0, 2));

        final HashMap<JComponent, Field> components = new HashMap<JComponent, Field>();
        
        try {
        	
        	Field [] fields = Rules.class.getDeclaredFields();
        	for (Field f : fields) {
        		Annotation [] anno = f.getAnnotations();
        		for (Annotation a : anno) {
        			if (a.annotationType().equals(RuleVariable.class)) {
        				f.setAccessible(true);
        				RuleVariable ruleVar = (RuleVariable)a;
        				if (ruleVar.separator().length() > 0) {
        					view.add(new JLabel(ruleVar.separator()));
        			        view.add(new JLabel());
        				}
        				
        				if (f.getType().equals(boolean.class)) {
        					view.add(new JLabel());
        			        JToggleButton button = new JToggleButton(ruleVar.description(), f.getBoolean(rules));
        			        view.add(button);
        			        components.put(button,  f);
        				} else if (f.getType().equals(int.class)) {
        			        view.add(new JLabel(ruleVar.description()));
        			        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(f.getInt(rules), ruleVar.minValue(), ruleVar.maxValue(), ruleVar.valueStep()));
        			        view.add(spinner);
        			        components.put(spinner, f);
        				} else {
        					System.err.println("Dont know how to handle field type:" + f.getType());
        				}
        				break;
        			}
        		}
        	}
        	
        	PopupButton [] buttons = new PopupButton[4];
            buttons[0] = new PopupButton("View\nDefaults") {
                public boolean doAction() {
                    new Thread(new Runnable() {
                        public void run() {
                            showConfigureGameSettingsPopup(new Rules());
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
		MenuOp op = MenuOp.valueOf(e.getActionCommand());
		OpButton button = (OpButton)e.getSource();
		processOp(op, button);
		frame.repaint();
	}
	

	@Override
    public void onPick(PickMode mode, int pickedValue) {
        try {
            
            switch (this.menuStack.peek()) {
                case MENU_DEBUGGING:
                	processDebugPick(mode, pickedValue);
                    break;
                    
                case MENU_CONFIG_BOARD:
                	switch (mode) {
                		case PM_CELLPAINT:
                			break;
                		case PM_ISLAND: {
                			int islandNum = getBoard().getTile(pickedValue).getIslandNum();
                			if (getBoard().getTile(pickedValue).getIslandNum() > 0) {
                				// remove the island
                				getBoard().removeIsland(islandNum);
                			} else {
                            	islandNum = getBoard().createIsland(pickedValue);
                            	console.addText(Color.black, "Found island: " + islandNum);
                			}
                        	break;
                		}
                	}
                    break;
                default:
            
                    returnValue = null;
                    switch (mode) {
                    	case PM_SHIP:
                    		getBoard().getRoute(pickedValue).setShip(true);
                        case PM_ROAD:
                            returnValue = getBoard().getRoute(pickedValue);
                            break;
                            
                        case PM_SETTLEMENT:
                        case PM_CITY:
                        case PM_WALLED_CITY:
                        case PM_METROPOLIS_TRADE:
                        case PM_METROPOLIS_POLITICS:
                        case PM_METROPOLIS_SCIENCE:
                        case PM_KNIGHT:
                        case PM_ACTIVATE_KNIGHT:
                        case PM_PROMOTE_KNIGHT:
                            returnValue = getBoard().getVertex(pickedValue);
                            break;
                            
                        case PM_CELL:
                        case PM_ROBBER:
                        case PM_MERCHANT:
                            returnValue = getBoard().getTile(pickedValue);
                            break;
                            
                    }
                    
                    boardComp.setPickMode(0, PickMode.PM_NONE, null);
                    synchronized (waitObj) {
                        waitObj.notify();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void componentHidden(ComponentEvent arg0) {}
	public void componentMoved(ComponentEvent arg0) {
        Component comp = arg0.getComponent();
        props.setProperty("gui.x", comp.getX());
        props.setProperty("gui.y", comp.getY());
        //log.debug("Moved too : " + frame.getX() + " x " + frame.getY()); 
    }
	public void componentResized(ComponentEvent arg0) {
	    Component comp = frame;
        props.setProperty("gui.w", comp.getWidth());
        props.setProperty("gui.h", comp.getHeight());
        setupDimensions(comp.getWidth(), comp.getHeight());
        //initMenu();
        frame.doLayout();
        frame.validate();
        //frame.repaint();
        log.debug("Resized too : " + frame.getWidth() + " x " + frame.getHeight()); 
	}
	public void componentShown(ComponentEvent arg0) {}

	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {
		synchronized (soc) {
			System.exit(0);
		}
    }
	public void windowClosing(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	
	@Override
	public void run() {
		log.debug("Entering thread");
		assert(!running);
		running = true;
		try {
			while (running && soc != null) {
				soc.runGame();
				if (running && soc.isGoodForSave() && soc.getCurGuiPlayer() instanceof GUIPlayerUser)
					synchronized (soc) {
						FileUtils.backupFile(saveGameFile.getAbsolutePath(), 10);
						soc.save(saveGameFile.getAbsolutePath());
					}
				frame.repaint();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			quitToMainMenu();
		}
		running = false;
		log.debug("Exiting thread");
	}

    public BoardComponent getBoardComponent() {
        return this.boardComp;
    }

    public void logDebug(String msg) {
        log.debug(msg);
    }

    public void logError(String msg) {
        log.error(msg);
    }

    public void logInfo(String string) {
        log.info(string);
    }

    public MenuState getCurrentMenu() {
    	if (menuStack.size() == 0)
    		return MenuState.MENU_START;
    	return menuStack.peek();
    }
    
    GUIProperties getProps() {
    	return this.props;
    }

	public void setDice(int ... die) {
		if (diceComps != null) {
    		for (int i=0; i<die.length; i++) {
    			if (die[i] > 0)
    				diceComps[i].setDie(die[i]);
    		}
		}
	}
	
	private void processDebugPick(PickMode mode, int pickedValue) {
		Vertex v = null;
		Tile t = null;
		Route r = null;
		switch (mode) {
			case PM_ACTIVATE_KNIGHT:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				if (v.getType().isKnight()) {
					if (v.getType().isKnightActive())
						v.setType(v.getType().deActivatedType());
					else
						v.setType(v.getType().activatedType());
				} else {
					v.setType(VertexType.BASIC_KNIGHT_ACTIVE);
				}
				break;
			case PM_CELLPAINT:
			case PM_CELL:
				t = getBoard().getTile(pickedValue);
				t.setType(Utils.incrementEnum(t.getType(), TileType.values()));
				break;
			case PM_CITY:
				v = getBoard().getVertex(pickedValue);
				v.setType(VertexType.CITY);
				v.setPlayer(getCurPlayerNum());
				break;
			case PM_EDGE:
				r = getBoard().getRoute(pickedValue);
				getBoard().setPlayerForRoute(r, getCurPlayerNum());
				if (!r.isShip()) {
					if (r.isDamaged()) {
						r.setShip(true);
						r.setDamaged(false);
					} else {
						r.setDamaged(true);
					}
				} else {
					r.setShip(true);
				}
				break;
			case PM_ISLAND:
				break;
			case PM_KNIGHT:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				switch (v.getType()) {
					case BASIC_KNIGHT_INACTIVE:
						v.setType(VertexType.BASIC_KNIGHT_ACTIVE);
						break;
					case BASIC_KNIGHT_ACTIVE:
						v.setType(VertexType.BASIC_KNIGHT_INACTIVE);
						break;
					case STRONG_KNIGHT_INACTIVE:
						v.setType(VertexType.STRONG_KNIGHT_ACTIVE);
						break;
					case STRONG_KNIGHT_ACTIVE:
						v.setType(VertexType.STRONG_KNIGHT_INACTIVE);
						break;
					case MIGHTY_KNIGHT_INACTIVE:
						v.setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
						break;
					case MIGHTY_KNIGHT_ACTIVE:
						v.setType(VertexType.MIGHTY_KNIGHT_INACTIVE);
						break;
					default:
						v.setType(VertexType.BASIC_KNIGHT_INACTIVE);
						break;
				}
				break;
			case PM_MERCHANT:
				getBoard().setMerchant(pickedValue, getCurPlayerNum());
				break;
			case PM_METROPOLIS_POLITICS:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				v.setType(VertexType.METROPOLIS_POLITICS);
				break;
			case PM_METROPOLIS_SCIENCE:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				v.setType(VertexType.METROPOLIS_SCIENCE);
				break;
			case PM_METROPOLIS_TRADE:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				v.setType(VertexType.METROPOLIS_TRADE);
				break;
			case PM_NONE:
				break;
			case PM_PATH:
				break;
			case PM_PROMOTE_KNIGHT:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				switch (v.getType()) {
					case BASIC_KNIGHT_ACTIVE:
						v.setType(VertexType.STRONG_KNIGHT_ACTIVE);
						break;
						
					case BASIC_KNIGHT_INACTIVE:
						v.setType(VertexType.STRONG_KNIGHT_INACTIVE);
						break;
						
					case STRONG_KNIGHT_ACTIVE:
						v.setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
						break;
						
					case STRONG_KNIGHT_INACTIVE:
						v.setType(VertexType.MIGHTY_KNIGHT_INACTIVE);
						break;
					case MIGHTY_KNIGHT_ACTIVE:
						v.setType(VertexType.BASIC_KNIGHT_ACTIVE);
						break;
					default:
					case MIGHTY_KNIGHT_INACTIVE:
						v.setType(VertexType.BASIC_KNIGHT_INACTIVE);
						break;
				}
				break;
			case PM_ROAD:
				r = getBoard().getRoute(pickedValue);
				if (r.getPlayer() > 0) {
					getBoard().setPlayerForRoute(r, 0);
					r.setShip(false);
				} else {
					getBoard().setPlayerForRoute(r, getCurPlayerNum());
					r.setShip(false);
				}
				break;
			case PM_ROBBER:
				t = getBoard().getTile(pickedValue);
            	if (t.isWater())
            		getBoard().setPirate(pickedValue);
            	else
            		getBoard().setRobber(pickedValue);
				break;
			case PM_ROUTE:
				break;
			case PM_ROUTE2:
				break;
			case PM_SETTLEMENT:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				v.setType(VertexType.SETTLEMENT);
				break;
			case PM_MOVABLE_SHIPS:
			case PM_SHIP:
				r = getBoard().getRoute(pickedValue);
				if (r.getPlayer() > 0) {
					getBoard().setPlayerForRoute(r, 0);
					r.setShip(false);
				} else {
					getBoard().setPlayerForRoute(r, getCurPlayerNum());
					r.setShip(true);
				}
				break;
			case PM_VERTEX:
				v = getBoard().getVertex(pickedValue);
				v.setType(Utils.incrementEnum(v.getType(), VertexType.values()));
				if (v.getType() == VertexType.OPEN) {
					v.setPlayer(0);
				} else {
					v.setPlayer(getCurPlayerNum());
				}
				break;
			case PM_WALLED_CITY:
				v = getBoard().getVertex(pickedValue);
				v.setPlayer(getCurPlayerNum());
				v.setType(VertexType.WALLED_CITY);
				break;
		}
		getBoard().clearRouteLenCache();
        console.addText(Color.BLACK, "Road Length: " + getBoard().computeMaxRouteLengthForPlayer(getCurPlayerNum(), getRules().isEnableRoadBlock()));
        getBoard().findAllPairsShortestPathToDiscoverables(getCurPlayerNum());
        try {
        	getBoard().save(new File(homeFolder, "debugboard.txt").getAbsolutePath());
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
}

