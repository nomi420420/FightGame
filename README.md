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
2. Ensure you have Java 21 or higher installed
3. Verify the `assets/` folder contains all sprite sheets and sounds
4. Compile and run the `FightingGame` class
5. Select your game mode with 1 (Local) or 2 (AI)
6. Choose your fighters and stage
7. Press ENTER to start the match
8. Enjoy the fight!

## Game Flow

1. **Start Menu**: Press ENTER to begin
2. **Mode Select**: Choose Local Versus or AI mode
3. **Character Select**: Pick your fighter (and opponent in local mode)
4. **Stage Select**: Choose your battle arena
5. **Fight**: Best-of-3 stocks combat
6. **Game Over**: Winner declared, option to restart or quit

## Technical Details

- **Frame Rate**: 60 FPS
- **Screen Resolution**: 800x500 pixels
- **Physics**: Custom gravity and velocity system
- **Animation**: Sprite-based with automatic frame cycling
- **Collision**: Rectangle-based hitbox detection

## Future Updates

Planned features and improvements for upcoming versions:

- **Local Multiplayer Enhancement**: Support for separate keyboards and game controllers, allowing each player to use their own dedicated input device instead of sharing a single keyboard
- **AI Mode**: Single-player mode with computer-controlled opponents featuring different difficulty levels and fighting styles
- **Multiple Input Device Support**: Full compatibility with various controllers (Xbox, PlayStation, generic gamepads) alongside individual keyboard configurations for maximum flexibility