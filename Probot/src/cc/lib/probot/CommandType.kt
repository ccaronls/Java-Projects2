package cc.lib.probot

enum class CommandType {
	Advance,
	TurnRight,
	TurnLeft,
	UTurn,
	LoopStart,
	LoopEnd,
	Jump,
	IfThen,
	IfElse,
	IfEnd;

	fun build() : Command = Command(this, 0)
}