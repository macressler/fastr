package r.data;

import r.Truffle.*;
import r.nodes.ASTNode;
import r.nodes.truffle.*;

public interface RFunction {
    RFunction enclosingFunction();

    RSymbol[] paramNames();

    RNode[] paramValues();

    RNode body();

    RClosure createClosure(Frame frame);

    RSymbol[] localWriteSet();

    CallTarget callTarget();

    ASTNode getSource();

    int nlocals();

    int nparams();

    // FIXME: will also need methods to modify a function

    public static final class EnclosingSlot {

        public EnclosingSlot(RSymbol sym, int hops, int slot) {
            symbol = sym;
            this.hops = hops;
            this.slot = slot;
        }

        public final RSymbol symbol;
        public final int hops;
        public final int slot;
    }

    int positionInLocalWriteSet(RSymbol sym);

    int positionInLocalReadSet(RSymbol sym);

    EnclosingSlot getLocalReadSetEntry(RSymbol sym);

    int localSlot(RSymbol sym);

    EnclosingSlot enclosingSlot(RSymbol sym);

    boolean isInWriteSet(RSymbol sym);
}
