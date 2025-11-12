import java.awt.*;

public class Fighter {
    public int x, y, width = 50, height = 50;
    public Color color;
    public int velY = 0;
    public boolean onGround = true;
    public int health = 100;
    public int attackCooldown = 0;

    public int leftKey, rightKey, jumpKey, attackKey;

    public Fighter(int x, int y, Color color, int left, int right, int jump, int attack) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.leftKey = left;
        this.rightKey = right;
        this.jumpKey = jump;
        this.attackKey = attack;
    }

    public void update(boolean[] keys) {
        // Movement
        if (keys[leftKey]) x -= 5;
        if (keys[rightKey]) x += 5;

        // Jump
        if (keys[jumpKey] && onGround) {
            velY = -15;
            onGround = false;
        }

        // Gravity
        y += velY;
        if (y >= 400) {
            y = 400;
            velY = 0;
            onGround = true;
        } else {
            velY += 1;
        }

        // Attack cooldown
        if (attackCooldown > 0) attackCooldown--;
    }

    public Rectangle getRect() {
        return new Rectangle(x, y, width, height);
    }

    public Rectangle getAttackRect() {
        int offset = color == Color.BLUE ? 60 : -30;
        return new Rectangle(x + offset, y + 10, 20, 20);
    }

    public boolean attack() {
        if (attackCooldown == 0) {
            attackCooldown = 20;
            return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x, y, width, height);
        if (attackCooldown > 10) {
            g.setColor(Color.YELLOW);
            Rectangle ar = getAttackRect();
            g.fillRect(ar.x, ar.y, ar.width, ar.height);
        }
    }
}

