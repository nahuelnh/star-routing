package commons;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(60);
    public static final String RESOURCES_PATH ="src/resources/";
    private static final String DELIMITER = " ";

    private static List<Integer> readIntegerLine(String line) {
        return Arrays.stream(line.split(DELIMITER)).map(Integer::valueOf).toList();
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

    public static IloIntExpr getArraySum(IloCplex cplex, IloIntVar[] intVarArray) throws IloException {
        IloIntExpr sum = cplex.linearIntExpr();
        for (IloIntVar i : intVarArray) {
            sum = cplex.sum(sum, i);
        }
        return sum;
    }

    public static IloNumExpr getArraySum(IloCplex cplex, IloNumVar[] numVarArray) throws IloException {
        IloNumExpr sum = cplex.linearNumExpr();
        for (IloNumVar i : numVarArray) {
            sum = cplex.sum(sum, i);
        }
        return sum;
    }

    public static IloNumExpr getRowSum(IloCplex cplex, IloNumVar[][] numVarMatrix, int row) throws IloException {
        IloNumExpr sum = cplex.linearNumExpr();
        for (IloNumVar i : numVarMatrix[row]) {
            sum = cplex.sum(sum, i);
        }
        return sum;
    }

    public static IloIntExpr getRowSum(IloCplex cplex, IloIntVar[][] numVarMatrix, int row) throws IloException {
        IloIntExpr sum = cplex.linearIntExpr();
        for (IloIntVar i : numVarMatrix[row]) {
            sum = cplex.sum(sum, i);
        }
        return sum;
    }

    public static IloNumExpr getColumnSum(IloCplex cplex, IloNumVar[][] numVarMatrix, int column) throws IloException {
        IloNumExpr sum = cplex.linearNumExpr();
        for (IloNumVar[] row : numVarMatrix) {
            sum = cplex.sum(sum, row[column]);
        }
        return sum;
    }

    public static IloIntExpr getColumnSum(IloCplex cplex, IloIntVar[][] numVarMatrix, int column) throws IloException {
        IloIntExpr sum = cplex.linearIntExpr();
        for (IloIntVar[] row : numVarMatrix) {
            sum = cplex.sum(sum, row[column]);
        }
        return sum;
    }

    public static boolean isSolutionFeasible(IloCplex cplex) throws IloException {
        return IloCplex.Status.Optimal.equals(cplex.getStatus()) || IloCplex.Status.Feasible.equals(cplex.getStatus());
    }

    public static boolean getBoolValue(IloCplex cplex, IloNumVar iloNumVar) throws IloException {
        return Math.round(cplex.getValue(iloNumVar)) == 1;
    }

    public static boolean getBoolValue(IloCplex cplex, IloNumVar iloNumVar, int solutionIndex) throws IloException {
        return Math.round(cplex.getValue(iloNumVar, solutionIndex)) == 1;
    }

    public static void printNonZero(IloCplex cplex, IloNumVar[][] iloNumVar) throws IloException {
        for (IloNumVar[] row : iloNumVar) {
            printNonZero(cplex, row);
        }
    }

    public static void printNonZero(IloCplex cplex, IloNumVar[] iloNumVar) throws IloException {
        for (IloNumVar numVar : iloNumVar) {
            if (getBoolValue(cplex, numVar)) {
                System.out.println(numVar);
            }
        }
    }

    public static Set<Integer> bitSetToIntSet(BitSet arr) {
        Set<Integer> ret = new HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            if (arr.get(i)) {
                ret.add(i);
            }
        }
        return ret;
    }

    public static boolean isSubset(BitSet b1, BitSet b2) {
        BitSet b3 = b2.get(0, b2.length());
        b3.and(b1);
        b3.xor(b1);
        return b3.isEmpty();
    }

    public static Duration getElapsedTime(Instant start) {
        return Duration.between(start, Instant.now());
    }

    public static Duration getRemainingTime(Instant start, Duration timeLimit) {
        return timeLimit.minus(getElapsedTime(start));
    }

}
