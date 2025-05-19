public class SearchResult {
    String fileName, nodeText, lastModified;
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