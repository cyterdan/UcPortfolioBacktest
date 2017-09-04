package cyterdan.backtest.comparators;

import java.util.Comparator;
import java.util.Map;
import cyterdan.backtest.model.DailySerie;

/**
 * compares fund sharp ratios
 * @author cytermann
 */
public class SharpRatioComparator implements Comparator<Map.Entry<String, DailySerie>> {

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

        Double sharp1 = o1.getValue().sharpRatio();
        Double sharp2 = o2.getValue().sharpRatio();
        return sharp2.compareTo(sharp1);
    }

}
