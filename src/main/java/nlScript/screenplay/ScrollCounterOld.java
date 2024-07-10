package nlScript.screenplay;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;

@Deprecated
public final class ScrollCounterOld {

	public final User32 USER32INST;
	public final Kernel32 KERNEL32INST;

	private int scrollCounts = 0;

	public ScrollCounterOld() {
		if (!Platform.isWindows()) {
			throw new UnsupportedOperationException("Not supported on this platform.");
		}
		USER32INST = User32.INSTANCE;
		KERNEL32INST = Kernel32.INSTANCE;
		mouseHook = hookTheMouse();
		Native.setProtected(true);

	}
	public static WinUser.LowLevelMouseProc mouseHook;
	public WinUser.HHOOK hhk;
	public Thread thrd;
	public boolean threadFinish = true;
	public boolean isHooked = false;

	public static final int WM_MOUSEWHEEL = 522;

	public void unsetMouseHook() {
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

	public void setMouseHook() {
		thrd = new Thread(() -> {
			try {
				if (!isHooked) {
					hhk = USER32INST.SetWindowsHookEx(User32.WH_MOUSE_LL, mouseHook, KERNEL32INST.GetModuleHandle(null), 0);
					isHooked = true;
					WinUser.MSG msg = new WinUser.MSG();
					while ((USER32INST.GetMessage(msg, null, 0, 0)) != 0) {
						USER32INST.TranslateMessage(msg);
						USER32INST.DispatchMessage(msg);
						if (!isHooked) {
							break;
						}
					}
				} else {
					System.out.println("The Hook is already installed.");
				}
			} catch (Exception e) {
				throw new RuntimeException("Exception in MouseHook", e);
			}
		});
		threadFinish = false;
		thrd.start();

	}
	public short hiword(int l) {
		return (short)((l >> 16) & 0xffff);
	}

	public WinUser.LowLevelMouseProc hookTheMouse() {
		return new WinUser.LowLevelMouseProc() {
			@Override
			public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.MSLLHOOKSTRUCT info) {
				if (nCode >= 0) {
					boolean isCtrlDown  = (USER32INST.GetAsyncKeyState(WinUser.VK_CONTROL)  & (1 << 15)) != 0;
					boolean isShiftDown = (USER32INST.GetAsyncKeyState(WinUser.VK_SHIFT)    & (1 << 15)) != 0;
					boolean isAltDown   = (USER32INST.GetAsyncKeyState(WinUser.VK_MENU)     & (1 << 15)) != 0;
					// WinDef.POINT pt = info.pt;
					Point pt = MouseInfo.getPointerInfo().getLocation(); // these are scaled coordinates
					switch (wParam.intValue()) {
						case MouseHook.WM_MOUSEWHEEL: // Scrolling by wheel
							scrollCounts++;
//							System.out.println("scrolled by " +  hiword(info.mouseData));
							break;
						default:
							break;
					}
					if (threadFinish) {
						USER32INST.PostQuitMessage(0);
					}
				}
				Pointer ptr = info.getPointer();
				long peer = Pointer.nativeValue(ptr);
				return USER32INST.CallNextHookEx(hhk, nCode, wParam, new WinDef.LPARAM(peer));
			}
		};
	}

	public int getScrollCounts() {
		return scrollCounts;
	}

	public static void main(String[] args) throws InterruptedException {
		ScrollCounterOld scrollCounter = new ScrollCounterOld();
		scrollCounter.setMouseHook();
		System.out.println("ready");
		Thread.sleep(5 * 1000);
		scrollCounter.unsetMouseHook();
		System.out.println("Counted " + scrollCounter.getScrollCounts() + " scroll ticks.");
		System.exit(0);
	}
}
