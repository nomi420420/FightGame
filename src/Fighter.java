import java.awt.*;
import java.awt.image.BufferedImage;

public class Fighter {
    // --- Constants ---
    // FIXED: Using the actual asset height (24px)
    public static final int SPRITE_SIZE = 24;
    private static final int SCREEN_WIDTH = 800;
    private static final int MOVEMENT_SPEED = 5;
    private static final int JUMP_VELOCITY = -15;
    private static final int GRAVITY_ACCELERATION = 1;
    private static final int GROUND_Y = 400;

    // Attack and Animation
    private static final int ATTACK_DURATION = 20;
    private static final int ACTIVE_HIT_FRAME = 10;
    // FIXED: Height now matches sprite size
    private static final int STAND_HEIGHT = SPRITE_SIZE;
    private static final int CROUCH_HEIGHT = 16; // Scaled crouching height

    // Blocking
    private static final int MAX_BLOCK_COOLDOWN = 120; // 2 seconds cooldown

    // Attack Hitbox Constants
    private static final int ATTACK_HITBOX_WIDTH = 30;
    private static final int ATTACK_HITBOX_HEIGHT = 20;
    private static final int STAND_ATTACK_OFFSET_Y = 15;
    private static final int CROUCH_ATTACK_OFFSET_Y = 5;

    // Dashing
    private static final int DASH_DISTANCE = 100; // Total distance to travel
    private static final int DASH_FRAMES = 10;    // Frames the dash lasts (10 FPS speed)
    private static final int DASH_COOLDOWN = 30;  // Cooldown after dash (0.5s)

    // --- KNOCKDOWN & STUN CONSTANTS ---
    private static final int REGULAR_STUN_DURATION = 8;
    private static final int KNOCKBACK_STRENGTH_LIGHT = 6;
    private static final int KNOCKBACK_STRENGTH_HEAVY = 12;
    private static final int KNOCKDOWN_DURATION = 90;
    private static final int WAKEUP_INVULNERABILITY = 15;
    private static final int FALL_VELOCITY = 10;

    // --- SUPER METER CONSTANTS ---
    private static final int MAX_METER = 100;
    public static final int SUPER_ATTACK_COST = 50;
    public static final double METER_GAIN_HIT = 8.0;
    public static final double METER_GAIN_TAKEN = 4.0;

    // --- ANIMATION CONSTANTS ---
    public static final int RUN_FRAME_COUNT = 4;
    private static final int RUN_ANIMATION_SPEED = 5;

    // --- Private Fields (Encapsulation) ---
    private int x, y;
    private final int width = SPRITE_SIZE; // Use SPRITE_SIZE
    private final int MAX_X_BOUND = SCREEN_WIDTH - width;

    private int height;
    private final Color color;
    private BufferedImage idleSprite;
    private BufferedImage[] runSprites;

    private int velY = 0;
    private float velX = 0;
    private boolean onGround = true;
    private int health = 100;
    private float superMeter = 0;

    private int direction = 1;
    private boolean hasHit = false;

    private boolean isCrouching = false;
    private boolean isBlocking = false;
    private int blockCooldown = 0;
    private boolean isBlockOnCooldown = false;

    // Knockdown & Stun
    private int stunTimer = 0;
    private int knockdownTimer = 0;
    private boolean isKnockedDown = false;
    private int invulnerabilityTimer = 0;

    // Dashing
    private int dashTimer = 0;
    private boolean isDashing = false;
    private int dashCooldown = 0;

    // Attack and Animation
    private int attackCooldown = 0;
    private int animationTimer = 0;
    private int frameIndex = 0;
    private boolean isRunning = false;

    // --- Public Key Fields ---
    public final int leftKey, rightKey, jumpKey, attackKey, superAttackKey, crouchKey, dashFwdKey, dashBackKey;

    public Fighter(int x, int y, Color color, int left, int right, int jump, int attack, int superAttack, int crouch, int dashFwd, int dashBack, BufferedImage idleSprite, BufferedImage[] runSprites) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.idleSprite = idleSprite;
        this.runSprites = runSprites;
        this.leftKey = left;
        this.rightKey = right;
        this.jumpKey = jump;
        this.attackKey = attack;
        this.superAttackKey = superAttack;
        this.crouchKey = crouch;
        this.dashFwdKey = dashFwd;
        this.dashBackKey = dashBack;
        this.height = STAND_HEIGHT;
    }

    public void update(boolean[] keys) {

        // --- ANIMATION TIMER (Always run) ---
        animationTimer++;
        if (animationTimer >= RUN_ANIMATION_SPEED) {
            animationTimer = 0;
            frameIndex = (frameIndex + 1) % RUN_FRAME_COUNT;
        }
        // ------------------------------------

        // --- 1. HANDLE KNOCKDOWN STATE ---
        if (knockdownTimer > 0) {
            knockdownTimer--;

            if (y < GROUND_Y - height) {
                y += velY;
                velY += GRAVITY_ACCELERATION;
            } else {
                y = GROUND_Y - height;
                velY = 0;
                isKnockedDown = true;
            }
            x += velX;
            velX *= 0.85;

            if (knockdownTimer == 0) {
                isKnockedDown = false;
                invulnerabilityTimer = WAKEUP_INVULNERABILITY;
            }
            return;
        }

        // --- 2. HANDLE STUN/KNOCKBACK ---
        if (stunTimer > 0) {
            stunTimer--;
            x += velX;
            velX *= 0.85;

            if (Math.abs(velX) < 1.0) {
                velX = 0;
            }
            return;
        }

        // --- 3. HANDLE DASH STATE ---
        if (dashCooldown > 0) dashCooldown--;

        if (dashTimer > 0) {
            dashTimer--;
            x += velX;
            if (dashTimer == 0) {
                isDashing = false;
                velX = 0;
            }
        }

        // --- 4. INPUT HANDLING (Standard Movement) ---

        // 4a. Block Cooldown Logic
        if (blockCooldown > 0) {
            blockCooldown--;
            if (blockCooldown == 0) {
                isBlockOnCooldown = false;
            }
        }

        // 4b. Handle Crouching/Blocking state
        isCrouching = keys[crouchKey];
        if (isCrouching && onGround && !isBlockOnCooldown) {
            this.isBlocking = true;
        } else if (!isCrouching || !onGround) {
            this.isBlocking = false;
        }

        // 4c. Height and Y adjustment for crouching
        if (isCrouching) {
            this.height = CROUCH_HEIGHT;
            this.y = GROUND_Y - CROUCH_HEIGHT;
        } else {
            this.height = STAND_HEIGHT;
            if (onGround) {
                this.y = GROUND_Y - STAND_HEIGHT;
            }
        }

        // 4d. User Movement and Running State Check (Only move if NOT dashing, NOT stunned, and velX has stopped)
        isRunning = false;
        if (!isDashing && velX == 0) {
            if (!isCrouching) {
                if (keys[leftKey]) {
                    x -= MOVEMENT_SPEED;
                    isRunning = true; // Set run flag
                }
                if (keys[rightKey]) {
                    x += MOVEMENT_SPEED;
                    isRunning = true; // Set run flag
                }
            }
        }

        // --- 5. PHYSICS & TIMERS ---

        // Corner Boundary Check
        if (x < 0) {
            x = 0;
        } else if (x > MAX_X_BOUND) {
            x = MAX_X_BOUND;
        }

        // Jump
        if (!isCrouching && !isDashing && keys[jumpKey] && onGround) {
            velY = JUMP_VELOCITY;
            onGround = false;
        }

        // Gravity
        y += velY;
        if (y >= GROUND_Y - this.height) {
            y = GROUND_Y - this.height;
            velY = 0;
            onGround = true;
        } else {
            velY += GRAVITY_ACCELERATION;
        }

        // Attack Cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
            if (attackCooldown == 0) {
                hasHit = false;
            }
        }

        // Invulnerability Timer
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer--;
        }

        // Meter Clamping
        if (superMeter > MAX_METER) superMeter = MAX_METER;
        if (superMeter < 0) superMeter = 0;
    }

    // --- Getters and Setters ---
    public Rectangle getRect() { return new Rectangle(x, y, width, height); }
    public int getHealth() { return health; }
    public int getDirection() { return direction; }
    public boolean isBlockOnCooldown() { return isBlockOnCooldown; }
    public int getX() { return x; }
    public float getSuperMeter() { return superMeter; }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0; }
    public boolean isKnockedDown() { return isKnockedDown || knockdownTimer > 0; }

    /** Resets health and all status effects for a new round. */
    public void resetHealth() {
        this.health = 100;
        this.isKnockedDown = false;
        this.knockdownTimer = 0;
        this.stunTimer = 0;
        this.velX = 0;
        this.velY = 0;
        this.invulnerabilityTimer = 0;
        this.attackCooldown = 0;
        this.isDashing = false;
        this.dashCooldown = 0;
        this.superMeter = 0;
    }

    public void setDirection(int newDirection) {
        if (newDirection == 1 || newDirection == -1) {
            this.direction = newDirection;
        }
    }

    public void setX(int newX) {
        this.x = newX;
        if (x < 0) {
            x = 0;
        } else if (x > MAX_X_BOUND) {
            x = MAX_X_BOUND;
        }
    }

    /** Allows GamePanel to reset vertical position during round start. */
    public void setY(int newY) {
        this.y = newY;
    }

    public void gainMeter(double amount) {
        this.superMeter += amount;
        if (this.superMeter > MAX_METER) this.superMeter = MAX_METER;
    }

    // --- Dash Methods (Unchanged) ---

    private boolean startDash(int directionMultiplier) {
        if (dashCooldown == 0 && stunTimer == 0 && onGround) {
            isDashing = true;
            dashTimer = DASH_FRAMES;
            dashCooldown = DASH_COOLDOWN;
            velX = (float) (directionMultiplier * DASH_DISTANCE) / DASH_FRAMES;
            return true;
        }
        return false;
    }

    public boolean dashForward() {
        return startDash(this.direction);
    }

    public boolean dashBack() {
        return startDash(-this.direction);
    }

    // --- Attack & Damage Logic (Unchanged) ---

    public Rectangle getAttackRect() {
        if (isAttackActive()) {
            int offsetY;
            if (isCrouching) {
                offsetY = CROUCH_ATTACK_OFFSET_Y;
            } else {
                offsetY = STAND_ATTACK_OFFSET_Y;
            }

            int offsetX = direction == 1 ? width : -ATTACK_HITBOX_WIDTH;

            return new Rectangle(x + offsetX, y + offsetY, ATTACK_HITBOX_WIDTH, ATTACK_HITBOX_HEIGHT);
        }
        return new Rectangle(0, 0, 0, 0);
    }

    public boolean attack() {
        if (attackCooldown == 0 && stunTimer == 0) {
            attackCooldown = ATTACK_DURATION;
            hasHit = false;
            return true;
        }
        return false;
    }

    public boolean superAttack() {
        if (superMeter >= SUPER_ATTACK_COST && attackCooldown == 0 && stunTimer == 0) {
            attackCooldown = ATTACK_DURATION;
            superMeter -= SUPER_ATTACK_COST;
            hasHit = false;
            return true;
        }
        return false;
    }

    public boolean isAttackActive() {
        return attackCooldown > ATTACK_DURATION - ACTIVE_HIT_FRAME && attackCooldown > 0;
    }

    public boolean canHit() {
        return isAttackActive() && hasHit == false;
    }

    public void registerHit(boolean isSuper) {
        hasHit = true;
    }

    public void takeDamage(int damage, int attackerDirection) {
        if (isInvulnerable() || knockdownTimer > 0) {
            return;
        }

        boolean isFacingAttack = (attackerDirection != this.direction);

        if (isBlocking && isFacingAttack) {
            blockCooldown = MAX_BLOCK_COOLDOWN;
            isBlockOnCooldown = true;
            this.isBlocking = false;
            return;
        }

        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }

        gainMeter(METER_GAIN_TAKEN);

        boolean isSuper = damage == 50;
        int knockbackSign = attackerDirection * -1;

        if (isSuper) {
            knockdownTimer = KNOCKDOWN_DURATION;
            velX = (float) (knockbackSign * KNOCKBACK_STRENGTH_HEAVY);

            if (!onGround) {
                velY = FALL_VELOCITY;
            }

        } else {
            stunTimer = REGULAR_STUN_DURATION;
            velX = (float) (knockbackSign * KNOCKBACK_STRENGTH_LIGHT);
        }
    }

    // --- Drawing ---
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // 1. Determine which sprite frame to draw
        BufferedImage currentSprite = idleSprite;

        // Prioritize Run Animation if applicable
        if (runSprites != null && runSprites[0] != null && isRunning && onGround) {
            currentSprite = runSprites[frameIndex];
        }
        // NOTE: Add logic here later for jumping, attacking, etc.

        // 2. Invulnerability Flash Check
        boolean isFlashing = isInvulnerable() && (invulnerabilityTimer % 5 != 0);

        if (!isFlashing) {
            if (currentSprite != null) {
                int drawX = x;
                int drawWidth = width;

                // FLIPPING LOGIC: Draw the sprite inverted if facing left
                if (direction == -1) {
                    drawX = x + width;
                    drawWidth = -width;
                }

                // Draw the sprite, using the fighter's width/height constraints
                g2.drawImage(currentSprite, drawX, y, drawWidth, height, null);
            } else {
                // FALLBACK: Draw colored circle if sprite not found
                g.setColor(color);
                g.fillOval(x, y, width, height);
            }
        }

        // Draw status indicators over the fighter (always draw)

        // Draw block indicators
        if (isBlocking) {
            g.setColor(new Color(50, 200, 255, 180));
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        } else if (isBlockOnCooldown()) {
            g.setColor(new Color(255, 50, 50, 80));
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        }

        // Draw the attack indicator (hitbox visualization)
        if (isAttackActive()) {
            g.setColor(Color.YELLOW);
            Rectangle ar = getAttackRect();
            if (ar.width > 0) {
                g.fillRect(ar.x, ar.y, ar.width, ar.height);
            }
        }
    }
}