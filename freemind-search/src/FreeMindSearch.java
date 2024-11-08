import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class FreeMindSearch extends JFrame {
    private JTextField searchField;
    private JTextArea resultArea;
    private DefaultListModel<SearchResult> listModel;
    private JList<SearchResult> resultList;

    public FreeMindSearch() {
        setTitle("FreeMind Search");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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

        JScrollPane scrollPane = new JScrollPane(resultList);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(searchField, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    private void search() {
        listModel.clear();
        String searchText = searchField.getText().toLowerCase();

        File folder = new File("C:\\Users\\johand\\Documents\\mindmaps");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mm"));

        if (files == null) {
            resultArea.setText("No .mm files found in the specified directory.");
            return;
        }

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
                        if (nodeText.toLowerCase().equals(searchText)) {
                            listModel.addElement(new SearchResult(file.getName(), nodeText));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (listModel.isEmpty()) {
            resultArea.setText("No matches found.");
        }
    }

    private void openSelectedFile() {
        SearchResult selectedResult = resultList.getSelectedValue();
        if (selectedResult != null) {
            try {
                File fileToOpen = new File("C:\\Users\\johand\\Documents\\mindmaps\\" + selectedResult.fileName);
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

        SearchResult(String fileName, String nodeText) {
            this.fileName = fileName;
            this.nodeText = nodeText;
        }

        @Override
        public String toString() {
            return fileName + " - " + nodeText;
        }
    }
}
