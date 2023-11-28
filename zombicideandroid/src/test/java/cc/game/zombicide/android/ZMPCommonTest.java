package cc.game.zombicide.android;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import cc.lib.net.AClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTestUser;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.p2p.ZUserMP;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ZMPCommonTest {

    ZGame game = new ZGame();
    ZGame game2 = new ZGame();

    @Test
    public void testMPCommon() throws Exception {

        ZMPCommon.SVR svr = new ZMPCommon.SVR() {
            @Override
            public void parseCLCommand(@NotNull AClientConnection conn, @NotNull GameCommand cmd) {

            }

            @NotNull
            @Override
            public GameCommand newUpdateGameCommand(@NotNull ZGame game) {
                return null;
            }

            @NotNull
            @Override
            public GameCommand newAssignPlayer(@NotNull Assignee assignee) {
                return null;
            }

            @NotNull
            @Override
            public GameCommand newLoadQuest(@NotNull ZQuests quest) {
                return null;
            }

            @Nullable
            @Override
            public GameCommand newInit(int clientColor, int maxCharacters, @NotNull List<Assignee> playerAssignments) {
                return null;
            }

            @Override
            public void onChooseCharacter(AClientConnection conn, ZPlayerName name, boolean checked) {
                Assert.assertEquals(name, ZPlayerName.Ann);
                Assert.assertEquals(checked, true);
            }

            @Override
            public void onStartPressed(AClientConnection conn) {

            }

            @Override
            public void onError(Exception e) {
                Assert.assertFalse(true);
            }

            @Override
            public void onUndoPressed(AClientConnection conn) {

            }
        };


        ZMPCommon.CL cl = new ZMPCommon.CL() {

            @Override
            public void parseSVRCommand(@NotNull GameCommand cmd) {

            }

            @NotNull
            @Override
            public GameCommand newUndoPressed() {
                return null;
            }

            @NotNull
            @Override
            public GameCommand newStartPressed() {
                return null;
            }

            @NotNull
            @Override
            public GameCommand newAssignCharacter(@NotNull ZPlayerName name, boolean checked) {
                return null;
            }

            @Override
            public void onLoadQuest(ZQuests quest) {

            }

            @Override
            public void onInit(int color, int maxCharacters, List<Assignee> playerAssignments) {
                Assert.assertEquals(maxCharacters, 2);
                Assert.assertEquals(color, 2);
                Assert.assertNotNull(playerAssignments);
            }

            @Override
            public void onAssignPlayer(Assignee assignee) {
                Assert.assertNotNull(assignee);
                Assert.assertEquals(assignee.getName(), ZPlayerName.Baldric);
                Assert.assertEquals(assignee.getUserName(), "Chris");
                Assert.assertEquals(assignee.getColor(), 2);
                Assert.assertEquals(assignee.getChecked(), true);
            }

            @Override
            public void onError(Exception e) {
                Assert.assertFalse(true);
            }

            @Override
            public ZGame getGameForUpdate() {
                return game;
            }

            @Override
            public void onGameUpdated(ZGame game) {
                Assert.assertNotNull(game.board);
                System.out.println(game.toStringNumbered());

                Assert.assertEquals(game.getAllCharacters().size(), 2);
                for (ZCharacter c : game.board.getAllCharacters()) {
                    if (c.getType() == ZPlayerName.Baldric) {
                        Assert.assertEquals(c.getExp(), 5);
                    }
                }
            }
        };

        svr.parseCLCommand(null, transfer(cl.newAssignCharacter(ZPlayerName.Ann, true)));
        cl.parseSVRCommand(transfer(svr.newInit( 2, 2, new ArrayList<>())));
        cl.parseSVRCommand(transfer(svr.newAssignPlayer(new Assignee(ZPlayerName.Baldric, "Chris", 2, true))));
        game2.loadQuest(ZQuests.The_Black_Book);
        game2.addCharacter(ZPlayerName.Baldric);
        game2.addCharacter(ZPlayerName.Nelly);
        game = game2.deepCopy();
        System.out.println(game2.toStringNumbered());
        Assert.assertEquals(game.getAllCharacters().size(), 2);
        Assert.assertEquals(game.getQuest().getQuest(), ZQuests.The_Black_Book);
        ZPlayerName.Baldric.getCharacter().addExp(5);
        game2.moveActorInDirectionDebug(ZPlayerName.Baldric.getCharacter(), ZDir.WEST);
        game2.addUser(new ZTestUser());
        cl.parseSVRCommand(transfer(svr.newUpdateGameCommand(game2)));
    }

    GameCommand transfer(GameCommand cmd) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bout);
        cmd.write(out);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bout.toByteArray()) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 1000));
            }
        });
        return GameCommand.parse(in);
    }

    @Test
    public void testDeserializeGameKeepInstances() throws Exception {

        ZGame game = new ZGame();
        game.loadQuest(ZQuests.The_Black_Book);
        ZUser user = new ZUserMP(null);
        game.addUser(user);
        game.addCharacter(ZPlayerName.Nelly);
        user.addCharacter(ZPlayerName.Nelly);
        Assert.assertTrue(game.runGame());

        ZGame copy = new ZGame();
        copy.loadQuest(ZQuests.The_Abomination);
        copy.deserialize(game.toString());

    }

}