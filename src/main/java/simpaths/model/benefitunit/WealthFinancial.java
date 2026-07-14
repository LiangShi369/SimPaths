package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.BenefitUnit;
import simpaths.model.enums.TimeVaryingRate;
import simpaths.model.enums.UnsecuredDebtState;

public class WealthFinancial {

    private double wealthFinancialAssetsValue;              // value of financial assets before unsecured debts
    private double wealthUnsecuredDebtLowValue;             // low-cost unsecured debt
    private double wealthUnsecuredDebtHighValue;            // high-cost unsecured debt
    private double wealthUnsecuredDebtLowInnovation;        // innovation used to project low-cost unsecured debt
    private double wealthUnsecuredDebtHighInnovation;       // innovation used to project high-cost unsecured debt
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

    public void projectWealthValue(BenefitUnit benefitUnit, WealthFinancial wealthFinancialL1, double netFinancialWealth,
                                   double innovUnsecuredDebtState, double innovLowDebtValue, double innovHighDebtValue) {
        // projects values - see BenefitUnit.updateNonPensionWealth()
        setValue(netFinancialWealth);
        unsecuredDebtState = drawUnsecuredDebtState(wealthFinancialL1.getUnsecuredDebtState(), innovUnsecuredDebtState);

        wealthUnsecuredDebtLowValue = 0.0;
        wealthUnsecuredDebtHighValue = 0.0;
        wealthUnsecuredDebtLowInnovation = 0.0;
        wealthUnsecuredDebtHighInnovation = 0.0;

        if (unsecuredDebtState.hasLowCostDebt()) {
            boolean continuing = wealthFinancialL1.hasLowCostDebt();
            String regression = continuing ? "FW2c" : "FW2b";
            double score = continuing ?
                    Parameters.getRegFW2c().getScore(benefitUnit, BenefitUnit.Variables.class) :
                    Parameters.getRegFW2b().getScore(benefitUnit, BenefitUnit.Variables.class);
            wealthUnsecuredDebtLowInnovation = getInnovation(regression, innovLowDebtValue);
            wealthUnsecuredDebtLowValue = Math.max(0.0, Math.sinh(score + wealthUnsecuredDebtLowInnovation));
            if (!Parameters.isFinite(wealthUnsecuredDebtLowValue))
                throw new RuntimeException("projection for low-cost unsecured debt value is not finite"
                        + " for benefit unit " + benefitUnit.getId()
                        + ", regression " + regression
                        + ", netFinancialWealth " + netFinancialWealth
                        + ", score " + score
                        + ", randomDraw " + innovLowDebtValue
                        + ", innovation " + wealthUnsecuredDebtLowInnovation
                        + ", transformed value " + (score + wealthUnsecuredDebtLowInnovation));
        }

        if (unsecuredDebtState.hasHighCostDebt()) {
            boolean continuing = wealthFinancialL1.hasHighCostDebt();
            String regression = continuing ? "FW2e" : "FW2d";
            double score = continuing ?
                    Parameters.getRegFW2e().getScore(benefitUnit, BenefitUnit.Variables.class) :
                    Parameters.getRegFW2d().getScore(benefitUnit, BenefitUnit.Variables.class);
            wealthUnsecuredDebtHighInnovation = getInnovation(regression, innovHighDebtValue);
            wealthUnsecuredDebtHighValue = Math.max(0.0, Math.sinh(score + wealthUnsecuredDebtHighInnovation));
            if (!Parameters.isFinite(wealthUnsecuredDebtHighValue))
                throw new RuntimeException("projection for high-cost unsecured debt value is not finite"
                        + " for benefit unit " + benefitUnit.getId()
                        + ", regression " + regression
                        + ", netFinancialWealth " + netFinancialWealth
                        + ", score " + score
                        + ", randomDraw " + innovHighDebtValue
                        + ", innovation " + wealthUnsecuredDebtHighInnovation
                        + ", transformed value " + (score + wealthUnsecuredDebtHighInnovation));
        }

        wealthFinancialAssetsValue = netFinancialWealth + wealthUnsecuredDebtLowValue + wealthUnsecuredDebtHighValue;
    }

    private UnsecuredDebtState drawUnsecuredDebtState(UnsecuredDebtState lagState, double randomDraw) {
        double cumulative = 0.0;
        for (UnsecuredDebtState state : UnsecuredDebtState.values()) {
            cumulative += Parameters.getFinancialWealthTransitionProbability(lagState.getValue(), state.getValue());
            if (randomDraw < cumulative) {
                return state;
            }
        }
        return UnsecuredDebtState.Mixed;
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
