package model.allocation;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Variable allocation depending on dates
 * @author cytermann
 */
public class DateBasedAllocation implements Allocation {

    //holds the allocation
    private SortedMap<LocalDate, FixedAllocation> orders = new TreeMap<>();

    @Override
    public AllocationRebalanceMode getRebalanceMode() {
        return AllocationRebalanceMode.REBALANCE_NEVER;
    }

    public DateBasedAllocation() {
        orders = new TreeMap<>();
    }

    @Override
    public Set<String> getIsinsForDate(LocalDate date) {

        SortedMap<LocalDate, FixedAllocation> headMap = orders.headMap(date);
        if (headMap.isEmpty()) {
            return new HashSet<>();
        } else {
            return orders.get(headMap.lastKey()).getFunds();
        }

    }

    public LocalDate firstOrder() {
        return orders.firstKey();
    }

    public LocalDate lastOrder() {
        return orders.lastKey();
    }

    @Override
    public Double getPositionForDateAndIsin(LocalDate date, String isin) {

        LocalDate lastFundDate = orders.headMap(date).lastKey();
        return orders.get(lastFundDate).get(isin);
    }

    /**
     * sets the allocation for the given date
     * @param date
     * @param dateAllocation 
     */
    public void set(LocalDate date, FixedAllocation dateAllocation) {
        orders.put(date, dateAllocation);
    }

    @Override
    public void reset() {
        
        //Could be implemented but useless for now
    }

    @Override
    public double distanceFromInitial(String isin) {
        //no distance since this allocation is not updated with returns
        return 0.0;
    }

    @Override
    public void updatePositionWithReturn(String isin, Double inDayReturn) {
        //nothing again
    }

    /**
     * simple display for debugging purposes
     */
    public void print() {
        for (Map.Entry<LocalDate, FixedAllocation> order : orders.entrySet()) {
            System.out.println(order.getKey() + " : " + order.getValue());
        }
    }

    public Set<LocalDate> dates() {
        return orders.keySet();
    }

}
