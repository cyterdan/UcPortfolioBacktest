package cyterdan.backtest.app.experimental;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import model.DailySerie;
import model.HistoricalData;
import model.Portfolio;
import model.allocation.AllocationRebalanceMode;
import model.allocation.DateBasedAllocation;
import model.allocation.FixedAllocation;
import quickml.data.AttributesMap;
import quickml.data.instances.RegressionInstance;
import quickml.supervised.ensembles.randomForest.randomRegressionForest.RandomRegressionForest;
import quickml.supervised.ensembles.randomForest.randomRegressionForest.RandomRegressionForestBuilder;
import quickml.supervised.tree.regressionTree.RegressionTreeBuilder;

/**
 * try to use machine learning (RandomRegressionForest) to predict 
 * a funds evolution based on other funds historical data => does not work !
 * 
 * @author cytermann
 */
public class MachineLearningAllocation {

    private static final DataProvider dataProvider = new H2DataProvider();

    public static Double doMLA(String isin, HistoricalData data) {
        List<RegressionInstance> trainingData = new ArrayList<>();

        LocalDate firstDate = data.getFundData(isin).firstDate();
        LocalDate lastDate = data.getFundData(isin).latestDate();
        LocalDate midDate = firstDate.plusDays((long) ((lastDate.toEpochDay() - firstDate.toEpochDay()) * 0.9));

        HistoricalData subData = data.subData(firstDate, lastDate);

        final int qty = 2;
        final ChronoUnit unit = ChronoUnit.WEEKS;
        //train on first half
        for (LocalDate date = firstDate.plus(qty, unit).plusWeeks(5); date.isBefore(midDate.minus(qty, unit).minusDays(1)); date = date.plus(qty, unit)) {

            final AttributesMap features = new AttributesMap();
            final LocalDate finalDate = date;
            //features
            features.putAll(
                    subData.series().stream()
                    .filter(entry -> !entry.getKey().equals(isin))
                    .collect(Collectors.toMap(
                                    entry -> entry.getKey(), entry -> entry.getValue().extractReturn(finalDate.minus(qty, unit), finalDate)
                            ))
            );

            //previous 5 weeks performances
            features.putAll(
                    Arrays.asList(1, 2, 3, 4, 5).stream()
                    .map(w -> finalDate.minusWeeks(w))
                    .collect(Collectors.toMap(d -> d.toString(), d -> subData.getFundData(isin).extractReturn(d, finalDate)))
            );
            //target
            Double target = subData.series().stream()
                    .filter(entry -> entry.getKey().equals(isin))
                    .map(entry -> entry.getValue().extractReturn(finalDate, finalDate.plus(qty, unit)))
                    .findFirst().get();

            RegressionInstance instance = new RegressionInstance(features, target);
            trainingData.add(instance);

        }

        RandomRegressionForest forest = new RandomRegressionForestBuilder<>(new RegressionTreeBuilder<>()
        )//.maxDepth(5))
                //.numTrees(200)
                .buildPredictiveModel(trainingData);

        //backtest on second half
        DateBasedAllocation allocation = new DateBasedAllocation();
        int goodDecisionCounter = 0;
        for (LocalDate date = midDate.plus(qty, unit); date.isBefore(lastDate.minus(qty, unit).minusDays(1)); date = date.plus(qty, unit)) {
            final AttributesMap features = new AttributesMap();
            final LocalDate finalDate = date;
            //features
            features.putAll(
                    subData.series().stream()
                    .filter(entry -> !entry.getKey().equals(isin))
                    .collect(Collectors.toMap(
                                    entry -> entry.getKey(), entry -> entry.getValue().extractReturn(finalDate.minus(qty, unit), finalDate)
                            ))
            );

            Double prediction = forest.predict(features);

            Double actual = subData.getFundData(isin).extractReturn(date, date.plus(qty, unit));
            FixedAllocation dayAllocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);
            if (prediction > 0.0) {
                if (actual > 0.0) {
                    goodDecisionCounter++;
                } else {
                    //System.err.println(date+" : dropped "+actual*100);
                    goodDecisionCounter--;
                }
                dayAllocation.put(isin, 1.0);
            } else {
                //dayAllocation.put("FR0010411884",1.0);
                dayAllocation.put(HistoricalData.CASH, 1.0);
                if (actual < 0.0) {
                    goodDecisionCounter++;
                    //System.out.println(date+" : avoided drop of"+actual*100 );
                } else {
                    goodDecisionCounter--;
                }
            }
            allocation.set(date, dayAllocation);

        }
        Portfolio portfolio = new Portfolio(allocation);
        DailySerie results = portfolio.calculateAllocationPerformance(firstDate, lastDate, data);
        System.out.println(firstDate + " to " + lastDate);
        Double buyAndHold = data.subData(firstDate, lastDate).getFundData(isin).annualReturns();

        System.out.println(results.annualReturns() + " vs " + buyAndHold);
        /* System.out.println(results.yearlyVolatility());
         System.out.println(results.totalReturn() * 100);
         */
        double good = 100 * (float) (goodDecisionCounter) / allocation.dates().size();
        if (good > 30) {
            System.err.println(isin + "| r=" + results.annualReturns() + " | Good decision : " + good + " %");
        }

        return results.annualReturns();
    }

    public static void main(String[] args) throws SQLException {

        Set<String> isins = dataProvider.getIsins();
        HistoricalData data = dataProvider.getDataForIsins(isins);

        data.excludeNonDailyFunds();

        //String isin = "FR0010342592";
        for (String isin : Arrays.asList("LU0304955437", "FR0011869387", "FR0011466093", "FR0011558246")) {
            try {
                doMLA(isin, data);
            } catch (Exception e) {
                e.printStackTrace();
                //ignore
            }

        }

    }

}
