package graph;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;


public class OSMExtractor {
    public static void main(String[] args) {

        try{
            // Create "document builder" and use it to parse an XML file 
            // The XML file is in the same level as src/
            DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File file = new File("map.osm");
            InputStream input = new FileInputStream(file);
            Document doc = builder.parse(input);
            System.out.println("--------------------");
            

            // everything is contained in an <osm> ... </osm> tag
            NodeList osm = doc.getChildNodes().item(0).getChildNodes();

            // Main data structures: save nodeID -> "lat,long"
            //                        and  wayID -> [nodeID1, nodeID2,...]
            Map<String, String> nodeIdToLatlong = new HashMap<>();
            Map<String, Collection<String>> wayIdToNodes = new HashMap<>();
            Map<String, Integer> refsPerNode = new HashMap<>();

            // Filter to XML tags of type node and way
            Collection<String> wantedTypes = Arrays.asList(new String[]{"node", "way"});
            Collection<String> carTypes = Arrays.asList(new String[]{"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                                                                     "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link",
                                                                     "living_street", "service"});
            // above: the list of "highway" tag values that are for car travel. Excluding walking streets, but that should be okay.
            for (int i = 0; i < osm.getLength(); i++) {
                Node n = osm.item(i);
                String type = n.getNodeName();
                if ( ! wantedTypes.contains(type)) {
                    // skip this element if it's not node or way
                    continue;
                }

                if ("node".equals(type)) {
                    // We hit a node, save its lat, long separated by a comma
                    // <node id="..." lat="..." lon="..."> has attributes id, lat, lon.
                    // We just save all nodes, and then filter them later
                    String id = getAttribute(n, "id");
                    String lat = getAttribute(n, "lat");
                    String lon = getAttribute(n, "lon");
                    nodeIdToLatlong.put(id, lat + "," + lon);
                }
                if ("way".equals(type)) {
                    // We hit a way! Get its contained children;
                    // <way id="...">
                    //   <nd ref="...">
                    //   <tag k="..." v="...">
                    // </way>
                    // So way has attribute id, and a list of children that contains an "nd" and a "tag".
                    String id = getAttribute(n, "id");
                    NodeList children = n.getChildNodes();
                    Collection<String> childIDs = new ArrayList<>();

                    // Save for later whether this way has a "highway" tag and is a "car" type road.
                    boolean isCarAccessible = false;

                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if ("nd".equals(child.getNodeName())){
                            String nodeID = getAttribute(child, "ref");
                            childIDs.add(nodeID);
                            refsPerNode.put(nodeID, refsPerNode.getOrDefault(nodeID, 0) + 1);
                        }
                        if ("tag".equals(child.getNodeName())) {
                            String k = getAttribute(child, "k");
                            String v = getAttribute(child, "v");
                            if ("highway".equals(k) && carTypes.contains(v)) {
                                isCarAccessible = true;
                            }
                        }
                    }
                    if (isCarAccessible) {
                        wayIdToNodes.put(id, childIDs);
                    }
                }
            }

            // Output the collected lat/lon data from nodes to a csv.
            // Filter to only nodes that are associated to a way.
            File csv = new File("raw.csv");
            try (PrintWriter pw = new PrintWriter(csv)) {
                pw.write("lat,lon,wayID,\n");
                
                wayIdToNodes.forEach((wayID, nodes) -> {
                    nodes.forEach( nodeID -> {
                        // if we only want "intersection", enable if statement:
                        //if (refsPerNode.getOrDefault(nodeID, 0) > 1) {
                            String latlon = nodeIdToLatlong.get(nodeID);
                            pw.write(latlon + "," + wayID + ",\n");
                        //}
                    });
                });
                
            } catch(Exception e) {
                System.out.println("--> " + e);
            }

        } catch(Exception e) {
            System.out.println(e);
        }

    }


    private static String getAttribute(Node n, String key) {
        Node keyNode = n.getAttributes().getNamedItem(key);
        if (keyNode != null) {
            return keyNode.getNodeValue();
        }
        return "";
    }
}

