package gui;

import ai.AI;
import ai.EasyAI;
import ai.NormalAI;
import data.*;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class WorldFrame extends JFrame implements World {

    public static final int WIDTH = 1250;
    public static final int HEIGHT = 650;

    private List<Continent> continents;
    private List<Territory> territories;
    private List<Player> players;

    private int activePlayer;
    private int activeReinforcements;

    private Thread AIThread;

    private Territory selectedTerritory;
    private Territory attackedTerritory;
    private Territory attackedFromTerritory;

    private Territory moveFromTerritory;
    private Territory moveToTerritory;
    private boolean movePossible;

    private boolean showTerritoryNames;
    private Font drawFont;

    private boolean claimPhase;
    private boolean reinforcePhase;

    private HashMap<PlayerType, AI> allAIs;

    private final JButton nextTurnBtn;
    private final Canvas gameBoard;

    public WorldFrame(List<Player> players, List<Territory> territories, List<Continent> continents) {
        this.continents = continents;
        this.players = players;
        this.territories = territories;

        this.drawFont = new Font("Arial", Font.BOLD, 12);
        this.showTerritoryNames = false;

        AIThread = new Thread(this::nextTurn);

        /* Variablen fürs Spiel initialisieren */
        claimPhase = true;
        activePlayer = 0;

        allAIs = new HashMap<>();

        AI TheAI_TM = new EasyAI(this);
        for (PlayerType t : PlayerType.values())
            allAIs.put(t, TheAI_TM);

        allAIs.put(PlayerType.NORMAL, new NormalAI(this));

        allAIs.remove(PlayerType.SPIELER);
        /*-------------------------------------*/

        setTitle("AllThoseTerritories");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        gameBoard = new Canvas() {
            @Override
            public void paint(Graphics g) {
                paintGameBoard(g);
            }
        };
        gameBoard.setSize(WIDTH, HEIGHT);

        gameBoard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // TODO evtl. für Tooltips
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                for (Territory territory : territories) {
                    if (territory.contains(e.getPoint())) {
                        // TODO Aktionen


                        if (players.get(activePlayer).isAI()) {
                            // Evtl Meldung "AI ist an der Reihe" oder Informationen anzeigen
                            break;
                        }

                        if (claimPhase) {
                            claimTerritory(territory);
                            invokeNextTurn();
                        } else {
                            if (reinforcePhase) {
                                placeReinforcements(territory, 1);
                                if (activeReinforcements == 0)
                                    invokeNextTurn();
                            } else {
                                if (SwingUtilities.isLeftMouseButton(e)) {

                                    Territory tmp = selectedTerritory;
                                    selectedTerritory = territory;
                                    if (tmp != null)
                                        gameBoard.repaint(tmp.getBounds().x, tmp.getBounds().y,
                                                tmp.getBounds().width, tmp.getBounds().height);
                                    gameBoard.repaint(territory.getBounds().x, territory.getBounds().y,
                                            territory.getBounds().width, territory.getBounds().height);

                                } else if (SwingUtilities.isRightMouseButton(e)) {
                                    if (territory.getPlayer() == activePlayer)
                                        moveArmy(selectedTerritory, territory, 1);
                                    else
                                        attackTerritory(selectedTerritory, territory);
                                }
                            }
                        }

                        break;
                    }

                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.BLACK));
        nextTurnBtn = new JButton("Zug beenden");
        nextTurnBtn.setEnabled(!claimPhase);
        nextTurnBtn.addActionListener(e -> invokeNextTurn());
        buttonPanel.add(nextTurnBtn);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(gameBoard, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);

        gameBoard.createBufferStrategy(4);

        if (players.get(activePlayer).isAI())
            AIThread.start();
    }

    private void paintGameBoard(Graphics g) {
        // TODO alles zeichnen

        // Verbindungen zeichnen
        g.setColor(Color.BLACK);
        for (int i = 0; i < territories.size(); i++) {
            Territory t1 = territories.get(i);
            for (int j = i + 1; j < territories.size(); j++) {
                Territory t2 = territories.get(j);
                if (t1.isNeighbor(t2)) {
                    int x1 = t1.getCapitalPosition().x;
                    int x2 = t2.getCapitalPosition().x;

                    if (Math.abs(x1 - x2) > WIDTH / 2) {
                        int y = (t1.getCapitalPosition().y + t2.getCapitalPosition().y) / 2;
                        if (x1 < x2) {
                            g.drawLine(0, y, x1, t1.getCapitalPosition().y);
                            g.drawLine(x2, t2.getCapitalPosition().y, WIDTH, y);
                        } else {
                            g.drawLine(0, y, x2, t2.getCapitalPosition().y);
                            g.drawLine(x1, t1.getCapitalPosition().y, WIDTH, y);
                        }
                    } else {
                        g.drawLine(t1.getCapitalPosition().x, t1.getCapitalPosition().y,
                                t2.getCapitalPosition().x, t2.getCapitalPosition().y);
                    }
                }
            }
        }

        // Landflächen
        for (Territory territory : territories) {
            if (territory.getPlayer() == -1) {
                g.setColor(Color.GRAY);
            } else {
                g.setColor(players.get(territory.getPlayer()).getColor());
            }
            for (Polygon polygon : territory.getParts()) {
                g.fillPolygon(polygon);
            }
            g.setColor(Color.DARK_GRAY);
            for (Polygon polygon : territory.getParts()) {
                g.drawPolygon(polygon);
            }
        }

        // Ausgewähltes Territorium
        if (selectedTerritory != null) {
            g.setColor(Color.YELLOW);
            for (Polygon polygon : selectedTerritory.getParts()) {
                g.drawPolygon(polygon);
            }
        }

        // Informationen (Name, Anzahl Armeen)
        g.setColor(Color.BLACK);
        g.setFont(drawFont);
        FontMetrics metrics = g.getFontMetrics(drawFont);

        for (Territory territory : territories) {
            Point cap = territory.getCapitalPosition();

            String toDraw;

            if (showTerritoryNames) {
                toDraw = territory.getName();
                g.drawString(toDraw, cap.x - metrics.stringWidth(toDraw) / 2, cap.y - metrics.getHeight() / 2);
            }

            toDraw = String.valueOf(territory.getArmyCount());
            g.drawString(toDraw, cap.x - metrics.stringWidth(toDraw) / 2, cap.y + metrics.getHeight() / 2);

        }
    }

    @Override
    public long getGamespeed() {
        return 200;
    }

    @Override
    public List<Territory> getTerritories() {
        return territories;
    }

    @Override
    public List<Continent> getContinents() {
        return continents;
    }

    @Override
    public void claimTerritory(Territory territory) {
        if (claimPhase) {
            if (territory.getPlayer() == -1) {
                territory.setPlayer(activePlayer);
                territory.setArmyCount(1);

                if (territories.stream().allMatch(t -> t.getPlayer() >= 0)) {
                    claimPhase = false;
                    reinforcePhase = false;
                    activePlayer = players.size() - 1;
                    // selber status wie am Ende einer Angriffs-Phase
                }

                Rectangle bounds = territory.getBounds();
                gameBoard.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            // else {  //Evtl Meldung anzeigen: schon belegt }


        } else {
            System.out.println("Achtung: claimTerritory falsch aufgerufen!");
        }
    }

    @Override
    public void attackTerritory(Territory from, Territory to) {
        if (claimPhase || reinforcePhase || from.getPlayer() != activePlayer || to.getPlayer() == activePlayer || from.getArmyCount() <= 1) {
            System.out.println("Achtung! attackTerritory falsch aufgerufen!");
        } else {
            int attackers = Math.min(3, from.getArmyCount() - 1);
            int[] attDices = new int[]{0, 0, 0};
            int[] defDices = new int[]{0, 0};

            for (int i = 0; i < attackers; i++)
                attDices[i] = (int) (Math.random() * 6) + 1;
            for (int i = 0; i < to.getArmyCount() && i < 2; i++)
                defDices[i] = (int) (Math.random() * 6) + 1;

            Arrays.sort(attDices);
            Arrays.sort(defDices);

            if (attDices[2] > defDices[1])
                to.decreaseArmyCount(1);
            else
                from.decreaseArmyCount(1);

            if (defDices[0] != 0 && attDices[1] != 0) // zweite Auswertung nur, wenn zwei Verteidiger und Angreifer
            {
                if (attDices[1] > defDices[0])
                    to.decreaseArmyCount(1);
                else
                    from.decreaseArmyCount(1);
            }

            if (to.getArmyCount() == 0) // übernehme Gebiet
            {
                to.setPlayer(activePlayer);
                to.setArmyCount(attackers);
                from.decreaseArmyCount(attackers);
                attackedTerritory = to;
                attackedFromTerritory = from;
            }
            gameBoard.repaint();

            // Verhindert weiteres Verschieben, wenn Land in Angriff verwickelt war.
            if (movePossible && (from == moveFromTerritory || to == moveFromTerritory))
                movePossible = false;
        }
    }

    @Override
    public void moveArmy(Territory from, Territory to, int count) {
        if (!claimPhase && !reinforcePhase && from.getPlayer() == activePlayer
                && to.getPlayer() == activePlayer && from.isNeighbor(to)) {
            // Armeen nach Angriff nachziehen...
            if (from == attackedFromTerritory && to == attackedTerritory) {
                if (from.getArmyCount() > count) {
                    from.decreaseArmyCount(count);
                    to.increaseArmyCount(count);
                    gameBoard.repaint();
                }
                return;
            }

            if (moveFromTerritory == null) {
                moveFromTerritory = from;
                moveToTerritory = to;
            }
            if (movePossible && ((moveFromTerritory == from && moveToTerritory == to) || (moveFromTerritory == to && moveToTerritory == from))) {
                if (from.getArmyCount() > count) {
                    from.decreaseArmyCount(count);
                    to.increaseArmyCount(count);
                    gameBoard.repaint();
                }
            }
        }
    }

    @Override
    public void placeReinforcements(Territory territory, int count) {
        if (!claimPhase && reinforcePhase && territory.getPlayer() == activePlayer) {
            if (count > 0 && count <= activeReinforcements) {
                activeReinforcements -= count;
                territory.increaseArmyCount(count);
                gameBoard.repaint(territory.getBounds().x, territory.getBounds().y, territory.getBounds().width, territory.getBounds().height);
            }
        }
    }

    private void invokeNextTurn()
    {
        if (players.get(activePlayer).isAI())
            return;

        if (AIThread.isAlive())
        {
            System.out.println("Achtung, es wurde versucht, den Zug zu beenden, obwohl noch ein AIThread läuft!");
            AIThread.interrupt();
        }

        AIThread = new Thread(this::nextTurn);
        AIThread.start();
    }

    private void nextTurn() {
        do {
            // Siegbedingung
            if (!claimPhase && territories.stream().allMatch(t -> t.getPlayer() == activePlayer)) {
                System.out.println("Spieler " + players.get(activePlayer).getName() + " hat gewonnen!");
                nextTurnBtn.setEnabled(false);
                return;
            }

            // Index weiter
            if (++activePlayer == players.size()) {
                activePlayer = 0;

                reinforcePhase = !reinforcePhase;
                nextTurnBtn.setEnabled(!reinforcePhase && !claimPhase);
            }

            if (!claimPhase && territories.stream().noneMatch(t -> t.getPlayer() == activePlayer))
                nextTurn();


            if (reinforcePhase) {
                activeReinforcements = 0;
                for (Territory t : territories) {
                    if (t.getPlayer() == activePlayer)
                        activeReinforcements++;
                }

                activeReinforcements /= 3;
                if (activeReinforcements < 3) // entspricht nicht den Vorschreibungen, aber den normalen Risiko-Regeln - abklären!
                    activeReinforcements = 3;

                for (Continent c : continents) {
                    if (c.getTerritories().stream().allMatch(t -> t.getPlayer() == activePlayer))
                        activeReinforcements += c.getReinforcements();
                }
            } else {
                movePossible = true;
                moveFromTerritory = null;
                moveToTerritory = null;
                attackedFromTerritory = null;
                attackedTerritory = null;
            }

            if (players.get(activePlayer).isAI()) {
                nextTurnBtn.setEnabled(false);

                startAITurn();
            } else
                nextTurnBtn.setEnabled(!claimPhase && !reinforcePhase);
        } while (players.get(activePlayer).isAI());

    }

    private void startAITurn() {
        if (!players.get(activePlayer).isAI())
            return;

        AI ai = allAIs.get(players.get(activePlayer).getPlayerType());

        if (claimPhase)
            ai.claimTurn(activePlayer);
        else if (reinforcePhase) {
            ai.reinforceTurn(activePlayer, activeReinforcements);
        } else {
            ai.movementTurn(activePlayer);
        }
    }

}