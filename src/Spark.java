import java.awt.Color;
import java.awt.Graphics;
import java.lang.Math;

public class Spark {
    private int x, y;
    private float velX, velY;
    private final int size;
    private int life;
    private final Color color;

    private static final int MAX_LIFE = 10; // Frames the spark lasts

    public Spark(int x, int y) {
        this.x = x;
        this.y = y;
        this.life = MAX_LIFE;
        this.size = 3; // INCREASED SIZE for better visibility

        // Random velocity to shoot sparks outward
        double angle = Math.random() * 2 * Math.PI;
        double speed = 3 + Math.random() * 4; // INCREASED MAX SPEED for wider spread

        this.velX = (float) (Math.cos(angle) * speed);
        this.velY = (float) (Math.sin(angle) * speed);

        // Color is now WHITE for maximum visibility
        this.color = Color.WHITE;
    }

    public void update() {
        if (life > 0) {
            x += velX;
            y += velY;
            // Simple gravity effect
            velY += 0.3;
            life--;
        }
    }

    public boolean isAlive() {
        return life > 0;
    }

    public void draw(Graphics g) {
        // Fade the spark color based on remaining life
        float alpha = (float) life / MAX_LIFE;
        // Use Color(R, G, B, Alpha) to set transparency
        Color drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * alpha));

        g.setColor(drawColor);
        g.fillRect(x, y, size, size);
    }
}