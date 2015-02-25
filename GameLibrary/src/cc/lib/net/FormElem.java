package cc.lib.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.xml.XmlDoc;

/**
 * Hold info for all types of element forms
 * 
 * @author ccaron
 *
 */
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

    final Form form;
    Type type;
    int minChars, maxChars=64;
    String text;
    String id;
    boolean enabled;
    FormElem parent;
    private List<FormElem> children = new ArrayList<FormElem>();
    
    FormElem(Form form) {
        this.form = form;
    }
    
    void addChild(FormElem e) {
        children.add(e);
        e.parent = this;
    }
    
    public final List<FormElem> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    public final String getText() {
        return text;
    }

    public final void setText(String text) {
        this.text = text;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final Type getType() {
        return type;
    }

    public final int getMinChars() {
        return minChars;
    }

    public final int getMaxChars() {
        return maxChars;
    }
    
    public final String getId() {
        return this.id;
    }
    
    public final void doAction(GameClient client, Object input) {
        switch (type) {
            case VLISTCONTAINER:
            case HLISTCONTAINER:
                break;
            case SUBMITBUTTON:
                client.submitForm((ClientForm)form);
                break;
            case CHOICEBUTTON:
                this.setText(input.toString());
                break;
            case BUTTONOPTION:
                this.parent.setText(getText());
                break;
            case TOGGLEBUTTON:
                this.setEnabled(!isEnabled());
                break;
            case TEXTINPUT:
                this.setText(input.toString());
                break;
            case LABEL:
                break;
            
        }        
    }
    
    public final String getValue() {
        switch (type) {
        case TOGGLEBUTTON:
            return this.enabled ? "enabled" : "disabled";
        default:
            return this.text;
        }
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        toString(buf, "");
        return buf.toString();
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
    
    void toXML(XmlDoc doc) {
        switch (type) {
            case VLISTCONTAINER:
                doc.complexTag(Form.Tag.Container.name(), "type", "list", "orientation", "vertical");
                break;
            case HLISTCONTAINER:
                doc.complexTag(Form.Tag.Container.name(), "type", "list", "orientation", "horizontal");
                break;
            case SUBMITBUTTON:
                doc.complexTag(Form.Tag.Button.name(), "id", id, "type", "submit", "text", text);
                break;
            case CHOICEBUTTON:
                doc.complexTag(Form.Tag.Button.name(), "id", id, "type", "choice", "text", text);
                break;
            case BUTTONOPTION:
                doc.complexTag(Form.Tag.ButtonOption.name(), "text", text);
                break;
            case TOGGLEBUTTON:
                doc.complexTag(Form.Tag.Button.name(), 
                                        new String[] { "id", "type", "text", "enabled" },
                                        new String[] { id, "toggle", text, enabled ? "true" : "false"} );
                break;
            case TEXTINPUT:
                doc.complexTag(Form.Tag.TextInput.name(), 
                                           new String[] { "id", "minChars", "maxChars", "text" },
                                           new String[] { id, "" + minChars, "" + maxChars, text });
                break;
            case LABEL:
                doc.complexTag(Form.Tag.Label.name(), "text", text);
                break;
            
        }
        for (FormElem e : children) {
            e.toXML(doc);
        }
        doc.closeTag();
        
    }
    
    void toXMLOld(StringBuffer buffer) {
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
                e.toXMLOld(buffer);
            }
            buffer.append(closer);
        }
        
    }
    
}
