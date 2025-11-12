import java.awt.*;

public class Fighter {
    // --- Constants ---
    private static final int SCREEN_WIDTH = 800;
    private static final int MOVEMENT_SPEED = 5;
    private static final int JUMP_VELOCITY = -15;
    private static final int GRAVITY_ACCELERATION = 1;
    private static final int GROUND_Y = 400;

    // REGULAR ATTACK CONSTANTS
    private static final int ATTACK_DURATION = 20;
    private static final int ACTIVE_HIT_FRAME = 10;

    // NEW SUPER ATTACK CONSTANTS
    private static final int SUPER_ATTACK_DURATION = 30; // Slightly longer animation
    private static final int SUPER_ACTIVE_HIT_FRAME = 15;
    private static final int SUPER_COOLDOWN_FRAMES = 25 * 60; // 25 seconds * 60 FPS

    private static final int STAND_HEIGHT = 50;
    private static final int CROUCH_HEIGHT = 30;

    // BLOCKING CONSTANTS
    private static final int MAX_BLOCK_COOLDOWN = 120; // 2 seconds cooldown

    // ATTACK HITBOX CONSTANTS
    private static final int ATTACK_HITBOX_WIDTH = 30;
    private static final int ATTACK_HITBOX_HEIGHT = 20;
    private static final int STAND_ATTACK_OFFSET_Y = 15;
    private static final int CROUCH_ATTACK_OFFSET_Y = 5;

    // --- Private Fields (Encapsulation) ---
    private int x, y;
    private final int width = 50;
    private final int MAX_X_BOUND = SCREEN_WIDTH - width;

    private int height;
    private final Color color;
    private int velY = 0;
    private boolean onGround = true;
    private int health = 100;

    // ATTACK COOLDOWNS/STATES
    private int attackCooldown = 0; // Cooldown for the regular attack
    private int superAttackCooldown = 0; // Cooldown for the super attack (25 seconds)
    public boolean isSuperActive = false; // Flag to track which attack is running
    private boolean hasHit = false;

    private int direction = 1;

    private boolean isCrouching = false;
    private boolean isBlocking = false;
    private int blockCooldown = 0;
    private boolean isBlockOnCooldown = false;

    // --- Public Key Fields (NEW superAttackKey) ---
    public final int leftKey, rightKey, jumpKey, attackKey, crouchKey, superAttackKey;

    public Fighter(int x, int y, Color color, int left, int right, int jump, int attack, int crouch, int superAttack) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.leftKey = left;
        this.rightKey = right;
        this.jumpKey = jump;
        this.attackKey = attack;
        this.crouchKey = crouch;
        this.superAttackKey = superAttack; // Store new key
        this.height = STAND_HEIGHT;
    }

    public void update(boolean[] keys) {
        // 1. Block Cooldown Logic
        if (blockCooldown > 0) {
            blockCooldown--;
            if (blockCooldown == 0) {
                isBlockOnCooldown = false;
            }
        }

        // 2. Super Attack Global Cooldown
        if (superAttackCooldown > 0) {
            superAttackCooldown--;
        }

        // 3. Handle Crouching/Blocking state
        isCrouching = keys[crouchKey];
        if (isCrouching && onGround && !isBlockOnCooldown) {
            this.isBlocking = true;
        } else if (!isCrouching || !onGround) {
            this.isBlocking = false;
        }

        // 4. Height and Y adjustment for crouching
        if (isCrouching) {
            this.height = CROUCH_HEIGHT;
            this.y = GROUND_Y - CROUCH_HEIGHT;
        } else {
            this.height = STAND_HEIGHT;
            if (onGround) {
                this.y = GROUND_Y - STAND_HEIGHT;
            }
        }

        // 5. Movement
        if (!isCrouching) {
            if (keys[leftKey]) {
                x -= MOVEMENT_SPEED;
            }
            if (keys[rightKey]) {
                x += MOVEMENT_SPEED;
            }
        }

        // Corner Boundary Check
        if (x < 0) {
            x = 0;
        } else if (x > MAX_X_BOUND) {
            x = MAX_X_BOUND;
        }

        // 6. Jump
        if (keys[jumpKey] && onGround && !isCrouching) {
            velY = JUMP_VELOCITY;
            onGround = false;
        }

        // 7. Gravity
        y += velY;
        if (y >= GROUND_Y - this.height) {
            y = GROUND_Y - this.height;
            velY = 0;
            onGround = true;
        } else {
            velY += GRAVITY_ACCELERATION;
        }

        // 8. Attack Cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
            if (attackCooldown == 0) {
                hasHit = false;
                isSuperActive = false; // Reset super active flag if regular attack finishes
            }
        }
    }

    // --- Getters and Setters ---
    public Rectangle getRect() { return new Rectangle(x, y, width, height); }
    public int getHealth() { return health; }
    public int getDirection() { return direction; }
    public boolean isBlockOnCooldown() { return isBlockOnCooldown; }
    public int getX() { return x; }
    public int getSuperCooldown() { return superAttackCooldown; } // New getter for UI

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

    // --- Attack & Damage Logic ---

    /** Initiates a regular attack if possible. */
    public boolean attack() {
        if (attackCooldown == 0 && onGround && superAttackCooldown == 0) {
            attackCooldown = ATTACK_DURATION;
            hasHit = false;
            isSuperActive = false; // Ensure it's marked as regular
            return true;
        }
        return false;
    }

    /** Initiates a super attack if cooldown is finished. */
    public boolean superAttack() {
        // Can only start if both attack cooldowns are 0 AND on the ground
        if (attackCooldown == 0 && superAttackCooldown == 0 && onGround) {
            attackCooldown = SUPER_ATTACK_DURATION; // Use attackCooldown as the active timer
            hasHit = false;
            isSuperActive = true; // Mark as super attack
            superAttackCooldown = SUPER_COOLDOWN_FRAMES; // Start the 25s global cooldown
            return true;
        }
        return false;
    }

    /** Calculates the attack hitbox based on the fighter's state and active attack type. */
    public Rectangle getAttackRect() {
        if (isAttackActive()) {
            // Determine vertical offset based on crouching (applies to both types)
            int offsetY;
            if (isCrouching) {
                offsetY = CROUCH_ATTACK_OFFSET_Y;
            } else {
                offsetY = STAND_ATTACK_OFFSET_Y;
            }

            // Super attack has a larger hitbox area
            int rectWidth = isSuperActive ? ATTACK_HITBOX_WIDTH * 2 : ATTACK_HITBOX_WIDTH;
            int rectHeight = isSuperActive ? ATTACK_HITBOX_HEIGHT + 10 : ATTACK_HITBOX_HEIGHT;

            int offsetX = direction == 1 ? width : -rectWidth;

            return new Rectangle(
                    x + offsetX,
                    y + offsetY,
                    rectWidth,
                    rectHeight
            );
        }
        return new Rectangle(0, 0, 0, 0);
    }

    /** Checks if the currently running attack animation is in its hit window. */
    public boolean isAttackActive() {
        int hitFrame = isSuperActive ? SUPER_ACTIVE_HIT_FRAME : ACTIVE_HIT_FRAME;
        int duration = isSuperActive ? SUPER_ATTACK_DURATION : ATTACK_DURATION;

        // Attack is active during the second half of the animation
        return attackCooldown > duration - hitFrame && attackCooldown > 0;
    }

    public boolean canHit() {
        return isAttackActive() && !hasHit;
    }

    public void registerHit() {
        hasHit = true;
    }

    public void takeDamage(int damage, int attackerDirection) {
        boolean isFacingAttack = (attackerDirection != this.direction);

        if (isBlocking && isFacingAttack) {
            blockCooldown = MAX_BLOCK_COOLDOWN;
            isBlockOnCooldown = true;
            this.isBlocking = false;
        } else {
            this.health -= damage;
        }

        if (this.health < 0) {
            this.health = 0;
        }
    }

    // --- Drawing ---
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x, y, width, height);

        // Draw block indicators
        if (isBlocking) {
            g.setColor(new Color(50, 200, 255, 180)); // Active Block Shield (Cyan)
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        } else if (isBlockOnCooldown()) {
            g.setColor(new Color(255, 50, 50, 80)); // Block Cooldown Indicator (Faded Red)
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        }

        // Draw the attack indicator
        if (isAttackActive()) {
            g.setColor(isSuperActive ? Color.MAGENTA : Color.YELLOW); // Super Attack is MAGENTA
            Rectangle ar = getAttackRect();
            if (ar.width > 0) {
                g.fillRect(ar.x, ar.y, ar.width, ar.height);
            }
        }
    }
}