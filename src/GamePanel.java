import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.util.ArrayList;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    // --- 1. CONSTANTS ---
    // Screen Dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 500;
    private static final int GAME_FPS = 60;
    private static final int GROUND_Y = 400;
    private static final int INITIAL_STOCKS = 3;
    private static final int ROUND_END_PAUSE_DURATION = 120; // 2 seconds pause

    // Game States
    private final int START_MENU = 0, MODE_SELECT = 1, CHARACTER_SELECT = 2, FIGHT = 4, AI_FIGHT = 5, GAME_OVER = 6;

    // Combat
    private static final int REGULAR_DAMAGE = 10;
    private static final int SUPER_DAMAGE = 50;
    private static final int FIGHT_SPLASH_DURATION = 60;
    private static final int PUSH_BACK_AMOUNT = 2;

    // Fighter Start Positions
    private static final int P1_START_X = 200;
    private static final int P2_START_X = 550;

    // --- Stage Structure ---
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

    // --- Fighter Asset Structure ---
    private static class FighterAssets {
        BufferedImage idleSprite;
        BufferedImage[] runSprites;
    }

    // --- 2. FIELDS ---
    private Timer timer;
    private int state = START_MENU;

    // ASSET FIELDS
    private final ArrayList<FighterAssets> fighterAssetSets = new ArrayList<>();

    // CHARACTER SELECT FIELDS (4 CHOICES)
    private final Color[] availableColors = {Color.BLUE, Color.RED, Color.MAGENTA, Color.YELLOW};
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

    private int p1Stocks;
    private int p2Stocks;

    private Fighter player1, player2;

    private final boolean[] keys = new boolean[256];

    private boolean showFightText = false;
    private int fightTimer = 0;

    private int roundEndTimer = 0;
    private String roundEndMessage = "";

    private String winnerText = "";

    private int gameStateMode = FIGHT; // Stores whether the fight is local or AI

    private int menuTransitionTimer = 0; // Input Gate Timer (5 frames)

    // --- 3. CONSTRUCTOR & INITIALIZATION ---

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        loadImages(); // Load all sprite assets once

        timer = new Timer(1000 / GAME_FPS, this);
        timer.start();
    }

    private void loadImages() {
        for (int i = 0; i < 4; i++) {
            FighterAssets assets = new FighterAssets();
            try {
                BufferedImage baseSpriteSheet = ImageIO.read(new File("assets/fighter_sheet_" + i + ".png"));

                final int FRAME_SIZE = Fighter.SPRITE_SIZE;
                final int RUN_FRAME_COUNT = Fighter.RUN_FRAME_COUNT;

                if (baseSpriteSheet.getHeight() < FRAME_SIZE) {
                    throw new IOException("Sprite sheet height is too small for frame size: " + baseSpriteSheet.getHeight());
                }

                // SLICING LOGIC
                assets.idleSprite = baseSpriteSheet.getSubimage(0, 0, FRAME_SIZE, FRAME_SIZE);
                assets.runSprites = new BufferedImage[RUN_FRAME_COUNT];
                for (int j = 0; j < RUN_FRAME_COUNT; j++) {
                    int xOffset = (j + 1) * FRAME_SIZE;
                    if (xOffset + FRAME_SIZE > baseSpriteSheet.getWidth()) {
                        throw new IOException("Sprite sheet width is too small for " + RUN_FRAME_COUNT + " run frames.");
                    }
                    assets.runSprites[j] = baseSpriteSheet.getSubimage(xOffset, 0, FRAME_SIZE, FRAME_SIZE);
                }

                fighterAssetSets.add(assets);

            } catch (IOException e) {
                System.err.println("Error loading sprite sheet for index " + i + ": " + e.getMessage());

                // Fallback: Create a placeholder sprite if image loading fails
                final int FALLBACK_SIZE = FRAMEBITS;
                BufferedImage fallback = new BufferedImage(FALLBACK_SIZE, FALLBACK_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2_fallback = fallback.createGraphics();
                g2_fallback.setColor(Color.WHITE);
                g2_fallback.fillRect(0, 0, FALLBACK_SIZE, FALLBACK_SIZE);
                g2_fallback.dispose();

                assets.idleSprite = fallback;
                assets.runSprites = new BufferedImage[]{fallback, fallback, fallback, fallback};
                fighterAssetSets.add(assets);
            }
        }
    }

    private void initializeFighters() {
        if (p1SelectionIndex == p2SelectionIndex) {
            p2SelectionIndex = (p2SelectionIndex + 1) % availableColors.length;
        }

        p1Stocks = INITIAL_STOCKS;
        p2Stocks = INITIAL_STOCKS;

        FighterAssets assets1 = fighterAssetSets.get(p1SelectionIndex);
        FighterAssets assets2 = fighterAssetSets.get(p2SelectionIndex);

        // Player 1 (Keyset 1)
        player1 = new Fighter(
                P1_START_X, GROUND_Y, availableColors[p1SelectionIndex],
                KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W,
                KeyEvent.VK_F, KeyEvent.VK_G,
                KeyEvent.VK_S,
                KeyEvent.VK_E, KeyEvent.VK_R,
                assets1.idleSprite, assets1.runSprites
        );
        // Player 2 (Keyset 2)
        player2 = new Fighter(
                P2_START_X, GROUND_Y, availableColors[p2SelectionIndex],
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP,
                KeyEvent.VK_L, KeyEvent.VK_K,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_I, KeyEvent.VK_O,
                assets2.idleSprite, assets2.runSprites
        );

        // Clear P2 keys if in AI mode
        if (gameStateMode == AI_FIGHT) {
            keys[player2.leftKey] = keys[player2.rightKey] = keys[player2.jumpKey] = false;
            keys[player2.attackKey] = keys[player2.superAttackKey] = keys[player2.crouchKey] = false;
            keys[player2.dashFwdKey] = keys[player2.dashBackKey] = false;
        }
    }

    private void resetGame() {
        p1Stocks = INITIAL_STOCKS;
        p2Stocks = INITIAL_STOCKS;
        state = MODE_SELECT;
        winnerText = "";
    }

    // --- 4. GAME LOOP (actionPerformed) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // --- Decrement Menu Input Gate Timer ---
        if (menuTransitionTimer > 0) {
            menuTransitionTimer--;
        }
        // ----------------------------------------

        if (state == FIGHT || state == AI_FIGHT) {

            // --- PAUSE/ROUND RESET LOGIC ---
            if (roundEndTimer > 0) {
                roundEndTimer--;
                if (roundEndTimer <= 0) {
                    player1.resetHealth();
                    player2.resetHealth();

                    // Reset positions for start of round
                    player1.setX(P1_START_X);
                    player1.setY(GROUND_Y - player1.getRect().height);
                    player2.setX(P2_START_X);
                    player2.setY(GROUND_Y - player2.getRect().height);

                    roundEndMessage = "";
                    showFightText = true;
                    fightTimer = FIGHT_SPLASH_DURATION;
                }
                repaint();
                return;
            }

            // --- AI LOGIC FOR PLAYER 2 ---
            if (state == AI_FIGHT) {
                AIOpponent.runAILogic(player1, player2, keys);
                
                // Trigger AI attacks if keys are set
                if (keys[player2.attackKey]) {
                    player2.attack();
                    keys[player2.attackKey] = false;
                }
                if (keys[player2.superAttackKey]) {
                    player2.superAttack();
                    keys[player2.superAttackKey] = false;
                }
            }
            // -----------------------------

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

            // --- ATTACK COLLISION LOGIC ---

            // P1 Attack Check
            if (player1.canHit()) {
                int damage = keys[player1.superAttackKey] ? SUPER_DAMAGE : REGULAR_DAMAGE;
                boolean isSuper = damage == SUPER_DAMAGE;

                if (player1.getAttackRect().intersects(player2.getRect()) && !player2.isKnockedDown()) {
                    player2.takeDamage(damage, player1.getDirection());
                    player1.registerHit(isSuper);
                    player1.gainMeter(isSuper ? Fighter.METER_GAIN_HIT * 2 : Fighter.METER_GAIN_HIT);
                    SoundPlayer.playSound("assets/sounds/" + (player2.isBlocking() ? "block.wav" : (isSuper ? "hit_super.wav" : "hit_regular.wav")));
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
                    SoundPlayer.playSound("assets/sounds/" + (player1.isBlocking() ? "block.wav" : (isSuper ? "hit_super.wav" : "hit_regular.wav")));
                }
            }

            // --- CHECK FOR HEALTH/STOCK LOSS (ROUND/MATCH END) ---
            boolean stockLost = false;
            String finalWinner = null;

            if (player1.getHealth() <= 0) {
                p1Stocks--;
                stockLost = true;
                if (p1Stocks <= 0) {
                    finalWinner = "Player 2 Wins!";
                } else {
                    roundEndMessage = "Player 2 Wins Round!";
                }
            }

            if (player2.getHealth() <= 0) {
                p2Stocks--;
                stockLost = true;
                if (p2Stocks <= 0) {
                    finalWinner = "Player 1 Wins!";
                } else {
                    roundEndMessage = "Player 1 Wins Round!";
                }
            }

            // 1. CHECK FOR GAME OVER (Match End)
            if (finalWinner != null) {
                winnerText = finalWinner;
                state = GAME_OVER;
            }

            // 2. START NEW ROUND (If match is NOT over, but a stock was lost)
            if (stockLost && state != GAME_OVER) {
                roundEndTimer = ROUND_END_PAUSE_DURATION;
            }

            if (showFightText) {
                fightTimer--;
                if (fightTimer <= 0) showFightText = false;
            }
        }
        repaint();
    }

    // --- DRAWING ---

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // --- BACKGROUND DRAWING ---
        Color top = (state >= FIGHT) ? availableStages[selectedStageIndex].topColor : new Color(255, 80, 0);
        Color bottom = (state >= FIGHT) ? availableStages[selectedStageIndex].bottomColor : new Color(255, 200, 0);

        GradientPaint gp = new GradientPaint(0, 0, top, 0, HEIGHT, bottom);
        g2.setPaint(gp);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Ground (Only draw ground in fight/game over states)
        if (state >= FIGHT) {
            g.setColor(Color.GREEN.darker());
            g.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        }

        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.setFont(new Font("Arial", Font.BOLD, 48));

        if (state == START_MENU) {
            g.setFont(new Font("Arial", Font.BOLD, 64));
            fm = g.getFontMetrics();
            drawCenteredString(g, "JAVA FIGHTER", 150, fm);

            g.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Press ENTER to Begin", 350, fm);
            drawCenteredString(g, "Press ESC to Quit", 400, fm);

        } else if (state == MODE_SELECT) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();
            drawCenteredString(g, "SELECT MODE", 150, fm);

            g.setFont(new Font("Arial", Font.PLAIN, 30));
            fm = g.getFontMetrics();
            drawCenteredString(g, "1. LOCAL VERSUS (2 Players)", 250, fm);
            drawCenteredString(g, "2. SINGLE PLAYER (VS AI)", 300, fm);

        } else if (state == CHARACTER_SELECT) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            fm = g.getFontMetrics();
            drawCenteredString(g, "SELECT YOUR FIGHTER", 100, fm);

            drawCharacterSelection(g2, 150, 200, p1SelectionIndex, "PLAYER 1 (A/D)", p2SelectionIndex);
            drawCharacterSelection(g2, WIDTH - 350, 200, p2SelectionIndex, gameStateMode == AI_FIGHT ? "AI OPPONENT" : "PLAYER 2 (Arrows)", p1SelectionIndex);

            // Consolidated Stage Select Display
            g.setFont(new Font("Arial", Font.BOLD, 24));
            fm = g.getFontMetrics();
            drawCenteredString(g, "STAGE: " + availableStages[selectedStageIndex].name, 450, fm);

            g.setFont(new Font("Arial", Font.PLAIN, 18));
            fm = g.getFontMetrics();
            drawCenteredString(g, "Use Player 2's Arrows to Change Stage. Press ENTER to Fight!", 480, fm);


        } else if (state == FIGHT || state == AI_FIGHT) {
            // Draw Fighters
            player1.draw(g);
            player2.draw(g);

            // Draw HUD
            drawHealthBar(g, 50, 50, player1.getHealth(), availableColors[p1SelectionIndex]);
            drawHealthBar(g, WIDTH - 150, 50, player2.getHealth(), availableColors[p2SelectionIndex]);

            drawSuperMeter(g, 50, 70, player1.getSuperMeter(), availableColors[p1SelectionIndex]);
            drawSuperMeter(g, WIDTH - 150, 70, player2.getSuperMeter(), availableColors[p2SelectionIndex]);

            drawStocks(g, 50, 95, p1Stocks, availableColors[p1SelectionIndex]);
            drawStocks(g, WIDTH - 150, 95, p2Stocks, availableColors[p2SelectionIndex]);

            // Draw ROUND END Message
            if (roundEndTimer > 0) {
                g.setFont(new Font("Arial", Font.BOLD, 56));
                fm = g.getFontMetrics();
                drawCenteredString(g, roundEndMessage, 200, fm);
            }
            // Draw "FIGHT!" splash
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

    private void drawCharacterSelection(Graphics2D g2, int x, int y, int selectionIndex, String label, int opponentIndex) {
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString(label, x, y - 40);

        final int boxSize = 60;
        final int spriteSize = Fighter.SPRITE_SIZE; // 24

        for (int i = 0; i < availableColors.length; i++) {
            int boxX = x + i * boxSize;
            int boxY = y;

            // 1. Draw Color Box Background
            g2.setColor(availableColors[i].darker());
            g2.fillRect(boxX, boxY, boxSize - 10, boxSize - 10);

            // 2. Draw Sprite Preview
            BufferedImage sprite = fighterAssetSets.get(i).idleSprite;
            if (sprite != null) {
                int spriteDrawX = boxX + (boxSize - 10) / 2 - spriteSize / 2;
                int spriteDrawY = boxY + (boxSize - 10) / 2 - spriteSize / 2;
                g2.drawImage(sprite, spriteDrawX, spriteDrawY, spriteSize, spriteSize, null);
            }

            // 3. Draw Selection Frame/Indicator
            if (i == selectionIndex) {
                g2.setColor(Color.WHITE);
                Stroke oldStroke = g2.getStroke();
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(boxX - 5, boxY - 5, boxSize, boxSize);
                g2.setStroke(oldStroke);
            }

            // 4. Draw 'X' if opponent has picked this character/color
            if (i == opponentIndex && i != selectionIndex) {
                g2.setColor(Color.RED);
                g2.setFont(new Font("Arial", Font.BOLD, 30));
                g2.drawString("X", boxX + 10, boxY + 40);
            }
        }
    }

    private void drawStocks(Graphics g, int x, int y, int stocks, Color color) {
        final int STOCK_SIZE = 15;
        final int GAP = 5;

        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        g.drawString("STOCKS:", x, y - 5);

        for (int i = 0; i < INITIAL_STOCKS; i++) {
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
        // --- Input Gate Check (Must be at the top) ---
        if (menuTransitionTimer > 0) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
            return;
        }

        keys[e.getKeyCode()] = true;
        int max = availableColors.length;

        if (state == START_MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                keys[KeyEvent.VK_ENTER] = false;
                state = MODE_SELECT;
                menuTransitionTimer = 5;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        } else if (state == MODE_SELECT) {
            if (e.getKeyCode() == KeyEvent.VK_1) {
                keys[KeyEvent.VK_1] = false;
                gameStateMode = FIGHT;
                state = CHARACTER_SELECT;
                menuTransitionTimer = 5;
            } else if (e.getKeyCode() == KeyEvent.VK_2) {
                keys[KeyEvent.VK_2] = false;
                gameStateMode = AI_FIGHT;
                state = CHARACTER_SELECT;
                menuTransitionTimer = 5;
            }
        } else if (state == CHARACTER_SELECT) {
            // P1 Character Selection
            if (e.getKeyCode() == KeyEvent.VK_A) {
                p1SelectionIndex = (p1SelectionIndex - 1 + max) % max;
            } else if (e.getKeyCode() == KeyEvent.VK_D) {
                p1SelectionIndex = (p1SelectionIndex + 1) % max;
            }

            // P2 Character Selection (Local Mode)
            if (gameStateMode == FIGHT) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    p2SelectionIndex = (p2SelectionIndex - 1 + max) % max;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    p2SelectionIndex = (p2SelectionIndex + 1) % max;
                }
            } else {
                // AI Mode: P2 selection follows P1
                p2SelectionIndex = (p1SelectionIndex + 1) % max;

                // AI Mode: Use P2's Arrow Keys for STAGE Selection
                int stageMax = availableStages.length;
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    selectedStageIndex = (selectedStageIndex - 1 + stageMax) % stageMax;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    selectedStageIndex = (selectedStageIndex + 1) % stageMax;
                }
            }

            // Final Transition to Fight
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                keys[KeyEvent.VK_ENTER] = false;

                // START FIGHT! (Consolidated Logic)
                initializeFighters();
                showFightText = true;
                fightTimer = FIGHT_SPLASH_DURATION;

                // Set the final state (FIGHT or AI_FIGHT)
                state = gameStateMode;
            }
        } else if (state == FIGHT || state == AI_FIGHT) {
            if (roundEndTimer == 0) {
                // P1 Input (Always Active)
                if (e.getKeyCode() == player1.attackKey) player1.attack();
                if (e.getKeyCode() == player1.superAttackKey) player1.superAttack();
                if (e.getKeyCode() == player1.dashFwdKey) player1.dashForward();
                if (e.getKeyCode() == player1.dashBackKey) player1.dashBack();

                // P2 Input (Only Active in Local FIGHT Mode)
                if (state == FIGHT) {
                    if (e.getKeyCode() == player2.attackKey) player2.attack();
                    if (e.getKeyCode() == player2.superAttackKey) player2.superAttack();
                    if (e.getKeyCode() == player2.dashFwdKey) player2.dashForward();
                    if (e.getKeyCode() == player2.dashBackKey) player2.dashBack();
                }
            }
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