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
//import cc.game.soc.net.SOCGameCommand;
import cc.game.soc.swing.BoardComponent.PickMode;
import cc.game.soc.swing.BoardComponent.RenderFlag;
import cc.lib.game.Utils;
import cc.lib.swing.AWTUtils;
import cc.lib.swing.EZPanel;
import cc.lib.swing.ImageMgr;
import cc.lib.swing.JMultiColoredScrollConsole;
import cc.lib.swing.JWrapLabel;

public class GUI implements ActionListener, ComponentListener, WindowListener, Runnable, BoardComponent.BoardListener {

    final Logger log = Logger.getLogger(GUI.class);
    
    static GUI instance;

	public static void main(String [] args)  {
		JFrame frame = new JFrame();
		try {
			Utils.setDebugEnabled(true);
			GUIProperties props = new GUIProperties();
            props.load("gui.properties");
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

	private class SpinDiceThread implements Runnable {
	    public void run() {
	        menu.removeAll();
	        int delay = 10;
	        long startTime = System.currentTimeMillis();
	        while (true) {
	            long curTime = System.currentTimeMillis();
	            if (curTime - startTime > diceSpinTimeSeconds*1000)
	                break;
	            for (ADiceComponent comp : diceComps) {
	            	comp.setDie(Utils.rand() % 6 + 1);
	            }
	            try {
	                Thread.sleep(delay);
	            } catch (Exception e) {}
	            delay += 20;
	        }
	        synchronized (waitObj) {
	            waitObj.notify();
	        }
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
	private ADiceComponent [] diceComps;// = new ADiceComponent[3];
	private JSpinner [] diceChoosers;
	private final GUIProperties props;
	private final ImageMgr images;
	
	private Board board;

	private Stack<MenuState> menuStack = new Stack<MenuState>();
	private GUIPlayerUser localPlayer;
	private boolean running;
	private Object returnValue = null;
	private Object waitObj = this;
	private Container frame;
	private JFrame popup;
	private JPanel westBorderPanel = new JPanel();
	private JPanel cntrBorderPanel = new JPanel();
	
	private JPanel eastGridPanel = new JPanel();
	private JPanel westGridPanel = new JPanel();
	private JScrollPane consolePane;
	private JSpinner playerChooser;
	private JLabel helpText;
	
	final static class ColorString {
		final Color color;
		final String name;
		
		ColorString(Color color, String name) {
			this.color = color;
			this.name = name;
		}
	}
	
	private ColorString [] playerColors;

    private String defaultBoardFileName;
    private String saveGameFileName;
    private String saveRulesFileName;
    
    private float diceSpinTimeSeconds;
    
    private Rules rules;
    private JLabel boardNameLabel = new JLabel("Untitled");
    
	public GUI(Container frame, final GUIProperties props) throws IOException {
		instance = this;
		
		this.frame = frame;
		this.props = props;
		soc = new SOCGUI(this);
		
		board = soc.getBoard();
		rules = soc.getRules();
        defaultBoardFileName = props.getProperty("gui.defaultBoardFilename", "soc_def_board.txt");
        saveGameFileName = props.getProperty("gui.saveGameFileName", "socsavegame.txt");
        saveRulesFileName = props.getProperty("gui.saveRulesFileName", "socrules.txt");
        diceSpinTimeSeconds = props.getFloatProperty("gui.diceSpinTimeSeconds", 3);
        
        File ruleFile = new File(saveRulesFileName);
        if (ruleFile.exists()) {
        	rules.loadFromFile(ruleFile);
        }
        
        menuStack.push(MenuState.MENU_START);
		if (!loadBoard(defaultBoardFileName)) {
			//board.generateRectBoard(8);
			board.generateDefaultBoard();
			if (!board.isFinalized())
				menuStack.push(MenuState.MENU_CONFIG_BOARD);
			else
				saveBoard(defaultBoardFileName);
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
        boardComp = new BoardComponent(this, board, images);
        
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
        eastGridPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        westGridPanel.setLayout(new GridLayout(0,1));
        westGridPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        
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
                
                westGridPanel.add(new PlayerInfoComponent2(userPlayer));
                userPlayer.loc = GUIPlayer.CardLoc.CL_UPPER_LEFT;
                
                GUIPlayer.CardLoc [] locs = { GUIPlayer.CardLoc.CL_UPPER_RIGHT, GUIPlayer.CardLoc.CL_MIDDLE_RIGHT, GUIPlayer.CardLoc.CL_LOWER_RIGHT };

                int index = 0;
                for (int i=1; i<=soc.getNumPlayers(); i++) {
                	if (i == userPlayer.getPlayerNum())
                		continue;
                	GUIPlayer p = getGUIPlayer(i);
                	eastGridPanel.add(new PlayerInfoComponent2(p));
                	p.loc = locs[index++];
                }
                
                JPanel helpMenuDice = new JPanel();
                helpMenuDice.setLayout(new BorderLayout());
                JPanel dicePanel = null;
                if (rules.isEnableCitiesAndKnightsExpansion()) {
                	diceComps = new ADiceComponent [] {
                			new SixSideDiceComponent(Color.red, Color.yellow),
                			new SixSideDiceComponent(Color.yellow, Color.red),
                			new EventDiceComponent(images, Color.white)
                	};
                	diceChoosers = new JSpinner[2];
                	
                	diceChoosers[0] = new JSpinner(new SpinnerNumberModel(0, 1, 6, 1));
                	diceChoosers[1] = new JSpinner(new SpinnerNumberModel(0, 1, 6, 1));
                	diceChoosers[0].addChangeListener(diceComps[0]);
                	diceChoosers[1].addChangeListener(diceComps[1]);
                	diceChoosers[0].setVisible(false);
                	diceChoosers[1].setVisible(false);
                	dicePanel = new EZPanel(new FlowLayout(), diceComps[0], diceChoosers[0], diceComps[1], diceChoosers[1], diceComps[2]);
                } else {
                    diceComps = new ADiceComponent[] {
                    		new SixSideDiceComponent(Color.white, Color.black),
                    		new SixSideDiceComponent(Color.white, Color.black)
                    };
                    dicePanel = new EZPanel(new FlowLayout(), diceComps);
                }
                //dicePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
                helpMenuDice.add(dicePanel, BorderLayout.NORTH);
                helpMenuDice.add(helpText, BorderLayout.CENTER);
                westGridPanel.add(helpMenuDice);
                JPanel menuPanel = new JPanel();
                menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
                menuPanel.add(menu);
                westGridPanel.add(menuPanel);
                // console
                //cntrBorderPanel.add(consolePane, BorderLayout.SOUTH);                
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
		if (board.getName().length() == 0) {
			boardNameLabel.setText("Untitled");
		} else {
			boardNameLabel.setText(board.getName());
		}
        log.debug("MenuStack: " + menuStack);
        //menu = new JPanel();
        //menu.setLayout(new GridLayout(0 ,1));
        menu.removeAll();
		
		if (menuStack.size() > 0) {
    		switch (menuStack.peek()) {
    		case MENU_START:
    		    initLayout(LayoutType.LAYOUT_DEFAULT);
    			menu.add(getMenuOpButton(MenuOp.NEW));
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
    				menu.add(getMenuOpButton(MenuOp.CHOOSE_NUM_PLAYERS, String.valueOf(i+2)));
    			}
    			menu.add(getMenuOpButton(MenuOp.QUIT, "Back"));
    			break;
    			
    		case MENU_CHOOSE_COLOR:
                initLayout(LayoutType.LAYOUT_DEFAULT);
                for (ColorString cs : playerColors) {
    				menu.add(getMenuOpButton(MenuOp.CHOOSE_COLOR, cs.name, cs.color));
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
                    	initPickMode(mode);
                    }
                };
                //choiceButtons.add(new JLabel("PICK CHOICE"));
                for (PickMode pm : PickMode.values()) {
                	pickChoice.addButton(pm.name(), pm);
                }
                eastGridPanel.removeAll();
                //eastGridPanel.add(new JPanel());
                eastGridPanel.add(choiceButtons);
    		    
                JPanel menu2 = new JPanel();
                menu2.setLayout(new BoxLayout(menu2, BoxLayout.Y_AXIS));
                menu2.add(new EZPanel(new JLabel("Player:"), playerChooser));
                menu2.add(getMenuOpButton(MenuOp.RESET_BOARD));
                menu2.add(getMenuOpButton(MenuOp.RESET_BOARD_ISLANDS));
                eastGridPanel.add(menu2);
                
    		    menu.add(getMenuOpButton(MenuOp.BACK));
    		    break;
    		}
    		
    		case MENU_CONFIG_BOARD:
                initLayout(LayoutType.LAYOUT_CONFIGURE);
    			menu.add(getMenuOpButton(MenuOp.LOAD_DEFAULT));
    			menu.add(getMenuOpButton(MenuOp.LOAD_BOARD));
    			menu.add(getMenuOpButton(MenuOp.GEN_HEX_BOARD));
    			menu.add(getMenuOpButton(MenuOp.GEN_RECT_BOARD));
    			menu.add(getMenuOpButton(MenuOp.FINALIZE_BOARD));
    			menu.add(getMenuOpButton(MenuOp.SAVE_BOARD_AS_DEFAULT));
    			if (board.getName() != null) {
    				if (new File(board.getName()).isFile())
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
				//loadAiDebugSession();
				initMenu();
				break;

			case RESET_BOARD:
				board.reset();
				break;

			case RESET_BOARD_ISLANDS:
				board.clearIslands();
				break;

			case NEW:
				newGame();
				initMenu();
				break;

			case RESTORE:
				if (soc.load(saveGameFileName)) {
					board = soc.getBoard();
					boardComp.setBoard(board);
					menuStack.push(MenuState.MENU_PLAY_GAME);
					initMenu();
					new Thread(this).start();
				} else {
					button.setEnabled(false);
				}
				break;

			case CONFIG_BOARD:
				menuStack.push(MenuState.MENU_CONFIG_BOARD);
				initMenu();
				break;

			case CONFIG_SETTINGS:
				showConfigureGameSettingsPopup(rules.deepCopy());
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
				if (board.isFinalized()) {
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
				board.generateHexBoard(4);
				menuStack.pop();
				initMenu();
				break;

			case GEN_HEX_BOARD_MEDIUM:
				board.generateHexBoard(5);
				menuStack.pop();
				initMenu();
				break;

			case GEN_HEX_BOARD_LARGE:
				board.generateHexBoard(6);
				menuStack.pop();
				initMenu();
				break;

			case GEN_RECT_BOARD:
				menuStack.push(MenuState.MENU_CHOOSE_CUSTOM_BOARD_SIZE);
				initMenu();
				break;

			case GEN_RECT_BOARD_SMALL:
				board.generateRectBoard(6);
				menuStack.pop();
				initMenu();
				break;

			case GEN_RECT_BOARD_MEDIUM:
				board.generateRectBoard(8);
				menuStack.pop();
				initMenu();
				break;

			case GEN_RECT_BOARD_LARGE:
				board.generateRectBoard(10);
				menuStack.pop();
				initMenu();
				break;

			case FINALIZE_BOARD:
				board.finalizeBoard();
				break;

			case SAVE_BOARD_AS_DEFAULT:
				saveBoard(defaultBoardFileName);
				break;

			case LOAD_DEFAULT:
				if (loadBoard(defaultBoardFileName)) {
					frame.repaint();
				}
				break;

			case SAVE_BOARD:
				saveBoard(board.getName());
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

				String txt = board.getName();
				if (txt.length() == 0)
					txt = "MyScenario";
				final JTextField nameField = new JTextField(txt);
				EZPanel panel = new EZPanel(new GridLayout(0, 1), new JLabel("Enter Scenario name"), nameField);
				showPopup("Save Current Board and Rules as Scenario", panel, new PopupButton [] {
						new PopupButton("Cancel"),
						new PopupButton("Save") {
							@Override
							public boolean doAction() {
								SOC soc = new SOC();
								Board b = board.deepCopy();
								b.setName(nameField.getText());
								soc.setBoard(b);
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
					chooser.setDialogTitle("Save Scenario");
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
					int result = chooser.showOpenDialog(frame);
					if (result == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						try {
							soc.loadFromFile(file);
							board = soc.getBoard();
							boardComp.setBoard(board);
							newGame();
							initMenu();
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
        running = false;
//        soc.clear();
//        board.reset();
        console.clear();
        boardComp.setPickMode(0, PickMode.PM_NONE, null);
//        for (ADiceComponent comp : diceComps) {
//        	comp.setDie(0);
//        }
        menuStack.clear();
        menuStack.push(MenuState.MENU_START);
        initMenu();
    }
    
    private void newGame() {
    	soc.clear();
		soc.setBoard(board);
		menuStack.push(MenuState.MENU_GAME_SETUP);
		menuStack.push(MenuState.MENU_CHOOSE_COLOR);
		menuStack.push(MenuState.MENU_CHOOSE_NUM_PLAYERS);    	
    }
    
	private OpButton getMenuOpButton(MenuOp op) {
		return getMenuOpButton(op, op.txt, null);
	}

	public JMultiColoredScrollConsole getConsole() {
		return console;
	}
	
	private OpButton getMenuOpButton(MenuOp op, String txt) {
		return getMenuOpButton(op, txt, null);
	}

	private OpButton getMenuOpButton(MenuOp op, String text, Object extra) {
		OpButton button = new OpButton(op, text, extra);
		button.addActionListener(this);
		button.setActionCommand(op.name());
		return button;
	}
	
	private void initPlayers(int numPlayers) {
        soc.clear();
        Player [] players = new Player[numPlayers];
        players[0] = localPlayer = new GUIPlayerUser();
        localPlayer.setColor(playerColors[0].color);
        for (int i=1; i<numPlayers; i++) {
			GUIPlayer p = new GUIPlayer();
			p.setColor(playerColors[i].color);
			p.setPlayerNum(i+1);
            players[i] = p;			
		}
		
		// now shuffle the player nums
        Utils.shuffle(players);
        for (int i=0; i<numPlayers; i++) {
        	players[i].setPlayerNum(i+1);
        	soc.addPlayer(players[i]);
        }
		
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
			if (!board.isFinalized())
				board.finalizeBoard();
			board.setName(fileName);
			board.save(fileName);
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
			board.load(fileName);
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
    
	public Vertex getChooseCityVertex(List<Integer> vertices) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_CITY, vertices);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Vertex getChooseCityWallVertex(List<Integer> vertices) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_WALLED_CITY, vertices);
		completeMenu();
		return waitForReturnValue(null);
	}
	

	public Vertex getChooseSettlementVertex(List<Integer> vertices) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_SETTLEMENT, vertices);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public RouteChoiceType getChooseRouteType() {
		menu.removeAll();
		menu.add(getMenuOpButton(MenuOp.CHOOSE_ROAD, "Roads", RouteChoiceType.ROAD_CHOICE));
		menu.add(getMenuOpButton(MenuOp.CHOOSE_SHIP, "Ships", RouteChoiceType.SHIP_CHOICE));
		completeMenu();
		return waitForReturnValue(null);
	}

	public Route getChooseRoadEdge(List<Integer> edges) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_ROAD, edges);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Route getChooseShipEdge(List<Integer> edges) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_SHIP, edges);
		completeMenu();
		return waitForReturnValue(null);
	}	

	public Tile getChooseRobberTile(List<Integer> cells) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_ROBBER, cells);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Tile getChooseMerchantTile(List<Integer> tiles) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_MERCHANT, tiles);
		completeMenu();
		return waitForReturnValue(null);
	}

	public Tile getChooseInventorTile(List<Integer> tiles) {
		menu.removeAll();
		boardComp.setPickMode(getCurPlayerNum(), PickMode.PM_CELL, tiles);
		completeMenu();
		return waitForReturnValue(null);
	}
	
	// called from run thread
	public MoveType getChooseMoveMenu(List<MoveType> moves) {
		menu.removeAll();
		Iterator<MoveType> it = moves.iterator();
		while (it.hasNext()) {
			MoveType move = it.next();
			OpButton button = null;
			menu.add(button = getMenuOpButton(MenuOp.CHOOSE_MOVE, move.niceText, move));
			button.setToolTipText(move.helpText);
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	// called from run thread
	public <T extends Enum<T>> T getChooseEnum(List<T> choices) {
		menu.removeAll();
		Iterator<T> it = choices.iterator();
		while (it.hasNext()) {
			Enum<T> choice= it.next();
			menu.add(getMenuOpButton(MenuOp.CHOOSE_MOVE, choice.name(), choice));
		}
		completeMenu();
		return waitForReturnValue(null);
	}	
	
	public boolean getSetDiceMenu(int [] die, int num) {
		menu.removeAll();
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
	
	public Player getChoosePlayerToTakeCardFromMenu(List<Player> players) {
		menu.removeAll();
		Iterator<Player> it = players.iterator();
		while (it.hasNext()) {
			Player player = it.next();
			menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, "Player " + player.getPlayerNum() + " X " + player.getTotalCardsLeftInHand(), player));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Card getChooseCardMenu(List<Card> cards) {
		menu.removeAll();
		for (Card type : cards) {
			menu.add(getMenuOpButton(MenuOp.CHOOSE_CARD, type.getName(), type));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Trade getChooseTradeMenu(List<Trade> trades) {
		menu.removeAll();
		Iterator<Trade> it = trades.iterator();
		while (it.hasNext()) {
			Trade trade = it.next();	
			String str = trade.getType().name() + " X " + trade.getAmount();
			menu.add(getMenuOpButton(MenuOp.CHOOSE_TRADE, str, trade));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public void spinDice() {
	    new Thread(new SpinDiceThread()).start();
	}
	
	public void showCustomMenu(Component [] components) {
		menu.removeAll();
		for (int i=0; i<components.length; i++)
			menu.add(components[i]);
		completeMenu();
	}
	
	@SuppressWarnings("unchecked")
    public
	<T> T waitForReturnValue(T defaultValue) {
		returnValue = defaultValue;
		try {
			synchronized (waitObj) {
				waitObj.wait();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (this.running)
			menu.removeAll();
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
        //Dimension dim = popup.getSize();
        /*
        if (dim.getWidth() < 100)
            dim.setSize(100, dim.getHeight());
        if (dim.getHeight() < 100)
            dim.setSize(dim.getWidth(), 100);
        popup.setSize(dim);
        */
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
        	
        	PopupButton [] buttons = new PopupButton[3];
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
                		
                		rules.saveToFile(new File(saveRulesFileName));
                		GUI.this.rules.copyFrom(rules);
                	} catch (Exception e) {
                		e.printStackTrace();
                	}
                    return true;
                }
            };
            
            buttons[2] = new PopupButton("Cancel");
            
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
	
    private void initPickMode(PickMode mode) {
    	List<Integer> pickableIndices = null;
    	switch (mode) {
			case PM_CELL:
				break;
			case PM_CELLPAINT:
				break;
			case PM_CITY:
				pickableIndices = SOC.computeCityVertxIndices(getCurPlayerNum(), board);
				pickableIndices.addAll(board.getCitiesForPlayer(getCurPlayerNum()));
				break;
			case PM_WALLED_CITY:
				pickableIndices = SOC.computeCityWallVertexIndices(getCurPlayerNum(), board);
				pickableIndices.addAll(board.getCitiesForPlayer(getCurPlayerNum()));
				break;
			case PM_EDGE:
				break;
			case PM_ISLAND:
				break;
			case PM_MOVABLE_SHIPS:
				pickableIndices = SOC.computeOpenRouteIndices(getCurPlayerNum(), board, false, true);
				break;
			case PM_NONE:
				break;
			case PM_ROAD:
				pickableIndices = SOC.computeRoadRouteIndices(getCurPlayerNum(), board);
				pickableIndices.addAll(board.getRoadsForPlayer(getCurPlayerNum()));
				break;
			case PM_ROBBER:
				break;
			case PM_SETTLEMENT:
				pickableIndices = SOC.computeSettlementVertexIndices(getSOC(), getCurPlayerNum(), board);
				pickableIndices.addAll(board.getSettlementsForPlayer(getCurPlayerNum()));
				break;
			case PM_SHIP:
				pickableIndices = SOC.computeShipRouteIndices(getCurPlayerNum(), board);
				pickableIndices.addAll(board.getShipsForPlayer(getCurPlayerNum()));
				break;
			case PM_VERTEX:
				break;
			case PM_KNIGHT:
				pickableIndices = SOC.computeNewKnightVertexIndices(getCurPlayerNum(), board);
				break;
			case PM_MERCHANT:
				pickableIndices = SOC.computeMerchantTileIndices(soc, getCurPlayerNum(), board);
				break;
    		
    	}
        boardComp.setPickMode(getCurPlayerNum(), mode, pickableIndices);
    }
    
	@Override
    public void onPick(PickMode mode, int pickedValue) {
        try {
            
            switch (this.menuStack.peek()) {
                case MENU_DEBUGGING:
                    switch (mode) {
                        case PM_ROAD: {
                            Route edge = board.getRoute(pickedValue);
                            if (edge.getPlayer() == getCurPlayerNum()) {
                            	board.setPlayerForRoute(edge, 0);
                            } else {
                            	board.setPlayerForRoute(edge, getCurPlayerNum());
                            }
                            console.addText(Color.BLACK, "Road Length: " + board.computeMaxRouteLengthForPlayer(getCurPlayerNum(), rules.isEnableRoadBlock()));
                            break;
                        }

                        case PM_MOVABLE_SHIPS:
                        case PM_SHIP: {
                            Route edge = board.getRoute(pickedValue);
                            if (edge.getPlayer() == getCurPlayerNum()) {
                            	board.setPlayerForRoute(edge, 0);
                            	edge.setShip(false);
                            } else {
                            	board.setPlayerForRoute(edge, getCurPlayerNum());
                            	edge.setShip(true);
                            }
                            console.addText(Color.BLACK, "Road Length: " + board.computeMaxRouteLengthForPlayer(getCurPlayerNum(), rules.isEnableRoadBlock()));
                            break;
                        }

                        case PM_SETTLEMENT:
                        case PM_CITY:
                        case PM_WALLED_CITY:
                            Vertex vertex = board.getVertex(pickedValue);
                            switch (vertex.getType()) {
                            	case OPEN:
                            		vertex.setPlayer(getCurPlayerNum());
                            		vertex.setType(VertexType.SETTLEMENT);
                            		break;
								case CITY:
									vertex.setType(VertexType.WALLED_CITY);
									break;
								case SETTLEMENT:
									vertex.setType(VertexType.CITY);
									break;
								case WALLED_CITY:
									vertex.setPlayer(0);
									vertex.setType(VertexType.OPEN);
									break;
								default:
									break;
                            }
                            
                            board.clearRouteLenCache();
                            console.addText(Color.BLACK, "Road Length: " + board.computeMaxRouteLengthForPlayer(getCurPlayerNum(), rules.isEnableRoadBlock()));
                            break;
                            
                        case PM_ROBBER: {
                        	Tile cell = board.getTile(pickedValue);
                        	if (cell.isWater())
                        		board.setPirate(pickedValue);
                        	else
                        		board.setRobber(pickedValue);
                            break;
                        }
                        
                        case PM_MERCHANT:
                        	board.setMerchant(pickedValue, getCurPlayerNum());
                        	break;
                    }
                    board.findAllPairsShortestPathToDiscoverables(getCurPlayerNum());
                    initPickMode(mode);
                    break;
                    
                case MENU_CONFIG_BOARD:
                	switch (mode) {
                		case PM_CELLPAINT:
                			break;
                		case PM_ISLAND:
                        	int islandNum = board.createIsland(pickedValue);
                        	console.addText(Color.black, "Found island: " + islandNum);
                        	break;
                	}
                    break;
                default:
            
                    returnValue = null;
                    switch (mode) {
                    	case PM_SHIP:
                    		board.getRoute(pickedValue).setShip(true);
                        case PM_ROAD:
                            returnValue = board.getRoute(pickedValue);
                            break;
                            
                        case PM_SETTLEMENT:
                        case PM_CITY:
                        case PM_WALLED_CITY:
                            returnValue = board.getVertex(pickedValue);
                            break;
                            
                        case PM_ROBBER:
                            returnValue = board.getTile(pickedValue);
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
        initMenu();
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
				if (running && !soc.canCancel() && soc.getCurGuiPlayer() instanceof GUIPlayerUser)
					synchronized (soc) {
						soc.save(saveGameFileName);
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
		for (int i=0; i<die.length; i++) {
			diceComps[i].setDie(die[i]);
		}
	}
}

