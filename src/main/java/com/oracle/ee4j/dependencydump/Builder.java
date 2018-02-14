package com.oracle.ee4j.dependencydump;

import org.apache.maven.model.resolution.ModelResolver;

import java.util.logging.Logger;

public class Builder {

    private final static Logger logger = Logger.getLogger("builder");

    private boolean printTree;
    private boolean includeLicense;
    private String[] scopes;
    private String[] excludes;
    private String localRepo;
    private String projectPath;
    private String proxyHost;
    private Integer proxyPort;

    public Runner buildRunner() {

        RepositorySupport repositorySupport;
        if (proxyHost != null && proxyPort != null) {
            logger.info("Using proxy: " + proxyHost + ":"+proxyPort);
            repositorySupport = new RepositorySupport(localRepo, proxyHost, proxyPort);
        } else {
            repositorySupport = new RepositorySupport(localRepo);
        }


        DependencyCollector collector = new DependencyCollector(repositorySupport);
        collector.setScopes(scopes);
        collector.setExcludes(excludes);

        Printer printer = new Printer(collector);
        printer.setIncludeLicense(includeLicense);
        printer.setPrintTree(printTree);

        return new Runner(projectPath, collector, printer);
    }

    public void setPrintTree(boolean printTree) {
        this.printTree = printTree;
    }

    public void setIncludeLicense(boolean includeLicense) {
        this.includeLicense = includeLicense;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    public void setLocalRepo(String localRepo) {
        this.localRepo = localRepo;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
}
