package comparators;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import model.DailySerie;
import model.HistoricalData;

/**
 * compare data moving average
 * @author cytermann
 */
public class MovingAverageRatioComparator implements Comparator<Map.Entry<String, DailySerie>> {

    private final HistoricalData history;
    
    private final long qty;
    
    private final ChronoUnit unit;

    public MovingAverageRatioComparator(HistoricalData history, long qty, ChronoUnit unit) {
        this.history = history;
        this.qty = qty;
        this.unit = unit;
    }

    
    

    
    
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
        Double avg1 = history.getFundAveragePeriodicalReturnBetween(
                o1.getKey(), 
                unit,
                qty,
                history.getFundData(o1.getKey()).firstDate(),
                o1.getValue().firstDate()
        );
        
        
        Double perf2 = o2.getValue().totalReturn();
        
        Double avg2 = history.getFundAveragePeriodicalReturnBetween(
                o2.getKey(), 
                unit,
                qty,
                history.getFundData(o2.getKey()).firstDate(),
                o2.getValue().firstDate()
        );
        
        
        if(avg1 ==null && avg2 == null){
            return 0;
        }
        else{
            if(avg1 == null){
                return 1;
            }
            else if(avg2 == null){
                return -1;
            }
        }
        
        return Double.valueOf(perf2/avg2).compareTo(perf1/avg1);

    }

}
