package cc.game.probot

class Command(val type: CommandType, var count: Int) {
	var nesting = 0
}