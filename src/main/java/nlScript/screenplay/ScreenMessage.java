package nlScript.screenplay;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.*;
import java.net.URL;

public class ScreenMessage {
    private final JDialog frame;
    private final JLabel messageLabel;

    private static ScreenMessage instance = null;

    private ScreenMessage() {
        JFrame parent = new JFrame();
        frame = new JDialog(parent);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true); // Remove window decorations (title bar, etc.)
        frame.setBackground(new Color(0, 0, 0, 150)); // Make the window transparent
        frame.setAlwaysOnTop(true); // Keep the window on top of other windows
        frame.setFocusable(false);
        frame.setSize(200, 150);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.getContentPane().setLayout(gridbag);

        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Helvetica", Font.PLAIN, 24));
        c.gridx = 0; c.gridy = 0;
        c.insets = new Insets(10, 40, 10, 40);
        frame.getContentPane().add(messageLabel, c);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setLocation(frame.getX(), 20);


//        Timer timer = new Timer(5000, e -> {
//            frame.dispose();
//        });
//        timer.setRepeats(false);
//        timer.start();
    }

    private void doShowMessage(String message, Icon icon) {
        doHide();
        messageLabel.setText(message);
        messageLabel.setIcon(icon);
        frame.setAlwaysOnTop(true); // Keep the window on top of other windows
        frame.setFocusable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setLocation(frame.getX(), 20);

        frame.setFocusableWindowState(false);
        frame.setVisible(true);
        WindowUtils.makeWindowClickThrough(frame);
    }

    private void doHide() {
        if(frame.isVisible())
            frame.setVisible(false);
    }

    private static ScreenMessage instance() {
        if(instance == null)
            instance = new ScreenMessage();
        return instance;
    }

    public static void dispose() {
        if(instance != null) {
            instance.frame.dispose();
            instance = null;
        }
    }

    public static void hide() {
        if(instance != null)
            instance.doHide();
    }

    public static void showMessage(String message) {
        instance().doShowMessage(message, null);
    }

    public static void showMessage(String message, Icon icon) {
        instance().doShowMessage(message, icon);
    }

    public static void showMessage(String message, int ms) {
        showMessage(message, null, ms);
    }

    public static void showMessage(String message, Icon icon, int ms) {
        showMessage(message, icon, ms, false);
    }
    public static void showMessage(String message, Icon icon, int ms, boolean atCursor) {

        showMessage(message, icon);
        if(atCursor) {
            instance.frame.setLocation(MouseInfo.getPointerInfo().getLocation());
        }
        Timer timer = new Timer(ms, e -> {
            hide();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void main(String[] args) {
        URL url = ScreenMessage.class.getResource("/nlScript/screenplay/icons8-left-click-24.png");
        Icon icon = new ImageIcon(url);
        SwingUtilities.invokeLater(() -> {
//            ScreenMessage.showMessage("Hi");
//            sleep(5000);
//            ScreenMessage.showMessage("It's me again");
//            sleep(2000);
//            ScreenMessage.hide();
//            sleep(1000);
//            ScreenMessage.showMessage("blubb");
//            sleep(1000);
//            ScreenMessage.dispose();
            // ScreenMessage.showMessage("[Ctrl-shift a]", icon);
            ScreenMessage.showMessage("<html><b>This is just<br>shit!</b></html>");
        });
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
