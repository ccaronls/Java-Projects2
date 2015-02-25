package cc.lib.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import cc.lib.xml.XmlDoc;

/**
 * Base class for client and server forms.  Forms provide a way
 * for servers to provide input forms to the client, similar to
 * web forms.  Forms can be defined in XML and are transmitted
 * in XML between server and client.
 * 
 * @author ccaron
 *
 */
public abstract class Form {

    Form(int id) {
        this.id = id;
    }

	public String toString() {
	    StringBuffer buffer = new StringBuffer();
	    buffer.append("Form (").append(id).append(")\n");
	    if (rootElem != null)
	        rootElem.toString(buffer, "  ");
	    return buffer.toString();
	}

	FormElem rootElem;
	int id;

	public final FormElem getRootElem() {
	    return rootElem;
	}
	
	public final int getId() {
        return id;
    }
	
    public final String toXML() {
        XmlDoc doc = new XmlDoc();
        doc.complexTag(Tag.Form.name());
        if (rootElem != null)
            rootElem.toXML(doc);
        doc.closeAll();
        return doc.getXML();
    }
    
    enum Tag {
        Form,
        Container,
        Button,
        ButtonOption,
        Label,
        TextInput,
    }

    private class XMLFormHandler extends DefaultHandler {
        
        Stack<Tag> stack = new Stack<Tag>();
        Stack<FormElem> containers = new Stack<FormElem>();
        HashSet<String> usedIds = new HashSet<String>();
        
        @Override
        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            stack.pop();
            if (qName.equals(Tag.Container.name()))
                containers.pop();
            else if (qName.equals(Tag.Button.name()) && containers.peek().type == FormElem.Type.CHOICEBUTTON)
                containers.pop();
        }

        @Override
        public void setDocumentLocator(Locator arg0) {
            super.setDocumentLocator(arg0);
            this.locator = arg0;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
            try {
                Tag tag = Tag.valueOf(qName);
                switch (tag) {
                    case Form:
                        if (stack.size() > 0)
                            throw new SAXException("SOCForm can only be root tag");
                        break;
                    case Container:
                        containers.push(newContainer(attribs));
                        break;
                    case Button:
                        if (stack.size() == 0 || stack.peek() != Tag.Container)
                            throw new SAXException("Button can only be a child of Container");
                        containers.peek().addChild(newButton(attribs));
                        break;
                    case ButtonOption:
                        if (stack.size() == 0 || stack.peek() != Tag.Button || containers.peek().type != FormElem.Type.CHOICEBUTTON) 
                            throw new SAXException("ButtonOption can only be child of a choice Button");
                        containers.peek().addChild(newButtonOption(attribs));
                        break;
                    case Label:
                        if (stack.size() == 0 || stack.peek() != Tag.Container)
                            throw new SAXException("Label can only be a child of Container");
                        containers.peek().addChild(newLabel(attribs));
                        break;
                    case TextInput:
                        if (stack.size() == 0 || stack.peek() != Tag.Container)
                            throw new SAXException("TextInput can only be a child of Container");
                        containers.peek().addChild(newTextInput(attribs));
                        break;
                }
                stack.push(tag);
            } catch (NumberFormatException e) {
                throwError("Not a number '" + e.getMessage() + "'");
            } catch (SAXException e) {
                throwError(e.getMessage());
            } catch (IllegalArgumentException e) {
                throwError("Unknown tag " + e.getMessage());
            } catch (Exception e) {
                throwError(e.getClass().getSimpleName() + " " + e.getMessage());
            }

        }

        FormElem newContainer(Attributes attribs) throws SAXException {
            FormElem elem = new FormElem(Form.this);
            String type = attribs.getValue("type");
            if (type == null || type.equals("list")) {
                String orient = attribs.getValue("orientation");
                if (orient == null || orient.startsWith("vert")) {
                    elem.type = FormElem.Type.VLISTCONTAINER;
                } else if (orient.startsWith("horiz")) {
                    elem.type = FormElem.Type.HLISTCONTAINER;
                } else {
                    throw new SAXException("Unknown orientation for List Container '" + orient + "' must be [horiz]ontal or [vert]ical");
                }
            } else {
                throw new SAXException("Unknown list type '" + type + "'");
            }
            if (containers.size() > 0)
                containers.peek().addChild(elem);
            if (rootElem == null)
                rootElem = elem;
            return elem;
        }

        private void setId(FormElem elem, Attributes attribs) throws SAXException {
            String s = attribs.getValue("id");
            if (s == null)
                throw new SAXException("Missing required attribute 'id'");
            if (usedIds.contains(s))
                throw new SAXException("duplicate id '" + s + "'");
            usedIds.add(s);
            elem.id = s;            
        }
        
        private FormElem newTextInput(Attributes attribs) throws Exception {
            FormElem elem = new FormElem(Form.this);
            elem.type = FormElem.Type.TEXTINPUT;
            setId(elem, attribs);
            String s = attribs.getValue("minChars");
            if (s != null)
                elem.minChars = Integer.parseInt(s);
            s = attribs.getValue("maxChars");
            if (s != null)
                elem.maxChars = Integer.parseInt(s);
            s = attribs.getValue("text");
            if (s != null)
                elem.text = s;
            return elem;
        }

        private FormElem newLabel(Attributes attribs) throws SAXException {
            FormElem elem = new FormElem(Form.this);
            elem.type = FormElem.Type.LABEL;
            elem.text = attribs.getValue("text");
            if (elem.text == null)
                throw new SAXException("Missing required attribute 'text' for Label");
            return elem;
        }

        private FormElem newButtonOption(Attributes attribs) throws SAXException {
            FormElem elem = new FormElem(Form.this);
            elem.type = FormElem.Type.BUTTONOPTION;
            String s = attribs.getValue("text");
            if (s == null)
                throw new SAXException("Missing required attribute 'text'");
            elem.text = s;
            return elem;
        }

        private FormElem newButton(Attributes attribs) throws SAXException {
            FormElem elem = new FormElem(Form.this);
            setId(elem, attribs);
            String s = attribs.getValue("type");
            if (s == null || s.equals("submit")) {
                elem.type = FormElem.Type.SUBMITBUTTON;
            } else if (s.equals("toggle")) {
                elem.type = FormElem.Type.TOGGLEBUTTON;
            } else if (s.equals("choice")) {
                elem.type = FormElem.Type.CHOICEBUTTON;
                containers.push(elem);
            } else {
                throw new SAXException("Unknown button type '" + s + "' must be 'submit', 'toggle', or 'choice'");
            }
            s = attribs.getValue("text");
            if (s == null)
                throw new SAXException("missing required attribute 'text'");
            elem.text = s;
            return elem;
        }

        void throwError(String msg) throws SAXException {
            throw new SAXException("Line " + locator.getLineNumber() + " column " + locator.getColumnNumber() + " " + msg);
        }
        
        Locator locator;
    }
    
    void fromXML(File file) throws Exception {
        fromXML(new FileInputStream(file));
    }
    
    void fromXML(String xml) throws Exception {
        fromXML(new StringReader(xml));
    }
    
    void fromXML(InputStream input) throws Exception {
        try {
            fromXML(new InputStreamReader(input));
        } finally {
            input.close();
        }
    }
    
    void fromXML(Reader reader) throws Exception {
        try {
            InputSource source = new InputSource(reader);
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XMLFormHandler handler = new XMLFormHandler();
            xmlReader.setContentHandler(handler);
            xmlReader.parse(source);
        } finally {
            reader.close();
        }
    }
}
