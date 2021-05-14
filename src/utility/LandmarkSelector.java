package utility;

import java.util.*;
import graph.*;

public class LandmarkSelector {
    double INF_DIST = Double.MIN_VALUE;

    Collection<Vertex> activeLandmarks;

    private Map<Vertex, Map<Vertex, Double>> distanceToLandmark;
    private Map<Vertex, Map<Vertex, Double>> distanceFromLandmark;

    Landmarks allLandmarks;

    public LandmarkSelector(Graph g, int noOfLandmarks, int landmarkSelectionType){
        // TODO enum
        if (landmarkSelectionType == 0) {
            allLandmarks = GraphUtils.randomLandmarks(g, noOfLandmarks);
        } else if (landmarkSelectionType == 1){
            allLandmarks = GraphUtils.farthestLandmarks(g, noOfLandmarks);
        } else if (landmarkSelectionType == 2){
            allLandmarks = GraphUtils.PartionCorner(g, noOfLandmarks);
            System.out.println(allLandmarks.getFrom().keySet().size());
            System.out.println(allLandmarks.getTo().keySet().size());
        } else {
            throw new RuntimeException("Please provide valid landmark selection strategy");
        }

        distanceToLandmark = allLandmarks.getTo();
        distanceFromLandmark = allLandmarks.getFrom();

        activeLandmarks = new HashSet<>();

    }

    public void setAllLandmarks(){
        activeLandmarks = distanceFromLandmark.keySet();
    }
    
    public void resetLandmarks(){
        activeLandmarks = new HashSet<>();
    }

    public boolean updateLandmarks(Vertex curr, Vertex goal, int numberToAdd){
        ArrayList<Pair> pairs = new ArrayList<>();

        for (Vertex l : distanceFromLandmark.keySet()) {
            Map<Vertex, Double> distTo = distanceToLandmark.get(l);
            Map<Vertex, Double> distFrom = distanceFromLandmark.get(l);

            if (! distTo.containsKey(curr)
             || ! distFrom.containsKey(curr)) {
                 // This node either cannot reach l, or cannot be reached by l.
                 // So skip l, since the calculations wouldn't make sense.
                continue;
             }
            if (! distTo.containsKey(goal)
             || ! distFrom.containsKey(goal)){
                 continue;
             }

            // pi^l+ := dist(v, l) - dist(t, l)
            double dist_vl = distTo.get(curr);
            double dist_tl = distTo.get(goal);
            double pi_plus = dist_vl - dist_tl;

            // pi^l- := dist(l, t) - dist(l, v)
            double dist_lt = distFrom.get(goal);
            double dist_lv = distFrom.get(curr);
            double pi_minus = dist_lt - dist_lv;

            //System.out.println(dist_vl + "\n" + dist_tl + "\n" + dist_lt + "\n" + dist_lv + "\n\n");

            pairs.add(new Pair(l, Math.max(pi_plus, pi_minus)));
        }

        Collections.sort(pairs);
        Collections.reverse(pairs);
        //TODO:  keep all "old" landmarks, or update to only the best ones each time?

        int sizeBefore = activeLandmarks.size();
        pairs.stream()
             .limit(numberToAdd)                // Takes first n things
             .map(p -> p.v)                     // Maps (v,dist) -> v 
             .forEach(activeLandmarks :: add);  // add to set
        int sizeAfter = activeLandmarks.size();

        return sizeAfter != sizeBefore;
    }

    public double pi(Vertex curr, Vertex goal) {

        double max = 0.0; // TODO maybe -inf
        // http://www-or.amp.i.kyoto-u.ac.jp/members/ohshima/Paper/MThesis/MThesis.pdf
        for (Vertex l : activeLandmarks) {
            Map<Vertex, Double> distTo = distanceToLandmark.get(l);
            Map<Vertex, Double> distFrom = distanceFromLandmark.get(l);

            if (! distTo.containsKey(curr)
             || ! distFrom.containsKey(curr)) {
                 // This node either cannot reach l, or cannot be reached by l.
                 // So skip l, since the calculations wouldn't make sense.
                continue;
             }
            if (! distTo.containsKey(goal)
             || ! distFrom.containsKey(goal)){
                 continue;
             }

            // pi^l+ := dist(v, l) - dist(t, l)
            double dist_vl = distTo.get(curr);
            double dist_tl = distTo.get(goal);
            double pi_plus = dist_vl - dist_tl;

            // pi^l- := dist(l, t) - dist(l, v)
            double dist_lt = distFrom.get(goal);
            double dist_lv = distFrom.get(curr);
            double pi_minus = dist_lt - dist_lv;

            //System.out.println(dist_vl + "\n" + dist_tl + "\n" + dist_lt + "\n" + dist_lv + "\n\n");

            max = Math.max(max, Math.max(pi_plus, pi_minus));
        }
        return max;
    }
    
    public Collection<Vertex> getAllLandmarks(){
        return distanceToLandmark.keySet();
    }

    public Collection<Vertex> getActiveLandmarks(){
        return activeLandmarks;
    }

}
