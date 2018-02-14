package com.oracle.ee4j.dependencydump;


import org.apache.commons.cli.*;

import java.util.logging.Logger;

public class Main {

    private static final String excludeGroupsParam = "excludeGroups";
    private static final String includeScopesParam = "includeScopes";
    private static final String includeLicense = "includeLicense";
    private static final String localRepoParam = "localRepo";
    private static final String projectParam = "project";
    private static final String printTree = "tree";

    private static final Logger logger = Logger.getLogger("main");

    public static void main(final String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");

        Options options = new Options();
        options.addOption(new Option(localRepoParam, true, "Filesystem path to local maven repository (required)"));
        options.addOption(new Option(projectParam, true, "Filesystem path to project (required)"));
        options.addOption(new Option(excludeGroupsParam, true, "Excludes dependencies with specified groupIds separated by comma. Transitive dependencies of excluded artifacts will not be printed, use with caution."));
        options.addOption(new Option(includeScopesParam, true, "Include only dependencies with specified scopes. Separated by comma. Transitive dependencies of excluded artifacts will not be printed, use with caution."));
        options.addOption(new Option(includeLicense, false, "Include licenses parsed from POM files of direct dependencies. Default false."));
        options.addOption(new Option(printTree, false, "Print dependencies as a tree merged from all subprojects instead of flat output. Default false."));

        CommandLine line;

        // create the parser
        CommandLineParser parser = new BasicParser();
        try {
            // parse the command line arguments
            line = parser.parse( options, args );
            if (!line.hasOption(localRepoParam) || !line.hasOption(projectParam)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "dependency-dump", options);
                System.out.println("\n\nExample:");
                System.out.println("\njava -jar target/dependency-dump-1.0-SNAPSHOT-jar-with-dependencies.jar -project ~/dev/java/metro/jaxb-v2/jaxb-ri/ -localRepo ~/.m2/repository/ -includeScopes compile,provided");
                return;
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            return;
        }


        Builder builder = new Builder();

        builder.setProjectPath(line.getOptionValue(projectParam));
        builder.setLocalRepo(line.getOptionValue(localRepoParam));

        builder.setProxyHost(getProxyHost());
        String proxyPort = getProxyPort();
        Integer port = proxyPort!=null && !proxyPort.isEmpty() ? Integer.parseInt(proxyPort) : null;
        builder.setProxyPort(port);

        String excludes;
        if ((excludes = line.getOptionValue(excludeGroupsParam)) != null) {
            logger.info("Excluding ["+excludes+"] groups and its dependencies.");
            builder.setExcludes(excludes.split(","));
        }

        String includes;
        if ((includes = line.getOptionValue(includeScopesParam)) != null) {
            logger.info("Including ["+includes+"] scopes.");
            builder.setScopes(includes.split(","));
        }

        if (line.hasOption(printTree)) {
            builder.setPrintTree(true);
        }

        if (line.hasOption(includeLicense)) {
            builder.setIncludeLicense(true);
        }

        Runner runner = builder.buildRunner();
        runner.run();
    }

    private static String getProxyHost() {
        String proxyHost = System.getProperty("http.proxyHost");
        if (proxyHost != null) {
            return proxyHost;
        }
        proxyHost = System.getenv("http_proxy");
        if (proxyHost != null) {
            return proxyHost.substring(0, proxyHost.lastIndexOf(":"));
        }
        proxyHost = System.getenv("HTTP_PROXY");
        if (proxyHost != null) {
            return proxyHost.substring(0, proxyHost.lastIndexOf(":"));
        }
        return null;
    }

    private static String getProxyPort() {
        String proxyPort = System.getProperty("http.proxyPort");
        if (proxyPort != null) {
            return proxyPort;
        }
        proxyPort = System.getenv("http_proxy");
        if (proxyPort != null) {
            return proxyPort.substring(proxyPort.lastIndexOf(":") + 1, proxyPort.length());
        }
        proxyPort = System.getenv("HTTP_PROXY");
        if (proxyPort != null) {
            return proxyPort.substring(proxyPort.lastIndexOf(":") + 1, proxyPort.length());
        }
        return null;
    }
}
