package data;

import java.awt.*;

public class Player {
    private String name;
    private boolean ai;
    private Color color;

    public Player(String name, boolean ai, Color color) {
        this.name = name;
        this.ai = ai;
        this.color = color;
    }

    public boolean isAI() {
        return ai;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}