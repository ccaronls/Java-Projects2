package cc.applets.zombicide;

import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZZombie;

class UIZGame extends ZGame {

    @Override
    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle dest) {
        /*
        ZombicideApplet.instance.boardComp.moveActor(actor, start, dest);
        synchronized (ZombicideApplet.instance.boardComp) {
            try {
                ZombicideApplet.instance.boardComp.wait(5000);
            } catch (Exception e) {}
        }*/
    }

    @Override
    protected void onZombieSpawned(ZZombie zombie) {
        //ZombicideApplet.instance.boardComp.spawnZombie(zombie);
    }

    @Override
    protected void onZombieDestroyed(ZCharacter c, ZZombie zombie) {
        //ZombicideApplet.instance.boardComp.destroyActor(zombie);
    }
}
