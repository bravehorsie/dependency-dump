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


public class Main {

    private static final String excludeGroupsParam = "excludeGroups";
    private static final String includeScopesParam = "includeScopes";
    private static final String localRepoParam = "localRepo";
    private static final String projectParam = "project";
    private static final String printTree = "tree";

    public static void main(final String[] args) throws Exception {

        String projectPath = getArgument(args, projectParam);
        String localRepo = getArgument(args, localRepoParam);

        if (projectPath == null || localRepo == null) {
            System.out.println("\nUsage:");
            System.out.println("java -jar dependencydump.jar "+projectParam+"=~/path/to/projectsources "+localRepoParam+"=~/path/to/local/maven/repo");
            System.out.println();
            System.out.println("Optional parameters:");
            System.out.println("    excludeGroups");
            System.out.println("        Excludes dependencies with specified groupIds separated by comma.");
            System.out.println("        Transitive dependencies of excluded artifacts will not be printed, use with caution.\n");
            System.out.println("    includeScopes");
            System.out.println("        Include only dependencies with specified scopes. Separated by comma.");
            System.out.println("        Transitive dependencies of excluded artifacts will not be printed, use with caution.\n");
            System.out.println("    printTree");
            System.out.println("        Print dependencies as a tree merged from all subprojects instead of flat output. Default false.");
            System.out.println("\n\nExample:");
            System.out.println("\njava -jar target/dependency-dump-1.0-SNAPSHOT-jar-with-dependencies.jar project=~/dev/java/metro/jaxb-v2/jaxb-ri/ localRepo=~/.m2/repository/ includeScopes=compile,provided");
            return;
        }


        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        Integer port = proxyPort!=null && !proxyPort.isEmpty() ? Integer.parseInt(proxyPort) : null;

        Runner runner;
        if (proxyHost != null) {
            runner = new Runner(projectPath, localRepo, proxyHost, port);
        } else {
            runner = new Runner(projectPath, localRepo);
        }

        String excludes;
        if ((excludes = getArgument(args, excludeGroupsParam)) != null) {
            runner.setExcludes(excludes.split(","));
        }

        String includes;
        if ((includes = getArgument(args, includeScopesParam)) != null) {
            runner.setScopes(includes.split(","));
        }

        if (getArgument(args, printTree) != null) {
            runner.setPrintTree(true);
        }

        runner.run();
    }

    private static String getArgument(String[] args, String lookup) {
        for (String arg : args) {
            if (arg.startsWith(lookup)) {
                return arg.substring(arg.indexOf("=")+1, arg.length());
            }
        }
        return null;
    }
}
