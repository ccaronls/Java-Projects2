package cc.lib.xml.descrip;

public class DescriptorException extends Exception {

    DescriptorException(String msg) {
        super(msg);
    }
    
    DescriptorException(String msg, Exception cause) {
        super(msg, cause);
    }
    
}
