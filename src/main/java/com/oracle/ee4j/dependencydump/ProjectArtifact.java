package com.oracle.ee4j.dependencydump;

import java.util.Objects;

final class ProjectArtifact {
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
