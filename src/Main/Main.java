package Main;

import com.google.common.base.Stopwatch;

import java.io.*;
import java.math.BigInteger;
import java.util.Properties;

/**
 * Created by Cip on 05-Mar-17.
 */
public class Main {
    private static final String ENCODER_INPUT_FILE = "input.txt";
    private static final String ENCODER_OUTPUT_FILE = "encoded_output.txt";
    private static final String DECODER_INPUT_FILE = ENCODER_OUTPUT_FILE;
    private static final String DECODER_OUTPUT_FILE = "decoded_output.txt";
    private static final String PROPERTIES_FILE = "config.properties";

    private static final Encoder ENCODER;
    private static final Decoder DECODER;

    static {
        Encoder encoder;
        Decoder decoder;
        try {
            encoder = new Encoder();
            decoder = new Decoder();
        } catch (IOException e) {
            encoder = null;
            decoder = null;
        }
        ENCODER = encoder;
        DECODER = decoder;
    }

    public static void main(String[] args) {
        System.out.print("Enter command: ");

        Stopwatch watch;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String command = br.readLine();
            switch (command.trim().toLowerCase()) {
                case "encode":
                    watch = Stopwatch.createStarted();

                    Encoding encoding = ENCODER.encodeMessage(ENCODER_INPUT_FILE);
                    encoding.toFile(ENCODER_OUTPUT_FILE);
                    System.out.println("Encoding time: " + watch);
                    break;

                case "decode":
                    encoding = Encoding.fromFile(DECODER_INPUT_FILE);
                    watch = Stopwatch.createStarted();
                    DECODER.decodeOneInverse(encoding, DECODER_OUTPUT_FILE);
                    System.out.println("Decoding time: " + watch);
                    break;

                case "compare":
                    BigInteger p;
                    Properties props = new Properties();
                    try (InputStream is = new FileInputStream(PROPERTIES_FILE)) {
                        props.load(is);
                        p = new BigInteger(props.getProperty("p"));
                    }
                    encoding = Encoding.fromFile(DECODER_INPUT_FILE);
                    encoding = encoding.createPartialEncoding(encoding.size() - 1);

                    watch = Stopwatch.createStarted();
                    Polynomial.computeFreeCoefficientZeroInverses(encoding, p);
                    System.out.println("Free coefficient computation zero inverses: " + watch);

                    watch = Stopwatch.createStarted();
                    Polynomial.computeFreeCoefficientOneInverse(encoding, p);
                    System.out.println("Free coefficient computation one inverse: " + watch);

                    watch = Stopwatch.createStarted();
                    Polynomial.computeFreeCoefficientKInverses(encoding, p);
                    System.out.println("Free coefficient computation K inverses: " + watch);

                    watch = Stopwatch.createStarted();
                    Polynomial.computeFreeCoefficientMaximumInverses(encoding, p);
                    System.out.println("Free coefficient computation maximum inverses: " + watch);

                default:
                    System.out.println("Invalid command.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
