package nlScript.screenplay;

import com.sun.jna.platform.win32.*;
import com.sun.jna.Native;

public class WindowMonitor2 {

	// https://learn.microsoft.com/en-us/windows/win32/winauto/event-constants
	public static final int EVENT_SYSTEM_MOVESIZESTART = 0x000A;
	public static final int EVENT_SYSTEM_MOVESIZEEND = 0x000B;

	public static final int EVENT_OBJECT_CREATE = 0x8000;

	public static final int EVENT_OBJECT_SHOW = 0x8002;
	public static final int EVENT_MIN = 0x00000001;
	public static final int EVENT_MAX = 0x7FFFFFFF;

	public static final int WM_CREATE = 0x0001;

	// https://www.pinvoke.net/default.aspx/user32/SetWinEventHook.html
	public static final int WINEVENT_OUTOFCONTEXT   = 0x0000; // Events are ASYNC
	public static final int WINEVENT_SKIPOWNTHREAD  = 0x0001; // Don't call back for events on installer's thread
	public static final int WINEVENT_SKIPOWNPROCESS = 0x0002; // Don't call back for events on installer's process
	public static final int WINEVENT_INCONTEXT      = 0x0004; // Events are SYNC, this causes your dll to be injected into every process

	private static WinUser.HHOOK hook;

	public static void main(String[] args) {

		// Specify the event constant for window movement and resizing
		int eventMin = EVENT_OBJECT_SHOW; // EVENT_SYSTEM_MOVESIZESTART;
		int eventMax = EVENT_OBJECT_SHOW;

		WinUser.HOOKPROC winEventProc = new WinUser.HOOKPROC() {
			public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
//				switch(message) {
//					case WM_CREATE:
//						System.out.println("window created");
//
//				}
//				Pointer ptr = lParam.toPointer();
//				long peer = Pointer.nativeValue(ptr);
//				return User32.INSTANCE.CallNextHookEx(hook, message, wParam, new WinDef.LPARAM(peer));
				return new WinDef.LRESULT(0);
			}

		};

		// Set up the event hook
		hook = User32.INSTANCE.SetWindowsHookEx(
				5, // WH_CBT
				winEventProc,
				Kernel32.INSTANCE.GetModuleHandle(null),
				0);
		System.out.println("err = " + Native.getLastError());


		if (hook == null) {
			System.err.println("Failed to set up WinEventHook.");
			return;
		}
		User32.INSTANCE.GetMessage(null,null, 0, 0);

		// Wait for user input to exit
		System.out.println("Press Enter to exit.");
		new java.util.Scanner(System.in).nextLine();

		// Unhook the event
		User32.INSTANCE.UnhookWindowsHookEx(hook);
	}
}