package cc.lib.swing;

import java.io.File;
import java.io.PrintWriter;

import cc.lib.checkers.ACheckboardGame;
import cc.lib.checkers.Checkers;
import cc.lib.checkers.Dama;
import cc.lib.checkers.Draughts;
import cc.lib.checkers.Move;
import cc.lib.checkers.MoveType;
import cc.lib.checkers.Piece;
import cc.lib.checkers.Robot;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.MiniMaxTree;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class AWTCheckers extends AWTComponent {

    public static void main(String [] args) {
        new AWTCheckers();
    }

    ACheckboardGame game = null;
    EZFrame frame;
    File SAVE_FILE = new File("checkers.save");
    Robot robot;

    AWTCheckers() {
        frame = new EZFrame() {
            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (menu) {
                    case "File": onFileMenu(subMenu); break;
                    default: super.onMenuItemSelected(menu, subMenu);
                }
            }

            @Override
            protected void onWindowClosing() {
                if (game != null) {
                    try {
                        Reflector.serializeToFile(game, SAVE_FILE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        setMouseEnabled(true);
        setPadding(10);
        frame.addMenuBarMenu("File", "New Game", "Load Game", "Save as");
        frame.add(this);
        if (!frame.loadFromFile(new File("checkers.properties")))
            frame.centerToScreen(640, 640);

        try {
            if (SAVE_FILE.exists()) {
                game = Reflector.deserializeFromFile(SAVE_FILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (game == null) {
            game = new Checkers();
            game.newGame();
        }

    }

    enum GameType {
        Checkers {
            @Override
            ACheckboardGame newGame() {
                return new Checkers();
            }
        },
        /*
        Chess {
            @Override
            ACheckboardGame newGame() {
                return new Chess();
            }
        },*/
        Draughts {
            @Override
            ACheckboardGame newGame() {
                return new Draughts();
            }
        },
        Dama {
            @Override
            ACheckboardGame newGame() {
                return new Dama();
            }
        };

        abstract ACheckboardGame newGame();
    }

    void onFileMenu(String item) {
        switch (item) {
            case "New Game":
                int index = frame.showItemChooserDialog("New Game", "Choose Game Type", Utils.toStringArray(GameType.values()));
                if (index >= 0) {
                    int num = frame.showItemChooserDialog("New Game", "Choose Single or Multipllayer", "Single", "Multi");
                    if (num < 0)
                        break;
                    robot = null;
                    switch (num) {
                        case 0:
                            // single
                            robot = new Robot();
                        case 1:
                            // multi
                            game = GameType.values()[index].newGame();

                    }
                    game.newGame();
                }
                break;

            case "Load Game":
            case "Save as":
        }
    }

    int selectedRank=-1, selectedCol=-1;
    int highlightedRank, highlightedCol;
    Move selectedMove = null;
    final static GColor DAMA_BOARD_COLOR = new GColor(0xfffde9a9);

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        g.pushMatrix();
        if (game != null) try {
            int dim = Math.min(getWidth(), getHeight());

            int sx = getWidth()/2 - dim/2;
            int sy = getHeight()/2 - dim/2;

            g.translate(sx, sy);

            int dw = dim/game.COLUMNS;
            int dh = dim/game.RANKS;


            // draw the baord
            switch (game.getBoardType()) {
                case CHECKERS: {
                    GColor [] colors = {
                            GColor.LIGHT_GRAY,
                            GColor.BLACK
                    };

                    int cIndex = 0;
                    for (int i = 0; i < game.COLUMNS; i++) {
                        for (int ii = 0; ii < game.RANKS; ii++) {
                            g.setColor(colors[cIndex]);
                            g.drawFilledRect(i * dw, ii * dh, dw, dh);
                            cIndex = (cIndex + 1) % colors.length;
                        }
                        cIndex = (cIndex + 1) % colors.length;
                    }
                    break;
                }

                case DAMA: {
                    g.setColor(DAMA_BOARD_COLOR);
                    g.drawFilledRect(0, 0, dim, dim);
                    g.setColor(GColor.BLACK);
                    for (int i=0; i<=game.COLUMNS; i++) {
                        g.drawLine(i*dw, 0, i*dw, dim, 5);
                    }
                    for (int i=0; i<=game.RANKS; i++) {
                        g.drawLine(0, i*dh, dim, i*dh, 5);
                    }
                }
                break;
            }

            // draw the pieces and figure out what rect the mouse is over

            highlightedRank=highlightedCol =-1;
            mouseX -= sx;
            mouseY -= sy;

            for (int r=0; r<game.RANKS; r++) {
                for (int c=0; c<game.COLUMNS; c++) {
                    int x = c*dw;
                    int y = r*dh;

                    Piece p = game.getPiece(r, c);
                    if (p.playerNum >= 0)
                        drawPiece(g, p, x, y, dw, dh);

                    if (Utils.isPointInsideRect(mouseX, mouseY, x, y, dw, dh)) {
                        highlightedCol = c;
                        highlightedRank = r;
                        g.setColor(GColor.GREEN);
                        g.drawRect(x, y, dw, dh, 5);
                    }

                }
            }

            selectedMove = null;
            if (selectedCol >= 0 && selectedRank >= 0) {
                Piece p = game.getPiece(selectedRank, selectedCol);
                if (p.playerNum == game.getTurn()) {
                    for (Move m : p.moves) {
                        if (m.hasEnd()) {
                            g.setColor(GColor.GREEN);
                            g.drawCircleWithThickness(m.getEnd()[1]*dw + dw / 2, m.getEnd()[0]*dh + dh / 2, Math.min(dw, dh) * 1 / 3 + 2, 5);
                            if (highlightedRank == m.getEnd()[0] && highlightedCol == m.getEnd()[1])
                                selectedMove = m;
                            System.out.println("selected move = " + m);
                        }
                    }
                }
            } else {
                // draw the movable pieces
                g.setColor(GColor.CYAN);
                for (Move m : game.getMoves()) {
                    int[] pos = m.getStart();
                    if (pos != null) {
                        int y = pos[0] * dh;
                        int x = pos[1] * dw;
                        g.drawCircleWithThickness(x + dw / 2, y + dh / 2, Math.min(dw, dh) * 1 / 3 + 2, 5);
                    }
                }
            }

            for (Move m : game.getMoves()) {
                if (m.getMoveType() == MoveType.END) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER, Justify.CENTER, "Press 'e' to end turn");
                    break;
                }
            }

        } finally {
            g.popMatrix();
        }
    }

    GColor getColor(int playerNum) {
        switch (playerNum) {
            case 0:
                return GColor.RED;
            case 1:
                return GColor.DARK_GRAY;
        }
        throw new AssertionError("Unhandled Case");
    }

    void drawPiece(AGraphics g, Piece p, int x, int y, int w, int h) {
        g.pushMatrix();
        g.translate(x+w/2, y+h/2);
        g.scale(Math.min(w,h)*1/3);
        g.setColor(getColor(p.playerNum));
        g.drawFilledCircle(0, 0, 1);
        switch (p.type) {

            case EMPTY:
                break;
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "PAWN");
                break;
            case BISHOP:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "BSHP");
                break;
            case KNIGHT:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "KNGT");
                break;
            case ROOK:
            case ROOK_IDLE:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "ROOK");
                break;
            case QUEEN:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "QUEN");
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "KING");
                break;
            case KING:
            case FLYING_KING:
            case DAMA_KING:
                g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "K");
            case CHECKER:
            case DAMA_MAN:
                break;
            case UNAVAILABLE:
                break;
        }
        g.popMatrix();
    }

    @Override
    protected void onClick() {
        if (selectedMove != null && highlightedRank == selectedMove.getEnd()[0] && highlightedCol == selectedMove.getEnd()[1]) {
            game.executeMove(selectedMove);
            selectedCol = selectedRank = highlightedRank = highlightedCol = -1;
            if (robot != null && game.getTurn() == 1) {
                new Thread() {
                    public void run() {
                        MiniMaxTree.MMTreeNode<Move, ACheckboardGame> root = new MiniMaxTree.MMTreeNode(game);
                        robot.doRobot(game, root);
                        root.dumpTree(new PrintWriter(System.out));
                    }
                }.start();
            }
        } else if (selectedRank>=0 && selectedCol>=0){
            selectedRank=selectedCol=-1;
        } else {
            selectedRank=highlightedRank;
            selectedCol=highlightedCol;
        }
        repaint();
    }

    @Override
    protected void onKeyPressed(VKKey key) {
        if (game == null)
            return;
        if (game.getTurn() == 1 && robot != null)
            return;
        switch (key) {
            case VK_B:
                if (game.canUndo())
                    game.undo();
                break;
            case VK_E: {
                for (Move m : game.getMoves()) {
                    if (m.getMoveType() == MoveType.END) {
                        game.executeMove(m);
                        break;
                    }
                }
                break;
            }

            case VK_UP:
                if (highlightedRank < game.RANKS-1)
                    highlightedRank++;
                break;
            case VK_DOWN:
                if (highlightedRank > 0)
                    highlightedRank--;
                break;
            case VK_LEFT:
                if (highlightedCol > 0)
                    highlightedCol--;
                break;
            case VK_RIGHT:
                if (highlightedCol < game.COLUMNS-1)
                    highlightedCol++;
                break;

            case VK_ENTER:
                onClick();
                break;

            default:
                super.onKeyTyped(key);
        }
        repaint();
    }
}
