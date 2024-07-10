package nlScript.screenplay;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.*;

public class LabelScreenMessage extends ScreenMessage4 {
	private final JLabel messageLabel;

	private static JLabel makeMessageLabel() {
		JLabel messageLabel;
		messageLabel = new JLabel("");
		// messageLabel.setForeground(Color.WHITE);
		messageLabel.setFont(new Font("Helvetica", Font.PLAIN, 24));
		return messageLabel;
	}

	public LabelScreenMessage() {
		super(makeMessageLabel());
		this.messageLabel = (JLabel) super.component;
		setFontSize(36);
		setForeground(Color.WHITE);
	}

	public LabelScreenMessage showMessage(String message, Icon icon) {
		// hide();
		messageLabel.setText(message);
		messageLabel.setIcon(icon);
		super.show();
		return this;
	}

	public LabelScreenMessage showMessage(String message) {
		return showMessage(message, null);
	}

	public LabelScreenMessage showMessage(Icon icon) {
		return showMessage("", icon);
	}

	public static void main3(String[] args) {
		LabelScreenMessage msg = new LabelScreenMessage();
		msg.setPadding(10, 40, 10, 40);
		msg.setMargin(20);
		msg.setFontSize(24);
		msg.setBackground(new Color(0, 0, 0, 150));
		msg.showMessage("<html><b>This is just<br>shit!</b></html>");
	}

	public static void main2(String[] args) {
		LabelScreenMessage msg = new LabelScreenMessage();
		msg.showMessage("bla bla");
		sleep(1000);
		msg.hide();
		msg.setForeground(Color.WHITE);
		msg.setBackground(Color.blue);
		msg.showMessage("bla bla");
		sleep(1000);
		msg.hide();
		msg.setPadding(200, 500, 200, 500);
		msg.showMessage("hehe");
		sleep(500);
		msg.hide();
//		for(Alignment alignment : Alignment.values()) {
//			msg.hide();
//			msg.setAlignment(alignment);
//			msg.showMessage(alignment.name());
//			sleep(500);
//		}
//		msg.hide();

		msg.setPadding(20);
		msg.setMargin(10);
		msg.setAlignAt(AlignAt.CURSOR);
		for(Alignment alignment : Alignment.values()) {
			msg.hide();
			msg.setAlignment(alignment);
			msg.showMessage(alignment.name());
			sleep(1000);
		}
		msg.hide();


		msg.setSize(Size.PACKED);
		msg.setBackground(new Color(0, 0, 255, 170));
		msg.setAlignment(Alignment.CENTER_LEFT);
		msg.showMessage("<html><h1>hehe</h1><p>ha ha ha hahaha ha</p></html>");
		sleep(5000);
		msg.hide();
	}

	public static void main(String[] args) throws InterruptedException {
		LabelScreenMessage lsm = new LabelScreenMessage();
		lsm.setPadding(5);
		lsm.setMargin(0);
		lsm.showMessage("ha");
		lsm.setAlignAt(AlignAt.CURSOR);
		lsm.setAlignment(Alignment.TOP_RIGHT);
		while(true) {
			Thread.sleep(500);
			lsm.updatePosition();
		}
	}

	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
}
