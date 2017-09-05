package cyterdan.backtest.experimental;

import cyterdan.backtest.core.data.providers.DataProvider;
import cyterdan.backtest.core.data.providers.H2DataProvider;
import cyterdan.backtest.core.utils.CATEGORIES;
import cyterdan.backtest.core.utils.FUND_CONSTANTS;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import cyterdan.backtest.core.model.DailySerie;
import cyterdan.backtest.core.model.HistoricalData;
import cyterdan.backtest.core.model.Portfolio;
import cyterdan.backtest.core.model.allocation.AllocationRebalanceMode;
import cyterdan.backtest.core.model.allocation.DateBasedAllocation;
import cyterdan.backtest.core.model.allocation.FixedAllocation;

/**
 * find the n funds portfolio with the best sharp ratio in the past
 *
 * @author cytermann
 */
public class FindBestPastAllocation {

    private static final DataProvider dataProvider = new H2DataProvider();

    public static void main(String[] args) throws SQLException {

        int n = 3;
        double maxMonthlyDrawdown = 0.04;

        List<Double> percentages = new ArrayList<>();
        for (double d = 0.05; d < 1; d += 0.05) {
            percentages.add(d);
        }


        Set<String> allIsins = dataProvider.getIsins();

        List<String> inclusions = FUND_CONSTANTS.MES_PLACEMENTS_LIBERTE;

        //garder que certains fonds
        //retirer les scpis et les fonds euro 
        allIsins = allIsins.stream().filter(isin -> inclusions.contains(isin))
                .filter(isin -> !isin.contains("SCPI") && !isin.contains("QU")).collect(Collectors.toSet());

        Map<String, String> categories = CATEGORIES.MPL_CAT;

        Map<String, List<String>> isinsByCategorie = new HashMap<>();

        for (Map.Entry<String, String> entry : categories.entrySet()) {
            String categorie = entry.getValue();
            String isin = entry.getKey();

            if (allIsins.contains(isin)) {
                if (isinsByCategorie.containsKey(categorie)) {
                    isinsByCategorie.get(categorie).add(isin);
                } else {
                    List<String> list = new ArrayList<>();
                    list.add(isin);
                    isinsByCategorie.put(categorie, list);
                }
            }
        }

        HistoricalData data = dataProvider.getDataForIsins(allIsins);

        Map<String, DailySerie> dataByCategorie = new HashMap<>();
        //calculate performance by categorie
        for (Map.Entry<String, List<String>> entry : isinsByCategorie.entrySet()) {
            String categorie = entry.getKey();
            List<String> isins = entry.getValue();
            System.out.println(categorie + " " + isins);
            LocalDate catStart = isins.stream().map(isin -> data.getFundData(isin).firstDate()).min((a, b) -> a.compareTo(b)).get();
            LocalDate catEnd = isins.stream().map(isin -> data.getFundData(isin).latestDate()).max((a, b) -> a.compareTo(b)).get();
            DateBasedAllocation dateBasedAllocation = new DateBasedAllocation();

            for (LocalDate date = catStart; date.isBefore(catEnd); date = date.plusDays(1)) {
                LocalDate finalDate = date;
                List<String> isinsWithData = isins.stream().filter(isin -> data.getFundData(isin).getSerie().containsKey(finalDate)).collect(Collectors.toList());
                FixedAllocation allocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_FIVEPCTDIFF);
                isinsWithData.forEach(isin -> allocation.put(isin, 1.0 / isinsWithData.size()));
                dateBasedAllocation.set(date, allocation);
            }
            Portfolio categorieAveragePortfolio = new Portfolio(dateBasedAllocation);
            dataByCategorie.put(categorie, categorieAveragePortfolio.calculateAllocationPerformance(catStart, catEnd.minusDays(1), data));

        }

        HistoricalData categorieData = new HistoricalData(dataByCategorie);

        Random random = new Random();
        String[] isinsArray = dataByCategorie.keySet().toArray(new String[dataByCategorie.keySet().size()]);

        int arraySize = isinsArray.length;

        long tested = 0;
        double max = 0;
        while (true) {
            tested++;
            List<String> funds = new ArrayList<>();
            while (funds.size() < n) {
                funds.add(isinsArray[random.nextInt(dataByCategorie.keySet().size())]);
            }

            List<Double> parts = randomlySelectPercentages(percentages, random, n);

            FixedAllocation allocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_FIVEPCTDIFF);
            for (int i = 0; i < funds.size(); i++) {
                allocation.put((String) funds.get(i), parts.get(i));
            }

            LocalDate start = funds.stream().map(fund -> categorieData.getFundData(fund).firstDate()).max((a, b) -> a.compareTo(b)).get();
            LocalDate end = funds.stream().map(fund -> categorieData.getFundData(fund).latestDate()).min((a, b) -> a.compareTo(b)).get();

            int nbYears = end.getYear() - start.getYear();
            if (nbYears > 10) {
                Portfolio portfolio = new Portfolio(allocation);
                DailySerie performances = portfolio.calculateAllocationPerformance(start, end, categorieData);

                double maxDrop = performances.maxMonthlyDrawdown();
                //double sharp = (performances.annualReturns() - 0.02) / performances.yearlyVolatility();
                double annualReturn = performances.annualReturns();
                if (maxDrop > -maxMonthlyDrawdown && annualReturn > max) {
                    System.out.println(allocation + " " + annualReturn + " ( " + maxDrop + " ) " + (end.getYear() - start.getYear()) + " years");
                    max = annualReturn;
                    System.out.println("tested " + tested);
                }
            }

        }

    }

    private static List<Double> randomlySelectPercentages(List<Double> options, Random random, int n) {
        //make a copy
        List<Double> tempOptions = new ArrayList<>(options);
        List<Double> percentages = new ArrayList<>();
        //assume options is sorted
        Double step = options.get(0);

        Double total = 0.0;
        for (int i = 0; i < n - 1; i++) {

            //select one percentage randomely
            Double percent = tempOptions.get(random.nextInt(tempOptions.size() / n));
            percentages.add(percent);
            total += percent;
            double finalTotal = total;
            //next reduce the options, removing the one making sum > 1- smallest item
            tempOptions = tempOptions.stream().filter(d -> d <= (1 - finalTotal - step)).collect(Collectors.toList());
        }
        //last one is the complement to 1
        percentages.add(1 - total);
        return percentages;

    }


}
