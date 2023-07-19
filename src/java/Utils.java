import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    private static final String DELIMITER = " ";

    private static List<Integer> readIntegerLine(String line) {
        return Arrays.stream(line.split(" ")).map(Integer::valueOf).toList();
    }

    public static List<List<Integer>> parseIntegerMatrix(String filename) {
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

    public static Map<String, Integer> parseStringToIntMap(String filename) {
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

    public static IloIntExpr getIntArraySum(IloCplex cplex, IloIntVar[] intVarArray) throws IloException {
        IloIntExpr sum = cplex.linearIntExpr();
        for (IloIntVar i : intVarArray) {
            sum = cplex.sum(sum, i);
        }
        return sum;
    }

    public static IloNumExpr getNumArraySum(IloCplex cplex, IloNumVar[] numVarArray) throws IloException {
        IloNumExpr sum = cplex.linearNumExpr();
        for (IloNumVar i : numVarArray) {
            sum = cplex.sum(sum, i);
        }
        return sum;
    }
}
