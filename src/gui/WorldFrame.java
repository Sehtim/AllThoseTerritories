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

    public WorldFrame(List<Player> players, List<Territory> territories, List<Continent> continents) {
        this.continents = continents;
        this.players = players;
        this.territories = territories;

        setTitle("AllThoseTerritories");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        Canvas gameBoard = new Canvas() {
            @Override
            public void paint(Graphics g) {
                paintGameBoard(g);
            }
        };
        gameBoard.setSize(1250, 650);
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
                        break;
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.BLACK));
        JButton nextTurnBtn = new JButton("Zug beenden");
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
    }
}