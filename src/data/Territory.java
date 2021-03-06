package data;

import java.awt.*;
import java.util.List;
import java.util.*;

public class Territory {
    private List<Polygon> parts;
    private Point capital;
    private String name;

    private Set<Territory> neighbors;

    private int player = -1;
    private int armyCount = 0;

    public Territory(String name) {
        this.name = name;
        this.parts = new ArrayList<>();
        this.neighbors = new HashSet<>();
    }

    public boolean isNeighbor(Territory t) {
        return this.neighbors.contains(t);
    }

    public void addNeighbor(Territory t) {
        neighbors.add(t);
        // Auch anders herum hinzufügen
        t.neighbors.add(this);
    }

    public Set<Territory> getNeighbors() {
        return neighbors;
    }

    public String getName() {
        return this.name;
    }

    public Point getCapitalPosition() {
        return capital;
    }

    public void setCapitalPosition(Point capital) {
        this.capital = capital;
    }

    public void setPlayer(int playerID) {
        this.player = playerID;
    }

    public int getPlayer() {
        return this.player;
    }

    public void setArmyCount(int count) {
        this.armyCount = count;
    }

    public int getArmyCount() {
        return this.armyCount;
    }

    public void increaseArmyCount(int count) {
        this.armyCount += count;
    }

    public void decreaseArmyCount(int count) {
        this.armyCount -= count;
    }

    public List<Polygon> getParts() {
        return parts;
    }

    public void addPart(Polygon polygon) {
        parts.add(polygon);
    }

    public boolean contains(Point p) {
        for (Polygon polygon : parts) {
            if (polygon.contains(p)) {
                return true;
            }
        }
        return false;
    }
}
