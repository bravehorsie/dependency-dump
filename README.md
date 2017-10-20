### Dumps dependencies of maven project

Collect dependencies of a maven project and its modules / submodules. Prints all direct / transitive dependencies as flat list.

Be sure to install you project into local repository before running, otherwise versions not present in central (*-SNAPSHOT) for exampe will not be found. By installing to local running time is reduced dozen times.

Two arguments are required for running: "path to your project" and "path to your local maven repository"


```
Usage:
java -jar dependencydump.jar project ~/path/to/projectsources localRepo ~/path/to/local/maven/repo

Optional parameters:

    excludeGroups
        Excludes dependencies with specified groupIds separated by comma.
        Transitive dependencies of excluded artifacts will not be printed, use with caution.

    includeScopes
        Include only dependencies with specified scopes. Separated by comma.
        Transitive dependencies of excluded artifacts will not be printed, use with caution.

    printTree
        Print dependencies as a tree merged from all subprojects instead of flat output. Default false.
```        

### Example
`git clone yourproject`

`mvn clean install` in your porject

`git clone git@github.com:bravehorsie/dependency-dump.git`

`mvn clean install` in dependency-dump

`java -jar target/dependency-dump-1.0-SNAPSHOT-jar-with-dependencies.jar project=~/dev/java/jaxb-ri/ localRepo=~/.m2/repository/ includeScopes=compile,provided`

For use behind a proxy:

`java -Dhttp.proxyHost=your.proxy.com -Dhttp.proxyPort=80 -jar target/dependency-dump-1.0-SNAPSHOT-jar-with-dependencies.jar project=/home/roma/dev/java/jersey/ localRepo=~/.m2/repository/ includeScopes=compile,provided`