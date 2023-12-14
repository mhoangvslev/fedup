package fr.gdd.fedqpl;

import fr.gdd.fedqpl.operators.Mj;
import fr.gdd.fedqpl.operators.Mu;
import fr.gdd.fedqpl.visitors.ReturningOpBaseVisitor;
import fr.gdd.fedqpl.visitors.ReturningOpVisitor;
import fr.gdd.fedqpl.visitors.ReturningOpVisitorRouter;
import fr.gdd.fedup.transforms.ToQuadsTransform;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;

import java.util.Iterator;
import java.util.Set;

/**
 * Transform an `Op` into one that can be executed on the source
 * assignment results, i.e. on the RDF graph generated by the execution of
 * the summary query.
 */
public class Op2SAChecker extends ReturningOpBaseVisitor {

    public final ToQuadsTransform toQuads;

    public Op2SAChecker(ToQuadsTransform toQuads) {
        this.toQuads = toQuads;
    }

    @Override
    public Op visit(OpTriple triple) {
        Var g = toQuads.findVar(triple);
        return new OpQuad(Quad.create(g,
                NodeFactory.createURI(g.getVarName()),
                NodeFactory.createURI("row"),
                Var.alloc("row")
                ));
    }

    @Override
    public Op visit(OpBGP bgp) {
        Set<Var> gs = toQuads.findVars(bgp);
        OpSequence sequence = OpSequence.create();
        for (Var g : gs) {
            sequence.add(new OpQuad(Quad.create(g,
                    NodeFactory.createURI(g.getVarName()),
                    NodeFactory.createURI("row"),
                    Var.alloc("row")
            )));
        }

        return sequence;
    }

    @Override
    public Op visit(OpService req) {
        return new OpGraph(req.getService(), ReturningOpVisitorRouter.visit(this, req.getSubOp()));
    }

    @Override
    public Op visit(Mu mu) {
        return switch (mu.getElements().size()) {
            case 0 -> OpNull.create();
            case 1 -> ReturningOpVisitorRouter.visit(this, mu.getElements().iterator().next());
            default -> {
                // wrote as nested unions
                Iterator<Op> ops = mu.getElements().iterator();
                Op left = ReturningOpVisitorRouter.visit(this, ops.next());
                while (ops.hasNext()) {
                    Op right = ReturningOpVisitorRouter.visit(this, ops.next());
                    left = OpUnion.create(left, right);
                }
                yield left;
            }
        };
    }

    @Override
    public Op visit(Mj mj) {
        return switch (mj.getElements().size()) {
            case 0 -> OpNull.create();
            case 1 -> ReturningOpVisitorRouter.visit(this, mj.getElements().iterator().next());
            default -> {
                // as nested joins
                Iterator<Op> ops = mj.getElements().iterator();
                Op left = ReturningOpVisitorRouter.visit(this, ops.next());
                while (ops.hasNext()) {
                    Op right = ReturningOpVisitorRouter.visit(this, ops.next());
                    left = OpJoin.create(left, right);
                }
                yield left;
            }
        };
    }
}
