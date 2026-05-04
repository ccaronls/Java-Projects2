package cc.lib.ksp.reflex

/**
 * A property can be marked @Dirty when the object extends IDirtyReflector
 *
 * A Dirty object has its values tracker for changes
 *
 * Dirty properties must be marked open
 *
 * Example:
 *
 * @Reflect
 * abstract class AFoo : IDirtyReflector {
 *
 *      @Dirty
 *      protected open var bar = 0
 * }
 *
 * In addition to the generation from @Reflect, a IDirtyReflector will generate:
 *
 * class Foo : AFoo() {
 *      private var dirty = true
 *
 *      var bar by dirty(0)
 *
 *      override fun isDirty() : Boolean {
 *          if (dirty)
 *              return true
 *
 *           // list thru sub objects looking for IDirtyReflectors
 *
 *          return false
 *      }
 *
 *
 *
 *
 *      fun writeDirty(writer : JsonWriter) {
 *      }
 *
 *      // IReflex stuff
 *      fun getClassId() = "Foo"
 *      fun write(writer : JsonWriter) ...
 *      fun read(reader : JsonReader) ...
 *
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


@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Dirty
