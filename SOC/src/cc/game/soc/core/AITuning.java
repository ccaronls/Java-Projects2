package cc.game.soc.core;

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

	public abstract double getScalingFactor(String property);
	
}
