package commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Table {

    private static final String DELIMITER = ";";
    private final List<String> headers;
    private final List<Entry> entries;

    public Table(List<String> headers) {
        this.headers = headers;
        this.entries = new ArrayList<>();
    }

    public static Table ofHeaders(String... headers) {
        return new Table(Arrays.stream(headers).toList());
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    public void print() {
        printLine(headers);
        for (Entry entry : entries) {
            printLine(entry.getFields());
        }
    }

    private void printLine(List<String> fields) {
        System.out.println(String.join(DELIMITER, fields));
    }

    public interface Entry {

        List<String> getFields();

    }

}
