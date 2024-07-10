package nlScript.screenplay;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Image;

public class MouseButtonVisualization implements MouseHook.GlobalMouseListener {

	private static final String leftClickResource   = "/nlScript/screenplay/icons8-left-click-24.png";
	private static final String rightClickResource  = "/nlScript/screenplay/icons8-right-click-24.png";
	private static final String middleClickResource = "/nlScript/screenplay/icons8-middle-click-24.png";

	private final Icon leftClickIcon;
	private final Icon rightClickIcon;
	private final Icon middleClickIcon;

	private final nlScript.screenplay.LabelScreenMessage sm = new nlScript.screenplay.LabelScreenMessage();

	public MouseButtonVisualization(MouseHook mouseHook) {
		leftClickIcon   = getScaledIcon(leftClickResource);
		rightClickIcon  = getScaledIcon(rightClickResource);
		middleClickIcon = getScaledIcon(middleClickResource);

		sm.setAlignment(ScreenMessage4.Alignment.TOP_LEFT);
		sm.setAlignAt(ScreenMessage4.AlignAt.CURSOR);
		sm.setBackground(new Color(0, 0, 0, 0));
		sm.setPadding(0);

		if(mouseHook != null)
			mouseHook.addGlobalMouseListener(this);
	}

	public LabelScreenMessage getMessage() {
		return sm;
	}

	private Icon getScaledIcon(String resource) {
		ImageIcon icon = new ImageIcon(this.getClass().getResource(resource));
		return new ImageIcon(icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH));
	}

	public void mousePressed(int x, int y, int button) {
		switch(button) {
			case MouseHook.GlobalMouseEvent.LEFT:  sm.showMessage(leftClickIcon);   break;
			case MouseHook.GlobalMouseEvent.RIGHT: sm.showMessage(rightClickIcon);  break;
			case MouseHook.GlobalMouseEvent.MIDDLE:sm.showMessage(middleClickIcon); break;

		}
	}

	public void mouseClicked(int x, int y, int button) {
		switch(button) {
			case MouseHook.GlobalMouseEvent.LEFT:  sm.showMessage(leftClickIcon);   break;
			case MouseHook.GlobalMouseEvent.RIGHT: sm.showMessage(rightClickIcon);  break;
			case MouseHook.GlobalMouseEvent.MIDDLE:sm.showMessage(middleClickIcon); break;
		}
		sm.hideAfter(500);
	}

	public void mouseReleased() {
		sm.hide();
	}

	public void mouseMoved(int x, int y) {
		sm.updatePosition();
	}

	public void mouseWheel(int x, int y) {
		sm.showMessage(middleClickIcon);
		sm.hideAfter(500);
	}

	@Override
	public void mousePressed(MouseHook.GlobalMouseEvent e) {
		mousePressed(e.x, e.y, e.button);
	}

	@Override
	public void mouseReleased(MouseHook.GlobalMouseEvent e) {
		mouseReleased();
	}

	@Override
	public void mouseMoved(MouseHook.GlobalMouseEvent e) {
		mouseMoved(e.x, e.y);
	}

	@Override
	public void mouseWheel(MouseHook.GlobalMouseEvent e) {
		mouseWheel(e.x, e.y);
	}

	public static void main(String[] args) throws InterruptedException {
		MouseHook mHook = new MouseHook();
		mHook.setMouseHook();
		MouseButtonVisualization mbv = new MouseButtonVisualization(mHook);

		Thread.sleep(20000);
		mbv.sm.close();
		mHook.unsetMouseHook();
	}
}
