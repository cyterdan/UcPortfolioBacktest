package model;

import java.time.LocalDate;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author cytermann
 */
public class Portfolio extends TreeMap<String, SortedMap<LocalDate, Double>> {

    public Portfolio filterDates(LocalDate from, LocalDate to) {
        return (Portfolio) this.entrySet().stream().collect(Collectors.toMap((o) -> o.getKey(), (o) -> o.getValue().subMap(from, to)));
    }

}
