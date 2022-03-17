package cc.lib.zombicide.quests;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZColor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZTile;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class WolfTheGhostDoor extends ZQuest {

    public WolfTheGhostDoor() {
        super(ZQuests.The_Ghost_Door);
    }

    @Override
    public ZBoard loadBoard() {
        String [][] map = {
                { "z0","z1:spn","z2:i:ww:ode",              "z10:i:vd1:we","z11","z12:i:ww:ode",            "z20:i:we:ods","z21","z22"                  },
                { "z3:i:we:ods","z4","z2:i:ww:ws:we",       "z10:i:de:ws", "z13","z12:i:dw:ws:we:red",      "z23:i:red:ws:we","z24","z25:i:dn:ww:ods"   },
                { "z5:i:red:we:ods","z6","z7",              "z14","z15","z16",                              "z26","z27","z28:i:ww:red:ods"              },

                { "z30:i:ws:ode","z31:i:wn:we:ds","z32",    "z40:t3:rn:rw","z40:t3:rn:re","z41:t1:re",      "z50:i:dn:we:ods","z51","z52:i:ww:ws"       },
                { "z33","z34","z35",                        "z49:t3:rw:exit","z40:t3","z42:t2:re:rs",       "z53:i:we","z54","z55:spe"                  },
                { "z36","z37:i:wn:ww:ode:ws","z38:i:wn:ods","z40:t3:rw:rs","z40:t3:rs","z40:t3:rs:re",      "z53:i:we:ods","z56","z57:i:wn:ww:ods:red"  },

                { "z60","z61:i:ww:ws:ode","z62:i:ds:de",    "z70:i:ws","z70:i:ws:ode","z71:i:ode:ws",       "z80:i:ws:we","z81","z82:ww:ds"             },
                { "z63","z64","z65",                        "z72:st","z73","z74",                           "z83","z84","z85"                           },
                { "z66:i:wn:ode:red","z67:i:dn:we","z68:sps","z75:i:dw:vd1:wn:we","z76:sps","z77:i:ww:dn:ode","z86:i:wn:we:red","z87","z88:i:dn:ww:red" }
        };


        return load(map);
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[]{
                new ZTile("1R", 0, ZTile.getQuadrant(0, 0, board)),
                new ZTile("9V", 270, ZTile.getQuadrant(0, 3, board)),
                new ZTile("3V", 90, ZTile.getQuadrant(0, 6, board)),

                new ZTile("2R", 90, ZTile.getQuadrant(3, 0, board)),
                new ZTile("10R", 180, ZTile.getQuadrant(3, 3, board)),
                new ZTile("5R", 270, ZTile.getQuadrant(3, 6, board)),

                new ZTile("6V", 0, ZTile.getQuadrant(6, 0, board)),
                new ZTile("8R", 180, ZTile.getQuadrant(6, 3, board)),
                new ZTile("7V", 270, ZTile.getQuadrant(6, 6, board))

        };
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        c.addAvailableSkill(ZSkill.Inventory);
        game.chooseEquipmentFromSearchables();
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numSurvivorsAtDangerRED = Utils.count(game.getAllLivingCharacters(), pl -> pl.getCharacter().getSkillLevel().getDifficultyColor() == ZColor.RED);
        int numREDinEXIT = Utils.count(game.getAllLivingCharacters(), pl -> pl.getCharacter().getSkillLevel().getDifficultyColor() == ZColor.RED && getExitZone() == pl.getCharacter().getOccupiedZone());

        int needed = 2;
        int total = numREDinEXIT > 0 ? 1 : 0 + numSurvivorsAtDangerRED > 0 ? 1 : 0;

        return total * 100 / needed;
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        int numSurvivorsAtDangerRED = Utils.count(game.getAllLivingCharacters(), pl -> pl.getCharacter().getSkillLevel().getDifficultyColor() == ZColor.RED);
        int numREDinEXIT = Utils.count(game.getAllLivingCharacters(), pl -> pl.getCharacter().getSkillLevel().getDifficultyColor() == ZColor.RED && getExitZone() == pl.getCharacter().getOccupiedZone());
        return new Table(getName()).addRow(new Table().setNoBorder()
            .addRow("1.", "Get at least one survivor to RED danger level.", numSurvivorsAtDangerRED > 0)
            .addRow("2.", "Get at least one survor at danger level RED to the EXIT. The EXIT but be clear on zombies.", numREDinEXIT > 0)
            .addRow("3.", "Each OBJECTIVE grants " + getObjectiveExperience(0,0) + " points and allow survivor to take an equipment card of their choice form the deck as well as reorganize their inventory for free", String.format("%d of %d", getNumUnfoundObjectives(), getNumStartObjectives()))
        );
    }
}
