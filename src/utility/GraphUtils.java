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

    public static Vertex pickRandomVertexWithSeed(Graph g, Random rnd){
        Collection<Vertex> vertices = g.getAllVertices();
        return vertices.stream()
            .skip((int) (vertices.size() * rnd.nextDouble()))
            .findFirst()
            .get();
    }

    public static double pathDistance(List<Vertex> path) {
        // TODO: remove and replace references by realLength()
        if (path.size() < 2) {
            return 0;
        }

        double sum = 0;
        for (int i = 1; i < path.size(); i++) {
            sum += haversineDist(path.get(i-1), path.get(i));
        }
        return sum;
    }

    public static Graph pruneUndirectedChains(Graph g) {
        // Undirected chains can be pruned by removing a chain link
        //   and connecting its two neighbors (with the distances added together)
        // Special care must be taken when dealing with cycles. We compress cul-de-sacs to triangles.
        // I guess it may not be deterministic?? (technically)
        // After removing the edges to/from the unnecessary nodes, 
        //   we remove all isolated nodes at the end.
        System.out.println("--> Pruning undirected chains");
        System.out.println("  --> inverting graph");

        Graph g_inv = invertGraph(g);
        Collection<Vertex> vertices = g.getAllVertices();
        Iterator<Vertex> it = vertices.iterator();

        int pruned = 0;

        int progressBar = 100000;
        System.out.println("  --> Starting pruning. (Each dot is " + progressBar + " nodes)");

        int iterations = 0;
        while (it.hasNext()) {
            iterations++;
            if (iterations % progressBar == 0) {
                System.out.print(".");
            }

            Vertex v = it.next();
            boolean isLink = isChainLink(g, g_inv, v);

            if (! isLink) {
                // A normal node. Skip it.
                continue;
            }

            // If reached: we found a chain link!
            Iterator<Neighbor> neighbors = new HashSet<>(g.getNeighboursOf(v)).iterator();

            Neighbor a = neighbors.next();
            Neighbor b = neighbors.next();

            if (a.v.equals(b.v)) {
                // TODO: still necessary?
                continue;
            }

            // Handle the chain being reduced to a cycle
            if (isConnectedAtAll(g, a.v, b.v)) {
                // Got a cycle (aka a triangle)
                continue;
            }

            g.addEdge(a.v, b.v, a.distance + b.distance);
            g.addEdge(b.v, a.v, a.distance + b.distance);

            g_inv.addEdge(a.v, b.v, a.distance + b.distance);
            g_inv.addEdge(b.v, a.v, a.distance + b.distance);


            removeMiddleLink(g, g_inv, v);
            pruned += 1;
        }  
        System.out.println("    --> Pruned " + pruned + " nodes");

        System.out.println("  --> Removing isolated nodes");
        g = removeIsolatedNodes(g, g_inv);
        return g;
    }

    private static boolean isChainLink(Graph g, Graph g_inv, Vertex v) {
        // We showed that a chain link can be described by 
        //   * |in| = |out| = 2
        //   * in = out
        Collection<Neighbor> neighbor_in = g_inv.getNeighboursOf(v);
        Collection<Neighbor> neighbor_out = g.getNeighboursOf(v);
        Collection<Vertex> in_v =  neighbor_in.stream().map(n -> n.v).collect(Collectors.toSet());
        Collection<Vertex> out_v = neighbor_out.stream().map(n -> n.v).collect(Collectors.toSet());

        boolean isLink = in_v.size() == 2
                      && out_v.size() == 2
                      && in_v.equals(out_v);
        return isLink;
    }

    private static void removeMiddleLink(Graph g, Graph g_inv, Vertex v) {
        // We can safely assume v actually is a middle link when this is called.
        //  (Meaning  in = out, and  |in| = |out| = 2)

        // Pull vertices out of Neighbor objects, and put them in a HashSet
        Collection<Vertex> neighbors = g.getNeighboursOf(v)
            .stream()
            .map(n -> n.v)
            .collect(Collectors.toCollection(HashSet::new));

        // For all neighbors: remove edges TO and FROM v 
        for (Vertex other : neighbors) {
            g.removeEdge(other, v);
            g.removeEdge(v, other);        
        
            g_inv.removeEdge(other, v);
            g_inv.removeEdge(v, other);
        }
    }

    private static boolean isConnectedAtAll(Graph g, Vertex a, Vertex b) {
        // Check whether it is possible to travel from   a -> b   or   b -> a
        // Both are also fine.

        // Check a --> b
        Collection<Vertex> neighborsA = g.getNeighboursOf(a)
            .stream()
            .map(n -> n.v)
            .collect(Collectors.toCollection(HashSet::new));
        boolean aToB = neighborsA.contains(b);

        // Check a <-- b
        Collection<Vertex> neighborsB = g.getNeighboursOf(b)
            .stream()
            .map(n -> n.v)
            .collect(Collectors.toCollection(HashSet::new));
        boolean bToA = neighborsB.contains(a);

        return aToB || bToA;
    }

    private static Graph removeIsolatedNodes(Graph g, Graph g_inv) {
        // Remove all nodes that in- and out-degree 0
        // These were generated when pruning chains!
        // Changes the underlying graph, but it gets returned again 
        //   to make sure that the caller overwrites.
        Iterator<Vertex> it = g.getAllVertices().iterator();
        while (it.hasNext()) {
            Vertex v = it.next();

            Collection<Neighbor> out = g.getNeighboursOf(v);
            Collection<Neighbor> in  = g_inv.getNeighboursOf(v);
            
            if (out.size() == 0 && in.size() == 0) {
                it.remove();
            }
        }
        return g;
    }


    public static double realLength(Graph g, List<Vertex> path) {
        if (path.size() < 2) {
            return 0;
        }

        double dist = 0;
        Vertex prev = path.get(0);
        Vertex curr;
        for (int i = 1; i < path.size(); i++) {
            curr = path.get(i);
            for (Neighbor n : g.getNeighboursOf(prev)) {
                if (curr.equals(n.v)) {
                    dist += n.distance;
                }
            }
            prev = curr;
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

