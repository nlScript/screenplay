package nlScript.screenplay;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

public class KeyStrokeScreenMessage extends ScreenMessage4 {
	private final JPanel keystrokePanel;

	private static JPanel makeKeystrokePanel() {
		JPanel keystrokePanel;
		keystrokePanel = new JPanel();
		keystrokePanel.setBackground(new Color(0, 0, 0, 0));
		keystrokePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
		return keystrokePanel;
	}

	public KeyStrokeScreenMessage() {
		super(makeKeystrokePanel());
		this.keystrokePanel = (JPanel) super.component;
		setAlignment(Alignment.CENTER);
		setMargin(20);
		setFontSize(48);
		setBackground(new Color(0, 0, 0, 200));
		setForeground(new Color(200, 200, 200));
		setPadding(20);
	}

	public KeyStrokeScreenMessage showKeystrokes(KeyStroke... keystrokes) {
		keystrokePanel.removeAll();
		for(int j = 0; j < keystrokes.length; j++) {
			KeyStroke ks = keystrokes[j];
			String s = ks.toString().replace("pressed ", "");
			String[] toks = s.split(" ");
			for(int i = 0; i < toks.length; i++) {
				String tok = toks[i];
				KeyStrokeLabel label = new KeyStrokeLabel();
				label.setFont(keystrokePanel.getFont());
				label.setText(tok);
				label.setBackground(keystrokePanel.getForeground());
				label.setOpaque(true);
				label.setBorder(new RoundedBorder(keystrokePanel.getForeground(), 20));
				keystrokePanel.add(label);
				if(i < toks.length - 1) {
					JLabel label2 = new JLabel();
					label2.setFont(keystrokePanel.getFont());
					label2.setText("+");
					label2.setForeground(keystrokePanel.getForeground());
					label2.setBackground(new Color(0, 0, 0, 0));
					keystrokePanel.add(label2);
				}
			}
			if(j < keystrokes.length - 1) {
				JLabel label2 = new JLabel();
				label2.setFont(keystrokePanel.getFont().deriveFont(Font.BOLD));
				label2.setText(", ");
				label2.setForeground(keystrokePanel.getForeground());
				label2.setBackground(new Color(0, 0, 0, 0));
				keystrokePanel.add(label2);
			}
		}
		super.show();
		return this;
	}

	public static class KeyStrokeLabel extends JLabel {
		public KeyStrokeLabel() {
			super();
		}

		public void paintComponent(Graphics g) {
			g.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
			super.paintComponent(g);
		}
	}

	public class RoundedBorder extends AbstractBorder {

		private final Color color;
		private final int gap;

		public RoundedBorder(Color c, int g) {
			color = c;
			gap = g;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setColor(color);
			RoundRectangle2D.Double bubble = new RoundRectangle2D.Double(x, y, width - 1, height - 1, gap, gap);
			g2d.draw(bubble);

			Area area = new Area(bubble);
			// area.add(new Area(pointer));

			// g2d.setRenderingHints(hints);

//			g2d.setColor(color);
//			g2d.setStroke(new BasicStroke(2f));
//			g2d.draw(area);

			g2d.dispose();
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return (getBorderInsets(c, new Insets(gap, gap, gap, gap)));
		}

		@Override
		public Insets getBorderInsets(Component c, Insets insets) {
			insets.left = insets.top = insets.right = insets.bottom = gap / 2;
			return insets;
		}

		@Override
		public boolean isBorderOpaque() {
			return true;
		}
	}

	public static void main(String[] args) {
		KeyStrokeScreenMessage msg = new KeyStrokeScreenMessage();
		msg.showKeystrokes(
				KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK),
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		sleep(5000);
		msg.hide();
//		msg.showMessage("bla bla");
//		sleep(1000);
//		msg.hide();
//		msg.setForeground(Color.WHITE);
//		msg.setBackground(Color.blue);
//		msg.showMessage("bla bla");
//		sleep(1000);
//		msg.hide();
//		msg.setPadding(200, 500, 200, 500);
//		msg.showMessage("hehe");
//		sleep(500);
//		msg.hide();
//		for(Alignment alignment : Alignment.values()) {
//			msg.hide();
//			msg.setAlignment(alignment);
//			msg.showMessage("hehe");
//			sleep(500);
//		}
//		msg.hide();
	}

	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
}
