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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Printer {

    private static Logger logger = Logger.getLogger("printer");

    private DependencyCollector collector;
    private boolean printTree;
    private boolean includeLicense;
    private String[] scopes;
    private String[] excludes;
    private ModelResolver modelResolver;

    public Printer(DependencyCollector collector) {
        this.collector = collector;
        this.modelResolver = collector.getModelResolver();
        this.scopes = collector.getScopes();
        this.excludes = collector.getExcludes();
    }

    public void print() {
        if (printTree) {
            printTree();
        } else {
            printFlat();
        }
    }

    private void printDependencyNode(DependencyNode node, int count) {
        StringBuilder str = new StringBuilder();
        if (count == 0) {
            str.append("+");
        } else {
            for(int i=0;i<count; i++) {
                str.append("  ");
            }
        }

        str.append("").append(node.getArtifact().getGroupId()).append(":").append(node.getArtifact().getArtifactId()).append(":").append(node.getArtifact().getBaseVersion());
        str.append(" (Scope: ").append(node.getDependency().getScope()).append(", Optional: ").append(node.getDependency().isOptional()).append(")");
        logger.info(str.toString());
        for (DependencyNode child : node.getChildren()) {
            printDependencyNode(child, count+1);
        }
    }

    private void printFlat() {
        logger.info(" === Direct dependencies: === \n");
        List<Dependency> directFiltered = collector.getDirectDependencies().values().stream().filter(
                (dependency -> !collector.getProjectArtifacts().contains(new ProjectArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))))
                .collect(Collectors.toList());
        directFiltered.forEach((dependency) -> {
            printArtifactSeparator();
            logger.info("Artifact: " + dependency.getGroupId() + ":" + dependency.getArtifactId() +":" +dependency.getType() + ":" + dependency.getVersion() + ":" +dependency.getScope());
            printLicense(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        });
        logger.info("\n Count: "+directFiltered.size());
        logger.info("\n ============================\n\n\n");

        Map<String, DependencyNode> merged = new TreeMap<>();
        for (DependencyNode node : collector.getTransitiveDependencies()) {
            flattenDependencyTree(merged, node);
        }
        logger.info(" === Transitive dependencies: === \n");

        List<DependencyNode> filteredTransitive = merged.values().stream().filter(node -> {
            Artifact artifact = node.getArtifact();
            boolean excluded = dependencyExcluded(node.getDependency());
            String artifactKey = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
            return !excluded && !collector.getDirectDependencies().containsKey(artifactKey);
        }).collect(Collectors.toList());

        filteredTransitive.forEach(node -> {
            printArtifactSeparator();
            logger.info("Artifact: " + Util.toArtifactId(node) + ":" + node.getDependency().getScope());
            Artifact dependencyArtifact = node.getDependency().getArtifact();
            printLicense(dependencyArtifact.getGroupId(), dependencyArtifact.getArtifactId(), dependencyArtifact.getVersion());
        });

        logger.info("\n Count: "+filteredTransitive.size());

        logger.info("\n ================================ \n");
    }

    private void printArtifactSeparator() {
        if (includeLicense) {
            logger.info("--------------------------------");
        }
    }

    private void printTree() {
        for (DependencyNode dependencyNode : collector.getTransitiveDependencies()) {
            printDependencyNode(dependencyNode, 0);
        }
    }

    private void printLicense(String groupId, String artifactId, String version) {
        if (!includeLicense) {
            return;
        }
        try {
            ModelSource modelSource = modelResolver.resolveModel(groupId, artifactId, version);
            Model model = loadModel(modelSource);
            for (License license  : model.getLicenses()) {
                logger.info("  - License: [" + license.getName() + "] URL: ["+license.getUrl()+"]");
            }
        } catch (UnresolvableModelException | RuntimeException e) {
            //doesn't matter
        }
    }

    private Model loadModel(ModelSource modelSource) {

        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setSystemProperties(System.getProperties()).setModelResolver(modelResolver).setModelSource(modelSource);

        return Util.getModel(modelBuildingRequest);
    }

    private boolean dependencyExcluded(org.eclipse.aether.graph.Dependency dependency) {
        if (dependency.isOptional()) {
            return true;
        }
        if (excludes != null) {
            for (String excl : excludes) {
                if (dependency.getArtifact().getGroupId().contains(excl)) {
                    return true;
                }
            }
        }

        if (scopes != null) {
            for (String scp : scopes) {
                if (scp.equals(dependency.getScope())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    private void flattenDependencyTree(Map<String, DependencyNode> merged, DependencyNode node) {
        if (node.getDependency().isOptional()) {
            return;
        }
        merged.put(Util.toArtifactId(node), node);
        for (DependencyNode child : node.getChildren()) {

            String childId = Util.toArtifactId(child);
            if (merged.containsKey(childId) || isExcluded(node, child)) {
                continue;
            }
            merged.put(childId, child);
            flattenDependencyTree(merged, child);
        }

    }

    private boolean isExcluded(DependencyNode parent, DependencyNode child) {
        Dependency direct = collector.getDirectDependencies().get(Util.toArtifactId(parent));
        if (direct == null) {
            return false;
        }
        for (org.apache.maven.model.Exclusion excl : direct.getExclusions()) {
            if (excl.getGroupId().equals(child.getArtifact().getGroupId()) && excl.getArtifactId().equals(child.getArtifact().getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    public void setPrintTree(boolean printTree) {
        this.printTree = printTree;
    }

    public void setIncludeLicense(boolean includeLicense) {
        this.includeLicense = includeLicense;
    }
}
