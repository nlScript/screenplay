package nlScript.screenplay;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicReference;

public class FullscreenOverlay {
	private final JDialog frame;
//	private final JLabel coordinatesLabel;
	private final CoordinatesPanel coordinatesPanel;

	private final AtomicReference<Rectangle> dragged = new AtomicReference<>();

	private class CoordinateTextField extends JTextField {
		public CoordinateTextField() {
			super(7);
			setBackground(Color.DARK_GRAY);
			setForeground(Color.WHITE);
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_ENTER) {
						updateRectangleFromTextFields();
						transferFocus();
					}
				}
			});
			addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					updateRectangleFromTextFields();
				}

				public void focusGained(FocusEvent e) {
					selectAll();
				}
			});
		}
	}

	private static class CoordinateLabel extends JLabel {
		public CoordinateLabel(String text) {
			super(text);
			setBackground(Color.BLACK);
			setForeground(Color.WHITE);
		}
	}

	private class CoordinatesPanel extends JPanel {
		CoordinateTextField xTF = new CoordinateTextField();
		CoordinateTextField yTF = new CoordinateTextField();
		CoordinateTextField wTF = new CoordinateTextField();
		CoordinateTextField hTF = new CoordinateTextField();

		public CoordinatesPanel() {
			setLayout(new GridLayout(4, 2, 5, 5));
			setForeground(Color.WHITE);
			// setBackground(Color.BLACK);
			setOpaque(false);
			add(new CoordinateLabel("x")); add(xTF);
			add(new CoordinateLabel("y")); add(yTF);
			add(new CoordinateLabel("width")); add(wTF);
			add(new CoordinateLabel("height")); add(hTF);

			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(null, "Rectangle", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, null, Color.WHITE),
					BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		}

		void reset() {
			xTF.setText("");
			yTF.setText("");
			wTF.setText("");
			hTF.setText("");
		}

		void updateFromRectangle() {
			Rectangle r = dragged.get();
			if(r == null) {
				reset();
				return;
			}
			xTF.setText(Integer.toString(r.x));
			yTF.setText(Integer.toString(r.y));
			wTF.setText(Integer.toString(r.width));
			hTF.setText(Integer.toString(r.height));
		}
	}

	private void updateRectangleFromTextFields() {
		Rectangle r = dragged.get();
		if(r == null)
			return;
		r.x = Integer.parseInt(coordinatesPanel.xTF.getText());
		r.y = Integer.parseInt(coordinatesPanel.yTF.getText());
		r.width = Integer.parseInt(coordinatesPanel.wTF.getText());
		r.height = Integer.parseInt(coordinatesPanel.hTF.getText());
		frame.repaint();
	}

	public FullscreenOverlay(String message) {
		JFrame parent = new JFrame();
		frame = new JDialog(parent) {
			public void paint(Graphics g) {
				super.paint(g);
				customPaint(g);
			}
		};
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setUndecorated(true); // Remove window decorations (title bar, etc.)
		frame.setBackground(new Color(0, 0, 0, 150)); // Make the window transparent
		frame.setAlwaysOnTop(true); // Keep the window on top of other windows

		Rectangle2D result = new Rectangle2D.Double();
		GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (GraphicsDevice gd : localGE.getScreenDevices()) {
			Rectangle2D.union(result, gd.getDefaultConfiguration().getBounds(), result);
		}
		System.out.println(result);
		// frame.setSize(200, 150);
		frame.setSize((int) result.getWidth(), (int) result.getHeight());
		frame.setLocation((int) result.getX(), (int) result.getY());
		// frame.setSize(frame.getGraphicsConfiguration().getBounds().getSize());

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		frame.getContentPane().setLayout(gridbag);

		coordinatesPanel = new CoordinatesPanel();
//		coordinatesLabel = new JLabel("X: 0,   Y: 0");
		coordinatesPanel.setForeground(Color.WHITE);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		c.weighty = 0;
		c.anchor = GridBagConstraints.NORTHEAST;
		frame.getContentPane().add(coordinatesPanel, c);

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;
		JLabel messageLabel = new JLabel(message);
		messageLabel.setForeground(Color.WHITE);
		frame.getContentPane().add(messageLabel, c);

		c.gridy = 2;
		JButton close = new JButton("Done");
		c.insets = new Insets(5, 20, 5, 10);
		close.addActionListener(e -> frame.dispose());
		close.setForeground(Color.WHITE);
		close.setBackground(Color.DARK_GRAY);
		frame.getContentPane().add(close, c);

		c.gridy = 3;
		c.weighty = 1;
		JPanel dummy = new JPanel();
		dummy.setOpaque(false);
		frame.getContentPane().add(dummy, c);

//		Timer timer = new Timer(100, e -> {
//			PointerInfo pointerInfo = MouseInfo.getPointerInfo();
//			Point point = pointerInfo.getLocation();
//			int x = (int) point.getX();
//			int y = (int) point.getY();
//			coordinatesLabel.setText("X: " + x + ",   Y: " + y);
//		});
//		timer.start();

		frame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
					frame.dispose();
			}
		});

		frame.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				System.out.println(e.getPoint());
				updateRectangleFromMouse(e.getPoint());
				frame.repaint();
			}
		});

		frame.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mouseDown = e.getPoint();
				moving = dragged.get() != null && dragged.get().contains(mouseDown);
				updateRectangleFromMouse(e.getPoint());
				frame.repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mouseDown = null;
				Rectangle r = dragged.get();
				if(r == null)
					return;
				if(r.width == 0 || r.height == 0) {
					dragged.set(null);
					frame.repaint();
					coordinatesPanel.updateFromRectangle();
				}
			}
		});
	}

	private void updateRectangleFromMouse(Point mouse) {
		if(mouseDown == null)
			return;
		Rectangle r = dragged.get();
		if(!moving) {
			if(r == null) {
				r = new Rectangle(0, 0, 0, 0);
				dragged.set(r);
			}
			r.x = Math.min(mouse.x, mouseDown.x);
			r.y = Math.min(mouse.y, mouseDown.y);
			r.width = Math.abs(mouse.x - mouseDown.x);
			r.height = Math.abs(mouse.y - mouseDown.y);
		} else {
			if(r == null)
				return;
			r.x += mouse.x - mouseDown.x;
			r.y += mouse.y - mouseDown.y;
			mouseDown.setLocation(mouse);
		}
		coordinatesPanel.updateFromRectangle();
	}

	private boolean moving = false;
	private Point mouseDown = null;

	public void customPaint(Graphics g) {
		Rectangle r = dragged.get();
		if(r != null) {
			g.setColor(Color.WHITE);
			((Graphics2D) g).setStroke(new BasicStroke(2f));
			g.drawRect(r.x, r.y, r.width, r.height);
		}
		MouseInfo.getPointerInfo().getLocation();
	}

	private void show() {
		frame.setAlwaysOnTop(true);
		frame.setModal(true);
		frame.setVisible(true);
	}

	public static Rectangle getDraggedRectangle() {
		FullscreenOverlay fo = new FullscreenOverlay("Draw and adjust a rectangle");
		fo.show();
		Rectangle r = fo.dragged.get();
		return fo.dragged.get();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Rectangle ret = FullscreenOverlay.getDraggedRectangle();
			System.out.println(ret);
		});
	}
}
