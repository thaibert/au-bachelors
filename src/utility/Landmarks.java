package utility;

import java.util.*;
import graph.*;

public class Landmarks {

    Map<Vertex, Map<Vertex, Double>> to;
    Map<Vertex, Map<Vertex, Double>> from;

    public Landmarks(Map<Vertex, Map<Vertex, Double>> to, Map<Vertex, Map<Vertex, Double>> from){
        this.to = to;
        this.from = from;
    }

    public Map<Vertex, Map<Vertex, Double>> getTo(){
        return to;
    }

    public Map<Vertex, Map<Vertex, Double>> getFrom(){
        return from;
    }
    
}
