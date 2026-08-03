package simpaths.data;

import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.statistics.IDoubleSource;
import microsim.statistics.regression.MultinomialRegression;
import microsim.statistics.regression.RegressionType;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.junit.jupiter.api.Test;
import simpaths.model.BenefitUnit;
import simpaths.model.enums.UnsecuredDebtState;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialWealthRegressionPackageTest {

    @Test
    void loadsJointContinuingDebtModelsAndInnovationRmse() {
        MultiKeyCoefficientMap lowCostNew = ExcelAssistant.loadCoefficientMap(
                "input/reg_wealth_financial.xlsx", "FW2b", 1);
        MultiKeyCoefficientMap lowCost = ExcelAssistant.loadCoefficientMap(
                "input/reg_wealth_financial.xlsx", "FW2c", 1);
        MultiKeyCoefficientMap highCostNew = ExcelAssistant.loadCoefficientMap(
                "input/reg_wealth_financial.xlsx", "FW2d", 1);
        MultiKeyCoefficientMap highCost = ExcelAssistant.loadCoefficientMap(
                "input/reg_wealth_financial.xlsx", "FW2e", 1);
        MultiKeyCoefficientMap rmse = ExcelAssistant.loadCoefficientMap(
                "input/reg_RMSE.xlsx", "UK", 1);

        assertEquals(36, lowCostNew.size());
        assertEquals(37, lowCostNew.getValuesNames().length);
        assertEquals(37, lowCost.size());
        assertEquals(38, lowCost.getValuesNames().length);
        assertEquals(36, highCostNew.size());
        assertEquals(37, highCostNew.getValuesNames().length);
        assertEquals(37, highCost.size());
        assertEquals(38, highCost.getValuesNames().length);

        assertEquals(
                0.4762894896988582,
                ((Number) lowCost.getValue(
                        "LowCostDebtPersistence", "COEFFICIENT")).doubleValue(),
                1.0e-12);
        assertEquals(
                0.0017944601103354,
                ((Number) lowCost.getValue(
                        "LowCostDebtPersistence",
                        "LowCostDebtPersistence")).doubleValue(),
                1.0e-12);
        assertEquals(
                -0.001649330408284,
                ((Number) lowCost.getValue(
                        "Constant", "LowCostDebtPersistence")).doubleValue(),
                1.0e-12);
        assertEquals(
                1.13574141020715,
                ((Number) rmse.getValue("FW2c")).doubleValue(),
                1.0e-12);

        assertEquals(
                0.700917378850978,
                ((Number) highCost.getValue(
                        "HighCostDebtPersistence", "COEFFICIENT")).doubleValue(),
                1.0e-12);
        assertEquals(
                0.0004992590014949,
                ((Number) highCost.getValue(
                        "HighCostDebtPersistence",
                        "HighCostDebtPersistence")).doubleValue(),
                1.0e-12);
        assertEquals(
                0.0031980211306322,
                ((Number) highCost.getValue(
                        "Constant", "HighCostDebtPersistence")).doubleValue(),
                1.0e-12);
        assertEquals(
                0.9720760974457268,
                ((Number) rmse.getValue("FW2e")).doubleValue(),
                1.0e-12);

        for (MultiKeyCoefficientMap model :
                new MultiKeyCoefficientMap[]{lowCostNew, lowCost, highCostNew, highCost}) {
            assertNull(model.getValue("Employed"));
            assertNull(model.getValue("Graduate"));
            assertNull(model.getValue("YdsesC52"));
            assertNull(model.getValue("YdsesC53"));
            assertNull(model.getValue("YdsesC54"));
            assertNull(model.getValue("YdsesC55"));
            assertNotNull(model.getValue("ReferencePersonEmployed"));
            assertNotNull(model.getValue("ReferencePersonGraduate"));
            assertNotNull(model.getValue("PrivateIncomeQuintile2"));
            assertNotNull(model.getValue("PrivateIncomeQuintile3"));
            assertNotNull(model.getValue("PrivateIncomeQuintile4"));
            assertNotNull(model.getValue("PrivateIncomeQuintile5"));
        }
    }

    @Test
    void mapsPrivateIncomeDecilesToAmountModelQuintiles() {
        BenefitUnit unit = new BenefitUnit(true);
        BenefitUnit.Variables[] indicators = {
                BenefitUnit.Variables.PrivateIncomeQuintile2,
                BenefitUnit.Variables.PrivateIncomeQuintile3,
                BenefitUnit.Variables.PrivateIncomeQuintile4,
                BenefitUnit.Variables.PrivateIncomeQuintile5
        };

        for (int decile = 1; decile <= 10; decile++) {
            unit.setPrivateIncomeDecile(decile);
            int expectedQuintile = (decile + 1) / 2;
            double activeIndicators = 0.0;
            for (int i = 0; i < indicators.length; i++) {
                double expected = expectedQuintile == i + 2 ? 1.0 : 0.0;
                double actual = unit.getDoubleValue(indicators[i]);
                assertEquals(expected, actual, 0.0);
                activeIndicators += actual;
            }
            assertEquals(expectedQuintile == 1 ? 0.0 : 1.0, activeIndicators, 0.0);
        }
    }

    @Test
    void covarianceMatricesRemainBootstrapCompatible() {
        for (String sheet : new String[]{"FW2a", "FW2b", "FW2c", "FW2d", "FW2e"}) {
            MultiKeyCoefficientMap model = ExcelAssistant.loadCoefficientMap(
                    "input/reg_wealth_financial.xlsx", sheet, 1);
            String[] parameters = Arrays.stream(model.getValuesNames())
                    .filter(name -> !"COEFFICIENT".equals(name))
                    .toArray(String[]::new);
            assertEquals(model.size(), parameters.length);

            double[][] covariance = new double[parameters.length][parameters.length];
            for (int row = 0; row < parameters.length; row++) {
                for (int column = 0; column < parameters.length; column++) {
                    covariance[row][column] = ((Number) model.getValue(
                            parameters[row], parameters[column])).doubleValue();
                }
            }
            assertDoesNotThrow(() -> new CholeskyDecomposition(
                    new Array2DRowRealMatrix(covariance)));
        }
    }

    @Test
    void reproducesStataGoldenProfileAndAnnualisesIt() {
        MultiKeyCoefficientMap coefficients = ExcelAssistant.loadCoefficientMap(
                "input/reg_wealth_financial.xlsx", "FW2a", 1);
        assertEquals(153, coefficients.size());
        assertEquals(154, coefficients.getValuesNames().length);
        MultinomialRegression<UnsecuredDebtState> regression = new MultinomialRegression<>(
                RegressionType.MultinomialLogit, UnsecuredDebtState.class, coefficients);

        double[][] expectedBiennial = {
                {0.9289152750686686, 0.0375788796791568, 0.0290670305787651, 0.0044388146734098},
                {0.7297591449284719, 0.2100378827636843, 0.0332500935269914, 0.0269528787808524},
                {0.7199702066655629, 0.0492145647300665, 0.1978465343623100, 0.0329686942420606},
                {0.5973338825162532, 0.1517747026271160, 0.1449488509571624, 0.1059425638994684}
        };
        UnsecuredDebtState[] states = UnsecuredDebtState.values();
        double[][] biennial = new double[states.length][states.length];
        for (UnsecuredDebtState lagState : states) {
            Map<UnsecuredDebtState, Double> probabilities = regression.getProbabilities(
                    new StataGoldenProfile(lagState, 2019), BenefitUnit.Variables.class);
            double rowSum = 0.0;
            for (UnsecuredDebtState currentState : states) {
                double probability = probabilities.get(currentState);
                assertTrue(probability >= 0.0 && probability <= 1.0);
                assertEquals(
                        expectedBiennial[lagState.getValue()][currentState.getValue()],
                        probability,
                        1.0e-12);
                biennial[lagState.getValue()][currentState.getValue()] = probability;
                rowSum += probability;
            }
            assertEquals(1.0, rowSum, 1.0e-12);
        }

        double[][] annual = TransitionMatrixAnnualiser.annualiseBiennial(biennial);
        for (double[] row : annual) {
            double rowSum = 0.0;
            for (double probability : row) {
                assertTrue(probability >= 0.0 && probability <= 1.0);
                rowSum += probability;
            }
            assertEquals(1.0, rowSum, 1.0e-12);
        }
    }

    private record StataGoldenProfile(UnsecuredDebtState lagState, int year) implements IDoubleSource {

        @Override
        public double getDoubleValue(Enum<?> variableID) {
            if (BenefitUnit.Variables.Constant.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.LagUnsecuredDebtLowCostOnly.equals(variableID))
                return UnsecuredDebtState.LowCostOnly.equals(lagState) ? 1.0 : 0.0;
            if (BenefitUnit.Variables.LagUnsecuredDebtHighCostOnly.equals(variableID))
                return UnsecuredDebtState.HighCostOnly.equals(lagState) ? 1.0 : 0.0;
            if (BenefitUnit.Variables.LagUnsecuredDebtMixed.equals(variableID))
                return UnsecuredDebtState.Mixed.equals(lagState) ? 1.0 : 0.0;
            if (BenefitUnit.Variables.HomeOwnedOutrightCurrent.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.NetFinancialWealthDecile9.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.PrivateIncomeDecile4.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.Age50plus.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.NumberMembersOver17.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.UKJ.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.ReferencePersonGraduate.equals(variableID)) return 1.0;
            if (BenefitUnit.Variables.Year2019.equals(variableID)) return year == 2019 ? 1.0 : 0.0;
            if (BenefitUnit.Variables.Year2020.equals(variableID)) return year == 2020 ? 1.0 : 0.0;
            if (BenefitUnit.Variables.Year2021.equals(variableID)) return year == 2021 ? 1.0 : 0.0;
            if (BenefitUnit.Variables.Year2022.equals(variableID)) return year == 2022 ? 1.0 : 0.0;
            return 0.0;
        }
    }
}
