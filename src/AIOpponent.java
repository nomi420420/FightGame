import java.lang.Math;
import java.awt.event.KeyEvent;

/**
 * Static utility class containing the logic for the Player 2 (AI) opponent.
 * This version uses a reliable, immediate attack input when in range.
 */
public class AIOpponent {

    // AI Constants
    private static final int AI_MIN_REACT_TIME = 1; // Minimum delay is 1 frame (instant)
    private static final int AI_MAX_REACT_TIME = 3; // Maximum delay is 3 frames (very fast)
    private static final int AI_ATTACK_RANGE = 50;
    private static final int AI_BLOCK_RANGE = 80;

    private static int aiReactTimer = 0;

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
            return;
        }

        // Reset all active movement/defense keys before making a new decision
        keys[player2.leftKey] = keys[player2.rightKey] = keys[player2.jumpKey] = keys[player2.crouchKey] = false;
        // Do NOT reset attack keys here, as they are handled by direct function calls below.

        int distance = Math.abs(player1.getX() - player2.getX());
        int directionToPlayerKey = (player1.getX() < player2.getX()) ? player2.leftKey : player2.rightKey;

        // --- IMMEDIATE ATTACK CHECK (Highest Priority) ---
        // If we are close and ready to attack, call the attack function directly to bypass key timing issues.
        if (distance < AI_ATTACK_RANGE && player2.isAttackReady()) {
            if (player2.getSuperMeter() >= Fighter.SUPER_ATTACK_COST) {
                player2.superAttack(); // DIRECT FUNCTION CALL
            } else {
                player2.attack(); // DIRECT FUNCTION CALL
            }
            return; // EXIT: Attack committed
        }
        // ----------------------------------------------------


        // --- React Timer (GATES MOVEMENT AND DEFENSE) ---
        if (aiReactTimer > 0) {
            aiReactTimer--;
            return;
        }

        // Set a new random reaction delay
        aiReactTimer = (int) (Math.random() * (AI_MAX_REACT_TIME - AI_MIN_REACT_TIME)) + AI_MIN_REACT_TIME;


        // --- 1. DEFENSE LOGIC (High Priority) ---
        if (player1.isAttackActive() && distance < AI_BLOCK_RANGE) {
            boolean isFacingAttacker = player1.getDirection() != player2.getDirection();

            if (isFacingAttacker) {
                if (Math.random() < 0.90) { // 90% chance to block
                    keys[player2.crouchKey] = true;
                    return;
                }
            }
        }

        // --- 2. MOVEMENT LOGIC (Move toward player) ---
        else {
            if (distance > AI_ATTACK_RANGE * 2) {
                // If far, walk toward
                keys[directionToPlayerKey] = true;
            } else if (distance > AI_ATTACK_RANGE) {
                // If just outside range, hop or walk slightly toward
                if (Math.random() < 0.2 && player2.onGround()) {
                    keys[player2.jumpKey] = true;
                } else {
                    keys[directionToPlayerKey] = true;
                }
            } else {
                // If too close (overlapping): try to jump away
                keys[directionToPlayerKey == player2.leftKey ? player2.rightKey : player2.leftKey] = true; // Walk away
            }
        }
    }
}