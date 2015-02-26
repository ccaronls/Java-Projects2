package cc.game.roids.core;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;

public class Asteroid extends PolygonThingy {

    float angSpeed = 0;
    
    Asteroid() {
        angSpeed = Utils.randRange(-10, 10);
    }

    @Override
    void update(Roids roids, float curTimeSeconds, float deltaTimeSeconds) {
        this.setOrientation(getOrientation() + angSpeed);
    }

}
