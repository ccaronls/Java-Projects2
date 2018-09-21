package cc.lib.swing;

import java.io.File;
import java.util.LinkedList;
import java.util.Properties;

import cc.lib.board.BCell;
import cc.lib.board.BEdge;
import cc.lib.board.BVertex;
import cc.lib.board.CustomBoard;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class BoardBuilder extends AWTComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String [] args) {
        new BoardBuilder();
    }

    final EZFrame frame = new EZFrame() {
        @Override
        protected void onMenuItemSelected(String menu, String subMenu) {
            BoardBuilder.this.onMenuItemSelected(menu, subMenu);
        }

        @Override
        protected void onWindowClosing() {
            board.trySaveToFile(boardFile);
        }
    };

    interface Action {
        void undo();
    }

    final File DEFAULT_FILE = new File("bb.backup.board");

    final CustomBoard board;
    int background = -1;
    int selectedIndex = -1;
    File boardFile = DEFAULT_FILE;
    final LinkedList<Action> undoList = new LinkedList<>();
    PickMode pickMode = PickMode.VERTEX;

    enum PickMode {
        VERTEX, EDGE, CELL
    }

    BoardBuilder() {
        board = newBoard();
        setMouseEnabled(true);
        setPadding(10);
        initFrame(frame);
        frame.add(this);
        if (!frame.loadFromFile(new File("bb.properties")))
            frame.centerToScreen(640, 480);
    }

    float progress = 0;

    protected CustomBoard newBoard() {
        return new CustomBoard();
    }

    /**
     * This is a good place to add top bar menus
     * @param frame
     */
    protected void initFrame(EZFrame frame) {
        frame.addMenuBarMenu("File", "New Board", "Load Board", "Load Image", "Clear Image", "Save As...", "Save");
        frame.addMenuBarMenu("Mode", Utils.toStringArray(PickMode.values()));
        frame.addMenuBarMenu("Action", "Compute", "Undo");
    }

    /**
     * Handle menu pushes. Call super if not handled.
     *
     * @param menu
     * @param subMenu
     */
    protected void onMenuItemSelected(String menu, String subMenu) {
        switch (menu) {
            case "File":
                onFileMenu(subMenu);
                break;

            case "Mode":
                onModeMenu(PickMode.valueOf(subMenu));
                break;

            case "Action":
                onActionMenu(subMenu);
                break;

            default:
                log.warn("Unhandled case %s", menu);
        }
    }

    @Override
    protected void init(AWTGraphics g) {
        Properties p = frame.getProperties();
        progress += 0.1f;
        String image = p.getProperty("image");
        if (image != null)
            background = g.loadImage(image);
        progress = 0.5f;
        boardFile = new File(p.getProperty("boardFile", "bb.backup.board"));
        progress = 0.75f;
        board.tryLoadFromFile(boardFile);
        progress = 1;
    }

    @Override
    protected float getInitProgress() {
        return progress;
    }

    @Override
    protected synchronized void paint(AWTGraphics g, int mouseX, int mouseY) {
        g.setIdentity();
        g.ortho();//0, 1, 1, 0);
        if (background >= 0) {
            g.drawImage(background, 0, 0, getWidth(), getHeight());
        } else {
            g.clearScreen(GColor.GREEN);
        }
        g.setColor(GColor.YELLOW);
        g.setLineWidth(3);
        board.drawEdges(g);
        g.setPointSize(5);
        g.setColor(GColor.GREEN);
        board.drawVerts(g);
        g.setColor(GColor.BLUE);
        board.drawCells(g, 0.9f);

        switch (pickMode) {
            case VERTEX:
                drawVertexMode(g, mouseX, mouseY);
                break;

            case EDGE:
                drawEdgeMode(g, mouseX, mouseY);
                break;

            case CELL:
                drawCellMode(g, mouseX, mouseY);
                break;
        }

        g.setColor(GColor.RED);
        g.drawJustifiedString(getWidth()-10, 10, Justify.RIGHT, Justify.TOP, pickMode.name());
    }

    protected void drawVertexMode(APGraphics g, int mouseX, int mouseY) {

        if (selectedIndex >= 0) {
            g.setColor(GColor.RED);
            g.begin();
            g.vertex(board.getVertex(selectedIndex));
            g.drawPoints(8);
        }
        int highlighted = board.pickVertex(g, mouseX, mouseY);
        if (highlighted >= 0) {
            g.setColor(GColor.MAGENTA);
            g.begin();
            g.vertex(board.getVertex(highlighted));
            g.drawPoints(8);
        }

        g.setColor(GColor.BLACK);
        board.drawVertsNumbered(g);

        if (selectedIndex >= 0 && selectedIndex != highlighted) {
            g.setColor(GColor.RED);
            g.begin();
            g.vertex(board.getVertex(selectedIndex));
            g.drawPoints(8);
        }
    }

    protected void drawEdgeMode(APGraphics g, int mouseX, int mouseY) {
        if (selectedIndex >= 0) {
            g.setColor(GColor.RED);
            g.begin();
            BEdge e = board.getEdge(selectedIndex);
            board.renderEdge(e, g);
            g.drawLines();
        }

        int highlighted = board.pickEdge(g, mouseX, mouseY);
        if (highlighted >= 0) {
            g.setColor(GColor.MAGENTA);
            g.begin();
            BEdge e = board.getEdge(highlighted);
            board.renderEdge(e, g);
            g.drawLines();
        }

        g.setColor(GColor.BLACK);
        board.drawEdgesNumbered(g);
    }

    protected void drawCellMode(APGraphics g, int mouseX, int mouseY) {
        if (selectedIndex >= 0) {
            g.setColor(GColor.RED);
            g.begin();
            BCell c = board.getCell(selectedIndex);
            board.renderCell(c, g, 0.9f);
            g.setLineWidth(4);
            g.drawLines();
        }

        int highlighted = board.pickEdge(g, mouseX, mouseY);
        if (highlighted >= 0) {
            g.setColor(GColor.MAGENTA);
            g.begin();
            BEdge e = board.getEdge(highlighted);
            board.renderEdge(e, g);
            g.setLineWidth(2);
            g.drawLines();
        }

        g.setColor(GColor.BLACK);
        board.drawCellsNumbered(g);
    }

    public final int getSelectedIndex() {
        return selectedIndex;
    }

    void setBoardFile(File file) {
        if (file == null) {
            boardFile = DEFAULT_FILE;
            Properties p = frame.getProperties();
            p.remove("boardFile");
            frame.setProperties(p);
        } else {
            boardFile = file;
            Properties p = frame.getProperties();
            p.setProperty("boardFile", boardFile.getAbsolutePath());
            frame.setProperties(p);
        }
    }

    void onModeMenu(PickMode mode) {
        pickMode = mode;
        selectedIndex = -1;
    }

    synchronized void onActionMenu(String item) {
        switch (item) {
            case "Compute":
                board.compute();
                break;

            case "Undo":
                if (undoList.size() > 0) {
                    undoList.removeLast().undo();
                }
                break;
        }
        repaint();
    }

    synchronized void onFileMenu(String item) {
        switch (item) {
            case "New Board":
                board.clear();
                selectedIndex = -1;
                setBoardFile(null);
                break;
            case "Load Board": {
                File file = showFileOpenChooser(frame, "Load Board", "board");
                if (file != null) {
                    try {
                        CustomBoard b = new CustomBoard();
                        b.loadFromFile(file);
                        board.copyFrom(b);
                        setBoardFile(file);
                    } catch (Exception e) {
                        showMessageDialog(frame, "Error", "Failed to load file\n" + file.getAbsolutePath() + "\n\n" + e.getClass().getSimpleName() + ":" + e.getMessage());
                    }
                }
                break;
            }
            case "Load Image": {
                File file = showFileOpenChooser(frame, "Load Image", null);
                if (file != null) {
                    background = getAPGraphics().loadImage(file.getAbsolutePath());
                    if (background < 0) {
                        showMessageDialog(frame, "Error", "Failed to load image\n" + file.getAbsolutePath());
                    } else {
                        Properties p = frame.getProperties();
                        p.setProperty("image", file.getAbsolutePath());
                        frame.setProperties(p);
                    }
                }
                break;
            }
            case "Save As...": {
                File file = showFileSaveChooser(frame, "Save Board", "board", null);
                if (file != null) {
                    try {
                        board.saveToFile(file);
                        setBoardFile(file);
                    } catch (Exception e) {
                        showMessageDialog(frame, "Error", "Failed to Save file\n" + file.getAbsolutePath() + "\n\n" + e.getClass().getSimpleName() + ":" + e.getMessage());
                    }
                }
                break;
            }
            case "Save": {
                if (boardFile != null) {
                    try {
                        board.saveToFile(boardFile);
                    } catch (Exception e) {
                        showMessageDialog(frame, "Error", "Failed to Save file\n" + boardFile.getAbsolutePath() + "\n\n" + e.getClass().getSimpleName() + ":" + e.getMessage());
                    }
                }
                break;
            }

            case "Clear Image": {
                background = -1;
                Properties p = frame.getProperties();
                p.remove("image");
                frame.setProperties(p);
                break;
            }
        }
        repaint();
    }

    @Override
    protected final void onClick() {
        switch (pickMode) {
            case VERTEX:
                pickVertex();
                break;

            case EDGE:
                pickEdge();
                break;

            case CELL:
                pickCell();
                break;
        }
    }

    protected void pickVertex() {

        int picked = board.pickVertex(getAPGraphics(), getMouseX(), getMouseY());
        Action a = null;
        if (picked < 0) {
            final int v = board.addVertex(getMousePos());
            final int s = selectedIndex;
            a = new Action() {
                @Override
                public void undo() {
                    board.removeVertex(v);
                    selectedIndex = s;
                }
            };
            picked = v;
        }
        if (selectedIndex >= 0 && selectedIndex != picked) {
            final int p = picked;
            final int s = selectedIndex;
            board.addEdge(selectedIndex, picked);
            a = new Action() {
                @Override
                public void undo() {
                    board.removeEdge(s, p);
                    board.removeVertex(p);
                    selectedIndex = s;
                }
            };
        }
        if (a != null) {
            pushUndoAction(a);
        }
        selectedIndex = picked;
        repaint();
    }

    protected void pickEdge() {
        int picked = board.pickEdge(getAPGraphics(), getMouseX(), getMouseY());
        Action a = null;
        if (picked >= 0) {
            final BEdge e = board.getEdge(picked);
            board.removeEdge(picked);
            pushUndoAction(new Action() {
                @Override
                public void undo() {
                    board.addEdge(e);
                }
            });
        }
        repaint();
    }

    protected void pickCell() {
        selectedIndex = board.pickCell(getAPGraphics(), getMouseX(), getMouseY());
    }

    protected void pushUndoAction(Action a) {
        undoList.add(a);
        if (undoList.size() > 100)
            undoList.remove(0);
    }

    @Override
    protected void onDragStarted(int x, int y) {
        if (selectedIndex < 0) {
            selectedIndex = board.pickVertex(getAPGraphics(), x, y);
        } else {
            BVertex v = board.getVertex(selectedIndex);
            v.set(getMousePos(x, y));
        }
        repaint();
    }

    @Override
    protected void onDragStopped() {
        selectedIndex = -1;
        repaint();
    }

    @Override
    protected void onKeyTyped(VKKey key) {
        switch (key) {
            case VK_ESCAPE:
                selectedIndex = -1;
                break;

            case VK_V:
                pickMode = PickMode.VERTEX;
                selectedIndex = -1;
                break;

            case VK_E:
                pickMode = PickMode.EDGE;
                selectedIndex = -1;
                break;

            case VK_C:
                pickMode = PickMode.CELL;
                selectedIndex = -1;
                break;

            case VK_TAB:
                pickMode = Utils.incrementEnum(pickMode, PickMode.values());
                selectedIndex = -1;
                break;

            default:
                super.onKeyTyped(key);
        }
        repaint();
    }
}
