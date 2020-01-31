
<p align="center">
    <img src ="https://img.shields.io/badge/version-1.0-blueviolet.svg"/>
    <img src ="https://img.shields.io/badge/platform-windows|linux|macos-yellow.svg"/>
    <img src ="https://img.shields.io/badge/java-1.8-blue.svg" />
</p>

GalaxyHistoryBrowser is tool that can be used to browse histories that have been exported from Galaxy.
It can load history archives (in tar.gz format) from local files or URLs, display the structure of the history
with datasets and collections, show metadata for selected datasets and display previews of dataset files.

![Screenshot](http://folk.ntnu.no/kjetikl/galaxy/galaxy_history_browser.png)



### Prerequisites

GalaxyHistoryBrowser is written in Java. To build the project from source you will need:

* [Java JDK 8](https://www.java.com) - programming language
* [Maven](https://maven.apache.org/) - build and dependency manager


### Download Git repository

```
git clone https://github.com/kjetilkl/galaxy_history_browser.git
```


### Building from source

To compile the Java files and package the project, go into the directory containing the "pom.xml" file and run the Maven command:

```
mvn package
```

Maven will package GalaxyHistoryBrowser in a JAR-file and place it in the "target" subdirectory. Other dependencies will be placed in "target/lib". 


## Running GalaxyHistoryBrowser

To start GalaxyHistoryBrowser, go into the "target" directory and run:

```
java -jar GalaxyHistoryBrowser-1.0.jar
```

This will start up GalaxyHistoryBrowser with a graphical user interface. You can also run the program as a pure command-line tool by supplying the path to a local file or URL
that points to a Galaxy History Archive file (tar.gz format).

```
java -jar GalaxyHistoryBrowser-1.0.jar <filepath|URL>
```


## Authors

* **Kjetil Klepper** (kjetil.klepper@ntnu.no)
