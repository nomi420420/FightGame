# Java Fighting Game

A fast-paced, two-player fighting game built using Java and Swing. Features robust, competitive mechanics designed for strategic play and precise timing.

## Core Mechanics

- **Strategic Blocking**: Players can block 100% of damage, but a successful block instantly disables the defense and starts a 2-second cooldown, preventing abuse.

- **Knockdown System**: The powerful Super Attack forces the opponent into a Knockdown state, where they fall to the ground. They are briefly invincible as they stand up.

- **Hit Impact**: Regular attacks apply a short stun and light knockback, interrupting the opponent's actions.

- **Dynamic Hitboxes**: Attacks have specific vertical properties:
  - Standing attacks hit mid-level (can be dodged by crouching)
  - Crouching attacks hit low (must be blocked)

- **Movement**: Includes quick Dashes and Backsteps for advanced positioning. Fighters automatically turn to face their opponent and are restricted by Corner Boundaries.

## Attack Types and Cooldowns

- **Regular Attack**: A standard, quick attack with low cooldown.
- **Super Attack**: High-damage attack that triggers Knockdown and has a long, 25-second cooldown.

## Controls

The game is designed for two players sharing a single keyboard.

### Player 1 (Blue)

| Action | Key |
|--------|-----|
| Move Left / Right | A / D |
| Jump | W |
| Crouch / Block | S |
| Regular Attack | F |
| Super Attack | G |
| Forward Dash | E |
| Backstep | R |

### Player 2 (Red)

| Action | Key |
|--------|-----|
| Move Left / Right | Left / Right Arrows |
| Jump | Up Arrow |
| Crouch / Block | Down Arrow |
| Regular Attack | L |
| Super Attack | K |
| Forward Dash | I |
| Backstep | O |

### Game Controls

- **Start Game**: ENTER (can be pressed by either player)

## Requirements

- Java 21 or higher

## How to Run

1. Clone the repository
2. Compile and run the `FightingGame` class
3. Press ENTER to start the game
4. Enjoy the match!