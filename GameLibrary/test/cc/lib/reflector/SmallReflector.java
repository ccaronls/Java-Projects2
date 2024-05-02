package cc.lib.reflector;

import cc.lib.math.Vector2D;
import cc.lib.utils.SomeEnum;

public class SmallReflector extends Reflector<SmallReflector> {

    static {
        addAllFields(SmallReflector.class);
    }

    String a = "hello";
    String b = "goodbye";
    String empty = "";
    SomeEnum e = SomeEnum.ENUM1;
    Vector2D vec = new Vector2D(10, 20);
}
