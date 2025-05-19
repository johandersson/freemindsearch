import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class FreeMindSearch extends JFrame {
    private static final String DEFAULT_FOLDER_FILE = "defaultFolder.txt";

    private JTextField searchField;
    private JCheckBox searchNotesCheckBox;
    private DefaultListModel<SearchResult> listModel;
    private JList<SearchResult> resultList;
    private File defaultFolder;

    public FreeMindSearch() {
        setupUI();
        loadDefaultFolder();
    }

    private void setupUI() {
        setTitle("FreeMind Search");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setJMenuBar(createMenuBar());

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.addActionListener(e -> search());

        searchNotesCheckBox = new JCheckBox("Search inside notes");

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchNotesCheckBox, BorderLayout.EAST);

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

        JScrollPane scrollPane = new JScrollPane(resultList);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(searchPanel, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutMenuItem);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem setDefaultFolderMenuItem = new JMenuItem("Set Default Folder");
        setDefaultFolderMenuItem.addActionListener(e -> setDefaultFolder());
        settingsMenu.add(setDefaultFolderMenuItem);

        menuBar.add(helpMenu);
        menuBar.add(settingsMenu);

        return menuBar;
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "<html><strong>Freemind Search Multiple Maps.</strong><br>Copyright Johan Andersson.<br>License: MIT.</html>",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setDefaultFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = folderChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            defaultFolder = folderChooser.getSelectedFile();
            saveDefaultFolder();
        }
    }

    private void loadDefaultFolder() {
        File file = new File(System.getProperty("user.home"), DEFAULT_FOLDER_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                defaultFolder = new File(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveDefaultFolder() {
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(System.getProperty("user.home"), DEFAULT_FOLDER_FILE)))) {
            out.println(defaultFolder.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void search() {
        if (defaultFolder == null || searchField.getText().trim().isEmpty()) return;

        listModel.clear();
        File[] files = defaultFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mm"));

        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, "No FreeMind files found in the selected folder.", "No Files Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        String searchText = searchField.getText().toLowerCase();
        boolean searchNotes = searchNotesCheckBox.isSelected();
        boolean matchFound = false;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (File file : files) {
            if (file.length() == 0) continue;

            try {
                Document doc = parseXmlFile(file);
                NodeList nodeList = doc.getElementsByTagName("node");

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    String nodeText = element.getAttribute("TEXT").toLowerCase();

                    boolean textMatch = nodeText.contains(searchText);
                    boolean notesMatch = false;

                    if (searchNotes) {
                        notesMatch = searchInNotes(element, searchText, file, sdf);
                    }

                    if (textMatch && !notesMatch) {
                        listModel.addElement(new SearchResult(file.getName(), nodeText, sdf.format(file.lastModified()), false));
                        matchFound = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!matchFound) {
            JOptionPane.showMessageDialog(this, "No matches found.", "Search Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private Document parseXmlFile(File file) throws Exception {
        String xmlContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        xmlContent = xmlContent.replaceAll("&nbsp;", " ");

        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputStream correctedStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        Document doc = dBuilder.parse(correctedStream);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private boolean searchInNotes(Element element, String searchText, File file, SimpleDateFormat sdf) {
        NodeList richContentNodes = element.getElementsByTagName("richcontent");
        for (int j = 0; j < richContentNodes.getLength(); j++) {
            String richContentText = richContentNodes.item(j).getTextContent().toLowerCase();
            if (richContentText.contains(searchText)) {
                listModel.addElement(new SearchResult(file.getName(),
                        "Note: " + (richContentText.length() > 50 ? richContentText.substring(0, 50) + "..." : richContentText),
                        sdf.format(file.lastModified()), true));
                return true;
            }
        }
        return false;
    }

    private void openSelectedFile() {
        SearchResult selectedResult = resultList.getSelectedValue();
        if (selectedResult != null) {
            try {
                Desktop.getDesktop().open(new File(defaultFolder, selectedResult.fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FreeMindSearch().setVisible(true));
    }
}
