package comparators;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;
import model.DateBasedSerie;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 *
 * @author cytermann
 */
public class SharpRatioComparator implements Comparator<Map.Entry<String, DateBasedSerie>> {

    @Override
    public int compare(Map.Entry<String, DateBasedSerie> o1, Map.Entry<String, DateBasedSerie> o2) {
        if (o1.getValue().getSerie().isEmpty() && o2.getValue().getSerie().isEmpty()) {
            return 0;
        } else {
            if (o1.getValue().getSerie().isEmpty()) {
                return 1;
            } else if (o2.getValue().getSerie().isEmpty()) {
                return -1;
            }
        }

        Double sharp1 = o1.getValue().sharpRatio();
        Double sharp2 = o2.getValue().sharpRatio();
        return sharp2.compareTo(sharp1);
    }

}
