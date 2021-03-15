package datamanipulation;

import java.io.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.apache.commons.compress.compressors.*;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;

import java.util.*;

public class XMLFilter2 {

    private static Collection<String> carTypes = Arrays.asList(
        new String[]{"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                     "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link",
                     "living_street", "service"} );
    private static Collection<String> roundabouts = Arrays.asList(
        new String[]{"roundabout", "mini-roundabout", "circular"} );



    public static void filter(String osmFile, String outPrefix) {
        
    }

    public static void main(String[] args) {
        filter("/media/andreas/3E3D-9507/europe-210303.osm.bz2", "europe");
    }
}
