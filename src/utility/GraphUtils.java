package utility;

import graph.*;
import java.util.*;
import java.util.stream.Collectors;

import org.checkerframework.checker.units.qual.min;

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

    public static Graph pruneChains(Graph g) {
        g = pruneUndirectedChains(g);
        g = pruneDirectedChains(g);
        return g;
    }

    private static Graph pruneDirectedChains(Graph g) {
        // Prune directed chains from the graph
        // Meaning e.g.:
        //     a -1-> b -1-> c -1-> d
        // should turn into
        //     a -3-> d
        System.out.println("--> Pruning directed chains");

        System.out.println("  --> inverting graph");
        Graph g_inv = invertGraph(g);

        Collection<Vertex> vertices = g.getAllVertices();
        Iterator<Vertex> it = vertices.iterator();

        int prunedNodes = 0;
        int prunedEdges = 0;

        int progressBar = 100000;
        System.out.println("  --> Starting pruning. (Each dot is " + progressBar + " nodes)");

        int iterations = 0;
        while (it.hasNext()) {
            iterations++;
            if (iterations % progressBar == 0) {
                System.out.print(".");
            }

            Vertex v = it.next();

            Collection<Neighbor> neighbor_in = g_inv.getNeighboursOf(v);
            Collection<Neighbor> neighbor_out = g.getNeighboursOf(v);
            Collection<Vertex> in_v =  neighbor_in.stream().map(n -> n.v).collect(Collectors.toSet());
            Collection<Vertex> out_v = neighbor_out.stream().map(n -> n.v).collect(Collectors.toSet());

            boolean isLink = in_v.size() == 1
                        && out_v.size() == 1
                        && ! in_v.equals(out_v);
            
            if (! isLink) {
                continue;
            }

            // if reached: we have a one-way chain link!

            // pull out neighbors for easy access
            Neighbor in = neighbor_in
                .stream()
                .collect(Collectors.toList())
                .get(0);

            Neighbor out = neighbor_out
                .stream()
                .collect(Collectors.toList())
                .get(0);
            
            // Handle cycles not collapsing - make them a triangle
            boolean isTriangle = isConnectedAtAll(g, in.v, out.v);
            if (isTriangle) {
                continue;
            }

            // Add new edges and isolate v
            g.addEdge(    in.v, out.v, out.distance + in.distance);
            g_inv.addEdge(out.v, in.v, out.distance + in.distance);

            removeMiddleLink(g, g_inv, v);

            prunedNodes += 1;
            prunedEdges += 2-1;
        }
        System.out.println("    --> Pruned " + prunedNodes + " nodes");
        System.out.println("    --> Pruned " + prunedEdges + " edges");

        System.out.println("  --> Removing isolated nodes");
        g = removeIsolatedNodes(g, g_inv);

        return g;
    }

    private static Graph pruneUndirectedChains(Graph g) {
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

        int prunedNodes = 0;
        int prunedEdges = 0;

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

            // pull out neighbors for easy access
            List<Neighbor> neighbors = g.getNeighboursOf(v).stream().collect(Collectors.toList());

            Neighbor a = neighbors.get(0);
            Neighbor b = neighbors.get(1);

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
            prunedNodes += 1;
            prunedEdges += (4-2)/2; // counting undirected edges, hence div by 2
        }  
        System.out.println("    --> Pruned " + prunedNodes + " nodes");
        System.out.println("    --> Pruned " + prunedEdges + " edges");

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
        Collection<Vertex> out = g.getNeighboursOf(v)
            .stream()
            .map(n -> n.v)
            .collect(Collectors.toCollection(HashSet::new));
        Collection<Vertex> in = g_inv.getNeighboursOf(v)
            .stream()
            .map(n -> n.v)
            .collect(Collectors.toCollection(HashSet::new));
        Collection<Vertex> neighbors = new HashSet<>();
        neighbors.addAll(out);
        neighbors.addAll(in);

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
        int seed = 10;  
        Random rnd = new Random(seed);


        for (int i = 0; i < noOfLandmarks; i++) {
            landmarks.add(GraphUtils.pickRandomVertexWithSeed(g, rnd));
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

        Random rnd = new Random(5);
        Vertex random = GraphUtils.pickRandomVertexWithSeed(g, rnd);
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


    public static Landmarks PartionCorner(Graph g, int noOfLandmarks){
        // noOfLandmarks need to be divisible with 4
        Set<Vertex> landmarks = new HashSet<>();

        Graph ginv = invertGraph(g);

        // This is hardcore an hack
        // Find a vertex that can reach at least 70% of all nodes in the graph
        Set<Vertex> reachable = null;
        boolean keepSearching = true;        
        Random rnd = new Random(5);
        while (keepSearching){
            Vertex v = pickRandomVertexWithSeed(g, rnd);
            Map<Vertex, Double> distance = dijkstra(g, v);
            if (distance.keySet().size() > g.getAllVertices().size() * 0.7){
                reachable = distance.keySet();
                keepSearching = false;
            }
        }

        Map<Vertex, Map<Vertex, Double>> distanceToLandmark = new HashMap<>();
        Map<Vertex, Map<Vertex, Double>> distanceFromLandmark = new HashMap<>();

        // Calculate the middle most vertex in g
        // Do this the "lazy" way with finding the vertex closet to the middle by coordinates
        double minLatitude = INF_DIST;
        double minLongitude = INF_DIST;
        double maxLatitude = -INF_DIST;
        double maxLongitude = -INF_DIST;

        for (Vertex v: g.getAllVertices()){
            if (!reachable.contains(v)){
                continue;
            }
            if (v.getLatitude() > maxLatitude){
                maxLatitude = v.getLatitude();
            }
            if (v.getLatitude() < minLatitude){
                minLatitude = v.getLatitude();
            }
            if (v.getLongitude() < minLongitude){
                minLongitude = v.getLongitude();
            }
            if (v.getLongitude() > maxLongitude){
                maxLongitude = v.getLongitude();
            }
        }

        if (noOfLandmarks == 4){
            // no partion is needed
            landmarks.add(findExtremeAbove(g, 0, minLatitude, maxLatitude, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 1, minLatitude, maxLatitude, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 0, minLatitude, maxLatitude, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 1, minLatitude, maxLatitude, minLongitude, maxLongitude, reachable));
        } else if(noOfLandmarks == 8){
            // find middle point on longitude
            // TODO should this be partitioned in another way?
            double middleLat = (minLatitude + maxLatitude)/2; 
            landmarks.add(findExtremeAbove(g, 0, minLatitude, middleLat, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 1, minLatitude, middleLat, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 0, minLatitude, middleLat, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 1, minLatitude, middleLat, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 0, middleLat, maxLatitude, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 1, middleLat, maxLatitude, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 0, middleLat, maxLatitude, minLongitude, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 1, middleLat, maxLatitude, minLongitude, maxLongitude, reachable));

            // Partion into noOfLandmarks/4 squares
        } else if(noOfLandmarks == 16){
            double middleLat = (minLatitude + maxLatitude)/2; 
            double middleLong = (minLongitude + maxLongitude)/2; 

            landmarks.add(findExtremeAbove(g, 0, minLatitude, middleLat, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeAbove(g, 1, minLatitude, middleLat, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeBelow(g, 0, minLatitude, middleLat, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeBelow(g, 1, minLatitude, middleLat, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeAbove(g, 0, middleLat, maxLatitude, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeAbove(g, 1, middleLat, maxLatitude, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeBelow(g, 0, middleLat, maxLatitude, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeBelow(g, 1, middleLat, maxLatitude, minLongitude, middleLong, reachable));
            landmarks.add(findExtremeAbove(g, 0, minLatitude, middleLat, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 1, minLatitude, middleLat, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 0, minLatitude, middleLat, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 1, minLatitude, middleLat, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 0, middleLat, maxLatitude, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeAbove(g, 1, middleLat, maxLatitude, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 0, middleLat, maxLatitude, middleLong, maxLongitude, reachable));
            landmarks.add(findExtremeBelow(g, 1, middleLat, maxLatitude, middleLong, maxLongitude, reachable));
        } else{
            throw new RuntimeException("Please provide valid noOfLandmarks divisible with 4");

        }
        
        landmarks.remove(null);

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

    public static Vertex findExtremeBelow(Graph g, int axis, double minLat, double maxLat, double minLong, double maxLong, Set<Vertex> reachable){
        Vertex v = null;
        for (Vertex w: g.getAllVertices()){
            if (axis == 0){
                // TODO should pref be <= arg, but that could give the same landmark multiple times
                if (w.getLatitude() < maxLat && w.getLongitude() < maxLong && w.getLongitude() > minLong && reachable.contains(w)){
                    if (v == null){
                        v = w;
                    } else if(w.getLatitude() > v.getLatitude()){
                        v = w;  
                    }
                }
            } else if (axis == 1){
                // TODO should pref be <= arg, but that could give the same landmark multiple times
                if (w.getLongitude() < maxLong && w.getLatitude() < maxLat && w.getLatitude() > minLat && reachable.contains(w)){
                    if (v == null){
                        v = w;
                    } else if(w.getLongitude() > v.getLongitude()){
                        v = w;  
                    }
                }
            }
        }
        return v;
    }

    public static Vertex findExtremeAbove(Graph g, int axis, double minLat, double maxLat, double minLong, double maxLong, Set<Vertex> reachable){
        Vertex v = null;
        for (Vertex w: g.getAllVertices()){
            if (axis == 0){
                // TODO should pref be >= arg, but that could give the same landmark multiple times
                if (w.getLatitude() > minLat && w.getLongitude() < maxLong && w.getLongitude() > minLong && reachable.contains(w)){
                    if (v == null){
                        v = w;
                    } else if(w.getLatitude() < v.getLatitude()){
                        v = w;  
                    }
                }
            } else if (axis == 1){
                // TODO should pref be >= arg, but that could give the same landmark multiple times
                if (w.getLongitude() > minLong && w.getLatitude() < maxLat && w.getLatitude() > minLat && reachable.contains(w)){
                    if (v == null){
                        v = w;
                    } else if(w.getLongitude() < v.getLongitude()){
                        v = w;  
                    }
                }
            }
        }
        return v;
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

