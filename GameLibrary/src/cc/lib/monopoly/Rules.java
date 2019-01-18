package cc.lib.monopoly;

import cc.lib.utils.Reflector;

public class Rules extends Reflector<Rules> {
    public int startMoney = 1000; // initial starting momey
    public int valueToWin = 5000; // If no other players go bankrupt then first player to this value is the winner
    public boolean jailBumpEnabled = false; // if true then when a player goes to jail they bump an existing jailed player to freedomn (Sebi Rule)
    public float taxScale = 1f; // iuse this to scale the 'meanness' of the tax squares.
}
