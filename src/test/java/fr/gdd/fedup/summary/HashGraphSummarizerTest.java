package fr.gdd.fedup.summary;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashGraphSummarizerTest {

    @Test
    public void simple_test_of_hash_summary_with_one_quad() {
        HashGraphSummarizer hgs = new HashGraphSummarizer(1);
        Node graphURI = NodeFactory.createURI("https://example.com/Graph1");
        hgs.add(Quad.create(graphURI,
                NodeFactory.createURI("https://example.com/Alice"),
                NodeFactory.createURI("https://example.com/hasFriend"),
                NodeFactory.createURI("https://example.com/Julien")));
        hgs.summary.begin(ReadWrite.READ);
        assertEquals(1, hgs.summary.getNamedModel(graphURI.getURI()).size());
        hgs.summary.end();
    }

    @Test
    public void summarize_actually_summarize() {
        HashGraphSummarizer hgs = new HashGraphSummarizer(1);
        Node graphURI = NodeFactory.createURI("https://example.com/Graph1");
        hgs.add(Quad.create(graphURI,
                NodeFactory.createURI("https://example.com/Alice"),
                NodeFactory.createURI("https://example.com/hasFriend"),
                NodeFactory.createURI("https://example.com/Julien")));
        hgs.add(Quad.create(graphURI,
                NodeFactory.createURI("https://example.com/Julien"),
                NodeFactory.createURI("https://example.com/hasFriend"),
                NodeFactory.createURI("https://example.com/Alice")));
        hgs.summary.begin(ReadWrite.READ);

        Query q = QueryFactory.create("SELECT * WHERE {GRAPH ?g {?s ?p ?o}}");
        QueryExecution qe = QueryExecutionFactory.create(q, hgs.summary);
        ResultSet rs = qe.execSelect();

        assertTrue(rs.hasNext());
        QuerySolution qs = rs.next();
        assertEquals(graphURI.toString(), qs.getResource("g").getURI());
        assertEquals("https://example.com/0", qs.getResource("s").getURI());
        assertEquals("https://example.com/hasFriend", qs.getResource("p").getURI());
        assertEquals("https://example.com/0", qs.getResource("o").getURI());
        assertFalse(rs.hasNext());

        assertEquals(1, hgs.summary.getNamedModel(graphURI.getURI()).size());
        hgs.summary.end();
    }

}