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

    public static final String FILE_PREFIX = "denmark-latest";
    public static final int NODES_PER_CHUNK = (int) 2e7;
    // 1e6 = 1 000 000 nodes per chunk gives ~12mb chunk files and 32mb csv files


    public static void main(String[] args) {
        System.out.println("Starting 1st pass");
        //FirstPassSink firstPass = firstPass(FILE_PREFIX);
        //int chunks = firstPass.getChunksCreated();

        int chunks = 1;
        System.out.println("Starting 2nd pass");
        secondPass(FILE_PREFIX, chunks);


        System.out.println("Combining in 1 csv file");
        combineCSVs(chunks);
        

        System.out.println("done");
    }

    private static FirstPassSink firstPass(String filePrefix) {
        // Stream the pbf file and pass it to the FirstPassSink
        // This class handles chunking the saved ways and node ids

        InputStream inputStream = null;
        try{
            inputStream = new FileInputStream(filePrefix + ".osm.pbf");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        OsmosisReader pbfReader = new OsmosisReader(inputStream);
        FirstPassSink firstPass =  new FirstPassSink();
        

        long start = System.currentTimeMillis();
        pbfReader.setSink(firstPass);
        pbfReader.run();
        long time = (System.currentTimeMillis() - start) / 1000;
        System.out.printf("1st pass took %d seconds\n", time);

        System.out.println("Created " + firstPass.getChunksCreated() + " chunks");
        return firstPass;
    }

    private static void secondPass(String filePrefix, int chunks) {
        // For each generated chunk of node IDs, stream the pbf file once. 
        // For each chunk, fill the data structures that SecondPassSink needs with the given data
        // For now just a simple BufferedReader and string manipulation
        // When the Sink is done, save a csv file corresponding to the chunk.

        for (int i = 0; i < chunks; i++) {
            
            InputStream chunkStream = null;
            try{
                chunkStream = new FileInputStream("out/" + filePrefix + "-chunk"+i);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(chunkStream));

            Map<String, List<String>> wayIDtoNodeIDs = new HashMap<>();
            Set<String> onewayStreets = new HashSet<>();

            try {
                String line = reader.readLine(); // skip first header line
                while ((line = reader.readLine()) != null) {
                    // System.out.println(line);
                    // Lines coming in like:
                    // wayID:oneway,node1,node2,node3
                    // where node1 etc. are node IDs

                    String[] split = line.split(":");
                    String wayID = split[0];
                    String onewayAndNodes = split[1];

                    split = onewayAndNodes.split(",");
                    String oneway = split[0];
                    if ("1".equals(oneway)) {
                        onewayStreets.add(wayID);
                    }

                    List<String> nodeIDs = new ArrayList<>();
                    for (int node = 1; node < split.length; node++) {
                        nodeIDs.add(split[node]);
                    }

                    wayIDtoNodeIDs.put(wayID, nodeIDs);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Data has been read in now, pass it on to the Sink!
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(filePrefix + ".osm.pbf");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
            OsmosisReader pbfReader = new OsmosisReader(inputStream);
            SecondPassSink secondPass = new SecondPassSink(wayIDtoNodeIDs, onewayStreets);

            long start = System.currentTimeMillis();
            System.out.print("  " + i + ": ");
            pbfReader.setSink(secondPass);
            pbfReader.run();
            long time = (System.currentTimeMillis() - start) / 1000;
            System.out.printf("chunk %d took %d seconds\n", i, time);
            writeToCSV("out/" + filePrefix + "-roads" + i + ".csv", secondPass);
        }
    }

    private static void combineCSVs(int no_of_chunks) {
        File csv = new File(FILE_PREFIX + "-roads.csv");

        try (PrintWriter pw = new PrintWriter(csv)) {
            pw.write("lat,lon,wayID,oneway,\n");
            for (int i = 0; i < no_of_chunks; i++) {
                InputStream chunkStream = null;
                try{
                    chunkStream = new FileInputStream("out/" + FILE_PREFIX + "-roads"+i + ".csv");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(chunkStream));
                String line = reader.readLine(); // skip header line
                while ((line = reader.readLine()) != null) {
                    pw.write(line + "\n");
                }
                reader.close();
            }
            
        } catch(Exception e) {
            System.out.println("--> " + e);
            return;
        }
    }

    private static void writeToCSV(String filename, SecondPassSink secondPass) {
        File csv1 = new File(filename);

        try (PrintWriter pw = new PrintWriter(csv1)) {
            pw.write("lat,lon,wayID,oneway,\n");
            
            secondPass.wayIDtoNodeID.forEach((wayID, nodes) -> {
                for (String nodeID : nodes) {
                    String latlon = secondPass.nodeIDtoCoords.get(nodeID);
                    if (latlon == null) {
                        System.out.println("null!!!");
                        System.out.println("way " + wayID + "   " + nodes);
                    }
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

    private int nodesBuffered = 0;
    private int currentChunk = 0;
    private final int nodesPerChunk = PBFExtractor.NODES_PER_CHUNK;

    // Map<String, Collection<String>> nodesInWays = new HashMap<>(); // nodeID -> set(wayID)
    // Map<String, List<String>> wayIDtoNodeID = new HashMap<>(); // wayID -> List(nodeID)
    Map<String, List<String>> buffer = new HashMap<>();  // wayID -> list(nodeID)
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

                // wayIDtoNodeID.put(curr_wayID, nodes);
                buffer.put(curr_wayID, nodes);
                nodesBuffered += nodes.size();

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

        if (nodesBuffered > nodesPerChunk) {
            // Reached size limit, write to file.
            saveChunk();
        }
    }
 
    @Override
    public void complete() {
    }
 
    @Override
    public void close() {
        saveChunk(); // flush the rest
    }

    private void saveChunk() {
        new File("out/").mkdir(); // make the out/ dir if it isn't already present
        System.out.print(currentChunk); // mix it in with the dots :)

        String out_dir = "out/";
        String filePrefix = PBFExtractor.FILE_PREFIX;
        File csv = new File(out_dir + filePrefix + "-chunk" + currentChunk);

        try (PrintWriter pw = new PrintWriter(csv)) {
            pw.write("wayID:oneway,nodeID,nodeID,...\n");
            
            buffer.forEach((wayID, nodeIDs) -> {
                String nodes = "";
                for (String nodeID : nodeIDs) {
                    nodes = nodes + "," + nodeID;
                }
                pw.write(wayID + ":" 
                    + (onewayStreets.contains(wayID) ? 1 : 0)
                    + nodes
                    + "\n");
            });
            
        } catch(Exception e) {
            System.out.println("--> " + e);
            return;
        }
        currentChunk++;
        nodesBuffered = 0;
        buffer = new HashMap<>();
        onewayStreets = new HashSet<>();
    }

    public int getChunksCreated() {
        return this.currentChunk;
    }
}


class SecondPassSink implements Sink {
    Set<String> onewayStreets;
    // Map<String, Collection<String>> nodesInWays;
    Map<String, List<String>> wayIDtoNodeID;

    Map<String, String> nodeIDtoCoords;
    Set<String> nodesInHighways;

    int iterations = 0;

    public SecondPassSink(Map<String, List<String>> wayIDtoNodeIDs, Set<String> onewayStreets) {
        this.onewayStreets = onewayStreets;
        this.wayIDtoNodeID = wayIDtoNodeIDs;

        nodeIDtoCoords = new HashMap<>();
        nodesInHighways = new HashSet<>();

        // Populate nodesInHighways
        for (List<String> nodes : wayIDtoNodeID.values()) {
            nodesInHighways.addAll(nodes);
        }
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
            // System.out.println("Unknown Entity!: " + entityContainer.getEntity().toString());
        }
    }
 
    @Override
    public void complete() {
    }
 
    @Override
    public void close() {
    }
}
