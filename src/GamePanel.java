import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    // --- 1. CONSTANTS ---
    // Screen Dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;
    private static final int GAME_FPS = 60;
    private static final int GROUND_Y = 400;

    // Game States
    private final int MENU = 0;
    private final int FIGHT = 1;
    private final int GAME_OVER = 2;

    // Damage Constants
    private static final int P1_START_X = 200;
    private static final int P2_START_X = 550;
    private static final int REGULAR_DAMAGE = 10; // Regular attack damage
    private static final int SUPER_DAMAGE = 50; // Super attack damage (5x stronger)
    private static final int FIGHT_SPLASH_DURATION = 60;

    // --- 2. FIELDS ---
    private Timer timer;
    private int state = MENU;

    private Fighter player1, player2;

    private final boolean[] keys = new boolean[256];

    private boolean showFightText = false;
    private int fightTimer = 0;

    private String winnerText = "";

    // --- 3. CONSTRUCTOR & INITIALIZATION ---

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        initializeFighters();

        timer = new Timer(1000 / GAME_FPS, this);
        timer.start();
    }

    private void initializeFighters() {
        // Player 1 (Blue) uses G for Super Attack
        player1 = new Fighter(
                P1_START_X, GROUND_Y, Color.BLUE,
                KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_F, KeyEvent.VK_S, KeyEvent.VK_G // New Key: VK_G
        );
        // Player 2 (Red) uses K for Super Attack
        player2 = new Fighter(
                P2_START_X, GROUND_Y, Color.RED,
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_L, KeyEvent.VK_DOWN, KeyEvent.VK_K // New Key: VK_K
        );
    }

    private void resetGame() {
        initializeFighters();
        state = MENU;
        winnerText = "";
    }

    // --- 4. GAME LOOP (actionPerformed) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == FIGHT) {
            player1.update(keys);
            player2.update(keys);

            // DIRECTIONAL FACING / AUTO-TURN LOGIC
            if (player1.getX() < player2.getX()) {
                player1.setDirection(1);
                player2.setDirection(-1);
            } else if (player1.getX() > player2.getX()) {
                player1.setDirection(-1);
                player2.setDirection(1);
            }

            // --- ATTACK COLLISION LOGIC ---

            // Check P1 attack vs P2
            if (player1.canHit() && player1.getAttackRect().intersects(player2.getRect())) {
                // Determine damage based on whether the attack is a Super Attack
                int damage = player1.isSuperActive ? SUPER_DAMAGE : REGULAR_DAMAGE;
                player2.takeDamage(damage, player1.getDirection());
                player1.registerHit();
            }

            // Check P2 attack vs P1
            if (player2.canHit() && player2.getAttackRect().intersects(player1.getRect())) {
                // Determine damage based on whether the attack is a Super Attack
                int damage = player2.isSuperActive ? SUPER_DAMAGE : REGULAR_DAMAGE;
                player1.takeDamage(damage, player2.getDirection());
                player2.registerHit();
            }

            // --- CHECK FOR GAME END ---
            if (player1.getHealth() <= 0) {
                winnerText = "Player 2 Wins!";
                state = GAME_OVER;
            } else if (player2.getHealth() <= 0) {
                winnerText = "Player 1 Wins!";
                state = GAME_OVER;
            }

            // Fight splash timer
            if (showFightText) {
                fightTimer--;
                if (fightTimer <= 0) showFightText = false;
            }
        }
        repaint();
    }

    // --- 5. DRAWING (paintComponent) ---

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Background
        GradientPaint gp = new GradientPaint(0, 0, new Color(255, 80, 0), 0, HEIGHT, new Color(255, 200, 0));
        g2.setPaint(gp);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Ground
        g.setColor(Color.GREEN.darker());
        g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

        // Text settings
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();

        if (state == MENU) {
            drawCenteredString(g, "FIGHTING GAME", 200, fm);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press ENTER to Start", 300, fm);
            drawCenteredString(g, "Press ESC to Quit", 340, fm);

        } else if (state == FIGHT) {
            // Draw Fighters
            player1.draw(g);
            player2.draw(g);

            // Draw Health Bars and Cooldowns
            drawHealthBar(g, 50, 50, player1.getHealth(), Color.BLUE);
            drawSuperCooldown(g, 50, 70, player1.getSuperCooldown(), Color.BLUE);

            drawHealthBar(g, WIDTH - 150, 50, player2.getHealth(), Color.RED);
            drawSuperCooldown(g, WIDTH - 150, 70, player2.getSuperCooldown(), Color.RED);

            // Draw "FIGHT!" splash
            if (showFightText) {
                g.setFont(new Font("Arial", Font.BOLD, 72));
                fm = g.getFontMetrics();
                drawCenteredString(g, "FIGHT!", 200, fm);
            }
        } else if (state == GAME_OVER) {
            drawCenteredString(g, winnerText, 200, fm);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press R to Restart", 300, fm);
            drawCenteredString(g, "Press ESC to Quit", 340, fm);
        }
    }

    // Helper method to draw a centered string
    private void drawCenteredString(Graphics g, String text, int y, FontMetrics fm) {
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    // Helper method to draw a health bar
    private void drawHealthBar(Graphics g, int x, int y, int health, Color color) {
        final int BAR_WIDTH = 100;
        final int BAR_HEIGHT = 10;

        g.setColor(Color.BLACK);
        g.fillRect(x - 2, y - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4);

        g.setColor(color);
        int currentWidth = Math.max(0, health);
        g.fillRect(x, y, currentWidth, BAR_HEIGHT);
    }

    // NEW Helper method to draw the Super Cooldown indicator
    private void drawSuperCooldown(Graphics g, int x, int y, int cooldownFrames, Color color) {
        final int MAX_COOLDOWN = 25 * 60; // Max frames (1500)
        final int BAR_WIDTH = 100;
        final int BAR_HEIGHT = 5;

        float cooldownRatio = (float) cooldownFrames / MAX_COOLDOWN;
        int currentWidth = (int) (BAR_WIDTH * cooldownRatio);

        g.setColor(Color.GRAY.darker());
        g.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);

        if (cooldownFrames > 0) {
            // Draw cooldown fill (e.g., in black)
            g.setColor(Color.BLACK);
            g.fillRect(x, y, currentWidth, BAR_HEIGHT);
        } else {
            // Flash green when ready
            g.setColor(Color.GREEN.brighter());
            g.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("SUPER", x, y - 2);
    }

    // --- 6. INPUT HANDLING (KeyListener) ---

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (state == MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                state = FIGHT;
                showFightText = true;
                fightTimer = FIGHT_SPLASH_DURATION;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        } else if (state == FIGHT) {
            // Regular Attack
            if (e.getKeyCode() == player1.attackKey) player1.attack();
            if (e.getKeyCode() == player2.attackKey) player2.attack();

            // Super Attack (New)
            if (e.getKeyCode() == player1.superAttackKey) player1.superAttack();
            if (e.getKeyCode() == player2.superAttackKey) player2.superAttack();

        } else if (state == GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                resetGame();
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