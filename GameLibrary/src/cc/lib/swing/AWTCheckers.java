package cc.lib.swing;

import java.io.File;

import cc.lib.checkers.ACheckboardGame;
import cc.lib.checkers.Checkers;
import cc.lib.checkers.Dama;
import cc.lib.checkers.Draughts;
import cc.lib.checkers.Move;
import cc.lib.checkers.Piece;
import cc.lib.checkers.Robot;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
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
                    game.trySaveToFile(SAVE_FILE);
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

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (game != null) {
            int dim = Math.min(getWidth(), getHeight());

            int sx = getWidth()/2 - dim/2;
            int sy = getHeight()/2 - dim/2;

            g.pushMatrix();
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
                    }
                    break;
                }

                case DAMA: {
                    g.setColor(new GColor(0xfffde9a9));
                    g.drawFilledRect(0, 0, dim, dim);
                    g.setColor(GColor.BLACK);
                    for (int i=0; i<=game.COLUMNS; i++) {
                        g.drawLine(i*dw, 0, i*dw, dim, 5);
                    }
                    for (int i=0; i<game.RANKS; i++) {
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
                for (Move m : game.getMoves()) {
                    int [] pos = m.getStart();
                    if (selectedRank == pos[0] && selectedCol == pos[1]) {
                        pos = m.getEnd();
                        int y = pos[0] * dh;
                        int x = pos[1] * dw;
                        g.drawCircleWithThickness(x + dw / 2, y + dh / 2, Math.min(dw, dh) * 2 / 3 + 2, 5);
                        selectedMove = m;
                        break;
                    }
                }
            } else {
                // draw the movable pieces
                g.setColor(GColor.CYAN);
                for (Move m : game.getMoves()) {
                    int[] pos = m.getStart();
                    int y = pos[0] * dh;
                    int x = pos[1] * dw;
                    g.drawCircleWithThickness(x + dw / 2, y + dh / 2, Math.min(dw, dh) * 2 / 3 + 2, 5);
                }
            }

            g.popMatrix();

        }
    }

    GColor getColor(int playerNum) {
        switch (playerNum) {
            case 0:
                return GColor.RED;
            case 1:
                return GColor.BLACK;
        }
        throw new AssertionError("Unhandled Case");
    }

    void drawPiece(AGraphics g, Piece p, int x, int y, int w, int h) {
        switch (p.type) {

            case EMPTY:
                break;
            case PAWN:
                break;
            case PAWN_IDLE:
                break;
            case PAWN_ENPASSANT:
                break;
            case PAWN_TOSWAP:
                break;
            case BISHOP:
                break;
            case KNIGHT:
                break;
            case ROOK:
                break;
            case ROOK_IDLE:
                break;
            case QUEEN:
                break;
            case CHECKED_KING:
                break;
            case CHECKED_KING_IDLE:
                break;
            case UNCHECKED_KING:
                break;
            case UNCHECKED_KING_IDLE:
                break;
            case KING:
            case FLYING_KING:
            case CHECKER:
            case CAPTURED_CHECKER:
            case DAMA_MAN:
            case DAMA_KING:
            case CAPTURED_DAMA:
                g.setColor(getColor(p.playerNum));
                g.drawFilledCircle(x+w/2, y+h/2, Math.min(w,h)*2/3);
                break;
            case UNAVAILABLE:
                break;
        }
    }

    @Override
    protected void onClick() {
        if (selectedMove != null && highlightedRank == selectedMove.getEnd()[0] && highlightedCol == selectedMove.getEnd()[1]) {
            game.executeMove(selectedMove);
            if (robot != null && game.getTurn() == 1) {
                new Thread() {
                    public void run() {

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
}
