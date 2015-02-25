package cc.game.superrobotron;

public interface ILocalization {

    public enum StringID {
        GAME_OVER
    }
    
    String getString(StringID id);
    
}
