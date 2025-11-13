import java.lang.Math;
import java.awt.event.KeyEvent;

/**
 * Static utility class containing the simplified logic for the Player 2 (AI) opponent.
 * This version focuses on core actions (Attack, Block, Move) for reliability.
 */
public class AIOpponent {

    // AI Constants
    private static final int AI_ATTACK_RANGE = 50;
    private static final int AI_BLOCK_RANGE = 80;
    private static final int AI_DECISION_COOLDOWN = 8;  // Frames between movement decisions

    private static int decisionTimer = 0;
    private static int lastDecision = 0;  // 0 = none, 1 = move toward, 2 = move away, 3 = jump

    /**
     * Executes the AI's decision-making process for Player 2.
     * @param player1 The human-controlled fighter (target).
     * @param player2 The AI-controlled fighter.
     * @param keys The global keys array to simulate input.
     */
    public static void runAILogic(Fighter player1, Fighter player2, boolean[] keys) {

        // Ensure the AI can physically act (not stunned/knocked down)
        if (!player2.canAct()) {
            // Stop ALL keys
            keys[player2.leftKey] = keys[player2.rightKey] = keys[player2.jumpKey] = keys[player2.crouchKey] = false;
            keys[player2.attackKey] = false;
            keys[player2.superAttackKey] = false;
            decisionTimer = 0;
            lastDecision = 0;
            return;
        }

        // Reset all active keys before making a new decision
        keys[player2.leftKey] = keys[player2.rightKey] = keys[player2.jumpKey] = keys[player2.crouchKey] = false;
        keys[player2.attackKey] = false;
        keys[player2.superAttackKey] = false;

        int distance = Math.abs(player1.getX() - player2.getX());
        int directionToPlayerKey = (player1.getX() < player2.getX()) ? player2.leftKey : player2.rightKey;
        int awayKey = (directionToPlayerKey == player2.leftKey) ? player2.rightKey : player2.leftKey;

        // --- IMMEDIATE ATTACK CHECK (Highest Priority) ---
        if (distance < AI_ATTACK_RANGE && player2.isAttackReady()) {
            if (player2.getSuperMeter() >= Fighter.SUPER_ATTACK_COST && Math.random() < 0.3) {
                keys[player2.superAttackKey] = true;
            } else {
                keys[player2.attackKey] = true;
            }
            decisionTimer = 0;
            lastDecision = 0;
            return; // EXIT: Attack committed
        }

        // --- DEFENSE LOGIC (Reduced block chance) ---
        if (player1.isAttackActive() && distance < AI_BLOCK_RANGE) {
            boolean isFacingAttacker = player1.getDirection() != player2.getDirection();

            if (isFacingAttacker && Math.random() < 0.60) {  // Reduced from 90% to 60%
                keys[player2.crouchKey] = true;
                return;
            }
        }

        // --- MOVEMENT LOGIC (With decision cooldown for smoothness) ---
        
        // Maintain last decision until timer expires
        if (decisionTimer > 0) {
            decisionTimer--;
            
            // Execute the last decision
            if (lastDecision == 1) {
                keys[directionToPlayerKey] = true;
            } else if (lastDecision == 2) {
                keys[awayKey] = true;
            } else if (lastDecision == 3 && player2.onGround()) {
                keys[player2.jumpKey] = true;
            }
            return;
        }

        // Make a new decision
        if (distance > AI_ATTACK_RANGE * 2) {
            // Far: walk toward
            lastDecision = 1;
            decisionTimer = AI_DECISION_COOLDOWN;
            keys[directionToPlayerKey] = true;
            
        } else if (distance > AI_ATTACK_RANGE) {
            // Medium range: sometimes jump, mostly walk toward
            if (Math.random() < 0.10 && player2.onGround()) {
                lastDecision = 3;
                decisionTimer = AI_DECISION_COOLDOWN;
                keys[player2.jumpKey] = true;
            } else {
                lastDecision = 1;
                decisionTimer = AI_DECISION_COOLDOWN;
                keys[directionToPlayerKey] = true;
            }
            
        } else {
            // Too close: back away
            lastDecision = 2;
            decisionTimer = AI_DECISION_COOLDOWN / 2;  // Shorter cooldown for backing away
            keys[awayKey] = true;
        }
    }
}