package model.allocation;

/**
 * Rebalancing modes for an allocation
 * @author cytermann
 */
public enum AllocationRebalanceMode {

    //never rebalance
    REBALANCE_NEVER(0),
    //rebalance every 2 weeks
    REBALANCE_EVERY2WEEKS(1),
    //rebalance once the allocation diverges more than 5% of the initial allocation
    REBALANCE_FIVEPCTDIFF(2);

    /*integer representation*/
    private final int mode;

    //private constructor
    private AllocationRebalanceMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
    
    
    /**
     * return the rebalancing mode from the integer representation
     * @param mode
     * @return 
     */
    public static AllocationRebalanceMode fromInteger(int mode){
        for (AllocationRebalanceMode value : values()) {
            if (value.getMode() == mode) {
                return value;
            }
        }
        throw new NoSuchFieldError("No such rebalance mode");
    }

}
