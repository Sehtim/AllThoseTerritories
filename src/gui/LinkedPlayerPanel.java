package gui;

import data.Player;
import data.PlayerType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LinkedPlayerPanel extends JPanel {

    public LinkedPlayerPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void addPlayer(String name, PlayerType playerType, Color color) {
        addPlayer(name, playerType, color, -1);
    }

    private void addPlayer(String name, PlayerType playerType, Color color, int index) {
        JPanel newPanel = new PlayerPanel(name, playerType, color);
        newPanel.setMaximumSize(newPanel.getPreferredSize()); // Damit das Boxlayout die Panels nicht gleichmäßig verteilt

        add(newPanel, index);
        revalidate();
        repaint();
    }

    private Color getRandomColor() {
        return new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
    }

    public List<Player> getPlayerList() {
        List<Player> players = new ArrayList<Player>();
        for (Component comp : getComponents()) {
            PlayerPanel panel = (PlayerPanel) comp;
            players.add(panel.getPlayerObject());
        }
        return players;
    }

    private class PlayerPanel extends JPanel {

        private JComboBox<PlayerType> playerTypeBox;
        private JTextField playerNameTF;
        private JButton colorButton;

        public PlayerPanel(String name, PlayerType playerType, Color color) {
            playerNameTF = new JTextField(name, 15);
            playerTypeBox = new JComboBox<>(PlayerType.values());
            playerTypeBox.setSelectedItem(playerType);
            colorButton = new JButton();
            colorButton.setBackground(color);
            colorButton.addActionListener(e -> {
                Color choosenColor = JColorChooser.showDialog(LinkedPlayerPanel.this, "Farbe wählen", colorButton.getBackground());
                if (choosenColor != null) {
                    colorButton.setBackground(choosenColor);
                }
            });

            JButton removeButton = new JButton("-");
            removeButton.addActionListener(e -> {
                if (LinkedPlayerPanel.this.getComponentCount() == 2) {
                    JOptionPane.showMessageDialog(LinkedPlayerPanel.this, "Es müssen mindestens 2 Teilnehmer vorhanden sein!", "Zu wenig Teilnehmer", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                LinkedPlayerPanel.this.remove(PlayerPanel.this);
                LinkedPlayerPanel.this.revalidate();
                LinkedPlayerPanel.this.repaint();
            });

            JButton addButton = new JButton("+");
            addButton.addActionListener(e -> {
                Component[] components = LinkedPlayerPanel.this.getComponents();
                int i;
                for (i = 0; i < components.length; i++) {
                    if (this.equals(components[i])) {
                        break;
                    }
                }
                // Wenn letztes Panel: -1 übergeben, damit am Ende angefügt wird.
                // Sonst am nächsten index einfügen
                addPlayer("Neuer Spieler", PlayerType.NORMAL, getRandomColor(), ++i == components.length ? -1 : i);
            });

            add(playerTypeBox);
            add(playerNameTF);
            add(colorButton);
            add(removeButton);
            add(addButton);
        }

        public Player getPlayerObject() {
            return new Player(playerNameTF.getText(), (PlayerType) playerTypeBox.getSelectedItem(), colorButton.getBackground());
        }
    }
}
