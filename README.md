Metric: Unit Conversion for Java
================================

Metric is a Java library that processes unit conversion queries written in natural language.  It currently understands only English, but it aims at supporting other languages in the future.  Among the major highlights, it supports currency conversion and unit prefixes, both [SI prefixes](http://en.wikipedia.org/wiki/SI_prefix) and [binary prefixes](http://en.wikipedia.org/wiki/Binary_prefix).

The library loads unit definitions from a *universe definition file*, where properties and their units are declared and defined. Refer to the sample file included in the project to get an idea of the syntax and features supported.

Downloading and building the project
------------------------------------

First of all, clone the repo or [download the zipball](https://github.com/gnapse/metric/zipball/master) and extract it.  Then, open a terminal and
go to the root folder of the project, and build it with Maven.

    $ mvn assembly:assembly

This generates a couple of jar packages inside the `target/` subdirectory within the project root, one with dependencies included, and the other one without dependencies, just the library.  You can use it as a library in your own project, or you can run the jar file itself to try a small command-line application included.

Trying out the command-line app
-------------------------------

Standing in the project root folder, after building the project as instructed above, you can run unit conversion queries by invoking the jar file as an application, and passing the query as command-line arguments.

    $ java -jar target/metric-0.01-jar-with-dependencies.jar 100 miles per hour in meters per second
    100 mi / h = 44.704 m / s

It'll print out the answer to standard output, as shown above.

What's next
-----------

Feel free to contribute.  This project is just starting and any suggestions of new features, improvements and bug fixes are welcome.





