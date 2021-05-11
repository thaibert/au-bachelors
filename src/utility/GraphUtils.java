package utility;

import graph.*;
import java.util.*;
import java.util.stream.Collectors;

public class GraphUtils {
    private static final double INF_DIST = Double.MAX_VALUE;


    public static Graph invertGraph(Graph g) {
        Collection<Vertex> vertices = g.getAllVertices();
        if (vertices.size() == 0) {
            return g;
        }

        Graph inverted = new SimpleGraph(); // TODO if we get more graph types?
        for (Vertex v : vertices) {
            inverted.addVertex(v);
        }

        for (Vertex v : vertices) {
            for (Neighbor n : g.getNeighboursOf(v)) {
                inverted.addEdge(n.v, v, n.distance);
            }
        }

        return inverted;
    }

    public static Vertex findNearestVertex(Graph g, double lat, double lon) {
        Collection<Vertex> vertices = g.getAllVertices();
        Vertex coords = new Vertex(lat, lon);

        Vertex bestSoFar = null;
        double bestDist = Double.MAX_VALUE;
        for (Vertex v : vertices) {
            if (haversineDist(v, coords) < bestDist) {
                bestSoFar = v;
                bestDist = haversineDist(v, coords);
            }
        }
        return bestSoFar;
    }

    public static Vertex findNearestVertex(Graph g, Vertex v) {
        Collection<Vertex> vertices = g.getAllVertices();

        Vertex bestSoFar = null;
        double bestDist = Double.MAX_VALUE;
        for (Vertex u : vertices) {
            if (haversineDist(u, v) < bestDist) {
                bestSoFar = u;
                bestDist = haversineDist(u, v);
            }
        }
        return bestSoFar;
    }

    public static double haversineDist(Vertex a, Vertex b) {
        double radius = 6371000; // ~6371 km
        double DEGREES_TO_RADIANS = Math.PI / 180;

        double phi_1 = a.getLatitude() * DEGREES_TO_RADIANS;
        double phi_2 = b.getLatitude() * DEGREES_TO_RADIANS;
        double lambda_1 = a.getLongitude() * DEGREES_TO_RADIANS;
        double lambda_2 = b.getLongitude() * DEGREES_TO_RADIANS;
        double dist = 2 * radius * Math.asin(
            Math.sqrt(
                hav(phi_1 - phi_2) 
              + Math.cos(phi_1)
              * Math.cos(phi_2)
              * hav(lambda_1-lambda_2)
            )
        );
        return dist;
    }
    private static double hav(double number) {
        return (1.0 - Math.cos(number)) / 2.0;
    }

    public static Vertex pickRandomVertex(Graph g) {
        Collection<Vertex> vertices = g.getAllVertices();
        return vertices.stream()
            .skip((int) (vertices.size() * Math.random()))
            .findFirst()
            .get();
    }

    public static double pathDistance(List<Vertex> path) {
        if (path.size() < 2) {
            return 0;
        }

        double sum = 0;
        for (int i = 1; i < path.size(); i++) {
            sum += haversineDist(path.get(i-1), path.get(i));
        }
        return sum;
    }

    public static Graph pruneGraphOfChains(Graph g) {
        System.out.println("pruning graph");
        Graph g_inv = invertGraph(g);
        Collection<Vertex> vertices = g.getAllVertices();
        Iterator<Vertex> it = vertices.iterator();

        System.out.println("  inverted graph, starting now");

        Set<Vertex> removed = new HashSet<>();

        Graph newGraph = new SimpleGraph();

        int iterations = 0;
        while (it.hasNext()) {
            iterations++;
            if (iterations % 100000 == 0) {
                System.out.print(".");
            }


            Vertex curr = it.next();
            
            if (removed.contains(curr)) {
                // We have pruned this node - loop again
                continue;
            }

            Collection<Neighbor> incoming = g_inv.getNeighboursOf(curr);
            Collection<Neighbor> outgoing = g.getNeighboursOf(curr);

            boolean isMiddleLink = incoming.size() == 1 
                                && outgoing.size() == 1
                                && ! incoming.equals(outgoing);
            
            if (! isMiddleLink) {
                // A normal node


            } else {
                // We're a middle link in a chain!
                Neighbor in = incoming.iterator().next();
                Neighbor out = outgoing.iterator().next();

                removed.add(curr);

                // Find start of chain
                double distBack = 0;
                Neighbor potentialLinkBefore = in; // neighbor on inverted graph
                int links = 0;
                while (g_inv.getNeighboursOf(potentialLinkBefore.v).size() == 1
                    && g.getNeighboursOf(potentialLinkBefore.v).size() == 1
                    && ! g_inv.getNeighboursOf(potentialLinkBefore.v).equals(g.getNeighboursOf(potentialLinkBefore.v))
                    && ! removed.contains(potentialLinkBefore.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(potentialLinkBefore.v); // Todo remove debug code
                        }
                        distBack += potentialLinkBefore.distance;
                        removed.add(potentialLinkBefore.v);
                        potentialLinkBefore = g_inv.getNeighboursOf(potentialLinkBefore.v).iterator().next();
                }
                distBack += potentialLinkBefore.distance;
                Neighbor chainStart = potentialLinkBefore;

                // Find end of chain
                double distForward = 0;
                Neighbor potentialLinkAfter = out; // neighbor on normal graph
                links = 0;
                while (g_inv.getNeighboursOf(potentialLinkAfter.v).size() == 1
                    && g.getNeighboursOf(potentialLinkAfter.v).size() == 1
                    && ! g_inv.getNeighboursOf(potentialLinkAfter.v).equals(g.getNeighboursOf(potentialLinkAfter.v))
                    && ! removed.contains(potentialLinkAfter.v)) {
                        // Looking at a node with 1 in, 1 out, and in != out.
                        // found another middle link
                        links++;
                        if (links > 1000) {
                            System.out.println(links); // todo remove debug code
                        }
                        distForward += potentialLinkAfter.distance;
                        removed.add(potentialLinkAfter.v);
                        potentialLinkAfter = g.getNeighboursOf(potentialLinkAfter.v).iterator().next();
                }
                distForward += potentialLinkAfter.distance;
                Neighbor chainEnd = potentialLinkAfter;

                double chainDist = distBack + distForward;

                // System.out.println("chain: " + chainStart.v + "-->" + chainEnd.v + "    dist: " + chainDist);
                if (chainDist < haversineDist(chainStart.v, chainEnd.v)) {
                    System.out.println(chainDist + " < " + haversineDist(chainStart.v, chainEnd.v));
                    System.out.println("  " + chainStart.v + "-->" + chainEnd.v);
                }

                newGraph.addVertex(chainStart.v);
                newGraph.addVertex(chainEnd.v);
                newGraph.addEdge(chainStart.v, chainEnd.v, chainDist);
            }
            // todo undirected chain: check if incoming == outgoing != empty?
        }

        // All nodes that were removed have been marked.
        // Now copy everything else over
        it = vertices.iterator();
        System.out.println("  copying intersections over");

        while (it.hasNext()) {
            Vertex curr = it.next();

            if (removed.contains(curr)) {
                continue;
            }

            newGraph.addVertex(curr);
            for (Neighbor n : g.getNeighboursOf(curr)) {
                if (! removed.contains(n.v)) {
                    newGraph.addVertex(n.v);
                    newGraph.addEdge(curr, n.v, n.distance);
                }
            }

        }



        System.out.println("\nnodes: " + g.getAllVertices().size());
        System.out.println("pruned: " + removed.size());
        System.out.println("new graph: " + newGraph.getAllVertices().size());

        return newGraph;
    }


    public static Graph pruneUndirectedChains(Graph g) {
        System.out.println("pruning graph of undirected chains");
        Graph g_inv = invertGraph(g);
        Collection<Vertex> vertices = g.getAllVertices();
        Iterator<Vertex> it = vertices.iterator();

        System.out.println("  inverted graph, starting now");

        int iterations = 0;
        while (it.hasNext()) {
            iterations++;
            if (iterations % 100000 == 0) {
                System.out.print(".");
            }


            Vertex v = it.next();
            // System.out.println("looking at " + v);
            
            Collection<Neighbor> neighbor_in = g_inv.getNeighboursOf(v);
            Collection<Neighbor> neighbor_out = g.getNeighboursOf(v);
            Collection<Vertex> in_v =  neighbor_in.stream().map(n -> n.v).collect(Collectors.toSet());
            Collection<Vertex> out_v = neighbor_out.stream().map(n -> n.v).collect(Collectors.toSet());

            boolean isMiddleLink = in_v.size() == 2
                                && out_v.size() == 2
                                && in_v.equals(out_v);
            
            if (! isMiddleLink) {
                // A normal node. Skip it.
                continue;
            } else {
                // A chain link!
                Iterator<Neighbor> neigh_it = neighbor_in.iterator();
                Neighbor a = neigh_it.next();
                Neighbor b = neigh_it.next();

                if (a.v.equals(b.v)) {
                    System.out.println("Hold up!! pruning node between the same vertex??");
                    System.out.println("  a: " + a.v);
                    System.out.println("  b: " + b.v);
                    continue;
                }
                // System.out.println(a.v + " <--> " + v + " <--> " + b.v);

                g.removeEdge(a.v, v);
                g.removeEdge(v, a.v);

                g.removeEdge(b.v, v);
                g.removeEdge(v, b.v);

                g_inv.removeEdge(a.v, v);
                g_inv.removeEdge(v, a.v);

                g_inv.removeEdge(b.v, v);
                g_inv.removeEdge(v, b.v);

                it.remove();

                g.addEdge(a.v, b.v, a.distance + b.distance);
                g.addEdge(b.v, a.v, a.distance + b.distance);

                g_inv.addEdge(a.v, b.v, a.distance + b.distance);
                g_inv.addEdge(b.v, a.v, a.distance + b.distance);


            }
        }

        return g;
    }


    public static double realLength(Graph g, List<Vertex> path) {
        if (path.size() < 2) {
            return 0;
        }

        double dist = 0;
        Vertex curr = path.get(0);
        for (int i = 1; i < path.size(); i++) {
            Vertex next = path.get(i);
            for (Neighbor n : g.getNeighboursOf(curr)) {
                if (! next.equals(n.v)) {
                    continue;
                }
                dist += n.distance;
            }
            curr = next;
        }
        return dist;
    }
    


    public static Landmarks randomLandmarks(Graph g, int noOfLandmarks){
        Graph ginv = invertGraph(g);

        Map<Vertex, Map<Vertex, Double>> distanceToLandmark = new HashMap<>();
        Map<Vertex, Map<Vertex, Double>> distanceFromLandmark = new HashMap<>();

        List<Vertex> landmarks = new ArrayList<>();  

        for (int i = 0; i < noOfLandmarks; i++) {
            landmarks.add(GraphUtils.pickRandomVertex(g));
        }


        landmarks.forEach( l -> {
            System.out.print(".");
            Map<Vertex, Double> normal = dijkstra(g, l);
            Map<Vertex, Double> inv = dijkstra(ginv, l);

            distanceFromLandmark.put(l, normal);
            distanceToLandmark.put(l, inv);
        });

        Landmarks out = new Landmarks(distanceToLandmark, distanceFromLandmark);

        return out;
    }


    
    public static Landmarks farthestLandmarks(Graph g, int noOfLandmarks){
        // TODO this does something weird on small data sets, but on Denmarks does exactly what would be expected !!!!

        Graph ginv = invertGraph(g);

        Map<Vertex, Map<Vertex, Double>> distanceToLandmark = new HashMap<>();
        Map<Vertex, Map<Vertex, Double>> distanceFromLandmark = new HashMap<>();

        List<Vertex> landmarks = new ArrayList<>();  

        // Phase 1, pick the landmarks.
        // We use the farthest-landmark picking idea for this one
    
        // Random landmark to get started
        //System.out.println("Searching for landmarks");

        Vertex random = GraphUtils.pickRandomVertex(g);
        landmarks.add(random);

        //Keep track of all distances from landmarks to other vertices, so we can find the one furthest away
        List<Map<Vertex, Double>> distances = new ArrayList<>();
        distances.add(dijkstra(g, random));

        for (int i = 0; i < noOfLandmarks; i++){
            System.out.print(".");
            double max = 0;
            Vertex maxLandmark = null;
            for (Vertex v: g.getAllVertices()){
                double dist = INF_DIST;
                // At most noOfLandmarks iterations, or maybe noOfLandsmarks -1
                for (int j = 0; j < landmarks.size(); j++){
                    dist = Math.min(distances.get(j).getOrDefault(v, 0.0), dist); 
                }
                
                if (dist > max && !landmarks.contains(v)) {
                    max = dist;
                    maxLandmark = v;
                }
            }
            if (i == 0) {
                landmarks.remove(random);
                distances.clear();
                System.out.println(landmarks.size());
                System.out.println(distances.size());

            }
            landmarks.add(maxLandmark);
            distances.add(dijkstra(g, maxLandmark));
        }
        //System.out.println("\nSize of landmarks: " + landmarks.size());
        //System.out.println(landmarks);
        //System.out.println("Found landmarks\nCalculating distances");
        distances.clear(); // Performance
        // Phase 2, calc distance to and from landmarks from all other vertices
        landmarks.forEach( l -> {
            System.out.print(".");
            Map<Vertex, Double> normal = dijkstra(g, l);
            Map<Vertex, Double> inv = dijkstra(ginv, l);

            distanceFromLandmark.put(l, normal);
            distanceToLandmark.put(l, inv);
        });

        Landmarks out = new Landmarks(distanceToLandmark, distanceFromLandmark);

        return out;
    }


    public static List<Map<Vertex, Map<Vertex, Double>>> avoidLandmarks(Graph g, int noOfLandmarks){
        
        return null;
    }

    public static List<Map<Vertex, Map<Vertex, Double>>> planerLandmarks(Graph g, int noOfLandmarks){
        
        return null;
    }

    public static Map<Vertex, Double> dijkstra(Graph g, Vertex start){

        //  Pseudocode from CLRS
        //  Initialize-Single-Source(G, s) (s = source)
        //  S = Ø
        //  Q = G.V
        //  While Q != Ø
        //      u = Extract-Min(Q)    
        //      S = S U {u}
        //      for each vertex v in G.Adj[u]
        //          Relax(u,v,w)   



        Map<Vertex, Double> bestDist = new HashMap<>();

        DistComparator comp = new DistComparator();
        PriorityQueue<Pair> pq = new PriorityQueue<>(comp);

        pq.add(new Pair(start, 0));
        
        while (pq.size() > 0) {

            Pair head = pq.poll();


            g.getNeighboursOf(head.v)
                .forEach(n -> {
                    // RELAX

                    double maybeNewBestDistance = head.dist + n.distance;
                    double previousBestDistance = bestDist.getOrDefault(n.v, INF_DIST);

                    if (maybeNewBestDistance < previousBestDistance) {
                        bestDist.put(n.v, maybeNewBestDistance);

                        // put back in PQ with new dist, but leave the old, "wrong" dist in there too.
                        pq.add(new Pair(n.v, maybeNewBestDistance)); 
                    }
                });
        }
        return bestDist;
    }
}

