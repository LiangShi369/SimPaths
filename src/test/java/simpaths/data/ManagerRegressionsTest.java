package simpaths.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagerRegressionsTest {

    @Test
    void recoversAnnualBinaryProbabilities() {
        double biennialEntry = 0.20;
        double biennialPersistence = 0.80;

        double[] annual = ManagerRegressions.recoverAnnualEntryPersistence(biennialEntry, biennialPersistence);
        double[][] annualMatrix = {
                {1.0 - annual[0], annual[0]},
                {1.0 - annual[1], annual[1]}
        };
        double[][] reconstructed = multiply(annualMatrix, annualMatrix);

        assertEquals(biennialEntry, reconstructed[0][1], 1.0e-12);
        assertEquals(biennialPersistence, reconstructed[1][1], 1.0e-12);
    }

    @Test
    void rejectsInadmissibleBinaryPair() {
        assertThrows(ArithmeticException.class,
                () -> ManagerRegressions.recoverAnnualEntryPersistence(0.80, 0.20));
    }

    private double[][] multiply(double[][] first, double[][] second) {
        int size = first.length;
        double[][] result = new double[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                for (int inner = 0; inner < size; inner++) {
                    result[row][column] += first[row][inner] * second[inner][column];
                }
            }
        }
        return result;
    }
}
