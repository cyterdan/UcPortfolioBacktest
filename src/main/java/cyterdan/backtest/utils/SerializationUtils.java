package cyterdan.backtest.utils;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Used to handle portfolio data (de/)serialization
 * @author cytermann
 */
public class SerializationUtils {

    /**
     * object->json->gzip->base64 encore->string
     * @param object
     * @return
     * @throws IOException 
     */
    public static String serialize(Object object) throws IOException {
        ByteArrayOutputStream byteaOut = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = null;
        try {
            gzipOut = new GZIPOutputStream(Base64.getEncoder().wrap(byteaOut));
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

    /**
     * string-> base64 decode-> ungzip -> json -> T
     * @param <T>
     * @param string
     * @param type
     * @return
     * @throws IOException 
     */
    public static <T> T deserialize(String string, Type type) throws IOException {
        ByteArrayOutputStream byteaOut = new ByteArrayOutputStream();
        GZIPInputStream gzipIn = null;
        try {
            gzipIn = new GZIPInputStream(Base64.getDecoder().wrap(new ByteArrayInputStream(string.getBytes("UTF-8"))));
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

}
