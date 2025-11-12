import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    private final int WIDTH = 800, HEIGHT = 500;
    private Timer timer;

    // Game states
    private final int MENU = 0, FIGHT = 1, GAME_OVER = 2;
    private int state = MENU;

    // Fighters
    private Fighter player1, player2;

    // Input
    private boolean[] keys = new boolean[256];

    // Fight splash
    private boolean showFightText = false;
    private int fightTimer = 0;

    // Winner
    private String winnerText = "";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        // Initialize fighters
        player1 = new Fighter(200, 400, Color.BLUE, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_F);
        player2 = new Fighter(550, 400, Color.RED, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_L);

        // Timer for game loop
        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Gradient background
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint gp = new GradientPaint(0, 0, new Color(255, 80, 0), 0, HEIGHT, new Color(255, 200, 0));
        g2.setPaint(gp);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Ground
        g.setColor(Color.GREEN);
        g.fillRect(0, 400, WIDTH, 100);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));

        if (state == MENU) {
            g.drawString("FIGHTING GAME", 200, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press ENTER to Start", 270, 300);
            g.drawString("Press ESC to Quit", 300, 340);
        } else if (state == FIGHT) {
            if (showFightText) {
                g.drawString("FIGHT!", 350, 200);
            }
            player1.draw(g);
            player2.draw(g);

            // Health bars
            g.setColor(Color.BLACK);
            g.fillRect(48, 48, 104, 14);
            g.fillRect(648, 48, 104, 14);
            g.setColor(Color.BLUE);
            g.fillRect(50, 50, player1.health, 10);
            g.setColor(Color.RED);
            g.fillRect(650, 50, player2.health, 10);
        } else if (state == GAME_OVER) {
            g.drawString(winnerText, 200, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press R to Restart", 300, 300);
            g.drawString("Press ESC to Quit", 300, 340);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == FIGHT) {
            player1.update(keys);
            player2.update(keys);

            // Attack collision
            if (player1.attackCooldown > 10 && player1.getAttackRect().intersects(player2.getRect()))
                player2.health -= 1;
            if (player2.attackCooldown > 10 && player2.getAttackRect().intersects(player1.getRect()))
                player1.health -= 1;

            if (player1.health <= 0) {
                winnerText = "Player 2 Wins!";
                state = GAME_OVER;
            } else if (player2.health <= 0) {
                winnerText = "Player 1 Wins!";
                state = GAME_OVER;
            }

            if (showFightText) {
                fightTimer--;
                if (fightTimer <= 0) showFightText = false;
            }
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (state == MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                state = FIGHT;
                showFightText = true;
                fightTimer = 60;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        } else if (state == FIGHT) {
            if (e.getKeyCode() == player1.attackKey) player1.attack();
            if (e.getKeyCode() == player2.attackKey) player2.attack();
        } else if (state == GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                player1.health = 100;
                player2.health = 100;
                player1.x = 200; player1.y = 400;
                player2.x = 550; player2.y = 400;
                state = MENU;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
