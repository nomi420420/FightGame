import javax.swing.*;

public class DinoFightingGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Dino Fighting Game");
        GamePanel panel = new GamePanel();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
