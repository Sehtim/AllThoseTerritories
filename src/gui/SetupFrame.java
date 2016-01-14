package gui;

import data.Continent;
import data.Territory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SetupFrame extends JFrame {
    private List<Territory> territories;
    private List<Continent> continents;

    private JTextField mapFileTF;
    private boolean validFile;

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].endsWith(".map")) {
            System.out.println("Richtiger Aufruf: AllThoseTerritories <Pfad.map>");
            return;
        }

        new SetupFrame(args[0]).setVisible(true);
    }

    private void initFromFile(File mapFile) {
        validFile = false;
        if (!mapFile.exists()) {
            JOptionPane.showMessageDialog(this, "Der angegebene Pfad ist ungültig!", "Falscher Pfad", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!mapFile.getName().endsWith(".map")) {
            JOptionPane.showMessageDialog(this, "Die Datei muss die Endung .map haben!", "Falscher Dateityp", JOptionPane.ERROR_MESSAGE);
            return;
        }

        territories = new ArrayList<Territory>();
        continents = new ArrayList<Continent>();
        // TODO Map auslesen und territories und continents befüllen

        validFile = true;
    }

    public SetupFrame(String pathToMap) {
        initFromFile(new File(pathToMap));

        setTitle("AllThoseTerritories");
        setSize(512, 512);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);


        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // ############
        // North
        mapFileTF = new JTextField();
        mapFileTF.setColumns(20);
        mapFileTF.setText(pathToMap);
        JButton chooseMapFileBtn = new JButton("...");
        chooseMapFileBtn.addActionListener(e -> {
            JFileChooser mapChooser = new JFileChooser(mapFileTF.getText());
            if (mapChooser.showOpenDialog(SetupFrame.this) == JOptionPane.OK_OPTION) {
                File mapFile = mapChooser.getSelectedFile();
                mapFileTF.setText(mapFile.getPath());
                initFromFile(mapFile);
            }
        });

        JPanel mapFilePanel = new JPanel();
        mapFilePanel.add(new JLabel("Map-Datei:"));
        mapFilePanel.add(mapFileTF);
        mapFilePanel.add(chooseMapFileBtn);

        // ############
        // Center
        LinkedPlayerPanel playerSettingsPanel = new LinkedPlayerPanel();
        playerSettingsPanel.addPlayer("Spieler1", false, Color.BLUE);
        playerSettingsPanel.addPlayer("Computer", true, Color.RED);
        JScrollPane playerScrollPane = new JScrollPane(playerSettingsPanel);
        playerScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        playerScrollPane.setBorder(new TitledBorder("Teilnehmer"));

        // ############
        // South
        JButton startButton = new JButton("Spiel beginnen");
        startButton.addActionListener(e -> {
            if (validFile) {
                new WorldFrame(playerSettingsPanel.getPlayerList(), territories, continents).setVisible(true);
                SetupFrame.this.dispose();
            } else {
                JOptionPane.showMessageDialog(SetupFrame.this, "Das Spiel kann nur mit einer gültigen .map-Datei begonnen werden!", "Ungültige Karte", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(startButton);

        contentPane.add(mapFilePanel, BorderLayout.NORTH);
        contentPane.add(playerScrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }
}
