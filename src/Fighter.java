import java.awt.*;

public class Fighter {
    // --- Constants ---
    private static final int MOVEMENT_SPEED = 5;
    private static final int JUMP_VELOCITY = -15;
    private static final int GRAVITY_ACCELERATION = 1;
    private static final int GROUND_Y = 400;
    private static final int ATTACK_DURATION = 20;
    private static final int ACTIVE_HIT_FRAME = 10;
    private static final int STAND_HEIGHT = 50;
    private static final int CROUCH_HEIGHT = 30;

    // BLOCKING CONSTANTS
    private static final int MAX_BLOCK_COOLDOWN = 120; // 2 seconds cooldown

    // NEW ATTACK HITBOX CONSTANTS
    private static final int STAND_ATTACK_OFFSET_Y = 15; // Vertical position from top (mid-level hit)
    private static final int CROUCH_ATTACK_OFFSET_Y = 5;  // Vertical position from top (low hit)
    private static final int ATTACK_HITBOX_WIDTH = 30;
    private static final int ATTACK_HITBOX_HEIGHT = 20;

    // --- Private Fields (Encapsulation) ---
    private int x, y;
    private final int width = 50;
    private int height;
    private final Color color;
    private int velY = 0;
    private boolean onGround = true;
    private int health = 100;
    private int attackCooldown = 0;

    private int direction = 1;
    private boolean hasHit = false;

    private boolean isCrouching = false;
    private boolean isBlocking = false;
    private int blockCooldown = 0;
    private boolean isBlockOnCooldown = false;

    // --- Public Key Fields ---
    public final int leftKey, rightKey, jumpKey, attackKey, crouchKey;

    public Fighter(int x, int y, Color color, int left, int right, int jump, int attack, int crouch) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.leftKey = left;
        this.rightKey = right;
        this.jumpKey = jump;
        this.attackKey = attack;
        this.crouchKey = crouch;
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

        // 2. Handle Crouching/Blocking state
        isCrouching = keys[crouchKey];

        // Block state is active only if crouching, on the ground, and NOT on cooldown
        if (isCrouching && onGround && !isBlockOnCooldown) {
            this.isBlocking = true;
        }
        // If the key is released OR we are not on the ground, the block ends
        else if (!isCrouching || !onGround) {
            this.isBlocking = false;
        }

        // 3. Height and Y adjustment for crouching
        if (isCrouching) {
            this.height = CROUCH_HEIGHT;
            this.y = GROUND_Y - CROUCH_HEIGHT;
        } else {
            this.height = STAND_HEIGHT;
            if (onGround) {
                this.y = GROUND_Y - STAND_HEIGHT;
            }
        }

        // 4. Movement (disabled while crouching/blocking)
        if (!isCrouching) {
            if (keys[leftKey]) {
                x -= MOVEMENT_SPEED;
                direction = -1;
            }
            if (keys[rightKey]) {
                x += MOVEMENT_SPEED;
                direction = 1;
            }
        }

        // 5. Jump (only if not crouching)
        if (keys[jumpKey] && onGround && !isCrouching) {
            velY = JUMP_VELOCITY;
            onGround = false;
        }

        // 6. Gravity
        y += velY;
        if (y >= GROUND_Y - this.height) {
            y = GROUND_Y - this.height;
            velY = 0;
            onGround = true;
        } else {
            velY += GRAVITY_ACCELERATION;
        }

        // 7. Attack Cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
            if (attackCooldown == 0) {
                hasHit = false;
            }
        }
    }

    // --- Getters ---
    public Rectangle getRect() { return new Rectangle(x, y, width, height); }
    public int getHealth() { return health; }
    public int getDirection() { return direction; }
    public boolean isBlockOnCooldown() { return isBlockOnCooldown; }

    // --- Attack & Damage Logic ---

    /**
     * Calculates the attack hitbox based on the fighter's state (standing or crouching).
     */
    public Rectangle getAttackRect() {
        if (isAttackActive()) {

            int offsetY;

            // Determine the vertical position based on crouching state
            if (isCrouching) {
                offsetY = CROUCH_ATTACK_OFFSET_Y; // Low Attack
            } else {
                offsetY = STAND_ATTACK_OFFSET_Y; // Mid Attack
            }

            // Horizontal placement (based on facing direction)
            int offsetX = direction == 1 ? width : -ATTACK_HITBOX_WIDTH;

            return new Rectangle(
                    x + offsetX,
                    y + offsetY,
                    ATTACK_HITBOX_WIDTH,
                    ATTACK_HITBOX_HEIGHT
            );
        }
        return new Rectangle(0, 0, 0, 0);
    }

    /**
     * Initiates an attack if the cooldown is finished AND the fighter is on the ground.
     */
    public boolean attack() {
        // Attack is only allowed if on the ground and cooldown is clear
        if (attackCooldown == 0 && onGround) {
            attackCooldown = ATTACK_DURATION;
            hasHit = false;
            return true;
        }
        return false;
    }

    public boolean isAttackActive() {
        return attackCooldown > ATTACK_DURATION - ACTIVE_HIT_FRAME && attackCooldown > 0;
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
            // Successful block: 0 damage, initiate cooldown, and forcibly disable active block
            blockCooldown = MAX_BLOCK_COOLDOWN;
            isBlockOnCooldown = true;
            this.isBlocking = false;
        } else {
            // Take full damage
            this.health -= damage;
        }

        if (this.health < 0) {
            this.health = 0;
        }
    }

    // --- Drawing ---
    public void draw(Graphics g) {
        // Draw the fighter (circle)
        g.setColor(color);
        g.fillOval(x, y, width, height);

        // Draw block indicators
        if (isBlocking) {
            // Active Block Shield (Cyan)
            g.setColor(new Color(50, 200, 255, 180));
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        } else if (isBlockOnCooldown()) {
            // Block Cooldown Indicator (Faded Red)
            g.setColor(new Color(255, 50, 50, 80));
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        }

        // Draw the attack indicator
        if (isAttackActive()) {
            g.setColor(Color.YELLOW);
            Rectangle ar = getAttackRect();
            if (ar.width > 0) {
                g.fillRect(ar.x, ar.y, ar.width, ar.height);
            }
        }
    }
}