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
import org.apache.maven.model.building.*;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Runner {


    private static final Logger logger = Logger.getLogger("runner");
    private final String pomName = "pom.xml";
    private final String rootProjectDir;
    private final RepositorySupport repositorySupport;

    private TreeMap<String, Dependency> directDependencies = new TreeMap<>();
    private final List<DependencyNode> transitiveDependencies = new ArrayList<>();
    private final Set<ProjectArtifact> projectArtifacts = new HashSet<>();
    private Model model;

    private String[] excludes;

    private String[] scopes;

    private boolean printTree;
    private final ModelResolver modelResolver;

    public Runner(String rootProjectDir, String localRepo) {
        this.rootProjectDir = rootProjectDir;
        this.repositorySupport = new RepositorySupport(localRepo);
        this.modelResolver = new ExternalModelResolver(repositorySupport);
    }

    public Runner(String rootProjectDir, String localRepo, String proxyHost, int proxyPort) {
        this.rootProjectDir = rootProjectDir;
        this.repositorySupport = new RepositorySupport(localRepo, proxyHost, proxyPort);
        this.modelResolver = new ExternalModelResolver(repositorySupport);
    }

    public void run() {
        parsePom(rootProjectDir);
        if (printTree) {
            printTree();
        } else {
            printFlat();
        }
    }

    private void printTree() {
        for (DependencyNode dependencyNode : transitiveDependencies) {
            printDependencyNode(dependencyNode, 0);
        }
    }

    private void printFlat() {
        logger.info(" === Direct dependencies: === \n");
        List<Dependency> directFiltered = directDependencies.values().stream().filter(
                (dependency -> !dependencyExcluded(dependency, getManagedDependency(model, dependency))
                        && !projectArtifacts.contains(new ProjectArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))))
                .collect(Collectors.toList());
        directFiltered.forEach((dependency) -> {
            logger.info(dependency.getGroupId() + ":" + dependency.getArtifactId() +":" +dependency.getType() + ":" + dependency.getVersion() + ":" +dependency.getScope());
            try {
                ModelSource modelSource = modelResolver.resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                Model model = loadModel(modelSource);
                logger.info("  POM Licenses: ");
                for (License license  : model.getLicenses()) {
                    logger.info("   " + license.getName());
                    logger.info("   " + license.getUrl());
                    logger.info("");
                }
            } catch (UnresolvableModelException | RuntimeException e) {
                //doesn't matter
            }
        });
        logger.info("\n Count: "+directFiltered.size());
        logger.info("\n ============================\n\n\n");

        Map<String, DependencyNode> merged = new TreeMap<>();
        for (DependencyNode node : transitiveDependencies) {
            flattenDependencyTree(merged, node);
        }
        logger.info(" === Transitive dependencies: === \n");

        List<DependencyNode> filteredTransitive = merged.values().stream().filter(node -> {
            Artifact artifact = node.getArtifact();
            boolean excluded = dependencyExcluded(node.getDependency());
            String artifactKey = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
            return !excluded && !directDependencies.containsKey(artifactKey);
        }).collect(Collectors.toList());

        filteredTransitive.forEach(node -> {
            logger.info(toArtifactId(node) + ":" + node.getDependency().getScope() + ":" +node.getDependency());
        });

        logger.info("\n Count: "+filteredTransitive.size());

        logger.info("\n ================================ \n");
    }

    private void flattenDependencyTree(Map<String, DependencyNode> merged, DependencyNode node) {
        if (node.getDependency().isOptional()) {
            return;
        }
        merged.put(toArtifactId(node), node);
        for (DependencyNode child : node.getChildren()) {

            String childId = toArtifactId(child);
            if (merged.containsKey(childId) || isExcluded(node, child)) {
                continue;
            }
            merged.put(childId, child);
            flattenDependencyTree(merged, child);
        }

    }

    private boolean isExcluded(DependencyNode parent, DependencyNode child) {
        Dependency direct = directDependencies.get(toArtifactId(parent));
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

    private String toArtifactId(DependencyNode child) {
        return child.getArtifact().getGroupId() +":"+ child.getArtifact().getArtifactId() + ":"+  child.getArtifact().getExtension() +":"+ child.getArtifact().getVersion();
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

    private void parsePom(String projectDir) {

        model = loadLocalPomModel(projectDir);

        projectArtifacts.add(new ProjectArtifact(model.getGroupId(), model.getArtifactId(), model.getVersion()));

        if (model.getPackaging().equals("pom")) {
            for (String module : model.getModules()) {
                String modulePom = projectDir + File.separator + module;
                parsePom(modulePom);
            }
        }

        List<Dependency> dependencies = model.getDependencies();
        for (org.apache.maven.model.Dependency dependency : dependencies) {
            String artifactId = toArtifactId(dependency);

            directDependencies.put(artifactId, dependency);

            DependencyResolver dependencyResolver = new DependencyResolver(repositorySupport);
            if (dependencyExcluded(dependency, getManagedDependency(model, dependency))) {
                continue;
            }
            try {
                DependencyResult dependencyResolved = dependencyResolver.resolve(dependency, scopes);
                transitiveDependencies.add(dependencyResolved.getRoot());
            } catch (DependencyResolutionException e) {
                logger.severe(e.getMessage());
                if (e.getResult().getRoot() != null) {
                    transitiveDependencies.add(e.getResult().getRoot());
                }
            }
        }


    }

    private String toArtifactId(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
    }

    private Dependency getManagedDependency(Model model, Dependency dependency) {
        for (Dependency managed : model.getDependencyManagement().getDependencies()) {
            if (managed.getGroupId().equals(dependency.getGroupId()) && managed.getArtifactId().equals(dependency.getArtifactId())) {
                return managed;
            }
        }
        return null;
    }

    private boolean dependencyExcluded(Dependency dependency, Dependency managed) {
        if (dependency.isOptional() || (managed != null && managed.isOptional())) {
            return true;
        }
        if (excludes != null) {
            for (String excl : excludes) {
                if (dependency.getGroupId().contains(excl)) {
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


    private Model loadLocalPomModel(String projectDir) {

        File pomFile = new File(projectDir + File.separator + pomName);
        if (!pomFile.exists()) {
            throw new IllegalStateException("Pom file not exists: " + pomFile.getAbsolutePath());
        }

        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setSystemProperties(System.getProperties()).setModelResolver(modelResolver).setPomFile(pomFile);

        return getModel(modelBuildingRequest);
    }


    private Model loadModel(ModelSource modelSource) {

        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setSystemProperties(System.getProperties()).setModelResolver(modelResolver).setModelSource(modelSource);

        return getModel(modelBuildingRequest);
    }

    private Model getModel(DefaultModelBuildingRequest modelBuildingRequest) {
        ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult modelBuildingResult;
        try {
            modelBuildingResult = modelBuilder.build(modelBuildingRequest);
        } catch (ModelBuildingException e) {
            throw new RuntimeException(e);
        }

        return modelBuildingResult.getEffectiveModel();
    }


    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public void setPrintTree(boolean printTree) {
        this.printTree = printTree;
    }

    private static final class ProjectArtifact {
        private final String groupId;
        private final String versionId;
        private final String version;

        public ProjectArtifact(String groupId, String versionId, String version) {
            this.groupId = groupId;
            this.versionId = versionId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getVersionId() {
            return versionId;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProjectArtifact that = (ProjectArtifact) o;
            return Objects.equals(groupId, that.groupId) &&
                    Objects.equals(versionId, that.versionId) &&
                    Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, versionId, version);
        }
    }
}
