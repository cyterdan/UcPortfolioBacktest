package cyterdan.backtest.model.allocation;

import java.time.LocalDate;
import java.util.Set;

/**
 * Common interface for portfolio allocations
 * @author cytermann
 */
public interface Allocation {

    /**
     * reset the allocation to it's initial state
     */
    public void reset();

    /**
     * set of funds used in allocation for a given date
     * @param date
     * @return 
     */
    public Set<String> getIsinsForDate(LocalDate date);

    /**
     * return the position (min : 0%, max 100%) for a given date and security
     * @param date
     * @param isin
     * @return 
     */
    public Double getPositionForDateAndIsin(LocalDate date, String isin);

    /**
     * return the portfolio's allocation rebalancing mode
     * @return 
     */
    public AllocationRebalanceMode getRebalanceMode();

    /**
     * calculate the distance between the initial allocation and the current allocaion
     * @param isin
     * @return 
     */
    public double distanceFromInitial(String isin);

    /**
     * updates the allocation using a return
     * @param isin
     * @param inDayReturn 
     */
    public void updatePositionWithReturn(String isin, Double inDayReturn);

}
