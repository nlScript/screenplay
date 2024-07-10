package nlScript.screenplay;

import javax.swing.JComponent;
import java.awt.Point;
import java.util.ArrayList;

public interface OverlayEditor {

	ArrayList<Point> getControls();

	void setControls(ArrayList<Point> points);

	void appendPoint(Point p);

	JComponent getPanel();

	String getClickThroughInstruction();

	String getEditInstruction();
}
