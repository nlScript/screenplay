package nlScript.screenplay;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import static com.sun.jna.platform.win32.WinUser.*;

public class BubbleScreenMessage extends ScreenMessage4 {
	private final BubblePanel bubblePanel;

	private final JLabel bubbleLabel;

	private static final int GAP = 10; // this is the gap between panel and bubble, while
	                            // padding is the gap between bubble and label, and
	                            // margin is ignored, since the bubble is manually positioned.
	                            // TODO use margin as the gap between panel and bubble (instead of GAP)

	private static class BubblePanel extends JPanel {

		private final JLabel bubbleLabel;

		private BubbleScreenMessage bubbleScreenMessage = null;

		public BubblePanel() {
			super();
			setOpaque(false);
			setLayout(new FlowLayout(FlowLayout.CENTER, GAP, GAP));
			bubbleLabel = new JLabel();
			add(bubbleLabel);
		}

		public void setBubbleScreenMessage(BubbleScreenMessage bubbleScreenMessage) {
			this.bubbleScreenMessage = bubbleScreenMessage;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if(bubbleScreenMessage == null)
				return;

			JLabel label = bubbleLabel;
			g.setColor(label.getBackground());

			Area bubble = null;
			switch(bubbleScreenMessage.correctedAlignment) {
				case TOP_LEFT:      bubble = makeBubbleTopLeft    (label.getWidth(), label.getHeight()); break;
				case TOP_CENTER:    bubble = makeBubbleTopLeft    (label.getWidth(), label.getHeight()); break;
				case TOP_RIGHT:     bubble = makeBubbleTopRight   (label.getWidth(), label.getHeight()); break;
				case CENTER_LEFT:   bubble = makeBubbleTopLeft    (label.getWidth(), label.getHeight()); break;
				case CENTER:        bubble = makeBubbleTopLeft    (label.getWidth(), label.getHeight()); break;
				case CENTER_RIGHT:  bubble = makeBubbleTopRight   (label.getWidth(), label.getHeight()); break;
				case BOTTOM_LEFT:   bubble = makeBubbleBottomLeft (label.getWidth(), label.getHeight()); break;
				case BOTTOM_CENTER: bubble = makeBubbleBottomLeft (label.getWidth(), label.getHeight()); break;
				case BOTTOM_RIGHT:  bubble = makeBubbleBottomRight(label.getWidth(), label.getHeight()); break;
			}

			((Graphics2D) g).fill(bubble);
			g.setColor(Color.BLACK);
			((Graphics2D) g).draw(bubble);
		}

		private static Area makeBubbleTopLeft(int labelW, int labelH) {
			int GAP2 = GAP + GAP;
			Path2D.Double path = new Path2D.Double();
			path.moveTo(GAP2, GAP);
			path.lineTo(0, 0);
			path.lineTo(GAP, GAP2);

			Area bubble = new Area(path);
			RoundRectangle2D.Double r = new RoundRectangle2D.Double(GAP, GAP, labelW, labelH, GAP, GAP);
			bubble.add(new Area(r));

			return bubble;
		}

		private static Area makeBubbleTopRight(int labelW, int labelH) {
			int GAP2 = GAP + GAP;
			Path2D.Double path = new Path2D.Double();
			path.moveTo(labelW, GAP);
			path.lineTo(labelW + GAP2, 0);
			path.lineTo(labelW + GAP, GAP2);

			Area bubble = new Area(path);
			RoundRectangle2D.Double r = new RoundRectangle2D.Double(GAP, GAP, labelW, labelH, GAP, GAP);
			bubble.add(new Area(r));

			return bubble;
		}

		private static Area makeBubbleBottomLeft(int labelW, int labelH) {
			int GAP2 = GAP + GAP;
			Path2D.Double path = new Path2D.Double();
			path.moveTo(GAP, labelH);
			path.lineTo(0, labelH + GAP2);
			path.lineTo(GAP2, labelH + GAP);

			Area bubble = new Area(path);
			RoundRectangle2D.Double r = new RoundRectangle2D.Double(GAP, GAP, labelW, labelH, GAP, GAP);
			bubble.add(new Area(r));

			return bubble;
		}

		private static Area makeBubbleBottomRight(int labelW, int labelH) {
			int GAP2 = GAP + GAP;
			Path2D.Double path = new Path2D.Double();
			path.moveTo(labelW + GAP, labelH);
			path.lineTo(labelW + GAP2, labelH + GAP2);
			path.lineTo(labelW, labelH + GAP);

			Area bubble = new Area(path);
			RoundRectangle2D.Double r = new RoundRectangle2D.Double(GAP, GAP, labelW, labelH, GAP, GAP);
			bubble.add(new Area(r));

			return bubble;
		}
	}

	public BubbleScreenMessage() {
		super(new BubblePanel());
		this.bubblePanel = (BubblePanel) super.component;
		this.bubbleLabel = bubblePanel.bubbleLabel;
		this.bubbleLabel.setOpaque(true);
		bubblePanel.setBubbleScreenMessage(this);
		frame.setBackground(new Color(0, 0, 0, 0));
		setAlignAt(AlignAt.CUSTOM);
		setAlignment(Alignment.TOP_LEFT);
		setMargin(0);
		setFontSize(24);
		setBackground(new Color(200, 200, 200, 100));
		setBackground(new Color(255, 234, 128, 255));
		setForeground(new Color(20, 20, 20));
		setPadding(10);
	}

	public ScreenMessage4 setBackground(Color color) {
		bubbleLabel.setBackground(color);
		return this;
	}

	public ScreenMessage4 setPadding(int top, int left, int bottom, int right) {
		bubbleLabel.setBorder(new EmptyBorder(new Insets(top, left, bottom, right)));
		return this;
	}

	public BubbleScreenMessage showMessage(String text, Icon icon) {
		bubbleLabel.setFont(bubblePanel.getFont());
		bubbleLabel.setText(text);
		bubbleLabel.setIcon(icon);
		bubbleLabel.setForeground(bubblePanel.getForeground());
		bubbleLabel.setOpaque(false);
		bubblePanel.add(bubbleLabel);
		frame.pack();
		updatePosition();

		if(!enabled)
			return this;

		Point loc = frame.getLocation();

		Dimension size = frame.getPreferredSize();
		int w = size.width;
		int h = size.height;
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.createGraphics();
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, w, h);
		bubblePanel.paint(g);
		g.dispose();

		drawImageOnScreen(bi, loc.x, loc.y);
//		ImagePlus imp = new ImagePlus("", bi);
//		imp.show();
//		imp.getWindow().setLocation(loc);

		// super.show();

//		WinDef.HWND thisWindow = WindowUtils.getHWnd(frame);
//		WinDef.HWND foreground = WindowUtils.getForegroundWindowHandle();
//
//		if(thisWindow.equals(foreground))
//			return this;
//
//		System.out.println("foreground window: " + foreground + " class: " + WindowUtils.getClassName(foreground) + " name: " + WindowUtils.getWindowTitle(foreground));
//		WinDef.RECT foregroundRect = new WinDef.RECT();
//		WindowUtils.MyUser32.INSTANCE.GetWindowRect(foreground, foregroundRect);
//		System.out.println("foregroundRect = " + foregroundRect);
//		Rectangle foregroundBounds = foregroundRect.toRectangle();
//		Screen.scale(foregroundBounds);
//		System.out.println("foregroundBounds = " + foregroundBounds);
//		Rectangle thisBounds = frame.getBounds();
//		System.out.println("thisBounds = " + thisBounds);
//
//		if(!thisBounds.intersects(foregroundBounds))
//			return this;
//
//		Point loc = frame.getLocation();
//		System.out.println("loc = " + loc);
//		loc.x = foregroundBounds.x + foregroundBounds.width;
//		System.out.println("loc = " + loc);
//		frame.setLocation(loc);

		return this;
	}

	public BubbleScreenMessage hide() {
		Rectangle bounds = frame.getBounds();
		Screen screen = Screen.getScreenForPoint(bounds.getLocation());
		screen.unscale(bounds);
		WinDef.RECT rect = new WinDef.RECT();
		rect.left = bounds.x;
		rect.top = bounds.y;
		rect.right = bounds.x + bounds.width;
		rect.bottom = bounds.y + bounds.height;
		// User32.INSTANCE.InvalidateRect(null, rect, false);

//		User32.INSTANCE.RedrawWindow(null, rect, null, new WinDef.DWORD(RDW_INVALIDATE | RDW_ERASE | RDW_ERASENOW | RDW_ALLCHILDREN));
		User32.INSTANCE.RedrawWindow(null, rect, null, new WinDef.DWORD(RDW_INVALIDATE | RDW_ALLCHILDREN));
		return this;
	}

	public BubbleScreenMessage showMessage(String text) {
		return this.showMessage(text, null);
	}

	public static void main(String[] args) throws InterruptedException {
		BubbleScreenMessage msg = new BubbleScreenMessage();
		Thread.sleep(1500);
		for(int i = 0; i < 1; i++) {
			msg.setManualLocation(MouseInfo.getPointerInfo().getLocation());
			System.out.println("show");
			msg.showMessage("bla bla bla", null);
			sleep(1500);
			System.out.println("hide");
			msg.hide();
		}
//		while(true) {
//			Thread.sleep(1000);
//			System.out.println(MouseInfo.getPointerInfo().getLocation());
//		}
//
	}

	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	// http://www.winprog.org/tutorial/bitmaps.html
	// com.sun.jna.platform.WindowUtils.paintDirect
	private static void drawImageOnScreen(BufferedImage buf, int x, int y) {
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
		PointerByReference ppbits = new PointerByReference();
		WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateDIBSection(hdcMem, bmi,
				WinGDI.DIB_RGB_COLORS,
				ppbits, null, 0);
		Pointer pbits = ppbits.getValue();

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
				if(alpha == 0)
					red = green = blue = 0;
				else {
					red   = Math.max(1, red);
					green = Math.max(1, green);
					blue  = Math.max(1, blue);
				}
				bits[col] = alpha | red | green | blue;
			}
			int v = buf.getHeight() - row - 1;
			pbits.write(v * buf.getWidth() * 4L, bits, 0, bits.length);
		}

		WinNT.HANDLE hbmOld = GDI32.INSTANCE.SelectObject(hdcMem, hBitmap);

		Screen screen = Screen.getScreenForPoint(new Point(x, y));
		Rectangle dst = new Rectangle(x, y, buf.getWidth(), buf.getHeight());
		screen.unscale(dst);

		// GDI32.INSTANCE.BitBlt(hdc, dst.x, dst.y, buf.getWidth(), buf.getHeight(), hdcMem, 0, 0, SRCCOPY);
		MyMsimg32.INSTANCE.TransparentBlt(hdc, dst.x, dst.y, dst.width, dst.height, hdcMem, 0, 0, buf.getWidth(), buf.getHeight(), 0);

		GDI32.INSTANCE.SelectObject(hdcMem, hbmOld);
		GDI32.INSTANCE.DeleteDC(hdcMem);
		User32.INSTANCE.ReleaseDC(null, hdc);
	}

	public interface MyMsimg32 extends Library {
		MyMsimg32 INSTANCE = Native.load("Msimg32", MyMsimg32.class, W32APIOptions.DEFAULT_OPTIONS);

		/**
		 * Important: These are unscaled coordinates, within an unscaled virtual screen
		 * @param dst
		 * @param dstX
		 * @param dstY
		 * @param dstW
		 * @param dstH
		 * @param src
		 * @param srcX
		 * @param srcY
		 * @param srcW
		 * @param srcH
		 * @param transparentColor
		 * @return
		 */
		boolean TransparentBlt(
				HDC dst, int dstX, int dstY, int dstW, int dstH,
				HDC src, int srcX, int srcY, int srcW, int srcH,
				int transparentColor);
	}
}
