package cc.game.soc.core

abstract class AITuning {
	abstract fun getScalingFactor(property: String): Double

	companion object {
		private var instance: AITuning? = null
		fun getInstance(): AITuning {
			return instance?:object : AITuning() {
					init {
						instance = this
					}
					override fun getScalingFactor(p: String): Double {
						return 1.0
					}
				}
			}


		fun setInstance(instance: AITuning) {
			Companion.instance = instance
		}

		fun setMap(map: Map<String, Double>) {
			instance = object : AITuning() {
				override fun getScalingFactor(property: String): Double {
					return map[property]?:1.0
				}
			}
		}
	}
}