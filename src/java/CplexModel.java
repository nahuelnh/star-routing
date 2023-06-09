// --------------------------------------------------------------------------
// File: examples/src/LPex1.java
// --------------------------------------------------------------------------
//  Copyright (C) 2001 by ILOG.
//  All Rights Reserved.
//  Permission is expressly granted to use this example in the
//  course of developing applications that use ILOG products.
// --------------------------------------------------------------------------
//
// LPex1.java - Entering and optimizing an LP problem
// 
// Demonstrates different methods for creating a problem.  The user has to
// choose the method on the command line:
// 
//    java LpEX1 -r  generates the problem by adding constraints
//    java LpEX1 -c  generates the problem by adding variables
//    java LpEX1 -n  generates the problem by adding expressions
//    java LpEX1 -mr generates the problem by adding rows to an LP matrix
//    java LpEX1 -mc generates the problem by adding columns to an LP matrix
//    java LpEX1 -mn generates the problem by adding nonzeros to an LP matrix
// 

import ilog.concert.*;
import ilog.cplex.*;

public class CplexModel {

  public static void main(String[] args) {

    try {
      // Create the modeler/solver object
      IloCplex cplex = new IloCplex();

      IloNumVar[][] var = new IloNumVar[1][];
      IloRange[][] rng = new IloRange[1][];

      populateByRow(cplex, var, rng);

      // write model to file
      cplex.exportModel("lpex1.lp");

      // solve the model and display the solution if one was found
      if (cplex.solve()) {
        double[] x = cplex.getValues(var[0]);
        double[] dj = cplex.getReducedCosts(var[0]);
        double[] pi = cplex.getDuals(rng[0]);
        double[] slack = cplex.getSlacks(rng[0]);

        System.out.println("Solution status = " + cplex.getStatus());
        System.out.println("Solution value  = " + cplex.getObjValue());

        int ncols = cplex.getNcols();
        for (int j = 0; j < ncols; ++j) {
          System.out.println("Column: " + j +
              " Value = " + x[j] +
              " Reduced cost = " + dj[j]);
        }

        int nrows = cplex.getNrows();
        for (int i = 0; i < nrows; ++i) {
          System.out.println("Row   : " + i +
              " Slack = " + slack[i] +
              " Pi = " + pi[i]);
        }
      }
      cplex.end();
    } catch (IloException e) {
      System.err.println("Concert exception '" + e + "' caught");
    }
  }

  // The following methods all populate the problem with data for the following
  // linear program:
  //
  // Maximize
  // x1 + 2 x2 + 3 x3
  // Subject To
  // - x1 + x2 + x3 <= 20
  // x1 - 3 x2 + x3 <= 30
  // Bounds
  // 0 <= x1 <= 40
  // End
  //
  // using the IloMPModeler API

  static void populateByRow(IloMPModeler model,
      IloNumVar[][] var,
      IloRange[][] rng) throws IloException {
    double[] lb = { 0.0, 0.0, 0.0 };
    double[] ub = { 40.0, Double.MAX_VALUE, Double.MAX_VALUE };
    IloNumVar[] x = model.numVarArray(3, lb, ub);
    var[0] = x;

    double[] objvals = { 1.0, 2.0, 3.0 };
    model.addMaximize(model.scalProd(x, objvals));

    rng[0] = new IloRange[2];
    rng[0][0] = model.addLe(model.sum(model.prod(-1.0, x[0]),
        model.prod(1.0, x[1]),
        model.prod(1.0, x[2])), 20.0);
    rng[0][1] = model.addLe(model.sum(model.prod(1.0, x[0]),
        model.prod(-3.0, x[1]),
        model.prod(1.0, x[2])), 30.0);
  }

  static void populateByColumn(IloMPModeler model,
      IloNumVar[][] var,
      IloRange[][] rng) throws IloException {
    IloObjective obj = model.addMaximize();

    rng[0] = new IloRange[2];
    rng[0][0] = model.addRange(-Double.MAX_VALUE, 20.0);
    rng[0][1] = model.addRange(-Double.MAX_VALUE, 30.0);

    IloRange r0 = rng[0][0];
    IloRange r1 = rng[0][1];

    double[] objs = { 1.0, 2.0, 3.0 };
    double[] lb = { 0.0, 0.0, 0.0 };
    double[] ub = { 40.0, Double.MAX_VALUE, Double.MAX_VALUE };
    double[] val1 = { -1.0, 1.0, 1.0 };
    double[] val2 = { 1.0, -3.0, 1.0 };

    var[0] = model.numVarArray(model.columnArray(obj, objs).and(
        model.columnArray(r0, val1).and(
            model.columnArray(r1, val2))),
        lb, ub);
  }

  static void populateByNonzero(IloMPModeler model,
      IloNumVar[][] var,
      IloRange[][] rng) throws IloException {
    double[] lb = { 0.0, 0.0, 0.0 };
    double[] ub = { 40.0, Double.MAX_VALUE, Double.MAX_VALUE };
    IloNumVar[] x = model.numVarArray(3, lb, ub);
    var[0] = x;

    double[] objvals = { 1.0, 2.0, 3.0 };
    model.add(model.maximize(model.scalProd(x, objvals)));

    rng[0] = new IloRange[2];
    rng[0][0] = model.addRange(-Double.MAX_VALUE, 20.0);
    rng[0][1] = model.addRange(-Double.MAX_VALUE, 30.0);

    rng[0][0].setExpr(model.sum(model.prod(-1.0, x[0]),
        model.prod(1.0, x[1]),
        model.prod(1.0, x[2])));
    rng[0][1].setExpr(model.sum(model.prod(1.0, x[0]),
        model.prod(-3.0, x[1]),
        model.prod(1.0, x[2])));
  }

  static void populateLPByRow(IloMPModeler model,
      IloNumVar[][] var,
      IloRange[][] rng) throws IloException {
    IloLPMatrix lp = model.addLPMatrix();

    double[] lb = { 0.0, 0.0, 0.0 };
    double[] ub = { 40.0, Double.MAX_VALUE, Double.MAX_VALUE };
    IloNumVar[] x = model.numVarArray(model.columnArray(lp, 3), lb, ub);

    double[] lhs = { -Double.MAX_VALUE, -Double.MAX_VALUE };
    double[] rhs = { 20.0, 30.0 };
    double[][] val = { { -1.0, 1.0, 1.0 },
        { 1.0, -3.0, 1.0 } };
    int[][] ind = { { 0, 1, 2 },
        { 0, 1, 2 } };
    lp.addRows(lhs, rhs, ind, val);

    double[] objvals = { 1.0, 2.0, 3.0 };
    model.addMaximize(model.scalProd(x, objvals));

    var[0] = lp.getNumVars();
    rng[0] = lp.getRanges();
  }

  static void populateLPByColumn(IloMPModeler model,
      IloNumVar[][] var,
      IloRange[][] rng) throws IloException {
    IloObjective obj = model.addMaximize();
    IloLPMatrix lp = model.addLPMatrix();

    double[] lhs = { -Double.MAX_VALUE, -Double.MAX_VALUE };
    double[] rhs = { 20.0, 30.0 };

    lp.addRows(lhs, rhs, null, null);

    double[] objs = { 1.0, 2.0, 3.0 };
    double[] lb = { 0.0, 0.0, 0.0 };
    double[] ub = { 40.0, Double.MAX_VALUE, Double.MAX_VALUE };
    double[][] val = { { -1.0, 1.0 },
        { 1.0, -3.0 },
        { 1.0, 1.0 } };
    int[][] ind = { { 0, 1 },
        { 0, 1 },
        { 0, 1 } };

    model.numVarArray(model.columnArray(obj, objs).and(
        model.columnArray(lp, 3, ind, val)), lb, ub);

    var[0] = lp.getNumVars();
    rng[0] = lp.getRanges();
  }

  static void populateLPByNonzero(IloMPModeler model,
      IloNumVar[][] var,
      IloRange[][] rng) throws IloException {
    IloObjective obj = model.addMaximize();
    IloLPMatrix lp = model.addLPMatrix();

    double[] lhs = { -Double.MAX_VALUE, -Double.MAX_VALUE };
    double[] rhs = { 20.0, 30.0 };

    lp.addRows(lhs, rhs, null, null);

    double[] objs = { 1.0, 2.0, 3.0 };
    double[] lb = { 0.0, 0.0, 0.0 };
    double[] ub = { 40.0, Double.MAX_VALUE, Double.MAX_VALUE };

    model.numVarArray(model.columnArray(obj, objs).and(
        model.columnArray(lp, 3)), lb, ub);

    int[] row = { 0, 0, 0, 1, 1, 1 };
    int[] col = { 0, 1, 2, 0, 1, 2 };
    double[] val = { -1.0, 1.0, 1.0, 1.0, -3.0, 1.0 };

    lp.setNZs(row, col, val);

    var[0] = lp.getNumVars();
    rng[0] = lp.getRanges();
  }
}
