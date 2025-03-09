package nlScript.screenplay;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class MouseDragOverlay {

	public enum Mode {
		SINGLE_POINT,
		CURVE
	}

	private JDialog frame;
	private OverlayEditor curveEditor;

	public MouseDragOverlay(Mode mode) {
		switch(mode) {
			case SINGLE_POINT: this.curveEditor = new SinglePointEditor(); break;
			case CURVE: this.curveEditor = new CurveEditor(); break;
		}
		this.curveEditor.getPanel().setOpaque(false);
		this.curveEditor.getPanel().setForeground(Color.WHITE);
		JFrame parent = new JFrame();
		frame = new JDialog(parent);
		frame.getContentPane().add(curveEditor.getPanel(), BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setUndecorated(true); // Remove window decorations (title bar, etc.)
		frame.setBackground(new Color(0, 0, 0, 150));
		// curveEditor.setBackground(new Color(0, 0, 0, 150)); // Make the window transparent
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

		frame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					close();
				}
			}
		});
	}

	public void close() {
		// correct for screen location
		for(Point p : curveEditor.getControls()) {
			p.x += frame.getX();
			p.y += frame.getY();
		}
		frame.dispose();
	}

	public void setControls(List<Point> controls) {
		ArrayList<Point> shifted = new ArrayList<>(controls);
		for(Point p : shifted) {
			p.x -= frame.getX();
			p.y -= frame.getY();
		}
		curveEditor.setControls(shifted);
	}

	public ArrayList<Point> getControls() {
		return curveEditor.getControls();
	}

	public void show(boolean withOK) {
		frame.setAlwaysOnTop(true);
		frame.setModal(false);
		frame.setVisible(true);
		if(withOK) {
			WaitForOK.block(frame, curveEditor.getEditInstruction());
			System.out.println("done");
			close();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			MouseHook mouseHook = new MouseHook();
			mouseHook.setMouseHook();
			nlScript.screenplay.KeyboardHook keyboardHook = new nlScript.screenplay.KeyboardHook();
			keyboardHook.setKeyboardHook();
			MouseDragOverlay mdo = new MouseDragOverlay(Mode.SINGLE_POINT);
			// mdo.showWithClickThrough(mouseHook, keyboardHook);
			mdo.show(true);
		});
	}

	public void showWithClickThrough(MouseHook mHook, nlScript.screenplay.KeyboardHook kHook) { // TODO name
		show(false);
		ScreenMessage.showMessage(curveEditor.getClickThroughInstruction());
		WindowUtils.makeWindowClickThrough(frame);
		final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
//		MouseHook.GlobalMouseListener ml = new MouseHook.GlobalMouseListener() {
//			@Override
//			public void mousePressed(MouseHook.GlobalMouseEvent e) {
//				System.out.println("mouse pressed: " + Thread.currentThread().getName());
//				Point p = new Point(
//						e.x - frame.getX(),
//						e.y - frame.getY());
//				curveEditor.appendPoint(p);
//			}
//
//			@Override
//			public void mouseReleased(MouseHook.GlobalMouseEvent e) {
//				System.out.println("mouse released: " + Thread.currentThread().getName());
//				List<Point> ctrls = getControls();
//				Point p = new Point(
//						e.x - frame.getX(),
//						e.y - frame.getY());
//				if(ctrls.isEmpty() || ctrls.get(ctrls.size() - 1).distance(p) > 2)
//					curveEditor.appendPoint(p);
//				loop.exit();
//			}
//
//			@Override
//			public void mouseMoved(MouseHook.GlobalMouseEvent e) {
//			}
//
//			@Override
//			public void mouseWheel(MouseHook.GlobalMouseEvent e) {
//			}
//		};

		nlScript.screenplay.KeyboardHook.GlobalKeyListener kl = new nlScript.screenplay.KeyboardHook.GlobalKeyListener() {
			@Override
			public void keyPressed(nlScript.screenplay.KeyboardHook.GlobalKeyEvent e) {
				System.out.println("key pressed: " + Thread.currentThread().getName());

				if (e.keycode == KeyEvent.VK_F3) {
					loop.exit();
					e.consume();
				} else if (e.keycode == KeyEvent.VK_F2) {
					Point cursor = MouseInfo.getPointerInfo().getLocation();
					Point p = new Point(
							cursor.x - frame.getX(),
							cursor.y - frame.getY());
					curveEditor.appendPoint(p);
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyboardHook.GlobalKeyEvent e) {
			}
		};

//		mHook.addGlobalMouseListener(ml);
		kHook.addGlobalKeyListener(kl);
		loop.enter();

//		mHook.removeGlobalMouseListener(ml);
		kHook.removeGlobalKeyListener(kl);

		ScreenMessage.hide();

		WindowUtils.undoMakeWindowClickThrough(frame);
		WaitForOK.block(frame, curveEditor.getEditInstruction());
		System.out.println("done");
		close();
	}

	public void switchToClickThrough() {

	}

	public void switchBackFromClickThrough() {

	}
}
