package life.slide.app;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created by Michael on 12/22/2014.
 */
public class SlideServices {
    public static Future<Boolean> postData(Map<String, String> fields, Request request) {
        return new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //do something
                return true;
            }
        });
    }

    public static Map<String, String> encrypt(Map<String, String> fields, String pem) {
        Map<String, String> ret = new HashMap<String, String>();

        PublicKey publicKey = publicKeyFromString(pem);
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            for (String key : fields.keySet()) {
                String item = fields.get(key);
                byte[] cipherData = cipher.doFinal(item.getBytes());
                String encodedItem = Base64.encodeToString(cipherData, Base64.DEFAULT);
                ret.put(key, encodedItem);
            }

            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static PublicKey publicKeyFromString(String pem) {
        String finalPem = pem.
                replace("-----BEGIN PUBLIC KEY-----\n", "").
                replace("-----END PUBLIC KEY-----", "");
        byte[] decoded = Base64.decode(finalPem, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception nsa) {
            nsa.printStackTrace();
        }

        return null;
    }
}
