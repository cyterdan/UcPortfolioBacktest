
package com.mycompany.rapidoidtest;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cytermann
 */
public class FeedDatabase {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
     
        //setup new database
        
        
        
        CSVReader reader = null;
        Map<String,Map<Date,Double>> data = new HashMap<>();
        try {
            reader = new CSVReader(new FileReader( "/home/cytermann/Code/python/opcvmHistory/allFundsDataT.csv"),'|');
            String[] line;
            String[] firstLine = reader.readNext();
            while ((line = reader.readNext()) != null) {
                //System.out.println("data [isin= " + line[0] + ", date= " + line[1] + " , value=" + line[2] + "]");
                Date date = Date.valueOf(line[0]);
                for(int j=1;j<line.length;j++){
                    String isin = firstLine[j];
                    Double value = null;
                    if(!line[j].isEmpty()){
                        value = Double.valueOf(line[j]);
                    }
                    try{
                        data.get(isin).put(date, value);
                    }catch(NullPointerException npe){
                      data.put(isin, new HashMap<>());
                      data.get(isin).put(date, value);
                    }
                }
                
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }    
        System.out.println(data);
    }
    
}
