package datamanipulation;

import java.io.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import java.util.*;

public class XMLFilter {

    private static Collection<String> carTypes = Arrays.asList(
        new String[]{"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                     "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link",
                     "living_street", "service"} );
    private static Collection<String> roundabouts = Arrays.asList(
        new String[]{"roundabout", "mini-roundabout", "circular"} );



    public static void filter(String osmFile, String outPrefix) {
        Map<String, List<String>> wayID_to_nds = new HashMap<>();
        Set<String> onewayStreets = new HashSet<>();

        Map<String, String> nodeIDtoCoords = new HashMap<>();
        Map<String, Integer> refsPerNode = new HashMap<>();

        int events = 0;
        try {
            System.out.println("--> 1st pass of " + osmFile);
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(osmFile));
            

            String curr_wayID = "";
            boolean isCarRoad = false;
            List<String> curr_refs = new ArrayList<>();

            while(eventReader.hasNext()) {
                events++;
                if (events % 10e5 == 0) {
                    System.out.print(".");
                }
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    // <whatever>
                    StartElement startElement = event.asStartElement();
                    String qName = startElement.getName().getLocalPart();
                    
                    if (qName.equalsIgnoreCase("way")) {
                        Attribute id_attr = startElement.getAttributeByName(new QName("id"));
                        curr_wayID = id_attr.getValue();
                    }
                    if (qName.equalsIgnoreCase("nd")) {
                        Attribute ref_attr = startElement.getAttributeByName(new QName("ref"));
                        curr_refs.add(ref_attr.getValue());
                    }
                    if (qName.equalsIgnoreCase("tag")) {
                        String k = startElement.getAttributeByName(new QName("k")).getValue();
                        String v = startElement.getAttributeByName(new QName("v")).getValue();
                        
                        if ( "highway".equals(k) && carTypes.contains(v) ) {
                            // Actually a road meant for car travel
                            isCarRoad = true;
                        }
                        if ("oneway".equals(k) && "yes".equals(v)) {
                            onewayStreets.add(curr_wayID);
                        }
                        if ("junction".equals(k) && roundabouts.contains(v)) {
                            // Roundabouts are one-way!
                            onewayStreets.add(curr_wayID);
                        }
                    }
                }


                else if (event.getEventType() == XMLStreamConstants.END_ELEMENT) {
                    // </whatever>
                    EndElement endElement = event.asEndElement();
                    String qName = endElement.getName().getLocalPart();
                    
                    if (qName.equalsIgnoreCase("way") ) {
                        if (isCarRoad) {
                            // Include way in output
                            wayID_to_nds.put(curr_wayID, curr_refs);

                            // Count up how many times each node is ref'd
                            for (String nodeID : curr_refs) {
                                int current = refsPerNode.getOrDefault(nodeID, 0);
                                refsPerNode.put(nodeID, current + 1);
                            }
                        }
                        // reset values
                        curr_wayID = "";
                        isCarRoad = false;
                        curr_refs = new ArrayList<>();
                    }
                }
                // if (event.getEventType() == XMLStreamConstants.CHARACTERS) {//todo does it ever appear? }
            }
            System.out.println(); // The "progress bar" dots have no newline, so print one here.

        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.printf("  --> %10d events processed\n", events);

        Set<String> appears = new HashSet<>();
        for (List<String> refs : wayID_to_nds.values()) {
            for (String ref : refs) {
                appears.add(ref);
            }
        }
        System.out.printf("  --> %10d refs found\n", appears.size());






        // Run through again, this time get nodes out
        try {
            System.out.println("--> 2nd pass");
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(osmFile));

            while(eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();
                    String qName = startElement.getName().getLocalPart();

                    if (qName.equalsIgnoreCase("node")) {
                        String node_id = startElement.getAttributeByName(new QName("id")).getValue();
                        if (appears.contains(node_id)) {
                            String lat = startElement.getAttributeByName(new QName("lat")).getValue();
                            String lon = startElement.getAttributeByName(new QName("lon")).getValue();
                            nodeIDtoCoords.put(node_id, lat + "," + lon);
                            
                            if (nodeIDtoCoords.keySet().size() % (appears.size()/10) == 0) {
                                System.out.printf("  --> %3d %% \n", (1 + 100 * nodeIDtoCoords.keySet().size()/appears.size() ));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }



        // WRITE TO CSV

        System.out.println("--> Writing  " + outPrefix + "-all-roads.csv");
        File csv1 = new File(outPrefix + "-all-roads.csv");
        try (PrintWriter pw = new PrintWriter(csv1)) {
            pw.write("lat,lon,wayID,oneway,\n");
            
            wayID_to_nds.forEach((wayID, nodes) -> {
                nodes.forEach( nodeID -> {
                        String latlon = nodeIDtoCoords.get(nodeID);
                        int oneway = onewayStreets.contains(wayID) ? 1 : 0;
                        pw.write(latlon + "," + wayID + "," + oneway + ",\n");
                });
            });
            
        } catch(Exception e) {
            System.out.println("--> " + e);
            return;
        }

        System.out.println("--> Writing  " + outPrefix + "-intersections.csv");
        File csv2 = new File(outPrefix + "-intersections.csv");
        try (PrintWriter pw = new PrintWriter(csv2)) {
            pw.write("lat,lon,wayID,oneway,\n");
            
            wayID_to_nds.forEach((wayID, nodes) -> {
                nodes.forEach( nodeID -> {
                    // Filtering on ways ref'ing this node filter for intersections!
                    if (refsPerNode.getOrDefault(nodeID, 0) > 1) {
                        String latlon = nodeIDtoCoords.get(nodeID);
                        String oneway = onewayStreets.contains(wayID) ? "1" : "0";
                        pw.write(latlon + "," + wayID + "," + oneway + ",\n");
                    }
                });
            });

        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

    }

    public static void main(String[] args) {
        filter("denmark-latest.osm", "denmark");
        // filter("aarhus-silkeborg.osm", "aarhus-silkeborg");
    }
}
