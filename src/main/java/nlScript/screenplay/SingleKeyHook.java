package nlScript.screenplay;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;

import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class SingleKeyHook {

	public static int getPressedKey() {
		AtomicInteger pressedKeyCode = new AtomicInteger();
		HOOKPROC keyboardHookProcedure = new WinUser.LowLevelKeyboardProc() {
			@Override
			public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT lParam) {
				if (nCode >= 0) {
					if (wParam.intValue() == WinUser.WM_KEYDOWN) {
						// Suppress the left mouse button down event
						pressedKeyCode.set(lParam.vkCode);
						User32.INSTANCE.PostQuitMessage(0);
						return new WinDef.LRESULT(1);
					}
				}
				Pointer ptr = lParam.getPointer();
				long peer = Pointer.nativeValue(ptr);
				return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, new WinDef.LPARAM(peer));
			}
		};

		WinUser.HHOOK keyboardHook = User32.INSTANCE.SetWindowsHookEx(
				WinUser.WH_KEYBOARD_LL, keyboardHookProcedure, null, 0);

		if (keyboardHook != null) {
			// Listen for mouse events
//			System.out.println("before getMessage");
			User32.INSTANCE.GetMessage(new WinUser.MSG(), null, 0, 0);
//			System.out.println("unhook");
			User32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
		} else {
			System.err.println("Failed to set mouse hook.");
		}
		return pressedKeyCode.get();
	}

	public static void waitForKey(int keycode) {
		HOOKPROC keyboardHookProcedure = new WinUser.LowLevelKeyboardProc() {
			@Override
			public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT lParam) {
				if (nCode >= 0) {
					if (wParam.intValue() == WinUser.WM_KEYDOWN && lParam.vkCode == keycode) {
						// Suppress the left mouse button down event
						User32.INSTANCE.PostQuitMessage(0);
						return new WinDef.LRESULT(1);
					}
				}
				Pointer ptr = lParam.getPointer();
				long peer = Pointer.nativeValue(ptr);
				return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, new WinDef.LPARAM(peer));
			}
		};

		WinUser.HHOOK keyboardHook = User32.INSTANCE.SetWindowsHookEx(
				WinUser.WH_KEYBOARD_LL, keyboardHookProcedure, null, 0);

		if (keyboardHook != null) {
			// Listen for mouse events
//			System.out.println("before getMessage");
			User32.INSTANCE.GetMessage(new WinUser.MSG(), null, 0, 0);
//			System.out.println("unhook");
			User32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
		} else {
			System.err.println("Failed to set mouse hook.");
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				waitForKey(KeyEvent.VK_F3);
				System.out.println("pressed F2");
			}
		});
		thread.start();

		Thread.sleep(1000);
	}
}
