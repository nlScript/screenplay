package nlScript.screenplay;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import java.awt.KeyboardFocusManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class KeystrokeDialog {
	public static String getKeyStrokeStringGUI() {
		JTextField keystrokeField = new JTextField();
		keystrokeField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<>());
		keystrokeField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new HashSet<>());
		keystrokeField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				e.consume();
			}

			public void keyPressed(KeyEvent e) {
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				keystrokeField.setText("<" + keyStroke.toString() + ">");
				e.consume();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				e.consume();
			}
		});
		final JComponent[] inputs = new JComponent[] {
				new JLabel("Keystroke"),
				keystrokeField,
		};

		JOptionPane pane = new JOptionPane(inputs, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
			@Override
			public void selectInitialValue() {
				keystrokeField.requestFocusInWindow();
			}
		};

		JDialog dialog = pane.createDialog(null, "Keystroke");
		dialog.setVisible(true);

		if(!pane.getValue().equals(JOptionPane.OK_OPTION))
			return "";
		String ks = keystrokeField.getText();
		if(ks.startsWith("<"))
			ks = ks.substring(1);
		if(ks.endsWith(">"))
			ks = ks.substring(0, ks.length() - 1);
		return ks;
	}

	public static String getKeyStrokeString(JTextComponent c) {
		final AtomicReference<KeyStroke> keyStroke = new AtomicReference<>();
		final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
		KeyListener kl = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				e.consume();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				keyStroke.set(KeyStroke.getKeyStrokeForEvent(e));
				e.consume();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(keyStroke.get() == null)
					return;
				e.consume();
				loop.exit();
			}
		};
		c.addKeyListener(kl);
		loop.enter();
		c.removeKeyListener(kl);

		return keyStroke.toString().replace("pressed ", "").replace("pressed", "");
	}

	public static String getKeyStrokeString(nlScript.screenplay.KeyboardHook hook) {
		final AtomicBoolean isAltDown   = new AtomicBoolean();
		final AtomicBoolean isShiftDown = new AtomicBoolean();
		final AtomicBoolean isCtrlDown  = new AtomicBoolean();
		final AtomicInteger keycode     = new AtomicInteger();

		final AtomicBoolean ready = new AtomicBoolean();

		System.out.println("getKeyStrokeString");

		final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();

		final nlScript.screenplay.KeyboardHook.GlobalKeyListener l = new nlScript.screenplay.KeyboardHook.GlobalKeyListener() {
			@Override
			public void keyPressed(nlScript.screenplay.KeyboardHook.GlobalKeyEvent e) {
				System.out.println("e = " + e);
				ready.set(true);
				isAltDown.set(e.isAltDown);
				isShiftDown.set(e.isShiftDown);
				isCtrlDown.set(e.isControlDown);
				keycode.set(e.keycode);
				// if(e.keycode != KeyEvent.VK_SHIFT && e.keycode != KeyEvent.VK_CONTROL && e.keycode != KeyEvent.VK_ALT)
				e.consume();
			}

			@Override
			public void keyReleased(KeyboardHook.GlobalKeyEvent e) {
				if(!ready.get())
					return;
				loop.exit();
				hook.removeGlobalKeyListener(this);
				e.consume();
			}
		};
		hook.addGlobalKeyListener(l);
		loop.enter();
		int modifiers = 0;
		if(isAltDown.get())   modifiers |= InputEvent.ALT_DOWN_MASK;
		if(isShiftDown.get()) modifiers |= InputEvent.SHIFT_DOWN_MASK;
		if(isCtrlDown.get())  modifiers |= InputEvent.CTRL_DOWN_MASK;
		return KeyStroke.getKeyStroke(keycode.get(), modifiers, false).toString();
	}

	public static void mainGUI(String[] args) {
		System.out.println(KeystrokeDialog.getKeyStrokeStringGUI());
	}

	public static void main(String[] args) {
		mainGUI(args);
//		KeyboardHook hook = new KeyboardHook();
//		hook.setKeyboardHook();
//		System.out.println(KeystrokeDialog.getKeyStrokeString(hook));
//		hook.unsetKeyboardHook();
		// System.out.println(KeyStroke.getKeyStroke("shift ctrl alt SHIFT"));
	}
}
