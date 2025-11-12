import java.awt.*;

public class Fighter {
    // --- Constants ---
    private static final int SCREEN_WIDTH = 800;
    private static final int MOVEMENT_SPEED = 5;
    private static final int JUMP_VELOCITY = -15;
    private static final int GRAVITY_ACCELERATION = 1;
    private static final int GROUND_Y = 400;

    // ATTACK CONSTANTS
    private static final int ATTACK_DURATION = 20;
    private static final int ACTIVE_HIT_FRAME = 10;
    private static final int SUPER_ATTACK_DURATION = 30;
    private static final int SUPER_ACTIVE_HIT_FRAME = 15;
    private static final int SUPER_COOLDOWN_FRAMES = 25 * 60; // 1500 frames (25 seconds)

    private static final int STAND_HEIGHT = 50;
    private static final int CROUCH_HEIGHT = 30;

    // BLOCKING CONSTANTS
    private static final int MAX_BLOCK_COOLDOWN = 120; // 2 seconds

    // HITBOX CONSTANTS
    private static final int ATTACK_HITBOX_WIDTH = 30;
    private static final int ATTACK_HITBOX_HEIGHT = 20;
    private static final int STAND_ATTACK_OFFSET_Y = 15;
    private static final int CROUCH_ATTACK_OFFSET_Y = 5;

    // DASH/BACKSTEP CONSTANTS
    private static final int DASH_DISTANCE_PER_FRAME = 12;
    private static final int DASH_DURATION = 8;
    private static final int DASH_COOLDOWN = 45;

    // KNOCKBACK & STUN CONSTANTS
    private static final int KNOCKBACK_STRENGTH = 12;
    private static final int REGULAR_STUN_DURATION = 10; // New: For regular hits
    private static final float KNOCKBACK_DECAY = 0.85f;

    // --- KNOCKDOWN CONSTANTS ---
    private static final int KNOCKDOWN_DURATION = 90;
    private static final int WAKEUP_INVULNERABILITY = 15;
    private static final int FALL_VELOCITY = -10; // Initial upward velocity when knocked back

    // --- Private Fields (Encapsulation) ---
    private int x, y;
    private final int width = 50;
    private final int MAX_X_BOUND = SCREEN_WIDTH - width;

    private int height;
    private final Color color;
    private int velY = 0;
    private int velX = 0;
    private boolean onGround = true;
    private int health = 100;

    // ATTACK COOLDOWNS/STATES
    private int attackCooldown = 0;
    private int superAttackCooldown = 0;
    private boolean isSuperActive = false;
    private boolean hasHit = false;

    private int direction = 1;

    private boolean isCrouching = false;
    private boolean isBlocking = false;
    private int blockCooldown = 0;
    private boolean isBlockOnCooldown = false;

    // DASH FIELDS
    private int dashTimer = 0;
    private int dashCooldownTimer = 0;
    private int dashDirection = 0;
    private boolean isDashing = false;

    // STUN/KNOCKDOWN FIELDS
    private int stunTimer = 0;
    private boolean isStunned = false;
    private int knockdownTimer = 0; // NEW
    private boolean isKnockedDown = false; // NEW
    private int invulnerabilityTimer = 0; // NEW

    // --- Public Key Fields ---
    public final int leftKey, rightKey, jumpKey, attackKey, crouchKey, superAttackKey;
    public final int dashForwardKey, dashBackKey;

    public Fighter(int x, int y, Color color, int left, int right, int jump, int attack, int crouch, int superAttack, int dashForward, int dashBack) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.leftKey = left;
        this.rightKey = right;
        this.jumpKey = jump;
        this.attackKey = attack;
        this.crouchKey = crouch;
        this.superAttackKey = superAttack;
        this.dashForwardKey = dashForward;
        this.dashBackKey = dashBack;
        this.height = STAND_HEIGHT;
    }

    public void update(boolean[] keys) {
        // 1. Cooldown Timers
        if (blockCooldown > 0) {
            blockCooldown--;
            if (blockCooldown == 0) {
                isBlockOnCooldown = false;
            }
        }
        if (superAttackCooldown > 0) {
            superAttackCooldown--;
        }

        // --- KNOCKDOWN LOGIC (Highest Priority State) ---
        if (knockdownTimer > 0) {
            knockdownTimer--;
            isKnockedDown = true;
            isStunned = false;

            // Force fall until grounded
            if (y < GROUND_Y - height) {
                y += velY;
                velY += GRAVITY_ACCELERATION;
            } else {
                // Grounded: stop movement
                y = GROUND_Y - height;
                velY = 0;
            }

            // Still apply knockback friction while falling/down
            x += velX;
            if (velX != 0) {
                velX = (int)(velX * KNOCKBACK_DECAY);
                if (Math.abs(velX) < 1) velX = 0;
            }

            // Corner Boundary Check (essential during knockdown)
            if (x < 0) x = 0; else if (x > MAX_X_BOUND) x = MAX_X_BOUND;

            return; // EXIT: NO other actions are processed during knockdown
        } else if (isKnockedDown) {
            // End of knockdown: Stand up, start invulnerability, and reset state
            isKnockedDown = false;
            invulnerabilityTimer = WAKEUP_INVULNERABILITY;
        }

        // --- Invulnerability Timer ---
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer--;
        }

        // --- STUN LOGIC ---
        if (stunTimer > 0) {
            stunTimer--;
            isStunned = true;
        } else {
            isStunned = false;
        }
        // ------------------

        // --- DASH LOGIC ---
        if (dashCooldownTimer > 0) {
            dashCooldownTimer--;
        }

        if (dashTimer > 0) {
            dashTimer--;
            isDashing = true;
            x += dashDirection * DASH_DISTANCE_PER_FRAME;
        } else {
            isDashing = false;
        }
        // ------------------

        // 2. Crouching/Blocking state
        isCrouching = keys[crouchKey];
        if (isCrouching && onGround && !isBlockOnCooldown) {
            this.isBlocking = true;
        } else if (!isCrouching || !onGround || isStunned) {
            this.isBlocking = false;
        }

        // 3. Height and Y adjustment
        if (isCrouching) {
            this.height = CROUCH_HEIGHT;
            this.y = GROUND_Y - CROUCH_HEIGHT;
        } else {
            this.height = STAND_HEIGHT;
            if (onGround) {
                this.y = GROUND_Y - STAND_HEIGHT;
            }
        }

        // 4. Movement: only allowed if NOT dashing AND NOT stunned
        if (!isCrouching && !isDashing && !isStunned) {
            if (keys[leftKey]) {
                x -= MOVEMENT_SPEED;
            }
            if (keys[rightKey]) {
                x += MOVEMENT_SPEED;
            }
        }

        // --- KNOCKBACK MOVEMENT & FRICTION ---
        x += velX;
        if (velX != 0) {
            velX = (int)(velX * KNOCKBACK_DECAY);
            if (Math.abs(velX) < 1) velX = 0;
        }
        // ------------------------------------------

        // Corner Boundary Check
        if (x < 0) {
            x = 0;
            velX = 0;
        } else if (x > MAX_X_BOUND) {
            x = MAX_X_BOUND;
            velX = 0;
        }

        // 5. Jump: only allowed if NOT dashing AND NOT stunned
        if (keys[jumpKey] && onGround && !isCrouching && !isDashing && !isStunned) {
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
                isSuperActive = false;
            }
        }
    }

    // --- DASH METHODS ---
    private boolean initiateDash(int directionMultiplier) {
        if (onGround && !isDashing && dashCooldownTimer == 0 && !isStunned && !isKnockedDown) {
            dashTimer = DASH_DURATION;
            dashCooldownTimer = DASH_COOLDOWN;
            dashDirection = this.direction * directionMultiplier;
            return true;
        }
        return false;
    }

    public boolean dashForward() {
        return initiateDash(1);
    }

    public boolean dashBack() {
        return initiateDash(-1);
    }

    // --- Getters and Setters ---
    public Rectangle getRect() { return new Rectangle(x, y, width, height); }
    public int getHealth() { return health; }
    public int getDirection() { return direction; }
    public boolean isBlockOnCooldown() { return isBlockOnCooldown; }
    public boolean isStunned() { return isStunned; }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0; } // NEW Getter for Drawing
    public int getX() { return x; }
    public int getSuperCooldown() { return superAttackCooldown; }
    public boolean isSuperActive() { return isSuperActive; }

    public void setDirection(int newDirection) {
        if (newDirection == 1 || newDirection == -1) {
            this.direction = newDirection;
        }
    }

    public void setX(int newX) {
        this.x = newX;
        if (x < 0) x = 0; else if (x > MAX_X_BOUND) x = MAX_X_BOUND;
    }

    // --- Attack & Damage Logic ---
    public boolean attack() {
        if (attackCooldown == 0 && !isDashing && !isStunned && !isKnockedDown) {
            attackCooldown = ATTACK_DURATION;
            hasHit = false;
            isSuperActive = false;
            return true;
        }
        return false;
    }

    public boolean superAttack() {
        if (attackCooldown == 0 && superAttackCooldown == 0 && !isDashing && !isStunned && !isKnockedDown) {
            attackCooldown = SUPER_ATTACK_DURATION;
            hasHit = false;
            isSuperActive = true;
            superAttackCooldown = SUPER_COOLDOWN_FRAMES;
            return true;
        }
        return false;
    }

    public Rectangle getAttackRect() {
        if (isAttackActive()) {
            int offsetY;
            if (isCrouching) {
                offsetY = CROUCH_ATTACK_OFFSET_Y;
            } else {
                offsetY = STAND_ATTACK_OFFSET_Y;
            }

            int rectWidth = isSuperActive ? ATTACK_HITBOX_WIDTH * 2 : ATTACK_HITBOX_WIDTH;
            int rectHeight = isSuperActive ? ATTACK_HITBOX_HEIGHT + 10 : ATTACK_HITBOX_HEIGHT;

            int offsetX = direction == 1 ? width : -rectWidth;

            return new Rectangle(x + offsetX, y + offsetY, rectWidth, rectHeight);
        }
        return new Rectangle(0, 0, 0, 0);
    }

    public boolean isAttackActive() {
        int hitFrame = isSuperActive ? SUPER_ACTIVE_HIT_FRAME : ACTIVE_HIT_FRAME;
        int duration = isSuperActive ? SUPER_ATTACK_DURATION : ATTACK_DURATION;
        return attackCooldown > duration - hitFrame && attackCooldown > 0;
    }

    public boolean canHit() {
        return isAttackActive() && !hasHit;
    }

    public void registerHit() {
        hasHit = true;
    }

    public void takeDamage(int damage, int attackerDirection) {
        if (invulnerabilityTimer > 0 || isKnockedDown) {
            return; // Ignore damage if invulnerable or already down
        }

        boolean isFacingAttack = (attackerDirection != this.direction);

        if (isBlocking && isFacingAttack) {
            blockCooldown = MAX_BLOCK_COOLDOWN;
            isBlockOnCooldown = true;
            this.isBlocking = false;
        } else {
            this.health -= damage;

            // --- KNOCKDOWN TRIGGER ---
            if (damage >= 50) {
                isStunned = false;
                knockdownTimer = KNOCKDOWN_DURATION;
                onGround = false;
                velY = FALL_VELOCITY; // Launch upwards
                velX = attackerDirection * KNOCKBACK_STRENGTH;
            } else {
                // Regular hit stun/knockback
                stunTimer = REGULAR_STUN_DURATION;
                velX = attackerDirection * (KNOCKBACK_STRENGTH / 3);
            }
        }

        if (this.health < 0) {
            this.health = 0;
        }
    }

    // --- Drawing ---
    public void draw(Graphics g) {
        // Invulnerability Flash (Highest Priority Visual)
        if (isInvulnerable() && (invulnerabilityTimer % 4 == 0)) { // Flash white every 4 frames
            g.setColor(Color.WHITE);
            g.fillOval(x, y, width, height);
            return;
        }

        // Draw character slightly brighter if stunned (visual feedback)
        if (isStunned) {
            g.setColor(Color.WHITE);
            g.fillOval(x - 5, y - 5, width + 10, height + 10);
            g.setColor(color);
            g.fillOval(x, y, width, height);
        } else {
            g.setColor(color);
            g.fillOval(x, y, width, height);
        }

        // Draw block indicators
        if (isBlocking) {
            g.setColor(new Color(50, 200, 255, 180));
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        } else if (isBlockOnCooldown()) {
            g.setColor(new Color(255, 50, 50, 80));
            g.fillRect(x - 5, y - 5, width + 10, height + 10);
        }

        // Draw the attack indicator
        if (isAttackActive()) {
            g.setColor(isSuperActive ? Color.MAGENTA : Color.YELLOW);
            Rectangle ar = getAttackRect();
            if (ar.width > 0) {
                g.fillRect(ar.x, ar.y, ar.width, ar.height);
            }
        }
    }
}