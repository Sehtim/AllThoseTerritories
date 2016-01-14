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


    public Territory(String name, Point cap, List<Polygon> parts) {
        this.name = name;
        this.capital = cap;
        this.parts = parts;
    }

    public boolean isNeighbor(Territory t) {
        return this.neighbors.contains(t);
    }

    public void addNeighbor(Territory t) {
        neighbors.add(t);
    }

    public String getName() {
        return this.name;
    }

    public Point getCapitalPosition() {
        return capital;
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

    public List<Polygon> getParts() {
        return parts;
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
