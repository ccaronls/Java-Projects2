package cc.lib.net;

import java.util.HashMap;

public class ClientForm extends Form {
    
    String action;
    HashMap<String, FormElem> values = new HashMap<String, FormElem>();
    
    ClientForm(int id, String xml) throws Exception {
        super(id);
        fromXML(xml);
        parseValues(getRootElem());
    }

    private void parseValues(FormElem root) {
        for (FormElem elem : root.getChildren()) {
            switch (elem.getType()) {
                case VLISTCONTAINER:
                case HLISTCONTAINER:
                case LABEL:
                case BUTTONOPTION:
                    break;
                case SUBMITBUTTON:
                case CHOICEBUTTON:
                case TOGGLEBUTTON:
                case TEXTINPUT:
                    values.put(elem.id, elem);
                    break;
                default:
                    throw new RuntimeException("Unhandled case");
            }
            parseValues(elem);
        }
    }
    
    /**
     * 
     * @param id
     * @return
     */
    public final FormElem getElem(String id) {
        return this.values.get(id);
    }
    
    interface Visitor<T> {

        T onContainer(T parent, boolean vertical);

        T onSubmitButton(T parent, String text, String id);

        T onChoiceButton(T parent, String text, String id);

        T onButtonOption(T parent, String text, String id);

        T onToggleButton(T parent, String text, String id);

        T onTextInput(T parent, String text, int minChars, int maxChars, String id);

        T onLabel(T parent, String text);
    };
    
    public final <T> void visit(Visitor<T> builder) {
        visit_recursive(builder, getRootElem(), null);
    }
    
    private <T> void visit_recursive(Visitor<T> builder, FormElem root, T parent) {
        for (FormElem elem: root.getChildren()) {
            final T saveParent = parent;
            switch (elem.getType()) {
                case VLISTCONTAINER:
                    parent = builder.onContainer(parent, true);
                    break;
                case HLISTCONTAINER:
                    parent = builder.onContainer(parent, false);
                    break;
                case SUBMITBUTTON:
                    builder.onSubmitButton(parent, elem.getText(), elem.id);
                    break;
                case CHOICEBUTTON:
                    builder.onChoiceButton(parent, elem.getText(), elem.id);
                    break;
                case BUTTONOPTION:
                    builder.onButtonOption(parent, elem.getText(), elem.id);
                    break;
                case TOGGLEBUTTON:
                    builder.onToggleButton(parent, elem.getText(), elem.id);
                    break;
                case TEXTINPUT:
                    builder.onTextInput(parent, elem.getText(), elem.getMinChars(), elem.getMaxChars(), elem.id);
                    break;
                case LABEL:
                    builder.onLabel(parent, elem.getText());
                    break;         
                default:
                    throw new RuntimeException("Unhandled case");                    
            }
            visit_recursive(builder, elem, parent);
            parent = saveParent;
        }
    }
}
