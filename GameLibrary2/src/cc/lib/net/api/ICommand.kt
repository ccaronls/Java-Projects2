interface ICommand {

	/**
	 * Code <= 0 are reserved
	 */
	val code: Byte

	/**
	 *
	 */
	val args: MutableMap<String, Any>
}