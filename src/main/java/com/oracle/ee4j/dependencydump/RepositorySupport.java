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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.util.Collections;
import java.util.List;

public class RepositorySupport {

    private RepositorySystem repositorySystem;

    private final LocalRepository localRepository;

    private Proxy proxy;

    public RepositorySupport(String localRepositoryPath) {
        this.localRepository = new LocalRepository(localRepositoryPath);

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        repositorySystem = locator.getService(RepositorySystem.class);
    }

    public RepositorySupport(String localRepositoryPath, String proxyHost, Integer proxyPort) {
        this(localRepositoryPath);
        this.proxy = new Proxy(Proxy.TYPE_HTTP, "www-proxy-hqdc.us.oracle.com", 80);
    }


    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    public List<RemoteRepository> getRepositories() {
        RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/")
                .setProxy(proxy).build();
        return Collections.singletonList(central);
    }

    public RepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
        return session;
    }

    private void resolve() throws ArtifactResolutionException, DependencyResolutionException {


        RepositorySystemSession session = newSession();

//        Artifact artifact = new DefaultArtifact("org.glassfish.tyrus:tyrus-core:1.13.1");
        Artifact artifact = new DefaultArtifact("org.glassfish.tyrus:jvnet-parent:5");

        ArtifactRequest artifactRequest = new ArtifactRequest(artifact, getRepositories(), null);
        ArtifactResult artifactResult1 = repositorySystem.resolveArtifact(session, artifactRequest);

        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), getRepositories());
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        DependencyRequest request = new DependencyRequest(collectRequest, filter);
        DependencyResult result = repositorySystem.resolveDependencies(session, request);


        for (ArtifactResult artifactResult : result.getArtifactResults()) {
            System.out.println(artifactResult.getArtifact().getArtifactId());
            System.out.println(artifactResult.getArtifact().getProperties());
        }
    }
}
