/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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


        String projectPath = line.getOptionValue(projectParam);
        String localRepo = line.getOptionValue(localRepoParam);


        String proxyHost = getProxyHost();
        String proxyPort = getProxyPort();
        Integer port = proxyPort!=null && !proxyPort.isEmpty() ? Integer.parseInt(proxyPort) : null;

        Runner runner;
        if (proxyHost != null && port != null) {
            logger.info("Using proxy: " + proxyHost + ":"+proxyPort);
            runner = new Runner(projectPath, localRepo, proxyHost, port);
        } else {
            runner = new Runner(projectPath, localRepo);
        }

        String excludes;
        if ((excludes = line.getOptionValue(excludeGroupsParam)) != null) {
            logger.info("Excluding ["+excludes+"] groups and its dependencies.");
            runner.setExcludes(excludes.split(","));
        }

        String includes;
        if ((includes = line.getOptionValue(includeScopesParam)) != null) {
            logger.info("Including ["+includes+"] scopes.");
            runner.setScopes(includes.split(","));
        }

        if (line.hasOption(printTree)) {
            runner.setPrintTree(true);
        }

        if (line.hasOption(includeLicense)) {
            runner.setIncludeLicense(true);
        }

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
