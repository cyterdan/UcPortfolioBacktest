package model.allocation;

import java.time.LocalDate;
import java.util.Set;

/**
 *
 * @author cytermann
 */
public interface Allocation {

    public void reset();

    public Set<String> getIsinsForDate(LocalDate date);

    public Double getPositionForDateAndIsin(LocalDate date, String isin);

    public AllocationRebalanceMode getRebalanceMode();

    public double distanceFromInitial(String isin);

    public void updatePositionWithReturn(String isin, Double inDayReturn);

}
