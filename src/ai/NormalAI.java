package ai;

import data.Continent;
import data.Territory;
import data.World;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class NormalAI implements AI {

    private World world;

    private Continent targetContinent;
    private List<Territory> targetTerritories;

    int min_attacker_count = 2;

    public NormalAI(World world) {
        this.world = world;
    }

    @Override
    public void claimTurn(int computerID) {
        List<Territory> ownTerritories = world.getTerritories().stream().filter(t -> t.getPlayer() == computerID).collect(Collectors.toList());
        List<Continent> allContinents = world.getContinents();

        // Bevorzugt: Länder auf Kontinenten, auf denen er schon etwas besetzt hat
        for (Territory ownTerritory : ownTerritories)
        {
            for (Continent c : allContinents)
            {
                if (c.getTerritories().contains(ownTerritory))
                {
                    List<Territory> wantedTerritories = c.getTerritories().stream().filter(t -> t.getPlayer() == -1).collect(Collectors.toList());
                    if (wantedTerritories.size() != 0) {
                        delay();
                        world.claimTerritory(wantedTerritories.get(ThreadLocalRandom.current().nextInt(wantedTerritories.size())));
                        return;
                    }
                }
            }
        }

        // Default: random
        List<Territory> freeTerritories = world.getTerritories().stream().filter(t -> t.getPlayer() == -1).collect(Collectors.toList());
        world.claimTerritory(freeTerritories.get(ThreadLocalRandom.current().nextInt(freeTerritories.size())));
    }

    @Override
    public void reinforceTurn(int computerID, int reinforcements) {
        findTargets(computerID);
        List<Territory> ownTerritories = world.getTerritories().stream().filter(t -> t.getPlayer() == computerID).collect(Collectors.toList());

        Territory source = null;

        for (Territory target : targetTerritories)
        {
            // Suche mögliche Angreifer und wähle den mit den meisten Armeen, um ihn zu verstärken
            source = ownTerritories.stream().filter(t -> t.isNeighbor(target)).max((t1, t2) -> t1.getArmyCount() - t2.getArmyCount()).orElse(null);
            if (source != null)
            {
                // Verstärke bestenfalls mit doppelt so vielen Armeen + 1, wie im Zielland stehen
                delay();
                int count = Math.min(reinforcements, target.getArmyCount() * 2 + 1);
                world.placeReinforcements(source, count);
                reinforcements -= count;
                if (reinforcements <= 0)
                    return;
            }
        }

        // Verbleibende Verstärkungen auf das erste Angriffsland verteilen, sonst zufällig
        if (reinforcements > 0)
        {
            delay();
           if (source != null)
               world.placeReinforcements(source, reinforcements);
           else
               world.placeReinforcements(ownTerritories.get(ThreadLocalRandom.current().nextInt(ownTerritories.size())), reinforcements);
        }
    }

    @Override
    public void movementTurn(int computerID) {
        findTargets(computerID);
        List<Territory> attackerTerritories;

        boolean continentPhase = true;

        // Greife nur an, solange mehr als 1 Angreifer zur Verfügung steht...
        while ((attackerTerritories = world.getTerritories().stream().filter(t -> t.getPlayer() == computerID && t.getArmyCount() > min_attacker_count).collect(Collectors.toList())).size() > 0)
        {
            boolean attacked = false;

            for (Territory att : attackerTerritories) {
                Territory lastTarget = null;
                for (Territory target : targetTerritories)
                {
                    if (att.isNeighbor(target) && target.getPlayer() != computerID)
                    {
                        while (att.getArmyCount() > min_attacker_count) {
                            delay();
                            world.attackTerritory(att, target);
                            attacked = true;
                            if (target.getPlayer() == computerID) // Erfolgreicher Angriff
                            {
                                lastTarget = target;
                                break;
                            }
                        }
                    }
                }

                // Verschiebe Armeen in letztes erobertes Gebiet (darf man nach Angriff immer)
                if (att.getArmyCount() > 1 && lastTarget != null)
                {
                    delay();
                    world.moveArmy(att, lastTarget, att.getArmyCount() - 1);
                }
            }

            // Kein Angriff auf Zielkontinent mehr möglich - weite targetTerritories aus auf Gebiete außerhalb, erhöhe jedoch min_attacker_count, um Schwachstellen in der Verteidigung zu vermeiden
            if (!attacked) {
                if (continentPhase) {
                    targetTerritories = world.getTerritories().stream().filter(t -> t.getPlayer() != computerID).collect(Collectors.toList());
                    continentPhase = false;
                    min_attacker_count += 2;
                }
                else
                {
                    min_attacker_count -= 2;
                    break;
                }
            }
        }
    }

    private void findTargets(int computerID)
    {
        // Kontinent ausfindig machen, von dem er schon am meisten besetzt hat -> nächstes Ziel
        double targetQuot = -1.0;
        List<Continent> allContinents = world.getContinents();
        for (Continent c : allContinents)
        {
            double x = (double)c.getTerritories().stream().filter(t -> t.getPlayer() == computerID).count()
                    / (double)c.getTerritories().size();

            if (x < 1.0 && x > targetQuot) {
                targetContinent = c;
                targetQuot = x;
            }
        }

        // Zielterritorien festlegen...
        targetTerritories = targetContinent.getTerritories().stream().filter(t -> t.getPlayer() != computerID).collect(Collectors.toList());
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
