package model.allocation;

/**
 *
 * @author cytermann
 */
public enum AllocationRebalanceMode {

    REBALANCE_NEVER(0),
    REBALANCE_EVERY2WEEKS(1),
    REBALANCE_FIVEPCTDIFF(2);

    private final int mode;

    private AllocationRebalanceMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
    
    
    
    public static AllocationRebalanceMode fromInteger(int mode){
        for (AllocationRebalanceMode value : values()) {
            if (value.getMode() == mode) {
                return value;
            }
        }
        throw new NoSuchFieldError("No such rebalance mode");
    }

}
