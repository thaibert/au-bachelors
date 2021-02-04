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

            // Filter to XML tags of type node and way
            Collection<String> wantedTypes = Arrays.asList(new String[]{"node", "way"});
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
                    String id = n.getAttributes().getNamedItem("id").getNodeValue();
                    String lat = n.getAttributes().getNamedItem("lat").getNodeValue();
                    String lon = n.getAttributes().getNamedItem("lon").getNodeValue();
                    nodeIdToLatlong.put(id, lat + "," + lon);
                }
                if ("way".equals(type)) {
                    // We hit a way! Get its contained children;
                    // <way id="...">
                    //   <nd ref="...">
                    //   <tag k="..." v="...">
                    // </way>
                    // So way has attribute id, and a list of children that contains an "nd" and a "tag".
                    String id = n.getAttributes().getNamedItem("id").getNodeValue();
                    NodeList children = n.getChildNodes();
                    Collection<String> childIDs = new ArrayList<>();

                    // Save for later whether this way has a "highway" tag.
                    boolean isHighway = false;

                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if ("nd".equals(child.getNodeName())){
                            childIDs.add(child.getAttributes().getNamedItem("ref").getNodeValue());
                        }
                        if ("tag".equals(child.getNodeName())) {
                            String k = child.getAttributes().getNamedItem("k").getNodeValue();
                            if ("highway".equals(k)) {
                                isHighway = true;
                            }
                        }
                    }
                    if (isHighway) {
                        wayIdToNodes.put(id, childIDs);
                    }
                }
            }

            // Output the collected lat/lon data from nodes to a csv.
            // Filter to only nodes that are associated to a way.
            File csv = new File("raw.csv");
            try (PrintWriter pw = new PrintWriter(csv)) {
                wayIdToNodes.forEach((k,v) -> {
                    v.forEach( id -> {
                        String latlon = nodeIdToLatlong.get(id);
                        pw.write(latlon + ",\n");
                        // System.out.println(latlon + ",");
                    });
                });
            } catch(Exception e) {
                System.out.println("--> " + e);
            }

        } catch(Exception e) {
            System.out.println(e);
        }

    }
}

