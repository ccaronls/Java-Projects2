package cc.lib.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class OverridesSerialize extends Reflector<OverridesSerialize> {

    String msg;
    int x;
    
    public OverridesSerialize() {}
    
    public OverridesSerialize(String msg, int x) {
        super();
        this.msg = msg;
        this.x = x;
    }

    @Override
    public void serialize(PrintWriter out) throws IOException {
        out.println(msg + " " + x);
    }

    @Override
    public void deserialize(BufferedReader in, boolean keepInstances) throws IOException {
        String text = in.readLine().trim();
        String [] parts = text.split("[ ]+");
        msg = parts[0];
        x = Integer.parseInt(parts[1]);
    }
    
    public boolean equals(Object o) {
    	if (o == null || !(o instanceof OverridesSerialize))
    		return false;
    	
    	OverridesSerialize os = (OverridesSerialize)o;
    	return msg.equals(os.msg) && x == os.x;
    }
}