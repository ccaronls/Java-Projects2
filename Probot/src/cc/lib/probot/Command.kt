package cc.lib.probot

class Command(val type: CommandType, var count: Int) {
	var nesting = 0
}