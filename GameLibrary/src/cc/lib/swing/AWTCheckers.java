package cc.lib.swing;

import java.awt.event.KeyEvent;
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
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.DescisionTree;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

import static java.awt.event.KeyEvent.*;

public class AWTCheckers extends AWTComponent {

    public static void main(String [] args) {
        Utils.setDebugEnabled();
        new AWTCheckers();
    }

    ACheckboardGame game = null;
    final AWTFrame frame;
    final File SAVE_FILE;
    Robot robot;

    class AWTRobot extends Robot {

        AWTRobot(int difficulty) {
            super(difficulty);
        }

        @Override
        protected void onNewMove(Move m) {
            super.onNewMove(m);
            repaint();
        }
    }

    AWTCheckers() {
        frame = new AWTFrame() {
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
        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        if (!frame.loadFromFile(new File(settings, "checkers.properties")))
            frame.centerToScreen(640, 640);
        SAVE_FILE = new File(settings,"checkers.save");
        try {
            if (SAVE_FILE.exists()) {
                game = Reflector.deserializeFromFile(SAVE_FILE);
                if (game.singlePlayerDifficulty >= 0) {
                    robot = new AWTRobot(game.singlePlayerDifficulty);
                    checkRobotTurn();
                } else {
                    robot = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (game == null) {
            game = new Checkers();
            game.newGame();
            game.singlePlayerDifficulty = 1;
            game.setTurn(0);
            robot = new AWTRobot(1);
        }

    }

    enum GameType {
        Checkers {
            @Override
            ACheckboardGame create() {
                return new Checkers();
            }
        },
        /*
        Chess {
            @Override
            ACheckboardGame create() {
                return new Chess();
            }
        },*/
        Draughts {
            @Override
            ACheckboardGame create() {
                return new Draughts();
            }
        },
        Dama {
            @Override
            ACheckboardGame create() {
                return new Dama();
            }
        };

        abstract ACheckboardGame create();
    }

    boolean isRobotTurn() {
        return robot != null && game.getTurn() == 1;
    }

    synchronized void onFileMenu(String item) {
        switch (item) {
            case "New Game":
                int index = frame.showItemChooserDialog("New Game", "Choose Game Type", Utils.toStringArray(GameType.values(), true));
                if (index >= 0) {
                    int num = frame.showItemChooserDialog("New Game", "Choose Single or Multipllayer", "Single", "Multi");
                    if (num < 0)
                        break;
                    robot = null;
                    switch (num) {
                        case 0:
                            // single
                            robot = new AWTRobot(1);
                        case 1:
                            // multi
                    }
                    game = GameType.values()[index].create();
                    game.newGame();
                    game.singlePlayerDifficulty = -1;
                    if (robot != null)
                        game.singlePlayerDifficulty = robot.getDifficulty().ordinal();
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

    final float CHECKER_HEIGHT = 0.2f;

    private void drawCheckerKing(AGraphics g, GColor outlineColor) {
        if (outlineColor != null) {
            GColor c = g.getColor();
            g.setColor(outlineColor);
            g.pushMatrix();
            drawChecker(g, outlineColor != null);
            g.translate(0, -CHECKER_HEIGHT);
            drawChecker(g, outlineColor != null);
            g.popMatrix();
            g.setColor(c);
        }
        g.pushMatrix();
        drawChecker(g, false);
        g.translate(0, -CHECKER_HEIGHT);
        drawChecker(g, false);
        g.popMatrix();
    }

    private void drawChecker(AGraphics g, boolean outlined) {
        float vr = 2-CHECKER_HEIGHT;
        GColor c = g.getColor();
        if (!outlined)
            g.setColor(c.darkened(0.5f));
        g.drawFilledOval(-1f, 1f-vr, 2, vr);
        g.drawFilledRect(-1, -CHECKER_HEIGHT/2, 2, CHECKER_HEIGHT);
        g.setColor(c);
        g.drawFilledOval(-1f, -1f, 2, vr);
    }

    /*
        float hgt = CHECKER_HEIGHT;
        GColor c = g.getColor();
        if (!outlined)
            g.setColor(c.darkened(0.5f));
        g.drawFilledOval(-1f, -hgt/2, 2, 2-hgt);
        g.drawFilledRect(-1f, -hgt/2, 2, hgt);
        g.setColor(c);
        g.drawFilledOval(-1f, -1f, 2, 2-hgt);
    }*/

    float cellWidth = 1;
    float cellHeight = 1;

    @Override
    protected synchronized void paint(AWTGraphics g, int mouseX, int mouseY) {
        //Utils.println("RENDER");
        g.pushMatrix();
        if (game != null) try {
            int dim = Math.min(getWidth(), getHeight());

            int sx = getWidth()/2 - dim/2;
            int sy = getHeight()/2 - dim/2;

            g.translate(sx, sy);

            cellWidth = dim/game.COLUMNS;
            cellHeight = dim/game.RANKS;


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
                            g.drawFilledRect(i * cellWidth, ii * cellHeight, cellWidth, cellHeight);
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
                        g.drawLine(i*cellWidth, 0, i*cellWidth, dim, 5);
                    }
                    for (int i=0; i<=game.RANKS; i++) {
                        g.drawLine(0, i*cellHeight, dim, i*cellHeight, 5);
                    }
                }
                break;
            }

            // draw the pieces and figure out what rect the mouse is over

            highlightedRank=highlightedCol =-1;

            if (isRobotTurn()) {
                return;
            }

            mouseX -= sx;
            mouseY -= sy;

            Piece highlightedPiece = null;

            for (int r=0; r<game.RANKS; r++) {
                for (int c=0; c<game.COLUMNS; c++) {
                    float x = c*cellWidth;
                    float y = r*cellHeight;

                    Piece p = game.getPiece(r, c);
                    if (p.getPlayerNum() >= 0)
                        drawPiece(g, p, x, y, null);

                    if (Utils.isPointInsideRect(mouseX, mouseY, x, y, cellWidth, cellHeight)) {
                        highlightedCol = c;
                        highlightedRank = r;
                        if (p.getPlayerNum() >= 0)
                            highlightedPiece = p;
                        g.setColor(GColor.GREEN);
                        g.drawRect(x, y, cellWidth, cellHeight, 5);
                    }

                }
            }

            selectedMove = null;
            if (selectedCol >= 0 && selectedRank >= 0) {
                Piece p = game.getPiece(selectedRank, selectedCol);
                if (p.getPlayerNum() == game.getTurn()) {
                    for (Move m : p.getMoves()) {
                        if (m.hasEnd()) {
                            g.setColor(GColor.GREEN);
                            g.drawCircle(m.getEnd()[1]*cellWidth + cellWidth / 2, m.getEnd()[0]*cellHeight + cellHeight / 2, Math.min(cellWidth, cellHeight) * 1 / 3 + 2, 5);
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
                        float y = pos[0] * cellHeight;
                        float x = pos[1] * cellWidth;
                        //g.drawCircleWithThickness(x + cellWidth / 2, y + cellHeight / 2, Math.min(cellWidth, cellHeight) * 1 / 3 + 2, 5);
                        Piece p = game.getPiece(pos[0], pos[1]);
                        if (p.a != null) {
                            p.a.update(g);
                            if (p.a.isDone())
                                p.a = null;
                        }
                        else {
                            drawPiece(g, p, x, y, GColor.CYAN);
                            drawPiece(g, p, x, y, null);
                        }
                    }
                }
            }

            if (highlightedPiece != null) {
                float x = cellWidth*highlightedCol;
                float y = cellHeight*highlightedRank;
                drawPiece(g, highlightedPiece, x, y, null);
            }
/*
            for (int r=0; r<game.RANKS; r++) {
                for (int c=0; c<game.COLUMNS; c++) {
                    float x = c*cellWidth;
                    float y = r*cellHeight;

                    Piece p = game.getPiece(r, c);
                    if (p.getPlayerNum() >= 0)
                        drawPiece(g, p, x, y, false);

                    if (Utils.isPointInsideRect(mouseX, mouseY, x, y, cellWidth, cellHeight)) {
                        highlightedCol = c;
                        highlightedRank = r;
                        g.setColor(GColor.GREEN);
                        g.drawRect(x, y, cellWidth, cellHeight, 5);
                    }

                }
            }*/

            for (Move m : game.getMoves()) {
                if (m.getMoveType() == MoveType.END) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER, Justify.CENTER, "Press 'e' to end turn");
                    break;
                }
            }

        } finally {
            g.popMatrix();
            if (isRobotTurn()) {
                g.setColor(GColor.GREEN);
                g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER, Justify.CENTER, "THINKING");
            }
        }

    }

    GColor getColor(int playerNum) {
        switch (playerNum) {
            case 0:
                return GColor.RED;
            case 1:
                return GColor.BLUE;
        }
        throw new AssertionError("Unhandled Case");
    }

    void drawPiece(AGraphics g, Piece p, float x, float y, GColor outlineColor) {
        if (outlineColor == null && p.isCaptured()) {
            drawPiece(g, p, x, y, GColor.PINK);
        }
        g.pushMatrix();
        float w = cellWidth;
        float h = cellHeight;
        g.translate(x+w/2, y+h/2);
        g.scale(Math.min(w,h)*1/3);
        g.setColor(getColor(p.getPlayerNum()));
        if (outlineColor != null) {
            g.setColor(outlineColor);
            g.scale(1.2f);
        }

//        g.drawFilledCircle(0, 0, 1);
        switch (p.getType()) {

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
                drawCheckerKing(g, outlineColor);
                break;
            case CHECKER:
            case DAMA_MAN:
                drawChecker(g, outlineColor != null);
                break;
            case UNAVAILABLE:
                break;
        }
        g.popMatrix();
    }

    void checkRobotTurn() {
        if (isRobotTurn()) {
            new Thread() {
                public void run() {
                    ACheckboardGame g = game.deepCopy();
                    DescisionTree root = new DescisionTree(0);
                    robot.doRobot(g, root);
                    root.dumpTreeXML(new PrintWriter(System.out));
                    /*
                    for (Move m : root.getPath()) {
                        animateMove(m);
                        game.executeMove(m);
                    }*/
                    repaint();
                    Utils.print("Thread " + Thread.currentThread().getName() + " DONE");
                }

            }.start();
        }
    }

    void animateMove(final Move m) {
        AAnimation<AGraphics> a = null;
        switch (m.getMoveType()) {
            case SLIDE:
                game.getPiece(m.getStart()).a = a = new AAnimation<AGraphics>(1000) {
                    @Override
                    protected void draw(AGraphics g, float position, float dt) {
                        int [] start = m.getStart();
                        int [] end = m.getEnd();
                        Piece p = game.getPiece(start);
                        float sx = start[1];
                        float sy = start[0];
                        float ex = end[1];
                        float ey = end[0];
                        float x = (ex-sx) * position;
                        float y = (ey-sy) * position;
                        g.pushMatrix();
                        g.translate(x, y);
                        drawPiece(g, p, x, y, null);
                    }

                    @Override
                    protected void onDone() {
                        synchronized (this) {
                            notify();
                        }
                    }
                };
        }

        if (a != null) {
            a.start();
            repaint();
            Utils.waitNoThrow(a, 2000);
        }
    }

    @Override
    protected void onClick() {
        if (selectedMove != null && highlightedRank == selectedMove.getEnd()[0] && highlightedCol == selectedMove.getEnd()[1]) {
            game.executeMove(selectedMove);
            selectedCol = selectedRank = highlightedRank = highlightedCol = -1;
            checkRobotTurn();
        } else if (selectedRank>=0 && selectedCol>=0){
            selectedRank=selectedCol=-1;
        } else {
            Piece p = game.getPiece(highlightedRank, highlightedCol);
            if (p != null && p.getPlayerNum() == game.getTurn()) {
                selectedRank = highlightedRank;
                selectedCol = highlightedCol;
            }
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (game == null)
            return;
        if (game.getTurn() == 1 && robot != null)
            return;
        switch (e.getKeyCode()) {
            case VK_B:
                if (game.canUndo())
                    game.undo();
                break;
            case VK_E: {
                for (Move m : game.getMoves()) {
                    if (m.getMoveType() == MoveType.END) {
                        game.executeMove(m);
                        checkRobotTurn();
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
        }
        repaint();
    }
}
