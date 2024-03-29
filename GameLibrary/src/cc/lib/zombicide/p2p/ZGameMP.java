package cc.lib.zombicide.p2p;

import java.util.List;

import cc.lib.game.GRectangle;
import cc.lib.net.GameClient;
import cc.lib.net.GameServer;
import cc.lib.zombicide.ZActionType;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorPosition;
import cc.lib.zombicide.ZAttackType;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZombieCategory;

/**
 * Created by Chris Caron on 7/17/21.
 */
public class ZGameMP extends ZGame {

    GameServer server;
    GameClient client;

    private ZPlayerName currentCharacter = null;

    public static final String GAME_ID = "ZGame";

    public void setServer(GameServer server) {
        this.server = server;
    }

    public void setClient(GameClient client) {
        this.client = client;
    }

    @Override
    public void addLogMessage(String msg) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, msg);
        }
    }

    @Override
    protected void onCurrentUserUpdated(ZUser user) {
        setCurrentUser(user.getName());
    }

    protected void setCurrentUser(String name) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, name);
        } else if (client != null) {
            currentCharacter = null;
        }
    }

    @Override
    protected void onCurrentCharacterUpdated(ZPlayerName priorPlayer, ZPlayerName player) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, priorPlayer, player);
        } else if (client != null) {
            currentCharacter = player;
        }
    }

    @Override
    public ZPlayerName getCurrentCharacter() {
        if (client != null) {
            return currentCharacter;
        }
        return super.getCurrentCharacter();
    }

    @Override
    protected void onZombieSpawned(ZZombie zombie) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, zombie);
        } else if (client != null) {
            board.addActor(zombie);
        }
    }

    @Override
    protected void onQuestComplete() {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID);
        }
    }

    @Override
    protected void onCharacterDestroysSpawn(ZPlayerName c, int zoneIdx) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, zoneIdx);
        }
    }

    @Override
    protected void onCharacterDefends(ZPlayerName cur, ZActorPosition position) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, cur, position);
        }
    }

    @Override
    protected void onNewSkillAquired(ZPlayerName c, ZSkill skill) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, skill);
        }
    }

    @Override
    protected void onGameLost() {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID);
        }
    }

    @Override
    protected void onCharacterAttacked(ZPlayerName character, ZActorPosition attackerPosition, ZAttackType attackType, boolean characterPerished) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, character, attackerPosition, attackType, characterPerished);
        }
    }

    @Override
    protected void onEquipmentThrown(ZPlayerName c, ZIcon icon, int zone) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, icon, zone);
        }
    }

    @Override
    protected void onAhhhhhh(ZPlayerName c) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c);
        }
    }

    @Override
    protected void onNecromancerEscaped(ZZombie necro) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, necro);
        }
    }

    @Override
    protected void onEquipmentFound(ZPlayerName c, List<ZEquipment> equipment) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, equipment);
        }
    }

    @Override
    protected void onWeaponGoesClick(ZPlayerName c, ZWeapon weapon) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, weapon);
        }
    }

    @Override
    protected void onCharacterOpenedDoor(ZPlayerName cur, ZDoor door) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, cur, door);
        }
    }

    @Override
    protected void onCharacterOpenDoorFailed(ZPlayerName cur, ZDoor door) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, cur, door);
        }
    }

    @Override
    protected void onAttack(ZPlayerName attacker, ZWeapon weapon, ZActionType actionType, int numDice, List<ZActorPosition> hits, int targetZone) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, attacker, weapon, actionType, numDice, hits, targetZone);
        }
    }

    @Override
    protected void onCharacterGainedExperience(ZPlayerName c, int points) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, points);
        }
    }

    @Override
    protected void onRollDice(Integer[] roll) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, (Object)roll);
        }
    }

    @Override
    protected void onZombieDestroyed(ZPlayerName c, ZAttackType deathType, ZActorPosition pos) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, deathType, pos);
        }
    }

    @Override
    protected void onDoubleSpawn(int multiplier) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, multiplier);
        }
    }
/*
    @Override
    protected void moveActor(ZActor actor, int toZone, long speed, ZActionType actionType) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, actor, toZone, speed, actionType);
        }
        super.moveActor(actor, toZone, speed, actionType);
    }
*/
    @Override
    protected void moveActorInDirection(ZActor actor, ZDir dir) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, actor, dir);
        }
        super.moveActorInDirection(actor, dir);
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, zoneIndex);
        }
    }

    @Override
    protected void onZombiePath(ZZombie zombie, List<ZDir> path) {
        //if (server != null) {
        //    server.broadcastExecuteOnRemote(GAME_ID, zombie, path);
       // }
    }

    @Override
    public void onIronRain(ZPlayerName c, int targetZone) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, targetZone);
        }
    }

    @Override
    protected void onDoorUnlocked(ZDoor door) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, door);
        }
    }

    @Override
    protected void onDragonBileExploded(int zoneIdx) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, zoneIdx);
        }
    }

    @Override
    protected void onBonusAction(ZPlayerName pl, ZSkill action) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, pl, action);
        }
    }

    @Override
    protected void onExtraActivation(ZZombieCategory category) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, category);
        }
    }

    @Override
    protected void onBeginRound(int roundNum) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, roundNum);
        }
    }

    @Override
    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle end, long speed) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, actor, start, end, speed);
        }
    }

    @Override
    protected void onCharacterHealed(ZPlayerName c, int amt) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, amt);
        }
    }

    @Override
    protected void onSkillKill(ZPlayerName c, ZSkill skill, ZZombie z, ZAttackType attackType) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, skill, z, attackType);
        }
    }

    @Override
    protected void onRollSixApplied(ZPlayerName c, ZSkill skill) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, skill);
        }
    }

    @Override
    protected void onWeaponReloaded(ZPlayerName c, ZWeapon w) {
        if (server != null) {
            server.broadcastExecuteOnRemote(GAME_ID, c, w);
        }
    }
}
