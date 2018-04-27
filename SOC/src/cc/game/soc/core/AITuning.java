package cc.game.soc.core;

import java.util.Map;

public abstract class AITuning {

	private static AITuning instance = null;
	
	public static AITuning getInstance() {
		if (instance == null) {
			instance = new AITuning() {
				public double getScalingFactor(String p) {
					return 1.0;
				}
			};
		}
		return instance;
	}
	
	public static void setInstance(AITuning instance) {
		AITuning.instance = instance;
	}

	public static void setMap(final Map<String, Double> map) {
	    instance = new AITuning() {
            @Override
            public double getScalingFactor(String property) {
                return map.get(property);
            }
        };
    }

	public abstract double getScalingFactor(String property);
	
}
