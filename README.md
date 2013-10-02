file-tailer - A simple tool for tailing files.
===============

Getting Started
---------------
Clone this repo:

    git clone https://github.com/onlychoice/file-tailer.git

run

    mvn package # or mvn deploy for deploying the package to the maven repository

add the package to the classpath, or use maven dependency:

	<dependency>
		<groupId>com.netease.util</groupId>
		<artifactId>file-tailer</artifactId>
		<version>1.0.0</version>
	</dependency>

use following code to start the tailer on a file:

```java
Tailer tailer = TailerHelper.createTailer(targetFile, tailListener, 0);
Thread thread = new Thread(tailer);
thread.start();
```