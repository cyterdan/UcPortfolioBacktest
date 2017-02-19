package com.mycompany.rapidoidtest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.rapidoid.http.Req;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import org.h2.jdbcx.JdbcConnectionPool;
import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;

/**
 *
 * @author cytermann
 */
public class NewMain {

    private static final String STATUS = "status";

    private static final String MESSAGE = "message";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException {

        String port = System.getenv("PORT");
        if (port != null) {
            On.port(Integer.valueOf(port));
        }

        On.page("/portfolio").mvc(() -> {
            return U.map("count", 12);
        });

        On.post("/backtest").json((Req req) -> {
            Map<String, Integer> porte = extractPortfolio(req.posted());

            Map<String, String> response = new HashMap<>();

            if (!validatePortfolio(porte)) {
                response.put(STATUS, "ERROR");
                response.put(MESSAGE, "la somme des parts doit être 100%");
            } else {

                response.put(STATUS, STATUS);
                response.put(MESSAGE, MESSAGE);

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
