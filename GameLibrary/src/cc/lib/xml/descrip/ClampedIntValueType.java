package cc.lib.xml.descrip;

public class ClampedIntValueType extends AValueType {

    private int min, max;
    
    public ClampedIntValueType(int min, int max) {
        this.min = min;
        this.max = max;
    }
    
    public void validate(String value) throws Exception {
        int val = Integer.parseInt(value);
        if (val < min || val > max)
            throw new Exception("invalid value [" + value + "] must be in range [" + min + ", " + max + "] inclusive");
    }
    
}
