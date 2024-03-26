package fr.gdd.fedup.utils;

import fr.gdd.fedup.FedUP;
import fr.gdd.fedup.summary.Summary;
import fr.gdd.fedup.summary.SummaryFactory;
import org.apache.commons.cli.*;
import org.apache.jena.base.Sys;
import org.apache.jena.dboe.base.file.Location;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class QuerySourceSelectionExplain {
    public static void main(String[] args) throws ParseException, IOException {
        // Declare options
        Options options = new Options();

        options.addOption(new Option("h", "help", false, "print this message"));

        options.addOption(new Option("q", "query", true,
                "The input query file"));

        options.addOption(new Option("o", "output", true,
                "The input output file"));

        options.addOption(new Option("s", "summary", true,
                "Path(s) to summary"));

        options.addOption(new Option("f", "federation", true,
                "Path(s) to federation file"));

        for(String arg: args) {
            System.out.println(arg);
        }

        // Parse options
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help") ||
                cmd.getOptions().length==0 ||
                !cmd.hasOption("query") ||
                !cmd.hasOption("output") ||
                !cmd.hasOption("summary") ||
                !cmd.hasOption("federation")
        ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("fedup-explain -q <path> -o <path> -s <path> -f <path>", options);
            return;
        }

        // Read input file into a string
        Path queryInputFileName = Path.of(cmd.getOptionValue("query"));
        String query = Files.readString(queryInputFileName);

        // Set of endpoints from file
        Set<String> endpoints = new HashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(cmd.getOptionValue("federation")));
        String line;
        while ((line = br.readLine()) != null) {
            endpoints.add(line);
        }

        // If undefined, load identity
        Summary summary = SummaryFactory.createIdentity(Location.create(cmd.getOptionValue("summary")));

        FedUP fedup = new FedUP(summary, endpoints)
                .shouldNotFactorize()
                .modifyEndpoints(e-> String.format("http://localhost:5555/sparql?default-graph-uri=%s%s", (e.substring(0,e.length()-1)), "/"));

        String planAsString = fedup.query(query);

        // Write output
        PrintWriter writer = new PrintWriter(cmd.getOptionValue("output"));
        writer.println(planAsString);
        writer.close();


    }
}
