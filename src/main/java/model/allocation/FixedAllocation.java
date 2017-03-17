package model.allocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author cytermann
 */
public class FixedAllocation implements Allocation {

    private final HashMap<String, Double> initialAllocation;
    private HashMap<String, Double> allocation;

    private AllocationRebalanceMode rebalanceMode;

    @Override
    public AllocationRebalanceMode getRebalanceMode() {
        return rebalanceMode;
    }

    @Override
    public String toString() {
        String string = "";
        for(String isin : allocation.keySet()){
            String formatted = String.valueOf(100*(new BigDecimal(allocation.get(isin)).setScale(2,RoundingMode.FLOOR)).doubleValue())+"%";
            string+="|"+isin+":"+formatted;
        }
        
        return string;

    }

    
    
    
    public static FixedAllocation fromHtmlFormData(Map<String, Object> posted, AllocationRebalanceMode rebalanceMode) {
        FixedAllocation ret = new FixedAllocation(rebalanceMode);
        Integer size = Integer.valueOf(posted.get("size").toString());

        Pattern ucpattern = Pattern.compile("[A-Z0-9_]+");
        for (int i = 0; i < size; i++) {
            String ucKey = String.format("data[%d][uc]", i);
            String partKey = String.format("data[%d][part]", i);
            if (posted.containsKey(ucKey) && posted.containsKey(partKey)) {
                String isin = posted.get(ucKey).toString();

                if (!posted.get(partKey).toString().isEmpty()) {

                    Double p = Double.valueOf(posted.get(partKey).toString());
                    if (p > 0 && !isin.isEmpty()) {
                        if (ucpattern.matcher(isin).matches()) {
                            ret.put(isin, p);
                        } else {
                            throw new SecurityException("something fishy is going on...");
                        }
                    }
                }
            }
        }
        return ret;
    }

    public Set<String> getFunds() {
        return allocation.keySet();
    }

    public FixedAllocation(AllocationRebalanceMode rebalanceMode) {
        allocation = new HashMap<>();
        initialAllocation = new HashMap<>();
        this.rebalanceMode = rebalanceMode;
    }

    public boolean isValid() {
        double partSum = allocation.values().stream().mapToDouble(Double::doubleValue).sum();
        return Math.abs(partSum - 1.00) < 0.001;
    }

    public Double get(String isin) {
        return allocation.get(isin);
    }

    public Double put(String isin, Double value) {
        initialAllocation.put(isin, value);
        return allocation.put(isin, value);
    }

    @Override
    public void reset() {
        allocation = new HashMap<>(initialAllocation);
    }

    @Override
    public Set<String> getIsinsForDate(LocalDate date) {
        return allocation.keySet();
    }

    @Override
    public Double getPositionForDateAndIsin(LocalDate date, String isin) {
        return get(isin);
    }

    public void completeWith(String isin) {
        //remplacer les manquants par du cash
        if(allocation.containsKey("_CASH_")){
            System.out.println("");
        }
        Double sumOfFundsInPortfolio = this.allocation.values().stream().mapToDouble(a -> a).sum();
        if (sumOfFundsInPortfolio < 1.0) {
            put(isin, 1 - sumOfFundsInPortfolio);
        }
    }

    @Override
    public double distanceFromInitial(String isin) {
        return Math.abs(initialAllocation.get(isin) - allocation.get(isin));
    }

    @Override
    public void updatePositionWithReturn(String isin, Double inDayReturn) {
        allocation.put(isin, allocation.get(isin) * (1 + inDayReturn));
    }

    public HashMap<String, Double> toAllocationMap() {
        return initialAllocation;
    }

}
