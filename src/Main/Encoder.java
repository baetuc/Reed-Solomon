package Main;

import com.google.common.annotations.VisibleForTesting;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;

/**
 * Created by Cip on 03-Mar-17.
 */
public class Encoder {
    private static final String PROPERTIES_FILE = "config.properties";
    private static final int REPRESENTATION_SIZE_BITS = 257;
    private static final int BLOCK_SIZE_BYTES = (REPRESENTATION_SIZE_BITS - 1) / (Byte.BYTES * 8);
    private static final int S = 1;
    // OBSERVATION: BLOCK_SIZE_BITS MUST be a multiple of 8 + 1, this property is vital in the implementation

    private BigInteger p;

    public Encoder() throws IOException {
        initializeP();
    }

    public Encoding encodeMessage(String filename) throws IOException {
        Polynomial poly = new Polynomial(p, filename, BLOCK_SIZE_BYTES);
        Encoding encoding = new Encoding();
        BigInteger point = BigInteger.ONE;

        for (int i = 0; i < poly.getRank() + 2 * S + 2; ++i) {
            encoding.addValue(point, poly.eval(point));
            point = point.add(BigInteger.ONE);
        }

        return encoding;
    }


    private void initializeP() throws IOException {
        if (!Files.exists(Paths.get(PROPERTIES_FILE))) {
            generateP();
        } else {
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(PROPERTIES_FILE)) {
                props.load(is);
                this.p = new BigInteger(props.getProperty("p"));
            }
        }
    }

    private void generateP() throws IOException {
        this.p = BigInteger.probablePrime(REPRESENTATION_SIZE_BITS, new Random());
        Properties props = new Properties();
        try (OutputStream os = new FileOutputStream(PROPERTIES_FILE)) {
            props.setProperty("p", this.p.toString());
            props.store(os, null);
        }
    }


}
