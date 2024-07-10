package nlScript.screenplay;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class CurveEditor extends JPanel implements OverlayEditor, MouseListener, MouseMotionListener {

	private final ArrayList<Point> points = new ArrayList<>();

	private final JPopupMenu popupMenu;

	private static final String clickThroughInstruction =
			"Drag/Move interactively<br>" +
			"Press <F2> regularly if dragging along a bent path<br>" +
			"Press <F3> to finish";

	private static final String editInstruction =
			"Edit the drag/move path by"+
			"<ul>" +
			"<li><p>Adding new points at the end of the path via left-click</p>" +
			"<li>Adding new points in the middle of the path by clicking on a path segment" +
			"<li>Dragging control points into their final position" +
			"<li>Deleting a control point by shift-clicking on it" +
			"</ul>" +
			"Then click OK";

	public CurveEditor() {
		super();
		// setBackground(Color.WHITE);
		addMouseListener(this);
		addMouseMotionListener(this);
		popupMenu = new JPopupMenu();
		JMenuItem item = new JMenuItem("Clear");
		item.addActionListener((e) -> {
			points.clear();
			repaint();
		});
		popupMenu.add(item);
	}

	@Override
	public ArrayList<Point> getControls() {
		return points;
	}

	@Override
	public void setControls(ArrayList<Point> controls) {
		points.clear();
		points.addAll(controls);
		repaint();
	}

	@Override
	public void appendPoint(Point p) {
		points.add(p);
		repaint();
	}

	@Override
	public String getClickThroughInstruction() {
		return clickThroughInstruction;
	}

	@Override
	public String getEditInstruction() {
		return editInstruction;
	}

	@Override
	public JComponent getPanel() {
		return this;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		GeneralPath path = new Spline2D(points).getPathOld();
		g2.draw(path);

		for(Point p : points) {
			g.fillOval(p.x - 2, p.y - 2, 5, 5);
		}
	}

	public int getClosestSegment(Point p) {
		int minI = -1;
		double minD = Double.POSITIVE_INFINITY;
		for(int i = 0; i < points.size() - 1; i++) {
			double d = Line2D.ptLineDist(
					points.get(i).x,
					points.get(i).y,
					points.get(i + 1).x,
					points.get(i + 1).y,
					p.x,
					p.y);
			if(d < minD) {
				minD = d;
				minI = i;
			}
		}
		return minI;
	}

	private int getSegmentWithin5Pixels(Point p) {
		int closest = getClosestSegment(p);
		if(closest == -1)
			return -1;
		double d = Line2D.ptSegDist(
				points.get(closest).x,
				points.get(closest).y,
				points.get(closest + 1).x,
				points.get(closest + 1).y,
				p.x,
				p.y);
		return d < 5 ? closest : -1;
	}

	private Point getPoint(int i) {
		return (i >= 0 && i < points.size()) ? points.get(i) : null;
	}

	private int getPointWithin5Pixels(Point p) {
		int closestIdx = getClosestIndex(p);
		Point closest = getPoint(closestIdx);
		return (closest != null && p.distance(closest) < 5) ? closestIdx : -1;
	}

	private int getClosestIndex(Point p) {
		double minD = Double.POSITIVE_INFINITY;
		int minI = -1;
		for(int i = 0; i < points.size(); i++) {
			Point po = points.get(i);
			double d = p.distance(po);
			if(d < minD) {
				minD = d;
				minI = i;
			}
		}
		return minI;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int existingIdx = getPointWithin5Pixels(e.getPoint());
		if(existingIdx == -1) {
			int closestSegment = getSegmentWithin5Pixels(e.getPoint());
			if(closestSegment == -1)
				points.add(e.getPoint());
			else {
				System.out.println("insert at " + (closestSegment + 1));
				points.add(closestSegment + 1, e.getPoint());
			}
			repaint();
		} else if(e.isShiftDown()) {
			points.remove(existingIdx);
			repaint();
		}
	}

	int mouseDown = -1;
	@Override
	public void mousePressed(MouseEvent e) {
		if(e.isPopupTrigger()) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
			return;
		}
		mouseDown = getPointWithin5Pixels(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger()) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
			return;
		}
		mouseDown = -1;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(mouseDown == -1)
			return;
		points.get(mouseDown).setLocation(e.getPoint());
		repaint();
	}

	/* https://qroph.github.io/2018/07/30/smooth-paths-using-catmull-rom-splines.html */
	public static class Spline2D {
		final ArrayList<Point> ctrlPoints; // contains artificial start and stop point
		final double[] timeForCtrls;
		final double totalTime;

		Spline2D(List<Point> ctrls) {
			if(ctrls.size() < 2) {
				ctrlPoints = null;
				timeForCtrls = null;
				totalTime = 0;
				return;
			}
			Point first = new Point(
					2 * ctrls.get(0).x - ctrls.get(1).x,
					2 * ctrls.get(0).y - ctrls.get(1).y);
			Point last = new Point(
					2 * ctrls.get(ctrls.size() - 1).x - ctrls.get(ctrls.size() - 2).x,
					2 * ctrls.get(ctrls.size() - 1).y - ctrls.get(ctrls.size() - 2).y);
			this.ctrlPoints = new ArrayList<>(ctrls.size() + 2);
			ctrlPoints.add(first);
			ctrlPoints.addAll(ctrls);
			ctrlPoints.add(last);

			this.timeForCtrls = new double[ctrlPoints.size()];
			double summedTime = 0;
			for(int i = 2; i < ctrlPoints.size() - 1; i++) {
				double d = ctrlPoints.get(i).distance(ctrlPoints.get(i - 1));
				timeForCtrls[i] = summedTime + d;
				summedTime += d;
			}
			timeForCtrls[ctrlPoints.size() - 1] = summedTime;
			totalTime = summedTime;
		}

		public GeneralPath getPathOld() {
			GeneralPath path = new GeneralPath();
			if(ctrlPoints == null)
				return path;
			path.moveTo(ctrlPoints.get(1).x, ctrlPoints.get(1).y);
			for(int i = 1; i < ctrlPoints.size() - 2; i++) {
				Point p0 = ctrlPoints.get(i - 1);
				Point p1 = ctrlPoints.get(i);
				Point p2 = ctrlPoints.get(i + 1);
				Point p3 = ctrlPoints.get(i + 2);

				SplineSegment seg = new SplineSegment(p0,p1, p2, p3);
				double start = timeForCtrls[i];
				double end = timeForCtrls[i + 1];
				double interval = end - start;
				int n = (int)Math.round(interval / 5);
				double dTime = 1.0 / n;
				for(int s = 0; s < n; s++) {
					double time = s * dTime;
					double x = seg.interpolateX(time);
					double y = seg.interpolateY(time);
					path.lineTo(x, y);
				}
			}
			return path;
		}

		public GeneralPath getPath(int stepsPerSegment) {
			GeneralPath path = new GeneralPath();
			if(ctrlPoints == null)
				return path;
			path.moveTo(ctrlPoints.get(1).x, ctrlPoints.get(1).y);
			for(int i = 1; i < ctrlPoints.size() - 2; i++) {
				Point p0 = ctrlPoints.get(i - 1);
				Point p1 = ctrlPoints.get(i);
				Point p2 = ctrlPoints.get(i + 1);
				Point p3 = ctrlPoints.get(i + 2);

				SplineSegment seg = new SplineSegment(p0,p1, p2, p3);
				double dTime = 1.0 / stepsPerSegment;
				for(int s = 1; s <= stepsPerSegment; s++) {
					double time = s * dTime;
					double x = seg.interpolateX(time);
					double y = seg.interpolateY(time);
					path.lineTo(x, y);
				}
			}
			return path;
		}
	}

	private static final double TENSION = 0;

	/*
	 * ALPHA =   0: uniform     Catmull-Rom spline
	 * ALPHA = 0.5: centripetal
	 * ALPHA =   1: chordal
	 */
	private static final double ALPHA = 0.5;

	private static class SplineSegment {
		private final double ax, bx, cx, dx;
		private final double ay, by, cy, dy;

		public SplineSegment(Point p0, Point p1, Point p2, Point p3) {
			double t01 = Math.pow(p0.distance(p1), ALPHA);
			double t12 = Math.pow(p1.distance(p2), ALPHA);
			double t23 = Math.pow(p2.distance(p3), ALPHA);

			double m1x = (1.0 - TENSION) * (p2.x - p1.x + t12 * ((p1.x - p0.x) / t01 - (p2.x - p0.x) / (t01 + t12)));
			double m1y = (1.0 - TENSION) * (p2.y - p1.y + t12 * ((p1.y - p0.y) / t01 - (p2.y - p0.y) / (t01 + t12)));

			double m2x = (1.0 - TENSION) * (p2.x - p1.x + t12 * ((p3.x - p2.x) / t23 - (p3.x - p1.x) / (t12 + t23)));
			double m2y = (1.0 - TENSION) * (p2.y - p1.y + t12 * ((p3.y - p2.y) / t23 - (p3.y - p1.y) / (t12 + t23)));

			ax =  2 * (p1.x - p2.x) + m1x + m2x;
			ay =  2 * (p1.y - p2.y) + m1y + m2y;

			bx = -3 * (p1.x - p2.x) - m1x - m1x - m2x;
			by = -3 * (p1.y - p2.y) - m1y - m1y - m2y;

			cx = m1x;
			cy = m1y;

			dx = p1.x;
			dy = p1.y;
		}

		double interpolateX(double t) {
			double t2 = t * t;
			return ax * t2 * t + bx * t2 + cx * t + dx;
		}

		double interpolateY(double t) {
			double t2 = t * t;
			return ay * t2 * t + by * t2 + cy * t + dy;
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		CurveEditor ce = new CurveEditor();
		frame.getContentPane().add(ce, BorderLayout.CENTER);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
}
