package gui;

import data.Continent;
import data.PlayerType;
import data.Territory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetupFrame extends JFrame {

    private static final String PATCH_OF = "patch-of";
    private static final String CAPITAL_OF = "capital-of";
    private static final String NEIGHBORS_OF = "neighbors-of";
    private static final String CONTINENT = "continent";

    private Map<String, Territory> territories;
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

        territories = new HashMap<String, Territory>();
        continents = new ArrayList<Continent>();
        try {
            FileReader fileReader = new FileReader(mapFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            String territoryName;
            Territory territory;
            List<Integer> xCoordinateList;
            List<Integer> yCoordinateList;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(PATCH_OF)) {
                    // Zeile beginnt mit patch-of. Namen und Koordinaten heraus parsen
                    xCoordinateList = new ArrayList<Integer>();
                    yCoordinateList = new ArrayList<Integer>();
                    territoryName = readCoordinates(line, PATCH_OF.length(), xCoordinateList, yCoordinateList);

                    // Patch zum jeweiligen Territory hinzufügen
                    territory = territories.get(territoryName);
                    if (territory == null) {
                        territory = new Territory(territoryName);
                        territories.put(territoryName, territory);
                    }

                    int[] xCoords = new int[xCoordinateList.size()];
                    int[] yCoords = new int[yCoordinateList.size()];
                    for (int i = 0; i < xCoordinateList.size(); i++) {
                        xCoords[i] = xCoordinateList.get(i);
                    }
                    for (int i = 0; i < yCoordinateList.size(); i++) {
                        yCoords[i] = yCoordinateList.get(i);
                    }
                    territory.addPart(new Polygon(xCoords, yCoords, xCoords.length));
                } else if (line.startsWith(CAPITAL_OF)) {
                    // Zeile beginnt mit capital-of. Namen und Koordinaten heraus parsen
                    xCoordinateList = new ArrayList<Integer>();
                    yCoordinateList = new ArrayList<Integer>();
                    territoryName = readCoordinates(line, CAPITAL_OF.length(), xCoordinateList, yCoordinateList);

                    if (xCoordinateList.isEmpty() || yCoordinateList.isEmpty()) {
                        throw new IOException("Folgende Zeile entspricht nicht dem erwarteten Format capital-of <Name> <x> <y>: " + line);
                    }

                    // Capital des jeweiligen Territory setzen
                    territory = territories.get(territoryName);
                    if (territory == null) {
                        territory = new Territory(territoryName);
                        territories.put(territoryName, territory);
                    }
                    territory.setCapitalPosition(new Point(xCoordinateList.get(0), yCoordinateList.get(0)));
                } else if (line.startsWith(NEIGHBORS_OF)) {
                    // Zeile beginnt mit neighbors-of. Namen heraus parsen
                    String[] parts = line.substring(NEIGHBORS_OF.length()).split(":");
                    if (parts.length != 2) {
                        throw new IOException("Folgende Zeile entspricht nicht dem erwarteten Format neighbors-of <Name> : <Name> - <Name>...: " + line);
                    }
                    territoryName = parts[0].trim();

                    parts = parts[1].split("-");
                    Territory temp;
                    for (String part : parts) {
                        part = part.trim();
                        if (part.isEmpty()) {
                            continue;
                        }
                        territory = territories.get(territoryName);
                        if (territory == null) {
                            territory = new Territory(territoryName);
                            territories.put(territoryName, territory);
                        }
                        temp = territories.get(part);
                        if (temp == null) {
                            temp = new Territory(part);
                            territories.put(part, temp);
                        }
                        territory.addNeighbor(temp);
                    }
                } else if (line.startsWith(CONTINENT)) {
                    // Zeile beginnt mit continent. Namen heraus parsen
                    String[] parts = line.substring(CONTINENT.length()).split(":");
                    String errorMessage = "Folgende Zeile entspricht nicht dem erwarteten Format continent <Name> <Verstärkungen>: <Name> - <Name>...: " + line;
                    if (parts.length != 2) {
                        throw new IOException(errorMessage);
                    }
                    String continent = parts[0].trim();
                    // Aus erstem Teil die Verstärkungen rausparsen
                    int index_reinforcements;
                    for (index_reinforcements = 0; index_reinforcements < continent.length(); index_reinforcements++) {
                        if (Character.isDigit(continent.charAt(index_reinforcements))) {
                            break;
                        }
                    }
                    if (index_reinforcements == continent.length()) {
                        throw new IOException(errorMessage);
                    }
                    int reinforcments;
                    try {
                        reinforcments = Integer.parseInt(continent.substring(index_reinforcements).trim());
                    } catch (NumberFormatException ex) {
                        throw new IOException(errorMessage, ex);
                    }
                    continent = continent.substring(0, index_reinforcements).trim();


                    parts = parts[1].split("-");
                    List<Territory> continentTerritories = new ArrayList<Territory>();
                    for (String part : parts) {
                        part = part.trim();
                        if (part.isEmpty()) {
                            continue;
                        }
                        territory = territories.get(part);
                        if (territory == null) {
                            territory = new Territory(part);
                            territories.put(part, territory);
                        }
                        continentTerritories.add(territory);
                    }
                    continents.add(new Continent(continent, continentTerritories, reinforcments));
                }
            }
            validFile = true;
        } catch (IOException e) {
            e.printStackTrace();
            // Substring damit die Fehlermeldung nicht über 3 Bildschirme geht : )
            JOptionPane.showMessageDialog(this, "Beim lesen der .map-Datei ist ein Fehler aufgetreten: " + e.getMessage().substring(0, 150), "Fehler beim Lesen", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String readCoordinates(String line, int fromLine, List<Integer> xCoordinateList, List<Integer> yCoordinateList) throws IOException {
        String[] parts = line.substring(fromLine).split(" ");
        String territoryName = "";
        boolean isXCoordinate = true;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (Character.isDigit(part.toCharArray()[0])) {
                // Koordinaten - abwechselnd x und y Koordinate
                try {
                    if (isXCoordinate) {
                        xCoordinateList.add(Integer.parseInt(part));
                    } else {
                        yCoordinateList.add(Integer.parseInt(part));
                    }
                } catch (NumberFormatException ex) {
                    throw new IOException("Folgende Zeile entspricht nicht dem erwarteten Format xxx-of <Name> <x> <y> <x> <y>...: " + line, ex);
                }
                isXCoordinate = !isXCoordinate;
            } else {
                // Name immer erweitern, da dieser Leerzeichen enthalten kann
                territoryName += (territoryName.isEmpty() ? "" : " ") + part;
            }
        }

        if (xCoordinateList.size() != yCoordinateList.size()) {
            throw new IOException("Folgende Zeile enthält nicht gleich viele X- und Y-Koordinaten: " + line);
        }

        return territoryName;
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
        playerSettingsPanel.addPlayer("Spieler1", PlayerType.SPIELER, Color.BLUE);
        playerSettingsPanel.addPlayer("Computer", PlayerType.NORMAL, Color.RED);
        JScrollPane playerScrollPane = new JScrollPane(playerSettingsPanel);
        playerScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        playerScrollPane.setBorder(new TitledBorder("Teilnehmer"));

        // ############
        // South
        JButton startButton = new JButton("Spiel beginnen");
        startButton.addActionListener(e -> {
            if (validFile) {
                new WorldFrame(playerSettingsPanel.getPlayerList(), new ArrayList<Territory>(territories.values()), continents).setVisible(true);
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
