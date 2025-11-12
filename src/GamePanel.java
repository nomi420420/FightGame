import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    // --- 1. CONSTANTS ---
    // Screen Dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;
    private static final int GAME_FPS = 60;
    private static final int GROUND_Y = 400;
    private static final int INITIAL_STOCKS = 3; // MATCH IS BEST OUT OF 3 STOCKS
    private static final int ROUND_END_PAUSE_DURATION = 120; // 2 seconds pause after stock loss

    // Game States (Updated Indices)
    private final int START_MENU = 0, CHARACTER_SELECT = 1, STAGE_SELECT = 2, FIGHT = 3, GAME_OVER = 4;

    // Fighter Constants
    private static final int P1_START_X = 200;
    private static final int P2_START_X = 550;
    private static final int REGULAR_DAMAGE = 10;
    private static final int SUPER_DAMAGE = 50;
    private static final int FIGHT_SPLASH_DURATION = 60;
    private static final int PUSH_BACK_AMOUNT = 2;

    // --- Stage Structure (For simple color themes) ---
    private static class Stage {
        String name;
        Color topColor;
        Color bottomColor;

        public Stage(String name, Color top, Color bottom) {
            this.name = name;
            this.topColor = top;
            this.bottomColor = bottom;
        }
    }

    // --- 2. FIELDS ---
    private Timer timer;
    private int state = START_MENU;

    // CHARACTER SELECT FIELDS
    private final Color[] availableColors = {Color.BLUE, Color.RED, Color.MAGENTA, Color.YELLOW, Color.CYAN};
    private int p1SelectionIndex = 0;
    private int p2SelectionIndex = 1;

    // STAGE SELECT FIELDS
    private final Stage[] availableStages = {
            new Stage("Daytime City", new Color(255, 80, 0), new Color(255, 200, 0)),
            new Stage("Night Arena", new Color(20, 20, 80), new Color(50, 50, 150)),
            new Stage("Volcano Summit", new Color(150, 50, 0), new Color(255, 100, 0)),
            new Stage("Frost Peaks", new Color(150, 200, 255), new Color(220, 240, 255))
    };
    private int selectedStageIndex = 0;

    private int p1Stocks; // Tracks P1's remaining stocks
    private int p2Stocks; // Tracks P2's remaining stocks

    private Fighter player1, player2;

    private final boolean[] keys = new boolean[256];

    private boolean showFightText = false;
    private int fightTimer = 0;

    private int roundEndTimer = 0;
    private String roundEndMessage = "";

    private String winnerText = "";

    // --- 3. CONSTRUCTOR & INITIALIZATION ---

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(1000 / GAME_FPS, this);
        timer.start();
    }

    private void initializeFighters() {
        // Ensure players don't select the same color
        if (p1SelectionIndex == p2SelectionIndex) {
            p2SelectionIndex = (p2SelectionIndex + 1) % availableColors.length;
        }

        // Initialize stocks
        p1Stocks = INITIAL_STOCKS;
        p2Stocks = INITIAL_STOCKS;

        Color color1 = availableColors[p1SelectionIndex];
        Color color2 = availableColors[p2SelectionIndex];

        // Player 1 (Keyset 1)
        player1 = new Fighter(
                P1_START_X, GROUND_Y, color1,
                KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W,
                KeyEvent.VK_F, KeyEvent.VK_G,
                KeyEvent.VK_S,
                KeyEvent.VK_E, KeyEvent.VK_R
        );
        // Player 2 (Keyset 2)
        player2 = new Fighter(
                P2_START_X, GROUND_Y, color2,
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
                KeyEvent.VK_L, KeyEvent.VK_K,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_I, KeyEvent.VK_O
        );
    }

    private void resetGame() {
        // Reset stocks and return to stage select
        p1Stocks = INITIAL_STOCKS;
        p2Stocks = INITIAL_STOCKS;
        state = STAGE_SELECT;
        winnerText = "";
    }

    // --- 4. GAME LOOP (actionPerformed) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == FIGHT) {

            // --- PAUSE/ROUND RESET LOGIC (Delayed Reset Fix) ---
            if (roundEndTimer > 0) {
                roundEndTimer--;
                if (roundEndTimer <= 0) {
                    // *** FIX: Reset health and state immediately before starting next round ***
                    player1.resetHealth();
                    player2.resetHealth();

                    // Time to start the next round
                    roundEndMessage = "";
                    // Reset positions for start of round
                    player1.setX(P1_START_X);
                    player1.setY(GROUND_Y - player1.getRect().height);
                    player2.setX(P2_START_X);
                    player2.setY(GROUND_Y - player2.getRect().height);
                    showFightText = true;
                    fightTimer = FIGHT_SPLASH_DURATION;
                }
                repaint();
                return; // PAUSE: Skip all update/attack logic
            }
            // ----------------------------------------------------

            player1.update(keys);
            player2.update(keys);

            // --- DIRECTIONAL FACING / AUTO-TURN LOGIC ---
            if (player1.getX() < player2.getX()) {
                player1.setDirection(1);
                player2.setDirection(-1);
            } else if (player1.getX() > player2.getX()) {
                player1.setDirection(-1);
                player2.setDirection(1);
            }

            // --- FIGHTER-TO-FIGHTER COLLISION ---
            Rectangle r1 = player1.getRect();
            Rectangle r2 = player2.getRect();

            if (r1.intersects(r2)) {
                if (player1.getX() < player2.getX()) {
                    player1.setX(player1.getX() - PUSH_BACK_AMOUNT);
                    player2.setX(player2.getX() + PUSH_BACK_AMOUNT);
                } else {
                    player1.setX(player1.getX() + PUSH_BACK_AMOUNT);
                    player2.setX(player2.getX() - PUSH_BACK_AMOUNT);
                }
            }
            // ------------------------------------

            // --- ATTACK COLLISION LOGIC ---

            // P1 Attack Check
            if (player1.canHit()) {
                int damage = keys[player1.superAttackKey] ? SUPER_DAMAGE : REGULAR_DAMAGE;
                boolean isSuper = damage == SUPER_DAMAGE;

                if (player1.getAttackRect().intersects(player2.getRect()) && !player2.isKnockedDown()) {
                    player2.takeDamage(damage, player1.getDirection());
                    player1.registerHit(isSuper);
                    player1.gainMeter(isSuper ? Fighter.METER_GAIN_HIT * 2 : Fighter.METER_GAIN_HIT);
                }
            }

            // P2 Attack Check
            if (player2.canHit()) {
                int damage = keys[player2.superAttackKey] ? SUPER_DAMAGE : REGULAR_DAMAGE;
                boolean isSuper = damage == SUPER_DAMAGE;

                if (player2.getAttackRect().intersects(player1.getRect()) && !player1.isKnockedDown()) {
                    player1.takeDamage(damage, player2.getDirection());
                    player2.registerHit(isSuper);
                    player2.gainMeter(isSuper ? Fighter.METER_GAIN_HIT * 2 : Fighter.METER_GAIN_HIT);
                }
            }

            // --- CHECK FOR HEALTH/STOCK LOSS (ROUND/MATCH END) ---
            boolean stockLost = false;
            String finalWinner = null;

            // Check P1 Health (P2 Wins Round)
            if (player1.getHealth() <= 0) {
                p1Stocks--;
                stockLost = true;

                if (p1Stocks <= 0) {
                    finalWinner = "Player 2 Wins!";
                } else {
                    roundEndMessage = "Player 2 Wins Round!";
                    // Health reset is now handled in roundEndTimer == 0 block
                }
            }

            // Check P2 Health (P1 Wins Round)
            if (player2.getHealth() <= 0) {
                p2Stocks--;
                stockLost = true;

                if (p2Stocks <= 0) {
                    finalWinner = "Player 1 Wins!";
                } else {
                    roundEndMessage = "Player 1 Wins Round!";
                    // Health reset is now handled in roundEndTimer == 0 block
                }
            }

            // 1. CHECK FOR GAME OVER (Match End)
            if (finalWinner != null) {
                winnerText = finalWinner;
                state = GAME_OVER;
            }

            // 2. START NEW ROUND (If match is NOT over, but a stock was lost)
            if (stockLost && state == FIGHT) {
                roundEndTimer = ROUND_END_PAUSE_DURATION;
            }


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

        // --- BACKGROUND DRAWING ---
        Color top = (state == FIGHT || state == GAME_OVER || state == STAGE_SELECT) ? availableStages[selectedStageIndex].topColor : new Color(255, 80, 0);
        Color bottom = (state == FIGHT || state == GAME_OVER || state == STAGE_SELECT) ? availableStages[selectedStageIndex].bottomColor : new Color(255, 200, 0);

        GradientPaint gp = new GradientPaint(0, 0, top, 0, HEIGHT, bottom);
        g2.setPaint(gp);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Ground (Only draw ground in fight/game over states)
        if (state == FIGHT || state == GAME_OVER) {
            g.setColor(Color.GREEN.darker());
            g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        }

        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();

        if (state == START_MENU) {
            g.setFont(new Font("Arial", Font.BOLD, 64));
            fm = g.getFontMetrics();
            drawCenteredString(g, "JAVA FIGHTER", 150, fm);

            g.setFont(new Font("Arial", Font.PLAIN, 30));
            fm = g.getFontMetrics();
            drawCenteredString(g, "A GAME OF SKILL AND TIMING", 250, fm);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press ENTER to Begin", 350, fm);
            drawCenteredString(g, "Press ESC to Quit", 400, fm);

        } else if (state == CHARACTER_SELECT) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();
            drawCenteredString(g, "SELECT YOUR FIGHTER", 100, fm);

            drawCharacterSelection(g, 150, 200, p1SelectionIndex, "PLAYER 1 (A/D)", KeyEvent.VK_A, KeyEvent.VK_D);
            drawCharacterSelection(g, WIDTH - 350, 200, p2SelectionIndex, "PLAYER 2 (Arrows)", KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press ENTER for Stage Select", 450, fm);

        } else if (state == STAGE_SELECT) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();
            drawCenteredString(g, "SELECT YOUR STAGE", 100, fm);

            g.setFont(new Font("Arial", Font.PLAIN, 30));
            fm = g.getFontMetrics();
            drawCenteredString(g, availableStages[selectedStageIndex].name, 200, fm);

            // Draw a simplified preview of the selected stage gradient
            int previewWidth = 400;
            int previewHeight = 150;
            int previewX = (WIDTH - previewWidth) / 2;
            int previewY = 230;

            Stage current = availableStages[selectedStageIndex];
            GradientPaint previewGp = new GradientPaint(previewX, previewY, current.topColor, previewX, previewY + previewHeight, current.bottomColor);
            g2.setPaint(previewGp);
            g2.fillRect(previewX, previewY, previewWidth, previewHeight);

            // Draw selection marker
            g.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(4));
            g2.drawRect(previewX - 2, previewY - 2, previewWidth + 4, previewHeight + 4);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press A/D or Arrows to Choose. ENTER to Fight!", 450, fm);

        } else if (state == FIGHT) {
            // Draw Fighters
            player1.draw(g);
            player2.draw(g);

            // Draw Health Bars
            drawHealthBar(g, 50, 50, player1.getHealth(), availableColors[p1SelectionIndex]);
            drawHealthBar(g, WIDTH - 150, 50, player2.getHealth(), availableColors[p2SelectionIndex]);

            // Draw Super Meters
            drawSuperMeter(g, 50, 70, player1.getSuperMeter(), availableColors[p1SelectionIndex]);
            drawSuperMeter(g, WIDTH - 150, 70, player2.getSuperMeter(), availableColors[p2SelectionIndex]);

            // Draw Stocks
            drawStocks(g, 50, 95, p1Stocks, availableColors[p1SelectionIndex]);
            drawStocks(g, WIDTH - 150, 95, p2Stocks, availableColors[p2SelectionIndex]);

            // Draw ROUND END Message (Fixes overlap with FIGHT! splash)
            if (roundEndTimer > 0) {
                g.setFont(new Font("Arial", Font.BOLD, 56));
                fm = g.getFontMetrics();
                drawCenteredString(g, roundEndMessage, 200, fm);
            }
            // Draw "FIGHT!" splash (Only runs when roundEndTimer is NOT active)
            else if (showFightText) {
                g.setFont(new Font("Arial", Font.BOLD, 72));
                fm = g.getFontMetrics();
                drawCenteredString(g, "FIGHT!", 200, fm);
            }

        } else if (state == GAME_OVER) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();
            drawCenteredString(g, winnerText, 200, fm);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press R to Restart", 300, fm);
            drawCenteredString(g, "Press ESC to Quit", 340, fm);
        }
    }

    // --- Custom Drawing Methods ---

    // Draws the stock indicators
    private void drawStocks(Graphics g, int x, int y, int stocks, Color color) {
        final int STOCK_SIZE = 15;
        final int GAP = 5;

        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        // Position the text to the left of the stocks
        g.drawString("STOCKS:", x, y - 5);

        for (int i = 0; i < INITIAL_STOCKS; i++) {
            // Stocks start drawing to the right of the "STOCKS:" text, offset by 50
            int stockX = x + 50 + i * (STOCK_SIZE + GAP);

            if (i < stocks) {
                g.setColor(color);
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            g.fillOval(stockX, y - STOCK_SIZE + 2, STOCK_SIZE, STOCK_SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(stockX, y - STOCK_SIZE + 2, STOCK_SIZE, STOCK_SIZE);
        }
    }

    private void drawCharacterSelection(Graphics g, int x, int y, int selectionIndex, String label, int leftKey, int rightKey) {
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString(label, x, y - 40);

        for (int i = 0; i < availableColors.length; i++) {
            g.setColor(availableColors[i]);
            int boxX = x + i * 60;
            g.fillOval(boxX, y, 50, 50);

            if (i == selectionIndex) {
                g.setColor(Color.WHITE);
                Graphics2D g2 = (Graphics2D) g;
                Stroke oldStroke = g2.getStroke();
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(boxX - 5, y - 5, 60, 60);
                g2.setStroke(oldStroke);
            }

            if (i == (label.contains("PLAYER 1") ? p2SelectionIndex : p1SelectionIndex) && i != selectionIndex) {
                g.setColor(Color.BLACK);
                g.drawString("X", boxX + 15, y + 35);
            }
        }
    }

    private void drawCenteredString(Graphics g, String text, int y, FontMetrics fm) {
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private void drawHealthBar(Graphics g, int x, int y, int health, Color color) {
        final int BAR_WIDTH = 100;
        final int BAR_HEIGHT = 10;

        g.setColor(Color.BLACK);
        g.fillRect(x - 2, y - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4);

        g.setColor(color);
        int currentWidth = Math.max(0, health);
        g.fillRect(x, y, currentWidth, BAR_HEIGHT);
    }

    private void drawSuperMeter(Graphics g, int x, int y, float meter, Color color) {
        final int BAR_WIDTH = 100;
        final int BAR_HEIGHT = 5;

        g.setColor(Color.GRAY);
        g.fillRect(x - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2);

        g.setColor(new Color(255, 165, 0));
        int currentWidth = (int) Math.max(0, meter);
        g.fillRect(x, y, currentWidth, BAR_HEIGHT);

        if (meter >= Fighter.SUPER_ATTACK_COST && (System.currentTimeMillis() / 200) % 2 == 0) {
            g.setColor(Color.YELLOW);
            g.drawRect(x - 1, y - 1, BAR_WIDTH + 1, BAR_HEIGHT + 1);
        }
    }

    // --- 6. INPUT HANDLING (KeyListener) ---

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;

        if (state == START_MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                state = CHARACTER_SELECT;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        } else if (state == CHARACTER_SELECT) {
            if (e.getKeyCode() == KeyEvent.VK_A) {
                p1SelectionIndex = (p1SelectionIndex - 1 + availableColors.length) % availableColors.length;
            } else if (e.getKeyCode() == KeyEvent.VK_D) {
                p1SelectionIndex = (p1SelectionIndex + 1) % availableColors.length;
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                p2SelectionIndex = (p2SelectionIndex - 1 + availableColors.length) % availableColors.length;
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                p2SelectionIndex = (p2SelectionIndex + 1) % availableColors.length;
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                // Transition to stage select
                state = STAGE_SELECT;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        } else if (state == STAGE_SELECT) {
            int maxStages = availableStages.length;
            if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
                selectedStageIndex = (selectedStageIndex - 1 + maxStages) % maxStages;
            } else if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                selectedStageIndex = (selectedStageIndex + 1) % maxStages;
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                // Finalize selection and start the fight
                initializeFighters();
                state = FIGHT;
                showFightText = true;
                fightTimer = FIGHT_SPLASH_DURATION;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        } else if (state == FIGHT) {
            if (e.getKeyCode() == player1.attackKey) player1.attack();
            if (e.getKeyCode() == player2.attackKey) player2.attack();

            if (e.getKeyCode() == player1.superAttackKey) player1.superAttack();
            if (e.getKeyCode() == player2.superAttackKey) player2.superAttack();

            if (e.getKeyCode() == player1.dashFwdKey) player1.dashForward();
            if (e.getKeyCode() == player1.dashBackKey) player1.dashBack();
            if (e.getKeyCode() == player2.dashFwdKey) player2.dashForward();
            if (e.getKeyCode() == player2.dashBackKey) player2.dashBack();

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