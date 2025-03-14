package nlScript.screenplay;

import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import nlScript.ui.ACEditor;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Recorder implements nlScript.screenplay.MouseHook.GlobalMouseListener, nlScript.screenplay.KeyboardHook.GlobalKeyListener, WindowMonitor.WindowListener, AutoCloseable {

	private final nlScript.screenplay.MouseHook mHook;

	private final nlScript.screenplay.KeyboardHook kHook;

	private final WindowMonitor wHook;

	private final ACEditor editor;

	private boolean ignoreEvents = false;

	private static final int TOGGLE_IGNORE_KEYCODE = java.awt.event.KeyEvent.VK_F8;

	private Runnable onToggleIgnoreEvents = null;

	public Recorder(nlScript.screenplay.MouseHook mHook, nlScript.screenplay.KeyboardHook kHook, ACEditor editor) {
		this.mHook = mHook;
		this.kHook = kHook;
		this.wHook = new WindowMonitor();
		this.wHook.setWindowEventHook();
		this.editor = editor;
		emitLast.start();
	}

	public void close() {
		mHook.removeGlobalMouseListener(this);
		kHook.removeGlobalKeyListener(this);
		wHook.removeWindowListener(this);
		wHook.close();
		emitLast.cancel();
	}

	public void start() {
		eventList.clear();
		mHook.addGlobalMouseListener(this);
		kHook.addGlobalKeyListener(this);
		wHook.addWindowListener(this);
		ignoreEvents = false;
	}

	public void pause() {
		mHook.removeGlobalMouseListener(this);
		kHook.removeGlobalKeyListener(this);
		wHook.removeWindowListener(this);
	}

	public void setOnToggleIgnoreEvents(Runnable onToggleIgnoreEvents) {
		this.onToggleIgnoreEvents = onToggleIgnoreEvents;
	}

	public void ignoreEvents() {
		ignoreEvents = true;
		if(onToggleIgnoreEvents != null)
			onToggleIgnoreEvents.run();
	}

	public void unIgnoreEvents() {
		ignoreEvents = false;
		if(onToggleIgnoreEvents != null)
			onToggleIgnoreEvents.run();
	}

	public void toggleIgnoreEvents() {
		ignoreEvents = !ignoreEvents;
		if(onToggleIgnoreEvents != null)
			onToggleIgnoreEvents.run();
	}

	public boolean getIgnoreEvents() {
		return ignoreEvents;
	}

	@Override
	public void windowOpened(WindowMonitor.WindowEvent e) {
		try {
			Thread.sleep(300);
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
		addEvent(new WindowOpenedEvent(e.window, System.currentTimeMillis()));
		onNewEvent();
	}

	@Override
	public void windowClosed(WindowMonitor.WindowEvent e) {
	}

	@Override
	public void keyPressed(nlScript.screenplay.KeyboardHook.GlobalKeyEvent e) {
		System.out.println("keyPressed " + e);
		if(e.keycode == TOGGLE_IGNORE_KEYCODE) {
			toggleIgnoreEvents();
			e.consume();
			return;
		}

//		System.out.println("key pressed " + e.getKeyStroke());
		if(!keyDown[e.keycode]) {
			nKeysDown++;
			keyDown[e.keycode] = true;
		}
		addEvent(new KeyPressEvent(e.keycode, e.unicode, e.getModifiers(), nKeysDown, System.currentTimeMillis()));
	}

	@Override
	public void keyReleased(nlScript.screenplay.KeyboardHook.GlobalKeyEvent e) {
		System.out.println("keyReleased " + e);
		if(e.keycode == TOGGLE_IGNORE_KEYCODE) {
			e.consume();
			return;
		}
//		System.out.println("key released: " + e.getKeyStroke());
		if(keyDown[e.keycode]) {
			nKeysDown--;
			if(nKeysDown == 0)
				System.out.println("No keys pressed any more");
			keyDown[e.keycode] = false;
		}
		addEvent(new KeyReleaseEvent(e.keycode, e.unicode, e.getModifiers(), nKeysDown, System.currentTimeMillis()));
		onNewEvent();
	}

	private final boolean[] keyDown = new boolean[256];

	private int nKeysDown = 0;
	private final boolean[] mouseButtonDown = new boolean[3];

	private Point lastRecordedMouseLocation = new Point(-100000, -100000);

	@Override
	public void mousePressed(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
		mouseButtonDown[e.button - 1] = true;
		addEvent(new MousePressEvent(e.x, e.y, e.button, System.currentTimeMillis()));
		lastRecordedMouseLocation.setLocation(e.x, e.y);
	}

	@Override
	public void mouseReleased(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
		mouseButtonDown[e.button - 1] = false;
		addEvent(new MouseReleaseEvent(e.x, e.y, e.button, System.currentTimeMillis()));
		onNewEvent();
		lastRecordedMouseLocation.setLocation(e.x, e.y);
	}

	@Override
	public void mouseMoved(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
		if(mouseButtonDown[0] || mouseButtonDown[1] || mouseButtonDown[2]) {
			addEvent(new MouseMoveEvent(e.x, e.y, e.button, System.currentTimeMillis()));
			onNewEvent();
			lastRecordedMouseLocation.setLocation(e.x, e.y);
		}
	}

	@Override
	public void mouseWheel(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
		addEvent(new MouseWheelEvent(e.x, e.y, e.scrollAmount,System.currentTimeMillis()));
		onNewEvent();
	}

	private static String getPointString(Point p) {
		return "(" + p.x + ", " + p.y + ")";
	}

	private static String getPointString(MouseEvent e) {
		return "(" + e.x + ", " + e.y + ")";
	}

	private static String getPointListString(Point[] pts) {
		return Arrays.stream(pts).map(Recorder::getPointString).collect(Collectors.joining(","));
	}

	private static String getButtonString(int button) {
		switch(button) {
			case nlScript.screenplay.MouseHook.GlobalMouseEvent.LEFT: return "left";
			case nlScript.screenplay.MouseHook.GlobalMouseEvent.RIGHT: return "right";
			case nlScript.screenplay.MouseHook.GlobalMouseEvent.MIDDLE: return "middle";
		}
		return null;
	}




	private abstract static class Event {
		public final long millis;

		private boolean done = false;

		Event(long millis) {
			this.millis = millis;
		}

		public abstract String emit();

		public abstract Event mergeWithNext(Event next);

		public abstract int maxDelayToMergeNext();

		public boolean isDone() {
			return done;
		}

		public void setDone(boolean b) {
			done = b;
		}
	}

	private static abstract class MouseEvent extends Event {
		public final int x, y, button;

		public MouseEvent(int x, int y, int button, long millis) {
			super(millis);
			this.x = x;
			this.y = y;
			this.button = button;
		}
	}

	private static class MousePressEvent extends MouseEvent {
		public MousePressEvent(int x, int y, int button, long millis) {
			super(x, y, button, millis);
		}

		public String emit() {
			return "Mouse press at " + getButtonString(button) + " at " + getPointString(this) + ".";
		}

		// Does nothing.
		// TODO Could check here for mouse release, currently that's done in analyzeFromMousePressToRelease
		public Event mergeWithNext(Event next) {
			return null;
		}

		public int maxDelayToMergeNext() {
			return -1;
		}
	}

	private static class MouseReleaseEvent extends MouseEvent {
		public MouseReleaseEvent(int x, int y, int button, long millis) {
			super(x, y, button, millis);
		}

		public String emit() {
			return "Mouse release at " + getButtonString(button) + " at " + getPointString(this) + ".";
		}

		public Event mergeWithNext(Event next) {
			return null;
		}

		public int maxDelayToMergeNext() {
			return -1;
		}
	}

	private static class MouseClickEvent extends MouseEvent {
		public MouseClickEvent(int x, int y, int button, long millis) {
			super(x, y, button, millis);
		}

		public String emit() {
			return "Mouse click " + getButtonString(button) + " at " + getPointString(this) + ".";
		}

		public Event mergeWithNext(Event next) {
			if(next instanceof MouseClickEvent) {
				MouseClickEvent n = (MouseClickEvent) next;
				if(n.x == x && n.y == y && n.button == button) {
					DoubleClickEvent dce = new DoubleClickEvent(x, y, button, millis);
					dce.setDone(true);
					return dce;
				}
			}
			return null;
		}

		public int maxDelayToMergeNext() {
			return 500;
		}
	}

	private static class DoubleClickEvent extends MouseEvent {
		public DoubleClickEvent(int x, int y, int button, long millis) {
			super(x, y, button, millis);
		}

		public String emit() {
			return "Mouse double-click " + getButtonString(button) + " at " + getPointString(this) + ".";
		}

		public Event mergeWithNext(Event next) {
			return null;
		}

		public int maxDelayToMergeNext() {
			return -1;
		}
	}

	private static class MouseMoveEvent extends MouseEvent {
		public MouseMoveEvent(int x, int y, int button, long millis) {
			super(x, y, button, millis);
			setDone(true);
		}

		public String emit() {
			return "Mouse move to " + getPointString(this) + ".";
		}

		public Event mergeWithNext(Event next) {
			if(next instanceof MouseMoveEvent) {
				MouseMoveEvent n = (MouseMoveEvent) next;
				Point[] pts = new Point[] { new Point(x, y), new Point(n.x, n.y) };
				MultiMouseMoveEvent mme = new MultiMouseMoveEvent(pts, millis);
				mme.setDone(true);
				return mme;
			}
			else if(next instanceof MultiMouseMoveEvent) {
				MultiMouseMoveEvent n = (MultiMouseMoveEvent) next;
				Point[] pts = new Point[n.pts.length + 1];
				pts[0] = new Point(x, y);
				System.arraycopy(n.pts, 0, pts, 1, n.pts.length);
				MultiMouseMoveEvent mme = new MultiMouseMoveEvent(pts, millis);
				mme.setDone(true);
				return mme;
			}
			return null; // TODO merge with mouseDrag
		}

		public int maxDelayToMergeNext() {
			return 500;
		}
	}

	private static class MouseDragEvent extends MouseEvent {

		public final Point[] pts;

		public MouseDragEvent(Point[] pts, int button, long millis) {
			super(pts[pts.length - 1].x, pts[pts.length - 1].y, button, millis);
			this.pts = pts;
		}

		public String emit() {
			return "Mouse drag " + getButtonString(button) + " along coordinates " + getPointListString(pts) + ".";
		}

		public Event mergeWithNext(Event next) {
//			if(next instanceof MouseDragEvent) { // TODO check button
//				MouseDragEvent n = (MouseDragEvent) next;
//				Point[] pts = new Point[this.pts.length + n.pts.length];
//				System.arraycopy(this.pts, 0, pts, 0, this.pts.length);
//				System.arraycopy(n.pts,    0, pts, this.pts.length, n.pts.length);
//				MouseDragEvent mde = new MouseDragEvent(pts, button, millis);
//				mde.setDone(true);
//				return mde;
//			}
			return null; // TODO merge with mouse move
		}

		public int maxDelayToMergeNext() {
			return 500;
		}
	}

	/*
	 * Reasoning:
	 * Subdivide key event chains into sections that start from a first key press (after all keys were released) until
	 * all keys are released again
	 *
	 * Analyze key events in-between:
	 *   (1) Single key was pressed (and obviously then released)
	 *       (1.1) It translates to unicode character(s)
	 *             (1.1.1) no other events (like mouse events) in-between: Emit "Enter '<unicode character(s)>'
	 *             (1.1.2) other events (e.g. a mouse click) was in-between: Emit "Press key <keystroke>"
	 *
	 *       (1.2) It does not translate to unicode character(s)
	 *             (1.1.1) no other events (like mouse events) in-between: Emit "Enter <keystroke>"
	 *             (1.1.2) other events (e.g. a mouse click) was in-between: Emit "Press key <keystroke>"
	 *   (2) Several keys were pressed (and then released)
	 *       (2.1a) More than one key was not a modifier: throw an exception (or ignore, or whatever)
	 *       (2.1b) Modifiers must come first, then the other key, otherwise throw an exception
	 *       (2.2) It translates to unicode character(s)
	 *             (1.1.1) no other events (like mouse events) in-between: Emit "Enter '<unicode character(s)>'
	 *             (1.1.2) other events (e.g. a mouse click) was in-between: Emit "Press key <keystroke>"
	 *
	 *       (2.3) It does not translate to unicode character(s)
	 *             (1.1.1) no other events (like mouse events) in-between: Emit "Enter <keystroke>"
	 *             (1.1.2) other events (e.g. a mouse click) was in-between: Emit "Press key <keystroke>"
	 *
	 *
	 * New:
	 *
	 * Still subdivide event chains into sections that start from a first key press (after all keys were released) until
	 * all keys are released again.
	 *
	 * Check key presses in-between: If it is only modifier keys, check if some other event like a mouse event
	 * happens in-between. If yes, translate into key-press and key-release, otherwise use 'enter <keystroke>'
	 *
	 * If there is another key involved, scan for key presses. Whenever a non-modifier key is pressed, emit.
	 * If no event is between key press and key release, emit 'Enter', otherwise emit 'Press' and 'Release'.
	 *
	 * If keycode/scancode translate into valid unicode chars, emit 'Enter text', otherwise 'Enter keystroke'.
	 *
	 *
	 *
	 */
	private static abstract class KeyEvent extends Event {
		public final int keycode;
		public final int modifiers;
		public final char[] unicode;

		public final int nKeysDown;


		public KeyEvent(int keycode, char[] unicode, int modifiers, int nKeysDown, long millis) {
			super(millis);
			this.keycode = keycode;
			this.unicode = unicode;
			this.modifiers = modifiers;
			this.nKeysDown = nKeysDown;
		}

		public boolean isModifier() {
			return nlScript.screenplay.KeyboardHook.GlobalKeyEvent.isModifier(keycode);
		}

		public KeyStroke getKeyStroke() {
			return nlScript.screenplay.KeyboardHook.GlobalKeyEvent.getKeyStroke(keycode, modifiers);
		}
	}

	private static class KeyPressEvent extends KeyEvent {
		public KeyPressEvent(int keycode, char[] unicode, int modifiers, int nKeysDown, long millis) {
			super(keycode, unicode, modifiers, nKeysDown, millis);
		}

		public String emit() {
			return "Press key <" + getKeyStroke() + ">.";
		}

		public Event mergeWithNext(Event next) {
			return null;
		}

		public int maxDelayToMergeNext() {
			return -1;
		}
	}

	private static class KeyReleaseEvent extends KeyEvent {
		public KeyReleaseEvent(int keycode, char[] unicode, int modifiers, int nKeysDown, long millis) {
			super(keycode, unicode, modifiers, nKeysDown, millis);
		}

		public String emit() {
			return "Release key <" + getKeyStroke() + ">.";
		}

		public Event mergeWithNext(Event next) {
			return null;
		}

		public int maxDelayToMergeNext() {
			return -1;
		}
	}

	private static abstract class EnterKeyEvent extends KeyEvent {
		public EnterKeyEvent(long millis) {
			super(-1, new char[0], 0, 0, millis);
		}
	}

	private static class EnterKeyStrokeEvent extends EnterKeyEvent {

		private final KeyStroke keyStroke;

		public EnterKeyStrokeEvent(KeyStroke keyStroke, long millis) {
			super(millis);
			this.keyStroke = keyStroke;
		}

		public String emit() {
			return "Enter " + this + ".";
		}

		public String toString() {
			return "<" + keyStroke.toString().replace("pressed ", "").replace("pressed", "") + ">";
		}

		public Event mergeWithNext(Event next) {
			if(next instanceof EnterKeyStrokeEvent) {
				Event e = new MultiEnterEvent(new EnterKeyEvent[] {this, (EnterKeyEvent) next}, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof EnterTextEvent) {
				MultiEnterEvent mee = new MultiEnterEvent(new EnterKeyEvent[] {this, (EnterKeyEvent) next}, millis);
				mee.setDone(true);
				return mee;
			}
			else if(next instanceof MultiEnterEvent) {
				MultiEnterEvent mee = (MultiEnterEvent) next;
				EnterKeyEvent[] events = new EnterKeyEvent[mee.events.length + 1];
				events[0] = this;
				System.arraycopy(mee.events, 0, events, 1, mee.events.length);
				Event e = new MultiEnterEvent(events, millis);
				e.setDone(true);
				return e;
			}
			return null;
		}

		public int maxDelayToMergeNext() {
			return 2000;
		}
	}

	private static class EnterTextEvent extends EnterKeyEvent {

		private final String text;

		public EnterTextEvent(String s, long millis) {
			super(millis);
			this.text = s;
		}

		public String toString() {
			return "'" + text + "'";
		}

		public String emit() {
			return "Enter '" + text + "'.";
		}

		public Event mergeWithNext(Event next) {
			if(next instanceof EnterKeyStrokeEvent) {
				Event e = new MultiEnterEvent(new EnterKeyEvent[] {this, (EnterKeyEvent) next}, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof EnterTextEvent) {
				Event e = new EnterTextEvent(this.text + ((EnterTextEvent) next).text, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof MultiEnterEvent) {
				MultiEnterEvent mee = (MultiEnterEvent) next;
				EnterKeyEvent[] events = new EnterKeyEvent[mee.events.length + 1];
				events[0] = this;
				System.arraycopy(mee.events, 0, events, 1, mee.events.length);
				Event e =  new MultiEnterEvent(events, millis);
				e.setDone(true);
				return e;
			}
			return null;
		}

		public int maxDelayToMergeNext() {
			return 2000;
		}
	}

	private static class MultiEnterEvent extends EnterKeyEvent {

		private final EnterKeyEvent[] events;

		public MultiEnterEvent(EnterKeyEvent[] events, long millis) {
			super(millis);
			this.events = events;
		}

		public String emit() {
			return "Enter " + Arrays.stream(events).map(Object::toString).collect(Collectors.joining(", ")) + ".";
		}

		public Event mergeWithNext(Event next) {
			if(next instanceof EnterKeyStrokeEvent) {
				EnterKeyStrokeEvent mee = (EnterKeyStrokeEvent) next;
				EnterKeyEvent[] events = new EnterKeyEvent[this.events.length + 1];
				System.arraycopy(this.events, 0, events, 0, this.events.length);
				events[events.length - 1] = mee;
				Event e = new MultiEnterEvent(events, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof EnterTextEvent) {
				EnterTextEvent mee = (EnterTextEvent) next;
				EnterKeyEvent last = events[events.length - 1];
				if(last instanceof EnterTextEvent) {
					events[events.length - 1] = new EnterTextEvent(((EnterTextEvent) last).text + mee.text, millis);
					setDone(true);
					return this;
				}

				EnterKeyEvent[] events = new EnterKeyEvent[this.events.length + 1];
				System.arraycopy(this.events, 0, events, 0, this.events.length);
				events[events.length - 1] = mee;
				Event e = new MultiEnterEvent(events, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof MultiEnterEvent) {
				MultiEnterEvent mee = (MultiEnterEvent) next;
				EnterKeyEvent[] events = new EnterKeyEvent[this.events.length + mee.events.length];
				System.arraycopy(this.events, 0, events, 0, this.events.length);
				System.arraycopy(mee.events, 0, events, this.events.length, mee.events.length);
				Event e = new MultiEnterEvent(events, millis);
				e.setDone(true);
				return e;
			}
			return null;
		}

		public int maxDelayToMergeNext() {
			return 2000;
		}
	}

//	private static class EnterKeyEvent extends Event {
//
//		private final String unicode;
//		private final KeyStroke keyStroke;
//
//		public EnterKeyEvent(String s, long millis) {
//			super(millis);
//			this.unicode = s;
//			this.keyStroke = null;
//		}
//
//		public EnterKeyEvent(KeyStroke keyStroke, long millis) {
//			super(millis);
//			this.unicode = null;
//			this.keyStroke = keyStroke;
//		}
//
//		public String emit() {
//			return "Enter key " + (unicode != null
//					? "'" + unicode + "'."
//					: "<" + keyStroke + ">.");
//		}
//
//		public Event mergeWithNext(Event next) {
//			if(next instanceof EnterKeyEvent) {
//				EnterKeyEvent n = (EnterKeyEvent) next;
//
//			}
//			return null;
//		}
//	}

//	private static class PressKeyEvent extends Event {
//
//		private final KeyStroke keyStroke;
//
//		public PressKeyEvent(KeyStroke keyStroke, long millis) {
//			super(millis);
//			this.keyStroke = keyStroke;
//		}
//
//		public String emit() {
//			return "Press key <" + keyStroke + ">.";
//		}
//	}
//
//	private static class ReleaseKeyEvent extends Event {
//
//		private final KeyStroke keyStroke;
//
//		public ReleaseKeyEvent(KeyStroke keyStroke, long millis) {
//			super(millis);
//			this.keyStroke = keyStroke;
//		}
//
//		public String emit() {
//			return "Release key <" + keyStroke + ">.";
//		}
//	}

	private static class MultiMouseMoveEvent extends MouseEvent {

		public final Point[] pts;

		public MultiMouseMoveEvent(Point[] pts, long millis) {
			super(pts[pts.length - 1].x, pts[pts.length - 1].y, -1 , millis);
			this.pts = pts;
		}

		public String emit() {
			// return "Mouse move along coordinates " + getPointListString(pts) + ".";
			return "Mouse move to " + getPointString(pts[pts.length - 1]) + ".";
		}

		@Override
		public Event mergeWithNext(Event next) {
			if(next instanceof MouseMoveEvent) {
				MouseMoveEvent mme = (MouseMoveEvent) next;
				Point[] newpts = new Point[pts.length + 1];
				System.arraycopy(pts, 0, newpts, 0, pts.length);
				newpts[pts.length] = new Point(mme.x, mme.y);
				Event e = new MultiMouseMoveEvent(newpts, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof MultiMouseMoveEvent) {
				MultiMouseMoveEvent mme = (MultiMouseMoveEvent) next;
				Point[] newpts = new Point[pts.length + mme.pts.length];
				System.arraycopy(pts, 0, newpts, 0, pts.length);
				System.arraycopy(mme.pts, 0, newpts, pts.length, mme.pts.length);
				Event e = new MultiMouseMoveEvent(newpts, millis);
				e.setDone(true);
				return e;
			}
			else if(next instanceof MouseClickEvent || next instanceof MousePressEvent || next instanceof MouseDragEvent) {
				// ignore the move, it's done automatically
				return next;
			}
			return null;
		}

		public int maxDelayToMergeNext() {
			return 2500;
		}
	}

	private static class MouseWheelEvent extends MouseEvent {
		private final int ticks;
		public MouseWheelEvent(int x, int y, int ticks, long millis) {
			super(x, y, MouseHook.GlobalMouseEvent.MIDDLE, millis);
			this.ticks = ticks;
			setDone(true);
		}

		public String emit() {
			return "Mouse scroll by " + ticks + " tick(s).";
		}

		public Event mergeWithNext(Event next) {
			if(next instanceof MouseWheelEvent) {
				MouseWheelEvent n = (MouseWheelEvent) next;
				if(n.x == x && n.y == y) {
					Event e = new MouseWheelEvent(x, y, ticks + n.ticks, millis);
					e.setDone(true);
					return e;
				}
			}
			return null;
		}

		public int maxDelayToMergeNext() {
			return 1000;
		}
	}

	private static class WindowOpenedEvent extends Event {
		private final WinDef.HWND window;
		public WindowOpenedEvent(WinDef.HWND window, long millis) {
			super(millis);
			this.window = window;
			setDone(true);
		}

		public String emit() {
			String title = WindowUtils.getWindowTitle(window);
			Rectangle r = WindowUtils.getWindowPosition(window);
			return
					"Wait for window '" + title + "'.\n" +
					"Position the window '" + title + "' " + r.width + " x " + r.height + " at (" + r.x + ", " + r.y + ").";
		}

		public Event mergeWithNext(Event next) {
			return null;
		}

		public int maxDelayToMergeNext() {
			return -1;
		}
	}

	private final AtomicBoolean modified = new AtomicBoolean(false);

	private int recordedModifierState = 0;

	private synchronized void onNewEvent() {
		List<Event> eventList;
		// work on a copy to not interrupt events
		synchronized (this.eventList) {
			eventList = new ArrayList<>(this.eventList);
			this.eventList.clear();
		}
		modified.set(true);
		int to = eventList.size() - 1;
		Event event = eventList.get(to);
		int from;

		if (event instanceof MouseReleaseEvent) {
			boolean found = false;
			for (from = to - 1; from >= 0; from--) {
				Event current = eventList.get(from);
				if (current instanceof MousePressEvent) {
					found = true;
					break;
				}
			}
			if(!found)
				throw new RuntimeException("Could not find mouse press preceding a mouse release");

			List<Event> sl = eventList.subList(from, to + 1);
			analyzeFromMousePressToRelease(sl);

			// TODO need to check for double-click here
//			for(Event e : sl)
//				System.out.println(e.emit());
//			System.out.println();
		}

		else if(event instanceof KeyReleaseEvent) {
			if(((KeyReleaseEvent) event).nKeysDown != 0) {
				synchronized (this.eventList) {
					this.eventList.addAll(0, eventList);
				}
				return;
			}

			// go back to find the first key press after all keys were released
			from = -1;
			for(int idx = to - 1; idx >= 0; idx--) {
				Event current = eventList.get(idx);
				if(current instanceof KeyPressEvent) {
					from = idx;
				}
				if(current instanceof KeyEvent) {
					if (((KeyEvent) current).nKeysDown == 0) {
						break;
					}
				}
			}
			if(from == -1)
				throw new RuntimeException("Could not find key press preceding a key release");
			List<Event> sl = eventList.subList(from, to + 1);
			analyzeFromAllKeysDownToAllKeysDown(sl);
			System.out.println();
		}

//		else if(event instanceof KeyPressEvent) {
//			KeyPressEvent ke = (KeyPressEvent) event;
//			if(!ke.isModifier()) {
//				char[] unicode = ke.unicode;
//				Event newEv = (unicode.length > 0 && ke.keycode != Win32VK.VK_RETURN.code)
//						? new EnterTextEvent(new String(unicode), ke.millis)
//						: new EnterKeyStrokeEvent(ke.getKeyStroke(), ke.millis);
//				// replace the KeyPressEvent with the EnterEvent
//				eventList.set(to, newEv);
//			}
//		}

		int i = 0;
		while(i < eventList.size() - 1) {
			Event e = eventList.get(i);
			if(!e.isDone()) {
				System.out.println("Not done yet:");
				System.out.println(e.emit());
				break;
			}
			Event n = eventList.get(i + 1);
			Event m = e.mergeWithNext(n);
			if(m == null) {
				eventList.remove(i); // don't increment i; since we remove the item at i, i points anyway to the next item
				record(e);
			}
			else {
				eventList.remove(i + 1);
				eventList.set(i, m); // don't emit, and don't increment i, because next we want to look at the new merged event
			}
		}

		// process the last one:
		if(eventList.size() == 1) {
			// first check if we need to wait
			Event last = eventList.get(0);
			int delay = last.maxDelayToMergeNext();
			if(delay <= 0) {
				eventList.remove(0);
				record(last);
			}
			else if (Recorder.this.eventList.isEmpty()) { // if eventList is not empty, there are anyway new events in the pipeline
				emitLast.push();
			}
		}

		// copy the changes back
		synchronized (this.eventList) {
			this.eventList.addAll(0, eventList);
		}
	}

	private void record(Event e) {
		if(ignoreEvents)
			return;
		String emission = e.emit();
		SwingUtilities.invokeLater(() -> {
			String text = editor.getText();
			if(!text.isEmpty() && !text.endsWith("\n"))
				text += "\n";
			text += (emission + "\n");
			editor.getTextArea().setText(text);
		});
	}

	private final EmitLast emitLast = new EmitLast();

	public class EmitLast extends Thread {

		private boolean shouldStop = false;

		private boolean pushed = false;

		public void push() {
			synchronized (EmitLast.this) {
				pushed = true;
				notify();
			}
		}

		public void consume() {
			while(!pushed) {
				synchronized (this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
			pushed = false;
			Event e = null;
			synchronized (eventList) {
				if(eventList.size() == 1)
					e = eventList.get(0);
			}
			if(e == null)
				return;
			try {
				Thread.sleep(e.maxDelayToMergeNext());
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			synchronized (eventList) {
				if (eventList.size() == 1) {
					Event last = eventList.get(0);
					if (System.currentTimeMillis() - last.millis > last.maxDelayToMergeNext()) {
						record(last);
						eventList.clear();
					}
				}
			}
		}

		public void run() {
			shouldStop = false;
			while(!shouldStop) {
				consume();
			}
		}

		public void cancel() {
			shouldStop = true;
			this.notify();
			try {
				this.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/*
	 *
	 * Still subdivide event chains into sections that start from a first key press (after all keys were released) until
	 * all keys are released again.
	 *
	 * The first element in the list is a key press, the last is a key release.
	 *
	 * Check key presses in-between: If it is only modifier keys, check if some other event like a mouse event
	 * happens in-between. If yes, translate into key-press and key-release, otherwise use 'enter <keystroke>'
	*
	* If there is another key involved, scan for key presses. Whenever a non-modifier key is pressed, emit.
	* If no event is between key press and key release, emit 'Enter', otherwise emit 'Press' and 'Release'.
	*
	* If keycode/scancode translate into valid unicode chars, emit 'Enter text', otherwise 'Enter keystroke'.
	*/

	private void analyzeFromAllKeysDownToAllKeysDown(List<Event> eventList) {
		System.out.println("analyzeFromAllKeysDownToAllKeysDown");
		boolean onlyModifiers = true;
		boolean sthElseInBetween = false;
		// -----
		// Check if other keys than modifiers were pressed,
		// and if there were any other events (mouse, window) in between
		for(int i = 0; i < eventList.size(); i++) {
			Event event = eventList.get(i);
			if(event instanceof KeyEvent) {
				if(event instanceof KeyPressEvent) {
					if(!((KeyEvent)event).isModifier())
						onlyModifiers = false;
				}
			}
			else {
				sthElseInBetween = true;
				// MouseEvent
				// WindowEvent
			}
		}

		MouseMoveEvent mme = null;
		Point currentCursorLoc = MouseInfo.getPointerInfo().getLocation();
		if(!currentCursorLoc.equals(lastRecordedMouseLocation)) {
			mme = new MouseMoveEvent(currentCursorLoc.x, currentCursorLoc.y, -1, System.currentTimeMillis());
			lastRecordedMouseLocation.setLocation(currentCursorLoc);
		}

		// -----
		// First case: just modifiers, nothing in between
		if(onlyModifiers && !sthElseInBetween) {
			// search for the last press event
			KeyEvent lastPress = null;
			for(int i = eventList.size() - 1; i >= 0; i--) {
				Event e = eventList.get(i);
				if (e instanceof KeyPressEvent) {
					lastPress = (KeyEvent) e;
					break;
				}
			}
			eventList.clear();
			KeyStroke stroke = lastPress.getKeyStroke();
			Event e = new EnterKeyStrokeEvent(stroke, lastPress.millis);
			e.setDone(true);
			eventList.add(e);
		}

		// -----
		// Second cse: just modifiers, with non-KeyEvents in between
		else if(onlyModifiers && sthElseInBetween) {
			List<Event> newEvents = new ArrayList<>();
			KeyEvent lastPress = null;
			for(int i = 0; i < eventList.size(); i++) {
				Event e = eventList.get(i);
				if (e instanceof KeyPressEvent)
					lastPress = (KeyEvent) e;
				else if (e instanceof KeyReleaseEvent) {
					e.setDone(true);
					newEvents.add(e);
				}
				else if (!(e instanceof KeyEvent)) {
					if(lastPress != null) {
						lastPress.setDone(true);
						newEvents.add(lastPress);
					}
					newEvents.add(e);
					lastPress = null;
				}
			}
			eventList.clear();
			eventList.addAll(newEvents);
		}

		// -----
		// Third case: not just modifiers, only KeyEvents
		// 	* If there is another key involved, scan for key presses. Whenever a non-modifier key is pressed, emit.
		//	* If no event is between key press and key release, emit 'Enter', otherwise emit 'Press' and 'Release'.
		//	*
		//	* If keycode/scancode translate into valid unicode chars, emit 'Enter text', otherwise 'Enter keystroke'.
		else if (!onlyModifiers && !sthElseInBetween) {
			List<Event> newEvents = new ArrayList<>();
			for(int i = 0; i < eventList.size(); i++) {
				Event e = eventList.get(i);
				if (e instanceof KeyPressEvent) {
					KeyPressEvent pe = (KeyPressEvent) e;
					if(pe.isModifier())
						continue;
					char[] unicode = pe.unicode;
					boolean isPrintable = true;
					for(char c : unicode) {
						if(Character.isISOControl(c)) {
							isPrintable = false;
							break;
						}
					}
					Event newEv = (!KeyboardHook.GlobalKeyEvent.isControlDown(pe.modifiers) && unicode.length > 0 && isPrintable && pe.keycode != Win32VK.VK_RETURN.code) ?
							new EnterTextEvent(new String(unicode), e.millis) :
							new EnterKeyStrokeEvent(pe.getKeyStroke(), e.millis);
					newEv.setDone(true);
					newEvents.add(newEv);
				}
			}
			eventList.clear();
			eventList.addAll(newEvents);
		}

		// -----
		// Fourth case: not just modifiers, not only KeyEvents
		else if(!onlyModifiers && sthElseInBetween) {
			List<Event> newEvents = new ArrayList<>();
			for(int i = 0; i < eventList.size(); i++) {
				Event e = eventList.get(i);
				if (e instanceof KeyPressEvent) {
					e.setDone(true);
					newEvents.add(e);
				}
				else if (e instanceof KeyReleaseEvent) {
					e.setDone(true);
					newEvents.add(e);
				}
				else if (!(e instanceof KeyEvent)) {
					newEvents.add(e);
				}
			}
			eventList.clear();
			eventList.addAll(newEvents);
		}

		if(mme != null)
			eventList.add(0, mme);
	}

	private static void analyzeFromMousePressToRelease(List<Event> eventList) {
		System.out.println("analyzeFromMousePressToRelease");
		boolean movedInBetween = false;
		boolean sthElseInBetween = false;

		MousePressEvent press = (MousePressEvent) eventList.get(0);
		MouseReleaseEvent release = (MouseReleaseEvent) eventList.get(eventList.size() - 1);

		// -----
		// First check if there are move events between press and release,
		// and if there are other events (key, window) in between
		for(int i = 1; i < eventList.size() - 1; i++) {
			Event event = eventList.get(i);
			if(event instanceof MouseEvent) {
				if(event instanceof MouseMoveEvent) {
					MouseMoveEvent mme = (MouseMoveEvent) event;
					if(mme.x != press.x || mme.y != press.y)
						movedInBetween = true;
				}
				else
					throw new RuntimeException("Found " + event.getClass().getName() + " between press and release");
			}
			else {
				sthElseInBetween = true;
				// KeyEvent
				// WindowEvent
			}
		}



		// -----
		// First case: no movement between press and release
		if(!movedInBetween && !sthElseInBetween) {
			if(press.x == release.x && press.y == release.y) {
				if(press.button != release.button)
					throw new RuntimeException("Pressed " + getButtonString(press.button) + " but released " + getButtonString(release.button));
				// mouse release directly follows mouse press, and is at the same location:
				// it is a click
				eventList.clear();
				MouseClickEvent e = new MouseClickEvent(press.x, press.y, press.button, press.millis);
				e.setDone(true);
				eventList.add(e);
			} else {
				if(press.button != release.button)
					throw new RuntimeException("Pressed " + getButtonString(press.button) + " but released " + getButtonString(release.button));
				// release directly follows mouse press, but not at the same location:
				// it is a drag
				eventList.clear();

				MouseDragEvent e = new MouseDragEvent(
						new Point[] { new Point(press.x, press.y), new Point(release.x, release.y) },
						press.button, press.millis);
				e.setDone(true);
				eventList.add(e);
			}
		}

		// -----
		// Second case: Only mouse moves between press and release
		else if(movedInBetween && !sthElseInBetween) {
			// just moves between press and release:
			// it's a drag
			// find out how many moves there are
			Point[] pts = interpolatedPointsFromMouseMoveEvents(eventList);
			eventList.clear();
			MouseDragEvent e = new MouseDragEvent(pts, press.button, press.millis);
			e.setDone(true);
			eventList.add(e);
		}

		// -----
		// Third case: mouse moves and also other events (key, window) between press and release
		else if(sthElseInBetween) {
			List<Event> newList = new ArrayList<>();
			newList.add(press);
			List<Event> moveEvents = new ArrayList<>();
			for(int i = 1; i < eventList.size() - 1; i++) {
				Event ev = eventList.get(i);
				if(ev instanceof MouseMoveEvent)
					moveEvents.add(ev);
				else if(ev instanceof MouseEvent) { // press release scroll should all not appear here
					throw new RuntimeException("Found " + ev.getClass().getName() + " between mouse press and release.");
				}
				else { // no mouse event, sth else
					if(!moveEvents.isEmpty()) {
						// TODO prepend the last point of the previous move
						Point[] pts = interpolatedPointsFromMouseMoveEvents(moveEvents);
						MultiMouseMoveEvent mmme = new MultiMouseMoveEvent(pts, (moveEvents.get(0)).millis);
						mmme.setDone(true);
						newList.add(mmme);

						moveEvents.clear();
					}
					newList.add(ev);
				}
			}
			eventList.clear();
			eventList.addAll(newList);
		}
	}

	static Point[] interpolatedPointsFromMouseMoveEvents(List<Event> eventList) {
		int n = eventList.size();
		int nToKeep = (int) Math.round((n - 1.0) / 20) + 1;
		int inc = (int)Math.round((n - 1) / (nToKeep - 1.0));
		Point[] pts = new Point[nToKeep];
		for(int i = 0; i < nToKeep; i++) {// for now just take every 20th point TODO do something smarter
			int si = i == nToKeep - 1 ? n - 1 : i * inc;
			MouseEvent me = (MouseEvent) eventList.get(si);
			pts[i] = new Point(me.x, me.y);
			// TODO check that all of them have the same button (as the mouse press one at the beginning)
		}
		return pts;
	}


	private final List<Event> eventList = new ArrayList<>();

	private void addEvent(Event e) {
		synchronized (eventList) {
			eventList.add(e);
			System.out.println("Event added " + Thread.currentThread().getName());
			printEventList();
		}
	}

	private void printEventList() {
		System.out.println("EventList:");
		for(Event e : eventList)
			System.out.println(e.emit());
	}


	public static void main(String[] args) {
		int n = 18;
		int every = 5;

		int nToKeep = (int) Math.round((n - 1.0) / every) + 1;
		int inc = (int)Math.round((n - 1) / (nToKeep - 1.0));
		int rem = (n - 1) % inc;

		System.out.println("nToKeep = " + nToKeep);
		System.out.println("rem = " + rem);
		System.out.println("inc = " + inc);
	}

	// mouse drag should not merge with next (because a mouse drag ends with a release)
}
