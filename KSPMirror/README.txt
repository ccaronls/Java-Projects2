# How to use Mirror

build script options:

ksp {
    arg("package", "a.b.c") // package where your files are written under generated
}

Mirror library provided for a mechanism to serialize data across processes with the advantage
that only data that has been changed will be serialized.

Both data and function calls are mirrored.

The mirroring is one-way, so the client / server model is encouraged.

The mirror data delivery is managed byy a MirrorContext implementation.

Example Usage:

@Mirror
interface IFooBar : Mirror {
    val foo : String
    val bar : Int
}

The above is KSP compiled into a type that can be derived from

// Objects must support 0 argument constructor
class FooBar(foo : String = "", bar : Int = 0) : FooBarImpl

// Process 1 (server)
fun main(vararg args : String) {

  FooBar x = FooBar("hello", 10)

  FooBar y = FooBar("goodbye", 100)

  val context = MirroredContext()



}

// Process 2 (client)
fun main(vararg args : String) {

}