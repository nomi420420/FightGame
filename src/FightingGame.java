import javax.swing.JFrame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Dimension;

public class FightingGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Fantasy Fighting Game"); // New Game Title Here
        GamePanel panel = new GamePanel();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        if (gd.isFullScreenSupported()) {
            frame.setUndecorated(true);

            // Set the panel's preferred size to the screen resolution for scaling
            Dimension screenSize = gd.getDefaultConfiguration().getBounds().getSize();
            panel.setPreferredSize(screenSize);

            // Enter fullscreen exclusive mode
            gd.setFullScreenWindow(frame);

        } else {
            // Fallback for windowed mode
            frame.pack();
            frame.setResizable(false);
        }

        frame.setVisible(true);
    }
}