package model;

import comparators.BestPerformanceComparator;
import comparators.SharpRatioComparator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

/**
 *
 * @author cytermann
 */
public enum MomentumStrategy {

    BEST_PERFORMANCE,
    BEST_SHARP;

    public Comparator<Map.Entry<String, SortedMap<LocalDate, Double>>>
            getComparator() {
        switch (this) {
            case BEST_PERFORMANCE:
                return new BestPerformanceComparator();
            case BEST_SHARP:
                return new SharpRatioComparator();
            default:
                return new BestPerformanceComparator();
        }

    }

}
