package nlScript.screenplay;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;

import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

import static com.sun.jna.platform.win32.W32Errors.SUCCEEDED;
import static com.sun.jna.platform.win32.WinUser.*;

public class WindowUtils {

	// https://github.com/DeflatedPickle/Hanger-chan/blob/master/src/main/java/com/deflatedpickle/jna/User32Extended.java

	// States
	int STATE_SYSTEM_FOCUSABLE = 0x00100000;
	static int STATE_SYSTEM_INVISIBLE = 0x00008000;
	int STATE_SYSTEM_OFFSCREEN = 0x00010000;
	int STATE_SYSTEM_UNAVAILABLE = 0x00000001;
	int STATE_SYSTEM_PRESSED = 0x00000008;

	// Extended Styles
	// https://docs.microsoft.com/en-gb/windows/desktop/winmsg/extended-window-styles
	int WS_EX_ACCEPTFILES = 0x00000010;
	static int WS_EX_APPWINDOW = 0x00040000;
	int WS_EX_CLIENTEDGE = 0x00000200;
	int WS_EX_COMPOSITED = 0x02000000;
	int WS_EX_CONTEXTHELP = 0x00000400;
	int WS_EX_CONTROLPARENT = 0x00010000;
	int WS_EX_DLGMODALFRAME = 0x00000001;
	int WS_EX_LAYERED = 0x00080000;
	int WS_EX_LAYOUTRTL = 0x00400000;
	int WS_EX_LEFT = 0x00000000;
	int WS_EX_LEFTSCROLLBAR = 0x00004000;
	int WS_EX_LTRREADING = 0x00000000;
	int WS_EX_MDICHILD = 0x00000040;
	static int WS_EX_NOACTIVATE = 0x08000000;
	int WS_EX_NOINHERITLAYOUT = 0x00100000;
	int WS_EX_NOPARENTNOTIFY = 0x00000004;
	int WS_EX_NOREDIRECTIONBITMAP = 0x00200000;
	int WS_EX_RIGHT = 0x00001000;
	int WS_EX_RIGHTSCROLLBAR = 0x00000000;
	int WS_EX_RTLREADING = 0x00002000;
	int WS_EX_STATICEDGE = 0x00020000;
	static int WS_EX_TOOLWINDOW = 0x00000080;
	int WS_EX_TOPMOST = 0x00000008;
	int WS_EX_TRANSPARENT = 0x00000020;
	int WS_EX_WINDOWEDGE = 0x00000100;

	long WS_EX_OVERLAPPEDWINDOW = (WS_EX_WINDOWEDGE | WS_EX_CLIENTEDGE);
	long WS_EX_PALETTEWINDOW = (WS_EX_WINDOWEDGE | WS_EX_TOOLWINDOW | WS_EX_TOPMOST);

	public interface DwmApi extends Library {
		DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

		WinNT.HRESULT DwmGetWindowAttribute(
				WinDef.HWND hwnd,
				WinDef.DWORD dwAttribute,
				WinDef.PVOID pvAttribute,
				WinDef.DWORD cbAttribute
		);
	}

	// https://github.com/DeflatedPickle/Hanger-chan/blob/master/src/main/java/com/deflatedpickle/jna/TITLEBARINFO.java
	@Structure.FieldOrder({"cbSize", "rcTitleBar", "rgstate"})
	public static class TITLEBARINFO extends Structure {
		public int cbSize;
		public WinDef.RECT rcTitleBar;
		public int[] rgstate;

		public TITLEBARINFO() {
			rgstate = new int[CCHILDREN_TITLEBAR + 1];
			cbSize = size();
		}

		// Index constants
		public static final int TITLE_BAR = 0;
		public static final int RESERVED = 1;
		public static final int MINIMIZE_BUTTON = 2;
		public static final int MAXIMIZE_BUTTON = 3;
		public static final int HELP_BUTTON = 4;
		public static final int CLOSE_BUTTON = 5;

		// Child amount constant
		public static final int CCHILDREN_TITLEBAR = 5;
	}

	public interface MyUser32 extends User32 {
		// DEFAULT_OPTIONS is critical for W32 API functions to simplify ASCII/UNICODE details
		MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);
		WinDef.HWND WindowFromPoint(WinDef.POINT.ByValue cursor);

		BOOL IsTopLevelWindow(HWND hWnd);

		// https://docs.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-gettitlebarinfo
		boolean GetTitleBarInfo(WinDef.HWND hwnd, TITLEBARINFO titlebarinfo);

		// https://docs.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-getlastactivepopup
		WinDef.HWND GetLastActivePopup(WinDef.HWND hwnd);

		WinDef.HWND GetShellWindow();

		WinDef.HWND GetTopWindow(WinDef.HWND hwnd);

		boolean GetCaretPos(POINT p);

		HWND SetActiveWindow(HWND win);

		void keybd_event(byte bVk, byte bScan, int dwFlags, ULONG_PTR dwExtraInfo);

		void mouse_event(int dwFlags, int dx, int dy, int dwData, ULONG_PTR dwExtraInfo);
	}

	enum WindowState {
		NORMAL(WinUser.SW_NORMAL),
		MAXIMIZED(WinUser.SW_MAXIMIZE),
		MINIMIZED(WinUser.SW_MINIMIZE);

		private final int state;

		private WindowState(int state) {
			this.state = state;
		}

		public int getState() {
			return this.state;
		}
	}

	public static String getClassName(HWND hwnd) {
		char[] className = new char[256];
		MyUser32.INSTANCE.GetClassName(hwnd, className, className.length);
		return Native.toString(className);
	}

	public static Point getGlobalCaretPosition() {
		POINT p = new POINT(-1, 0);
//		WinDef.POINT.ByReference lpPoint = new POINT.ByReference();
		boolean hasCaret = MyUser32.INSTANCE.GetCaretPos(p);
		if(!hasCaret)
			return null;
		return new Point(p.x, p.y);
	}

	/**
	 * Get the window handle from the OS
	 */
	public static WinDef.HWND getHWnd(Component w) {
		WinDef.HWND hwnd = new WinDef.HWND();
		hwnd.setPointer(Native.getComponentPointer(w));
		return hwnd;
	}

	public static void makeWindowClickThrough(Component w) {
		WinDef.HWND hwnd = getHWnd(w);
		int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		wl = wl | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
	}

	public static boolean isTopLevelWindow(WinDef.HWND win) {
		return MyUser32.INSTANCE.IsTopLevelWindow(win).booleanValue();
	}

	public static void undoMakeWindowClickThrough(Component w) {
		WinDef.HWND hwnd = getHWnd(w);
		int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		wl = wl & /* ~WinUser.WS_EX_LAYERED & */ ~WinUser.WS_EX_TRANSPARENT;
		User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
	}

	/**
	 * Returns the window at the given screen coordinate
	 * @param x coordinate
	 * @param y coordinate
	 * @return the window at the given coordinate, or null if there is none
	 */
	public static WinDef.HWND getWindowAtPoint(int x, int y) {
		WinDef.POINT.ByValue pt = new WinDef.POINT.ByValue();
		pt.x = x;
		pt.y = y;
		WinDef.HWND win = MyUser32.INSTANCE.WindowFromPoint(pt);
		if (win == null)
			return null;

		WinDef.HWND tmp;
		while ((tmp = User32.INSTANCE.GetParent(win)) != null)
			win = tmp;
		return win;
	}

	public static void setAlwaysOnTop(WinDef.HWND win) {
		WinDef.HWND TOPMOST = new WinDef.HWND(Pointer.createConstant(-1));
		User32.INSTANCE.SetWindowPos(win, TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
	}

	public static void setAlwaysOnTop(Component c) {
		WinDef.HWND win = getHWnd(c);
		setAlwaysOnTop(win);
	}

	/**
	 * Returns the title of the given window
	 * @param win window
	 * @return the window's title
	 */
	public static String getWindowTitle(WinDef.HWND win) {
		int tl = User32.INSTANCE.GetWindowTextLength(win) + 1;
		char[] title = new char[tl];
		int l = User32.INSTANCE.GetWindowText(win, title, title.length);
		return Native.toString(title);
	}

	/**
	 * Returns the window with the specified title
	 * @param title the window's title
	 * @return the window with the given title
	 */
	public static WinDef.HWND getWindow(String title) {
		return User32.INSTANCE.FindWindow(null, title);
	}

	/**
	 * Gets a handle to the window with the specified title or throws an exception if such a window cannot be found.
	 * @param title of the window
	 * @return window handle
	 */
	public static WinDef.HWND getWindowOrThrow(String title) {
		WinDef.HWND ret = User32.INSTANCE.FindWindow(null, title);
		if(ret == null) {
			List<String> titles = listWindowTitles();
			throw new RuntimeException("No window with title '" + title + "', available windows are " + titles);
		}
		return ret;
	}

	/**
	 * Set the state of the window with the specified title
	 * @param title of the window
	 * @param state to be set
	 */
	public static void setWindowState(String title, WindowState state) {
		WinDef.HWND win = getWindowOrThrow(title);
		if(!User32.INSTANCE.ShowWindow(win, state.getState()))
			throw new RuntimeException("Cannot set the state of the window " + title + " to " + state);
	}

	/**
	 * Set the position and dimensions of the window with the specified title
	 * @param title of the window
	 * @param x coorinate
	 * @param y coordinate
	 * @param w width
	 * @param h height
	 */
	public static void setWindowPosition(String title, int x, int y, int w, int h) {
		WinDef.HWND win = getWindowOrThrow(title);
		if(!User32.INSTANCE.MoveWindow(win, x, y, w, h, true))
			throw new RuntimeException("Cannot set window (" + title + ") position and size to " +
					w + " x " + h + " at " + "(" + x + ", " + y + ")");
	}

	/**
	 * Activate the window with the given title
	 * @param title of the window
	 */
	public static void activateWindow(String title) {
		WinDef.HWND win = getWindowOrThrow(title);
		activateWindow(win);
	}

	public static void activateWindow(HWND win) {
//		forceForegroundWindow(win);
		HWND HWND_TOPMOST = new HWND(Pointer.createConstant(-1));
		HWND HWND_NOTOPMOST = new HWND(Pointer.createConstant(-2));
		HWND m_hWnd = win;
		HWND hCurWnd = User32.INSTANCE.GetForegroundWindow();
		int dwMyID = Kernel32.INSTANCE.GetCurrentThreadId();
		int dwCurID = User32.INSTANCE.GetWindowThreadProcessId(hCurWnd, null);
		User32.INSTANCE.AttachThreadInput(new DWORD(dwMyID), new DWORD(dwCurID), true);
		User32.INSTANCE.SetWindowPos(m_hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE);
		User32.INSTANCE.SetWindowPos(m_hWnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE);
		if(!User32.INSTANCE.SetForegroundWindow(m_hWnd)) {
			System.out.println("SetForegroundWindow() failed");
			// throw new RuntimeException("Cannot activate window " + getWindowTitle(win));
		}
		if(User32.INSTANCE.SetFocus(m_hWnd) == null)
			System.out.println("SetFocus() failed");
		if(MyUser32.INSTANCE.SetActiveWindow(m_hWnd) == null)
			System.out.println("SetActiveWindow() failed");
		User32.INSTANCE.AttachThreadInput(new DWORD(dwMyID), new DWORD(dwCurID), false);

		User32.INSTANCE.SetWindowLongPtr(hCurWnd, GWL_HWNDPARENT, win.getPointer());

		TITLEBARINFO tbi = new TITLEBARINFO();
		MyUser32.INSTANCE.GetTitleBarInfo(win, tbi);
		Rectangle rect = tbi.rcTitleBar.toRectangle();

		Point toClick = new Point(
			rect.x + rect.width / 2,
			rect.y + rect.height / 2);
		toClick = Screen.getScaled(toClick);
		try {
			Robot robot = new Robot();
			Point tmp = MouseInfo.getPointerInfo().getLocation();
			robot.mouseMove(toClick.x, toClick.y);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(50);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseMove(tmp.x, tmp.y);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	// https://pinvoke.net/default.aspx/user32.SetForegroundWindow
	// https://stackoverflow.com/questions/10740346/setforegroundwindow-only-working-while-visual-studio-is-open/13881647#13881647
	private static void forceForegroundWindow(HWND hWnd) {
		HWND hCurWnd = User32.INSTANCE.GetForegroundWindow();

		User32.INSTANCE.SetWindowLongPtr(hCurWnd, GWL_HWNDPARENT, hWnd.getPointer());

		DWORD foreThread = new DWORD(User32.INSTANCE.GetWindowThreadProcessId(hCurWnd, null));
		DWORD appThread = new DWORD(Kernel32.INSTANCE.GetCurrentThreadId());

		final int SW_SHOW = 5;

		try {
			byte ALT = (byte) 0xA4;
			byte EXTENDEDKEY = 0x1;
			byte KEYUP = 0x2;
			MyUser32.INSTANCE.keybd_event(ALT, (byte) 0x45, EXTENDEDKEY, null);
			MyUser32.INSTANCE.keybd_event(ALT, (byte) 0x45, EXTENDEDKEY | KEYUP, null);
			// new Robot().keyRelease(KeyEvent.VK_ALT);
		} catch(Exception e) {
			e.printStackTrace();
		}
//		if(!foreThread.equals(appThread))
//			User32.INSTANCE.AttachThreadInput(foreThread, appThread, true);

//		if(!User32.INSTANCE.BringWindowToTop(hWnd))
//			throw new RuntimeException("BringWindowToTop() failed");
//		if(!User32.INSTANCE.ShowWindow(hWnd, SW_SHOW))
//			throw new RuntimeException("ShowWindow() failed");
//		if(MyUser32.INSTANCE.SetActiveWindow(hWnd) == null)
//			throw new RuntimeException("SetActiveWindow() failed");
		if(!User32.INSTANCE.SetForegroundWindow(hWnd)) {
			System.out.println("SetForegroundWindow failed");
			// throw new RuntimeException("SetForegroundWindow() failed");
		}
		if(User32.INSTANCE.SetFocus(hWnd) == null) {
			System.out.println("SetFocus() failed");
			// throw new RuntimeException("SetFocus() failed");
		}

//		if(!foreThread.equals(appThread))
//			User32.INSTANCE.AttachThreadInput(foreThread, appThread, false);

	}

	/**
	 * Get the position of the window with the given title
	 * @param title of the window
	 * @return window position
	 */
	// TODO rename to getWindowBounds
	public static Rectangle getWindowPosition(String title) {
		WinDef.HWND win = getWindowOrThrow(title);
		return getWindowPosition(win);
	}

	public static Rectangle getWindowPosition(HWND win) {
		WinDef.RECT rect = new WinDef.RECT();
		if(!User32.INSTANCE.GetWindowRect(win, rect))
			throw new RuntimeException("Cannot get the bounds of the window '" + getWindowTitle(win) + "'");
		return rect.toRectangle();
	}

	/**
	 * Get the title of the window in the foreground
	 * @return the foreground window's title
	 */
	public static String getForegroundWindow() {
		WinDef.HWND hWnd = User32.INSTANCE.GetForegroundWindow();
		if(hWnd == null)
			throw new RuntimeException("Error retrieving foreground window");
		return getWindowTitle(hWnd);
	}

	public static HWND getForegroundWindowHandle() {
		return User32.INSTANCE.GetForegroundWindow();
	}

	/**
	 * Pause until a window with the given title is visible.
	 * @param title of the window to wait for
	 */
	public static void waitForWindow(String title) {
		while(!listWindowTitles().contains(title)) {
			System.out.println("Waiting for window " + title);
			delay(500);
		}
	}

	/**
	 * Returns a list of current top-level windows.
	 * @return list of top-level windows
	 */
	public static List<String> listWindowTitles() {
		final User32 user32 = User32.INSTANCE;
		final List<String> titles = new ArrayList<>();

		boolean success = user32.EnumWindows(new User32.WNDENUMPROC() {
			public boolean callback(WinDef.HWND hWnd, Pointer userData) {
				int n = user32.GetWindowTextLength(hWnd) + 1;
				char[] windowText = new char[n];
				user32.GetWindowText(hWnd, windowText, n + 1);
				String wText = Native.toString(windowText);
				if(WindowUtils.IsAltTabWindow(hWnd)) {
					titles.add(wText);
				}
				return true;
			}
		}, null);
		if(!success)
			throw new RuntimeException("Error retrieving list of windows");

		return titles;
	}

	public static List<HWND> listWindows() {
		final User32 user32 = User32.INSTANCE;
		final List<HWND> windows = new ArrayList<>();

		boolean success = user32.EnumWindows(new User32.WNDENUMPROC() {
			public boolean callback(WinDef.HWND hWnd, Pointer userData) {
				if(WindowUtils.IsAltTabWindow(hWnd))
					windows.add(hWnd);
				return true;
			}
		}, null);
		if(!success)
			throw new RuntimeException("Error retrieving list of windows");

		return windows;
	}

	// https://stackoverflow.com/questions/7277366/why-does-enumwindows-return-more-windows-than-i-expected
	// https://devblogs.microsoft.com/oldnewthing/20200302-00/?p=103507
	private static boolean IsWindowCloaked(WinDef.HWND hwnd) {
		WinDef.BOOLByReference isCloaked = new WinDef.BOOLByReference(new WinDef.BOOL(false));
		WinDef.PVOID p = new WinDef.PVOID(isCloaked.getPointer());
		return (SUCCEEDED(DwmApi.INSTANCE.DwmGetWindowAttribute(hwnd, new WinDef.DWORD(14) /* DWMWA_CLOAKED */,
				p, new WinDef.DWORD(WinDef.BOOL.SIZE))) && isCloaked.getValue().booleanValue());
	}

	public static boolean IsAltTabWindowOld(WinDef.HWND hwnd) {
		TITLEBARINFO ti = new TITLEBARINFO();
		WinDef.HWND hwndTry, hwndWalk = null;

		if(!User32.INSTANCE.IsWindowVisible(hwnd))
			return false;

		hwndWalk = User32.INSTANCE.GetAncestor(hwnd, GA_ROOTOWNER);
		while((hwndTry = MyUser32.INSTANCE.GetLastActivePopup(hwndWalk)) != hwndWalk)
		{
			if(User32.INSTANCE.IsWindowVisible(hwndTry))
				break;
			hwndWalk = hwndTry;
		}
		if(hwndWalk != hwnd)
			return false;

		// the following removes some task tray programs and "Program Manager"
		ti.cbSize = ti.size();
		MyUser32.INSTANCE.GetTitleBarInfo(hwnd, ti);
		if((ti.rgstate[0] & STATE_SYSTEM_INVISIBLE) != 0)
			return false;

		// Tool windows should not be displayed either, these do not appear in the
		// task bar.
		if((User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE) & WS_EX_TOOLWINDOW) != 0)
			return false;

		return true;
	}

	// https://stackoverflow.com/questions/72069771/show-a-list-of-all-alttab-windows-even-full-screen-uwp-windows-and-retrieve
	public static boolean IsAltTabWindowOld2(WinDef.HWND hwnd) {
		TITLEBARINFO ti = new TITLEBARINFO();
		WinDef.HWND hwndTry, hwndWalk = null;

		if(!User32.INSTANCE.IsWindowVisible(hwnd))
			return false;

//		if(User32.INSTANCE.GetAncestor(hwnd, GA_ROOT) != hwnd)
//			return false;

		if(IsWindowCloaked(hwnd))
			return false;

		// task bar.
		if((User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE) & WS_EX_TOOLWINDOW) != 0)
			return false;

		// the following removes some task tray programs and "Program Manager"
		ti.cbSize = ti.size();
		MyUser32.INSTANCE.GetTitleBarInfo(hwnd, ti);
		if((ti.rgstate[0] & STATE_SYSTEM_INVISIBLE) != 0)
			return false;

		return true;
	}

	public static boolean IsAltTabWindow(HWND hwnd) {
		if(!User32.INSTANCE.IsWindowVisible(hwnd))
			return false;

		if(IsWindowCloaked(hwnd))
			return false;

		int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
		if((wl & WS_EX_APPWINDOW) != 0)
			return true;
		if((wl & WS_EX_NOACTIVATE) != 0)
			return false;
		if((wl & WS_EX_TOOLWINDOW) != 0)
			return false;

		wl = User32.INSTANCE.GetWindowLong(hwnd, GWL_STYLE);
		if((wl & WS_CHILD) != 0)
			return false;
		if((wl & WS_TILEDWINDOW) != 0)
			return true;

		System.out.println("Not sure what to do with window " + hwnd);
		return true;
	}

	/**
	 * Returns true if the specified window is currently visible
	 * @param hwnd the window
	 * @return the window's visibility
	 */
	public static boolean IsWindowVisibleOnScreen(WinDef.HWND hwnd) {
		return User32.INSTANCE.IsWindowVisible(hwnd) &&
				!IsWindowCloaked(hwnd);
	}

	private static void delay(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main2(String[] args) {
		WinDef.HWND win = User32.INSTANCE.FindWindow(null, "Task-Manager");
		System.out.println(win);
		User32.INSTANCE.MoveWindow(win, 0, 0, 800, 600, true);
		User32.INSTANCE.SetForegroundWindow(win);

		win = User32.INSTANCE.GetForegroundWindow();
		int tl = User32.INSTANCE.GetWindowTextLength(win);
		char[] title = new char[tl];
		User32.INSTANCE.GetWindowText(win, title, title.length);
		System.out.println("foreground window is " + new String(title));

		System.out.println("Fiji");
		win = getWindow("(Fiji Is Just) ImageJ");
		System.out.println(win);
		System.out.println(IsAltTabWindow(win));
	}

	public static void main3(String[] args) {
		List<String> windows = listWindowTitles();
		System.out.println(windows);
		for(int i = 0; i < 20; i++) {
			System.out.println(getGlobalCaretPosition());
			delay(1000);
		}
	}
}
