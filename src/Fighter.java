import java.awt.*;
import java.awt.image.BufferedImage;

public class Fighter {
    // --- Constants ---
    private static final int SCREEN_WIDTH = 800;
    private static final int MOVEMENT_SPEED = 5;
    private static final int JUMP_VELOCITY = -15;
    private static final int GRAVITY_ACCELERATION = 1;
    private static final int GROUND_Y = 500; // UPDATED: Moved ground line down (closer to screen bottom)

    // Attack and Animation
    public static final int SPRITE_SIZE = 100; // Visual size of the fighter (pixels)

    // --- NEW CONSTANT FOR VISUAL ALIGNMENT ---
    private static final int SPRITE_VERTICAL_OFFSET = 40; // Shifts the drawn sprite down 40 px

    // --- COLLISION CONSTANTS ---
    private static final int COLLISION_WIDTH = 20;
    private static final int COLLISION_OFFSET_X = (SPRITE_SIZE - COLLISION_WIDTH) / 2;

    private static final int ATTACK_DURATION = 20;
    private static final int ACTIVE_HIT_FRAME = 10;
    private static final int STAND_HEIGHT = SPRITE_SIZE;
    private static final int CROUCH_HEIGHT = 66;

    // Blocking
    private static final int MAX_BLOCK_COOLDOWN = 120; // 2 seconds cooldown

    // Attack Hitbox Constants (Updated for 50 px size)
    private static final int ATTACK_HITBOX_WIDTH = 65; // INCREASED FOR FORGIVENESS
    private static final int ATTACK_HITBOX_HEIGHT = 30; // SLIGHTLY INCREASED
    private static final int STAND_ATTACK_OFFSET_Y = 30;  // Hits mid-level (y=30)
    private static final int CROUCH_ATTACK_OFFSET_Y = 5;   // Hits low (y=5)

    // Dashing
    private static final int DASH_DISTANCE = 100; // Total distance to travel
    private static final int DASH_FRAMES = 10;
    private static final int DASH_COOLDOWN = 30;

    // --- KNOCKDOWN & STUN CONSTANTS ---
    private static final int REGULAR_STUN_DURATION = 8;
    private static final int AIR_STUN_DURATION = 15; // Longer stun for air combos
    private static final int KNOCKBACK_STRENGTH_LIGHT = 6;
    private static final int KNOCKBACK_STRENGTH_HEAVY = 12;
    private static final int KNOCKDOWN_DURATION = 90;
    private static final int WAKEUP_INVULNERABILITY = 15;
    private static final int FALL_VELOCITY = 10;
    private static final int HIT_FLASH_DURATION = 15;

    // --- SUPER METER CONSTANTS ---
    private static final int MAX_METER = 100;
    public static final int SUPER_ATTACK_COST = 50;
    public static final double METER_GAIN_HIT = 8.0;
    public static final double METER_GAIN_TAKEN = 4.0;

    // --- ANIMATION CONSTANTS ---
    public static final int RUN_FRAME_COUNT = 6;
    private static final int RUN_ANIMATION_SPEED = 4;
    public static final int ATTACK_FRAME_COUNT = 6;

    // --- Private Fields (Encapsulation) ---
    private int x, y; // Y is now accessible via getter
    private final int width = SPRITE_SIZE; // Drawing width remains 50
    private final int MAX_X_BOUND = SCREEN_WIDTH - width;

    private int height;
    private final Color color;
    private BufferedImage idleSprite;
    private BufferedImage[] runSprites;
    private BufferedImage[] attackSprites;
    private BufferedImage jumpSprite;
    private BufferedImage hurtSprite;
    private BufferedImage downSprite;

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

    public Fighter(int x, int y, Color color, int left, int right, int jump, int attack, int superAttack, int crouch, int dashFwd, int dashBack, BufferedImage idleSprite, BufferedImage[] runSprites, BufferedImage[] attackSprites, BufferedImage jumpSprite, BufferedImage hurtSprite, BufferedImage downSprite) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.idleSprite = idleSprite;
        this.runSprites = runSprites;
        this.attackSprites = attackSprites;
        this.jumpSprite = jumpSprite;
        this.hurtSprite = hurtSprite;
        this.downSprite = downSprite;
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

        // --- 1. HANDLE KNOCKDOWN STATE ---
        if (knockdownTimer > 0) {
            knockdownTimer--;

            // Apply gravity/fall until ground is reached
            if (y < GROUND_Y - height) {
                y += velY;
                velY += GRAVITY_ACCELERATION;
            } else {
                y = GROUND_Y - height;
                velY = 0;
                isKnockedDown = true;
            }
            // Apply knockback velocity (velX)
            x += velX;
            velX *= 0.85;

            if (knockdownTimer == 0) {
                // End of knockdown: Stand up, start invulnerability
                isKnockedDown = false;
                invulnerabilityTimer = WAKEUP_INVULNERABILITY;
            }
            return; // EXIT: NO input or other physics if in knockdown
        }

        // --- 2. HANDLE STUN/KNOCKBACK ---
        if (stunTimer > 0) {
            stunTimer--;
            // Apply knockback velocity (velX)
            x += velX;
            velX *= 0.85;

            // Check if knockback has dissipated enough to restore control
            if (Math.abs(velX) < 1.0) {
                velX = 0;
            }
            return; // EXIT: NO user input if in stun
        }

        // --- 3. HANDLE DASH STATE ---
        if (dashCooldown > 0) dashCooldown--;

        if (dashTimer > 0) {
            dashTimer--;
            // Apply fixed dash velocity
            x += velX;
            if (dashTimer == 0) {
                isDashing = false;
                velX = 0; // Stop horizontal movement after dash frames expire
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

        // 4d. User Movement and Running State Check
        isRunning = false;
        if (!isDashing && velX == 0) {
            if (!isCrouching) {
                if (keys[leftKey]) {
                    x -= MOVEMENT_SPEED;
                    isRunning = true;
                }
                if (keys[rightKey]) {
                    x += MOVEMENT_SPEED;
                    isRunning = true;
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
    /** Returns the smaller, physical collision box for fighter-to-fighter checks. */
    public Rectangle getRect() {
        int currentCollisionHeight = isCrouching ? CROUCH_HEIGHT : STAND_HEIGHT;
        // Uses smaller width (20) and offsets it (15) from the main X coordinate
        return new Rectangle(x + COLLISION_OFFSET_X, y, COLLISION_WIDTH, currentCollisionHeight);
    }

    public int getHealth() { return health; }
    public int getDirection() { return direction; }
    public boolean isBlockOnCooldown() { return isBlockOnCooldown; }
    public int getX() { return x; }
    public int getY() { return y; }
    public float getSuperMeter() { return superMeter; }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0; }
    public boolean isKnockedDown() { return isKnockedDown || knockdownTimer > 0; }
    public boolean onGround() { return onGround; }
    public boolean isBlocking() { return isBlocking; }

    /** Helper for AI to know if the fighter can take action (move, attack, dash). */
    public boolean canAct() {
        return stunTimer == 0 && knockdownTimer == 0;
    }

    /** Helper for AI to check if attack cooldown is clear. */
    public boolean isAttackReady() {
        return attackCooldown == 0;
    }

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

    // --- Dash Methods ---

    private boolean startDash(int directionMultiplier) {
        if (stunTimer == 0 && onGround) {
            isDashing = true;
            dashTimer = DASH_FRAMES;
            dashCooldown = DASH_COOLDOWN;
            velX = (float) (directionMultiplier * DASH_DISTANCE) / DASH_FRAMES;
            return true;
        }
        return false;
    }

    public boolean dashForward() {
        if (dashCooldown == 0) return startDash(this.direction);
        return false;
    }

    public boolean dashBack() {
        if (dashCooldown == 0) return startDash(-this.direction);
        return false;
    }

    // --- Attack & Damage Logic ---

    /**
     * FIX: Adjusted offsetX to ensure the attack box overlaps the opponent's
     * collision box when facing forward (direction == 1).
     */
    public Rectangle getAttackRect() {
        if (isAttackActive()) {
            int offsetY;
            if (isCrouching) {
                offsetY = CROUCH_ATTACK_OFFSET_Y;
            } else {
                offsetY = STAND_ATTACK_OFFSET_Y;
            }

            // --- HORIZONTAL OFFSET FIX: STARTING THE PUNCH EARLIER (at x + 60) ---
            // Current fix attempts:
            // Previous (failed): width - 15 (85) -> Punch started at x+85
            // NEW (Aggressive Reach): width - 40 (60) -> Punch starts at x+60.
            // This guarantees overlap with the opponent's 20px wide body when fighters are close.
            int aggressiveStartOffset = width - 40;

            int offsetX = direction == 1 ? aggressiveStartOffset : -ATTACK_HITBOX_WIDTH;
            // -----------------------------

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
        // Renamed from 'canHit' to 'isHitRegistered' to reflect the logic better
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
            // Damage Blocked
            blockCooldown = MAX_BLOCK_COOLDOWN;
            this.isBlocking = false;
            return;
        }

        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }

        // --- HIT FLASH TRIGGER ---
        this.invulnerabilityTimer = HIT_FLASH_DURATION;
        // -------------------------

        gainMeter(METER_GAIN_TAKEN);

        boolean isSuper = damage == 50; // Simple way to check if it was a heavy hit
        int knockbackSign = attackerDirection * -1; // Push away from attacker

        if (isSuper) {
            // SUPER ATTACK: Always results in knockdown/heavy stun
            knockdownTimer = KNOCKDOWN_DURATION;
            velX = (float) (knockbackSign * KNOCKBACK_STRENGTH_HEAVY);

            if (!onGround) {
                velY = FALL_VELOCITY;
            }

        } else {
            // REGULAR ATTACK: Air hit vs Ground hit determines stun type

            if (!onGround) {
                // AIR COMBO STUN: Longer stun, lighter knockback to keep opponent floating
                stunTimer = AIR_STUN_DURATION;
                velX = (float) (knockbackSign * (KNOCKBACK_STRENGTH_LIGHT / 2.0));
            } else {
                // GROUND STUN: Standard stun
                stunTimer = REGULAR_STUN_DURATION;
                velX = (float) (knockbackSign * KNOCKBACK_STRENGTH_LIGHT);
            }
        }
    }

    // --- Drawing ---
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // 1. Determine which sprite frame to draw
        BufferedImage currentSprite = idleSprite;

        // --- STATE DRAWING PRIORITY ---

        // P1: Hurt/Knocked Down (Highest Priority)
        if (stunTimer > 0 && hurtSprite != null) {
            currentSprite = hurtSprite;
        } else if (knockdownTimer > 0 && downSprite != null) {
            // We use the same frame for the duration of the knockdown fall
            currentSprite = downSprite;
        }
        // P2: Attack Animation
        else if (attackCooldown > 0 && attackSprites != null) {
            int attackFrameIndex = (ATTACK_DURATION - attackCooldown) * attackSprites.length / ATTACK_DURATION;
            // Clamp frame index to prevent array bounds error
            if (attackFrameIndex >= attackSprites.length) {
                attackFrameIndex = attackSprites.length - 1;
            }
            currentSprite = attackSprites[attackFrameIndex];
        }
        // P3: Jump/Airborne
        else if (!onGround && jumpSprite != null) {
            currentSprite = jumpSprite;
        }
        // P4: Running
        else if (runSprites != null && isRunning && onGround) {
            currentSprite = runSprites[frameIndex];
        }
        // P5: Idle (Fallback)
        // currentSprite is already idleSprite by default

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

                // --- APPLIED VERTICAL OFFSET FOR DRAWING ---
                // Shifts the sprite down 40px to meet the feet on the ground line (GROUND_Y=400)
                g2.drawImage(currentSprite, drawX, y + SPRITE_VERTICAL_OFFSET, drawWidth, height, null);
            } else {
                // FALLBACK: Draw color block if sprite not found
                g.setColor(color);
                g.fillRect(x, y, width, height);
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
    }
}