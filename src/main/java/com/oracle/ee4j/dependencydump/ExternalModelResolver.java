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

import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.File;

public class ExternalModelResolver implements ModelResolver {

    private final RepositorySupport repositorySupport;

    public ExternalModelResolver(RepositorySupport repositorySupport) {
        this.repositorySupport = repositorySupport;
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            RepositorySystemSession session = repositorySupport.newSession();
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositorySupport.getRepositories(), null);
            pomArtifact = repositorySupport.getRepositorySystem().resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        File pomFile = pomArtifact.getFile();

        return new FileModelSource(pomFile);
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        Artifact artifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom",
                parent.getVersion());

        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {

    }

    @Override
    public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {

    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }



}
