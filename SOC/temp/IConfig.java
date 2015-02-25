package cc.game.soc.core;

public interface IConfig {

    public enum Type {
        INTEGER,
        FLOAT,
        STRING,
        BOOLEAN
    }
    
    public enum Key {
        NUM_RESOURCES_FOR_CITY(2),
        NUM_RESOURCES_FOR_SETTLEMENT(1),
        POINTS_CITY(2),
        POINTS_SETTLEMENT(1),
        POINTS_LONGEST_ROAD(2),
        POINTS_LARGEST_ARMY(2),
        MIN_PLAYER_CARDS_FOR_SURRENDER_ON_7(7),
        MIN_LONGEST_ROAD_LEN(5),
        MIN_LARGEST_ARMY_SIZE(3),
        WINNING_POINTS(10),
        ENABLE_ROAD_BLOCK(false),
        DECK_OCCURANCES_MONOPOLY(2),
        DECK_OCCURANCES_SOLDIER(50),
        DECK_OCCURANCES_YEAROFPLENTY(5),
        DECK_OCCURANCES_ROADBUILDING(20),
        DECK_OCCURANCES_VICTORY(4),
        ;
        final int defaultIntValue;
        final boolean defaultBoolValue;
        final String strValue;
        final Type type;
        Key(int defaultValue) {
            this.defaultIntValue = defaultValue;
            this.defaultBoolValue = defaultValue != 0;
            this.strValue = String.valueOf(defaultValue);
            this.type = Type.INTEGER;
        }
        Key(boolean defaultValue) {
            this.defaultIntValue = defaultValue ? 1 : 0;
            this.defaultBoolValue = defaultValue;
            this.strValue = String.valueOf(defaultValue);
            this.type = Type.BOOLEAN;
        }
        public Type getType() {
            return type;
        }
    }
    
    int getInt(Key key);
    boolean getBoolean(Key key);
    String getString(Key key);
    
    public static final IConfig DEFAULT_CONFIG = new IConfig() {
      public int getInt(Key key) {
          return key.defaultIntValue;
      }
      public boolean getBoolean(Key key) {
          return key.defaultBoolValue;
      }
      public String getString(Key key) {
          return key.strValue;
      }
    };
}
