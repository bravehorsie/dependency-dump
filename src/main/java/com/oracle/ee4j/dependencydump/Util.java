
package com.oracle.ee4j.dependencydump;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.eclipse.aether.graph.DependencyNode;

public class Util {

    public static String toArtifactId(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
    }

    public static String toArtifactId(DependencyNode child) {
        return child.getArtifact().getGroupId() +":"+ child.getArtifact().getArtifactId() + ":"+  child.getArtifact().getExtension() +":"+ child.getArtifact().getVersion();
    }

    public static Model getModel(DefaultModelBuildingRequest modelBuildingRequest) {
        ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult modelBuildingResult;
        try {
            modelBuildingResult = modelBuilder.build(modelBuildingRequest);
        } catch (ModelBuildingException e) {
            throw new RuntimeException(e);
        }

        return modelBuildingResult.getEffectiveModel();
    }
}
