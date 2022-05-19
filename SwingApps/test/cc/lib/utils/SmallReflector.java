package cc.lib.utils;

public class SmallReflector extends Reflector<SmallReflector> {

	static {
		addAllFields(SmallReflector.class);
	}
	
	String a = "hello";
	String b = "goodbye";
	SomeEnum e = SomeEnum.ENUM1;
	
}
