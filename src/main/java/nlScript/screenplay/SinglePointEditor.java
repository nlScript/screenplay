package nlScript.screenplay;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class SinglePointEditor extends JPanel implements OverlayEditor, MouseListener {

	private static final String clickThroughInstruction =
			"Move cursor to target position and click <b style=\"color: orange;\">F2</b><br>" +
			"Press <b style=\"color: orange;\">F3</b> to finish</html>";

	private static final String editInstruction =
			"Edit the point position by"+
			"<ul>" +
			"<li><p>Left-click at the target position</p>" +
			"<li>Deleting the point by shift-clicking on it" +
			"</ul>" +
			"Then click OK";

	private Point point = null;

	public SinglePointEditor() {
		super();
		// setBackground(Color.WHITE);
		addMouseListener(this);
	}

	@Override
	public ArrayList<Point> getControls() {
		ArrayList<Point> ret = new ArrayList<>();
		if(point != null)
			ret.add(point);
		return ret;
	}

	@Override
	public void setControls(ArrayList<Point> points) {
		if(points.isEmpty())
			point = null;
		else point = points.get(0);
	}

	@Override
	public void appendPoint(Point p) {
		point = p;
		repaint();
	}

	@Override
	public JComponent getPanel() {
		return this;
	}

	@Override
	public String getClickThroughInstruction() {
		return clickThroughInstruction;
	}

	@Override
	public String getEditInstruction() {
		return editInstruction;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point p) {
		this.point = p;
		repaint();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if(point != null)
			g.fillOval(point.x - 2, point.y - 2, 5, 5);
	}

	private boolean isPointWithin5Pixels(Point p) {
		return (point != null && p.distance(point) < 5);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.isShiftDown()) {
			if(isPointWithin5Pixels(e.getPoint()))
				point = null;
		}
		else {
			if(point == null)
				point = e.getPoint();
			else
				point.setLocation(e.getPoint());
		}
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		SinglePointEditor ce = new SinglePointEditor();
		frame.getContentPane().add(ce, BorderLayout.CENTER);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
}
