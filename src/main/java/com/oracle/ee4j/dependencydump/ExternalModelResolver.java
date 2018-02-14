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
