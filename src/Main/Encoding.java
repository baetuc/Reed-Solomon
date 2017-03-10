package Main;

import org.apache.commons.lang3.Validate;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Cip on 04-Mar-17.
 */
public class Encoding {
    private static final String DELIMITER = "\n";
    private List<PointValuePair> polynomialValues;

    public Encoding() {
        this.polynomialValues = new ArrayList<>();
    }

    public void addValue(BigInteger point, BigInteger value) {
        polynomialValues.add(new PointValuePair(point, value));
    }

    public void addValue(PointValuePair pair) {
        polynomialValues.add(pair);
    }

    public Encoding createPartialEncoding(int partialSize) {
        Validate.isTrue(0 <= partialSize && partialSize < polynomialValues.size(), "Partial size invalid: %d.", partialSize);

        Collections.shuffle(polynomialValues);
        Encoding partial = new Encoding();
        partial.polynomialValues = polynomialValues.subList(0, partialSize);

        return partial;
    }

    public PointValuePair getValue(int index) {
        Validate.isTrue(0 <= index && index < polynomialValues.size(), "Index out of bounds: %d.", index);

        return polynomialValues.get(index);
    }

    public static Encoding fromFile(String fileName) throws IOException {
        Encoding encoding = new Encoding();

        try (BufferedReader is = new BufferedReader(new FileReader(fileName))) {
            String pair;

            while ((pair = is.readLine()) != null) {
                encoding.addValue(PointValuePair.deserialize(pair));
            }
        }

        return encoding;
    }

    public int size() {
        return polynomialValues.size();
    }

    public void toFile(String filename) throws IOException {
        try (OutputStream os = new FileOutputStream(filename)) {
            for (PointValuePair polynomialValue : polynomialValues) {
                os.write(polynomialValue.toString().getBytes());
                os.write(DELIMITER.getBytes());
            }
        }
    }

    public static class PointValuePair {
        private static final String DELIMITER = "\t:\t";

        private BigInteger point;
        private BigInteger value;

        public PointValuePair(BigInteger point, BigInteger value) {
            this.point = point;
            this.value = value;
        }

        private static PointValuePair deserialize(String serializedPair) {
            String[] info = serializedPair.split(DELIMITER);
            Validate.isTrue(info.length == 2, "Point value pair wrong format.");
            return new PointValuePair(new BigInteger(info[0]), new BigInteger(info[1]));
        }

        public BigInteger getPoint() {
            return point;
        }

        public BigInteger getValue() {
            return value;
        }

        @Override
        public String toString() {
            return point + DELIMITER + value;
        }
    }
}
