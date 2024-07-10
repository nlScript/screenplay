package nlScript.screenplay;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Screen {

	public final GraphicsDevice device;

	public final int scaledOriginX;
	public final int scaledOriginY;
	public final Point scaledOrigin;

	public final int scaledWidth;
	public final int scaledHeight;
	public final Dimension scaledSize;

	public final Rectangle scaledBounds;

	public final int fullWidth;
	public final int fullHeight;
	public final Dimension fullSize;

	public final int scalingPercent;

	public final double scaling;

	public final int unscaledOriginX;
	public final int unscaledOriginY;
	public final Point unscaledOrigin;
	public final Rectangle unscaledBounds;

	private Screen(GraphicsDevice dev) {
		this.device = dev;

		scaledBounds = dev.getDefaultConfiguration().getBounds();
		scaledWidth = scaledBounds.width;
		scaledHeight = scaledBounds.height;
		scaledSize = new Dimension(scaledWidth, scaledHeight);
		scaledOrigin = scaledBounds.getLocation();
		scaledOriginX = scaledOrigin.x;
		scaledOriginY = scaledOrigin.y;

		DisplayMode dm = dev.getDisplayMode();
		fullWidth = dm.getWidth();
		fullHeight = dm.getHeight();
		fullSize = new Dimension(fullWidth, fullHeight);

		scaling = (double) fullSize.width / scaledBounds.width;
		scalingPercent = (int) Math.round(100 * scaling);

		unscaledOriginX = Math.round(scaledOriginX);
		unscaledOriginY = Math.round(scaledOriginY);
		unscaledOrigin = new Point(unscaledOriginX, unscaledOriginY);

		unscaledBounds = new Rectangle(unscaledOriginX, unscaledOriginY, fullWidth, fullHeight);
	}

	public static Screen[] list() {
		int n = nScreens();
		Screen[] screens = new Screen[n];
		for(int i = 0; i < n; i++)
			screens[i] = getScreen(i);
		return screens;
	}

	public static Rectangle getVirtualScreenBounds() {
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (GraphicsDevice gd : gs) {
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for (GraphicsConfiguration graphicsConfiguration : gc)
				virtualBounds = virtualBounds.union(graphicsConfiguration.getBounds());
		}
		return virtualBounds;
	}

	public String toString() {
		return "unscaled: " + unscaledBounds + " -> " + " scaled: " + scaledBounds + "(" + scaling + "x)";
	}

	public static int nScreens() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
	}

	public static Screen getScreen(int i) {
		return new Screen(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[i]);
	}

	public static Screen getPrimaryScreen() {
		return new Screen(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
	}

	public static Screen getScreenForPoint(Point scaled) {
		for(GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Rectangle bounds = gd.getDefaultConfiguration().getBounds();
			if(bounds.contains(scaled))
				return new Screen(gd);
		}
		throw new RuntimeException("No screen found for point "+ scaled);
	}

	public static Screen getScreenForUnscaledPoint(Point unscaled) {
		for(GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Screen s = new Screen(gd);
			Rectangle bounds = s.unscaledBounds;
			if(bounds.contains(unscaled))
				return s;
		}
		throw new RuntimeException("No screen found for point " + unscaled);
	}

	public static Point getUnscaled(Point scaled) {
		Screen s = getScreenForPoint(scaled);
		int sx = s.scaledOriginX;
		int sy = s.scaledOriginY;
		return new Point(
				sx + (int) Math.round((scaled.x - sx) * s.scaling),
				sy + (int) Math.round((scaled.y - sy) * s.scaling)
		);
	}

	public static Point getScaled(Point unscaled) {
		Screen s = getScreenForUnscaledPoint(unscaled);
		int sx = s.scaledOriginX;
		int sy = s.scaledOriginY;
		return new Point(
				sx + (int) Math.round((unscaled.x - sx) / s.scaling),
				sy + (int) Math.round((unscaled.y - sy) / s.scaling)
		);
	}

	/**
	 * Assumes the entire rectangle is contained in a single screen
	 * @param unscaled
	 */
	public static void scale(Rectangle unscaled) {
		Point loc = unscaled.getLocation();
		Screen s = getScreenForUnscaledPoint(loc);
		Point scaled = getScaled(loc);

		unscaled.setLocation(scaled);
		unscaled.width  = (int) Math.round(unscaled.width / s.scaling);
		unscaled.height = (int) Math.round(unscaled.height / s.scaling);
	}

	/**
	 * Assumes the entire rectangle is contained in a single screen
	 * @param r
	 */
	public void unscale(Rectangle r) {
		Point loc = r.getLocation();
		Screen s = getScreenForPoint(loc);
		Point unscaled = getUnscaled(loc);

		r.setLocation(unscaled);
		r.width  = (int) Math.round(r.width * scaling);
		r.height = (int) Math.round(r.height * scaling);
	}

	public static void main(String[] args) throws AWTException, IOException, InterruptedException {
		int nScreens = nScreens();
		System.out.println("Virtual Screen");
		System.out.println(getVirtualScreenBounds());
		System.out.println("Screens:");
		for(int s = 0; s < nScreens; s++) {
			System.out.println("Screen " + s + ": " + getScreen(s));
			// System.out.println(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[s].getDefaultConfiguration().getBounds());
			GraphicsConfiguration[] configs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[s].getConfigurations();
			for(GraphicsConfiguration gc : configs) {
				System.out.println("gc = " + gc);
				System.out.println(gc.getDefaultTransform());
			}
		}
		Robot robot = new Robot();
		BufferedImage image = robot.createScreenCapture(getScreen(0).scaledBounds);
		File file = File.createTempFile("robot", ".png");
		ImageIO.write(image, "png", file);
		Desktop.getDesktop().open(file);
	}
}
