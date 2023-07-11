# CPQ-native Index [![](https://img.shields.io/github/release/RoanH/CPQ-native-index.svg)](https://github.com/RoanH/CPQ-native-index/releases)
This repository contains the implementation of a CPQ-native Index, which is a language-aware graph database index for Conjunctive Path Queries (CPQ). This is the reference implementation for my Master's Thesis titled [Subquery Evaluation Using a CPQ-native Index](TODO) (link will be added soon). The work presented here is follow-up research to the [CPQ-aware graph database index](https://ieeexplore.ieee.org/document/9835359) proposed by Yuya Sasaki, George Fletcher and Makoto Onizuka. Documentation (javadoc) can be found at: [cpqnativeindex.docs.roanh.dev](https://cpqnativeindex.docs.roanh.dev/) and the most important information is available in my thesis.

## Getting Started
To support a wide variety of of use cases the CPQ-native Index is a available in a number of different formats. 

- [As a standalone executable with both a command line interface](#executable-download)
- [As a docker image](#docker-image-)
- [As a maven artifact](#maven-artifact-)

However, it should be noted that some of these options provide less control over the execution environment than others. The recommended way to use the index software is to either incorporate it directly in some other piece of software, or to use the Java archive release if no customisation is required. Allocating as much RAM to the heap of the process as possible is recommended when computing an index for large graphs.

### Command-line Usage
When using the command line interface of the index, the following arguments are supported:

```
usage: index [-c] -d <file> [-f] [-h] [-i <max>] -k <k> [-l] -o <file> [-t <number>] [-v <file>]
 -c,--cores                 If passed then cores will be computed.
 -d,--data <file>           The graph file to create an index for or a saved index file.
 -f,--full                  If passed the saved index has all information required to compute cores later.
 -h,--help                  Prints this help text
 -i,--intersections <max>   The maximum number of branches for intersection cores (unlimited by default).
 -k,--diameter <k>          The value of k (diameter) to compute the index for.
 -l,--labels                If passed then labels will be computed.
 -o,--output <file>         The file to save the constructed index to.
 -t,--threads <number>      The number of threads to use for core computation (1 by default).
 -v,--verbose <file>        Turns on verbose logging of construction steps, optionally to a file or Discord.
```

For example, a base index without cores can be constructed using:

```sh
java -Xmx1900G -jar Index.jar -f -d graph.edge -k 2 -t 64 -v log.txt -o base_index.idx
```

The `-Xmx` argument is passed to Java and controls the maximum amount of RAM that will be used for the heap. Later a version of the base index with cores can be constructed using:

```sh
java -Xmx1900G -jar Index.jar -d base_index.idx -k 2 -c -t 64 -v discord:log.txt -o index.idx
```

Note that `discord:` can be prepended to the log file argument, which will send computation progress updates to the webhook configured in the `DISCORD\_WEBHOOK` variable in the `Main` class of the program. By default no webhook is configured, so configuring this requires compiling from source.

### Executable Download
The \CPQ-native Index is available as a standalone portable executable with a command line interface. This version of the index requires Java 17 or higher to run. Note that the Windows executable release does not offer the same degree of control over the heap size as the Java archive version.

- [Windows executable download](https://github.com/RoanH/CPQ-native-index/releases/download/v1.0/Index-v1.0.exe)    
- [Runnable Java archive (JAR) download](https://github.com/RoanH/CPQ-native-index/releases/download/v1.0/Index-v1.0.jar)

All releases: [releases](https://github.com/RoanH/CPQ-native-index/releases)    
GitHub repository: [RoanH/CPQ-native-index](https://github.com/RoanH/CPQ-native-index)

### Docker Image [![](https://img.shields.io/docker/v/roanh/cpq-native-index?sort=semver)](https://hub.docker.com/r/roanh/cpq-native-index)
The CPQ-native Index is available as a [docker image](https://hub.docker.com/r/roanh/cpq-native-index) on Docker Hub. This means that you can obtain the image using the following command:

```sh
docker pull roanh/cpq-native-index:latest
```

Using the image then works much the same as regular command line usage. For example, we can generate the example base index using the following command:

```sh
docker run --rm -v "$PWD/data:/data" roanh/cpq-native-index:latest java -Xmx1900G -jar Index.jar -f -d /data/graph.edge -k 2 -t 64 -v /data/log.txt -o /data/base_index.idx
```

Note that we mount a local folder called `data` into the container to pass our graph and to retrieve the generated index and logs. Also note that the entry point of the image has to be overridden to explicitly allocate more RAM to the heap.

### Maven Artifact [![Maven Central](https://img.shields.io/maven-central/v/dev.roanh.cpqnativeindex/cpq-native-index)](https://mvnrepository.com/artifact/dev.roanh.cpqnativeindex/cpq-native-index)
The CPQ-native Index is available on maven central as [an artifact](https://mvnrepository.com/artifact/dev.roanh.cpqnativeindex/cpq-native-index), this way it can be included directly in another Java project using build tools like Gradle and Maven. Using this method it also becomes possible to directly use all the implemented constructs and utilities. A hosted version of the javadoc for for the index can be found at [cpqnativeindex.docs.roanh.dev](https://cpqnativeindex.docs.roanh.dev/).

##### Gradle 
```groovy
repositories{
	mavenCentral()
}

dependencies{
	implementation 'dev.roanh.cpqnativeindex:cpq-native-index:1.0'
}
```

##### Maven
```xml
<dependency>
	<groupId>dev.roanh.cpqnativeindex</groupId>
	<artifactId>cpq-native-index</artifactId>
	<version>1.0</version>
</dependency>
```

## Development of gMark
This repository contain an [Eclipse](https://www.eclipse.org/) & [Gradle](https://gradle.org/) project with [gMark](https://github.com/RoanH/gMark) and [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/introduction.html) as the only dependencies. In addition, in order to compile the native library a C compiler and CMake are also required. Development work can be done using the Eclipse IDE or using any other Gradle compatible IDE. Unit testing is employed to test core functionality. Continuous integration is used to run checks on the source files, check for regressions using the unit tests, and to generate release publications.

Compiling the native library can be done using the following command in the `CPQ-native Index` directory:

```sh
./gradlew compileNatives
```

Running this command will place the compiled native library in the `lib` directory. Next, compiling the runnable Java archive (JAR) release of the index using Gradle can be done by running the following command in the same directory:

```sh
./gradlew shadowJar
```

After running this command the generated JAR can be found in the `build/libs` directory. On windows `./gradlew.bat` should be used for both commands instead of `./gradlew`. Also note that the native libraries should always be compiled before building a complete release JAR.

In software, an index can be constructed using the following constructor:

```java
Index index = new Index(
	IndexUtil.readGraph(graphFile),
	k,
	cores,
	labels,
	threads,
	intersections,
	listener
);
```

## History
Project development started: 27th of February, 2023.