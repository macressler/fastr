package r.nodes.truffle;

import r.*;
import r.Convert;
import r.data.*;
import r.data.internal.*;
import r.errors.*;
import r.nodes.*;

import com.oracle.truffle.nodes.*;
import com.oracle.truffle.runtime.*;

// FIXME: probably could get some performance improvement by specializing for pairs of types,
// thus avoiding the cast nodes

public abstract class LogicalOperation extends BaseR {

    @Stable RNode left;
    @Stable RNode right;

    public LogicalOperation(ASTNode ast, RNode left, RNode right) {
        super(ast);
        this.left = updateParent(left);
        this.right = updateParent(right);
    }

    @Override
    public final Object execute(RContext context, Frame frame) {
        return RLogical.RLogicalFactory.getScalar(executeScalarLogical(context, frame));
    }

    @Override
    public abstract int executeScalarLogical(RContext context, Frame frame);

    public final int extractValue(RContext context, Frame frame, RNode node) {
        RNode curNode = node;
        for (;;) {
            try {
                return curNode.executeScalarLogical(context, frame);
            } catch (UnexpectedResultException e) {
                curNode = createCastNode(node.getAST(), node, (RAny) e.getResult(), curNode);
                replace(curNode, "install cast node");
                continue;
            }
        }
    }

    public static class Or extends LogicalOperation {
        public Or(ASTNode ast, RNode left, RNode right) {
            super(ast, left, right);
        }

        @Override
        public int executeScalarLogical(RContext context, Frame frame) {
            int leftValue = extractValue(context, frame, left);
            if (leftValue == RLogical.TRUE) {
                return RLogical.TRUE;
            }
            return extractValue(context, frame, right);
        }
    }

    public static class And extends LogicalOperation {
        public And(ASTNode ast, RNode left, RNode right) {
            super(ast, left, right);
        }

        @Override
        public int executeScalarLogical(RContext context, Frame frame) {
            int leftValue = extractValue(context, frame, left);
            if (leftValue == RLogical.TRUE) {
                return extractValue(context, frame, right);
            }
            if (leftValue == RLogical.FALSE) {
                return RLogical.FALSE;
            }
            // leftValue == RLogical.NA
            int rightValue = extractValue(context, frame, right);
            if (rightValue == RLogical.FALSE) {
                return RLogical.FALSE;
            }
            return RLogical.NA;
        }
    }

    // note: we can't use ConvertToLogicalOne, because of different error handling
    // FIXME: this could have more optimizations
    public abstract static class CastNode extends BaseR {
        @Stable RNode child;
        int iteration;

        public CastNode(ASTNode ast, RNode child, int iteration) {
            super(ast);
            this.child = updateParent(child);
            this.iteration = iteration;
        }

        @Override
        public final Object execute(RContext context, Frame frame) {
            Utils.nyi("unreachable");
            return null;
        }

        @Override
        public int executeScalarLogical(RContext context, Frame frame) throws UnexpectedResultException {
            RAny value = (RAny) child.execute(context, frame);
            return extract(value);
        }

        abstract int extract(RAny value) throws UnexpectedResultException;
    }

    public static CastNode createCastNode(ASTNode ast, RNode child, RAny template, RNode failedNode) {

        int iteration = -1;
        if (failedNode instanceof CastNode) {
            iteration = ((CastNode) failedNode).iteration;
        }
        if (iteration < 0) {
            if (template instanceof ScalarDoubleImpl) {
                return new CastNode(ast, child, iteration + 1) {
                    @Override
                    int extract(RAny value) throws UnexpectedResultException {
                        if (value instanceof ScalarDoubleImpl) {
                            return Convert.double2logical(((ScalarDoubleImpl) value).getDouble());
                        }
                        throw new UnexpectedResultException(value);
                    }
                };
            }
            if (template instanceof ScalarIntImpl) {
                return new CastNode(ast, child, iteration + 1) {
                    @Override
                    int extract(RAny value) throws UnexpectedResultException {
                        if (value instanceof ScalarIntImpl) {
                            return Convert.int2logical(((ScalarIntImpl) value).getInt());
                        }
                        throw new UnexpectedResultException(value);
                    }
                };
            }
        }
        if (iteration < 1) {
            if (template instanceof RLogical) {
                return new CastNode(ast, child, iteration + 1) {
                    @Override
                    int extract(RAny value) throws UnexpectedResultException {
                        if (value instanceof RLogical) {
                            RLogical v = (RLogical) value;
                            if (v.size() > 0) {
                                return v.getLogical(0);
                            } else {
                                return RLogical.NA;
                            }
                        }
                        throw new UnexpectedResultException(value);
                    }
                };
            }
            if (template instanceof RDouble) {
                return new CastNode(ast, child, iteration + 1) {
                    @Override
                    int extract(RAny value) throws UnexpectedResultException {
                        if (value instanceof RDouble) {
                            RDouble v = (RDouble) value;
                            if (v.size() > 0) {
                                return Convert.double2logical(v.getDouble(0));
                            } else {
                                return RLogical.NA;
                            }
                        }
                        throw new UnexpectedResultException(value);
                    }
                };
            }
            if (template instanceof RInt) {
                return new CastNode(ast, child, iteration + 1) {
                    @Override
                    int extract(RAny value) throws UnexpectedResultException {
                        if (value instanceof RInt) {
                            RInt v = (RInt) value;
                            if (v.size() > 0) {
                                return Convert.int2logical(v.getInt(0));
                            } else {
                                return RLogical.NA;
                            }
                        }
                        throw new UnexpectedResultException(value);
                    }
                };
            }
        }
        return new CastNode(ast, child, iteration + 1) {
            @Override
            int extract(RAny value) {
                if (value instanceof RLogical || value instanceof RInt || value instanceof RDouble) {
                    RLogical l = value.asLogical();
                    if (l.size() > 0) {
                        return l.getLogical(0);
                    } else {
                        return RLogical.NA;
                    }
                }
                Utils.nyi("unsupported logical operation argument");
                return -1;
            }
        };
    }
}