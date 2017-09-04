package cyterdan.backtest.comparators;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import cyterdan.backtest.model.DailySerie;

/**
 * compares performances
 * @author cytermann
 */
public class BestPerformanceComparator implements Comparator<Map.Entry<String, DailySerie>> {

    @Override
    public int compare(Map.Entry<String, DailySerie> o1, Map.Entry<String, DailySerie> o2) {
        if (o1.getValue().getSerie().isEmpty() && o2.getValue().getSerie().isEmpty()) {
            return 0;
        } else {
            if (o1.getValue().getSerie().isEmpty()) {
                return 1;
            } else if (o2.getValue().getSerie().isEmpty()) {
                return -1;
            }
        }
        Double perf1 = o1.getValue().totalReturn();
        Double perf2 = o2.getValue().totalReturn();

        return perf1.compareTo(perf2);

    }

}
