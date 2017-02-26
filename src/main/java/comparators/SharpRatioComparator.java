package comparators;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 *
 * @author cytermann
 */
public class SharpRatioComparator implements Comparator<Map.Entry<String, SortedMap<LocalDate, Double>>> {

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

        double std1 = getStd(o1.getValue().values());
        double std2 = getStd(o2.getValue().values());

        Double sharp1 = perf1 - 0.02 / std1;
        Double sharp2 = perf2 - 0.02 / std2;
        return sharp2.compareTo(sharp1);
    }

    private static double getStd(Collection<Double> v) {
        List<Double> values = v.stream().collect(Collectors.toList());
        double[] doubleValues = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            doubleValues[i] = values.get(i);
        }
        return new StandardDeviation().evaluate(doubleValues);
    }
}
