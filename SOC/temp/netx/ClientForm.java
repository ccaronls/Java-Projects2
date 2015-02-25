package cc.game.soc.netx;

public class ClientForm extends Form {
    
    NetClient client;
    String action;
    
    ClientForm(int id, NetClient client) {
        super(id);
        this.client = client;
    }

    /**
     * Should be called for each form element that the user has had interaction.
     * @param elem
     */
    public void doAction(FormElem elem) {
        switch (elem.type) {
            case VLISTCONTAINER:
            case HLISTCONTAINER:
                break;
            case SUBMITBUTTON:
                action = elem.id;
                submit();
                break;
            case CHOICEBUTTON:
                break;
            case BUTTONOPTION:
                elem.parent.text = elem.text;
                break;
            case TOGGLEBUTTON:
                elem.enabled = !elem.enabled;
                break;
            case TEXTINPUT:
                break;
            case LABEL:
                break;
            
        }
    }
    
    void submit() {
        try {
            client.send(Command.newClFormSubmit(this));
            client.onFormSubbmited();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
