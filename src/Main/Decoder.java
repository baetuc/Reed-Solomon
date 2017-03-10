package Main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;

/**
 * Created by Cip on 05-Mar-17.
 */
public class Decoder {
    private static final String PROPERTIES_FILE = "config.properties";
    private static final int REPRESENTATION_SIZE = 257;
    private static final int BLOCK_SIZE = (REPRESENTATION_SIZE - 1) / Byte.BYTES;
    private static final int S = 1;

    private BigInteger p;

    public Decoder() throws IOException {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(PROPERTIES_FILE)) {
            props.load(is);
            this.p = new BigInteger(props.getProperty("p"));
        }
    }

    public void decodeOneInverse(Encoding encodedMessage, String filename) throws IOException {
        int K = encodedMessage.size() - 2 * S;
        long left_tries = Long.MAX_VALUE;

        while (left_tries > 0) {
            Encoding partialEncoding = encodedMessage.createPartialEncoding(K);
            if (Polynomial.computeFreeCoefficientZeroInverses(partialEncoding, p).equals(BigInteger.ZERO)) {
                Polynomial reconstructed = Polynomial.interpolate(partialEncoding, p);
                reconstructed.writeCoefficientsToFile(filename);
                return;

            }
            --left_tries;
        }

        System.out.println("Did not find a valid polynomial. Decoding failed!");
    }

}
