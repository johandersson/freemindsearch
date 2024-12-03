import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class FreeMindSearch extends JFrame {
    private JTextField searchField;
    private JCheckBox searchNotesCheckBox;
    private JTextArea resultArea;
    private DefaultListModel<SearchResult> listModel;
    private JList<SearchResult> resultList;
    private File defaultFolder = null;

    public FreeMindSearch() {
        setTitle("FreeMind Search");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load default folder
        loadDefaultFolder();

        // Create Menu Bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Add "Help" Menu
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        helpMenu.add(aboutMenuItem);

        // About Menu Item Action
        aboutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,
                        "<html><body style='font-family:Arial;font-size:12px;'>" +
                                "<strong>Freemind Search Multiple Maps.</strong><br>" +
                                "Copyright Johan Andersson.<br>" +
                                "License: MIT." +
                                "</body></html>",
                        "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Add "Settings" Menu
        JMenu settingsMenu = new JMenu("Settings");
        menuBar.add(settingsMenu);

        JMenuItem setDefaultFolderMenuItem = new JMenuItem("Set Default Folder");
        settingsMenu.add(setDefaultFolderMenuItem);

        // Set Default Folder Menu Item Action
        setDefaultFolderMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDefaultFolder();
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });

        searchNotesCheckBox = new JCheckBox("Search inside notes");
        searchNotesCheckBox.setSelected(false);

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchNotesCheckBox, BorderLayout.EAST);

        resultArea = new JTextArea();
        resultArea.setEditable(false);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SearchResult) {
                    SearchResult result = (SearchResult) value;
                    if (result.isNote) {
                        c.setBackground(new Color(255, 255, 153)); // Light Yellow (Sticky Note Color)
                        c.setForeground(new Color(102, 102, 0)); // Darker Text for good contrast
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                    if (isSelected) {
                        c.setBackground(list.getSelectionBackground());
                        c.setForeground(list.getSelectionForeground());
                    }
                }
                return c;
            }
        });
        resultList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openSelectedFile();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultList);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(searchPanel, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(resultScrollPane, BorderLayout.SOUTH);
    }

    private void setDefaultFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = folderChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            defaultFolder = folderChooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(new FileWriter(getHomeDirectoryFile()))) {
                out.println(defaultFolder.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadDefaultFolder() {
        File file = getHomeDirectoryFile();
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                defaultFolder = new File(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getHomeDirectoryFile() {
        return new File(System.getProperty("user.home"), "defaultFolder.txt");
    }

    private void search() {
        if (defaultFolder == null) {
            JOptionPane.showMessageDialog(this, "No default folder is set. Please select a folder.");
            setDefaultFolder();
            if (defaultFolder == null) {
                return;
            }
        }

        listModel.clear();
        resultArea.setText("Searching...");

        String searchText = searchField.getText().toLowerCase();
        boolean searchNotes = searchNotesCheckBox.isSelected();

        if (searchText.length() <= 1) {
            resultArea.setText("Search text must be more than 1 character.");
            return;
        }

        File[] files = defaultFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mm"));

        if (files == null) {
            resultArea.setText("No .mm files found in the specified directory.");
            return;
        }

        // Sort files by last modified date, latest first
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        boolean matchFound = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (File file : files) {
            try {
                if (file.length() == 0) {
                    continue;
                }

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                NodeList nodeList = doc.getElementsByTagName("node");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String nodeText = element.getAttribute("TEXT").toLowerCase();
                        boolean textMatch = nodeText.contains(searchText);

                        // Check if we should search inside notes
                        boolean notesMatch = false;
                        if (searchNotes) {
                            NodeList richContentNodes = element.getElementsByTagName("richcontent");
                            for (int j = 0; j < richContentNodes.getLength(); j++) {
                                Node richContentNode = richContentNodes.item(j);
                                String richContentText = richContentNode.getTextContent().toLowerCase();
                                if (richContentText.contains(searchText)) {
                                    notesMatch = true;
                                    String displayText = "Note: " + (richContentText.length() > 50 ? richContentText.substring(0, 50) + "..." : richContentText);
                                    listModel.addElement(new SearchResult(file.getName(), displayText, sdf.format(file.lastModified()), true));
                                    matchFound = true;
                                    break;
                                }
                            }
                        }

                        if (textMatch && !notesMatch) {
                            listModel.addElement(new SearchResult(file.getName(), nodeText, sdf.format(file.lastModified()), false));
                            matchFound = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!matchFound) {
            resultArea.setText("No matches found.");
        } else {
            resultArea.setText(""); // Clear the status message when done
        }
    }

    private void openSelectedFile() {
        SearchResult selectedResult = resultList.getSelectedValue();
        if (selectedResult != null) {
            try {
                File fileToOpen = new File(defaultFolder, selectedResult.fileName);
                Desktop.getDesktop().open(fileToOpen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new FreeMindSearch().setVisible(true);
            }
        });
    }

    static class SearchResult {
        String fileName;
        String nodeText;
        String lastModified;
        boolean isNote;

        SearchResult(String fileName, String nodeText, String lastModified, boolean isNote) {
            this.fileName = fileName;
            this.nodeText = nodeText;
            this.lastModified = lastModified;
            this.isNote = isNote;
        }

        @Override
        public String toString() {
            return fileName + " - " + nodeText + " (Last Modified: " + lastModified + ")";
        }
    }
}
