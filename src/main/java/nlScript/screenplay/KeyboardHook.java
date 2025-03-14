package nlScript.screenplay;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.util.ArrayList;

public class KeyboardHook implements AutoCloseable{

	@Override
	public void close() {
		unsetKeyboardHook();
	}

	public static class GlobalKeyEvent {

		private boolean consumed;

		public final int keycode;

		public final char[] unicode;

		public final boolean isControlDown;
		public final boolean isShiftDown;
		public final boolean isAltDown;

		public GlobalKeyEvent(int keycode, char[] unicode, boolean isControlDown, boolean isShiftDown, boolean isAltDown) {
			this.keycode = keycode;
			this.unicode = unicode;
			this.isControlDown = isControlDown;
			this.isShiftDown = isShiftDown;
			this.isAltDown = isAltDown;
		}

		public void consume() {
			consumed = true;
		}

		public String toString() {
			return "isShiftDown? " + isShiftDown + " isAltDown? " + isAltDown + " isCtrlDown? " + isControlDown + " kc=" + keycode;
		}

		public static boolean isModifier(int keycode) {
			return keycode == WinUser.VK_CONTROL || keycode == WinUser.VK_LCONTROL || keycode == WinUser.VK_RCONTROL ||
					keycode == WinUser.VK_SHIFT  || keycode == WinUser.VK_LSHIFT   || keycode == WinUser.VK_RSHIFT ||
					keycode == WinUser.VK_MENU   || keycode == WinUser.VK_LMENU    || keycode == WinUser.VK_RMENU;
		}

		public boolean isModifier() {
			return isModifier(this.keycode);
		}

		public int getModifiers() {
			return toModifier(isAltDown, isShiftDown, isControlDown);
		}

		public static int toModifier(boolean isAltDown, boolean isShiftDown, boolean isControlDown) {
			int modifiers = 0;
			if (isAltDown)     modifiers |= InputEvent.ALT_DOWN_MASK;
			if (isShiftDown)   modifiers |= InputEvent.SHIFT_DOWN_MASK;
			if (isControlDown) modifiers |= InputEvent.CTRL_DOWN_MASK;
			return modifiers;
		}

		public static boolean isAltDown(int modifiers) {
			return (modifiers & InputEvent.ALT_DOWN_MASK) != 0;
		}

		public static boolean isShiftDown(int modifiers) {
			return (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
		}

		public static boolean isControlDown(int modifiers) {
			return (modifiers & InputEvent.CTRL_DOWN_MASK) != 0;
		}

		public KeyStroke getKeyStroke() {
			int keycode = KeyCodeMap.getJavaKeyCode(this.keycode);
			return KeyStroke.getKeyStroke(keycode, getModifiers());
		}

		public static KeyStroke getKeyStroke(int winKeyCode, int modifiers) {
			int keycode = KeyCodeMap.getJavaKeyCode(winKeyCode);
			return KeyStroke.getKeyStroke(keycode, modifiers);
		}
	}

	public interface GlobalKeyListener {
		void keyPressed(GlobalKeyEvent e);
		void keyReleased(GlobalKeyEvent e);
	}

	private final ArrayList<GlobalKeyListener> listeners = new ArrayList<>();

	public void addGlobalKeyListener(GlobalKeyListener l) {
		listeners.add(l);
	}

	public void removeGlobalKeyListener(GlobalKeyListener l) {
		listeners.remove(l);
	}

	public void waitForKey(final int keycode) {
		final Object lock = new Object();
		GlobalKeyListener l = new GlobalKeyListener() {
			@Override
			public void keyPressed(GlobalKeyEvent e) {
				if(e.keycode == keycode) {
					synchronized (lock) {
						lock.notify();
					}
					e.consume();
				}
			}

			@Override
			public void keyReleased(GlobalKeyEvent e) {
			}
		};

		addGlobalKeyListener(l);
		synchronized (lock) {
			try {
				lock.wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		removeGlobalKeyListener(l);
	}

	private void fireKeyPressed(GlobalKeyEvent e) {
		for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
			listeners.get(i).keyPressed(e);
	}

	private void fireKeyReleased(GlobalKeyEvent e) {
		for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
			listeners.get(i).keyReleased(e);
	}

	public final User32 USER32INST;
	public final Kernel32 KERNEL32INST;

	public KeyboardHook() {
		if (!Platform.isWindows()) {
			throw new UnsupportedOperationException("Not supported on this platform.");
		}
		USER32INST = User32.INSTANCE;
		KERNEL32INST = Kernel32.INSTANCE;
		keyboardHook = hookTheKeyboard();
		Native.setProtected(true);
		// setKeyboardHook();
	}

	private static WinUser.LowLevelKeyboardProc keyboardHook;
	private WinUser.HHOOK hhk;
	public Thread thrd;
	public boolean threadFinish = true;
	public boolean isHooked = false;

	public void unsetKeyboardHook() {
		threadFinish = true;
		if (thrd.isAlive()) {
			thrd.interrupt();
			thrd = null;
		}
		isHooked = false;
	}

	public boolean isIsHooked() {
		return isHooked;
	}

	public void setKeyboardHook() {
		thrd = new Thread(() -> {
			try {
				if (!isHooked) {
					hhk = USER32INST.SetWindowsHookEx(
							User32.WH_KEYBOARD_LL,
							keyboardHook,
							KERNEL32INST.GetModuleHandle(null),
							0);
					isHooked = true;
					WinUser.MSG msg = new WinUser.MSG();
					while ((USER32INST.GetMessage(msg, null, 0, 0)) != 0) {
						USER32INST.TranslateMessage(msg);
						USER32INST.DispatchMessage(msg);
						if (!isHooked) {
							break;
						}
					}
					USER32INST.UnhookWindowsHookEx(hhk);
				} else {
					System.err.println("The Hook is already installed.");
				}
			} catch (Exception e) {
				throw new RuntimeException("Exception in MouseHook", e);
			}
		});
		thrd.setName("keyboard-hook");
		threadFinish = false;
		thrd.start();

	}

	private boolean isCtrlDown = false;
	private boolean isShiftDown = false;
	private boolean isAltDown = false;


	public WinUser.LowLevelKeyboardProc hookTheKeyboard() {
		return new WinUser.LowLevelKeyboardProc() {
			@Override
			public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT lParam) {
				KeyboardHook.GlobalKeyEvent event = null;
				if (nCode >= 0) {
					int keycode = lParam.vkCode;
					int scancode = lParam.scanCode;
					User32Ex.INSTANCE.GetKeyState(WinUser.VK_SHIFT);
					User32Ex.INSTANCE.GetKeyState(WinUser.VK_CONTROL);
					User32Ex.INSTANCE.GetKeyState(WinUser.VK_MENU);
					char[] unicode = toUnicode(keycode, scancode);

					int intValue = wParam.intValue();

					if (intValue == WinUser.WM_KEYDOWN || intValue == WinUser.WM_SYSKEYDOWN) {
						isCtrlDown  = isCtrlDown  || keycode == WinUser.VK_CONTROL || keycode == WinUser.VK_LCONTROL || keycode == WinUser.VK_RCONTROL;
						isShiftDown = isShiftDown || keycode == WinUser.VK_SHIFT   || keycode == WinUser.VK_LSHIFT   || keycode == WinUser.VK_RSHIFT;
						isAltDown   = isAltDown   || keycode == WinUser.VK_MENU    || keycode == WinUser.VK_LMENU    || keycode == WinUser.VK_RMENU;
						event = new GlobalKeyEvent(keycode, unicode, isCtrlDown, isShiftDown, isAltDown);
						fireKeyPressed(event);
					} else if(intValue == WinUser.WM_KEYUP || intValue == WinUser.WM_SYSKEYUP) {
						if(keycode == WinUser.VK_CONTROL || keycode == WinUser.VK_LCONTROL || keycode == WinUser.VK_RCONTROL) isCtrlDown  = false;
						if(keycode == WinUser.VK_SHIFT   || keycode == WinUser.VK_LSHIFT   || keycode == WinUser.VK_RSHIFT)   isShiftDown = false;
						if(keycode == WinUser.VK_MENU    || keycode == WinUser.VK_LMENU    || keycode == WinUser.VK_RMENU)    isAltDown = false;
						event = new GlobalKeyEvent(keycode, unicode, isCtrlDown, isShiftDown, isAltDown);
						fireKeyReleased(event);
					} else {
						System.out.println("wParam.intValue() = " + wParam.intValue());
					}
					if (threadFinish) {
						USER32INST.PostQuitMessage(0);
					}
				}
				if(event != null && event.consumed)
					return new WinDef.LRESULT(1); // don't dispatch further, prevent default

				Pointer ptr = lParam.getPointer();
				long peer = Pointer.nativeValue(ptr);
				return USER32INST.CallNextHookEx(hhk, nCode, wParam, new WinDef.LPARAM(peer));
			}
		};
	}

	private char[] toUnicode(int keycode, int scancode) {
		WinDef.HKL keyboardLayout = User32.INSTANCE.GetKeyboardLayout(0);
		char[] ret = new char[2];
		byte[] lpKeyState = new byte[256];
		User32.INSTANCE.GetKeyboardState(lpKeyState);
		int l = User32.INSTANCE.ToUnicodeEx(keycode, scancode, lpKeyState, ret, ret.length, 4, keyboardLayout);
		if(l == ret.length)
			return ret;
		if(l == -1)
			return new char[0];
		char[] newret = new char[l];
		System.arraycopy(ret, 0, newret, 0, l);
		return newret;
	}

	public interface User32Ex extends User32 {
		// DEFAULT_OPTIONS is critical for W32 API functions to simplify ASCII/UNICODE details
		KeyboardHook.User32Ex INSTANCE = Native.load("user32", KeyboardHook.User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

		SHORT GetKeyState(int nVirtKey);
	}

	public static void main(String[] args) throws InterruptedException {
		KeyboardHook hooker = new KeyboardHook();
		hooker.setKeyboardHook();
		hooker.addGlobalKeyListener(new GlobalKeyListener() {
			@Override
			public void keyPressed(GlobalKeyEvent e) {
				System.out.println("keyPressed: " + e.keycode);
					e.consume();
			}

			@Override
			public void keyReleased(GlobalKeyEvent e) {
				System.out.println("keyReleased: " + e.keycode);
					e.consume();
			}
		});
		Thread.sleep(10 * 1000);
		hooker.unsetKeyboardHook();
		System.exit(0);
		System.out.println("done");
	}
}
