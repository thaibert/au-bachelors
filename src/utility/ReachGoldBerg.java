package utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.checkerframework.checker.units.qual.Length;
import org.checkerframework.checker.units.qual.h;

import graph.*;
import pathfinding.framework.Edge;

public class ReachGoldBerg {
    private static Double INF_DIST = Double.MAX_VALUE;


    public static Map<Vertex, Double> reach(Graph graph, double bs) {
        Set<Edge> edgesConsideredThroughout = new HashSet<>();

        Map<Vertex, Double> inPenalties = new HashMap<>();
        Map<Vertex, Double> outPenalties = new HashMap<>();

        Graph graphPrime = graph;

        Map<Edge, Double> r = new HashMap<>();
        Map<Vertex, Double> rVertex = new HashMap<>();

        double verticesInOriginalGraph = graph.getAllVertices().size();
        double epsilon = bs;
        int i = 0;

        //TODO do a shortcut step before we start?
        //TODO what should epsilon be fore start?
        Graph gPrimeShortcut = shortcut(graphPrime, graph, epsilon*0.5, inPenalties, outPenalties, r); 
        graphPrime = gPrimeShortcut;


        while (true) {
        //for (int i = 0; i < bs.length; i++){

            Set<Edge> edgesConsidered = new HashSet<>();

            int edgeNumber = 0;
            for (Vertex v: graphPrime.getAllVertices()){
                edgeNumber += graphPrime.getNeighboursOf(v).size();
            }
            System.out.println("Number of edge in graph " + edgeNumber + " at iteration " + i);

            GraphVisualiser vis2 = new GraphVisualiser(graphPrime, BoundingBox.Iceland);
            vis2.visualize("Iteration " + i);

            //Iterative step
            System.out.println("Iteration " + i + " with bs[i] = " + epsilon);
            System.out.println("Works with gPrime size: " + graphPrime.getAllVertices().size());


            for (Vertex v: graphPrime.getAllVertices()){
                for (Neighbor n: graphPrime.getNeighboursOf(v)){
                    Edge edge = new Edge(v, n.v, n.distance);
                    r.put(edge, 0.0);
                }
            }

            // Grow trees 
            int max = graphPrime.getAllVertices().size();
            int tenProcent = max/20;
            int counter2 = 0;
            int counter = 0;
            long timeBefore = System.currentTimeMillis();
            long totalReachCalcTime = 0;

            for (Vertex v: graphPrime.getAllVertices()){
                counter++;
                if (counter % tenProcent == 0) {
                    counter2++;
                    System.out.println("Completed the first " + counter2*5 + "%");
                    long timeAfter = System.currentTimeMillis();
                    System.out.println("Total reach calc time so far  " + totalReachCalcTime/1000 + " seconds");
                    System.out.println("Total calculating time so far " + ((timeAfter-timeBefore)/1000) + " seconds");

                }
                Tree tree = partialTree(graphPrime, v, epsilon, outPenalties, inPenalties);
                // We do not include v according to Goldberg

                // Modify tree according to the out-penalties
                int x = 0;
                int y = 0;
                Set<Vertex> newClosed = new HashSet<>(tree.closed); // Can't modify closed while we loop over it 
                Set<Vertex> iter = new HashSet<>(tree.dist.keySet());

                // This part can be made way more effective
                for (Vertex w: iter){ // TODO this is ugly
                    tree.leafs.remove(w);
                    Vertex wPrime = new Vertex(x,y); // This is the pseudo leafs
                    x++; 
                    y++;
                    tree.dist.put(wPrime, tree.dist.get(w) + outPenalties.getOrDefault(w, 0.0));
                    tree.leafs.add(wPrime);
                    Set<Vertex> path = new HashSet<>(tree.paths.get(w));
                    path.add(w);
                    tree.paths.put(wPrime, path);
                    if (tree.closed.contains(w)){
                        newClosed.add(wPrime);
                    }
                }
                tree.closed = newClosed;

                long timeBeforeReachCalc = System.currentTimeMillis();
                //System.out.println("Calculating reaches psudoleafs");

                for (Vertex u: tree.inner){
                    for (Neighbor n: graphPrime.getNeighboursOf(u)){
                        if (tree.pred.get(n.v) == null || !tree.pred.get(n.v).equals(u)){   
                            continue;
                        }
                        Edge un = new Edge(u, n.v, n.distance);
                        if (r.getOrDefault(un, 0.0) > epsilon){
                            continue; 
                            // We already know we're gonna keep this edge for next iteration, so it won't matter if we find something higher
                        }

                        Double tempR = calcReach(u,n.v, tree, inPenalties.getOrDefault(v, 0.0));
                        edgesConsidered.add(un);

                        if (r.getOrDefault(un, 0.0) < tempR){
                            //System.out.println(r.getOrDefault(un, 0.0));
                            r.put(un, tempR);
                        }
                    }
                }
                

                long timeAfterReachCalc = System.currentTimeMillis();
                totalReachCalcTime += (timeAfterReachCalc-timeBeforeReachCalc);
            }
            System.out.println(edgesConsidered.size());
            
            
            // Prune
            Graph newGPrime = new SimpleGraph();
        

            for (Vertex v: graphPrime.getAllVertices()){
                for (Neighbor n: graphPrime.getNeighboursOf(v)){
                    Edge vn = new Edge(v, n.v, n.distance);
                    double reach = r.getOrDefault(vn, 0.0);


                    if (reach > epsilon){
                        if (!newGPrime.getAllVertices().contains(v)){
                            newGPrime.addVertex(v);
                        }
                        if(!newGPrime.getAllVertices().contains(n.v)){
                            newGPrime.addVertex(n.v);
                        }
                        newGPrime.addEdge(v, n.v, n.distance);
                    } 
                }
            }
            graphPrime = newGPrime; 
            System.out.println("We have " + (graphPrime.getAllVertices().size()/verticesInOriginalGraph)*100 + "%");
            if ((graphPrime.getAllVertices().size()/verticesInOriginalGraph)*100 < 0.05){
                break;
            }

            // Penalties
            for (Vertex v: graph.getAllVertices()){
                for (Neighbor n: graph.getNeighboursOf(v)){
                    if (graphPrime.getAllVertices().contains(v) && graphPrime.getNeighboursOf(v).contains(n)) {
                        continue;
                    } 
                    // TODO check that penalty should only be added if they're not in the graph anymore
                    Edge edge = new Edge(v, n.v, n.distance);
                    outPenalties.put(v, Math.max(outPenalties.getOrDefault(v, 0.0), r.getOrDefault(edge, INF_DIST)));

                    inPenalties.put(n.v, Math.max(inPenalties.getOrDefault(n.v, 0.0), r.getOrDefault(edge, INF_DIST)));
                }
            }

            // Save graph and reach of this iteration
            writeGraphToFile("iceland-shortcutV2"+i, graph);

            // Arc into vertex
            Map<Vertex, Double> maxIncomming = new HashMap<>();
            Map<Vertex, Double> maxOutgoing = new HashMap<>();
            for (Edge e: r.keySet()){
                maxIncomming.put(e.getEnd(), Math.max(r.getOrDefault(e, INF_DIST), maxIncomming.getOrDefault(e.getEnd(), 0.0)));
                maxOutgoing.put(e.getStart(), Math.max(r.getOrDefault(e, INF_DIST), maxOutgoing.getOrDefault(e.getStart(), 0.0)));
            }
    
            for (Vertex v: graph.getAllVertices()){
                rVertex.put(v, Math.min(maxIncomming.getOrDefault(v, INF_DIST), maxOutgoing.getOrDefault(v, INF_DIST)));
            }
    
            // High reaches set to INF to ensure it works
            for (Vertex v: graph.getAllVertices()) {
                if (rVertex.get(v) > epsilon){
                    rVertex.put(v, INF_DIST);
                }
            }
            saveReachArrayToFile("iceland-reachV2"+i, rVertex);


            // Shortcuts    
            System.out.println("Before short");    
            gPrimeShortcut = shortcut(graphPrime, graph, epsilon*1.5, inPenalties, outPenalties, r); 
            graphPrime = gPrimeShortcut;
            System.out.println("After short");    

            

            edgesConsideredThroughout.addAll(edgesConsidered);

            System.out.println("Time spent in iteration " + i + " is " +  (System.currentTimeMillis() - timeBefore)/1000);
            System.out.println("Time spent calculating reaches " + totalReachCalcTime/1000);
            epsilon = epsilon * 3;
            i++;
        } 

        // TODO Refinement phase?
        // This just gives better upperbound by doing some recalculation.

        // Arc into vertex
        Map<Vertex, Double> maxIncomming = new HashMap<>();
        Map<Vertex, Double> maxOutgoing = new HashMap<>();


        // A better translation is described on page 13
        // https://www.microsoft.com/en-us/research/wp-content/uploads/2006/01/tr-2005-132.pdf
        
        for (Edge e: r.keySet()){
            maxIncomming.put(e.getEnd(), Math.max(r.getOrDefault(e, INF_DIST), maxIncomming.getOrDefault(e.getEnd(), 0.0)));
            maxOutgoing.put(e.getStart(), Math.max(r.getOrDefault(e, INF_DIST), maxOutgoing.getOrDefault(e.getStart(), 0.0)));
        }

        for (Vertex v: graph.getAllVertices()){
            rVertex.put(v, Math.min(maxIncomming.getOrDefault(v, INF_DIST), maxOutgoing.getOrDefault(v, INF_DIST)));
        }

        // High reaches set to INF to ensure it works
        for (Vertex v: graph.getAllVertices()) {
            if (rVertex.get(v) > epsilon){
                rVertex.put(v, INF_DIST);
            }
        }


        writeGraphToFile("iceland-shortcutV2", graph);

        return rVertex;
    }

    public static Double calcReach(Vertex v, Vertex u, Tree tree, double penalty){
        double depth = depth(v,u, tree, penalty);
        double height =  height(v,u,tree);


        return Math.min(depth, height);

    }

    public static double depth(Vertex v, Vertex u, Tree tree, double penalty){
        return tree.dist.get(u) + penalty ; // TODO i think it's to the end of the edge, but unsure
    }

    public static double height(Vertex v, Vertex u, Tree tree){
        Double h = 0.0;
        //System.out.println(u);
        for (Vertex w: tree.leafs){
            /*if (tree.paths.get(w).contains(u)) {
                h = Math.max(h, tree.dist.get(w) - tree.dist.get(v)); 
            }*/

            if (tree.paths.get(w).contains(u) && tree.closed.contains(w)) {
                h = Math.max(h, tree.dist.get(w) - tree.dist.get(v)); 
            } else if (tree.paths.get(w).contains(u) && !tree.closed.contains(w)){
                h = Math.max(h, INF_DIST);
                // No need to keep going, never beating INF
                break;
            }
        }
        return h;
    }

    public static Tree partialTree(Graph g, Vertex x, double epsilon, Map<Vertex, Double> outpen, Map<Vertex, Double> inpen){
        //System.out.print("Dijkstra begun");
        // This may be super inefficient to maintain all this, but we find it easier than trying to squeze it all 
        // into one data structure.

        Set<Vertex> innerCircle = new HashSet<>();
        Set<Vertex> outerCircle = new HashSet<>();

        Map<Vertex, Double> bestDist = new HashMap<>();
        Map<Vertex, Vertex> pred = new HashMap<>();


        Map<Vertex, Integer> pathsSize = new HashMap<>(); // This may not scale idk
        Map<Vertex, Double> xprimeDist = new HashMap<>();

        Set<Vertex> leafTprime = new HashSet<>(); 
        Set<Vertex> leafT = new HashSet<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        Map<Vertex, Double> noOfChildren = new HashMap<>();

        //List<Edge> edgesConsidered = new ArrayList<>();


        Set<Vertex> closed = new HashSet<>();

        pq.add(new Pair(x, 0));
        bestDist.put(x, 0.0);
        xprimeDist.put(x, 0.0);

        //paths.put(x, new HashSet<Vertex>());
        pathsSize.put(x, 0);
        


        double bestxPrimeDist = 0.0;
        

        Set<Vertex> relevant = new HashSet<>();
        Map<Vertex, Double> ext = new HashMap<>();
        Map<Vertex, Vertex> innerParent = new HashMap<>();

        Map<Vertex, Vertex> xPrimeVertex = new HashMap<>();

        pred.put(x, x);

        while (pq.size() > 0) {
            //System.out.print(".");
            /*boolean foundMistake = leafT.size() > 0 && bestxPrimeDist > 2 * epsilon;
            //boolean foundMistake = true && innerCircle.size() > 0;
            if (foundMistake){
                for (Vertex v: innerCircle){
                    // Property 1) Check for labeled descendents
                    //Pair[] list = pq.toArray(new Pair[0]);
                    Iterator<Pair> it = pq.iterator();

                    boolean labeledDescendent = false;
                    while (it.hasNext()){
                        Vertex p = it.next().v;
                        if (pred.get(p).equals(v)){
                            labeledDescendent = true;
                            break;
                        }

                    }*/
                    /*for (Pair p: list){
                        //System.out.println(p.v);
                        //Set<Vertex> path = paths.get(p.v);
  
                        //System.out.println(p.v.toString() + path);
                        if (pred.get(p.v).equals(v)){
                            labeledDescendent = true;
                            break;
                        }
                    }*/
                    /*if (!labeledDescendent){
                        // No labeled descendent so early go on
                        continue;
                    }*/
    
                    // Property 2 relaxed)   
                    /*boolean longDist = true;
    
                    for (Pair p: list) {
                        double closestDist = INF_DIST;
                        for (Vertex w: paths.get(p.v)){
                            Vertex ps = new Vertex(56.1483202, 9.9050756);
                            if (w.equals(ps)){
                                //System.out.println("p = " + p.v);
                                //System.out.println("Best dist pv = " + bestDist.get(p.v));
                                //System.out.println("Best dist w  = " + bestDist.get(w));
                    
                            }
                            // Find closest inner circle
                            
                            if (innerCircle.contains(w)){
                                closestDist = Math.min(closestDist, bestDist.get(p.v) - bestDist.get(w));
                            }
                        }
                        if (!(closestDist > epsilon)){
                            longDist = false;
                        }
                    }
    
                    if (longDist){continue;}*/
    
                    /*foundMistake = false;
                    break;
                } 
            }*/
  


            /*if (foundMistake){
                break;
            }*/




            Pair head = pq.poll();
            if (closed.contains(head.v)){
                continue;
            }
            closed.add(head.v);
            
            // Maintain inner and outercircles.

            if (head.v.equals(x) || pred.get(head.v).equals(x)){
                xPrimeVertex.put(head.v, head.v);
            } else {
                xPrimeVertex.put(head.v, xPrimeVertex.get(pred.get(head.v)));
            }

            if (head.v.equals(x)){
                innerCircle.add(head.v);
            } else if (pred.get(head.v).equals(x)) { 
                innerCircle.add(head.v);
            } else if (xprimeDist.get(head.v) + inpen.getOrDefault(xPrimeVertex.get(head.v), 0.0) < epsilon && 
                       bestDist.get(pred.get(head.v)) + inpen.getOrDefault(x, 0.0) >= 
                       inpen.getOrDefault(pred.get(head.v), 0.0)){
                innerCircle.add(head.v);
            }

            if (!innerCircle.contains(head.v)){
                if (innerCircle.contains(pred.get(head.v))){
                    // First outside of innerCircle
                    innerParent.put(head.v, pred.get(pred.get(head.v)));
                } else {
                    // Not first outside
                    innerParent.put(head.v, innerParent.get(pred.get(head.v)));
                }
                
                ext.put(head.v, bestDist.get(head.v) - bestDist.get(innerParent.get(head.v)));
            } else {
                ext.put(head.v, 0.0);
            }

            if (innerCircle.contains(head.v)){
                relevant.add(head.v);
            } else if (relevant.contains(pred.get(head.v)) && 
                        ext.get(pred.get(head.v)) + outpen.getOrDefault(pred.get(head.v), 0.0) <= epsilon) {
                relevant.add(head.v);
            }

            if (!relevant.contains(head.v)){
                continue;
            }



            //leafTprime.add(head.v);
            if (xprimeDist.get(head.v) > bestxPrimeDist){
                bestxPrimeDist = xprimeDist.get(head.v);
            }
            
            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX
                    if (closed.contains(n.v)){
                        return;
                    }

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    //edgesConsidered.add(new Edge(head.v, n.v, maybeNewBestDistance));

                    /*if (head.v.equals(q)){
                        System.out.println(n.v + " -> " + previousBestDistance + " prev vs " + maybeNewBestDistance + " new best");
                    }*/


                    if (maybeNewBestDistance < previousBestDistance) {
                        /*if (head.v.equals(q)){
                            System.out.println(q.toString() + " -> " + n.v);
                        }*/
                        leafT.add(n.v);
                        leafT.remove(head.v);
                        
                        //Set<Vertex> path = new HashSet<Vertex>(paths.get(head.v));
                        //path.add(head.v);
                        //paths.put(n.v, path); // This should maintain all shortest paths
                        pathsSize.put(n.v, pathsSize.get(head.v)+1);

                        if (!head.v.equals(x)) {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + n.distance);
                        } else {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + 0.0);
                        }
                        bestDist.put(n.v, maybeNewBestDistance);
                        Vertex oldParent = pred.get(n.v);
                        if (oldParent != null){
                            noOfChildren.put(oldParent, noOfChildren.get(oldParent) -1); // Should never give 0
                            if (noOfChildren.get(oldParent) == 0){
                                leafT.add(oldParent);
                            }
                        } 
                        bestDist.put(n.v, maybeNewBestDistance);

                        pred.put(n.v, head.v);
                        noOfChildren.put(head.v, noOfChildren.getOrDefault(head.v, 0.0) + 1);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    } else if (maybeNewBestDistance == previousBestDistance && pathsSize.get(n.v) > pathsSize.get(head.v) + 1){
                        // We found a path that takes less steps to get to n.v
                        leafT.add(n.v);
                        leafT.remove(head.v);
                        
                        //Set<Vertex> path = new HashSet<Vertex>(paths.get(head.v));
                        //path.add(head.v);
                        //paths.put(n.v, path); // This should maintain all shortest paths
                        pathsSize.put(n.v, pathsSize.get(head.v)+1);

                        if (!head.v.equals(x)) {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + n.distance);
                        } else {
                            xprimeDist.put(n.v, xprimeDist.get(head.v) + 0.0);
                        }
                        bestDist.put(n.v, maybeNewBestDistance);
                        Vertex oldParent = pred.get(n.v);
                        if (oldParent != null){
                            noOfChildren.put(oldParent, noOfChildren.get(oldParent) -1); // Should never give 0
                            if (noOfChildren.get(oldParent) == 0){
                                leafT.add(oldParent);
                            }
                        } 
                        bestDist.put(n.v, maybeNewBestDistance);

                        pred.put(n.v, head.v);
                        noOfChildren.put(head.v, noOfChildren.getOrDefault(head.v, 0.0) + 1);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance));  
                    }
                });
        }

        //System.out.println("Dijkstra done");
        
        Map<Vertex, Set<Vertex>> paths = new HashMap<>(); // This may not scale idk
        for (Vertex v: bestDist.keySet()){
            Vertex curr = v;
            Set<Vertex> path = new HashSet<>();
            while (curr != x){
                path.add(pred.get(curr));
                curr = pred.get(curr);
            }
            path.add(x);
            paths.put(v, path);
        }

        Tree tree = new Tree(bestDist, innerCircle, outerCircle, pred, leafT, paths, closed);

        return tree;

    }

    public static Graph shortcut(Graph g, Graph origGraph, double epsilon, Map<Vertex, Double> inPen, Map<Vertex, Double> outPen, Map<Edge, Double> reach){
        //TODO add shortcuts to the graph

        // Copy the graph, so we can modify it!
        Graph gToModify = new SimpleGraph();  
        for (Vertex v: g.getAllVertices()){
            for (Neighbor n: g.getNeighboursOf(v)){
                gToModify.addVertex(v);
                gToModify.addVertex(n.v);
                gToModify.addEdge(v, n.v, n.distance);
            }
        }

        Graph ginv = GraphUtils.invertGraph(g);


        Set<Vertex> alreadyConsidered = new HashSet<>();

        Iterator<Vertex> it = g.getAllVertices().iterator();
        int max = g.getAllVertices().size();
        int tenProcent = max/10;
        int counter2 = 0;
        int counter = 0;
        long timeBefore = System.currentTimeMillis();

        while (it.hasNext()){
            /*counter++;
            if (counter % tenProcent == 0) {
                counter2++;
                System.out.println("Completed the first " + counter2*10 + "% in shortcut");
                long timeAfter = System.currentTimeMillis();
                System.out.println("Total calculating time so far " + ((timeAfter-timeBefore)/1000) + " seconds");
    
            }*/

            Vertex v = it.next();
            Vertex shortCutFrom = null;
            Vertex shortCutTo = null;
            Set<Vertex> path = new HashSet<>();


            if (alreadyConsidered.contains(v)){
                continue;
            }
            alreadyConsidered.add(v);


            Collection<Neighbor> incoming = ginv.getNeighboursOf(v);
            Collection<Neighbor> outgoing = g.getNeighboursOf(v);

            if (incoming.size() > 2 || outgoing.size() > 2){
                // It have too high in or out degree for us to care!
                continue;
            }

            // Now to actually do some shortcuts
            path.add(v);

            if (incoming.size() == 1 && outgoing.size() == 1){
                // This is potentially a oneway shortcut
                // Check that the incomming and outgoing node is not the same.
                
                if (incoming.equals(outgoing)){
                    // They are equal. This means this is an end point skip it and move on
                    continue;
                }

                Neighbor in = incoming.iterator().next();
                Neighbor out = outgoing.iterator().next();

                // This is a place where we can actually add a shortcut. BUT we want to check if we can create a longer line.
                // Find beginning of link 
                 
                Neighbor potentialLinkBefore = in; // neighbor on inverted graph
                int links = 0;

                while(ginv.getNeighboursOf(potentialLinkBefore.v).size() == 1
                && g.getNeighboursOf(potentialLinkBefore.v).size() == 1
                && ! ginv.getNeighboursOf(potentialLinkBefore.v).equals(g.getNeighboursOf(potentialLinkBefore.v))
                && ! alreadyConsidered.contains(potentialLinkBefore.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(potentialLinkBefore.v); // Todo remove debug code
                        }
                        alreadyConsidered.add(potentialLinkBefore.v);
                        path.add(potentialLinkBefore.v);
                        potentialLinkBefore = ginv.getNeighboursOf(potentialLinkBefore.v).iterator().next();
                }

                path.add(potentialLinkBefore.v);
                shortCutFrom = potentialLinkBefore.v;

                Neighbor potentialLinkAfter = out; // neighbor on normal graph
                links = 0;
                while (ginv.getNeighboursOf(potentialLinkAfter.v).size() == 1
                    && g.getNeighboursOf(potentialLinkAfter.v).size() == 1
                    && ! ginv.getNeighboursOf(potentialLinkAfter.v).equals(g.getNeighboursOf(potentialLinkAfter.v))
                    && ! alreadyConsidered.contains(potentialLinkAfter.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(links); // todo remove debug code
                        }
                        alreadyConsidered.add(potentialLinkAfter.v);
                        path.add(potentialLinkAfter.v);
                        potentialLinkAfter = g.getNeighboursOf(potentialLinkAfter.v).iterator().next();
                }
                shortCutTo = potentialLinkAfter.v;
                path.add(potentialLinkAfter.v);

                addShortcut(shortCutFrom, shortCutTo, path, epsilon, g, gToModify, 0, origGraph, inPen, outPen, reach);

            }

            if (incoming.size() == 2 && outgoing.size() == 2){

                if (!incoming.equals(outgoing)){
                    // This is not a link!
                    continue;
                }

                Iterator<Neighbor> itTemp = incoming.iterator();
                Neighbor in = itTemp.next();
                Neighbor out = itTemp.next();

                // This is a place where we can actually add a shortcut. BUT we want to check if we can create a longer line.
                // Find beginning of link 
                 
                Neighbor potentialLinkBefore = in; // neighbor on inverted graph
                int links = 0;

                while(ginv.getNeighboursOf(potentialLinkBefore.v).size() == 2
                && g.getNeighboursOf(potentialLinkBefore.v).size() == 2
                && ginv.getNeighboursOf(potentialLinkBefore.v).equals(g.getNeighboursOf(potentialLinkBefore.v))
                && ! alreadyConsidered.contains(potentialLinkBefore.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(potentialLinkBefore.v); // Todo remove debug code
                        }
                        alreadyConsidered.add(potentialLinkBefore.v);
                        path.add(potentialLinkBefore.v);
                        


                        Iterator<Neighbor> neighbours = ginv.getNeighboursOf(potentialLinkBefore.v).iterator();
                        while (neighbours.hasNext()){
                            Neighbor next = neighbours.next();
                            if (!alreadyConsidered.contains(next.v)){
                                potentialLinkBefore = next;
                            }
                        }

                }

                path.add(potentialLinkBefore.v);
                shortCutFrom = potentialLinkBefore.v;


                Neighbor potentialLinkAfter = out; // neighbor on normal graph
                links = 0;

                while (ginv.getNeighboursOf(potentialLinkAfter.v).size() == 2
                    && g.getNeighboursOf(potentialLinkAfter.v).size() == 2
                    && ginv.getNeighboursOf(potentialLinkAfter.v).equals(g.getNeighboursOf(potentialLinkAfter.v))
                    && ! alreadyConsidered.contains(potentialLinkAfter.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(links); // todo remove debug code
                        }

                        alreadyConsidered.add(potentialLinkAfter.v);
                        path.add(potentialLinkAfter.v);

                        Iterator<Neighbor> neighbours = g.getNeighboursOf(potentialLinkAfter.v).iterator();
                        while (neighbours.hasNext()){
                            Neighbor next = neighbours.next();
                            if (!alreadyConsidered.contains(next.v)){
                                potentialLinkAfter = next;
                            }
                        }
                }
                shortCutTo = potentialLinkAfter.v;
                path.add(potentialLinkAfter.v);


                addShortcut(shortCutFrom, shortCutTo, path, epsilon, g, gToModify, 1, origGraph, inPen, outPen, reach);

            }         

        }

        //TODO remove bypassed vertexes?
        return gToModify;
    }

    private static Graph addShortcut(Vertex from, Vertex to, Set<Vertex> path, double epsilon, Graph g, Graph gToModify, int direction, Graph origGraph, Map<Vertex, Double> inPen, Map<Vertex,Double> outPen, Map<Edge, Double> reach){
        if (path.size() <= 2) {
            // This case can happen if you try and shortcut something with 4 vertices originally
            return gToModify;
        }
        if (to.equals(from)){
            // This is a circle, idk how to shortcut it
            return gToModify;
        }
        for (Neighbor n: g.getNeighboursOf(from)) {
            if (n.v.equals(from)){
                return gToModify;
            }
        }
        // Find length of path!

        double length = 0;
        double lengthReverse = 0;
        Vertex curr = from;
        //System.out.println("1");
        boolean calcLength = false;
        //System.out.println(path);
        //System.out.println(to);
        //System.out.println(from);

        Set<Vertex> alreadySeen = new HashSet<>();
        alreadySeen.add(from);
        while (!calcLength){
            /*if (i > path.size()*2){
                // TODO Shit way to avoid perma looping atm
                System.out.println("We exit because of perma loop?");
                return gToModify;
            }*/

            for (Neighbor n: g.getNeighboursOf(curr)){
                if (path.contains(n.v) && ! alreadySeen.contains(n.v)){
                    // We found the neighbor that is in the link!
                    alreadySeen.add(n.v);
                    length += n.distance;
                    curr = n.v; 

                    if (curr.equals(to)){
                        calcLength = true;
                        break;
                    }  
                }
            }
            //System.out.println("does this loop?");
        }
        //System.out.println("2");

        alreadySeen = new HashSet<>();
        int j = 0;
        if (direction == 1){
            
            curr = to;
            alreadySeen.add(to);
            calcLength = false;

            while (!calcLength){

                for (Neighbor n: g.getNeighboursOf(curr)){
                    if (path.contains(n.v) && ! alreadySeen.contains(n.v)){
                        // We found the neighbor that is in the link!
                        alreadySeen.add(n.v);
                        lengthReverse += n.distance;
                        curr = n.v;
                        if (curr.equals(from)){
                            calcLength = true;
                            break;
                        } 
                    }
                }
            }
            //if (length != lengthReverse) {
            //    System.out.println("Length: " + length + ",  reverse: " + lengthReverse);
            //}
        }
        //lengthReverse = length;
        //System.out.println("3");


        // Make the recursive calls !
        // TODO update the paths, so they only include the ones needed. May give issues in "bidirectional" else
        
        Vertex middle = null;
        double bestDistToMiddle = INF_DIST;
        Set<Vertex> pathToMiddle = new HashSet<>();
        // Find middle point
        curr = from;
        double distanceToCur = 0;
        //System.out.println("curr: " + curr);
        //System.out.println("to  :" + to);
        //System.out.println(path);
        alreadySeen = new HashSet<>();
        alreadySeen.add(from);
        int i = 0;
        while (i < path.size()){
            //System.out.println(g.getNeighboursOf(curr));
            //System.out.println(alreadySeen);
            for (Neighbor n: g.getNeighboursOf(curr)){
                if (path.contains(n.v) && ! alreadySeen.contains(n.v) && !n.v.equals(to)){
                    // We found the neighbor that is in the link!
                    alreadySeen.add(n.v);
                    distanceToCur += n.distance;
                    curr = n.v;

                    //System.out.println(distanceToCur-(length/2));
                    if (Math.abs(distanceToCur-(length/2)) < bestDistToMiddle && curr != to){
                        middle = curr;
                        bestDistToMiddle = Math.abs(distanceToCur-(length/2));
                    }
                }
            }
            i++;
        }
        //System.out.println("4");

        if (path.size() > 3){
            curr = from;
            alreadySeen = new HashSet<>();
            alreadySeen.add(from);
            //System.out.println("Moving to middle " + middle);
            //System.out.println("path:" + path);
            boolean foundMid = false;
            while (!foundMid){
                for (Neighbor n: g.getNeighboursOf(curr)){
                    //System.out.println(n.v);
                    if (path.contains(n.v) && ! alreadySeen.contains(n.v) && !n.v.equals(to)){
                        // We found the neighbor that is in the link!
                        alreadySeen.add(n.v);
                        pathToMiddle.add(curr);
                        // 56.1540562,10.1826677
                        curr = n.v;
                        if (curr.equals(middle)){
                            foundMid = true;
                        }
                        //System.out.println(foundMid);
                        break;
                    }
                }
            }
            //System.out.println("Found the middle");

            //System.out.println("5");

            
            Set<Vertex> pathToEnd = new HashSet<>(path);
            pathToEnd.removeAll(pathToMiddle);
            pathToMiddle.add(middle);
            pathToEnd.add(middle);
            pathToEnd.add(to);

            //System.out.println("from -> middle");
            addShortcut(from, middle, pathToMiddle, epsilon, g, gToModify, direction, origGraph, inPen, outPen, reach);
            //System.out.println("middle -> to");

            addShortcut(middle, to, pathToEnd, epsilon, g, gToModify, direction, origGraph, inPen, outPen, reach);
        }
        // Add the "full length" shortcut
        if (length <= epsilon){
            if (direction == 0){
                gToModify.addEdge(from, to, length);
                origGraph.addEdge(from, to, length);

                /*// Update reach of removed edges
                Edge edge1 = new Edge(from, middle, bestDistToMiddle);
                reach.put(edge1, bestDistToMiddle + outPen.getOrDefault(middle, 0.0));

                Edge edge2 = new Edge(middle, to, length - bestDistToMiddle);
                reach.put(edge2, length - bestDistToMiddle + inPen.getOrDefault(middle, 0.0));
                // Remove vertex, and edges
                gToModify.removeEdge(from, middle);
                gToModify.removeEdge(middle, to);
                gToModify.removeVertex(middle);*/

            }
            else { // TODO should calculate the correct distance from both ends!
                gToModify.addEdge(from, to, length);
                gToModify.addEdge(to, from, lengthReverse);
                origGraph.addEdge(from, to, length);
                origGraph.addEdge(to, from, lengthReverse);

                // Update reach of removed edges
                /*Edge edge1 = new Edge(from, middle, bestDistToMiddle);
                reach.put(edge1, bestDistToMiddle + outPen.getOrDefault(middle, 0.0));

                Edge edge2 = new Edge(middle, to, length - bestDistToMiddle);
                reach.put(edge2, length - bestDistToMiddle + inPen.getOrDefault(middle, 0.0));

                Edge edge3 = new Edge(middle, from, bestDistToMiddle);
                reach.put(edge3, length + inPen.getOrDefault(middle, 0.0));

                Edge edge4 = new Edge(to, middle, length - bestDistToMiddle);
                reach.put(edge4, length - bestDistToMiddle + outPen.getOrDefault(middle, 0.0));

                // Remove vertex, and edges
                gToModify.removeEdge(from, middle);
                gToModify.removeEdge(middle, to);
                gToModify.removeEdge(middle, from);
                gToModify.removeEdge(to, middle);
                gToModify.removeVertex(middle);*/

            }
        } 

        return gToModify;
    }


    public static void main(String[] args){
        // 56.1349785,9.7198848: with reach 240.59535364050208 wrong reach

        //Graph graph = makeExampleGraph();
        Graph graph = GraphPopulator.populateGraph("iceland-latest-roads.csv");
        Graph prunedGraph = GraphUtils.pruneChains(graph);
        //Graph graph = makeSquareGraph(); 

        // Graph graph = makeSingleLineGraph();
        /*Graph shortCuttedGraph = shortcut(graph, 1000);
        int amountOfEdges = 0;
        int edges = 0;
        for (Vertex v: shortCuttedGraph.getAllVertices()){
            amountOfEdges += shortCuttedGraph.getNeighboursOf(v).size();
        }
        for (Vertex v: graph.getAllVertices()){
            edges += graph.getNeighboursOf(v).size();
        }
        
        System.out.println("The graph originally had " + edges);
        System.out.println("The shortcutted graph have " + amountOfEdges);*/

        /*for (Vertex v: graph.getAllVertices()){
            for (Neighbor n: graph.getNeighboursOf(v)){boegebakken-intersections
                System.out.println(n);
            }
        }*/

        long timeBefore = System.currentTimeMillis();
        //double[] bs = new double[]{100, 500, 1500, 4500, 10000, 20000};
        //double[] bs = new double[]{1,2,3,4,5, 10, 25};
        Map<Vertex, Double> r = reach(prunedGraph, 500);
        long timeAfter = System.currentTimeMillis();

        
        System.out.println(r.keySet().size() + " reaches returned");
        //System.out.println(r);

        System.out.println("Calculating reach took " + ((timeAfter-timeBefore)/1000) + " seconds");

        /*int counter = 0;
        for (Vertex v : r.keySet()){
            if (r.get(v) > bs[bs.length-1]){
                counter++;
            }
        }
        System.out.println("number of vertices with very high reach : " + counter);*/

        saveReachArrayToFile("iceland-reachV2", r);

    }

    public static void saveReachArrayToFile(String filename, Map<Vertex, Double> r ){
        try {
            File fileOne = new File(filename);
            FileOutputStream fos = new FileOutputStream(fileOne);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
    
            oos.writeObject(r);
            oos.flush();
            oos.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeGraphToFile(String filename, Graph g){
        try {
            File fileOne = new File(filename);
            FileOutputStream fos = new FileOutputStream(fileOne);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
    
            oos.writeObject(g);
            oos.flush();
            oos.close();
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }


    private static Graph makeSingleLineGraph() {
        Graph graph = new SimpleGraph();

        Vertex a = new NamedVertex("a");
        Vertex b = new NamedVertex("b");
        Vertex c = new NamedVertex("c");
        Vertex d = new NamedVertex("d");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);

        graph.addEdge(a, b, 1);
        graph.addEdge(b, c, 3);
        graph.addEdge(c, d, 2);

        return graph;
    }

    private static Graph makeDoubleLineGraph() {
        Graph graph = new SimpleGraph();

        Vertex a = new NamedVertex("a");
        Vertex b = new NamedVertex("b");
        Vertex c = new NamedVertex("c");
        Vertex d = new NamedVertex("d");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);

        graph.addEdge(a, b, 1);
        graph.addEdge(b, a, 1);
        graph.addEdge(b, c, 3);
        graph.addEdge(c, b, 3);
        graph.addEdge(c, d, 2);
        graph.addEdge(d, c, 2);

        return graph;
    }


    private static Graph makeExampleGraph() {
        Graph graph = new SimpleGraph();
        Vertex a = new NamedVertex("a");
        Vertex b = new NamedVertex("b");
        Vertex c = new NamedVertex("c");
        Vertex d = new NamedVertex("d");
        Vertex e = new NamedVertex("e");
        Vertex f = new NamedVertex("f");
        Vertex g = new NamedVertex("g");
        Vertex s = new NamedVertex("s");
        Vertex t = new NamedVertex("t");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);
        graph.addVertex(e);
        graph.addVertex(f);
        graph.addVertex(g);
        graph.addVertex(s);
        graph.addVertex(t);
        
        graph.addEdge(a, d, 9);
        graph.addEdge(a, s, 4);
        graph.addEdge(b, e, 3);
        graph.addEdge(c, e, 4);
        graph.addEdge(c, f, 6);
        graph.addEdge(c, s, 7);
        graph.addEdge(d, a, 9);
        graph.addEdge(d, e, 12);
        graph.addEdge(d, t, 13);
        graph.addEdge(e, b, 3);
        graph.addEdge(e, c, 4);
        graph.addEdge(e, d, 12);
        graph.addEdge(e, f, 2);
        graph.addEdge(e, g, 5);
        graph.addEdge(f, c, 6);
        graph.addEdge(f, e, 2);
        graph.addEdge(f, t, 9);
        graph.addEdge(g, e, 5);
        graph.addEdge(g, t, 3);
        graph.addEdge(s, a, 4);
        graph.addEdge(s, b, 5);
        graph.addEdge(s, c, 7);
        graph.addEdge(t, d, 13);
        graph.addEdge(t, f, 9);
        graph.addEdge(t, g, 3); 

        return graph;
    }

    private static Graph makeSquareGraph() {
        Graph graph = new SimpleGraph();
        Vertex a = new NamedVertex("a");
        Vertex b = new NamedVertex("b");
        Vertex c = new NamedVertex("c");
        Vertex d = new NamedVertex("d");
        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);


        graph.addEdge(a, b, 19.23);
        graph.addEdge(b, a, 19.23);
        
        graph.addEdge(a, c, 14.49);
        graph.addEdge(c, a, 14.49);
        
        graph.addEdge(c, d, 19.25);
        graph.addEdge(d, c, 19.25);
        
        graph.addEdge(b, d, 13.80);
        graph.addEdge(d, b, 13.80);

        return graph;
    }

}

class Tree {
    public Map<Vertex, Double> dist;
    public Set<Vertex> inner;
    public Set<Vertex> outer;
    public Map<Vertex,Vertex> pred;
    public Set<Vertex> leafs;
    public Map<Vertex, Set<Vertex>> paths;
    public Set<Vertex> closed;

    public Tree(Map<Vertex, Double> dist, Set<Vertex> inner, Set<Vertex> outer, Map<Vertex,Vertex> pred, Set<Vertex> leafs, Map<Vertex, Set<Vertex>> paths, Set<Vertex> closed){
        this.dist = dist;
        this.inner = inner;
        this.outer = outer;
        this.pred = pred;
        this.leafs = leafs;
        this.paths = paths;
        this.closed = closed;
    }

} 

