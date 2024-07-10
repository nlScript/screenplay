package nlScript.screenplay;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;

public class WaitForOK {

	public static void block(Window parent) {
		block(parent, "");
	}

	public static void block(Window parent, String message) {
		// JFrame parent = new JFrame();

		final Object lock = new Object();

		JDialog frame;
		frame = new JDialog(parent);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setUndecorated(true); // Remove window decorations (title bar, etc.)
		frame.setBackground(new Color(0, 0, 0, 150)); // Make the window transparent
		frame.setAlwaysOnTop(true); // Keep the window on top of other windows
		frame.setFocusable(false);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		buttonPanel.setBackground(Color.DARK_GRAY);

		JButton button = new JButton("OK");
		buttonPanel.add(button);

		SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();

		button.addActionListener(e -> {
			loop.exit();
			frame.dispose();
		});
		button.setFont(button.getFont().deriveFont(20f));
		button.setForeground(Color.WHITE);
		button.setBackground(Color.DARK_GRAY);
		frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		frame.setBackground(Color.DARK_GRAY);

		if(message != null && !message.isEmpty()) {
			final String html = "<html><style>ul li{ padding-bottom: 5px; padding-top: 5px}</style><body style='width: %1spx'>%1s";
			message = String.format(html, 200, message);
			JPanel panel = new JPanel(new FlowLayout());
			panel.setBackground(Color.DARK_GRAY);
			panel.setBorder(new CompoundBorder(
					new EmptyBorder(5, 5, 5, 5),
					new TitledBorder(null, "Message", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, null, Color.GRAY)));
			JLabel area = new JLabel(message);
			area.setFont(area.getFont().deriveFont(12f));
			// area.setLineWrap(true);
			area.setForeground(Color.WHITE);
			area.setBackground(Color.DARK_GRAY);
			panel.add(area, BorderLayout.CENTER);
			frame.getContentPane().add(panel, BorderLayout.CENTER);
		}

		frame.pack();

		int w = GraphicsEnvironment.
				getLocalGraphicsEnvironment().
				getDefaultScreenDevice().
				getDefaultConfiguration().
				getBounds().width;

		frame.setLocation(w - 50 - frame.getWidth(), 50);
		frame.setVisible(true);
		loop.enter();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			block(new JFrame(), "Please do this and that and whatever <ul><li>bla</li><li>blubb</li></ul>and don't wait to long and then click on OK.");
			System.out.println("done");
		});
	}
}
