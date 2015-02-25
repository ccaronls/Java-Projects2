package cc.game.soc.netx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

public class ServerForm extends Form {

    ServerForm(int id) {
        super(id);
    }

    Stack<FormElem> containers = new Stack<FormElem>();

    private void push(FormElem elem) {
        if (containers.size() == 0)
            addListContainer(false);
        containers.peek().addChild(elem);
    }
    
    public void addListContainer(boolean horizontal) {
        FormElem elem = new FormElem();
        elem.type = horizontal ? FormElem.Type.HLISTCONTAINER : FormElem.Type.VLISTCONTAINER;
        if (rootElem == null) {
            rootElem = elem;
            containers.add(elem);
        } else {
            containers.peek().addChild(elem);
            containers.add(elem);
        }
    }
    
    public void addSubmitButton(String id, String text) {
        FormElem elem = new FormElem();
        elem.type = FormElem.Type.SUBMITBUTTON;
        elem.id = id;
        elem.text = text;
        push(elem);
    }

    public void addChoiceButton(String id, String text, String [] options) {
        addChoiceButton(id, text, Arrays.asList(options));
    }
    
    public void addChoiceButton(String id, String text, Collection<String> options) {
        FormElem elem = new FormElem();
        elem.type = FormElem.Type.CHOICEBUTTON;
        elem.id = id;
        elem.text = text;
        for (String op : options) {
            FormElem child = new FormElem();
            child.type = FormElem.Type.BUTTONOPTION;
            child.text = op;
            elem.addChild(child);
        }
        push(elem);
    }

    public void addToggleButton(String id, String text, boolean enabled) {
        FormElem elem = new FormElem();
        elem.id = id;
        elem.type = FormElem.Type.TOGGLEBUTTON;
        elem.text = text;
        elem.enabled = enabled;
        push(elem);
    }
    
    public void endContainer() {
        if (containers.size() > 1)
            containers.pop();
    }
    
    public void addLabel(String string) {
        FormElem elem = new FormElem();
        elem.type = FormElem.Type.LABEL;
        elem.text = string;
        push(elem);
    }

    
}
