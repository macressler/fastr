package r.nodes.exec;

import r.*;
import r.data.*;
import r.data.RLogical.RLogicalFactory;
import r.errors.*;
import r.runtime.*;

public abstract class ConvertToLogicalOne extends BaseR {

    @Child RNode input;

    private static final boolean DEBUG_C = false;

    private ConvertToLogicalOne(RNode input) {
        super(input.getAST());
        this.input = insert(input, "inserting cast node ConvertToLogicalOne");
    }

    @Override
    public final Object execute(Frame frame) {
        return RLogicalFactory.getScalar(executeScalarLogical(frame));
    }

    @Override
    protected <N extends RNode> N replaceChild(RNode oldNode, N newNode) {
        assert oldNode != null;
        if (input == oldNode) {
            input = newNode;
            return adoptInternal(newNode);
        }
        return super.replaceChild(oldNode, newNode);
    }

    @Override
    public final int executeScalarLogical(Frame frame) {
        assert Utils.check(getNewNode() == null);
        RAny value = (RAny) input.execute(frame);
        if (getNewNode() != null) {
            return ((ConvertToLogicalOne)getNewNode()).executeScalarLogical(value);
        }
        return executeScalarLogical(value);
    }

    // The execute methods are use by intermediate cast nodes - those assuming an array of logicals or ints
    public int executeScalarLogical(RAny condValue) {
        try {
            if (DEBUG_C) Utils.debug("executing 2nd level cast");
            return cast(condValue);
        } catch (SpecializationException e) {
            if (DEBUG_C) Utils.debug("2nd level cast failed, replacing by generic");
            ConvertToLogicalOne castNode = replace(fromGeneric(input), "installGenericConvertToLogical from cast node");
            return castNode.executeScalarLogical(condValue);
        }
    }

    public static ConvertToLogicalOne createAndInsertNode(RNode input, RAny value) {

        if (value instanceof RLogical) {
            return fromLogical(input);
        } else if (value instanceof RInt) {
            return fromInt(input);
        } else {
            return fromGeneric(input);
        }
    }

    public abstract int cast(RAny value) throws SpecializationException;

    public static ConvertToLogicalOne fromLogical(RNode input) {
        return new ConvertToLogicalOne(input) {

            @Override
            public int cast(RAny value) throws SpecializationException {
                if (DEBUG_C) Utils.debug("casting logical to one logical");
                if (!(value instanceof RLogical)) {
                    throw new SpecializationException(input);
                }
                RLogical logicalArray = ((RLogical) value);
                if (logicalArray.size() == 1) {
                    return logicalArray.getLogical(0);
                }
                if (logicalArray.size() > 1) {
                    RContext.warning(ast, RError.LENGTH_GT_1);
                    return logicalArray.getLogical(0);
                }
                throw RError.getLengthZero(null);
            }
        };
    }

    public static ConvertToLogicalOne fromInt(RNode input) {
        return new ConvertToLogicalOne(input) {

            @Override
            public int cast(RAny value) throws SpecializationException {
                if (DEBUG_C) Utils.debug("casting integer to one logical");
                if (!(value instanceof RInt)) {
                    throw new SpecializationException(input);
                }
                RInt intArray = ((RInt) value);
                int intValue;
                if (intArray.size() == 1) {
                    intValue = intArray.getInt(0);
                } else if (intArray.size() > 1) {
                    RContext.warning(ast, RError.LENGTH_GT_1);
                    intValue = intArray.getInt(0);
                } else {
                    throw RError.getLengthZero(null);
                }

                switch(intValue) {
                    case RLogical.FALSE: return intValue;
                    case RLogical.NA: throw RError.getArgumentNotInterpretableLogical(ast);
                    default: return RLogical.TRUE;
                }
            }
        };
    }

    public static ConvertToLogicalOne fromGeneric(RNode input) {
        return new ConvertToLogicalOne(input) {

            @Override
            public int cast(RAny value) {
                if (DEBUG_C) Utils.debug("casting generic to one logical");
                RLogical logicalArray = value.asLogical();
                int asize = logicalArray.size();
                int logicalValue;
                if (asize == 1) {
                    logicalValue = logicalArray.getLogical(0);
                } else if (asize > 1) {
                    logicalValue = logicalArray.getLogical(0);
                    RContext.warning(getAST(), RError.LENGTH_GT_1);
                } else {
                    assert Utils.check(asize == 0);
                    throw RError.getLengthZero(ast);
                }
                if (logicalValue == RLogical.NA && !(value instanceof RLogical)) {
                    throw RError.getArgumentNotInterpretableLogical(ast);
                }
                return logicalValue;
            }

            @Override
            public int executeScalarLogical(RAny condValue) {
                return cast(condValue);
            }
        };
    }
}
