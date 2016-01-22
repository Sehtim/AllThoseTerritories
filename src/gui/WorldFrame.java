package gui;

import data.Continent;
import data.Player;
import data.Territory;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class WorldFrame extends JFrame {

    public static final int WIDTH = 1250;
    public static final int HEIGHT = 650;

    private List<Continent> continents;
    private List<Territory> territories;
    private List<Player> players;

    private int activePlayer;
    private int activeReinforcements;

    private Territory selectedTerritory;
    private Territory attackedTerritory;

    private Territory moveFromTerritory;
    private Territory moveToTerritory;
    private boolean movePossible;

    private boolean showTerritoryNames;
    private Font drawFont;

    private boolean claimPhase;
    private boolean reinforcePhase;

    private final JButton nextTurnBtn;

    public WorldFrame(List<Player> players, List<Territory> territories, List<Continent> continents) {
        this.continents = continents;
        this.players = players;
        this.territories = territories;

        this.drawFont = new Font("Arial", Font.BOLD, 12);
        this.showTerritoryNames = false;

        claimPhase = true;
        activePlayer = 0;

        setTitle("AllThoseTerritories");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        Canvas gameBoard = new Canvas() {
            @Override
            public void paint(Graphics g) {
                paintGameBoard(g);
            }
        };
        gameBoard.setSize(WIDTH, HEIGHT);
        //gameBoard.createBufferStrategy(2);
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
                            gameBoard.repaint();
                        } else {
                            if (reinforcePhase) {
                                reinforceTerritory(territory, 1);
                                gameBoard.repaint();
                            } else {
                                if (e.getButton() == MouseEvent.BUTTON1) {
                                    selectedTerritory = territory;
                                } else if (e.getButton() == MouseEvent.BUTTON2) {
                                    attackedTerritory = territory;
                                }

                                gameBoard.repaint();
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
        buttonPanel.add(nextTurnBtn);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(gameBoard, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
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

    public boolean claimTerritory(Territory territory) {
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

                nextTurn();

                return true;
            } else {
                // Evtl Meldung anzeigen: schon belegt
                return false;
            }


        } else {
            System.out.println("Achtung: claimTerritory falsch aufgerufen!");
            return false;
        }
    }

    public boolean reinforceTerritory(Territory territory, int count) {
        if (!claimPhase && reinforcePhase && territory.getPlayer() == activePlayer) {
            if (count > 0 && count <= activeReinforcements) {
                activeReinforcements -= count;
                territory.setArmyCount(territory.getArmyCount() + count);
                if (activeReinforcements == 0)
                    nextTurn();
                return true;
            }
        }

        return false;
    }

    private void nextTurn() {
        if (++activePlayer == players.size()) {
            activePlayer = 0;

            reinforcePhase = !reinforcePhase;
            nextTurnBtn.setEnabled(!reinforcePhase && !claimPhase);
        }

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

        }

    }

}