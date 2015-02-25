package cc.lib.net;

import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

/**
 * Class to for servers to assemble form data for clients.
 * 
 * @author ccaron
 *
 */
public class ServerForm extends Form {

    public ServerForm(int id) {
        super(id);
    }

    Stack<FormElem> containers = new Stack<FormElem>();

    private void push(FormElem elem) {
        if (containers.size() == 0)
            addListContainer(false);
        containers.peek().addChild(elem);
    }
    
    public final void addListContainer(boolean horizontal) {
        FormElem elem = new FormElem(this);
        elem.type = horizontal ? FormElem.Type.HLISTCONTAINER : FormElem.Type.VLISTCONTAINER;
        if (rootElem == null) {
            rootElem = elem;
            containers.add(elem);
        } else {
            containers.peek().addChild(elem);
            containers.add(elem);
        }
    }
    
    public final void addSubmitButton(String id, String text) {
        FormElem elem = new FormElem(this);
        elem.type = FormElem.Type.SUBMITBUTTON;
        elem.text = text;
        elem.id = id;
        push(elem);
    }

    public final void addChoiceButton(String id, String text, String [] options) {
        addChoiceButton(id, text, Arrays.asList(options));
    }
    
    public final void addChoiceButton(String id, String text, Collection<String> options) {
        FormElem elem = new FormElem(this);
        elem.type = FormElem.Type.CHOICEBUTTON;
        elem.id = id;
        elem.text = text;
        for (String op : options) {
            FormElem child = new FormElem(this);
            child.type = FormElem.Type.BUTTONOPTION;
            child.text = op;
            elem.addChild(child);
        }
        push(elem);
    }

    public final void addToggleButton(String id, String text, boolean enabled) {
        FormElem elem = new FormElem(this);
        elem.id = id;
        elem.type = FormElem.Type.TOGGLEBUTTON;
        elem.text = text;
        elem.enabled = enabled;
        push(elem);
    }
    
    public final void endContainer() {
        if (containers.size() > 1)
            containers.pop();
    }
    
    public final void addLabel(String string) {
        FormElem elem = new FormElem(this);
        elem.type = FormElem.Type.LABEL;
        elem.text = string;
        push(elem);
    }

    public final void addTextInput(String id, String defaultText, int minChars, int maxChars) {
        FormElem elem = new FormElem(this);
        elem.id = id;
        elem.type = FormElem.Type.TEXTINPUT;
        elem.text = defaultText;
        elem.maxChars = maxChars;
        elem.minChars = minChars;
        push(elem);
    }
    
}
