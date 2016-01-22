package data;

import java.util.List;

public interface World {

    long getGamespeed();

    List<Territory> getTerritories();

    List<Continent> getContinents();

    void claimTerritory(Territory territory);

    void placeReinforcements(Territory territory, int count);

    void attackTerritory(Territory from, Territory to);

    void moveArmy(Territory from, Territory to, int count);
}
