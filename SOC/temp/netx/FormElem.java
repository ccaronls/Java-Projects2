package cc.game.soc.netx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormElem {

    public enum Type {
        
        /**
         * Vertical list container
         */
        VLISTCONTAINER,
        
        /**
         * Horizontal list container
         */
        HLISTCONTAINER,
        
        /**
         * Button that results in a submit.  @see ClientForm.doAction
         */
        SUBMITBUTTON,       
        
        /**
         * Container to display list of choices
         */
        CHOICEBUTTON,
        
        /**
         * Subelement of choice button
         */
        BUTTONOPTION,
        
        /**
         * Button with on/off behavior
         */
        TOGGLEBUTTON,
        
        /**
         * Element that accepts text input
         */
        TEXTINPUT,
        
        /**
         * Element that displays text
         */
        LABEL, 
    }

    Type type;
    int minChars, maxChars=64;
    String text;
    String id;
    boolean enabled;
    FormElem parent;
    private List<FormElem> children = new ArrayList<FormElem>();
    
    FormElem() {}
    
    void addChild(FormElem e) {
        children.add(e);
        e.parent = this;
    }
    
    public List<FormElem> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Type getType() {
        return type;
    }

    public int getMinChars() {
        return minChars;
    }

    public int getMaxChars() {
        return maxChars;
    }

    void toString(StringBuffer buffer, String indent) {
        buffer.append(indent).append(type);
        if (id != null)
            buffer.append(" id=").append(id);
        if (text != null)
            buffer.append(" text='").append(text).append("'");
        if (type == Type.TEXTINPUT)
            buffer.append(" minChars=").append(minChars).append(" maxChars=").append(maxChars);
        buffer.append("\n");
        for (FormElem child : children)
            child.toString(buffer, indent + "  ");              
    }
    
    void toXML(StringBuffer buffer) {
        String closer = null;
        switch (type) {
            case VLISTCONTAINER:
                buffer.append("<Container type=\"list\" orientation=\"vertical\">");
                closer = "</Container>";
                break;
            case HLISTCONTAINER:
                buffer.append("<Container type=\"list\" orientation=\"horizontal\">");
                closer = "</Container>";
                break;
            case SUBMITBUTTON:       
                buffer.append("<Button id=\"").append(id).append("\"").append(" type=\"submit\" text=\"").append(text).append("\"/>");
                break;
            case CHOICEBUTTON:
                buffer.append("<Button id=\"").append(id).append("\"").append(" type=\"choice\" text=\"").append(text).append("\">");
                closer = "</Button>";
                break;
            case BUTTONOPTION:
                buffer.append("<ButtonOption").append(" text=\"").append(text).append("\"/>");
                break;
            case TOGGLEBUTTON:
                buffer.append("<Button id=\"").append(id).append("\"").append(" type=\"toggle\" text=\"").append(text).append("\" enabled=\"").append(enabled ? "true" : "false").append("\"/>");
                break;
            case TEXTINPUT:
                buffer.append("<TextInput id=\"").append(id).append("\"").append(" minChars=\"").append(minChars).append("\" maxChars=\"").append(maxChars).append("\" text=\"").append(text).append("\"/>");
                break;
            case LABEL:
                buffer.append("<Label text=\"").append(text).append("\"/>");
                break;
            
        }
        if (closer != null) {
            for (FormElem e : children) {
                e.toXML(buffer);
            }
            buffer.append(closer);
        }
        
    }
    
}
