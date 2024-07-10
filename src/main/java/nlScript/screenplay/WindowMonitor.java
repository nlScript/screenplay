package nlScript.screenplay;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.*;

import java.util.ArrayList;
import java.util.HashMap;

public class WindowMonitor implements AutoCloseable {

	public static class WindowEvent {
		private boolean consumed = false;

		public final WinDef.HWND window;

		public WindowEvent(WinDef.HWND window) {
			this.window = window;
		}

		public void consume() {
			consumed = true;
		}
	}

	public interface WindowListener {
		void windowOpened(WindowEvent e);
		void windowClosed(WindowEvent e);
	}

	private final ArrayList<WindowListener> listeners = new ArrayList<>();

	public void addWindowListener(WindowListener l) {
		listeners.add(l);
	}

	public void removeWindowListener(WindowListener l) {
		listeners.remove(l);
	}

	private void fireWindowOpened(WindowEvent e) {
		for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
			listeners.get(i).windowOpened(e);
	}

	private void fireWindowClosed(WindowEvent e) {
		for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
			listeners.get(i).windowClosed(e);
	}



	// https://learn.microsoft.com/en-us/windows/win32/winauto/event-constants
	public static final int EVENT_SYSTEM_MOVESIZESTART = 0x000A;
	public static final int EVENT_SYSTEM_MOVESIZEEND = 0x000B;

	public static final int EVENT_OBJECT_CREATE = 0x8000;

	public static final int EVENT_OBJECT_DESTROY = 0x8001;

	public static final int EVENT_OBJECT_SHOW = 0x8002;
	public static final int EVENT_OBJECT_HIDE = 0x8003;
	public static final int EVENT_MIN = 0x00000001;
	public static final int EVENT_MAX = 0x7FFFFFFF;

	// https://www.pinvoke.net/default.aspx/user32/SetWinEventHook.html
	public static final int WINEVENT_OUTOFCONTEXT   = 0x0000; // Events are ASYNC
	public static final int WINEVENT_SKIPOWNTHREAD  = 0x0001; // Don't call back for events on installer's thread
	public static final int WINEVENT_SKIPOWNPROCESS = 0x0002; // Don't call back for events on installer's process
	public static final int WINEVENT_INCONTEXT      = 0x0004; // Events are SYNC, this causes your dll to be injected into every process

	@Override
	public void close() {
		unsetWindowEventHook();
	}

	private WinUser.WinEventProc windowHook;

	private Thread thrd;

	private boolean threadFinish = true;

	private boolean isHooked = false;

	WinNT.HANDLE hhk;

	private final HashMap<WinDef.HWND, String> openedWindows = new HashMap<>();

	public WindowMonitor() {
		if (!Platform.isWindows()) {
			throw new UnsupportedOperationException("Not supported on this platform.");
		}
		windowHook = hookWindowsEvents();
		Native.setProtected(true);
	}

	private void initializeOpenWindows() {
		for(WinDef.HWND hwnd : WindowUtils.listWindows()) {
			openedWindows.put(hwnd, WindowUtils.getWindowTitle(hwnd));
		}
	}

	public void unsetWindowEventHook() {
		threadFinish = true;
		if (thrd.isAlive()) {
			thrd.interrupt();
			thrd = null;
		}
		isHooked = false;
	}

	public boolean isHooked() {
		return isHooked;
	}

	public void setWindowEventHook() {
		final int eventMin = EVENT_OBJECT_SHOW; // EVENT_SYSTEM_MOVESIZESTART;
		final int eventMax = EVENT_OBJECT_HIDE;
		initializeOpenWindows();
		thrd = new Thread(() -> {
			try {
				if (!isHooked) {
					// Set up the event hook
					WinNT.HANDLE hook = User32.INSTANCE.SetWinEventHook(
							eventMin, eventMax,
							null,
							windowHook,
							0, 0,
							WINEVENT_OUTOFCONTEXT
					);

					if (hook == null) {
						System.err.println("Failed to set up WinEventHook.");
						return;
					}

					isHooked = true;
					WinUser.MSG msg = new WinUser.MSG();
					while ((User32.INSTANCE.GetMessage(msg, null, 0, 0)) != 0) {
						User32.INSTANCE.TranslateMessage(msg);
						User32.INSTANCE.DispatchMessage(msg);
						if (!isHooked) {
							break;
						}
					}
					User32.INSTANCE.UnhookWinEvent(hook);
				} else {
					System.out.println("The Hook is already installed.");
				}
			} catch (Exception e) {
				throw new RuntimeException("Exception in MouseHook", e);
			}
		});
		thrd.setName("window-hook");
		threadFinish = false;
		thrd.start();
	}

	public WinUser.WinEventProc hookWindowsEvents() {
		return new WinUser.WinEventProc() {
			@Override
			public void callback(WinNT.HANDLE hWinEventHook, WinDef.DWORD event, WinDef.HWND hwnd, WinDef.LONG idObject, WinDef.LONG idChild, WinDef.DWORD dwEventThread, WinDef.DWORD dwmsEventTime) {
				if(event.intValue() == EVENT_OBJECT_SHOW) {
					if(WindowUtils.IsAltTabWindow(hwnd)) {
						if(openedWindows.get(hwnd) == null) {
							openedWindows.put(hwnd, WindowUtils.getWindowTitle(hwnd));
							fireWindowOpened(new WindowEvent(hwnd));
						}
					}
				}
				else if(event.intValue() == EVENT_OBJECT_HIDE) {
					String title = openedWindows.get(hwnd);
					if(title != null) {
						if(!WindowUtils.IsAltTabWindow(hwnd)) { // window is not visible any more
							openedWindows.remove(hwnd);
							fireWindowClosed(new WindowEvent(hwnd));
						}
//						else {
//							System.out.println(title + " is still visible");
//						}
					}
				}
			}
		};
	}

	public static void mainOld(String[] args) {

		// Specify the event constant for window movement and resizing
		int eventMin = EVENT_OBJECT_SHOW; // EVENT_SYSTEM_MOVESIZESTART;
		int eventMax = EVENT_OBJECT_HIDE;

		HashMap<WinDef.HWND, String> openedWindows = new HashMap<>();

		// Set up the WinEventProc callback
		WinUser.WinEventProc winEventProc = (hWinEventHook, event, hwnd, idObject, idChild, dwEventThread, dwmsEventTime) -> {
			if(event.intValue() == EVENT_OBJECT_SHOW) {
				if(WindowUtils.IsAltTabWindow(hwnd)) {
					if(openedWindows.get(hwnd) == null) {
//						System.out.println("Window created: " + WindowUtils.getWindowTitle(hwnd) + " (" + hwnd + ")");
						openedWindows.put(hwnd, WindowUtils.getWindowTitle(hwnd));
					}
				}
			}
			else if(event.intValue() == EVENT_OBJECT_HIDE) {
				String title = openedWindows.get(hwnd);
				if(title != null) {
					System.out.println();
					if(!WindowUtils.IsAltTabWindow(hwnd)) { // window is not visible any more
						// System.out.println("Closed " + title + " (" + hwnd + ")");
						openedWindows.remove(hwnd);
					}
//					else {
//						System.out.println(title + " is still visible");
//					}
				}
			}
		};

		// Set up the event hook
		WinNT.HANDLE hook = User32.INSTANCE.SetWinEventHook(
				eventMin, eventMax,
				null,
				winEventProc,
				0, 0,
				WINEVENT_OUTOFCONTEXT
		);

		if (hook == null) {
			System.err.println("Failed to set up WinEventHook.");
			return;
		}
		User32.INSTANCE.GetMessage(null,null, 0, 0);

		// Wait for user input to exit
		System.out.println("Press Enter to exit.");
		new java.util.Scanner(System.in).nextLine();

		// Unhook the event
		User32.INSTANCE.UnhookWinEvent(hook);
	}

	public static void main(String[] args) throws InterruptedException {
		WindowMonitor monitor = new WindowMonitor();
		monitor.setWindowEventHook();
		monitor.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
				System.out.println("window opened: " + WindowUtils.getWindowTitle(e.window));
			}

			@Override
			public void windowClosed(WindowEvent e) {
				System.out.println("window closed: " + WindowUtils.getWindowTitle(e.window));
			}
		});
		Thread.sleep(10 * 1000);
		monitor.unsetWindowEventHook();
		System.exit(0);
//		System.out.println("waiting for F3");
//		hooker.waitForKey(KeyEvent.VK_F);
		System.out.println("done");
	}
}