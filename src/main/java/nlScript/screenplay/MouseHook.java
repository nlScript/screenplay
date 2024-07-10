package nlScript.screenplay;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public final class MouseHook {
    public static class GlobalMouseEvent {

        public static final int LEFT   = MouseEvent.BUTTON1;
        public static final int RIGHT  = MouseEvent.BUTTON3;
        public static final int MIDDLE = MouseEvent.BUTTON2;

        public static final int NO_SCROLL = 0;

        private boolean consumed = false;


        public final int x, y;
        public final int scrollAmount;

        public final boolean isControlDown;
        public final boolean isShiftDown;
        public final boolean isAltDown;

        public final int button;

        public GlobalMouseEvent(int x, int y, int button, boolean isControlDown, boolean isShiftDown, boolean isAltDown) {
            this(x, y, button, NO_SCROLL, isControlDown, isShiftDown, isAltDown);
        }

        public GlobalMouseEvent(int x, int y, int button, int scrollAmount, boolean isControlDown, boolean isShiftDown, boolean isAltDown) {
            this.x = x;
            this.y = y;
            this.button = button;
            this.scrollAmount = scrollAmount;
            this.isControlDown = isControlDown;
            this.isShiftDown = isShiftDown;
            this.isAltDown = isAltDown;
        }

        public void consume() {
            consumed = true;
        }

        public String toString() {
            return "GlobalMouseEvent[x=" + x +
                    ", y=" + y +
                    ", button=" + button +
                    ", scrollAmount=" + scrollAmount +
                    ", isControlDown=" + isControlDown +
                    ", isShiftDown=" + isShiftDown +
                    ", isAltDown=" + isAltDown + "]";
        }
    }

    public interface GlobalMouseListener {
        void mousePressed(GlobalMouseEvent e);
        void mouseReleased(GlobalMouseEvent e);
        void mouseMoved(GlobalMouseEvent e);
        void mouseWheel(GlobalMouseEvent e);
    }

    private final ArrayList<GlobalMouseListener> listeners = new ArrayList<>();

    public void addGlobalMouseListener(GlobalMouseListener l) {
        listeners.add(l);
    }

    public void removeGlobalMouseListener(GlobalMouseListener l) {
        listeners.remove(l);
    }

    private void fireMousePressed(GlobalMouseEvent e) {
        for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
            listeners.get(i).mousePressed(e);
    }

    private void fireMouseReleased(GlobalMouseEvent e) {
        for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
            listeners.get(i).mouseReleased(e);
    }

    private void fireMouseMoved(GlobalMouseEvent e) {
        for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
            listeners.get(i).mouseMoved(e);
    }

    private void fireMouseWheel(GlobalMouseEvent e) {
        for(int i = listeners.size() - 1; i >= 0 && !e.consumed; i--)
            listeners.get(i).mouseWheel(e);
    }

    public final User32 USER32INST;
    public final Kernel32 KERNEL32INST;

    public MouseHook() {
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
    public static final int WM_MOUSEMOVE = 512;
    public static final int WM_LBUTTONDOWN = 513;
    public static final int WM_LBUTTONUP = 514;
    public static final int WM_RBUTTONDOWN = 516;
    public static final int WM_RBUTTONUP = 517;
    public static final int WM_MBUTTONDOWN = 519;
    public static final int WM_MBUTTONUP = 520;
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
                    USER32INST.UnhookWindowsHookEx(hhk);
                } else {
                    System.out.println("The Hook is already installed.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception in MouseHook", e);
            }
        });
        thrd.setName("mouse-hook");
        threadFinish = false;
        thrd.start();

    }

    private WinUser.LowLevelMouseProc hookTheMouse() {
        // https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-msllhookstruct
        return new WinUser.LowLevelMouseProc() {
            @Override
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.MSLLHOOKSTRUCT info) {
                GlobalMouseEvent event = null;
                if (nCode >= 0) {
                    boolean isCtrlDown  = (USER32INST.GetAsyncKeyState(WinUser.VK_CONTROL)  & (1 << 15)) != 0;
                    boolean isShiftDown = (USER32INST.GetAsyncKeyState(WinUser.VK_SHIFT)    & (1 << 15)) != 0;
                    boolean isAltDown   = (USER32INST.GetAsyncKeyState(WinUser.VK_MENU)     & (1 << 15)) != 0;
                    // WinDef.POINT pt = info.pt;
                    Point pt = MouseInfo.getPointerInfo().getLocation(); // these are scaled coordinates
                    switch (wParam.intValue()) {
                        case MouseHook.WM_LBUTTONDOWN: // Left click
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.LEFT, isCtrlDown, isShiftDown, isAltDown);
//                            System.out.println("left button down at " + pt.x + ", " + pt.y);
                            fireMousePressed(event);
                            break;
                        case MouseHook.WM_RBUTTONDOWN: // Right click
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.RIGHT, isCtrlDown, isShiftDown, isAltDown);
//                            System.out.println("right button down at " + pt.x + ", " + pt.y);
                            fireMousePressed(event);
                            break;
                        case MouseHook.WM_MBUTTONDOWN:  // Middle click
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.MIDDLE, isCtrlDown, isShiftDown, isAltDown);
//                            System.out.println("middle button down at " + pt.x + ", " + pt.y);
                            fireMousePressed(event);
                            break;
                        case MouseHook.WM_LBUTTONUP:
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.LEFT, isCtrlDown, isShiftDown, isAltDown);
                            fireMouseReleased(event);
//                            System.out.println("left button up");
                            break;
                        case MouseHook.WM_RBUTTONUP:
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.RIGHT, isCtrlDown, isShiftDown, isAltDown);
                            fireMouseReleased(event);
//                            System.out.println("right button up");
                            break;
                        case MouseHook.WM_MBUTTONUP:
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.MIDDLE, isCtrlDown, isShiftDown, isAltDown);
                            fireMouseReleased(event);
//                            System.out.println("middle button up");
                            break;
                        case MouseHook.WM_MOUSEMOVE:
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.MIDDLE, isCtrlDown, isShiftDown, isAltDown);
                            fireMouseMoved(event);
                            break;
                        case MouseHook.WM_MOUSEWHEEL: // Scrolling by wheel
                            int scrollAmount = hiword(info.mouseData);
                            /*
                             * One tick is 120, directions are reversed in WINAPI and Java. Compare:
                             * https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-msllhookstruct#members
                             * https://docs.oracle.com/javase/8/docs/api/java/awt/Robot.html#mouseWheel-int-
                             */
                            scrollAmount /= -120;
                            event = new GlobalMouseEvent(pt.x, pt.y, GlobalMouseEvent.MIDDLE, scrollAmount, isCtrlDown, isShiftDown, isAltDown);
                            fireMouseWheel(event);
                            break;
                        default:
                            break;
                    }
                    if (threadFinish) {
                        USER32INST.PostQuitMessage(0);
                    }
                }
                if(event != null && event.consumed)
                    return new WinDef.LRESULT(1); // don't dispatch further, prevent default

                Pointer ptr = info.getPointer();
                long peer = Pointer.nativeValue(ptr);
                return USER32INST.CallNextHookEx(hhk, nCode, wParam, new WinDef.LPARAM(peer));
            }
        };
    }

    private static short hiword(int l) {
        return (short)((l >> 16) & 0xffff);
    }


    public static void main(String[] args) throws InterruptedException {
        MouseHook hooker = new MouseHook();
        hooker.setMouseHook();
        GlobalMouseListener l = new GlobalMouseListener() {
            @Override
            public void mousePressed(GlobalMouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseReleased(GlobalMouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseMoved(GlobalMouseEvent e) {

            }

            @Override
            public void mouseWheel(GlobalMouseEvent e) {
                e.consume();
            }
        };
        hooker.addGlobalMouseListener(l);
        Thread.sleep(10 * 1000);
        hooker.removeGlobalMouseListener(l);
        hooker.unsetMouseHook();
        System.exit(0);
    }
}
