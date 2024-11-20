import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class FreeMindSearch extends JFrame {
    private JTextField searchField;
    private JTextArea resultArea;
    private DefaultListModel<SearchResult> listModel;
    private JList<SearchResult> resultList;
    private JCheckBox subfolderCheckBox;
    private File defaultFolder = null;

    public FreeMindSearch() {
        setTitle("FreeMind Search");
        // Make the window bigger
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

        searchField = new JTextField();
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });

        resultArea = new JTextArea();
        resultArea.setEditable(false);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openSelectedFile();
                }
            }
        });

        // Create subfolder checkbox
        subfolderCheckBox = new JCheckBox("Include subfolders");
        subfolderCheckBox.setSelected(false); // Default setting: do not search in subfolders

        JScrollPane scrollPane = new JScrollPane(resultList);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(subfolderCheckBox, BorderLayout.EAST);
        contentPane.add(topPanel, BorderLayout.NORTH);
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

        if (searchText.length() <= 1) {
            resultArea.setText("Search text must be more than 1 character.");
            return;
        }

        List<File> files = new ArrayList<>();
        collectFiles(defaultFolder, files, subfolderCheckBox.isSelected());

        if (files.isEmpty()) {
            resultArea.setText("No .mm files found in the specified directory.");
            return;
        }

        // Sort files by last modified date, latest first
        files.sort(Comparator.comparingLong(File::lastModified).reversed());

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
                        String nodeText = element.getAttribute("TEXT");
                        if (nodeText.toLowerCase().contains(searchText)) {
                            listModel.addElement(new SearchResult(file.getName(), nodeText, sdf.format(file.lastModified())));
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

    private void collectFiles(File folder, List<File> files, boolean includeSubfolders) {
        File[] listFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mm") || (includeSubfolders && new File(dir, name).isDirectory()));

        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isDirectory() && includeSubfolders) {
                    collectFiles(file, files, true);
                } else if (file.isFile()) {
                    files.add(file);
                }
            }
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

        SearchResult(String fileName, String nodeText, String lastModified) {
            this.fileName = fileName;
            this.nodeText = nodeText;
            this.lastModified = lastModified;
        }

        @Override
        public String toString() {
            return fileName + " - " + nodeText + " (Last Modified: " + lastModified + ")";
        }
    }
}
