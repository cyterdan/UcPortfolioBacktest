package comparators;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

/**
 *
 * @author cytermann
 */
public class BestPerformanceComparator implements Comparator<Map.Entry<String, SortedMap<LocalDate, Double>>> {

    @Override
    public int compare(Map.Entry<String, SortedMap<LocalDate, Double>> o1, Map.Entry<String, SortedMap<LocalDate, Double>> o2) {
        if (o1.getValue().isEmpty() && o2.getValue().isEmpty()) {
            return 0;
        } else {
            if (o1.getValue().isEmpty()) {
                return 1;
            } else if (o2.getValue().isEmpty()) {
                return -1;
            }
        }

        LocalDate first1 = o1.getValue().firstKey();
        LocalDate last1 = o1.getValue().lastKey();
        LocalDate first2 = o2.getValue().firstKey();
        LocalDate last2 = o2.getValue().lastKey();
        Double perf1 = (o1.getValue().get(last1) - o1.getValue().get(first1)) / o1.getValue().get(first1);
        Double perf2 = (o2.getValue().get(last2) - o2.getValue().get(first2)) / o2.getValue().get(first2);

        return perf2.compareTo(perf1);

    }

}
