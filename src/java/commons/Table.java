package commons;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Table {

    private static final String DELIMITER = ";";
    private final List<String> headers;
    private final List<Entry> entries;
    private boolean lazyPrint;
    private BufferedWriter bufferedWriter;

    public Table(List<String> headers) {
        this.headers = headers;
        this.entries = new ArrayList<>();
        this.lazyPrint = false;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    public Table(List<String> headers, boolean lazyPrint) {
        this.headers = headers;
        this.entries = new ArrayList<>();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
        if (lazyPrint) {
            lazyPrint();
        }
    }

    public Table(List<String> headers, boolean lazyPrint, String filename) {
        this.headers = headers;
        this.entries = new ArrayList<>();
        setOutput(filename);
        if (lazyPrint) {
            lazyPrint();
        }
    }

    public static Table ofHeaders(String... headers) {
        return new Table(Arrays.stream(headers).toList());
    }

    public void setOutput(String filename) {
        try {
            this.bufferedWriter = new BufferedWriter(new FileWriter(Utils.RESOURCES_PATH + filename));
        } catch (IOException e) {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
        }
    }

    public void lazyPrint() {
        this.lazyPrint = true;
        print();
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
        if (lazyPrint) {
            printLine(entry.getFields());
        }
    }

    public void print() {
        printLine(headers);
        for (Entry entry : entries) {
            printLine(entry.getFields());
        }
    }

    public void close() {
        try {
            bufferedWriter.newLine();
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printLine(List<String> fields) {
        try {
            bufferedWriter.write(String.join(DELIMITER, fields));
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Entry {

        List<String> getFields();

    }

}
