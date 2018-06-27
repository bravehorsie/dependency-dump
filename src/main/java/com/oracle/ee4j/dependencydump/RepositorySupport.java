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

import java.util.Arrays;
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
        this.proxy = new Proxy(Proxy.TYPE_HTTP, proxyHost, proxyPort);
    }


    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    public List<RemoteRepository> getRepositories() {
        RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/")
                .setProxy(proxy).build();

        RemoteRepository javaNet = new RemoteRepository.Builder("java.net", "default", "https://maven.java.net/content/groups/promoted/")
                .setProxy(proxy).build();
        return Arrays.asList(central, javaNet);
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
