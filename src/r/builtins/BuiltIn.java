package r.builtins;

import java.util.*;

import com.oracle.truffle.runtime.*;

import r.*;
import r.data.*;
import r.nodes.*;
import r.nodes.truffle.*;
import r.nodes.truffle.AbstractCall.*;

public abstract class BuiltIn extends AbstractCall {

    public BuiltIn(ASTNode orig, RSymbol[] argNames, RNode[] argExprs) {
        super(orig, argNames, argExprs);
    }

    abstract static class BuiltIn0 extends BuiltIn {

        public BuiltIn0(ASTNode orig, RSymbol[] argNames, RNode[] argExprs) {
            super(orig, argNames, argExprs);
        }

        @Override
        public final Object execute(RContext context, Frame frame) {
            return doBuiltIn(context, frame);
        }

        public abstract RAny doBuiltIn(RContext context, Frame frame);

        @Override
        public final RAny doBuiltIn(RContext context, Frame frame, RAny[] params) {
            // TODO or not runtime test, since it's not the entry point
            return doBuiltIn(context, frame);
        }
    }

    abstract static class BuiltIn1 extends BuiltIn {

        public BuiltIn1(ASTNode orig, RSymbol[] argNames, RNode[] argExprs) {
            super(orig, argNames, argExprs);
        }

        @Override
        public final Object execute(RContext context, Frame frame) {
            return doBuiltIn(context, frame, (RAny) argExprs[0].execute(context, frame));
        }

        public abstract RAny doBuiltIn(RContext context, Frame frame, RAny arg);

        @Override
        public final RAny doBuiltIn(RContext context, Frame frame, RAny[] params) {
            // TODO or not runtime test, since it's not the entry point
            return doBuiltIn(context, frame, params[0]);
        }
    }

    abstract static class BuiltIn2 extends BuiltIn {

        public BuiltIn2(ASTNode orig, RSymbol[] argNames, RNode[] argExprs) {
            super(orig, argNames, argExprs);
        }

        @Override
        public final Object execute(RContext context, Frame frame) {
            return doBuiltIn(context, frame, (RAny) argExprs[0].execute(context, frame), (RAny) argExprs[1].execute(context, frame));
        }

        public abstract RAny doBuiltIn(RContext context, Frame frame, RAny arg0, RAny arg1);

        @Override
        public final RAny doBuiltIn(RContext context, Frame frame, RAny[] params) {
            return doBuiltIn(context, frame, params[0], params[1]);
        }
    }

    abstract static class NamedArgsBuiltIn extends BuiltIn {
        final int[] argPositions; // maps arguments to positions in parameters array
        final int nParams; // number of _named_ parameters (not ... symbol)

        public NamedArgsBuiltIn(ASTNode orig, RSymbol[] argNames, RNode[] argExprs, int nParams, AnalyzedArguments aargs) {
            super(orig, argNames, argExprs);
            this.argPositions = aargs.argPositions;
            this.nParams = nParams;
        }

        @ExplodeLoop
        private RAny[] evalArgs(RContext context, Frame frame) {
            int len = argExprs.length;
            RAny[] params = new RAny[nParams];
            for (int i = 0; i < len; i++) {
                RAny val = (RAny) argExprs[i].execute(context, frame);
                int pos = argPositions[i];
                params[pos] = val;
            }
            return params;
        }

        public static class AnalyzedArguments {
            public boolean[] providedParams;
            public int[] argPositions;
            public ArrayList<Integer> unusedArgs;

            AnalyzedArguments(int nParams, int nArgs) {
                providedParams = new boolean[nParams];
                argPositions = new int[nArgs];
            }
        }

        public static AnalyzedArguments analyzeArguments(RSymbol[] argNames, RNode[] argExprs, String[] paramNames) {
            RSymbol[] sparamNames = new RSymbol[paramNames.length];
            for (int i = 0; i < paramNames.length; i++) {
                sparamNames[i] = RSymbol.getSymbol(paramNames[i]);
            }
            return analyzeArguments(argNames, argExprs, sparamNames);
        }
        public static AnalyzedArguments analyzeArguments(RSymbol[] argNames, RNode[] argExprs, RSymbol[] paramNames) {

            int nParams = paramNames.length;

            int nArgs = argExprs.length;
            AnalyzedArguments a = new AnalyzedArguments(nParams, nArgs);
            boolean[] usedArgs = new boolean[nArgs];

            for (int i = 0; i < nArgs; i++) { // matching by name
                if (argNames[i] != null) {
                    for (int j = 0; j < nParams; j++) {
                        if (argNames[i] == paramNames[j]) {
                            a.argPositions[i] = j;
                            a.providedParams[j] = true;
                            usedArgs[i] = true;
                        }
                    }
                }
            }

            int nextParam = 0;
            for (int i = 0; i < nArgs; i++) { // matching by position
                if (!usedArgs[i]) {
                    while (nextParam < nParams && a.providedParams[nextParam]) {
                        nextParam++;
                    }
                    if (nextParam == nParams) {
                        // FIXME: handle passing of ``...'' objects
                        if (a.unusedArgs == null) {
                            a.unusedArgs = new ArrayList<>();
                        }
                        a.unusedArgs.add(i);
                    } else {
                        if (argExprs[i] != null) {
                            /* usedArgs[i] = true; - not needed */
                            a.argPositions[i] = nextParam;
                            a.providedParams[nextParam] = true;
                        } else {
                            nextParam++;
                        }
                    }
                }
            }
            return a;
        }
    }

    @Override
    public Object execute(RContext context, Frame frame) {
        return doBuiltIn(context, frame, evalArgs(context, frame));
    }

    public abstract RAny doBuiltIn(RContext context, Frame frame, RAny[] params);

    @ExplodeLoop
    private RAny[] evalArgs(RContext context, Frame frame) {
        int len = argExprs.length;
        RAny[] args = new RAny[len];
        for (int i = 0; i < len; i++) {
            args[i] = (RAny) argExprs[i].execute(context, frame);
        }
        return args;
    }
}
