package nlScript.screenplay;

import com.sun.jna.platform.win32.Win32VK;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


/**
 * Maps Windows key codes to Java key codes
 */
public class KeyCodeMap {

	public static int getJavaKeyCode(int winKeyCode) {
		return intance.map[winKeyCode];
	}

	private static KeyCodeMap intance = new KeyCodeMap();

	private int[] map = new int[256];

	private KeyCodeMap() {
		map[Win32VK.VK_UNDEFINED.code] = KeyEvent.VK_UNDEFINED;
		map[Win32VK.VK_LBUTTON.code] = MouseEvent.BUTTON1;
		map[Win32VK.VK_RBUTTON.code] = MouseEvent.BUTTON3;
		map[Win32VK.VK_CANCEL.code]  = KeyEvent.VK_CANCEL;
		map[Win32VK.VK_MBUTTON.code] = MouseEvent.BUTTON2;

		map[Win32VK.VK_XBUTTON1.code] = -1;
		map[Win32VK.VK_XBUTTON2.code] = -1;
		map[Win32VK.VK_RESERVED_07.code] = -1;
		map[Win32VK.VK_BACK.code] = KeyEvent.VK_BACK_SPACE;
		map[Win32VK.VK_TAB.code] = KeyEvent.VK_TAB;

		map[Win32VK.VK_RESERVED_0A.code] = -1;
		map[Win32VK.VK_RESERVED_0B.code] = -1;

		map[Win32VK.VK_CLEAR.code] = KeyEvent.VK_CLEAR;
		map[Win32VK.VK_RETURN.code] = KeyEvent.VK_ENTER;

		map[Win32VK.VK_UNASSIGNED_0E.code] = -1;
		map[Win32VK.VK_UNASSIGNED_0F.code] = -1;

		map[Win32VK.VK_SHIFT.code] = KeyEvent.VK_SHIFT;
		map[Win32VK.VK_CONTROL.code] = KeyEvent.VK_CONTROL;
		map[Win32VK.VK_MENU.code] = KeyEvent.VK_ALT;
		map[Win32VK.VK_PAUSE.code] = KeyEvent.VK_PAUSE;
		map[Win32VK.VK_CAPITAL.code] = KeyEvent.VK_CAPS_LOCK;
		map[Win32VK.VK_KANA.code] = KeyEvent.VK_KANA;

		map[Win32VK.VK_UNASSIGNED_16.code] = -1;

		map[Win32VK.VK_JUNJA.code] = -1;
		map[Win32VK.VK_FINAL.code] = KeyEvent.VK_FINAL;
		map[Win32VK.VK_KANJI.code] = KeyEvent.VK_KANJI;

		map[Win32VK.VK_UNASSIGNED_1A.code] = -1;
		map[Win32VK.VK_ESCAPE.code] = KeyEvent.VK_ESCAPE;
		map[Win32VK.VK_CONVERT.code] = KeyEvent.VK_CONVERT;
		map[Win32VK.VK_NONCONVERT.code] = KeyEvent.VK_NONCONVERT;
		map[Win32VK.VK_ACCEPT.code] = KeyEvent.VK_ACCEPT;
		map[Win32VK.VK_MODECHANGE.code] = KeyEvent.VK_MODECHANGE;

		map[Win32VK.VK_SPACE.code] = KeyEvent.VK_SPACE;
		map[Win32VK.VK_PRIOR.code] = KeyEvent.VK_PAGE_UP;
		map[Win32VK.VK_NEXT.code] = KeyEvent.VK_PAGE_DOWN;
		map[Win32VK.VK_END.code] = KeyEvent.VK_END;
		map[Win32VK.VK_HOME.code] = KeyEvent.VK_HOME;
		map[Win32VK.VK_LEFT.code] = KeyEvent.VK_LEFT;
		map[Win32VK.VK_UP.code] = KeyEvent.VK_UP;
		map[Win32VK.VK_RIGHT.code] = KeyEvent.VK_RIGHT;
		map[Win32VK.VK_DOWN.code] = KeyEvent.VK_DOWN;
		map[Win32VK.VK_SELECT.code] = -1;
		map[Win32VK.VK_PRINT.code] = -1;
		map[Win32VK.VK_EXECUTE.code] = -1;
		map[Win32VK.VK_SNAPSHOT.code] = KeyEvent.VK_PRINTSCREEN;
		map[Win32VK.VK_INSERT.code] = KeyEvent.VK_INSERT;
		map[Win32VK.VK_DELETE.code] = KeyEvent.VK_DELETE;
		map[Win32VK.VK_HELP.code] = KeyEvent.VK_HELP;

		map[Win32VK.VK_0.code] = KeyEvent.VK_0;
		map[Win32VK.VK_1.code] = KeyEvent.VK_1;
		map[Win32VK.VK_2.code] = KeyEvent.VK_2;
		map[Win32VK.VK_3.code] = KeyEvent.VK_3;
		map[Win32VK.VK_4.code] = KeyEvent.VK_4;
		map[Win32VK.VK_5.code] = KeyEvent.VK_5;
		map[Win32VK.VK_6.code] = KeyEvent.VK_6;
		map[Win32VK.VK_7.code] = KeyEvent.VK_7;
		map[Win32VK.VK_8.code] = KeyEvent.VK_8;
		map[Win32VK.VK_9.code] = KeyEvent.VK_9;

		map[Win32VK.VK_UNASSIGNED_3A.code] = -1;
		map[Win32VK.VK_UNASSIGNED_3B.code] = -1;
		map[Win32VK.VK_UNASSIGNED_3C.code] = -1;
		map[Win32VK.VK_UNASSIGNED_3D.code] = -1;
		map[Win32VK.VK_UNASSIGNED_3E.code] = -1;
		map[Win32VK.VK_UNASSIGNED_3F.code] = -1;
		map[Win32VK.VK_UNASSIGNED_40.code] = -1;

		map[Win32VK.VK_A.code] = KeyEvent.VK_A;
		map[Win32VK.VK_B.code] = KeyEvent.VK_B;
		map[Win32VK.VK_C.code] = KeyEvent.VK_C;
		map[Win32VK.VK_D.code] = KeyEvent.VK_D;
		map[Win32VK.VK_E.code] = KeyEvent.VK_E;
		map[Win32VK.VK_F.code] = KeyEvent.VK_F;
		map[Win32VK.VK_G.code] = KeyEvent.VK_G;
		map[Win32VK.VK_H.code] = KeyEvent.VK_H;
		map[Win32VK.VK_I.code] = KeyEvent.VK_I;
		map[Win32VK.VK_J.code] = KeyEvent.VK_J;
		map[Win32VK.VK_K.code] = KeyEvent.VK_K;
		map[Win32VK.VK_L.code] = KeyEvent.VK_L;
		map[Win32VK.VK_M.code] = KeyEvent.VK_M;
		map[Win32VK.VK_N.code] = KeyEvent.VK_N;
		map[Win32VK.VK_O.code] = KeyEvent.VK_O;
		map[Win32VK.VK_P.code] = KeyEvent.VK_P;
		map[Win32VK.VK_Q.code] = KeyEvent.VK_Q;
		map[Win32VK.VK_R.code] = KeyEvent.VK_R;
		map[Win32VK.VK_S.code] = KeyEvent.VK_S;
		map[Win32VK.VK_T.code] = KeyEvent.VK_T;
		map[Win32VK.VK_U.code] = KeyEvent.VK_U;
		map[Win32VK.VK_V.code] = KeyEvent.VK_V;
		map[Win32VK.VK_W.code] = KeyEvent.VK_W;
		map[Win32VK.VK_X.code] = KeyEvent.VK_X;
		map[Win32VK.VK_Y.code] = KeyEvent.VK_Y;
		map[Win32VK.VK_Z.code] = KeyEvent.VK_Z;

		map[Win32VK.VK_LWIN.code] = KeyEvent.VK_WINDOWS;
		map[Win32VK.VK_RWIN.code] = KeyEvent.VK_WINDOWS;
		map[Win32VK.VK_APPS.code] = -1;

		map[Win32VK.VK_RESERVED_5E.code] = -1;
		map[Win32VK.VK_SLEEP.code] = -1;

		map[Win32VK.VK_NUMPAD0.code] = KeyEvent.VK_NUMPAD0;
		map[Win32VK.VK_NUMPAD1.code] = KeyEvent.VK_NUMPAD1;
		map[Win32VK.VK_NUMPAD2.code] = KeyEvent.VK_NUMPAD2;
		map[Win32VK.VK_NUMPAD3.code] = KeyEvent.VK_NUMPAD3;
		map[Win32VK.VK_NUMPAD4.code] = KeyEvent.VK_NUMPAD4;
		map[Win32VK.VK_NUMPAD5.code] = KeyEvent.VK_NUMPAD5;
		map[Win32VK.VK_NUMPAD6.code] = KeyEvent.VK_NUMPAD6;
		map[Win32VK.VK_NUMPAD7.code] = KeyEvent.VK_NUMPAD7;
		map[Win32VK.VK_NUMPAD8.code] = KeyEvent.VK_NUMPAD8;
		map[Win32VK.VK_NUMPAD9.code] = KeyEvent.VK_NUMPAD9;

		map[Win32VK.VK_MULTIPLY.code] = KeyEvent.VK_MULTIPLY;
		map[Win32VK.VK_ADD.code] = KeyEvent.VK_ADD;
		map[Win32VK.VK_SEPARATOR.code] = KeyEvent.VK_SEPARATOR;
		map[Win32VK.VK_SUBTRACT.code] = KeyEvent.VK_SUBTRACT;
		map[Win32VK.VK_DECIMAL.code] = KeyEvent.VK_DECIMAL;
		map[Win32VK.VK_DIVIDE.code] = KeyEvent.VK_DIVIDE;

		map[Win32VK.VK_F1.code] = KeyEvent.VK_F1;
		map[Win32VK.VK_F2.code] = KeyEvent.VK_F2;
		map[Win32VK.VK_F3.code] = KeyEvent.VK_F3;
		map[Win32VK.VK_F4.code] = KeyEvent.VK_F4;
		map[Win32VK.VK_F5.code] = KeyEvent.VK_F5;
		map[Win32VK.VK_F6.code] = KeyEvent.VK_F6;
		map[Win32VK.VK_F7.code] = KeyEvent.VK_F7;
		map[Win32VK.VK_F8.code] = KeyEvent.VK_F8;
		map[Win32VK.VK_F9.code] = KeyEvent.VK_F9;
		map[Win32VK.VK_F10.code] = KeyEvent.VK_F10;
		map[Win32VK.VK_F11.code] = KeyEvent.VK_F11;
		map[Win32VK.VK_F12.code] = KeyEvent.VK_F12;
		map[Win32VK.VK_F13.code] = KeyEvent.VK_F13;
		map[Win32VK.VK_F14.code] = KeyEvent.VK_F14;
		map[Win32VK.VK_F15.code] = KeyEvent.VK_F15;
		map[Win32VK.VK_F16.code] = KeyEvent.VK_F16;
		map[Win32VK.VK_F17.code] = KeyEvent.VK_F17;
		map[Win32VK.VK_F18.code] = KeyEvent.VK_F18;
		map[Win32VK.VK_F19.code] = KeyEvent.VK_F19;
		map[Win32VK.VK_F20.code] = KeyEvent.VK_F20;
		map[Win32VK.VK_F21.code] = KeyEvent.VK_F21;
		map[Win32VK.VK_F22.code] = KeyEvent.VK_F22;
		map[Win32VK.VK_F23.code] = KeyEvent.VK_F23;
		map[Win32VK.VK_F24.code] = KeyEvent.VK_F24;

		map[Win32VK.VK_NAVIGATION_VIEW.code] = -1;
		map[Win32VK.VK_NAVIGATION_MENU.code] = -1;
		map[Win32VK.VK_NAVIGATION_UP.code] = -1;
		map[Win32VK.VK_NAVIGATION_DOWN.code] = -1;
		map[Win32VK.VK_NAVIGATION_LEFT.code] = -1;
		map[Win32VK.VK_NAVIGATION_RIGHT.code] = -1;
		map[Win32VK.VK_NAVIGATION_ACCEPT.code] = -1;
		map[Win32VK.VK_NAVIGATION_CANCEL.code] = -1;

		map[Win32VK.VK_NUMLOCK.code] = KeyEvent.VK_NUM_LOCK;
		map[Win32VK.VK_SCROLL.code] = KeyEvent.VK_SCROLL_LOCK;

		map[Win32VK.VK_LSHIFT.code] = KeyEvent.VK_SHIFT;
		map[Win32VK.VK_RSHIFT.code] = KeyEvent.VK_SHIFT;
		map[Win32VK.VK_LCONTROL.code] = KeyEvent.VK_CONTROL;
		map[Win32VK.VK_RCONTROL.code] = KeyEvent.VK_CONTROL;
		map[Win32VK.VK_LMENU.code] = KeyEvent.VK_ALT;
		map[Win32VK.VK_RMENU.code] = KeyEvent.VK_ALT_GRAPH;

		map[Win32VK.VK_BROWSER_BACK.code] = -1;
		map[Win32VK.VK_BROWSER_FORWARD.code] = -1;
		map[Win32VK.VK_BROWSER_REFRESH.code] = -1;
		map[Win32VK.VK_BROWSER_STOP.code] = -1;
		map[Win32VK.VK_BROWSER_SEARCH.code] = -1;
		map[Win32VK.VK_BROWSER_FAVORITES.code] = -1;
		map[Win32VK.VK_BROWSER_HOME.code] = -1;

		map[Win32VK.VK_VOLUME_MUTE.code] = -1;
		map[Win32VK.VK_VOLUME_DOWN.code] = -1;
		map[Win32VK.VK_VOLUME_UP.code] = -1;
		map[Win32VK.VK_MEDIA_NEXT_TRACK.code] = -1;
		map[Win32VK.VK_MEDIA_PREV_TRACK.code] = -1;
		map[Win32VK.VK_MEDIA_STOP.code] = -1;
		map[Win32VK.VK_MEDIA_PLAY_PAUSE.code] = -1;
		map[Win32VK.VK_LAUNCH_MAIL.code] = -1;
		map[Win32VK.VK_LAUNCH_MEDIA_SELECT.code] = -1;
		map[Win32VK.VK_LAUNCH_APP1.code] = -1;
		map[Win32VK.VK_LAUNCH_APP2.code] = -1;

		map[Win32VK.VK_RESERVED_B8.code] = -1;
		map[Win32VK.VK_RESERVED_B9.code] = -1;

		map[Win32VK.VK_OEM_1.code] = -1;
		map[Win32VK.VK_OEM_PLUS.code] = -1;
		map[Win32VK.VK_OEM_COMMA.code] = -1;
		map[Win32VK.VK_OEM_MINUS.code] = -1;
		map[Win32VK.VK_OEM_PERIOD.code] = -1;
		map[Win32VK.VK_OEM_2.code] = -1;
		map[Win32VK.VK_OEM_3.code] = -1;

		map[Win32VK.VK_RESERVED_C1.code] = -1;
		map[Win32VK.VK_RESERVED_C2.code] = -1;

		map[Win32VK.VK_GAMEPAD_A.code] = -1;
		map[Win32VK.VK_GAMEPAD_B.code] = -1;
		map[Win32VK.VK_GAMEPAD_X.code] = -1;
		map[Win32VK.VK_GAMEPAD_Y.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_SHOULDER.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_SHOULDER.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_TRIGGER.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_TRIGGER.code] = -1;
		map[Win32VK.VK_GAMEPAD_DPAD_UP.code] = -1;
		map[Win32VK.VK_GAMEPAD_DPAD_DOWN.code] = -1;
		map[Win32VK.VK_GAMEPAD_DPAD_LEFT.code] = -1;
		map[Win32VK.VK_GAMEPAD_DPAD_RIGHT.code] = -1;
		map[Win32VK.VK_GAMEPAD_MENU.code] = -1;
		map[Win32VK.VK_GAMEPAD_VIEW.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_THUMBSTICK_BUTTON.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_THUMBSTICK_BUTTON.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_THUMBSTICK_UP.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_THUMBSTICK_DOWN.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_THUMBSTICK_RIGHT.code] = -1;
		map[Win32VK.VK_GAMEPAD_LEFT_THUMBSTICK_LEFT.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_THUMBSTICK_UP.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_THUMBSTICK_DOWN.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_THUMBSTICK_RIGHT.code] = -1;
		map[Win32VK.VK_GAMEPAD_RIGHT_THUMBSTICK_LEFT.code] = -1;

		map[Win32VK.VK_OEM_4.code] = -1;
		map[Win32VK.VK_OEM_5.code] = -1;
		map[Win32VK.VK_OEM_6.code] = -1;
		map[Win32VK.VK_OEM_7.code] = -1;
		map[Win32VK.VK_OEM_8.code] = -1;

		map[Win32VK.VK_RESERVED_E0.code] = -1;

		map[Win32VK.VK_OEM_AX.code] = -1;
		map[Win32VK.VK_OEM_102.code] = -1;
		map[Win32VK.VK_ICO_HELP.code] = -1;
		map[Win32VK.VK_ICO_00.code] = -1;

		map[Win32VK.VK_PROCESSKEY.code] = -1;

		map[Win32VK.VK_ICO_CLEAR.code] = -1;

		map[Win32VK.VK_PACKET.code] = -1;

		map[Win32VK.VK_UNASSIGNED_E8.code] = -1;

		map[Win32VK.VK_OEM_RESET.code] = -1;
		map[Win32VK.VK_OEM_JUMP.code] = -1;
		map[Win32VK.VK_OEM_PA1.code] = -1;
		map[Win32VK.VK_OEM_PA2.code] = -1;
		map[Win32VK.VK_OEM_PA3.code] = -1;
		map[Win32VK.VK_OEM_WSCTRL.code] = -1;
		map[Win32VK.VK_OEM_CUSEL.code] = -1;
		map[Win32VK.VK_OEM_ATTN.code] = -1;
		map[Win32VK.VK_OEM_FINISH.code] = -1;
		map[Win32VK.VK_OEM_COPY.code] = -1;
		map[Win32VK.VK_OEM_AUTO.code] = -1;
		map[Win32VK.VK_OEM_ENLW.code] = -1;
		map[Win32VK.VK_OEM_BACKTAB.code] = -1;

		map[Win32VK.VK_ATTN.code] = -1;
		map[Win32VK.VK_CRSEL.code] = -1;
		map[Win32VK.VK_EXSEL.code] = -1;
		map[Win32VK.VK_EREOF.code] = -1;
		map[Win32VK.VK_PLAY.code] = -1;
		map[Win32VK.VK_ZOOM.code] = -1;
		map[Win32VK.VK_NONAME.code] = -1;
		map[Win32VK.VK_PA1.code] = -1;
		map[Win32VK.VK_OEM_CLEAR.code] = -1;

		map[Win32VK.VK_RESERVED_FF.code] = -1;
	}
}
