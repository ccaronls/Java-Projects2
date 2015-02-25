package cc.game.soc.netx;

import java.util.HashMap;
import java.util.Map;

class FormResponse {

    private int id;
    private Map<String, String> values = new HashMap<String,String>();
    private String action;
    
    FormResponse(int id, String action, Map<String,String> values) {
        this.id = id;
        this.action = action;
        this.values = values;
    }
    
    public String getAction() {
        return action;
    }
    
    public int getId() {
        return id;
    }
    
    public String get(String param) {
        return values.get(param);
    }
    
    public String toString() {
        return "FormResponse id(" + id + ") action=" + action + " params=" + values;
    }
}
