package Main;

import org.apache.commons.lang3.Validate;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Cip on 03-Mar-17.
 */
public class Polynomial {
    private static final byte ZERO = (byte) 0b00000000;
    private static final int REPRESENTATION_SIZE_BITS = 257;
    private static final int BLOCK_SIZE_BYTES = (REPRESENTATION_SIZE_BITS - 1) / (Byte.BYTES * 8);

    private BigInteger p;
    private List<BigInteger> coefficients;

    public Polynomial(BigInteger p, String filename, int block_size) throws IOException {
        this.p = p;
        initializeCoefficientsFromFile(filename, block_size);
    }

    public Polynomial(BigInteger p, List<BigInteger> coefficients) {
        this.p = p;
        this.coefficients = new ArrayList<>(coefficients);

        for (int i = 0; i < coefficients.size(); ++i) {
            coefficients.set(i, coefficients.get(i).mod(p));
        }
    }

    /**
     * Realize that in our particular case, we would need only to verify if the free coefficient is equal
     * to BigInteger.Zero. So, because the polynomial is over Zp, with p-prime, it means that it has no
     * zero divisors, so since the precomputed inverse is not equal to 0 (simply because it is an inverse),
     * it follows naturally that the free coefficient is zero iff the result of this function is equal to zero.
     *
     * @param partialEncoding the k values of the input.
     * @return the free coefficient divided by the inverse of the product of all differences (j-i), with j != i
     * and i, j \in {1,2,., n}.
     */
    public static BigInteger computeFreeCoefficientZeroInverses(Encoding partialEncoding, BigInteger p) {
        if (partialEncoding.size() == 1) {
            return BigInteger.ZERO;
        }

        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < partialEncoding.size(); ++i) {
            BigInteger value = partialEncoding.getValue(i).getValue();
            BigInteger toMultiply = BigInteger.ONE; // double product

            for (int j = 0; j < partialEncoding.size(); ++j) {
                if (i == j) {
                    continue;
                }

                BigInteger point = partialEncoding.getValue(j).getPoint(); // j
                toMultiply = toMultiply.multiply(point).mod(p);

                for (int l = 0; l < partialEncoding.size(); ++l) { // inner product
                    if (l == j) {
                        continue;
                    }

                    // toMultiply *= (l - j);
                    toMultiply = toMultiply.multiply(partialEncoding.getValue(l).getPoint().subtract(point)).mod(p);
                }
            }

            result = result.add(value.multiply(toMultiply).mod(p)).mod(p);
        }

        return result;
    }

    public static BigInteger computeFreeCoefficientOneInverse(Encoding partialEncoding, BigInteger p) {
        // First calculate the inverse of the product of all distinct non-zero differences between point values.
        BigInteger inverse = computeAllDifferencesInverse(partialEncoding, p);
        return inverse.multiply(computeFreeCoefficientZeroInverses(partialEncoding, p)).mod(p);
    }

    public static BigInteger computeFreeCoefficientKInverses(Encoding partialEncoding, BigInteger p) {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < partialEncoding.size(); ++i) {
            BigInteger iPoint = partialEncoding.getValue(i).getPoint();
            BigInteger value = partialEncoding.getValue(i).getValue();
            BigInteger simpleProduct = BigInteger.ONE;
            BigInteger differencesProduct = BigInteger.ONE;

            for (int j = 0; j < partialEncoding.size(); ++j) {
                if (i == j) {
                    continue;
                }
                BigInteger jPoint = partialEncoding.getValue(j).getPoint();

                simpleProduct = simpleProduct.multiply(jPoint).mod(p);
                differencesProduct = differencesProduct.multiply(jPoint.subtract(iPoint)).mod(p);
            }

            BigInteger inverse = differencesProduct.modInverse(p);
            result = result.add(value.multiply(inverse).mod(p).multiply(simpleProduct).mod(p)).mod(p);
        }

        return result;
    }


    public static BigInteger computeFreeCoefficientMaximumInverses(Encoding partialEncoding, BigInteger p) {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < partialEncoding.size(); ++i) {
            BigInteger value = partialEncoding.getValue(i).getValue();
            BigInteger iPoint = partialEncoding.getValue(i).getPoint();
            BigInteger toMultiply = BigInteger.ONE;

            for (int j = 0; j < partialEncoding.size(); ++j) {
                if (i == j) {
                    continue;
                }
                BigInteger jPoint = partialEncoding.getValue(j).getPoint();
                BigInteger inverse = jPoint.subtract(iPoint).modInverse(p);

                toMultiply = toMultiply.multiply(jPoint).mod(p);
                toMultiply = toMultiply.multiply(inverse).mod(p);
            }

            result = result.add(value.multiply(toMultiply).mod(p)).mod(p);
        }

        return result;
    }

    public static Polynomial interpolate(Encoding partialEncoding, BigInteger p) {
        Polynomial result = new Polynomial(p, Arrays.asList(BigInteger.ZERO));

        for (int i = 0; i < partialEncoding.size(); ++i) {
            Polynomial toAdd = new Polynomial(p, Arrays.asList(BigInteger.ONE));
            BigInteger polyCoef = partialEncoding.getValue(i).getValue().mod(p);

            for (int j = 0; j < partialEncoding.size(); ++j) {
                if (i == j) {
                    continue;
                }

                BigInteger jPoint = partialEncoding.getValue(j).getPoint(); // j
                toAdd.multiplyWithSimpleBinomial(jPoint);

                for (int l = 0; l < partialEncoding.size(); ++l) { // inner product
                    if (l == j) {
                        continue;
                    }
                    // toMultiply *= (l - j);
                    polyCoef = polyCoef.multiply(jPoint.subtract(partialEncoding.getValue(l).getPoint())).mod(p);
                }
            }

            toAdd.multiplyWithConstant(polyCoef);
            result.add(toAdd);
        }

        result.multiplyWithConstant(computeAllDifferencesInverse(partialEncoding, p));
        result.eliminateTrailingZeroes();

        Validate.isTrue(result.coefficients.get(result.coefficients.size() - 1).equals(BigInteger.ZERO),
                "Polynomial to interpolate must have free coefficient equal to 0.");

        result.coefficients.remove(result.coefficients.size() - 1);
        return result;
    }

    private static BigInteger computeAllDifferencesInverse(Encoding partialEncoding, BigInteger p) {
        BigInteger allProduct = BigInteger.ONE;
        for (int u = 0; u < partialEncoding.size(); ++u) {
            BigInteger uPoint = partialEncoding.getValue(u).getPoint();

            for (int v = 0; v < partialEncoding.size(); v++) {
                if (u == v) {
                    continue;
                }

                BigInteger vPoint = partialEncoding.getValue(v).getPoint();
                allProduct = allProduct.multiply(vPoint.subtract(uPoint)).mod(p);
            }
        }

        return allProduct.modInverse(p);
    }

    public int getRank() {
        return coefficients.size() - 1;
    }

    /**
     * Method that evaluates the polynomial in an integer point, using Horner's scheme.
     *
     * @param point the point the polynomial is evaluated in.
     * @return the value of the polynomial function in the given point.
     */
    public BigInteger eval(BigInteger point) {
        if (coefficients.size() == 0) {
            return BigInteger.ZERO;
        }

        BigInteger result = new BigInteger(coefficients.get(0).toString());

        for (int i = 1; i < coefficients.size(); i++) {
            result = result.multiply(point);
            result = result.add(coefficients.get(i));
            result = result.mod(p);
        }

        return result.multiply(point).mod(p);
    }

    public void writeCoefficientsToFile(String filename) throws IOException {
        try (OutputStream os = new FileOutputStream(filename)) {
            for (BigInteger coefficient : coefficients) {
                byte[] rep = coefficient.toByteArray();
                int paddingSize = BLOCK_SIZE_BYTES - rep.length;
                paddingSize = paddingSize < 0 ? 0 : paddingSize;
                byte[] padding = new byte[paddingSize];
                Arrays.fill(padding, ZERO);

                os.write(padding);
                os.write(rep);
            }
        }
    }

    private void add(Polynomial polynomial) {
        int maxRank = Math.max(this.getRank(), polynomial.getRank());
        padZeroesToLength(coefficients, maxRank);

        for (int i = maxRank; i >= 0; --i) {
            BigInteger newCoefficient = coefficients.get(i);
            if (polynomial.getRank() >= i) {
                newCoefficient = newCoefficient.add(polynomial.coefficients.get(i)).mod(p);
            }

            coefficients.set(i, newCoefficient);
        }
        eliminateTrailingZeroes();
    }

    private void multiplyWithConstant(BigInteger constant) {
        for (int i = 0; i < coefficients.size(); ++i) {
            coefficients.set(i, coefficients.get(i).multiply(constant).mod(p));
        }
        eliminateTrailingZeroes();
    }

    private void padZeroesToLength(List<BigInteger> coefs, int desiredLength) {
        int toComplete = desiredLength - coefs.size() + 1;
        while (toComplete > 0) {
            coefs.add(BigInteger.ZERO);
            --toComplete;
        }
    }

    /**
     * Method that multiplies the current polynomial with the simple binomial (X - b). It uses that
     * a_i = a_{i-1} - b * a_i, \forall i \in {1, 2, ..., k}, where k is the degree of old polynomial.
     *
     * @param b the number in the expression (X - b).
     */
    private void multiplyWithSimpleBinomial(BigInteger b) {
        BigInteger previousCoefficient = coefficients.get(0);

        for (int i = 1; i < coefficients.size(); ++i) {
            BigInteger toSubstract = b.multiply(previousCoefficient).mod(p);
            previousCoefficient = coefficients.get(i);
            BigInteger newCoefficient = coefficients.get(i).subtract(toSubstract).mod(p);

            coefficients.set(i, newCoefficient);
        }

        BigInteger freeCoefficient = previousCoefficient.multiply(b).mod(p);
        freeCoefficient = freeCoefficient.multiply(new BigInteger("-1")).mod(p);

        coefficients.add(freeCoefficient);
    }

    /**
     * Method that divides the current polynomial by the simple binomial (X - b). It uses that
     * a_i = a_{i-1} - b * a_i, \forall i \in {1, 2, ..., k}, where k is the degree of old polynomial.
     *
     * @param b the number in the expression (X - b).
     */
    private void divideBySimpleBinomial(BigInteger b) {
        if (coefficients.size() < 2) {
            coefficients.clear();
            return;
        }

        BigInteger previousCoefficient = coefficients.get(0);

        for (int i = 1; i < coefficients.size() - 1; ++i) {
            BigInteger toAdd = b.multiply(previousCoefficient).mod(p);
            BigInteger newCoefficient = coefficients.get(i).add(toAdd).mod(p);
            coefficients.set(i, newCoefficient);

            previousCoefficient = newCoefficient;
        }

        coefficients.remove(coefficients.size() - 1);
    }

    private void initializeCoefficientsFromFile(String filename, int block_size) throws IOException {
        this.coefficients = new ArrayList<>();

        try (InputStream is = new FileInputStream(filename)) {
            byte[] coef = new byte[block_size];
            while (is.read(coef, 0, block_size) != -1) {
                coefficients.add(new BigInteger(1, coef));
                Arrays.fill(coef, ZERO);
            }
        }
    }

    private void eliminateTrailingZeroes() {
        while (coefficients.size() > 0 && coefficients.get(0).equals(BigInteger.ZERO)) {
            coefficients.remove(0);
        }
    }

    @Override
    public String toString() {
        return coefficients.toString();
    }

}
