package testing;

import graph.*;
import pathfinding.framework.*;
import pathfinding.standard.*;

import java.io.*;
import java.util.*;


public class TestDijkstras {

    static void testTraditionalDijkstraVsOurs() {

        // Disable printing while running
        PrintStream originalStream = System.out;

        PrintStream noopStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                // NO-OP
            }
        });
        System.setOut(noopStream);

        Graph g = GraphPopulator.populateGraph("aarhus-silkeborg-intersections.csv");

        Vertex a = pickRandomVertex(g);
        Vertex b = pickRandomVertex(g);
        
        PathfindingAlgo traditional = new Dijkstra(); // TODO traditional
        PathfindingAlgo ours = new Dijkstra();

        Solution solutionTraditional = traditional.shortestPath(g, a, b);
        Solution solutionOurs = ours.shortestPath(g, a, b);
        System.setOut(originalStream);

        boolean equalSolutions = solutionTraditional.getShortestPath().equals(solutionOurs.getShortestPath());
        // assert !equalSolutions;
        // TODO wrap so only on when assertions are on
        if (!equalSolutions) {
            System.out.println("failed!");
            GraphVisualiser vis1 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
            vis1.drawPath(solutionOurs.getShortestPath());
            vis1.visualize();

            GraphVisualiser vis2 = new GraphVisualiser(g, BoundingBox.AarhusSilkeborg);
            vis2.drawPath(solutionTraditional.getShortestPath());
            vis2.visualize();

            try{
            Thread.sleep(400000);
            } catch(InterruptedException e) {
                System.out.println(e);
            }
        }
        List<Vertex> vertices = solutionTraditional.getShortestPath();
        Vertex first = vertices.get(0);
        Vertex last = vertices.get(vertices.size()-1);
        System.out.println("  " + first + "  ->  " + last);

    }

    static Vertex pickRandomVertex(Graph g) {
        Collection<Vertex> vertices = g.getAllVertices();
        return vertices.stream()
            .skip((int) (vertices.size() * Math.random()))
            .findFirst()
            .get();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.print(" -> " + i);
            try {

                testTraditionalDijkstraVsOurs();

            } catch(Exception e) {
                 System.out.println(" failed (exception)");
            } catch(AssertionError e) {
                System.out.println(" failed (assert)");
            }
        }
        System.out.println("[*] Done!");
    }
}
