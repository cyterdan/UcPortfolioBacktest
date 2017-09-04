package cyterdan.backtest.app.experimental;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import cyterdan.backtest.utils.FUND_CONSTANTS;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cyterdan.backtest.model.DailySerie;
import cyterdan.backtest.model.HistoricalData;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 *
 * @author cytermann
 */
public class FindAntiCorrelations {

    private static final DataProvider dataProvider = new H2DataProvider();

    public static void main(String[] args) throws SQLException {
        Set<String> allIsins = dataProvider.getIsins();

        List<String> inclusions = FUND_CONSTANTS.DEGIRO_ZERO;

        //garder que certains fonds
        //retirer les scpis et les fonds euro 
        allIsins = allIsins.stream().filter(isin -> inclusions.contains(isin))
                .filter(isin -> !isin.contains("SCPI") && !isin.contains("QU")).collect(Collectors.toSet());

        HistoricalData data = dataProvider.getDataForIsins(allIsins);

        double minCorrelation = 0;
        for (String isin : allIsins) {
            //check the most anti correlated fund
            DailySerie thisFund = data.getFundData(isin);
            for (String other : allIsins) {
                DailySerie otherFund = data.getFundData(other);
                LocalDate comparaisonStart = thisFund.firstDate().isBefore(otherFund.firstDate()) ? otherFund.firstDate() : thisFund.firstDate();
                LocalDate comparaisonEnd = thisFund.latestDate().isAfter(otherFund.latestDate()) ? otherFund.latestDate() : thisFund.latestDate();
                if (comparaisonEnd.isAfter(comparaisonStart)) {
                    Collection<Double> thisValues = thisFund.extract(comparaisonStart, comparaisonEnd).getSerie().values();
                    Collection<Double> otherValues = otherFund.extract(comparaisonStart, comparaisonEnd).getSerie().values();
                    double[] thisValuesArray = thisValues.stream().mapToDouble(d -> d).toArray();
                    double[] otherValuesArray = otherValues.stream().mapToDouble(d -> d).toArray();
                    if (thisValuesArray.length != otherValuesArray.length) {
                        System.err.println("probleme here, the two arrays are not the same size");
                    } else {
                        double correlation = new PearsonsCorrelation().correlation(thisValuesArray, otherValuesArray);
                        if(correlation<minCorrelation){
                            minCorrelation = correlation;
                            System.out.println(isin + " vs " + other + " : " + correlation);
                        }

                    }

                }

            }
        }
    }

}
