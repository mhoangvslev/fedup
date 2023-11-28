package fr.gdd.fedup;

import fr.gdd.fedup.summary.ModuloOnSuffix;
import fr.gdd.fedup.summary.Summary;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.binding.Binding;
import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

/**
 * Simple test by running on FedShop.
 *
 *         Have a virtuoso endpoint running with fedshop200…
 *         Virtuoso Open Source Edition (Column Store) (multi threaded)
 *         Version 7.2.6.3233-pthreads as of Jun 22 2021 (111d17e5b)
 *         Compiled for Mac OS 11 (Apple Silicon) (aarch64-apple-darwin20.5.0)
 *
 *         Listening on localhost:5555
 */
public class FedShopTest {

    private static final Logger log = LoggerFactory.getLogger(FedShopTest.class);
    private static final Integer PRINTRESULTTHRESHOLD = 10;

    public static FedUP fedup = new FedUP(new Summary(new ModuloOnSuffix(1),
            Location.create("/Users/nedelec-b-2/Desktop/Projects/fedup/temp/fedup-h0" )))
            .modifyEndpoints(e-> "http://localhost:5555/sparql?default-graph-uri="+(e.substring(0,e.length()-1)));

    Repository fedx = FedXFactory.newFederation()
            .withConfig(new FedXConfig())
            .withSparqlEndpoints(List.of()).create();

    // (TODO) conditional run
    @Disabled
    @Test
    public void small_try_to_benchmark_fedshop_on_multiple_executors() {
        doItAll("""
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX rev: <http://purl.org/stuff/rev#>
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>
                PREFIX dc: <http://purl.org/dc/elements/1.1/>
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                                
                SELECT DISTINCT ?productLabel ?offer ?price ?vendor ?vendorTitle ?review ?revTitle ?reviewer ?revName ?rating1 ?rating2 WHERE {
                  ?localProduct owl:sameAs bsbm:Product72080 .
                  ?localProduct rdf:type bsbm:Product .
                  ?localProduct rdfs:label ?productLabel .
                  OPTIONAL {
                    ?offerProduct  owl:sameAs bsbm:Product72080 .
                    ?offer bsbm:product ?offerProduct .
                    ?offer bsbm:price ?price .
                    ?offer bsbm:vendor ?vendor .
                    ?vendor rdfs:label ?vendorTitle .
                    ?vendor bsbm:country <http://downlode.org/rdf/iso-3166/countries#FR> .
                    ?offer bsbm:validTo ?date .
                    FILTER (?date > "2008-04-25T00:00:00"^^xsd:dateTime )
                  }
                  OPTIONAL {
                    ?reviewProduct owl:sameAs bsbm:Product72080 .
                    ?review bsbm:reviewFor ?reviewProduct .
                    ?review rev:reviewer ?reviewer .
                    ?reviewer foaf:name ?revName .
                    ?review dc:title ?revTitle .
                    OPTIONAL { ?review bsbm:rating1 ?rating1 . }
                    OPTIONAL { ?review bsbm:rating2 ?rating2 . }
                  }
                }
             """);
    }

    @Disabled
    @Test
    public void run_on_izmir_fedshop200() throws IOException {
        List<ImmutablePair<String, String>> queries = Files.walk(Paths.get("./queries/izmir_fedshop/"))
                .filter(p-> p.toString().endsWith(".sparql"))
                .map(p -> {
                    try {
                        return new ImmutablePair<>(p.getFileName().toString(), new String(Files.readAllBytes(p)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        // #1 get all queries from folder
        queries.forEach(q -> {
            log.info("Started {}…", q.getLeft());
            doItAllAndPrint(q.getRight(), q.getLeft());
        });
    }

    @Disabled
    @Test
    public void run_on_q05j() {
        doItAll("""
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                                
                SELECT DISTINCT ?product ?localProductLabel WHERE {
                    ?localProduct rdfs:label ?localProductLabel .
                                
                    ?localProduct bsbm:productFeature ?localProdFeature .
                    ?localProduct bsbm:productPropertyNumeric1 ?simProperty1 .
                    ?localProduct bsbm:productPropertyNumeric2 ?simProperty2 .
                                
                    ?localProduct owl:sameAs ?product .
                    ?localProdFeature owl:sameAs ?prodFeature .
                                
                    ?localProductXYZ bsbm:productFeature ?localProdFeatureXYZ .
                    ?localProductXYZ bsbm:productPropertyNumeric1 ?origProperty1 .
                    ?localProductXYZ bsbm:productPropertyNumeric2 ?origProperty2 .
                                
                    # const bsbm:Product171547
                    ?localProductXYZ owl:sameAs bsbm:Product171547 .
                    ?localProdFeatureXYZ owl:sameAs ?prodFeature .
                                
                    FILTER(bsbm:Product171547 != ?product)
                    # Values are pre-determined because we knew the boundaries from the normal distribution
                    FILTER(?simProperty1 < (?origProperty1 + 20) && ?simProperty1 > (?origProperty1 - 20))
                    FILTER(?simProperty2 < (?origProperty2 + 70) && ?simProperty2 > (?origProperty2 - 70))
                }
                ORDER BY ?product ?localProductLabel
                LIMIT 5
                """);
    }



    /* **************************************************************** */

    public void doItAll(String query) {
        Pair<String, Long> serviceQuery = measuredSourceSelection(query);
        measuredExecuteWithFedX(serviceQuery.getLeft());  // start with fedx since expected to be quicker
        measuredExecuteWithJena(serviceQuery.getLeft());
    }

    public void doItAllAndPrint(String query, String shortname) {
        Pair<String, Long> serviceQuery = measuredSourceSelection(query);
        long fedxTime = measuredExecuteWithFedX(serviceQuery.getLeft()); // start with fedx since expected to be quicker
        long jenaTime = measuredExecuteWithJena(serviceQuery.getLeft());
        System.out.println(String.format("%s %s %s %s", shortname, serviceQuery.getRight(), jenaTime, fedxTime));
    }

    public Pair<String, Long> measuredSourceSelection(String originalQuery) {
        long current = System.currentTimeMillis();
        String result = fedup.query(originalQuery);
        long elapsed = System.currentTimeMillis() - current;
        log.info("FedUP took {} ms to perform source selection.", elapsed);
        return new ImmutablePair<>(result, elapsed);
    }

    public long measuredExecuteWithJena(String serviceQuery) {
        long elapsed = -1;
        MultiSet<Binding> serviceResults = new HashMultiSet<>();
        try (QueryExecution qe =  QueryExecutionFactory.create(serviceQuery, DatasetFactory.empty())) {
            long current = System.currentTimeMillis();
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                serviceResults.add(results.nextBinding());
            }
            elapsed = System.currentTimeMillis() - current;
            log.info("Jena took {} ms to get {} results.", elapsed, serviceResults.size());
        }

        if (serviceResults.size() <= PRINTRESULTTHRESHOLD) {
            log.debug("Results:\n{}", String.join("\n", serviceResults.entrySet().stream().map(Object::toString).toList()));
        }
        return elapsed;
    }

    public long measuredExecuteWithFedX(String serviceQuery) {
        MultiSet<BindingSet> serviceResults = new HashMultiSet<>();

        long elapsed = -1;

        try (RepositoryConnection conn = fedx.getConnection()) {
            long current = System.currentTimeMillis();
            TupleQuery tq = conn.prepareTupleQuery(serviceQuery);
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    serviceResults.add(tqRes.next());
                }
            }
            elapsed = System.currentTimeMillis() - current;
            log.info("FedX took {} ms to get {} results.", elapsed, serviceResults.size());
        }

        if (serviceResults.size() <= PRINTRESULTTHRESHOLD) {
            log.debug("Results:\n{}", String.join("\n", serviceResults.entrySet().stream().map(Object::toString).toList()));
        }
        return elapsed;
    }


}
