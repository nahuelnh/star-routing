import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {


    private static final String DELIMITER = " ";

    public static <T> Set<T> singletonSet(T elem) {
        return Stream.of(elem).collect(Collectors.toSet());
    }

    public static <T> List<T> singletonList(T elem) {
        return Stream.of(elem).toList();
    }

    private static List<Integer> readIntegerLine(String line) {
        return Arrays.stream(line.split(" ")).map(Integer::valueOf).collect(Collectors.toList());
    }

    public List<List<Integer>> parseIntegerMatrix(String filename) {
        List<List<Integer>> matrix = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Integer> values = readIntegerLine(line);
                matrix.add(values);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return matrix;
    }

    public Map<String, Integer> parseStringToIntMap(String filename) {
        Map<String, Integer> ret = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                String key = values[0];
                Integer value = Integer.valueOf(values[1]);
                ret.put(key, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }
}
