# au-bachelors


Download links to .csv files that can be used for testing:
https://www.dropbox.com/sh/0r4ukxachq1zpni/AACgBCKRt80oBD0oYGQZW5QOa?dl=0

We've included both the parses and unparsed version of Iceland, such that if it was of any interest you may run it through yourself to see the parser.

Denmark-latest-roads is also included, by we want to note that using the unpruned version of this takes some time to both load in and paint when using.

Finally we've included the shortcut graph obtained when calculating reaches, along with the reaches.


To run a specific algorithm, you want to open the file and find the main method. It is usually (we think always, but we may have missed one) located at the bottom.

You can then change the following to run:
   
      (1) Graph graph = GraphPopulator.populateGraph("denmark-latest-roads.csv"); 
      (2) graph = GraphUtils.pruneChains(graph);
      (3) Solution solution = d.shortestPath(GraphUtils.findNearestVertex(graph, Location.CPH), GraphUtils.findNearestVertex(graph, Location.Skagen));


(1) This decides what graph you want to run on

(2) This can be commented in or out, to enable/disable pruning of chains.

(3) Here you may change what nodes is ran between, but we recommend just using GraphUtils.pickRandomVertex(graph), as we have not define a lot of vertices. But if you want to redo the images shown in the report, you can use Location.CPH, Location.Esbjerg and a few more.

For reaches, the main methods is more cluttered. But you may still run them, but do not change the .csv files, as reaches have only been calculated on Iceland.
Here we mostly just recommend leaving it as it, it will run random queries on iceland. 

If you want to run the code from the terminal, here is a step by step guide how to.
1) Generate a text file with all .java paths for easier compiling of the entire project

   find -name "*.java" > paths.txt

2) Compile the entire project including the libraries used (It may say something is wrong, this can be ignored as we've had no troubles doing that at least)

   javac -d "bin" -cp lib/commons-compress-1.20.jar:lib/osmosis-pbf/* @paths.txt

3) Finally run the program/algorithm of choice by moving to the bin/ directory.
   
   Note: All files needed (OSM file, .csv for graphs and reaches) needs to be moved into the bin directory as well
   
   Note: We used -Xmx10g, to ensure enough memory during test, but for most singular queries on less is also enough.
   
   java -Xmx10g pathfinding.standard.BidirectionalALT 

