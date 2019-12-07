# CRWLR
Crawls ads.txt from the given list of URLs, parses data and provides an endpoint to retrieve the collected dataset.
- [Run](#run)
- [Key Capabilities & Known Issues](#key-capabilities--known-issues)
- [Build](#build)
## Run
[Download Executable JAR](https://github.com/antonantsyferov/crwlr/releases/download/v0.0.1/ads-crawler-0.0.1.jar)
#### Run Instructions
* [JRE 8](https://openjdk.java.net/install/) is required.
* launch via command line: \
  `java -jar -DPORT=8080 -DPUBLISHERS=http://example.com/ads.txt,http://abc.com/ads.txt ads-crawler-0.0.1.jar`
    * `PORT` - to listen, optional parameter
    * `PUBLISHERS` - additional ads.txt URLs to crawl, comma-separated, optional parameter. The defaults are:
        * http://www.cnn.com/ads.txt
        * http://www.gizmodo.com/ads.txt
        * http://www.nytimes.com/ads.txt
        * https://www.bloomberg.com/ads.txt
        * https://wordpress.com/ads.txt

#### Usage
* `http://localhost:<port>/publishers` - get a list of supported publishers\
    Example: `GET http://localhost:8080/publishers`
    ```
    [
        {
            "name": "www.cnn.com",
            "url": "http://www.cnn.com/ads.txt"
        },
        {
            "name": "www.gizmodo.com",
            "url": "http://www.gizmodo.com/ads.txt"
        },
        {
            "name": "www.nytimes.com",
            "url": "http://www.nytimes.com/ads.txt"
        },
        {
            "name": "www.bloomberg.com",
            "url": "https://www.bloomberg.com/ads.txt"
        },
        {
            "name": "wordpress.com",
            "url": "https://wordpress.com/ads.txt"
        }
    ]
    ```
* `http://localhost:<port>/publishers/<name>` - get dataset by the publisher name\
    Example: `http://localhost:8080/publishers/www.bloomberg.com`
    ```
    [
        {
            "accountId": "8603",
            "domain": "advertising.com",
            "relationship": "RESELLER"
        },
        {
            "accountId": "8355",
            "domain": "appnexus.com",
            "relationship": "DIRECT"
        },
        {
            "accountId": "540158162",
            "authority": "6a698e2ec38604c6",
            "domain": "openx.com",
            "relationship": "DIRECT"
        }
    ]  
    ```
## Key Capabilities & Known Issues
#### Features
* Parses ads.txt according to [IAB Specification](https://iabtechlab.com/~iabtec5/wp-content/uploads/2016/07/IABOpenRTBAds.txtSpecification_Version1_Final.pdf) with some extra improvements:
    * case-insensitive Relationship values are supported ('direct'/'Direct')
    * ignores duplicate records.
* Supports HTTP redirects when fetching the content.
#### Unsupported
* Cyclic redirect protection
* ​RFC-1123 domain validation
* Realtime dataset updates (you have to restart the server to get the latest ads.txt changes)
* Physical storage (all data is kept in H2 RAM DB).
## Build
* [SBT](https://www.scala-sbt.org/download.html) is required.
* run command line: `sbt assemble`
* take `./target/scala-2.12/ads-crawler-0.0.1.jar` (fat jar)
