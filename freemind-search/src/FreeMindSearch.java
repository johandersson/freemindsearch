import java.awt.Desktop;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class FreeMindSearch {

    public static void main(String[] args) {
        String searchText;

        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Please enter the text string to search for: ");
            searchText = scanner.nextLine();
        } else {
            searchText = args[0];
        }

        File folder = new File("C:\\Users\\johand\\Documents\\mindmaps");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mm"));

        if (files == null) {
            System.out.println("No .mm files found in the specified directory.");
            return;
        }

        List<SearchResult> results = new ArrayList<>();

        for (File file : files) {
            try {
                // Check if the file is empty
                if (file.length() == 0) {
                    System.out.println("Skipping empty file: " + file.getName());
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
                        if (nodeText.contains(searchText)) {
                            results.add(new SearchResult(file.getName(), nodeText));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error processing file: " + file.getName());
                e.printStackTrace();
            }
        }

        if (results.isEmpty()) {
            System.out.println("No matches found.");
            return;
        }

        System.out.println("Matches found:");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.println((i + 1) + ". File: " + result.fileName);
            System.out.println("   First found node text: " + result.nodeText);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of the map you want to open in FreeMind (or press Enter to do nothing): ");
        String input = scanner.nextLine();

        if (!input.isEmpty()) {
            try {
                int choice = Integer.parseInt(input);
                if (choice > 0 && choice <= results.size()) {
                    SearchResult chosenResult = results.get(choice - 1);
                    File fileToOpen = new File("C:\\Users\\johand\\Documents\\mindmaps\\" + chosenResult.fileName);
                    Desktop.getDesktop().open(fileToOpen);
                } else {
                    System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class SearchResult {
        String fileName;
        String nodeText;

        SearchResult(String fileName, String nodeText) {
            this.fileName = fileName;
            this.nodeText = nodeText;
        }
    }
}
