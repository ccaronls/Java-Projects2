package cc.lib.checkerboard

enum class MoveType(val value: Int, val isJump: Boolean) {
	END(1, false),
	SLIDE(0, false),
	FLYING_JUMP(5, true),
	JUMP(2, true),
	STACK(10, false),
	SWAP(10, false),
	CASTLE(6, false);
}