package simpaths.data;

import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.statistics.IDoubleSource;
import microsim.statistics.regression.MultinomialRegression;
import microsim.statistics.regression.RegressionType;
import org.junit.jupiter.api.Test;
import simpaths.model.BenefitUnit;
import simpaths.model.enums.UnsecuredDebtState;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialWealthRegressionPackageTest {

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
