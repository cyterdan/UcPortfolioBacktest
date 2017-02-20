package com.mycompany.rapidoidtest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.postgresql.ds.PGPoolingDataSource;
import org.rapidoid.http.Req;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;

/**
 *
 * @author cytermann
 */
public class NewMain {

    private static final String STATUS = "status";

    private static final String MESSAGE = "message";

    
    public static double computeStandardDeviation(Number... collection) {
    return Arrays.stream(collection)
                 .map(Number::doubleValue)
                 .collect(DoubleStatistics.collector())
                 .getStandardDeviation();
}
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException {

        PGPoolingDataSource source = new PGPoolingDataSource();
        source.setDataSourceName("detna67j3hmevq");
        source.setServerName("ec2-54-235-120-39.compute-1.amazonaws.com");
        source.setDatabaseName("detna67j3hmevq");
        source.setUser("pmsyfqcauhvlco");
        source.setPassword("9f24a8f2548a78eb3b62e5e0591929f5b2e6e48a119be7b7d0ad20a3f48f5f83");
        source.setMaxConnections(10);
        source.setSsl(true);
        source.setSslfactory("org.postgresql.ssl.NonValidatingFactory");

        String port = System.getenv("PORT");
        if (port != null) {
            On.port(Integer.valueOf(port));
        }

        On.page("/portfolio").mvc(() -> {
            return U.map("count", 12);
        });

        On.post("/backtest").json((Req req) -> {
            Map<String, Integer> porte = extractPortfolio(req.posted());

            Map<String, Object> response = new HashMap<>();

            if (!validatePortfolio(porte)) {
                response.put(STATUS, "ERROR");
                response.put(MESSAGE, "la somme des parts doit être 100%");
            } else {

                response.put(STATUS, "OK");
                Connection conn = null;
                try {
                    conn = source.getConnection();
                    Set<String> isinSet = porte.keySet();
                    String isins = String.join(",", isinSet);
                    ResultSet rs = conn.createStatement().executeQuery(String.format("select date,%s  from funds", isins));
                    Map<String, Map<LocalDate, Double>> data = new HashMap<>();

                    while (rs.next()) {
                        LocalDate date = rs.getDate("date").toLocalDate();
                        for (String isin : isinSet) {

                            Double fundValue = rs.getDouble(isin);
                            if (fundValue != 0) {
                                if (!data.containsKey(isin)) {
                                    data.put(isin, new HashMap<>());
                                }
                                data.get(isin).put(date, fundValue);
                            }

                        }

                    }

                    LocalDate minDate = isinSet.stream()
                            .map(isin -> Collections.min(data.get(isin).keySet()))
                            .min((o2, o1) -> o1.compareTo(o2)).get();

                    LocalDate maxDate = isinSet.stream()
                            .map(isin -> Collections.max(data.get(isin).keySet()))
                            .max((o1, o2) -> o1.compareTo(o2)).get();

                    //on commence un jour plus tard que le min pour calculer la première perf
                    LocalDate d = minDate.plusDays(1);
                    
                    //on initialise le capital avec 100.0 à la date min
                    SortedMap<LocalDate, Double> K = new TreeMap<>();
              
                    K.put(minDate, 1.0);
                    Map<String, Map<LocalDate, Double>> sim = new HashMap<>();
                    for (String isin : isinSet) {
                        sim.put(isin, new HashMap<>());
                        sim.get(isin).put(minDate, porte.get(isin).doubleValue() / 100);
                    }

                    while (maxDate.isAfter(d)) {
                        LocalDate last = d.minusDays(1);
                        //System.out.println(d);
                        double perfGlobale = 0.0;
                        double totalParts = 0.0;
                        for (String isin : isinSet) {
                            //data.get(isin).get(Date.valueOf("2011-08-30").toLocalDate())
                            double rapport = sim.get(isin).get(last);
                            double perf = (data.get(isin).get(d) - data.get(isin).get(last))
                                    / data.get(isin).get(last);
                            perfGlobale+=perf*rapport;
                            double part = sim.get(isin).get(last)*(1+perf);
                            totalParts+=part;
                            sim.get(isin).put(d, part);
                        }
                        for(String isin : isinSet){
                            double part = sim.get(isin).get(d);
                            sim.get(isin).put(d,part/totalParts);
                        }
                        
                        K.put(d, K.get(last)*(1+perfGlobale));

                        d = d.plusDays(1);

                    }
                    
                    int startYear = minDate.getYear();
                    int endYear = maxDate.getYear();
                    
                    StandardDeviation stdev = new StandardDeviation();
                    
                    List<Double> stdevs = new ArrayList<>();
                    SortedMap<Integer,Double> perfAnnual = new TreeMap<>();
                    for(int year=startYear;year<=endYear;year++){
                        SortedMap<LocalDate, Double> yearK = K.subMap(LocalDate.of(year, Month.JANUARY, 1), LocalDate.of(year, Month.DECEMBER, 31));
                        double perf = (yearK.get(yearK.lastKey())-yearK.get(yearK.firstKey()))/yearK.get(yearK.lastKey());
                        
                        double[] values=yearK.values().stream().mapToDouble(x->x).toArray();
                        
                        stdev.clear();
                        double std = stdev.evaluate(values);
                        stdevs.add(std);
                        System.out.println(year+" [ perf : "+perf*100+" std :"+ std);
                        perfAnnual.put(year, std);
                    }
                   
                    double perfGlobal = (K.get(K.lastKey())-K.get(K.firstKey()))/K.get(K.firstKey())*100;
                    double stdGlobal =stdevs.stream().mapToDouble(a->a).sum()/stdevs.size();
                    System.out.println("perf globale : "+perfGlobal+" std : "+stdGlobal);
                    
                    response.put("history",K);
                    response.put("perf",perfGlobal);
                    response.put("std", stdGlobal);
                       
                    
                    
                    
                    //System.out.println(K);
                    
                    

                    response.put(MESSAGE, String.format("Les fonds de ce portefeuil ont des données entre %s et %s ", minDate.toString(), maxDate.toString()));

                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                        }
                    }
                }

            }

            return response;

        });
    }

    private static Map<String, Integer> extractPortfolio(Map<String, Object> posted) {
        Map<String, Integer> ret = new HashMap<>();
        Integer size = Integer.valueOf(posted.get("size").toString());

        for (int i = 0; i < size; i++) {
            String ucKey = String.format("data[%d][uc]", i);
            String partKey = String.format("data[%d][part]", i);
            if (posted.containsKey(ucKey) && posted.containsKey(partKey)) {
                ret.put(posted.get(ucKey).toString(), Integer.valueOf(posted.get(partKey).toString()));
            }
        }
        return ret;
    }

    private static boolean validatePortfolio(Map<String, Integer> porte) {
        return porte.values().stream().mapToInt(Integer::intValue).sum() == 100;
    }

}
