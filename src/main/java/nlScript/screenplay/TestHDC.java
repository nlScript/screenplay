package nlScript.screenplay;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import static com.sun.jna.platform.win32.WinUser.*;

public class TestHDC {

	public interface MyWinGDI extends Library {
		MyWinGDI INSTANCE = Native.load("Gdi32", MyWinGDI.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean Rectangle(WinDef.HDC hdc, int left, int top, int right, int bottom);

		WinDef.HBRUSH CreatePatternBrush(WinDef.HBITMAP hbm);

		WinDef.HBRUSH CreateSolidBrush(int color); // e.g. const COLORREF rgbRed   =  0x000000FF;

		int SetDCBrushColor(WinDef.HDC hdc, int color);

	}

	public interface MyUser32 extends Library {
		MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

		int FillRect(WinDef.HDC hDC, WinDef.RECT lprc, WinDef.HBRUSH hbr);
	}

	public interface MyMsimg32 extends Library {
		MyMsimg32 INSTANCE = Native.load("Msimg32", MyMsimg32.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean AlphaBlend(
					WinDef.HDC hdcDest,
					int xoriginDest,
					int yoriginDest,
					int wDest,
					int hDest,
					WinDef.HDC hdcSrc,
					int xoriginSrc,
					int yoriginSrc,
					int wSrc,
					int hSrc,
					WinUser.BLENDFUNCTION ftn
		);

		boolean TransparentBlt(
					HDC hdcDest,
					int xoriginDest,
					int yoriginDest,
					int wDest,
					int hDest,
					HDC hdcSrc,
					int xoriginSrc,
					int yoriginSrc,
					int wSrc,
					int hSrc,
					int crTransparent
		);
	}

	protected static WinDef.HDC bufferedImageToBitmap(BufferedImage buf) {
		// TODO: paint frame decoration if window is decorated
		GDI32 gdi = GDI32.INSTANCE;
		User32 user = User32.INSTANCE;

		WinDef.HDC screenDC = user.GetDC(null);
		WinNT.HANDLE oldBitmap = null;
		WinDef.HDC memDC = null;
		WinDef.HBITMAP hBitmap = null;
		Pointer pbits = null;
		Dimension bitmapSize;
		try {
			if (memDC == null) {
				memDC = gdi.CreateCompatibleDC(screenDC); // TODO delete it again
			}
			if (hBitmap == null) {
				WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
				bmi.bmiHeader.biWidth = buf.getWidth();
				bmi.bmiHeader.biHeight = buf.getHeight();
				bmi.bmiHeader.biPlanes = 1;
				bmi.bmiHeader.biBitCount = 32;
				bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
				bmi.bmiHeader.biSizeImage = buf.getWidth() * buf.getHeight() * 4;
				PointerByReference ppbits = new PointerByReference();
				hBitmap = gdi.CreateDIBSection(memDC, bmi,
						WinGDI.DIB_RGB_COLORS,
						ppbits, null, 0);
				pbits = ppbits.getValue();
				bitmapSize = new Dimension(buf.getWidth(), buf.getHeight());
			}
			oldBitmap = gdi.SelectObject(memDC, hBitmap);
			Raster raster = buf.getData();
			int[] pixel = new int[4];
			int[] bits = new int[buf.getWidth()];
			for (int row = 0; row < buf.getHeight(); row++) {
				for (int col = 0; col < buf.getWidth(); col++) {
					raster.getPixel(col, row, pixel);
					int alpha = (pixel[3] & 0xFF) << 24;
					int red = (pixel[2] & 0xFF);
					int green = (pixel[1] & 0xFF) << 8;
					int blue = (pixel[0] & 0xFF) << 16;
					bits[col] = alpha | red | green | blue;
				}
				int v = buf.getHeight() - row - 1;
				pbits.write(v * buf.getWidth() * 4, bits, 0, bits.length);
			}
		} finally {
			user.ReleaseDC(null, screenDC);
//			if (memDC != null && oldBitmap != null) {
//				gdi.SelectObject(memDC, oldBitmap);
//			}
		}
		return memDC;
	}

	public static void drawRectangle() {
		WinDef.HDC hdc = User32.INSTANCE.GetDC(null);

		MyWinGDI.INSTANCE.SetDCBrushColor(hdc, 0x00ff0000);
		MyWinGDI.INSTANCE.Rectangle(hdc, 1000, 1000, 1500, 1500);

		User32.INSTANCE.ReleaseDC(null, hdc);
	}

	// http://www.winprog.org/tutorial/bitmaps.html
	// com.sun.jna.platform.WindowUtils.paintDirect
	public static void test(BufferedImage buf, int x, int y) {
		WinDef.HDC hdc = User32.INSTANCE.GetDC(null);
		WinDef.HDC hdcMem = GDI32.INSTANCE.CreateCompatibleDC(hdc);

		// create the bitmap
		WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
		bmi.bmiHeader.biWidth = buf.getWidth();
		bmi.bmiHeader.biHeight = buf.getHeight();
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
		bmi.bmiHeader.biSizeImage = buf.getWidth() * buf.getHeight() * 4;
		bmi.bmiHeader.biXPelsPerMeter = 1000;
		bmi.bmiHeader.biYPelsPerMeter = 1000;
		bmi.bmiHeader.biClrUsed = 0;
		bmi.bmiHeader.biClrImportant = 0;
		PointerByReference ppbits = new PointerByReference();
		WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateDIBSection(hdcMem, bmi,
				WinGDI.DIB_RGB_COLORS,
				ppbits, null, 0);
		Pointer pbits = ppbits.getValue();

		WinNT.HANDLE hbmOld = GDI32.INSTANCE.SelectObject(hdcMem, hBitmap);


		// fill the bitmap
		Raster raster = buf.getData();
		int[] pixel = new int[4];
		int[] bits = new int[buf.getWidth()];
		for (int row = 0; row < buf.getHeight(); row++) {
			for (int col = 0; col < buf.getWidth(); col++) {
				raster.getPixel(col, row, pixel);
				int alpha = (pixel[3] & 0xFF) << 24;
				int red   = (pixel[2] & 0xFF);
				int green = (pixel[1] & 0xFF) << 8;
				int blue  = (pixel[0] & 0xFF) << 16;
				bits[col] = alpha | red | green | blue;
			}
			int v = buf.getHeight() - row - 1;
			pbits.write(v * buf.getWidth() * 4L, bits, 0, bits.length);
		}


		// GDI32.INSTANCE.BitBlt(hdc, x, y, buf.getWidth(), buf.getHeight(), hdcMem, 0, 0, GDI32.SRCCOPY);

		WinUser.BLENDFUNCTION fn = new WinUser.BLENDFUNCTION();
		fn.BlendOp = AC_SRC_OVER;
		fn.BlendFlags = 0;
		fn.SourceConstantAlpha = (byte) 127;
		fn.AlphaFormat = 0; // AC_SRC_ALPHA;
//		boolean success = MyMsimg32.INSTANCE.AlphaBlend(hdc, 0, 0, buf.getWidth(), buf.getHeight(), hdcMem, 0, 0, buf.getWidth(), buf.getHeight(), fn);
		RECT r = new RECT();
		r.left = -1920 + 10; r.top = 10; r.right = -10; r.bottom = 1200 - 10;
		r.left = 10; r.top = 10; r.right = 2560 - 10; r.bottom = 1440 - 10;
		r.left = 10; r.top = 10; r.right = 3840 - 10; r.bottom = 2160 - 10;
		boolean success = MyMsimg32.INSTANCE.TransparentBlt(hdc, r.left, r.top, r.right - r.left, r.bottom - r.top, hdcMem, 0, 0, buf.getWidth(), buf.getHeight(), 0);
		System.out.println("success = " + success);

		GDI32.INSTANCE.SelectObject(hdcMem, hbmOld);
		GDI32.INSTANCE.DeleteDC(hdcMem);
		User32.INSTANCE.ReleaseDC(null, hdc);
	}

	public static void main(String[] args) throws InterruptedException, AWTException {
//		drawRectangle();
//
//		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
//		BufferedImage bImg = image.getBufferedImage();
//
//		Robot robot = new Robot();
//		robot.mouseMove(1206, 1425);
//		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
//		robot.delay(100);
//		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
//		Thread.sleep(1000);
//
//		test(bImg, 1000, 1000);
//
//		Thread.sleep(2000);
	}

	public static void test3(String[] args) {
		HDC screen = User32.INSTANCE.GetDC(null);
		RECT r = new RECT();
		r.left = -1920 + 10; r.top = 10; r.right = -10; r.bottom = 1200 - 10;
		HBRUSH br = MyWinGDI.INSTANCE.CreateSolidBrush(0xff00);
		MyUser32.INSTANCE.FillRect(screen, r, br);
	}
}
