package nlScript.screenplay;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

public class Test {

	public static void main(String[] args) throws AWTException {
		Robot robot = new Robot();
		robot.delay(1000);
		Point start = new Point(1869,431);
		Point end = new Point(2250,861);

		double dx = (end.x - start.x) / 100.0;
		double dy = (end.y - start.y) / 100.0;

		robot.mouseMove(start.x, start.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		for(int i = 1; i <= 100; i++) {
			robot.mouseMove(
					(int) Math.round(start.x + dx * i),
					(int) Math.round(start.y + dy * i));
			robot.delay(30);
		}
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

	}
}
