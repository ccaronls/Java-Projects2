package cc.lib.xml.descrip;

public class AttributeNotFoundException extends DescriptorException {

    AttributeNotFoundException(OptAttrib attrib) {
        super("Attribute not found [" + attrib + "]");
    }
    
}
