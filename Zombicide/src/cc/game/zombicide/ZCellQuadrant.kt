package cc.game.zombicide

import cc.lib.annotation.Keep


@Keep
enum class ZCellQuadrant(val dx: Float, val dy: Float) {

	// the ordering is how actors are added to a cell
	UPPER_LEFT(0f, 0f),
	LOWER_RIGHT(.66f, .66f),
	UPPER_RIGHT(.66f, 0f),
	LOWER_LEFT(0f, .66f),
	TOP(.33f, 0f),
	BOTTOM(.33f, 66f),
	LEFT(0f, .33f),
	RIGHT(.66f, .33f),
	CENTER(.33f, .33f),

	UPPER_LEFT2(.1f, .1f),
	LOWER_RIGHT2(.56f, .56f),
	UPPER_RIGHT2(.56f, .1f),
	LOWER_LEFT2(.1f, .56f),
	TOP2(.33f, .43f),
	BOTTOM2(.33f, .56f),
	LEFT2(.1f, .33f),
	RIGHT2(.56f, .33f),
	CENTER2(.33f, .43f)

	; // make center the very last so that an ABOMINATION has good chance of not squashing something

	companion object {
		@JvmStatic
		fun valuesForRender(): Array<ZCellQuadrant> {
			return arrayOf(UPPER_LEFT, TOP, UPPER_RIGHT,
				UPPER_LEFT2, TOP2, UPPER_RIGHT2,
				LEFT, CENTER, RIGHT,
				LEFT2, CENTER2, RIGHT2,
				LOWER_LEFT, BOTTOM, LOWER_RIGHT,
				LOWER_LEFT2, BOTTOM2, LOWER_RIGHT2
			)
		}

		@JvmStatic
		fun valuesForInsert(): Array<ZCellQuadrant> = values()
	}
}