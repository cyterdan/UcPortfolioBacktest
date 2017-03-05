
package model.allocation;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author cytermann
 */
public class DateBasedAllocation implements Allocation{
    
    private SortedMap<LocalDate, FixedAllocation> orders = new TreeMap<>();

    @Override
    public AllocationRebalanceMode getRebalanceMode() {
        return AllocationRebalanceMode.REBALANCE_NEVER;
    }

    
    
    public DateBasedAllocation() {
        orders = new TreeMap<>();
    }
    
    @Override
    public Set<String> getIsinsForDate(LocalDate date){
        LocalDate lastFundDate = orders.headMap(date).lastKey();

        return orders.get(lastFundDate).getFunds();
    }
    
    public LocalDate firstOrder(){
        return orders.firstKey();
    }
    
    public LocalDate lastOrder(){
        return orders.lastKey();
    }
    
    @Override
    public Double getPositionForDateAndIsin(LocalDate date,String isin){
        
        LocalDate lastFundDate = orders.headMap(date).lastKey();
        return orders.get(lastFundDate).get(isin);
    }

    public void set(LocalDate tuesday, FixedAllocation tuesdayAllocation) {
        orders.put(tuesday, tuesdayAllocation);
    }

    @Override
    public void reset() {
        //nothing happens here :)
    }

    @Override
    public double distanceFromInitial(String isin) {
      return 0.0;
    }

    @Override
    public void updatePositionWithReturn(String isin, Double inDayReturn) {
       //nothing again
    }
    
    
    
}
