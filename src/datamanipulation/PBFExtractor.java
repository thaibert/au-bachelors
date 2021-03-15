package datamanipulation;
 
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.*;
 
import crosby.binary.osmosis.OsmosisReader;
 
/**
 * Receives data from the Osmosis pipeline and prints ways which have the
 * 'highway key.
 * 
 * @author pa5cal
 */

public class PBFExtractor {

    public static void main(String[] args) throws FileNotFoundException {
        String filePrefix = "denmark-latest";


        InputStream inputStream1 = new FileInputStream(filePrefix + ".osm.pbf");
        OsmosisReader reader1 = new OsmosisReader(inputStream1);
        FirstPassSink firstPass =  new FirstPassSink();

        long start = System.currentTimeMillis();
        reader1.setSink(firstPass);
        reader1.run();
        long time = (System.currentTimeMillis() - start) / 1000;
        System.out.printf("1st pass took %d seconds\n", time);



        InputStream inputStream2 = new FileInputStream(filePrefix + ".osm.pbf");
        OsmosisReader reader2 = new OsmosisReader(inputStream2);
        SecondPassSink secondPass = new SecondPassSink(firstPass);

        start = System.currentTimeMillis();
        reader2.setSink(secondPass);
        reader2.run();
        time = (System.currentTimeMillis() - start) / 1000;
        System.out.printf("2nd pass took %d seconds\n", time);

        writeToCSV(filePrefix, secondPass);

        System.out.println("done");
    }

    private static void writeToCSV(String outPrefix, SecondPassSink secondPass) {
        System.out.println("Writing to csv");
        System.out.println("--> Writing  " + outPrefix + "-all-roads.csv");
        File csv1 = new File(outPrefix + "-all-roads.csv");

        try (PrintWriter pw = new PrintWriter(csv1)) {
            pw.write("lat,lon,wayID,oneway,\n");
            
            secondPass.wayIDtoNodeID.forEach((wayID, nodes) -> {
                for (String nodeID : nodes) {
                    String latlon = secondPass.nodeIDtoCoords.get(nodeID);
                    int oneway = secondPass.onewayStreets.contains(wayID) ? 1 : 0;

                    pw.write(latlon + "," + wayID + "," + oneway + ",\n");
                }
            });
            
        } catch(Exception e) {
            System.out.println("--> " + e);
            return;
        }
    }
}


class FirstPassSink implements Sink {
    private static Collection<String> carTypes = Arrays.asList(
        new String[]{"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                     "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link",
                     "living_street", "service"} );
    private static Collection<String> roundabouts = Arrays.asList(
        new String[]{"roundabout", "mini-roundabout", "circular"} );

    // Map<String, Collection<String>> nodesInWays = new HashMap<>(); // nodeID -> set(wayID)
    Map<String, List<String>> wayIDtoNodeID = new HashMap<>(); // wayID -> List(nodeID)
    Set<String> onewayStreets = new HashSet<>();

    int iterations = 0;
    @Override
    public void initialize(Map<String, Object> arg0) {
    }
    
    

    @Override
    public void process(EntityContainer entityContainer) {
        iterations++;
        if (iterations % 1e6 == 0) {
            System.out.print(".");
        }

        if (entityContainer instanceof WayContainer) {
            Way way = ((WayContainer) entityContainer).getEntity();
            String curr_wayID = Long.toString(way.getId());

            boolean isCarRoad = false;
            
            for (Tag tag : way.getTags()) {
                String k = tag.getKey();
                String v = tag.getValue();
                
                if ( "highway".equalsIgnoreCase(k) && carTypes.contains(v) ) {
                    // Actually a road meant for car travel
                    isCarRoad = true;
                }
                if ("oneway".equalsIgnoreCase(k) && "yes".equalsIgnoreCase(v)) {
                    onewayStreets.add(curr_wayID);
                }
                if ("junction".equalsIgnoreCase(k) && roundabouts.contains(v)) {
                    // Roundabouts are one-way!
                    onewayStreets.add(curr_wayID);
                }
                if ("construction".equalsIgnoreCase(k) && carTypes.contains(v)) {
                    // road maybe under construction, sometimes also an old tag
                    isCarRoad = true;
                }
            }


            if (isCarRoad) {
                List<String> nodes = way.getWayNodes()
                    .stream()
                    .map((WayNode wn) -> Long.toString(wn.getNodeId()))
                    .collect(Collectors.toList());

                wayIDtoNodeID.put(curr_wayID, nodes);

                // // Get nodeIDs of all refs in this way, put them in a list.
                // way.getWayNodes().forEach((WayNode wn) -> {
                //     String nodeID = Long.toString(wn.getNodeId());
                //     if (nodesInWays.containsKey(nodeID)) {
                //         // Node already ref'd before
                //         nodesInWays.get(nodeID).add(curr_wayID);
                //     } else {
                //         // First ref for this node
                //         Collection<String> ways = new ArrayList<>();
                //         ways.add(curr_wayID);
                //         nodesInWays.put(nodeID, ways);
                //     }
                // });
                
                 
            }

        } else if (entityContainer instanceof NodeContainer || entityContainer instanceof RelationContainer) {
        } else {
            System.out.println("Unknown Entity!: " + entityContainer.getEntity().toString());
        }
    }
 
    @Override
    public void complete() {
    }
 
    @Override
    public void close() {
    }
}


class SecondPassSink implements Sink {
    Set<String> onewayStreets;
    // Map<String, Collection<String>> nodesInWays;
    Map<String, List<String>> wayIDtoNodeID;

    Map<String, String> nodeIDtoCoords;
    Set<String> nodesInHighways;

    int iterations = 0;

    public SecondPassSink(FirstPassSink firstPass) {
        // Copy required data from first pass
        onewayStreets = firstPass.onewayStreets;
        // nodesInWays = firstPass.nodesInWays;
        wayIDtoNodeID = firstPass.wayIDtoNodeID;

        nodeIDtoCoords = new HashMap<>();
        nodesInHighways = new HashSet<>();

        // Populate nodesInHighways
        for (List<String> nodes : wayIDtoNodeID.values()) {
            nodesInHighways.addAll(nodes);
        }
        System.out.println("Done creating secondpasssink");
    }
 
    @Override
    public void initialize(Map<String, Object> arg0) {
    }
    
    @Override
    public void process(EntityContainer entityContainer) {
        iterations++;
        if (iterations % 1e6 == 0) {
            System.out.print(".");
        }

        if (entityContainer instanceof NodeContainer) {

            Node node = ((NodeContainer) entityContainer).getEntity();
            String nodeID = Long.toString(node.getId());

            if (nodesInHighways.contains(nodeID)) {
                // We hit a node ref'd by a highway!

                 // truncate to float to get rid of rounding errors
                float lat = (float) node.getLatitude(); 
                float lon = (float) node.getLongitude();
                String latlon = lat + "," + lon;
                nodeIDtoCoords.put(nodeID, latlon);
            }

        } else if (entityContainer instanceof WayContainer || entityContainer instanceof RelationContainer) {
        } else {
            System.out.println("Unknown Entity!: " + entityContainer.getEntity().toString());
        }
    }
 
    @Override
    public void complete() {
    }
 
    @Override
    public void close() {
    }
}
