package cc.lib.swing;

import java.io.File;
import java.util.Properties;

import cc.lib.board.BVertex;
import cc.lib.board.GenericBoard;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
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
            switch (menu) {
                case "File":
                    onFileMenu(subMenu);
                    break;

                default:
                    log.warn("Unhandled case %s", menu);
            }
        }

        @Override
        protected void onWindowClosing() {
            board.trySaveToFile(new File("bb.backup.board"));
        }
    };

    GenericBoard board = new GenericBoard();
    int background = -1;

    BoardBuilder() {
        setMouseEnabled(true);
        setPadding(10);
        frame.addMenuBarMenu("File", "New Board", "Load Board", "Load Image", "Clear Image", "Save As...", "Save");
        frame.add(this);
        board.tryLoadFromFile(new File("bb.backup.board"));
        if (!frame.loadFromFile(new File("bb.properties")))
            frame.centerToScreen(640, 480);
    }

    @Override
    protected void init(AWTGraphics g) {
        String image = frame.getProperties().getProperty("image");
        if (image != null)
            background = g.loadImage(image);
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
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
        if (selectedVertex >= 0) {
            g.setColor(GColor.RED);
            g.vertex(board.getVertex(selectedVertex));
            g.drawPoints();
        }
    }

    File boardFile = null;

    void onFileMenu(String item) {
        switch (item) {
            case "New Board":
                board = new GenericBoard();
                break;
            case "Load Board": {
                File file = showFileOpenChooser(frame, "Load Board", "board");
                if (file != null) {
                    try {
                        GenericBoard b = new GenericBoard();
                        b.loadFromFile(file);
                        board = b;
                        boardFile = file;
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
                        boardFile = file;
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

    int selectedVertex = -1;

    @Override
    protected void onClick() {
        int picked = board.pickVertex(getAPGraphics(), getMouseX(), getMouseY());
        if (picked < 0) {
            picked = board.addVertex(getMousePos());
        }
        if (selectedVertex >= 0 && selectedVertex != picked) {
            board.addEdge(selectedVertex, picked);
        }
        selectedVertex = picked;
        repaint();
    }

    IVector2D getMousePos() {
        return getAPGraphics().screenToViewport(getMouseX(), getMouseY());
    }

    @Override
    protected void onDragStarted(int x, int y) {
        if (selectedVertex < 0) {
            selectedVertex = board.pickVertex(getAPGraphics(), x, y);
        } else {
            BVertex v = board.getVertex(selectedVertex);
            v.set(getMousePos());
        }
        repaint();
    }

    @Override
    protected void onDragStopped() {
        selectedVertex = -1;
        repaint();
    }

}
