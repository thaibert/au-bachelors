package graph;

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
                            String id = startElement.getAttributeByName(new QName("id")).toString();
                            String lat = startElement.getAttributeByName(new QName("lat")).toString();
                            String lon = startElement.getAttributeByName(new QName("lon")).toString();
                            nodeIdToLatlong.put(id, lat + "," + lon);
                            break;
                        case "way":
                            

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
