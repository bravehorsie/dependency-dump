package com.oracle.ee4j.dependencydump;

public class Runner {

    private final String rootProjectDir;
    private DependencyCollector collector;
    private Printer printer;

    public Runner(String rootProjectDir, DependencyCollector collector, Printer printer) {
        this.rootProjectDir = rootProjectDir;
        this.collector = collector;
        this.printer = printer;
    }


    public void run() {
        collector.parsePom(rootProjectDir);
        printer.print();
    }



}
