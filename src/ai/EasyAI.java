package ai;

import data.Territory;
import data.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EasyAI implements AI {

    private World world;

    public EasyAI(World world) {
        this.world = world;
    }

    /**
     * Die Methode geht davon aus, dass es zumindest noch 1 Territorium gibt, dass besetzt werden kann.
     */
    @Override
    public void claimTurn(int computerID) {
        delay();

        // Alle Territorien bestimmen, die noch frei sind
        List<Territory> unclaimedTerritories = new ArrayList<Territory>();
        for (Territory territory : world.getTerritories()) {
            if (territory.getPlayer() == -1) {
                unclaimedTerritories.add(territory);
            }
        }

        // Zufälliges Territorium besetzen
        world.claimTerritory(unclaimedTerritories.get(ThreadLocalRandom.current().nextInt(unclaimedTerritories.size())));
    }

    @Override
    public void reinforceTurn(int computerID, int reinforcements) {
        delay();

        // Alle Territorien bestimmen, die der AI gehören
        List<Territory> ownedTerritories = new ArrayList<Territory>();
        for (Territory territory : world.getTerritories()) {
            if (territory.getPlayer() == computerID) {
                ownedTerritories.add(territory);
            }
        }

        int placeCount;
        do {
            // Armeen zufällig verteilen
            placeCount = ThreadLocalRandom.current().nextInt(1, reinforcements + 1);
            reinforcements -= placeCount;
            world.placeReinforcements(ownedTerritories.get(ThreadLocalRandom.current().nextInt(ownedTerritories.size())), placeCount);
            delay();
        }
        while (reinforcements > 0);
    }

    @Override
    public void movementTurn(int computerID) {
        delay();

        // Alle Territorien bestimmen, die der AI gehören
        List<Territory> ownedTerritories = new ArrayList<Territory>();
        for (Territory territory : world.getTerritories()) {
            if (territory.getPlayer() == computerID) {
                ownedTerritories.add(territory);
            }
        }

        // Alle eigenen Territorien bestimmen, die mehr als 1 Armee beinhalten und jemanden angreifen können
        List<Territory> attackerTerritories = new ArrayList<Territory>();
        for (Territory territory : ownedTerritories) {
            if (territory.getArmyCount() > 1) {
                for (Territory neighbor : territory.getNeighbors()) {
                    if (neighbor.getPlayer() != computerID) {
                        attackerTerritories.add(territory);
                        break;
                    }
                }
            }
        }

        if (attackerTerritories.isEmpty()) {
            return; // AI kann nichts angreifen
        }

        // Zufällig angreifen, bis nichts mehr geht
        Territory from;
        boolean attacked;
        do {
            from = attackerTerritories.get(0);
            attacked = false;
            for (Territory to : from.getNeighbors()) {
                if (to.getPlayer() != computerID) {
                    attacked = true;
                    world.attackTerritory(from, to);
                    delay();
                    break;
                }
            }
            if (!attacked || from.getArmyCount() == 1) {
                // Von diesem Territorium aus kann nicht mehr angegriffen werden
                attackerTerritories.remove(0);
            }
        } while (!attackerTerritories.isEmpty());
    }

    private void delay() {
        try {
            Thread.sleep(world.getGamespeed());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
}
