package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.FileUtils;

import static java.awt.event.KeyEvent.*;

public abstract class AWTBoardBuilder extends AWTComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String [] args) {
        new AWTBoardBuilder() {
            @Override
            protected CustomBoard newBoard() {
                return new CustomBoard();
            }

            @Override
            protected String getPropertiesFileName() {
                return "bb.properties";
            }

            @Override
            protected String getDefaultBoardFileName() {
                return "bb.backup.board";
            }
        };
    }

    final AWTFrame frame = new AWTFrame() {
        @Override
        protected void onMenuItemSelected(String menu, String subMenu) {
            AWTBoardBuilder.this.onMenuItemSelected(menu, subMenu);
        }

        @Override
        protected void onWindowClosing() {
            board.trySaveToFile(boardFile);
        }
    };

    interface Action {
        void undo();
    }

    final File DEFAULT_FILE;

    final CustomBoard board;
    int background = -1;
    int selectedIndex = -1;
    int highlightedIndex = -1;
    File boardFile;
    final LinkedList<Action> undoList = new LinkedList<>();
    PickMode pickMode = PickMode.VERTEX;

    enum PickMode {
        VERTEX, EDGE, CELL
    }

    AWTBoardBuilder() {
        board = newBoard();
        setMouseEnabled(true);
        setPadding(10);
        initFrame(frame);
        frame.add(this);
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        if (!frame.loadFromFile(new File(settings, getPropertiesFileName())))
            frame.centerToScreen(640, 480);
        DEFAULT_FILE = new File(settings, getDefaultBoardFileName());
    }

    float progress = 0;

    protected abstract CustomBoard newBoard();

    protected abstract String getPropertiesFileName();

    protected abstract String getDefaultBoardFileName();

    /**
     * This is a good place to add top bar menus
     * @param frame
     */
    protected void initFrame(AWTFrame frame) {
        frame.addMenuBarMenu("File", "New Board", "Load Board", "Load Image", "Clear Image", "Save As...", "Save");
        frame.addMenuBarMenu("Mode", Utils.toStringArray(PickMode.values(), false));
        frame.addMenuBarMenu("Action", "Compute", "Clear", "Undo", "Generate Grid");
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
        boardFile = new File(p.getProperty("boardFile", DEFAULT_FILE.getPath()));
        pickMode = PickMode.valueOf(p.getProperty("pickMode", pickMode.name()));
        progress += 0.1f;
        String image = p.getProperty("image");
        if (image != null)
            background = g.loadImage(image);
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
        List<String> lines = new ArrayList<>();
        getDisplayData(lines);
        int y = 10;
        for (String line : lines) {
            g.drawJustifiedString(getWidth() - 10, y, Justify.RIGHT, Justify.TOP, line);
            y += g.getTextHeight();
        }
    }

    protected void getDisplayData(List<String> lines) {
        lines.add(boardFile.getName());
        lines.add(pickMode.name());
    }

    protected void drawVertexMode(APGraphics g, int mouseX, int mouseY) {

        if (selectedIndex >= 0) {
            g.setColor(GColor.RED);
            g.begin();
            g.vertex(board.getVertex(selectedIndex));
            g.drawPoints(8);
        }
        highlightedIndex = board.pickVertex(g, mouseX, mouseY);
        if (highlightedIndex >= 0) {
            g.setColor(GColor.MAGENTA);
            g.begin();
            BVertex v = board.getVertex(highlightedIndex);
            g.vertex(v);
            g.drawPoints(8);
            // draw lines from vertex to its adjacent cells
            g.begin();
            for (BCell c : board.getAdjacentCells(v)) {
                g.vertex(v);
                g.vertex(c);
            }
            g.drawLines();
        }

        g.setColor(GColor.BLACK);
        board.drawVertsNumbered(g);

        if (selectedIndex >= 0 && selectedIndex != highlightedIndex) {
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

        highlightedIndex = board.pickEdge(g, mouseX, mouseY);
        if (highlightedIndex >= 0) {
            g.setColor(GColor.MAGENTA);
            g.begin();
            BEdge e = board.getEdge(highlightedIndex);
            board.renderEdge(e, g);
            Vector2D mp = board.getMidpoint(e);
            // draw the lines from midpt of edge to the cntr of its adjacent cells
            for (BCell c : board.getAdjacentCells(e)) {
                g.vertex(mp);
                g.vertex(c);
            }
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

        highlightedIndex = board.pickCell(g, mouseX, mouseY);
        if (highlightedIndex >= 0) {
            g.setColor(GColor.MAGENTA);
            g.begin();
            BCell cell = board.getCell(highlightedIndex);
            g.setLineWidth(2);
            g.setColor(GColor.RED);
            board.drawCellArrowed(cell, g);
            g.drawCircle(cell.getX(), cell.getY(), cell.getRadius());
            g.begin();
            g.vertex(cell);
            g.drawPoints(3);
            g.begin();
            g.drawRect(board.getCellBoundingRect(highlightedIndex));
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
            frame.setProperty("boardFile", boardFile.getAbsolutePath());
        }
    }

    void onModeMenu(PickMode mode) {
        pickMode = mode;
        selectedIndex = -1;
        frame.setProperty("pickMode", mode.name());
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

            case "Clear":
                board.clear();
                break;

            case "Generate Grid": {
                AWTNumberPicker rows = new AWTNumberPicker.Builder().setLabel("Rows").setMin(1).setMax(100)
                        .setValue(frame.getIntProperty("gui.gridRows", 1)).build((int oldValue, int newValue)  -> frame.setProperty("gui.gridRows", newValue));
                AWTNumberPicker cols = new AWTNumberPicker.Builder().setLabel("Columns").setMin(1).setMax(100)
                        .setValue(frame.getIntProperty("gui.gridCols", 1)).build((int oldValue, int newValue)  -> frame.setProperty("gui.gridCols", newValue));
                AWTPanel panel = new AWTPanel(rows, cols);

                final AWTFrame popup = new AWTFrame("Generate Grid");
                popup.add(panel);
                AWTButton build = new AWTButton("Generate") {
                    @Override
                    protected void onAction() {
                        board.generateGrid(rows.getValue(), cols.getValue(), getViewportWidth(), getViewportHeight());
                        popup.closePopup();
                    }
                };
                AWTButton cancel = new AWTButton("Cancel") {
                    @Override
                    protected void onAction() {
                        popup.closePopup();
                    }
                };
                popup.add(new AWTPanel(build, cancel), BorderLayout.SOUTH);
                popup.showAsPopup(frame);
            }
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
                File file = frame.showFileOpenChooser("Load Board", "Generic Boards", "board");
                if (file != null) {
                    try {
                        CustomBoard b = new CustomBoard();
                        b.loadFromFile(file);
                        board.copyFrom(b);
                        setBoardFile(file);
                    } catch (Exception e) {
                        frame.showMessageDialog("Error", "Failed to load file\n" + file.getAbsolutePath() + "\n\n" + e.getClass().getSimpleName() + ":" + e.getMessage());
                    }
                }
                break;
            }
            case "Load Image": {
                File file = frame.showFileOpenChooser("Load Image", "Generic Boards", null);
                if (file != null) {
                    background = getAPGraphics().loadImage(file.getAbsolutePath());
                    if (background < 0) {
                        frame.showMessageDialog("Error", "Failed to load image\n" + file.getAbsolutePath());
                    } else {
                        Properties p = frame.getProperties();
                        p.setProperty("image", file.getAbsolutePath());
                        frame.setProperties(p);
                    }
                }
                break;
            }
            case "Save As...": {
                File file = frame.showFileSaveChooser("Save Board", "board", "Generic Boards", null);
                if (file != null) {
                    try {
                        board.saveToFile(file);
                        setBoardFile(file);
                    } catch (Exception e) {
                        frame.showMessageDialog("Error", "Failed to Save file\n" + file.getAbsolutePath() + "\n\n" + e.getClass().getSimpleName() + ":" + e.getMessage());
                    }
                }
                break;
            }
            case "Save": {
                if (boardFile != null) {
                    try {
                        board.saveToFile(boardFile);
                    } catch (Exception e) {
                        frame.showMessageDialog("Error", "Failed to Save file\n" + boardFile.getAbsolutePath() + "\n\n" + e.getClass().getSimpleName() + ":" + e.getMessage());
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
    public synchronized void keyTyped(KeyEvent evt) {
        switch (evt.getKeyCode()) {
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
                pickMode = Utils.incrementValue(pickMode, PickMode.values());
                selectedIndex = -1;
                break;

        }
        frame.setProperty("pickMode", pickMode.name());
        repaint();
    }

    @Override
    public synchronized void keyPressed(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case VK_UP:
                if (pickMode == PickMode.VERTEX && selectedIndex >= 0) {
                    BVertex bv = board.getVertex(selectedIndex);
                    MutableVector2D v = new MutableVector2D(bv);
                    v.setY(v.getY() - 1);
                    bv.set(v);
                }
                break;
            case VK_DOWN:
                if (pickMode == PickMode.VERTEX && selectedIndex >= 0) {
                    BVertex bv = board.getVertex(selectedIndex);
                    MutableVector2D v = new MutableVector2D(bv);
                    v.setY(v.getY() + 1);
                    bv.set(v);
                }
                break;
            case VK_RIGHT:
                if (pickMode == PickMode.VERTEX && selectedIndex >= 0) {
                    BVertex bv = board.getVertex(selectedIndex);
                    MutableVector2D v = new MutableVector2D(bv);
                    v.setX(v.getX() + 1);
                    bv.set(v);
                }
                break;
            case VK_LEFT:
                if (pickMode == PickMode.VERTEX && selectedIndex >= 0) {
                    BVertex bv = board.getVertex(selectedIndex);
                    MutableVector2D v = new MutableVector2D(bv);
                    v.setX(v.getX() - 1);
                    bv.set(v);
                }
                break;

        }
        repaint();
    }
}
