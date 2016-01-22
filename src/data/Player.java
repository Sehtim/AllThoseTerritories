package data;

import java.awt.*;

public class Player {
    private String name;
    private PlayerType playerType;
    private Color color;

    public Player(String name, PlayerType playerType, Color color) {
        this.name = name;
        this.playerType = playerType;
        this.color = color;
    }

    public boolean isAI() {
        return playerType != PlayerType.SPIELER;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}