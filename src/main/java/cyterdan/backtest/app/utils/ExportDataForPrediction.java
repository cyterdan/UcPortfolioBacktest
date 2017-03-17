package cyterdan.backtest.app.utils;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map.Entry;
import java.util.Set;
import model.DateBasedSerie;
import model.HistoricalData;

/**
 *
 * @author cytermann
 */
public class ExportDataForPrediction {

    private static DataProvider dataProvider = new H2DataProvider();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException, IOException {

        Set<String> isins = dataProvider.getIsins();
        HistoricalData data = dataProvider.getDataForIsins(isins);

        /*
         t |   x1   |   x2   |   x3   | y
         d   p1(d-1)   p2(d-1) p3(d-1)  py(d)
         */
        for (String isin : isins) {

            HistoricalData outputData = new HistoricalData();

            LocalDate firstDate = data.getFundData(isin).firstDate();
            LocalDate lastDate = data.getFundData(isin).latestDate();
            HistoricalData subData = data.subData(firstDate, lastDate);

            int qty =4;
            ChronoUnit unit = ChronoUnit.WEEKS;
            for (LocalDate date = firstDate.plus(qty,unit); date.isBefore(lastDate.minus(qty,unit).minusDays(1)); date = date.plus(qty,unit)) {
                for (Entry<String, DateBasedSerie> entry : subData.series()) {
                    Double perf;
                    if (!entry.getKey().equals(isin)) {
                        perf = entry.getValue().extractReturn(date.minus(qty,unit), date);

                    } else {
                        perf = entry.getValue().extractReturn(date, date.plus(qty,unit));
                    }
                    outputData.putForAt(entry.getKey(), date, perf);

                }

            }
            outputData.toCsv("/home/cytermann/Code/python/predictOpcvm/data/" + isin + ".csv");

        }

    }

}
