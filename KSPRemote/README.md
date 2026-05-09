REM = Remote

This library allows for execute of methods across processes without reflection.

OPTIONS:

- imports
    - comma separated list of imports to be used at top of generated files. Example: imports="
      java.lang.*,java.net.*"

To use this library, mark classes remote capability like so:

@Remote
open class Foo : IRemote {

    @RemoteMethod
    fun remote1()

    ...

}

The following class is generated:

open class FooRemote : Foo() {
override fun remote1() {
executeRemotely("remote1", null)
}

    final override fun executeLocally(method : String vararg args : Any?) : Any? {
        return when (method) {
            "remote1" -> remote1()
            else -> null
        }
    }

}

Notice that 'executeRemotely' has to be defined by the implementor.

class FooImpl : FooRemote {
fun executeRemotely(method: String, resultType: Class<*>?, vararg args: Any?): Any? {
network?.sendRequest(method, resultType, *args)
}
}