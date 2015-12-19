package cc.game.soc.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
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
import cc.game.soc.core.Player.PlayerChoice;
import cc.game.soc.core.Player.RouteChoice;
import cc.game.soc.core.Player.RouteChoiceType;
import cc.game.soc.core.Player.TileChoice;
import cc.game.soc.core.Player.VertexChoice;
import cc.game.soc.core.annotations.RuleVariable;
import cc.game.soc.swing.BoardComponent.PickHandler;
import cc.game.soc.swing.BoardComponent.PickMode;
import cc.game.soc.swing.BoardComponent.RenderFlag;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.swing.*;
import cc.lib.utils.FileUtils;

public class GUI implements ActionListener, ComponentListener, WindowListener, Runnable {

    final Logger log = Logger.getLogger(GUI.class);
    
    static GUI instance;
    static final File homeFolder = new File(System.getProperty("user.home") + "/.soc");

	public static void main(String [] args)  {
		JFrame frame = new JFrame();
		try {
			PlayerBot.DEBUG_ENABLED = true;
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
	//private JSpinner [] diceChoosers;
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
    final File saveGameFile;
    private final File saveRulesFile;
    private final File debugBoard;
    
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
        debugBoard = new File("boards/debug_board.txt");
        
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
        boardComp = new BoardComponent(getBoard(), images) {

			@Override
			protected GUIProperties getProperties() {
				return props;
			}

			@Override
			protected Color getPlayerColor(int playerNum) {
				return GUI.this.getPlayerColor(playerNum);
			}
        };
        
        // console
        Color bkColor = props.getColorProperty("console.bkColor", Color.LIGHT_GRAY);
        if (bkColor == null)
            console = new JMultiColoredScrollConsole();
        else
            console = new JMultiColoredScrollConsole(bkColor);
        consolePane = new JScrollPane();
        consolePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        consolePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        consolePane.getVerticalScrollBar().setBlockIncrement(console.getTextHeight());
        consolePane.getVerticalScrollBar().setUnitIncrement(console.getTextHeight());
        consolePane.setPreferredSize(console.getPreferredSize());
        consolePane.setViewportView(console);

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
                	sp.getViewport().add(playerComponents[num] = new PlayerInfoComponentCAK(num));
                	westGridPanel.add(sp);
                } else {
                	barbarianComp = null;
                	int num = userPlayer.getPlayerNum();
                	westGridPanel.add(playerComponents[num] = new PlayerInfoComponentCAK(num));
                }
                
                for (int i=1; i<=soc.getNumPlayers(); i++) {
                	if (i == userPlayer.getPlayerNum())
                		continue;
                	if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                		eastGridPanel.add(playerComponents[i] = new PlayerInfoComponentCAK(i));
                	} else {
                		eastGridPanel.add(playerComponents[i] = new PlayerInfoComponentCAK(i));
                	}
                }
                
                JPanel helpMenuDice = new JPanel();
                helpMenuDice.setLayout(new BorderLayout());
                JPanel dicePanel = null;
                
                Dice [] dice = soc.getDice();
                diceComps = new ADiceComponent[dice.length];
                for (int i=0; i<diceComps.length; i++)
                	diceComps[i] = new ADiceComponent();
                
                if (getRules().isEnableEventCards()) {
                	dicePanel = new EZPanel(new FlowLayout(), new EventCardComponent(diceComps));
                } else {
                	dicePanel = new EZPanel(new FlowLayout(), diceComps);
                }
                
                helpMenuDice.add(dicePanel, BorderLayout.NORTH);
                helpMenuDice.add(helpText, BorderLayout.CENTER);
                westGridPanel.add(helpMenuDice);
                JScrollPane menuPanel = new JScrollPane();
                menuPanel.setLayout(new ScrollPaneLayout());
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
                    		final TileType tt = (TileType)extra;
                    		boardComp.setPickHandler(new PickHandler() {
								
                    			@Override
                    			public PickMode getPickMode() {
                    				return PickMode.PM_TILE;
                    			}
                    			
								@Override
								public void onPick(BoardComponent bc, int pickedValue) {
									Tile t = bc.getBoard().getTile(pickedValue);
									t.setType(tt);
									if (t.getResource() == null) {
										t.setDieNum(0);
									}
								}
								
								@Override
								public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
									g.setColor(Color.YELLOW);
									bc.drawTileOutline(g, getBoard().getTile(highlightedIndex), 2);
								}
								
								@Override
								public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
									g.setColor(Color.YELLOW);
									bc.drawTileOutline(g, getBoard().getTile(index), 2);
								}
								
								@Override
								public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {}
								
								@Override
								public boolean isPickableIndex(BoardComponent bc, int index) {
									return true;
								}
							});
                    	} else if (extra instanceof PickHandler){
                    		boardComp.setPickHandler((PickHandler)extra);
                    	}
                    }
                };
                for (TileType c : TileType.values()) {
                    grp.addButton(formatString(c.name()), c);
                }
                grp.addButton("Islands", new PickHandler() {
					
					@Override
					public void onPick(BoardComponent bc, int pickedValue) {
            			int islandNum = getBoard().getTile(pickedValue).getIslandNum();
            			if (getBoard().getTile(pickedValue).getIslandNum() > 0) {
            				getBoard().removeIsland(islandNum);
            			} else {
                        	islandNum = getBoard().createIsland(pickedValue);
                        	console.addText(Color.black, "Found island: " + islandNum);
            			}

						
					}
					
					@Override
					public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
						bc.drawIslandOutlined(g, highlightedIndex);
					}
					
					@Override
					public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
					}
					
					@Override
					public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
					}
					
					@Override
					public boolean isPickableIndex(BoardComponent bc, int index) {
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
					public void onPick(BoardComponent bc, int pickedValue) {
						bc.getBoard().addPirateRoute(pickedValue);
						indices = computePirateRouteTiles();
					}
					
					@Override
					public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
						g.setColor(Color.BLACK);
						bc.drawTileOutline(g, getBoard().getTile(highlightedIndex), 2);
					}
					
					@Override
					public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
						g.setColor(Color.RED);
						bc.drawTileOutline(g, getBoard().getTile(index), 2);
					}
					
					@Override
					public void onDrawOverlay(BoardComponent bc, AWTRenderer render, Graphics g) {
						g.setColor(Color.black);
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
		                render.clearVerts();
		                render.addVertices(tiles);
		                render.drawLineStrip(g, 2);
					}
					
					@Override
					public boolean isPickableIndex(BoardComponent bc, int index) {
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
					public void onPick(BoardComponent bc, int pickedValue) {
						Route r = getBoard().getRoute(pickedValue);
						if (r.isClosed()) {
							r.setClosed(false);
						} else {
							r.setClosed(true);
						}
					}
					
					@Override
					public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
						Route rt = getBoard().getRoute(highlightedIndex);
						if (rt.isClosed())
							g.setColor(Color.BLACK);
						else
							g.setColor(Color.WHITE);
						bc.drawRoad(g, rt, true);
					}
					
					@Override
					public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
						Route rt = getBoard().getRoute(index);
						if (rt.isClosed())
							g.setColor(AWTUtils.setAlpha(Color.BLACK, 120));
						else
							g.setColor(AWTUtils.setAlpha(Color.WHITE, 120));
						bc.drawRoad(g, rt, false);
					}
					
					@Override
					public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public boolean isPickableIndex(BoardComponent bc, int index) {
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
					public void onPick(BoardComponent bc, int pickedValue) {
						Vertex v = bc.getBoard().getVertex(pickedValue);
						if (v.getType() == VertexType.OPEN) {
							v.setType(VertexType.PIRATE_FORTRESS);
						} else {
							v.setType(VertexType.OPEN);
						}
					}
					
					@Override
					public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
						Vertex v = getBoard().getVertex(highlightedIndex);
						g.setColor(Color.black);
						bc.drawSettlement(g, v, 0, true);
					}
					
					@Override
					public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
						Vertex v = getBoard().getVertex(index);
						if (v.getType() == VertexType.PIRATE_FORTRESS) {
							g.setColor(Color.black);
						} else {
							g.setColor(AWTUtils.setAlpha(Color.black, 120));
						}
						bc.drawSettlement(g, v, 0, true);
					}
					
					@Override
					public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
						
					}
					
					@Override
					public boolean isPickableIndex(BoardComponent bc, int index) {
						return indices.contains(index);
					}
					
					@Override
					public PickMode getPickMode() {
						indices.clear();
						for (int i=0; i<getBoard().getNumVerts(); i++) {
							Vertex v = getBoard().getVertex(i);
							if (v.canPlaceStructure() && v.getType() == VertexType.OPEN) {
								indices.add(i);
							}
						}
						return PickMode.PM_VERTEX;
					}
				});
                eastGridPanel.add(chooser);
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
    
    enum DebugPick {
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
    				menu.add(getMenuOpButton(MenuOp.CHOOSE_NUM_PLAYERS, String.valueOf(i+2), null, i+2));
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
                MyRadioButtonGroup<DebugPick> pickChoice = new MyRadioButtonGroup<DebugPick>(choiceButtons) {
                    protected void onChange(final DebugPick mode) {
                        //boardComp.setPickMode(getCurPlayerNum(), mode, null);
                    	boardComp.setPickHandler(new PickHandler() {
							
							@Override
							public void onPick(BoardComponent bc, int pickedValue) {
								Vertex v;
								Route r;
								switch (mode) {
									case CITY:
									case CITY_WALL:
									case SETTLEMENT:
										v = getBoard().getVertex(pickedValue);
										if (v.getPlayer() == 0) {
											v.setType(mode.vType);
											v.setPlayer(getCurPlayerNum());
										} else {
											v.reset();
										}
										break;
									case KNIGHT:
										v = getBoard().getVertex(pickedValue);
										v.setPlayer(getCurPlayerNum());
										switch (v.getType()) {
											default:
												v.setType(VertexType.BASIC_KNIGHT_INACTIVE);
												break;
											case BASIC_KNIGHT_INACTIVE:
												v.setType(VertexType.BASIC_KNIGHT_ACTIVE);
												break;
											case BASIC_KNIGHT_ACTIVE:
												v.setType(VertexType.STRONG_KNIGHT_INACTIVE);
												break;
											case STRONG_KNIGHT_INACTIVE:
												v.setType(VertexType.STRONG_KNIGHT_ACTIVE);
												break;
											case STRONG_KNIGHT_ACTIVE:
												v.setType(VertexType.MIGHTY_KNIGHT_INACTIVE);
												break;
											case MIGHTY_KNIGHT_INACTIVE:
												v.setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
												break;
											case MIGHTY_KNIGHT_ACTIVE:
												v.reset();
												break;
										}
										break;
									case FORTRESS:
										v = getBoard().getVertex(pickedValue);
										if (v.getType() == VertexType.PIRATE_FORTRESS) {
											v.setPirateHealth(v.getPirateHealth()-1);
											if (v.getPirateHealth() <= 0)
												v.setType(VertexType.OPEN);
										} else {
											v.reset();
											v.setType(VertexType.PIRATE_FORTRESS);
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
									
								}
							}
							
							@Override
							public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
								switch (mode.mode) {
									case PM_EDGE: {
										g.setColor(getPlayerColor(getCurPlayerNum()));
										Route e = getBoard().getRoute(highlightedIndex);
										bc.drawEdge(g, e, mode.rType, e.getPlayer(), true);
										break;
									}
									case PM_TILE: {
										g.setColor(Color.yellow);
										bc.drawTileOutline(g, getBoard().getTile(highlightedIndex), 2);
										break;
									}
									case PM_VERTEX: {
										g.setColor(getPlayerColor(getCurPlayerNum()));
										Vertex v = getBoard().getVertex(highlightedIndex);
										bc.drawVertex(g, v, mode.vType, v.getPlayer(), true);
										break;
									}
									case PM_NONE:
										break;
								}
							}
							
							@Override
							public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
								switch (mode.mode) {
									case PM_EDGE: {
										g.setColor(AWTUtils.setAlpha(getPlayerColor(getCurPlayerNum()), 100));
										Route e = getBoard().getRoute(index);
										bc.drawEdge(g, e, e.getType(), e.getPlayer(), true);
										break;
									}
									case PM_TILE: {
										break;
									}
									case PM_VERTEX: {
										g.setColor(AWTUtils.setAlpha(getPlayerColor(getCurPlayerNum()), 100));
										Vertex v = getBoard().getVertex(index);
										bc.drawVertex(g, v, v.getType(), v.getPlayer(), true);
										break;
									}
									case PM_NONE:
										break;
								}
							}
							
							@Override
							public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public boolean isPickableIndex(BoardComponent bc, int index) {
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
                choiceButtons.add(getMenuOpButton(MenuOp.RESET_BOARD));
                choiceButtons.add(getMenuOpButton(MenuOp.RESET_BOARD_ISLANDS));
                choiceButtons.add(getMenuOpButton(MenuOp.COMPUTE_DISTANCES));
                choiceButtons.add(getMenuOpButton(MenuOp.EVALUATE_VERTICES));
                choiceButtons.add(getMenuOpButton(MenuOp.LOAD_DEBUG));
                choiceButtons.add(getMenuOpButton(MenuOp.SAVE_DEBUG));
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
    			menu.add(getMenuOpButton(MenuOp.ASSIGN_RANDOM));
    			menu.add(getMenuOpButton(MenuOp.SAVE_BOARD_AS_DEFAULT));
    			if (getBoard().getName() != null) {
    				if (new File(getBoard().getName()).isFile())
    					menu.add(getMenuOpButton(MenuOp.SAVE_BOARD));
    			}
    			menu.add(getMenuOpButton(MenuOp.SAVE_BOARD_AS));
    			menu.add(getMenuOpButton(MenuOp.BACK));
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
				boardComp.setPickHandler(null);
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
				initPlayers((Integer)button.getExtra());
		    	soc.initGame();
				menuStack.pop();
				initMenu();
				break;

			case CHOOSE_COLOR:
				returnValue = button.getText();
				setPlayerColor((Color)button.getExtra());
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
				} else {
					logError("Board not ready");
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
				
			case ASSIGN_RANDOM:
				getBoard().assignRandom();
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

			case REWIND_GAME: {
				stopGameThread();
				try {
					FileUtils.restoreFile(saveGameFile.getAbsolutePath());
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
				boardComp.setPickHandler(null);
				soc.cancel();
				break;

			case CHOOSE_GIVEUP_CARD:
			case CHOOSE_MOVE:
			case CHOOSE_PLAYER:
			case CHOOSE_CARD:
			case CHOOSE_TRADE:
			case CHOOSE_ROAD:
			case CHOOSE_SHIP:
				returnValue = button.getExtra();
				break;

			case BUILDABLES_POPUP:
			{
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
    					for (ResourceType r: ResourceType.values())
    						row.add(String.valueOf(b.getCost(r)));
    					rowData.add(row);
					}
				}
				JTable table = new JTable(rowData, columnNames);
				table.getColumnModel().getColumn(0).setMinWidth(100);
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
				diceComps[0].setDicePickerEnabled(false);
				diceComps[1].setDicePickerEnabled(false);
//				diceChoosers[0].setVisible(false);
//				diceChoosers[1].setVisible(false);
				break;
			}
		
			case COMPUTE_DISTANCES: {
				long t = System.currentTimeMillis();
				PlayerBot.Distances distances = PlayerBot.evaluateWaterwayDistances(getBoard(), getCurPlayerNum());
				long dt = System.currentTimeMillis() - t;
				System.out.println("got Distances in " + dt + " MSecs:\n" + distances);
				break;
			}
			
			case EVALUATE_VERTICES: {
				long t = System.currentTimeMillis();
				BotNodeRoot root = new BotNodeRoot();
				PlayerBot.evaluateVertices(root, getRules(), getCurPlayerNum(), getBoard());
				long dt = System.currentTimeMillis() - t;
				System.out.println("---------------------------------\nVerts Evaluation in " + dt + " MSecs:\n" + root.debugDump());
				break;
			}
			
			case LOAD_DEBUG:
				loadBoard(debugBoard.getAbsolutePath());
				break;
				
			case SAVE_DEBUG:
				saveBoard(debugBoard.getAbsolutePath());
				break;
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
        boardComp.setPickHandler(null);
        menuStack.clear();
        menuStack.push(MenuState.MENU_START);
        initMenu();
    }
    
    private void newGame() {
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
			GUIPlayer p = new GUIPlayer();
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

    public String chooseColorMenu() {
        menuStack.push(MenuState.MENU_CHOOSE_COLOR);
        initMenu();
        return waitForReturnValue(null);
    }
    
    public Vertex chooseVertex(final Collection<Integer> vertices, final int playerNum, final VertexChoice choice) {
		clearMenu();
		boardComp.setPickHandler(new PickHandler() {
			
			@Override
			public void onPick(BoardComponent bc, int pickedValue) {
				bc.setPickHandler(null);
				setReturnValue(getBoard().getVertex(pickedValue));
			}
			
			@Override
			public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
				Vertex v = getBoard().getVertex(highlightedIndex);
				g.setColor(getPlayerColor(getCurPlayerNum())); 
				switch (choice) {
					case SETTLEMENT:
						bc.drawSettlement(g, v, v.getPlayer(), true);
						break;
					case CITY:
						bc.drawCity(g, v, v.getPlayer(), true);
						break;
					case CITY_WALL:
						bc.drawWalledCity(g, v, v.getPlayer(), true);
						break;
					case KNIGHT_DESERTER:
					case KNIGHT_DISPLACED:
					case KNIGHT_MOVE_POSITION:
					case KNIGHT_TO_MOVE:
					case OPPONENT_KNIGHT_TO_DISPLACE:
						bc.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), v.getType().isKnightActive(), true);
						break;
					case KNIGHT_TO_ACTIVATE:
						bc.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel(), true, true);
						break;
					case KNIGHT_TO_PROMOTE:
						bc.drawKnight(g, v, v.getPlayer(), v.getType().getKnightLevel()+1, v.getType().isKnightActive(), true);
						break;
					case NEW_KNIGHT:
						bc.drawKnight(g, v, v.getPlayer(), 1, false, true);
						break;
					case POLITICS_METROPOLIS:
						bc.drawMetropolisPolitics(g, v, v.getPlayer(), true);
						break;
					case SCIENCE_METROPOLIS:
						bc.drawMetropolisScience(g, v, v.getPlayer(), true);
						break;
					case TRADE_METROPOLIS:
						bc.drawMetropolisTrade(g, v, v.getPlayer(), true);
						break;
					case PIRATE_FORTRESS:
						bc.drawPirateFortress(g, v, true);
						break;
					case OPPONENT_STRUCTURE_TO_ATTACK:
						bc.drawVertex(g, v, v.getType(), v.getPlayer(), true);
						break;
				}

			}
			
			@Override
			public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
				Vertex v = getBoard().getVertex(index);
				g.setColor(AWTUtils.setAlpha(getPlayerColor(getCurPlayerNum()), 120));
				switch (choice) {
					case SETTLEMENT:
						bc.drawSettlement(g, v, 0, false);
						break;
					case CITY:
						bc.drawCity(g, v, 0, false);
						break;
					case CITY_WALL:
						bc.drawWalledCity(g, v, 0, false);
						break;
					case KNIGHT_DESERTER:
					case KNIGHT_DISPLACED:
					case KNIGHT_MOVE_POSITION:
					case KNIGHT_TO_MOVE:
					case OPPONENT_KNIGHT_TO_DISPLACE:
						bc.drawKnight(g, v, 0, v.getType().getKnightLevel(), v.getType().isKnightActive(), false);
						break;
					case KNIGHT_TO_ACTIVATE:
						bc.drawKnight(g, v, 0, v.getType().getKnightLevel(), true, false);
						break;
					case KNIGHT_TO_PROMOTE:
						bc.drawKnight(g, v, 0, v.getType().getKnightLevel()+1, v.getType().isKnightActive(), false);
						break;
					case NEW_KNIGHT:
						bc.drawKnight(g, v, 0, 1, false, false);
						break;
					case POLITICS_METROPOLIS:
						bc.drawMetropolisPolitics(g, v, 0, false);
						break;
					case SCIENCE_METROPOLIS:
						bc.drawMetropolisScience(g, v, 0, false);
						break;
					case TRADE_METROPOLIS:
						bc.drawMetropolisTrade(g, v, 0, false);
						break;
					case PIRATE_FORTRESS:
						bc.drawPirateFortress(g, v, false);
						break;
					case OPPONENT_STRUCTURE_TO_ATTACK:
						g.setColor(AWTUtils.setAlpha(getPlayerColor(v.getPlayer()), 120));
						bc.drawVertex(g, v, v.getType(), 0, false);
						break;
				}
			}
			
			@Override
			public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
			}
			
			@Override
			public boolean isPickableIndex(BoardComponent bc, int index) {
				return vertices.contains(index);
			}
			
			@Override
			public PickMode getPickMode() {
				return PickMode.PM_VERTEX;
			}
		});
		completeMenu();
		return waitForReturnValue(null);
    }
	
	public RouteChoiceType chooseRouteType() {
		clearMenu();
		menu.add(getMenuOpButton(MenuOp.CHOOSE_ROAD, "Roads", "View road options", RouteChoiceType.ROAD_CHOICE));
		menu.add(getMenuOpButton(MenuOp.CHOOSE_SHIP, "Ships", "View ship options", RouteChoiceType.SHIP_CHOICE));
		completeMenu();
		return waitForReturnValue(null);
	}

	public Route chooseRoute(final Collection<Integer> edges, final RouteChoice choice) {
		clearMenu();
		boardComp.setPickHandler(new PickHandler() {
			
			@Override
			public void onPick(BoardComponent bc, int pickedValue) {
				bc.setPickHandler(null);
				setReturnValue(getBoard().getRoute(pickedValue));
			}
			
			@Override
			public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
				Route route = getBoard().getRoute(highlightedIndex);
				g.setColor(getPlayerColor(getCurPlayerNum()));
				switch (choice) {
					case OPPONENT_ROAD_TO_ATTACK:
						g.setColor(getPlayerColor(route.getPlayer()));
						bc.drawEdge(g, route, route.getType(), 0, true);
						break;
					case ROAD:
					case ROUTE_DIPLOMAT:
						bc.drawRoad(g, route, true);
						break;
					case SHIP:
						bc.drawShip(g, route, true);
						break;
					case SHIP_TO_MOVE:
						bc.drawEdge(g, route, route.getType(), getCurPlayerNum(), true);
						break;
					case UPGRADE_SHIP:
						bc.drawWarShip(g, route, true);
						break;
				}
			}
			
			@Override
			public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
				Route route = getBoard().getRoute(index);
				g.setColor(AWTUtils.setAlpha(getPlayerColor(getCurPlayerNum()), 120));
				switch (choice) {
					case OPPONENT_ROAD_TO_ATTACK:
						g.setColor(AWTUtils.setAlpha(getPlayerColor(route.getPlayer()), 120));
						bc.drawEdge(g, route, route.getType(), 0, false);
						break;
					case ROAD:
					case ROUTE_DIPLOMAT:
						bc.drawRoad(g, route, false);
						break;
					case SHIP:
						bc.drawShip(g, route, false);
						break;
					case SHIP_TO_MOVE:
						bc.drawEdge(g, route, route.getType(), getCurPlayerNum(), true);
						break;
					case UPGRADE_SHIP:
						bc.drawWarShip(g, route, false);
						break;
				}
			}
			
			@Override
			public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isPickableIndex(BoardComponent bc, int index) {
				return edges.contains(index);
			}
			
			@Override
			public PickMode getPickMode() {
				return PickMode.PM_EDGE;
			}
		});
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Tile chooseTile(final Collection<Integer> cells, final TileChoice choice) {
		clearMenu();
		boardComp.setPickHandler(new PickHandler() {
			
			@Override
			public void onPick(BoardComponent bc, int pickedValue) {
				bc.setPickHandler(null);
				setReturnValue(getBoard().getTile(pickedValue));
			}
			
			@Override
			public void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex) {
				Tile t = bc.getBoard().getTile(highlightedIndex);
				switch (choice) {
					case INVENTOR:
						bc.drawTileOutline(g, t, 2);
						break;
					case MERCHANT:
						bc.drawMerchant(g, t, getCurPlayerNum());
						break;
					case ROBBER:
					case PIRATE:
						if (t.isWater())
							bc.drawPirate(g, t);
						else
							bc.drawRobber(g, t);
						break;
				}
			}
			
			@Override
			public void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index) {
				Tile t = bc.getBoard().getTile(index);
				g.setColor(Color.RED);
				bc.drawTileOutline(g, t, 1);
			}
			
			@Override
			public void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isPickableIndex(BoardComponent bc, int index) {
				return cells.contains(index);
			}
			
			@Override
			public PickMode getPickMode() {
				return PickMode.PM_TILE;
			}
		});
		completeMenu();
		return waitForReturnValue(null);
	}
	
	// called from run thread
	public MoveType chooseMoveMenu(Collection<MoveType> moves) {
		clearMenu();
		Iterator<MoveType> it = moves.iterator();
		while (it.hasNext()) {
			MoveType move = it.next();
			OpButton button = null;
			menu.add(button = getMenuOpButton(MenuOp.CHOOSE_MOVE, move.getNiceText(), move.getHelpText(getRules()), move));
			button.setToolTipText(move.getHelpText(getRules()));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	// called from run thread
	public <T extends Enum<T>> T chooseEnum(List<T> choices) {
		clearMenu();
		Iterator<T> it = choices.iterator();
		while (it.hasNext()) {
			Enum<T> choice= it.next();
			menu.add(getMenuOpButton(MenuOp.CHOOSE_MOVE, choice.name(), null, choice));
		}
		completeMenu();
		return waitForReturnValue(null);
	}	
	
	public boolean getSetDiceMenu(Dice [] die, int num) {
		clearMenu();
		//diceChoosers[0].setVisible(true);
		//diceChoosers[1].setVisible(true);
		diceComps[0].setDicePickerEnabled(true);
		diceComps[1].setDicePickerEnabled(true);
		menu.add(getMenuOpButton(MenuOp.SET_DICE));
		completeMenu();
		int [] result = (int[])waitForReturnValue(null);
		if (result != null) {
			for (int i=0; i<result.length; i++) {
				die[i].setNum(result[i]);
			}
			return true;
		}
		return false;
	}
	
	public Player choosePlayerMenu(Collection<Integer> players, PlayerChoice mode) {
		clearMenu();
		for (int num : players) {
			Player player = getGUIPlayer(num);
			switch (mode) {
				case PLAYER_FOR_DESERTION: {
					int numKnights = getBoard().getNumKnightsForPlayer(player.getPlayerNum());
					menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + numKnights + " Knights", null, player));
					break;
				}
				case PLAYER_TO_SPY_ON:
					menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + player.getUnusedCardCount(CardType.Progress) + " Progress Cards", null, player));
					break;
				default:
					System.err.println("ERROR: Unhandled case '" + mode + "'");
				case PLAYER_TO_FORCE_HARBOR_TRADE:
				case PLAYER_TO_GIFT_CARD:
				case PLAYER_TO_TAKE_CARD_FROM:
					menu.add(getMenuOpButton(MenuOp.CHOOSE_PLAYER, player.getName() + " X " + player.getTotalCardsLeftInHand() + " Cards", null, player));
					break;
			}
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	
	public Card chooseCardMenu(Collection<Card> cards) {
		clearMenu();
		for (Card type : cards) {
			menu.add(getMenuOpButton(MenuOp.CHOOSE_CARD, type.getName(), type.getHelpText(getRules()), type));
		}
		completeMenu();
		return waitForReturnValue(null);
	}
	
	public Trade chooseTradeMenu(Collection<Trade> trades) {
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
	
	/*
	private ADiceComponent [] getDiceComps(Dice [] config) {
		int wbIndex = 0;
		if (config.length == diceComps.length)
			return diceComps;
		ADiceComponent [] comps = new ADiceComponent[config.length];
		for (int i=0; i<config.length; i++) {
			for (int ii=i; ii<diceComps.length; ii++) {
				if (diceComps[ii].getType() == config[i].getType()) {
					comps[i] = diceComps[ii];
				}
			}
		}
		return comps;
	}*/
	
	
	public void spinDice(Dice ... dieToSpin) {
		clearMenu();
		int delay = 10;
		long startTime = System.currentTimeMillis();
		while (true) {
			long curTime = System.currentTimeMillis();
			if (curTime - startTime > diceSpinTimeSeconds*1000)
				break;
			for (int i=0; i<dieToSpin.length; i++) {
				diceComps[i].setType(dieToSpin[i].getType());
				diceComps[i].setDie(Utils.rand() % 6 + 1);
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
	
	public void setReturnValue(Object o) {
		returnValue = o;
		synchronized (waitObj) {
			waitObj.notify();
		}
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
        panel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.setPreferredSize(new Dimension(1000, 800));
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
        			        view.add(new JSeparator());
        			        view.add(new JSeparator());
        				}
        				
        				if (f.getType().equals(boolean.class)) {
        					view.add(new JLabel());
        			        JToggleButton button = new JToggleButton(ruleVar.description(), f.getBoolean(rules));
        			        view.add(button);
        			        components.put(button,  f);
        				} else if (f.getType().equals(int.class)) {
        			        view.add(new JLabel("<html><body style='width: 400'/>" + ruleVar.description()));
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
	
/*
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
*/
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
				/*
				soc.runGame();
				if (running && soc.isGoodForSave()) { // && soc.getCurGuiPlayer() instanceof GUIPlayerUser)
					synchronized (soc) {
						FileUtils.backupFile(saveGameFile.getAbsolutePath(), 10);
						soc.save(saveGameFile.getAbsolutePath());
					}
				}*/
				
				soc.runGame();
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

	public void setDice(Dice ... dice) {
		for (int i=0; i<dice.length; i++) {
			diceComps[i].setType(dice[i].getType());
			diceComps[i].setDie(dice[i].getNum());
		}
	}
	/*
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
	}*/
}

