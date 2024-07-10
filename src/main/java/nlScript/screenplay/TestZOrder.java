package nlScript.screenplay;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.JFrame;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.event.InputEvent;

import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;

public class TestZOrder {

	public static void main(String[] args) throws AWTException, InterruptedException {
		Robot robot = new Robot();
//		robot.mouseMove(300, 500);
//		robot.mouseMove(1271, 1426);
		robot.mouseMove(1220, 1425);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.delay(30);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		robot.delay(1000);

		JFrame frame = new JFrame("hehe");
		frame.setSize(300, 300);
		frame.setUndecorated(true);
		frame.setFocusable(false);
		frame.setFocusableWindowState(false);
		frame.setAlwaysOnTop(true);
		frame.setLocation(1220, 1200);
		frame.getContentPane().setBackground(Color.BLUE);
		frame.setVisible(true);

		WinDef.HWND thisWindow = WindowUtils.getHWnd(frame);
		WinDef.HWND foreground = WindowUtils.getForegroundWindowHandle();
		System.out.println("foreground window: " + foreground + " class: " + WindowUtils.getClassName(foreground) + " name: " + WindowUtils.getWindowTitle(foreground));
		WinDef.RECT foregroundRect = new WinDef.RECT();
		WindowUtils.MyUser32.INSTANCE.GetWindowRect(foreground, foregroundRect);
		System.out.println("foregroundRECT = " + foregroundRect);

		WinDef.HWND parent = foreground;
		while((parent = WindowUtils.MyUser32.INSTANCE.GetAncestor(parent, WinUser.GA_PARENT)) != null) {
			System.out.println("parent = " + parent + " class: " + WindowUtils.getClassName(parent));
		}
		WinDef.HWND foregroundRoot = WindowUtils.MyUser32.INSTANCE.GetAncestor(foreground, WinUser.GA_ROOT);
		System.out.println("foreground window parent: " + foregroundRoot);
		WinDef.HWND topWindow = WindowUtils.MyUser32.INSTANCE.GetTopWindow(null);
		System.out.println("top window = " + topWindow);
		System.out.println("this window: " + thisWindow);
		// System.out.println("active window: " + WindowUtils.MyUser32.INSTANCE.GetActiveWindow());

		System.out.println("bla");
		Thread.sleep(1000);

		if(!WindowUtils.MyUser32.INSTANCE.SetForegroundWindow(thisWindow))
			System.out.println("Error: " + Native.getLastError());

		// SetWindowPos(thisWindow, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE)
		WinDef.HWND TOP = new WinDef.HWND(Pointer.createConstant(0));
		if(!WindowUtils.MyUser32.INSTANCE.SetWindowPos(thisWindow, TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE))
			System.out.println("Error: " + Native.getLastError());


//		Rectangle myBounds = frame.getBounds();

//		final WindowUtils.MyUser32 inst = WindowUtils.MyUser32.INSTANCE;
//		boolean success = inst.EnumWindows(new User32.WNDENUMPROC() {
//			public boolean callback(WinDef.HWND hWnd, Pointer userData) {
////				if(!inst.IsWindowVisible(hWnd))
////					return true;
//
//				if(hWnd.equals(foreground))
//					System.out.println("FOUND FOREGROUND WINDOW");
//
//				if(hWnd.equals(thisWindow))
//					System.out.println("FOUND THIS WINDOW");
//
//				WinDef.RECT rect = new WinDef.RECT();
//				inst.GetWindowRect(hWnd, rect);
//				Rectangle bounds = rect.toRectangle();
//				System.out.println(hWnd + ": " + bounds);
//				if(bounds.intersects(myBounds)) {
//					System.out.println("found " + hWnd + " at " + bounds);
//					return true;
//				}
//				return true;
//			}
//		}, null);
	}
}
