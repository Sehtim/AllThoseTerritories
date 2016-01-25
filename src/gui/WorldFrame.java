package gui;

import ai.AI;
import ai.EasyAI;
import ai.NormalAI;
import data.*;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
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

    private HashMap<Continent, Color> continentColors;
    private boolean continentViewMode;

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

    private final JLabel infoLabel;
    private final JButton nextTurnBtn;
    private final JComponent gameBoard;

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


        this.continentViewMode = false;
        continentColors = new HashMap<>();
        for (Continent c : continents) {
            continentColors.put(c, new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
        }

        setTitle("AllThoseTerritories");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        gameBoard = new JComponent() {
            @Override
            public void paint(Graphics g) {
                paintGameBoard(g);
            }
        };
        gameBoard.setSize(WIDTH, HEIGHT);
        gameBoard.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        gameBoard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // TODO evtl. für Tooltips
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                for (Territory territory : territories) {
                    if (territory.contains(e.getPoint())) {
                        if (players.get(activePlayer).isAI()) {
                            // Evtl Meldung "AI ist an der Reihe" oder Informationen anzeigen
                            break;
                        }

                        if (claimPhase) {
                            if (territory.getPlayer() == -1) {
                                claimTerritory(territory);
                                invokeNextTurn();
                            }
                        } else {
                            if (reinforcePhase) {
                                placeReinforcements(territory, 1);
                                if (activeReinforcements == 0)
                                    invokeNextTurn();
                            } else {
                                if (SwingUtilities.isLeftMouseButton(e)) {
                                    selectedTerritory = territory;
                                    gameBoard.repaint();
                                } else if (SwingUtilities.isRightMouseButton(e)) {
                                    if (selectedTerritory == null) {
                                        return; // Kein Territorium ausgewählt
                                    }
                                    if (selectedTerritory.getPlayer() != activePlayer) {
                                        return; // Territorium gehört einem nicht
                                    }
                                    if (selectedTerritory.getArmyCount() == 1) {
                                        return; // Man kann weder angreifen noch verschieben
                                    }
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
        infoLabel = new JLabel(players.get(activePlayer).getName() + ": Claim Phase");

        // TODO: evtl unteren Bereich in zwei Teile aufteilen - links für Informationen und rechts für nextTurn-Button - auch für Alignment!

        if (continents.size() > 0) {
            JPanel ContinentInfoPanel = new JPanel(new GridLayout(continents.size(), 2));
            for (Continent c : continents) {
                JLabel nameLabel = new JLabel(c.getName());
                nameLabel.setForeground(continentColors.get(c));

                JLabel reinforcementsLabel = new JLabel(String.valueOf(c.getReinforcements()), SwingConstants.RIGHT);
                reinforcementsLabel.setForeground(continentColors.get(c));

                ContinentInfoPanel.add(nameLabel);
                ContinentInfoPanel.add(reinforcementsLabel);
            }

            ContinentInfoPanel.setBorder(new TitledBorder("Kontinente"));

            ContinentInfoPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    continentViewMode = true;
                    gameBoard.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    continentViewMode = false;
                    gameBoard.repaint();
                }
            });

            buttonPanel.add(ContinentInfoPanel);

        }

        buttonPanel.add(infoLabel);
        buttonPanel.add(nextTurnBtn);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(gameBoard, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);

        gameBoard.setDoubleBuffered(true);

        if (players.get(activePlayer).isAI()) {
            Thread firstAITurn = new Thread() {
                @Override
                public void run() {
                    startAITurn();
                    AIThread.start();
                }
            };
            firstAITurn.start();
        }
    }

    private void paintGameBoard(Graphics g) {
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
        
        HashMap<Territory, Color> colorMap = new HashMap<>();
        if (continentViewMode) {
            for (Continent c : continents)
                for (Territory t : c.getTerritories())
                    colorMap.put(t, continentColors.get(c));
        } else {
            for (Territory t : territories) {
                if (t.getPlayer() != -1)
                    colorMap.put(t, players.get(t.getPlayer()).getColor());
            }
        }

        // Landflächen
        for (Territory territory : territories) {
            g.setColor(colorMap.getOrDefault(territory, Color.GRAY));
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
            g.setColor(Color.ORANGE);
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
        if (!claimPhase || territory.getPlayer() != -1) {
            System.out.println("Achtung: claimTerritory falsch aufgerufen!");
            return;
        }

        territory.setPlayer(activePlayer);
        territory.setArmyCount(1);

        gameBoard.repaint();
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
                infoLabel.setText(players.get(activePlayer).getName() + ": Verstärkungsphase (" + activeReinforcements + " verfügbar)");
                gameBoard.repaint();
            }
        }
    }

    private void invokeNextTurn() {
        if (AIThread.isAlive()) {
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
                infoLabel.setText(players.get(activePlayer).getName() + " hat gewonnen!");
                nextTurnBtn.setEnabled(false);
                return;
            }

            if (claimPhase && territories.stream().allMatch(t -> t.getPlayer() >= 0)) {
                claimPhase = false;
                reinforcePhase = true;
                activePlayer = 0;
            } else if (++activePlayer == players.size()) { // Index weiter
                activePlayer = 0;
                reinforcePhase = !reinforcePhase;
                nextTurnBtn.setEnabled(!reinforcePhase && !claimPhase);
            }

            if (!claimPhase && territories.stream().noneMatch(t -> t.getPlayer() == activePlayer))
                continue; // Spieler bereits ausgeschieden


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
                selectedTerritory = null;
                gameBoard.repaint(); // Selektiertes Territorium abwählen
            }

            if (players.get(activePlayer).isAI()) {
                nextTurnBtn.setEnabled(false);

                startAITurn();
            } else
                nextTurnBtn.setEnabled(!claimPhase && !reinforcePhase);

            infoLabel.setText(players.get(activePlayer).getName() + ": " + (claimPhase ? "Claim Phase" : (reinforcePhase ? "Verstärkungsphase (" + activeReinforcements + " verfügbar)" : "Angriffsphase")));
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