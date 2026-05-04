package cc.lib.ksp.reflex

/**
 * Mark a class for Reflection with optional name for the derived class
 *
 * Example:
 *
 * @Reflect
 * abstract class AFoo : IReflex {
 *      protected var bar = 0
 *
 *      // these are ignored
 *      transient var sam = "x"
 *      @Omit var altman = "y"
 * }
 *
 * will generate:
 *
 * class Foo : AFoo() {
 *
 *      fun getClassId() = "Foo"
 *
 *      fun writer(writer : JsonWriter) {
 *          writer.name("bar")
 *          writer.value(bar)
 *      }
 *
 *      fun read(reader : JsonReader) {
 *      	while (reader.peek() == JsonToken.NAME) {
 * 			    when (reader.nextName()) {
 *                  "bar" -> bar = nextInt()
 *                  else -> super.reader(reader)
 * 			    }
 * 			}
 *      }
 *
 *      companion object {
 *          init {
 *              cc.lib.ref.REF.register("Foo") { Foo() }
 *          }
 *      }
 * }
 *
 *
 */


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Reflex(val className: String = "")
