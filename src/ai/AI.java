package ai;

public interface AI {
    void claimTurn(int computerID);

    void reinforceTurn(int computerID, int reinforcements);

    void movementTurn(int computerID);
}
