package simpaths.model.benefitunit;

import microsim.statistics.IDoubleSource;
import microsim.statistics.regression.LinearRegression;
import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.data.TransitionMatrixAnnualiser;
import simpaths.model.BenefitUnit;
import simpaths.model.enums.TimeVaryingRate;
import simpaths.model.enums.UnsecuredDebtState;

import java.util.Map;

public class WealthFinancial {

    private double wealthFinancialAssetsValue;              // value of financial assets before unsecured debts
    private double wealthUnsecuredDebtLowValue;             // low-cost unsecured debt
    private double wealthUnsecuredDebtHighValue;            // high-cost unsecured debt
    private double wealthUnsecuredDebtLowInnovation;        // persistent residual state for low-cost unsecured debt
    private double wealthUnsecuredDebtHighInnovation;       // persistent residual state for high-cost unsecured debt
    private double yWealthFinancialReturnYear;              // net return to financial assets/debts during year
    private UnsecuredDebtState unsecuredDebtState;


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthFinancial() {
        wealthFinancialAssetsValue = 0.0;
        wealthUnsecuredDebtLowValue = 0.0;
        wealthUnsecuredDebtHighValue = 0.0;
        wealthUnsecuredDebtLowInnovation = 0.0;
        wealthUnsecuredDebtHighInnovation = 0.0;
        yWealthFinancialReturnYear = 0.0;
        unsecuredDebtState = UnsecuredDebtState.None;
    }

    public WealthFinancial(WealthFinancial original) {
        wealthFinancialAssetsValue = original.wealthFinancialAssetsValue;
        wealthUnsecuredDebtLowValue = original.wealthUnsecuredDebtLowValue;
        wealthUnsecuredDebtHighValue = original.wealthUnsecuredDebtHighValue;
        wealthUnsecuredDebtLowInnovation = original.wealthUnsecuredDebtLowInnovation;
        wealthUnsecuredDebtHighInnovation = original.wealthUnsecuredDebtHighInnovation;
        yWealthFinancialReturnYear = original.yWealthFinancialReturnYear;
        unsecuredDebtState = original.unsecuredDebtState;
    }

    public WealthFinancial(double value) {
        this();
        setValue(value);
    }

    public WealthFinancial(double value, double lowCostDebt, double highCostDebt) {
        this();
        wealthUnsecuredDebtLowValue = positiveOrZero(lowCostDebt);
        wealthUnsecuredDebtHighValue = positiveOrZero(highCostDebt);
        unsecuredDebtState = getUnsecuredDebtStateFromValues();
        setValue(value);
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectIncomeAnnual(int year) {
        // projects returns - see BenefitUnit.setInvestmentIncomeAnnual()
        double returnsAssets = wealthFinancialAssetsValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealSavingReturn);
        double costsLowDebt = wealthUnsecuredDebtLowValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealDebtCostLow);
        double costsHighDebt = wealthUnsecuredDebtHighValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealDebtCostHigh);
        yWealthFinancialReturnYear = returnsAssets - costsLowDebt - costsHighDebt;
        return yWealthFinancialReturnYear;
    }

    public void projectUnsecuredDebt(BenefitUnit benefitUnit, WealthFinancial wealthFinancialL1,
                                     double innovUnsecuredDebtState,
                                     double innovLowDebtValue,
                                     double innovHighDebtValue) {
        // Net financial wealth was established in the population-wide prepare
        // phase, before current-year wealth deciles were assigned.
        double netFinancialWealth = getValue();
        unsecuredDebtState = drawUnsecuredDebtState(benefitUnit, wealthFinancialL1.getUnsecuredDebtState(), innovUnsecuredDebtState);

        wealthUnsecuredDebtLowValue = 0.0;
        wealthUnsecuredDebtHighValue = 0.0;
        wealthUnsecuredDebtLowInnovation = 0.0;
        wealthUnsecuredDebtHighInnovation = 0.0;

        if (unsecuredDebtState.hasLowCostDebt()) {
            boolean continuing = wealthFinancialL1.hasLowCostDebt();
            String regression = continuing ? "FW2c" : "FW2b";
            LinearRegression regressionModel = continuing ? Parameters.getRegFW2c() : Parameters.getRegFW2b();
            double score = regressionModel.getScore(benefitUnit, BenefitUnit.Variables.class);
            double shock = getInnovation(regression, innovLowDebtValue);
            double transformedValue = score + shock;
            if (continuing) {
                double persistence = Parameters.getRegFW2c().getCoefficient("LowCostDebtPersistence");
                wealthUnsecuredDebtLowInnovation = persistence * wealthFinancialL1.getWealthUnsecuredDebtLowInnovation() + shock;
            } else {
                double continuationScore = Parameters.getRegFW2c().getScore(benefitUnit, BenefitUnit.Variables.class);
                wealthUnsecuredDebtLowInnovation = transformedValue - continuationScore;
            }
            wealthUnsecuredDebtLowValue = Math.max(0.0, Math.sinh(transformedValue));
            if (!Parameters.isFinite(wealthUnsecuredDebtLowValue))
                throw new RuntimeException("projection for low-cost unsecured debt value is not finite"
                        + " for benefit unit " + benefitUnit.getId()
                        + ", regression " + regression
                        + ", netFinancialWealth " + netFinancialWealth
                        + ", score " + score
                        + ", randomDraw " + innovLowDebtValue
                        + ", shock " + shock
                        + ", transformed value " + transformedValue);
        }

        if (unsecuredDebtState.hasHighCostDebt()) {
            boolean continuing = wealthFinancialL1.hasHighCostDebt();
            String regression = continuing ? "FW2e" : "FW2d";
            LinearRegression regressionModel = continuing ? Parameters.getRegFW2e() : Parameters.getRegFW2d();
            double score = regressionModel.getScore(benefitUnit, BenefitUnit.Variables.class);
            double shock = getInnovation(regression, innovHighDebtValue);
            double transformedValue = score + shock;
            if (continuing) {
                double persistence = Parameters.getRegFW2e().getCoefficient("HighCostDebtPersistence");
                wealthUnsecuredDebtHighInnovation = persistence * wealthFinancialL1.getWealthUnsecuredDebtHighInnovation() + shock;
            } else {
                double continuationScore = Parameters.getRegFW2e().getScore(benefitUnit, BenefitUnit.Variables.class);
                wealthUnsecuredDebtHighInnovation = transformedValue - continuationScore;
            }
            wealthUnsecuredDebtHighValue = Math.max(0.0, Math.sinh(transformedValue));
            if (!Parameters.isFinite(wealthUnsecuredDebtHighValue))
                throw new RuntimeException("projection for high-cost unsecured debt value is not finite"
                        + " for benefit unit " + benefitUnit.getId()
                        + ", regression " + regression
                        + ", netFinancialWealth " + netFinancialWealth
                        + ", score " + score
                        + ", randomDraw " + innovHighDebtValue
                        + ", shock " + shock
                        + ", transformed value " + transformedValue);
        }

        wealthFinancialAssetsValue = netFinancialWealth + wealthUnsecuredDebtLowValue + wealthUnsecuredDebtHighValue;
    }

    private UnsecuredDebtState drawUnsecuredDebtState(BenefitUnit benefitUnit, UnsecuredDebtState lagState, double randomDraw) {
        UnsecuredDebtState[] states = UnsecuredDebtState.values();
        double[][] biennialTransition = new double[states.length][states.length];
        for (UnsecuredDebtState assumedLagState : states) {
            IDoubleSource source = new CounterfactualDebtStateSource(benefitUnit, assumedLagState);
            Map<UnsecuredDebtState, Double> probabilities = ManagerRegressions.getProbabilities(
                    source, BenefitUnit.Variables.class, RegressionName.WealthFinancialFW2a);
            for (UnsecuredDebtState currentState : states) {
                biennialTransition[assumedLagState.getValue()][currentState.getValue()] = probabilities.get(currentState);
            }
        }
        double[][] annualTransition = TransitionMatrixAnnualiser.annualiseBiennial(biennialTransition);

        double cumulative = 0.0;
        for (UnsecuredDebtState state : states) {
            cumulative += annualTransition[lagState.getValue()][state.getValue()];
            if (randomDraw < cumulative) {
                return state;
            }
        }
        return UnsecuredDebtState.Mixed;
    }

    private static final class CounterfactualDebtStateSource implements IDoubleSource {

        private final BenefitUnit benefitUnit;
        private final UnsecuredDebtState assumedLagState;

        private CounterfactualDebtStateSource(BenefitUnit benefitUnit, UnsecuredDebtState assumedLagState) {
            this.benefitUnit = benefitUnit;
            this.assumedLagState = assumedLagState;
        }

        @Override
        public double getDoubleValue(Enum<?> variableID) {
            if (BenefitUnit.Variables.LagUnsecuredDebtLowCostOnly.equals(variableID)) {
                return UnsecuredDebtState.LowCostOnly.equals(assumedLagState) ? 1.0 : 0.0;
            }
            if (BenefitUnit.Variables.LagUnsecuredDebtHighCostOnly.equals(variableID)) {
                return UnsecuredDebtState.HighCostOnly.equals(assumedLagState) ? 1.0 : 0.0;
            }
            if (BenefitUnit.Variables.LagUnsecuredDebtMixed.equals(variableID)) {
                return UnsecuredDebtState.Mixed.equals(assumedLagState) ? 1.0 : 0.0;
            }
            return benefitUnit.getDoubleValue(variableID);
        }
    }

    private double getInnovation(String regression, double randomDraw) {
        double rmse = Parameters.getRMSEForRegression(regression);
        double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(randomDraw);
        return gauss * rmse;
    }

    private double positiveOrZero(double value) {
        return (Parameters.isFinite(value) && value > 0.0) ? value : 0.0;
    }

    private UnsecuredDebtState getUnsecuredDebtStateFromValues() {
        boolean hasLowDebt = wealthUnsecuredDebtLowValue > 0.0;
        boolean hasHighDebt = wealthUnsecuredDebtHighValue > 0.0;
        if (hasLowDebt && hasHighDebt) return UnsecuredDebtState.Mixed;
        if (hasLowDebt) return UnsecuredDebtState.LowCostOnly;
        if (hasHighDebt) return UnsecuredDebtState.HighCostOnly;
        return UnsecuredDebtState.None;
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getValue() {
        return wealthFinancialAssetsValue - wealthUnsecuredDebtLowValue - wealthUnsecuredDebtHighValue;
    }

    public void setValue(double val) {
        wealthFinancialAssetsValue = val + wealthUnsecuredDebtLowValue + wealthUnsecuredDebtHighValue;
    }

    public double getWealthFinancialAssetsValue() {
        return wealthFinancialAssetsValue;
    }

    public double getWealthUnsecuredDebtLowValue() {
        return wealthUnsecuredDebtLowValue;
    }

    public double getWealthUnsecuredDebtHighValue() {
        return wealthUnsecuredDebtHighValue;
    }

    public double getWealthUnsecuredDebtLowInnovation() {
        return wealthUnsecuredDebtLowInnovation;
    }

    public double getWealthUnsecuredDebtHighInnovation() {
        return wealthUnsecuredDebtHighInnovation;
    }

    public void setWealthUnsecuredDebtLowInnovation(double value) {
        wealthUnsecuredDebtLowInnovation = value;
    }

    public void setWealthUnsecuredDebtHighInnovation(double value) {
        wealthUnsecuredDebtHighInnovation = value;
    }

    public UnsecuredDebtState getUnsecuredDebtState() {
        return unsecuredDebtState;
    }

    public boolean hasLowCostDebt() {
        return unsecuredDebtState.hasLowCostDebt();
    }

    public boolean hasHighCostDebt() {
        return unsecuredDebtState.hasHighCostDebt();
    }

    public boolean hasMixedDebt() {
        return unsecuredDebtState == UnsecuredDebtState.Mixed;
    }
}
