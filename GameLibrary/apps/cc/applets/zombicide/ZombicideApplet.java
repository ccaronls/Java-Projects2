package cc.applets.zombicide;

import java.io.File;
import java.util.List;

import cc.applets.typing.LearnToType;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.FileUtils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZZombieType;

public class ZombicideApplet extends AWTKeyboardAnimationApplet {

    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        //mode = 0;
        AWTFrame frame = new AWTFrame("Zombicide");
        AWTKeyboardAnimationApplet app = new ZombicideApplet();
        frame.add(app);
        app.init();
        app.start();
        app.setMillisecondsPerFrame(20);
        File settings = FileUtils.getOrCreateSettingsDirectory(LearnToType.class);
        frame.setPropertiesFile(new File(settings, "application.properties"));
        if (!frame.restoreFromProperties())
            frame.centerToScreen(800, 600);


    }

    ZGame game = new ZGame();
    List options;
    String message = "";
    Object monitor = new Object();
    Object result = null;
    UIMode uiMode = UIMode.NONE;

    enum UIMode {
        NONE,
        PICK_CHARACTER,
        PICK_ZONE,
        PICK_MENU,
        PICK_ZOMBIE
    }

    @Override
    protected void doInitialization() {
        ZUser user = new ZUser() {
            @Override
            public Integer chooseCharacter(List<Integer> characters) {
                message = "Pick character to play";
                uiMode = UIMode.PICK_CHARACTER;
                options = characters;
                return waitForUser();
            }

            @Override
            public Integer chooseZoneForBile(ZGame zGame, ZCharacter cur, List<Integer> zones) {
                message = "Pick a zone to place the Dragon Bile";
                uiMode = UIMode.PICK_ZONE;
                options = zones;
                return waitForUser();
            }

            @Override
            public ZMove chooseMove(ZGame zGame, ZCharacter cur, List<ZMove> moves) {
                message = "Choose Move";
                uiMode = UIMode.PICK_MENU;
                options = moves;
                return waitForUser();
            }

            @Override
            public ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions) {
                message = "Choose New Skill";
                uiMode = UIMode.PICK_MENU;
                options = skillOptions;
                return waitForUser();
            }
        };
        user.addCharacter(ZPlayerName.Baldric.create());
        user.addCharacter(ZPlayerName.Clovis.create());
        game.setUsers(user);
        game.loadQuest(ZQuests.Tutorial);
    }

    <T> T waitForUser() {
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (Exception e) {

            }
        }
        return (T)result;
    }

    void setResult(Object result) {
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    int [] selected = null;
    int mode = 0; // 0 == debug, 1 == choose players / quest, 2 == in game

    @Override
    protected void drawFrame(AGraphics g) {
        ZBoard board = game.board;
        int [] highlighted = board.drawDebug(g, getMouseX(), getMouseY());
        ZActor selectedActor = board.drawActors(g, getMouseX(), getMouseY());
        if (highlighted != null) {
            if (getKeyboardReset('a')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_WEST);
            } else if (getKeyboardReset('w')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_NORTH);
            } else if (getKeyboardReset('s')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_SOUTH);
            } else if (getKeyboardReset('d')) {
                board.toggleDorOpen(highlighted, ZBoard.DIR_EAST);
            }

            if (getMouseButtonClicked(0)) {
                selected = highlighted;
            } else if (selected != null) {
                g.setColor(GColor.RED);
                if (board.canSeeCell(selected, highlighted)) {
                    g.setColor(GColor.GREEN);
                }
                g.drawRect(board.getCell(selected).getRect());
            }
        }
    }

    boolean loaded = false;

    void loadImages(AGraphics g) {
        if (loaded)
            return;
        ZZombieType.Abomination.imageId = g.loadImage("zabomination.png");
        ZZombieType.Necromancer.imageId = g.loadImage("znecro.png");
        ZZombieType.Walker1.imageId = g.loadImage("zwalker1.png");
        ZZombieType.Walker2.imageId = g.loadImage("zwalker2.png");
        ZZombieType.Walker3.imageId = g.loadImage("zwalker3.png");
        ZZombieType.Walker4.imageId = g.loadImage("zwalker4.png");
        ZZombieType.Walker5.imageId = g.loadImage("zwalker5.png");
        ZZombieType.Runner1.imageId = g.loadImage("zrunner1.png");
        ZZombieType.Runner2.imageId = g.loadImage("zrunner1.png");
        ZZombieType.Fatty1.imageId = g.loadImage("zfatty1.png");
        ZZombieType.Fatty2.imageId = g.loadImage("zfatty2.png");
        ZPlayerName.Clovis.imageId = g.loadImage("zchar_clovis.png");
        ZPlayerName.Baldric.imageId = g.loadImage("zchar_baldric.png");
        ZPlayerName.Ann.imageId = g.loadImage("zchar_ann.png");
        ZPlayerName.Nelly.imageId = g.loadImage("zchar_nelly.png");
        ZPlayerName.Samson.imageId = g.loadImage("zchar_samson.png");
        ZPlayerName.Silas.imageId = g.loadImage("zchar_silas.png");
        loaded = true;
    }


    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        GDimension cellDim = game.board.initCellRects(g, g.getViewportWidth()-5, g.getViewportHeight()-5);
        loadImages(g);
        // now fit all the actors cell dimensions
    }
}