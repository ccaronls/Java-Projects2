package cc.lib.xml.descrip;

public class ChildNotFoundException extends DescriptorException {

    ChildNotFoundException(String name, OptElem parent) {
        super("Unknown child elem [" + name + "] for parent: " + parent);
    }
}
