package cyterdan.backtest.core.model;

import cyterdan.backtest.core.comparators.BestPerformanceComparator;
import cyterdan.backtest.core.comparators.MovingAverageRatioComparator;
import cyterdan.backtest.core.comparators.SharpRatioComparator;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author cytermann
 */
public enum MomentumStrategy {

    BEST_PERFORMANCE,
    WORST_PERFORMANCE,
    BEST_SHARP,
    WORST_SHARP,
    RANDOM,
    BEST_PERFORMANCE_RATIO;

    public Comparator<Map.Entry<String, DailySerie>>
            getComparator(HistoricalData data, long qty, ChronoUnit unit) {
        switch (this) {
            case BEST_PERFORMANCE:
                return new BestPerformanceComparator();
            case BEST_SHARP:
                return new SharpRatioComparator();
            case WORST_PERFORMANCE:
                return new BestPerformanceComparator().reversed();
            case WORST_SHARP:
                return new SharpRatioComparator().reversed();
            case RANDOM:
                return shuffle();
            case BEST_PERFORMANCE_RATIO:
                return new MovingAverageRatioComparator(data, qty, unit).reversed();
            default:
                return new BestPerformanceComparator();
        }

    }

    public Double getStatistic(String isin, DailySerie serie, HistoricalData data, long qty, ChronoUnit unit) {
        switch (this) {
            case BEST_PERFORMANCE:
                return serie.totalReturn();
            case BEST_SHARP:
                return serie.volatility();
            case WORST_PERFORMANCE:
                return -serie.totalReturn();
            case WORST_SHARP:
                return -serie.volatility();
            case RANDOM:
                return serie.totalReturn();
            case BEST_PERFORMANCE_RATIO:
                Double average = data.getFundAveragePeriodicalReturnBetween(
                        isin,
                        unit,
                        qty,
                        data.getFundData(isin).firstDate(),
                        serie.firstDate()
                );
                if(average == null){
                    return 0.0;
                }
                return serie.totalReturn() / data.getFundAveragePeriodicalReturnBetween(
                        isin,
                        unit,
                        qty,
                        data.getFundData(isin).firstDate(),
                        serie.firstDate()
                );
            default:
                return serie.totalReturn();
        }
    }

    public static <T> Comparator<T> shuffle() {
        final Map<Object, UUID> uniqueIds = new IdentityHashMap<>();
        return (e1, e2) -> {
            final UUID id1 = uniqueIds.computeIfAbsent(e1, k -> UUID.randomUUID());
            final UUID id2 = uniqueIds.computeIfAbsent(e2, k -> UUID.randomUUID());
            return id1.compareTo(id2);
        };
    }

}
