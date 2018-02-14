package com.oracle.ee4j.dependencydump;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class DependencyResolver {

    private static final Logger loger = Logger.getLogger(DependencyResolver.class.getName());

    private final RepositorySupport repositorySupport;

    public DependencyResolver(RepositorySupport repositorySupport) {
        this.repositorySupport = repositorySupport;
    }

    public DependencyResult resolve(org.apache.maven.model.Dependency dependency, String[] scopes) throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());

        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, dependency.getScope()), repositorySupport.getRepositories());
        List<String> dependencyScopes = scopes != null ? Arrays.asList(scopes)
                : Arrays.asList(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.SYSTEM, JavaScopes.RUNTIME, JavaScopes.TEST);
                    DependencyFilter filter = DependencyFilterUtils.classpathFilter(dependencyScopes);
        DependencyRequest request = new DependencyRequest(collectRequest, filter);
        return repositorySupport.getRepositorySystem().resolveDependencies(repositorySupport.newSession(), request);
    }
}
