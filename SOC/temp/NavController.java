package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.log4j.Logger;

import cc.game.soc.ai.AIEvaluator;
import cc.game.soc.core.BuildableType;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.swing.BoardComponent.PickMode;
import cc.game.soc.swing.GUI.MenuState;

/**
 * Basic navigation controller.  
 * @author ccaron
 *
 */
public class NavController implements IController, ActionListener {

    final Logger log = Logger.getLogger(getClass());

    public enum MenuState {

        MENU_START,
        MENU_CHOOSE_NUM_PLAYERS,
        MENU_CHOOSE_COLOR,
        MENU_GAME_SETUP,
        MENU_CONNECT_HOST,
    }
    
    IGui gui;
    Container menu;
    MenuState state;
    
    NavController(MenuState state, IGui gui) {
        
    }

    @Override
    public void onResized(int w, int h) {
        // TODO Auto-generated method stub
        
    }

    private OpButton getMenuOpButton(MenuOp op) {
        return getMenuOpButton(op, op.txt, null);
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

    private void initMenu() {
        log.debug("MenuStack: " + state);
        menu.removeAll();
        
        switch (state) {
        case MENU_START:
            menu.add(getMenuOpButton(MenuOp.NEW));
            menu.add(getMenuOpButton(MenuOp.RESTORE));
            menu.add(getMenuOpButton(MenuOp.CONFIG_BOARD));
            menu.add(getMenuOpButton(MenuOp.MULTIPLAYER));
            menu.add(getMenuOpButton(MenuOp.DEBUG_RENDER));
            menu.add(getMenuOpButton(MenuOp.DEBUG_AI));
            menu.add(getMenuOpButton(MenuOp.EXIT));
            break;
            
        case MENU_CHOOSE_NUM_PLAYERS:
            Map<String, Color> playerColors = getPlayerColors();
            for (int i=2; i<=playerColors.size(); i++) {
                menu.add(getMenuOpButton(MenuOp.CHOOSE_NUM_PLAYERS, String.valueOf(i)));
            }
            menu.add(getMenuOpButton(MenuOp.QUIT, "Back"));
            break;
            
        case MENU_CHOOSE_COLOR:
            playerColors = getPlayerColors();
            Iterator<String> it = playerColors.keySet().iterator();
            while (it.hasNext()) {
                String txt = it.next();
                Color color = playerColors.get(txt);
                menu.add(getMenuOpButton(MenuOp.CHOOSE_COLOR, txt, color));
            }
            break;
            
        case MENU_GAME_SETUP:
            menu.add(getMenuOpButton(MenuOp.START));
            menu.add(getMenuOpButton(MenuOp.CONFIG_BOARD));
            menu.add(getMenuOpButton(MenuOp.CONFIG_SETTINGS));
            menu.add(getMenuOpButton(MenuOp.BACK));
            break;
            
        case MENU_CONNECT_HOST:
            menu.add(new TextInputOp(OpTextField.OP_ADD_HOST, null, this.textInputListener));
            it = GUIProperties.getInstance().getListProperty("client.knownhosts").iterator();
            while (it.hasNext()) {
                String hostName = it.next();
                menu.add(getMenuOpButton(MenuOp.CONNECT_NETHOST, hostName));
            }
            menu.add(getMenuOpButton(MenuOp.BACK));            
            break;
        default:
            log.error("Unhandled case : " + state);
        }
        gui.validate();
    }
    private ActionListener textInputListener = new ActionListener() {
        void processOpTextField(OpTextField op, String text, Object extra) {
            switch (op) {
            case OP_ADD_HOST:
                GUIProperties.getInstance().addListItem("client.knownhosts", text);
                initMenu();
                break;
                
            default:
                assert(false); // unhandled
            }
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            TextInputOp input = (TextInputOp)arg0.getSource();
            processOpTextField(input.getOp(), input.getText(), input.getExtra());
        }
    };
    private Map<String, Color> getPlayerColors() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onPop() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuOp op = MenuOp.valueOf(e.getActionCommand());
        OpButton button = (OpButton)e.getSource();
        processOp(op, button);
        gui.validate();        
    }
    
    @SuppressWarnings("serial")
    void processOp(MenuOp op, OpButton button) {
        
        switch (op) {
            
        case EXIT:
            GUIProperties.getInstance().saveIfDirty();
            System.exit(0);
            break;
            
        case NEW:
            gui.pushMenu(new NavController(MenuState.MENU_GAME_SETUP, gui));
            gui.pushMenu(MenuState.MENU_CHOOSE_COLOR);
            gui.pushMenu(MenuState.MENU_CHOOSE_NUM_PLAYERS);
            initMenu();
            break;
            
        case RESTORE:
            gui.pushMenu(new InGameController(gui))
            soc.reset();
            soc.setBoard(board);
            if (soc.load(saveGameFileName)) {
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
            
        case MULTIPLAYER:
            menuStack.push(MenuState.MENU_CONNECT_HOST);
            initMenu();
            break;
            
        case LEAVE_NETGAME:
            client.disconnect();
            quitToMainMenu();
            break;
            
        case CONNECT_NETHOST:
            menuStack.pop();
            String host = (String)button.getText();
            try {
                client.connect(host, 44445);
                showPopup("CONNECTING", "Connecting to " + host + "\n ...", new PopupButton("CANCEL") {
                    @Override
                    public boolean doAction() {
                        client.disconnect();
                        menuStack.clear();
                        menuStack.push(MenuState.MENU_START);
                        initMenu();
                        return true;
                    }
                });
            } catch (Exception e) {
                this.showOkPopup("ERROR", "Error trying to connect\n" + e.getMessage());
            }
            break;
            
        case CONFIG_SETTINGS:
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
            if (board.isInitialized()) {
                menuStack.clear();
                menuStack.push(MenuState.MENU_START);
                menuStack.push(MenuState.MENU_PLAY_GAME);
                initMenu();
                new Thread(this).start();
            } 
            break;
            
        case GEN_DEFAULT_BOARD:
            menuStack.push(MenuState.MENU_CHOOSE_DEFAULT_BOARD_SIZE);
            initMenu();
            break;
            
        case GEN_DEFAULT_BOARD_SMALL:
            board.generateDefaultBoard(4);
            menuStack.pop();
            initMenu();
            break;
            
        case GEN_DEFAULT_BOARD_MEDIUM:
            board.generateDefaultBoard(5);
            menuStack.pop();
            initMenu();
            break;
            
        case GEN_DEFAULT_BOARD_LARGE:
            board.generateDefaultBoard(6);
            menuStack.pop();
            initMenu();
            break;
    
        case GEN_CUSTOM_BOARD:
            menuStack.push(MenuState.MENU_CHOOSE_CUSTOM_BOARD_SIZE);
            initMenu();
            break;
            
        case GEN_CUSTOM_BOARD_SMALL:
            board.generateCells(6);
            menuStack.pop();
            initMenu();
            break;
            
        case GEN_CUSTOM_BOARD_MEDIUM:
            board.generateCells(8);
            menuStack.pop();
            initMenu();
            break;
            
        case GEN_CUSTOM_BOARD_LARGE:
            board.generateCells(10);
            menuStack.pop();
            initMenu();
            break;
    
        case FINALIZE_BOARD:
            board.finalizeBoard();
            break;
            
        case SAVE_BOARD_AS_DEFAULT:
            if (board.isInitialized())
                saveBoard(defaultBoardFileName);
            break;
            
        case LOAD_DEFAULT:
            if (loadBoard(defaultBoardFileName)) {
                frame.repaint();
            }
            break;
            
        case SAVE_BOARD: {      
            JFileChooser chooser = new JFileChooser();
            File baseDir = GUIProperties.getInstance().getFileProperty("baseDirectory", true, false, false);
            if (baseDir != null)
                chooser.setCurrentDirectory(baseDir);
            chooser.setDialogTitle("Save Board");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                baseDir = file.getParentFile();
                String fileName = file.getAbsolutePath();
                if (!fileName.endsWith(".txt"))
                    fileName += ".txt";
                saveBoard(fileName);
                GUIProperties.getInstance().setProperty("baseDirectory", baseDir);
            }
            break;
        }
        case LOAD_BOARD: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(Helper.getExtensionFilter("txt", true));
            File baseDir = GUIProperties.getInstance().getFileProperty("baseDirectory", true, false, false);
            if (baseDir != null)
                chooser.setCurrentDirectory(baseDir);
            chooser.setDialogTitle("Load Board");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                baseDir = file.getParentFile();
                loadBoard(file.getAbsolutePath());
                GUIProperties.getInstance().setProperty("baseDirectory", baseDir);
            }
            break;
        }
            
        case SET_PICKMODE:
            boardComp.setPickMode((PickMode)button.extra, null);
            break;

            /*
        case SET_CELLTYPE:
            cellTypeButtonSelected.setEnabled(true);
            cellTypeButtonSelected  = button;
            cellTypeButtonSelected.setEnabled(false);
            break;
    */
        case QUIT:
            quitToMainMenu();
            break;
            
        case CANCEL:
            soc.cancel();
            synchronized (waitObj) {
                waitObj.notify();
            }
            break;
        case ROLL_DICE:
            spinDice();
            if (!client.isConnected())
                soc.save(saveGameFileName);
            returnValue = true;
            return;
            
        case CHOOSE_GIVEUP_CARD:
        case CHOOSE_MOVE:
        case CHOOSE_PLAYER_TO_TAKE_CARD_FROM:
        case CHOOSE_RESOURCE:
        case CHOOSE_TRADE:
            returnValue = button.extra;
            break;
            
        case BUILDABLES_POPUP:
        {
            String [] columnNames = { "Buildable", "Wood", "Sheep", "Ore", "Wheat", "Brick" };
            Object [][] rowData = new Object[4][];
            for (BuildableType b : BuildableType.values()) {
                rowData[b.ordinal()] = new Object[6];
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

        case AI_EVALUATE:
        {
            AIEvaluator eval = new AIEvaluator(aiTuningProperties);
            eval.evaluate(soc.getCurPlayer(), soc.getBoard(), soc);
            break;
        }
        
        case AI_ADD_RESOURCE: {
            soc.getCurPlayer().incrementResource((ResourceType)button.extra, 1);
            break;
        }
            
        
        case AI_ANALYZE_MOVE: {
            boolean done = false;
            do {
                SOCPlayer player = getGUIPlayer(soc.getCurPlayerNum());
                List<MoveType> moves = SOC.computeMoveOptions(soc.getCurPlayer(), soc.getBoard());
                switch (player.chooseMove(soc, moves))
                {
                    case BUILD_ROAD:
                        player.chooseRoadEdge(soc, null).setPlayer(player.getPlayerNum());
                        player.adjustResourcesForBuildable(BuildableType.Road, -1);
                        break;

                    // Build a Settlement
                    case BUILD_SETTLEMENT:
                        player.chooseSettlementVertex(soc, null).setPlayer(player.getPlayerNum());
                        player.adjustResourcesForBuildable(BuildableType.Settlement, -1);
                        break;

                    // Build a City
                    case BUILD_CITY:
                        player.chooseCityVertex(soc, null).setIsCity(true);
                        player.adjustResourcesForBuildable(BuildableType.City, -1);
                        break;

                    // Draw a card
                    case DRAW_DEVELOPMENT:
                        break;

                    case MONOPOLY_CARD:
                        break;

                    // Use a year of plenty card
                    case YEAR_OF_PLENTY_CARD:
                        break;

                    // use a road building card
                    case ROAD_BUILDING_CARD:
                        break;

                    // use a victory
                    case VICTORY_CARD:
                        break;

                    // use a soldier
                    case SOLDIER_CARD:
                        break;

                    // stop[ making moves
                    case CONTINUE:
                        done = true;
                        break;

                    // view trade options
                    case TRADE:
                        SOCTrade trade = player.chooseTradeOption(soc, null);
                        player.incrementResource(trade.type, -trade.amount);
                        player.incrementResource(player.chooseResource(soc), 1);
                        break;
                }
                
            } while (!done);
            break;
        }
        case AI_ANALYZE_ROBBER: {
            List<Integer> cellIndices = SOC.computeRobberOptions(soc.getBoard());
            soc.getBoard().setRobberCell(this.getGUIPlayer(soc.getCurPlayerNum()).chooseRobberCell(soc, cellIndices));
            break;
        }
        
        case AI_SAVE_TUNING: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(Helper.getExtensionFilter("properties", true));
            File baseDir = GUIProperties.getInstance().getFileProperty("baseDirectory", true, false, false);
            if (baseDir != null)
                chooser.setCurrentDirectory(baseDir);
            chooser.setDialogTitle("Load Tuning Properties");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                baseDir = file.getParentFile();
                try {
                    aiTuningProperties.store(new FileWriter(file), "Saved on " + new Date());
                    GUIProperties.getInstance().setProperty("baseDirectory", baseDir);
                } catch (IOException e) {
                    showOkPopup("ERROR", "Failed to save " + file.getName() + "\n" + e.getMessage());
                    e.printStackTrace();
                }
                GUIProperties.getInstance().setProperty("baseDirectory", baseDir);
            }
            break;
        }
        
        case AI_LOAD_TUNING: {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(Helper.getExtensionFilter("properties", true));
            File baseDir = GUIProperties.getInstance().getFileProperty("baseDirectory", true, false, false);
            if (baseDir != null)
                chooser.setCurrentDirectory(baseDir);
            chooser.setDialogTitle("Load Tuning Properties");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                baseDir = file.getParentFile();
                try {
                    aiTuningProperties.load(new FileInputStream(file));
                } catch (IOException e) {
                    showOkPopup("ERROR", "Failed to load " + file.getName() + "\n" + e.getMessage());
                    e.printStackTrace();
                }
                GUIProperties.getInstance().setProperty("baseDirectory", baseDir);
            }
            break;
        }
            
        default:
            log.error("Unhandled case '" + op + "'");
        }

        synchronized (waitObj) {
            waitObj.notify();
        }
}
