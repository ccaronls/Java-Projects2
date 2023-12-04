package cc.lib.reflector;

import cc.lib.utils.SomeEnum;

public class SmallReflector extends Reflector<SmallReflector> {

    static {
        addAllFields(SmallReflector.class);
    }

    String a = "hello";
    String b = "goodbye";
    SomeEnum e = SomeEnum.ENUM1;
	
}
