# Fantasy Fighting Game

A fast-paced, two-player fighting game built using Java 21 and Swing. Features robust, competitive mechanics designed for strategic play and precise timing, with both local multiplayer and single-player AI modes.

## Core Mechanics

- **Strategic Blocking**: Players can block 100% of damage, but a successful block instantly disables the defense and starts a 2-second cooldown, preventing abuse.

- **Knockdown System**: The powerful Super Attack forces the opponent into a Knockdown state, where they fall to the ground. They are briefly invincible as they stand up.

- **Hit Impact & Visual Effects**: Regular attacks apply a short stun and light knockback, interrupting the opponent's actions. On-hit visual effects (sparks) emphasize impact and are tuned for visibility and responsiveness.

- **Dynamic Hitboxes**: Attacks have specific vertical properties:
  - Standing attacks hit mid-level (can be dodged by crouching)
  - Crouching attacks hit low (must be blocked)

- **Movement**: Includes quick Dashes and Backsteps for advanced positioning. Fighters automatically turn to face their opponent and are restricted by Corner Boundaries.

- **Super Meter System**: Build meter by hitting opponents or taking damage. Spend 50 meter for devastating Super Attacks.

## Attack Types and Cooldowns

- **Regular Attack**: A standard, quick attack with low cooldown (20 frames). Builds meter on hit.
- **Super Attack**: High-damage attack (50 HP) that triggers Knockdown and consumes 50 meter. Has the same cooldown as regular attacks.

## Game Modes

### Local Versus (2 Players)
Classic head-to-head combat with two players sharing a keyboard. Perfect for competitive play with friends.

### Single Player (VS AI)
Battle against an intelligent AI opponent with adaptive behavior:
- **Smart Positioning**: AI maintains optimal spacing and approaches strategically
- **Defensive Reactions**: AI blocks incoming attacks with a configurable success rate
- **Aggressive Offense**: AI attacks when in range and uses Super Attacks when meter is available
- **Dynamic Movement**: Smooth, human-like movement with natural decision-making

## Controls

The game is designed for two players sharing a single keyboard.

### Player 1 (Blue)

| Action            | Key |
|-------------------|-----|
| Move Left / Right | A / D |
| Jump              | W   |
| Crouch / Block    | S   |
| Regular Attack    | F   |
| Super Attack      | G   |
| Forward Dash      | E   |
| Backstep          | R   |

### Player 2 (Red)

| Action            | Key |
|-------------------|-----|
| Move Left / Right | Left / Right Arrows |
| Jump              | Up Arrow            |
| Crouch / Block    | Down Arrow          |
| Regular Attack    | L                   |
| Super Attack      | K                   |
| Forward Dash      | I                   |
| Backstep          | O                   |

### Game Controls

- **Start Game**: ENTER (can be pressed by either player)
- **Quit Game**: ESC

## Character Selection

Choose from 4 unique fighters, each with their own color scheme and sprite animation:
- **Orc** (Default P1)
- **Knight** (Default P2)
- **Skeleton**
- **Guardsman**

## Stage Selection

Battle across 4 distinct stages, each with unique background gradients:
- **Daytime City**: Warm orange to yellow sunset atmosphere
- **Night Arena**: Deep blue night sky setting
- **Volcano Summit**: Fiery red and orange volcanic backdrop
- **Frost Peaks**: Cool blue and white icy mountains

Use Player 2's arrow keys (or Left/Right in AI mode) to cycle through stages before the match.

## Requirements

- Java 21 or higher

## How to Run

1. Clone the repository
2. Ensure you have Java 21 or higher installed
3. Verify the `assets/` folder contains all sprite sheets (`fighter_sheet_0.png` through `fighter_sheet_3.png`) and sound files
4. Compile and run the `FantasyFightingGame` class
5. Select your game mode with `1` (Local) or `2` (AI)
6. Choose your fighters and stage
7. Press ENTER to start the match
8. Enjoy the fight!

## Game Flow

1. **Start Menu**: Press ENTER to begin
2. **Mode Select**: Choose between Local Versus (2 Players) or Single Player (VS AI)
3. **Character Select**: Pick your fighter (and opponent's fighter in local mode)
4. **Stage Select**: Choose your battle arena
5. **Fight**: Best-of-3 stocks combat with health bars, super meters, and stock indicators
6. **Game Over**: Winner declared with option to restart or quit

## Technical Details

- **Frame Rate**: 60 FPS
- **Screen Resolution**: 800x500 pixels
- **Physics**: Custom gravity and velocity system
- **Animation**: Sprite-based with automatic frame cycling (4-frame run animation)
- **Collision**: Rectangle-based hitbox detection with precise attack timing
- **Sound System**: Multi-threaded audio playback for hit sounds, blocks, and super attacks
- **AI System**: Decision-based opponent with cooldowns for smooth, realistic behavior
- **Impact FX**: Lightweight particle-style hit effects optimized for clarity and performance

## Combat System Details

### Health & Damage
- Starting Health: 100 HP per stock
- Regular Attack Damage: 10 HP
- Super Attack Damage: 50 HP
- Stocks: 3 lives per player

### Meter System
- Maximum Meter: 100 points
- Meter Gain on Hit: 8 points (16 for Super Attacks)
- Meter Gain on Taking Damage: 4 points
- Super Attack Cost: 50 points

### Stun & Knockback
- Regular Ground Stun: 8 frames
- Air Combo Stun: 15 frames (extended for juggle combos)
- Knockdown Duration: 90 frames (1.5 seconds)
- Wakeup Invulnerability: 15 frames
- Block Cooldown: 120 frames (2 seconds)

## Recent Updates

- **Improved Hit Feedback**
  - Added clearer, more visible on-hit visual effects to emphasize attack impact.
  - Tuned effect duration and movement for a snappier feel during exchanges.

- **Visual Polish**
  - Refined visibility of combat effects against all background stages.
  - Small consistency improvements across stages and character interactions.

- **Documentation**
  - README updated to reflect the current AI mode (already implemented) and impact visual effects.

## Future Updates

Planned features and improvements for upcoming versions:

- **Hitbox & Hurtbox Rework**: Tighter, more accurate attack and hurtbox tuning (especially for jump-ins and low attacks), improved corner interaction, and better handling of edge cases where attacks visually connect but currently miss.
- **Local Multiplayer Enhancement**: Support for separate keyboards and game controllers, allowing each player to use their own dedicated input device instead of sharing a single keyboard.
- **Difficulty & AI Variety**: Multiple AI difficulty presets and alternative behavior styles (aggressive, defensive, zoning).
- **Training / Practice Mode**: Sandbox mode with input display, reset-to-center, and infinite meter/health options.
- **Expanded FX & UI**: Additional combat feedback such as counter-hit indicators, combo display, and optional damage numbers.