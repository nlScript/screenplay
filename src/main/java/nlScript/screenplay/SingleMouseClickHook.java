package nlScript.screenplay;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;

import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.util.concurrent.atomic.AtomicReference;

public class SingleMouseClickHook {

	public static final int WM_LBUTTONDOWN = 513;

	public static PointerInfo getClickedPoint() {
		final AtomicReference<PointerInfo> pointerInfo = new AtomicReference<>();
		HOOKPROC mouseHookProcedure = new WinUser.LowLevelMouseProc() {
			@Override
			public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.MSLLHOOKSTRUCT info) {

				if (nCode >= 0) {
					if (wParam.intValue() == WM_LBUTTONDOWN) {
						// Suppress the left mouse button down event
						pointerInfo.set(MouseInfo.getPointerInfo());
						User32.INSTANCE.PostQuitMessage(0);
						return new WinDef.LRESULT(1);
					}
				}
				Pointer ptr = info.getPointer();
				long peer = Pointer.nativeValue(ptr);
				return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, new WinDef.LPARAM(peer));
			}
		};

		WinUser.HHOOK mouseHook = User32.INSTANCE.SetWindowsHookEx(
				WinUser.WH_MOUSE_LL, mouseHookProcedure, null, 0);

		if (mouseHook != null) {
			// Listen for mouse events
//			System.out.println("before getMessage");
			User32.INSTANCE.GetMessage(new WinUser.MSG(), null, 0, 0);
//			System.out.println("unhook");
			User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
		} else {
			System.err.println("Failed to set mouse hook.");
		}
		return pointerInfo.get();
	}

	public static void main(String[] args) {
		System.out.println(getClickedPoint());
	}
}
