package utility;

import java.util.*;

public class DistComparator implements Comparator<Pair> {

    public long comparisons = 0;

    @Override
    public int compare(Pair p1, Pair p2) {
        this.comparisons++;
        return Double.compare(p1.dist, p2.dist);
    }

    public long getComparisons() {
        return this.comparisons;
    }
    
}