package cc.lib.xml.descrip;

import cc.lib.xml.XmlElement;

public class UnknownElementException extends DescriptorException {

	UnknownElementException(XmlElement elem) {
		super("Unknown element: " + elem);
	}
}
