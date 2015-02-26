package cc.app.umleditor.api;

import java.util.ArrayList;
import java.util.Collection;

public class UMLBubble {

	public enum Shape {
		UML_SHAPE_SQUARE,
		UML_SHAPE_ROUNDED_RECT,
		UML_SHAPE_OVAL,
		UML_SHAPE_DIAMOND
	};
	
	private Shape shape = Shape.UML_SHAPE_SQUARE;
	private Collection<UMLConnection> connections = new ArrayList<UMLConnection>();
	
}
