# au-bachelors


Download links to .csv files that can be used for testing:


If you want to run the code from the terminal, here is a step by step guide how to.
1) Generate a text file with all .java paths for easier compiling of the entire project

   find -name "*.java" > paths.txt

2) Compile the entire project including the libraries used (It may say something is wrong, this can be ignored as we've had no troubles doing that at least)

   javac -d "bin" -cp lib/commons-compress-1.20.jar:lib/osmosis-pbf/* @paths.txt

3) Finally run the program/algorithm of choice by moving to the bin/ directory.
   
   Note: All files needed (OSM file, .csv for graphs and reaches) needs to be moved into the bin directory as well
   
   Note: We used -Xmx10g, to ensure enough memory during test, but for most singular queries on less is also enough.
   
   java -Xmx10g pathfinding.standard.BidirectionalALT 

