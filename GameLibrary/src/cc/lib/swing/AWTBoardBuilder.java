package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cc.lib.board.BCell;
import cc.lib.board.BEdge;
import cc.lib.board.BVertex;
import cc.lib.board.CustomBoard;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.FileUtils;
import cc.lib.utils.GException;
import cc.lib.utils.Table;

import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_E;
import static java.awt.event.KeyEvent.VK_EQUALS;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_H;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_M;
import static java.awt.event.KeyEvent.VK_MINUS;
import static java.awt.event.KeyEvent.VK_N;
import static java.awt.event.KeyEvent.VK_PLUS;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_TAB;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_V;

public abstract class AWTBoardBuilder<V extends BVertex, E extends BEdge, C extends BCell, T extends CustomBoard<V,E,C>> extends AWTComponent {

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

    static class KeyAction implements Comparable<KeyAction> {
        final String key;
        final String description;
        final Runnable action;

        public KeyAction(String key, String description, Runnable action) {
            this.key = key;
            this.description = description;
            this.action = action;
        }

        int getCompareKey() {
            char c = key.charAt(0);
            if (Character.isUpperCase(c))
                return 256 + c;
            return c;
        }

        @Override
        public int compareTo(KeyAction o) {
            return Integer.compare(getCompareKey(), o.getCompareKey());
        }
    }

    enum PickMode {
        VERTEX, EDGE, CELL
    }

    public abstract class Tool {
        final String name;
        public Tool(String name) {
            this.name = name;
        }

        public final String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            String s = o.toString();
            return Utils.equals(name, s);
        }

        public void onPick() {
            switch (pickMode) {
                case VERTEX:
                    // add a new vertex if we are not highlighting an existing one
                    int picked = highlightedIndex;
                    if (picked < 0) {
                        final int v = board.addVertex(getMousePos());
                        final int s = getSelectedIndex();
                        pushUndoAction(new Action() {
                            @Override
                            public void undo() {
                                board.removeVertex(v);
                                setSelectedIndex(s);
                            }
                        });
                        highlightedIndex = v;
                    }
                default:
                    if (multiSelect) {
                        if (highlightedIndex >= 0 && !selected.contains(highlightedIndex))
                            selected.add(highlightedIndex);
                        else
                            selected.remove((Object)highlightedIndex);
                    } else {
                        setSelectedIndex(highlightedIndex);
                    }
            }
        }
        public void onDraw(AWTGraphics g) {}
    }

    List<Tool> tools = new ArrayList<Tool>() {{
        add(new Tool("NONE") {
        });
        add(new Tool("EDGE BUILDER") {
            @Override
            public void onPick() {
                if (pickMode == PickMode.VERTEX && !multiSelect) {
                    int sel = getSelectedIndex();
                    if (highlightedIndex < 0) {
                        super.onPick();
                    }
                    if (highlightedIndex >= 0 && sel >= 0 && highlightedIndex != sel) {
                        int h = highlightedIndex;
                        board.getOrAddEdge(sel, h);
                        pushUndoAction(new Action() {
                            @Override
                            public void undo() {
                                board.removeEdge(sel, h);
                            }
                        });
                    }
                    setSelectedIndex(highlightedIndex);
                } else {
                    super.onPick();
                }
            }
        });
        add(new Tool("CELL BUILDER") {
            final List<Integer> wrappedIndices = new ArrayList<>();
            boolean inside = false;

            @Override
            public void onPick() {
                if (inside) {
                    //board.addCell(wrappedIndices);
                }
                super.onPick();
            }

            @Override
            public void onDraw(AWTGraphics g) {
                Map<BVertex, Integer> vmap = new HashMap<>();
                for (int idx : selected) {
                    vmap.put(board.getVertex(idx), idx);
                }
                List<BVertex> wrapped = Utils.computeGiftWrapVertices(vmap.keySet());
                wrappedIndices.clear();
                for (BVertex v : wrapped) {
                    wrappedIndices.add(vmap.get(v));
                }

                if (inside = Utils.isPointInsidePolygon(mouse, wrapped)) {
                    g.setColor(GColor.RED);
                } else {
                    g.setColor(GColor.ORANGE);
                }
                g.begin();
                for (BVertex v : wrapped) {
                    g.vertex(v);
                }
                g.drawLineLoop(4);
            }
        });
    }};

    File DEFAULT_FILE = new File("AWTBoardBuilder.default");
    final T board;
    int background = -1;
    int highlightedIndex = -1;
    File boardFile;
    final LinkedList<Action> undoList = new LinkedList<>();
    PickMode pickMode = PickMode.VERTEX;
    boolean showNumbers = true;
    boolean showHelp = false;
    GRectangle rect = null;
    boolean multiSelect = false;
    List<Integer> selected = new ArrayList<>();
    final MutableVector2D mouse = new MutableVector2D();
    final Map<Integer, KeyAction> actions = new HashMap<>();
    Tool tool = tools.get(0);

    AWTBoardBuilder() {
        board = newBoard();
        setMouseEnabled(true);
        setPadding(10);
        initFrame(frame);
        frame.add(this);
        try {
            File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
            if (!frame.loadFromFile(new File(settings, getPropertiesFileName())))
                frame.centerToScreen(640, 480);
            DEFAULT_FILE = new File(settings, getDefaultBoardFileName());
        } catch (Exception e) {
            e.printStackTrace();
            frame.centerToScreen(640, 480);
        }
        board.setDimension(getWidth(), getHeight());
        initActions();
        setFocusTraversalKeysEnabled(false);
    }

    float progress = 0;

    protected abstract T newBoard();

    protected abstract String getPropertiesFileName();

    protected abstract String getDefaultBoardFileName();

    /**
     * This is a good place to add top bar menus
     * @param frame
     */
    private void initFrame(AWTFrame frame) {
        registerTools();
        frame.addMenuBarMenu("File", "New Board", "Load Board", "Load Image", "Clear Image", "Save As...", "Save");
        frame.addMenuBarMenu("Select", "All", "None");
        frame.addMenuBarMenu("Mode", Utils.toStringArray(PickMode.values(), false));
        frame.addMenuBarMenu("Tool", Utils.toStringArray(tools));
        frame.addMenuBarMenu("Action", "Compute", "Clear", "Undo", "Generate Grid");
    }

    /**
     * Override this method to add your tools.
     * Be sure to @CallSuper
     */
    protected void registerTools() {

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

            case "Select":
                onSelectMenu(subMenu);
                break;

            case "Mode":
                onModeMenu(PickMode.valueOf(subMenu));
                break;

            case "Action":
                onActionMenu(subMenu);
                break;

            case "Tool":
                setTool(subMenu);
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
        multiSelect = Boolean.valueOf(p.getProperty("multiSelect", "false"));
        progress += 0.1f;
        String image = p.getProperty("image");
        if (image != null)
            background = g.loadImage(image);
        progress = 0.75f;
        board.tryLoadFromFile(boardFile);
        progress = 1;
        setTool(p.getProperty("tool", tool.name));
    }

    private void setTool(String name) {
        tool = Utils.requireNotNull(Utils.findFirstOrNull(tools, t -> name.equals(t.name)));
        frame.setProperty("tool", name);
    }

    @Override
    protected float getInitProgress() {
        return progress;
    }

    @Override
    protected void onDimensionChanged(AWTGraphics g, int width, int height) {
        rect = null;
    }

    @Override
    protected synchronized void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (rect == null) {
            rect = new GRectangle(0, 0, g.getViewportWidth(), g.getViewportHeight());
            board.setDimension(rect.getDimension());
        }
        g.setIdentity();
        g.ortho(rect);
        mouse.set(mouseX, mouseY);
        if (background >= 0) {
            g.drawImage(background, new GRectangle(0, 0, getWidth(), getHeight()));
        } else {
            g.clearScreen(GColor.DARK_GRAY);
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
                drawVertexMode(g, mouse);
                break;

            case EDGE:
                drawEdgeMode(g, mouseX, mouseY);
                break;

            case CELL:
                g.screenToViewport(mouse);
                drawCellMode(g, mouseX, mouseY);
                break;
        }
        tool.onDraw(g);

        g.ortho();
        g.setColor(GColor.RED);
        List<String> lines = new ArrayList<>();
        getDisplayData(lines);
        StringBuffer buf = new StringBuffer();
        for (String s : lines) {
            if (buf.length() > 0)
                buf.append("\n");
            buf.append(s);
        }
        g.setColor(GColor.YELLOW);
        g.drawWrapStringOnBackground(getWidth()-10, 10,getWidth()/4, Justify.RIGHT, Justify.TOP, buf.toString(), GColor.TRANSLUSCENT_BLACK, 3);
        if (showHelp) {
            drawHelp(g);
        }
        checkScrolling();
    }

    void drawHelp(AGraphics g) {
        g.setColor(GColor.YELLOW);
        Table table = new Table();
        List<KeyAction> list = new ArrayList(actions.values());
        Collections.sort(list);
        Utils.unique(list);
        for (KeyAction action: list) {
            table.addRow(action.key, action.description);
        }
        table.draw(g, g.getViewport().getCenter());
    }

    void checkScrolling() {
        int width = getAPGraphics().getViewportWidth();
        int height = getAPGraphics().getViewportHeight();
        int margin = Math.min(width, height) / 10;

        int mouseX = getMouseX();
        int mouseY = getMouseY();

        int scrollSpeed=3;

        boolean refresh = false;
        if (mouseX > 0 && mouseX < margin) {
            if (rect.x > 0) {
                rect.x-=scrollSpeed;
                refresh = true;
            }
        } else if (mouseX < width && mouseX > width - margin) {
            if (rect.x + rect.w < width) {
                rect.x+=scrollSpeed;
                refresh = true;
            }
        }

        if (mouseY > 0 && mouseY < margin) {
            if (rect.y > 0) {
                rect.y-=scrollSpeed;
                refresh = true;
            }
        } else if (mouseY < height && mouseY > height - margin) {
            if(rect.y + rect.h < height) {
                rect.y+=scrollSpeed;
                refresh = true;
            }
        }

        if (refresh && isFocused()) {
            Utils.waitNoThrow(this, 30);
            redraw();
        }
    }

    protected void getDisplayData(List<String> lines) {
        String path = boardFile.getName();
        File parent = boardFile.getParentFile();
        if (parent != null) {
            path = parent.getName() + "/" + path;
        }
        lines.add(path);
        lines.add(pickMode.name() + ":" + (multiSelect?"MULTI:":"")+ tool.toString());
        lines.add(String.format("V:%d E:%d C:%d", board.getNumVerts(), board.getNumEdges(), board.getNumCells()));
    }

    public int getSelectedIndex() {
        return selected.size() > 0 ? selected.get(0) : -1;
    }

    public void setSelectedIndex(int idx) {
        if (multiSelect) {
            if (selected.contains(idx)) {
                selected.remove((Object)idx);
            } else if (idx >=0) {
                selected.add(idx);
            }
        } else {
            selected.clear();
            if (idx >= 0)
                selected.add(idx);
        }
        frame.setProperty("selected", selected);
    }

    public void clearSelected() {
        selected.clear();
        frame.setProperty("selected", selected);
    }

    protected void drawVertexMode(APGraphics g, Vector2D mouse) {

        g.setColor(GColor.RED);
        g.begin();
        for (int idx : selected) {
            g.vertex(board.getVertex(idx));
        }
        g.drawPoints(8);
        highlightedIndex = board.pickVertex(g, mouse);
        if (highlightedIndex >= 0) {
            BVertex v = board.getVertex(highlightedIndex);
            g.begin();
            g.vertex(v);
            g.drawPoints(10);
            // draw lines from vertex to its adjacent cells
            g.begin();
            for (BCell c : board.getAdjacentCells(v)) {
                g.vertex(v);
                g.vertex(c);
            }
            g.setColor(GColor.MAGENTA);
            g.drawLines();

            // draw lines to adjacent verts
            g.begin();
            for (int vIdx : v.getAdjVerts()) {
                BVertex vv = board.getVertex(vIdx);
                g.vertex(v);
                g.vertex(vv);
            }
            g.setColor(GColor.CYAN);
            g.drawLines();
        }

        g.setColor(GColor.BLACK);
        if (showNumbers)
            board.drawVertsNumbered(g);

    }

    protected void drawEdgeMode(APGraphics g, int mouseX, int mouseY) {
        if (getSelectedIndex() >= 0) {
            g.setColor(GColor.RED);
            g.begin();
            BEdge e = board.getEdge(getSelectedIndex());
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
            for (Object _c : board.getAdjacentCells(e)) {
                BCell c = (BCell)_c;
                g.vertex(mp);
                g.vertex(c);
            }
            g.drawLines();
        }

        g.setColor(GColor.BLACK);
        if (showNumbers)
            board.drawEdgesNumbered(g);
    }

    protected void drawCellMode(APGraphics g, int mouseX, int mouseY) {
        for (int idx : selected) {
            g.setColor(GColor.RED);
            g.begin();
            BCell c = board.getCell(idx);
            board.renderCell(c, g, 0.9f);
            g.setLineWidth(4);
            g.drawLineLoop();
        }

        highlightedIndex = board.pickCell(g, mouse);
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
            g.begin();
            for (int idx : cell.getAdjCells()) {
                BCell cell2 = board.getCell(idx);
                g.vertex(cell);
                g.vertex(cell2);
            }
            g.setColor(GColor.CYAN);
            g.drawLines();
        }

        g.setColor(GColor.BLACK);
        if (showNumbers)
            board.drawCellsNumbered(g);
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
        clearSelected();
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
                clearSelected();
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

    protected String getBoardFileExtension() {
        return null;
    }

    synchronized void onFileMenu(String item) {
        switch (item) {
            case "New Board":
                board.clear();
                clearSelected();
                setBoardFile(null);
                break;
            case "Load Board": {
                File file = frame.showFileOpenChooser("Load Board", getBoardFileExtension(), "board");
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
                File file = frame.showFileOpenChooser("Load Image", "png", null);
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

    private void onSelectMenu(String subMenu) {
        switch (subMenu) {
            case "All":
                setMultiselect(true);
                switch (pickMode) {
                    case VERTEX:
                        selected.clear();
                        selected.addAll(Utils.map(Utils.getRangeIterator(0, board.getNumVerts()-1), i->i));
                        break;
                    case EDGE:
                        selected.clear();
                        selected.addAll(Utils.map(Utils.getRangeIterator(0, board.getNumEdges()-1), i->i));
                        break;
                    case CELL:
                        selected.clear();
                        selected.addAll(Utils.map(Utils.getRangeIterator(0, board.getNumCells()-1), i->i));
                        break;
                }
                frame.setProperty("selected", selected);
                break;

            case "Invert": {
                setMultiselect(true);
                List<Integer> newSel;
                switch (pickMode) {
                    case VERTEX:
                        newSel = Utils.filter(Utils.getRangeIterator(0, board.getNumVerts()-1), i->!selected.contains(i));
                        selected.clear();
                        selected.addAll(newSel);
                        break;
                    case EDGE:
                        newSel = Utils.filter(Utils.getRangeIterator(0, board.getNumEdges()-1), i->!selected.contains(i));
                        selected.clear();
                        selected.addAll(newSel);
                        break;
                    case CELL:
                        newSel = Utils.filter(Utils.getRangeIterator(0, board.getNumCells()-1), i->!selected.contains(i));
                        selected.clear();
                        selected.addAll(newSel);
                        break;
                }
                frame.setProperty("selected", selected);
            }
        }
        redraw();
    }

    @Override
    protected final void onClick() {
        tool.onPick();
        repaint();
    }
/*
    protected void pickVertexMultiSelect() {
        if (highlightedIndex >= 0) {
            if (selected.contains(highlightedIndex)) {
                selected.remove((Object)highlightedIndex);
            } else {
                selected.add(highlightedIndex);
            }
        }
    }

    protected void pickVertexSingleSelect() {

        int picked = highlightedIndex;
        if (picked < 0) {
            final int v = board.addVertex(getMousePos());
            final int s = getSelectedIndex();
            pushUndoAction(new Action() {
                @Override
                public void undo() {
                    board.removeVertex(v);
                    setSelectedIndex(s);
                }
            });
            highlightedIndex = v;
        }
        repaint();
    }

    protected void pickEdge() {
        setSelectedIndex(highlightedIndex);
        repaint();
    }

    protected void pickCellSingleSelect() {
        setSelectedIndex(highlightedIndex);
        repaint();
    }

    protected void pickCellMultiselect() {
        int idx = board.pickCell(getAPGraphics(), mouse);
        if (idx >= 0) {
            selected.add(idx);
        }
        redraw();
    }*/

    protected void pushUndoAction(Action a) {
        undoList.add(a);
        if (undoList.size() > 100)
            undoList.remove(0);
    }

    @Override
    protected void onDragStarted(int x, int y) {
        if (getSelectedIndex() < 0) {
            setSelectedIndex(board.pickVertex(getAPGraphics(), mouse));
        } else {
            BVertex v = board.getVertex(getSelectedIndex());
            board.moveVertex(getSelectedIndex(), getMousePos(x, y));
        }
        repaint();
    }

    @Override
    protected void onDragStopped() {
        selected.clear();
        repaint();
    }

    protected void initActions() {
        addAction(VK_ESCAPE, "ESC", "Clear Selected", () -> selected.clear());
        addAction(VK_V, "V", "Set PickMode to VERTEX", ()->setPickMode(PickMode.VERTEX));
        addAction(VK_E, "E", "Set PickMode EDGE", ()->setPickMode(PickMode.EDGE));
        addAction(VK_C, "C", "Set PickMOde CELL", ()->setPickMode(PickMode.CELL));
        addAction(VK_M, "M", "Set toggle MULTI-SELECT", ()->setMultiselect(!multiSelect));
        addAction(VK_H, "H", "Toggle Show Help", ()->showHelp=!showHelp);
        addAction(Utils.toIntArray(VK_DELETE, VK_BACK_SPACE), "DELETE", "Remove Selected Item", ()->deleteSelected());
        addAction(VK_TAB, "TAB", "Toggle Pick Modes", ()-> setPickMode(Utils.incrementValue(pickMode, PickMode.values())));
        addAction(VK_N, "N", "Toggle Show Numbers", ()->showNumbers = !showNumbers);
        addAction(Utils.toIntArray(VK_PLUS, VK_EQUALS), "+", "Zoom in", ()->zoomIn());
        addAction(VK_MINUS, "-", "Zoom out", ()->zoomOut());
        addAction(VK_LEFT, "<", "Adjust Vertex Left", ()->moveVertex(-1, 0));
        addAction(VK_RIGHT, ">", "Adjust Vertex Right", ()->moveVertex(1, 0));
        addAction(VK_UP, "^", "Adjust Vertex Up", ()->moveVertex(0, -1));
        addAction(VK_DOWN, "v", "Adjust Vertex Down", ()->moveVertex(0, 1));
    }

    void zoomIn() {
        rect.scale(.5f);
        frame.setProperty("rect", rect);
    }

    void zoomOut() {
        rect.scale(2);
        rect.x=0;
        rect.y=0;
        rect.w = Math.min(rect.w, getViewportWidth());
        rect.h=Math.min(rect.h, getViewportHeight());
        frame.setProperty("rect", rect);
    }

    protected void addAction(int code, String key, String description, Runnable action) {
        actions.put(code, new KeyAction(key, description, action));
    }

    protected void addAction(int [] codes, String key, String description, Runnable action) {
        KeyAction a = new KeyAction(key, description, action);
        for (int code : codes) {
            actions.put(code, a);
        }
    }

    private void deleteSelected() {
        Collections.sort(selected);
        Collections.reverse(selected);
        switch (pickMode) {
            case CELL:
                for (int idx : selected)
                    board.removeCell(idx);
                break;
            case EDGE: {
                //board.removeEdge(getSelectedIndex());
                Action a = null;
                if (getSelectedIndex() >= 0) {
                    final E e = board.getEdge(getSelectedIndex());
                    board.removeEdge(getSelectedIndex());
                    pushUndoAction(() -> board.addEdge(e));
                }

                break;
            }
            case VERTEX:
                for (int idx : selected)
                    board.removeVertex(idx);
                break;
        }
        selected.clear();
    }

    @Override
    public final synchronized void keyTyped(KeyEvent evt) {
    }

    protected void moveVertex(float dx, float dy) {
        if (pickMode == PickMode.VERTEX && getSelectedIndex() >= 0) {
            board.moveVertex(getSelectedIndex(), new Vector2D(dx, dy));
        }
    }

    @Override
    public final synchronized void keyPressed(KeyEvent evt) {
        if (actions.containsKey(evt.getKeyCode())) {
            actions.get(evt.getKeyCode()).action.run();
            evt.consume();
        } else if (actions.containsKey(evt.getExtendedKeyCode())) {
            actions.get(evt.getExtendedKeyCode()).action.run();
            evt.consume();
        } else {
            System.err.println("Unhandled key type:" + evt.getKeyChar());
        }
        repaint();
    }

    void setPickMode(PickMode mode) {
        pickMode = mode;
        frame.setProperty("pickMode", pickMode.name());
        selected.clear();
    }

    void setMultiselect(boolean m) {
        multiSelect = m;
        frame.setProperty("multiSelect", multiSelect);
        selected.clear();
    }

    /**
     * to use, override the initFrame method and make calls to this method from there
     *
     * @param tool
     */
    public final void registerTool(Tool tool) {
        if (tools.contains(tool.name))
            throw new GException("A tool with name " + tool.name + " already exists");
        tools.add(tool);
    }
}
