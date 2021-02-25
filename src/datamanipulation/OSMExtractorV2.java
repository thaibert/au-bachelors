package datamanipulation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.namespace.QName;


public class OSMExtractorV2 {
    
    public static void main(String[] args) {
        parseXMLToCSV("denmark-latest.osm");
    }

    public static boolean parseXMLToCSV(String xmlFilename) {
        double iterations = 0;

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        try{ // Inspiration: https://www.baeldung.com/java-stax
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(xmlFilename));

            // Main data structures
            Map<String, String> nodeIdToLatlong = new HashMap<>();
            Map<String, Collection<String>> wayIdToNodes = new HashMap<>();
            Map<String, Boolean> onewayStreets = new HashMap<>();
            Map<String, Integer> refsPerNode = new HashMap<>();

            // Filter to XML tags of type node and way
            Collection<String> wantedTypes = Arrays.asList(new String[]{"node", "way"});
            Collection<String> carTypes = Arrays.asList(new String[]{"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                                                                                 "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link",
                                                                                 "living_street", "service"});

            String id = null;
            String lat = null;
            String lon = null;

            String k = null;
            String v = null;

            XMLEvent innerNextEvent = null;

            while(reader.hasNext()){

                // Between 382 000 000 and 383 000 000 events in denmark-latest.osm
                if (iterations % 1000000 == 0) {
                    System.out.printf("    --> %f \n", iterations);
                }

                XMLEvent nextEvent = reader.nextEvent(); 
                
                if (nextEvent.isStartElement()) {
                    StartElement startElement = nextEvent.asStartElement();
                    switch(startElement.getName().getLocalPart()) {
                        case "node":
                            id = startElement.getAttributeByName(new QName("id")).toString();
                            lat = startElement.getAttributeByName(new QName("lat")).toString();
                            lon = startElement.getAttributeByName(new QName("lon")).toString();
                            nodeIdToLatlong.put(id, lat + "," + lon);
                            break;
                        case "way":
                            id = startElement.getAttributeByName(new QName("id")).toString();

                            Collection<String> childIds = new ArrayList<>();

                            boolean isCarAccessible = false; 

                            while (reader.hasNext())
                                iterations++;
                                innerNextEvent = reader.nextEvent();
                                if (innerNextEvent.isStartElement()) {
                                    StartElement innerStartElement = innerNextEvent.asStartElement();
                                    switch(innerStartElement.getName().getLocalPart()) {
                                        case "nd":
                                            
                                            break;
                                        
                                        case "tag":
                                            k = startElement.getAttributeByName(new QName("k")).toString();
                                            v = startElement.getAttributeByName(new QName("v")).toString();

                                            if ("highway".equals(k) && carTypes.contains(v)) {
                                                isCarAccessible = true;
                                            }
                                            if ("oneway".equals(k) && "yes".equals(v)) {
                                                onewayStreets.put(id, true);
                                            }

                                            break;
                                    }

                                }
                                if (innerNextEvent.isEndElement()) {
                                    EndElement innerEndElement = innerNextEvent.asEndElement();

                                    if (innerEndElement.getName().getLocalPart().equals("way")) {
                                        break;
                                    }
                                }
                            
                            break;
                }

                } 
                if (nextEvent.isEndElement()) {
                    EndElement endElement = nextEvent.asEndElement();

                }

                iterations++;
            }

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }

        return true;
    }

}
