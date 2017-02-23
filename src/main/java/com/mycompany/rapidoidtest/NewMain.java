package com.mycompany.rapidoidtest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.json.JSONObject;
import org.postgresql.ds.PGPoolingDataSource;
import org.rapidoid.http.Req;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import org.rapidoid.util.Msc;

/**
 *
 * @author cytermann
 */
public class NewMain {

    private static final String STATUS = "status";

    private static final String MESSAGE = "message";

    private static final String PRESET = "preset";

    private static final String DATASOURCE_NAME = "detna67j3hmevq";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException {

        PGPoolingDataSource source = PGPoolingDataSource.getDataSource(DATASOURCE_NAME);
        if (source == null) {
            source = new PGPoolingDataSource();
            source.setDataSourceName(DATASOURCE_NAME);

            source.setServerName("ec2-54-235-120-39.compute-1.amazonaws.com");
            source.setDatabaseName("detna67j3hmevq");
            source.setUser("pmsyfqcauhvlco");
            source.setPassword("9f24a8f2548a78eb3b62e5e0591929f5b2e6e48a119be7b7d0ad20a3f48f5f83");
            source.setMaxConnections(10);
            source.setSsl(true);
            source.setSslfactory("org.postgresql.ssl.NonValidatingFactory");
            source.setInitialConnections(3);
            source.initialize();

        }

        String port = System.getenv("PORT");
        if (port != null) {
            On.port(Integer.valueOf(port));
        }

        On.page("/portfolio").mvc((req, resp) -> {

            if (req.params().containsKey(PRESET)) {
                System.out.println(req.param(PRESET));
                
                Map<String, Double> deserialize = deserialize(req.param(PRESET), new TypeToken<Map<String,Double>>(){}.getType());
                List<List> list= new ArrayList<>();
                for(Entry<String,Double> entry : deserialize.entrySet()){
                    List<String> subList = new ArrayList<>();
                    subList.add("'"+entry.getKey()+"'");
                    subList.add(entry.getValue().toString());
                list.add(subList);
                }
                
                return U.map("preset",Base64.encodeBase64String(list.toString().replace("'", "\"").getBytes()));
            } else {
                return U.map("count", 12);
            }
        });

        On.post("/backtest").json((Req req) -> {
            Map<String, Double> porte = extractPortfolio(req.posted());

            Map<String, Object> response = new HashMap<>();

            if (!validatePortfolio(porte)) {
                response.put(STATUS, "ERROR");
                response.put(MESSAGE, "la somme des parts doit être 100%");
            } else {

                response.put(STATUS, "OK");

                Set<String> isinSet = porte.keySet();
                Map<String, SortedMap<LocalDate, Double>> data = getDataForIsins(isinSet);

                Set<String> referenceIsin = new HashSet<>();

                String refIsin = "FR0010315770";
                referenceIsin.add(refIsin);

                SortedMap<LocalDate, Double> referenceData = getDataForIsins(referenceIsin).get(refIsin);

                LocalDate minDate = isinSet.stream()
                        .map(isin -> data.get(isin).firstKey())
                        .max((o1, o2) -> o1.compareTo(o2)).get();

                LocalDate maxDate = isinSet.stream()
                        .map(isin -> data.get(isin).lastKey())
                        .min((o1, o2) -> o1.compareTo(o2)).get();

                //on commence un jour plus tard que le min pour calculer la première perf
                LocalDate d = minDate.plusDays(1);

                //on initialise le capital avec 100.0 à la date min
                SortedMap<LocalDate, Double> K = new TreeMap<>();

                K.put(minDate, 1.0);
                Map<String, Map<LocalDate, Double>> sim = new HashMap<>();
                for (String isin : isinSet) {
                    sim.put(isin, new HashMap<>());
                    sim.get(isin).put(minDate, porte.get(isin).doubleValue());
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
                        perfGlobale += perf * rapport;
                        double part = sim.get(isin).get(last) * (1 + perf);
                        totalParts += part;
                        sim.get(isin).put(d, part);
                    }
                    for (String isin : isinSet) {
                        double part = sim.get(isin).get(d);
                        sim.get(isin).put(d, part / totalParts);
                    }

                    K.put(d, K.get(last) * (1 + perfGlobale));

                    d = d.plusDays(1);

                }

                int startYear = minDate.getYear();
                //si on commence pas en janvier, on commence l'année d'après
                if (!K.firstKey().getMonth().equals(Month.JANUARY)) {
                    startYear++;
                }
                int endYear = maxDate.getYear();

                StandardDeviation stdev = new StandardDeviation();

                List<Double> stdevs = new ArrayList<>();
                SortedMap<Integer, Double> perfAnnual = new TreeMap<>();
                for (int year = startYear; year <= endYear; year++) {
                    SortedMap<LocalDate, Double> yearK = K.subMap(LocalDate.of(year, Month.JANUARY, 1), LocalDate.of(year, Month.DECEMBER, 31));
                    double perf = (yearK.get(yearK.lastKey()) - yearK.get(yearK.firstKey())) / yearK.get(yearK.firstKey());

                    double[] values = yearK.values().stream().mapToDouble(x -> x).toArray();

                    stdev.clear();
                    double std = stdev.evaluate(values);
                    stdevs.add(std);
                    System.out.println(year + " [ perf : " + perf * 100 + " std :" + std);
                    perfAnnual.put(year, perf);
                }

                double perfGlobal = (K.get(K.lastKey()) - K.get(K.firstKey())) / K.get(K.firstKey()) * 100 / (endYear-startYear+1);
                double meanPerf = perfAnnual.values().stream().mapToDouble(a->a).sum()/perfAnnual.size() * 100;
                double stdGlobal = stdevs.stream().mapToDouble(a -> a).sum() / stdevs.size() * 100;
                System.out.println("perf globale : " + perfGlobal + " std : " + stdGlobal);
                List<List> history = formatAsJsArray(K);

                //retirer l'historique inutile de la réference
                referenceData = referenceData.subMap(minDate, maxDate);
                List<List> reference = formatAsJsArray(referenceData);

                response.put("history", history);
                response.put("reference", reference);

                BigDecimal formattedPerf = BigDecimal.valueOf(meanPerf);
                formattedPerf.setScale(2, RoundingMode.FLOOR);
                response.put("perf", formattedPerf);
                BigDecimal formattedStd = BigDecimal.valueOf(stdGlobal);
                formattedStd.setScale(2, RoundingMode.FLOOR);
                response.put("std", formattedStd);

                //System.out.println(K);
                System.out.println("before : "+serialize(porte));
                System.out.println("after : "+Base64.encodeBase64URLSafeString(serialize(porte).getBytes("utf-8")));
                response.put(MESSAGE, String.format("Les fonds de ce portefeuil ont des données entre %s et %s ", minDate.toString(), maxDate.toString()));
                response.put("permalink", "/portfolio?preset="+Msc.urlEncode(serialize(porte)));
               
            }

            return response;

        });
    }

    private static List<List> formatAsJsArray(SortedMap<LocalDate, Double> K) {
        double normal = K.get(K.firstKey());
        //formater l'historique
        List<List> history = new ArrayList<>();
        for (Entry<LocalDate, Double> entry : K.entrySet()) {
            List<Object> list = new ArrayList<>();
            list.add(entry.getKey().getYear());
            list.add(entry.getKey().getMonthValue());
            list.add(entry.getKey().getDayOfMonth());

            list.add(entry.getValue() / normal);
            history.add(list);
        }
        return history;
    }

    public static String serialize(Object object) throws IOException {
        ByteArrayOutputStream byteaOut = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = null;
        try {
            gzipOut = new GZIPOutputStream(new Base64OutputStream(byteaOut));
            gzipOut.write(new Gson().toJson(object).getBytes("UTF-8"));
        } finally {
            if (gzipOut != null) {
                try {
                    gzipOut.close();
                } catch (IOException logOrIgnore) {
                }
            }
        }
        return new String(byteaOut.toByteArray());
    }

    public static <T> T deserialize(String string, Type type) throws IOException {
        ByteArrayOutputStream byteaOut = new ByteArrayOutputStream();
        GZIPInputStream gzipIn = null;
        try {
            gzipIn = new GZIPInputStream(new Base64InputStream(new ByteArrayInputStream(string.getBytes("UTF-8"))));
            for (int data; (data = gzipIn.read()) > -1;) {
                byteaOut.write(data);
            }
        } finally {
            if (gzipIn != null) {
                try {
                    gzipIn.close();
                } catch (IOException logOrIgnore) {
                }
            }
        }
        return new Gson().fromJson(new String(byteaOut.toByteArray()), type);
    }

    private static Map<String, Double> extractPortfolio(Map<String, Object> posted) {
        Map<String, Double> ret = new HashMap<>();
        Integer size = Integer.valueOf(posted.get("size").toString());

        for (int i = 0; i < size; i++) {
            String ucKey = String.format("data[%d][uc]", i);
            String partKey = String.format("data[%d][part]", i);
            if (posted.containsKey(ucKey) && posted.containsKey(partKey)) {
                String isin = posted.get(ucKey).toString();
                if (!posted.get(partKey).toString().isEmpty()) {
                    Double p = Double.valueOf(posted.get(partKey).toString());
                    if (p > 0 && !isin.isEmpty()) {
                        ret.put(isin, p);
                    }
                }
            }
        }
        return ret;
    }

    private static boolean validatePortfolio(Map<String, Double> porte) {
        return Math.abs(porte.values().stream().mapToDouble(Double::doubleValue).sum() - 1.00) < 0.001;
    }

    private static Map<String, SortedMap<LocalDate, Double>>
            getDataForIsins(Set<String> isinSet) throws SQLException {
        Connection conn = null;
        Map<String, SortedMap<LocalDate, Double>> data = new HashMap<>();
        try {
            conn = PGPoolingDataSource.getDataSource(DATASOURCE_NAME).getConnection();
            String isins = String.join(",", isinSet);
            ResultSet rs = conn.createStatement().executeQuery(String.format("select date,%s  from funds", isins));

            while (rs.next()) {
                LocalDate date = rs.getDate("date").toLocalDate();
                for (String isin : isinSet) {

                    Double fundValue = rs.getDouble(isin);
                    if (fundValue != 0) {
                        if (!data.containsKey(isin)) {
                            data.put(isin, new TreeMap<>());
                        }
                        data.get(isin).put(date, fundValue);
                    }

                }

            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
        return data;

    }

}
