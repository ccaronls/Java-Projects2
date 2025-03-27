BinarySerializer

Designed for use with UDP connections

Generates fastest serialization routines for objects.

Example:

@BinarySerializable("Obj")
abstract class AObj {
var x = 0
var y = 10f
var s = "hello"
}

will generate class:

Obj : AObj(), IBinarySerializable {
...
}

fields must be var type and non-nullable