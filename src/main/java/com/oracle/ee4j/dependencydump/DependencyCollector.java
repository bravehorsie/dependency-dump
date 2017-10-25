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
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.model.resolution.ModelResolver;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class DependencyCollector {

    private static final Logger logger = Logger.getLogger("runner");

    private final String pomName = "pom.xml";
    private final RepositorySupport repositorySupport;

    private Map<String, Dependency> directDependencies = new TreeMap<>();
    private final List<DependencyNode> transitiveDependencies = new ArrayList<>();
    private final Set<ProjectArtifact> projectArtifacts = new HashSet<>();

    private String[] scopes;
    private String[] excludes;

    private final ModelResolver modelResolver;

    public DependencyCollector(RepositorySupport repositorySupport) {
        this.repositorySupport = repositorySupport;
        this.modelResolver = new ExternalModelResolver(repositorySupport);
    }

    public void parsePom(String projectDir) {

        Model model = loadLocalPomModel(projectDir);

        projectArtifacts.add(new ProjectArtifact(model.getGroupId(), model.getArtifactId(), model.getVersion()));

        if (model.getPackaging().equals("pom")) {
            for (String module : model.getModules()) {
                String modulePom = projectDir + File.separator + module;
                parsePom(modulePom);
            }
        }

        List<Dependency> dependencies = model.getDependencies();
        for (org.apache.maven.model.Dependency dependency : dependencies) {

            if (dependencyExcluded(dependency, getManagedDependency(model, dependency))) {
                continue;
            }

            String artifactId = Util.toArtifactId(dependency);

            directDependencies.put(artifactId, dependency);

            DependencyResolver dependencyResolver = new DependencyResolver(repositorySupport);
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

    private Model loadLocalPomModel(String projectDir) {

        File pomFile = new File(projectDir + File.separator + pomName);
        if (!pomFile.exists()) {
            throw new IllegalStateException("Pom file not exists: " + pomFile.getAbsolutePath());
        }

        final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setSystemProperties(System.getProperties()).setModelResolver(modelResolver).setPomFile(pomFile);

        return Util.getModel(modelBuildingRequest);
    }


    private Dependency getManagedDependency(Model model, Dependency dependency) {
        if (model.getDependencyManagement() == null) {
            return null;
        }
        for (Dependency managed : model.getDependencyManagement().getDependencies()) {
            if (managed.getGroupId().equals(dependency.getGroupId()) && managed.getArtifactId().equals(dependency.getArtifactId())) {
                return managed;
            }
        }
        return null;
    }

    private boolean dependencyExcluded(Dependency dependency, Dependency managed) {
        if (dependency.isOptional()) {
            return true;
        }
        if (managed != null && managed.isOptional()) {
            return true;
        }
        if (excludes != null) {
            for (String excl : excludes) {
                if (dependency.getGroupId().contains(excl)) {
                    return true;
                }
            }
        }

        String scope = dependency.getScope() != null ? dependency.getScope() :
                managed != null ? managed.getScope() : null;
        if (scopes != null) {
            for (String scp : scopes) {
                if (scp.equals(scope)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public Map<String, Dependency> getDirectDependencies() {
        return directDependencies;
    }

    public List<DependencyNode> getTransitiveDependencies() {
        return transitiveDependencies;
    }

    public Set<ProjectArtifact> getProjectArtifacts() {
        return projectArtifacts;
    }

    public String[] getScopes() {
        return scopes;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public ModelResolver getModelResolver() {
        return modelResolver;
    }
}
