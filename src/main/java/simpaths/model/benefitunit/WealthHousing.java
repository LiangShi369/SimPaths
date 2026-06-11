package simpaths.model.benefitunit;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.model.BenefitUnit;
import simpaths.model.enums.TimeVaryingRate;

public class WealthHousing {

    private double wealthPrptyValue;            // value of the property
    private double inYearAccrualPty;            // accrued property wealth in the year
    private double inYearAccrualMtg;            // accrued mortgage debt in the year
    private double wealthMortgageDebtValue;     // value of outstanding mortgage debt
    private double wealthNetInnovation;         // innovation used to project value of net housing wealth
    private double wealthMortgageDebtInnovation; // innovation used to project value of mortgage debt


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthHousing() {
        // used to initialise to zero
        wealthPrptyValue = 0.0;
        wealthMortgageDebtValue = 0.0;
        inYearAccrualPty = 0.0;
        inYearAccrualMtg = 0.0;
        wealthNetInnovation = 0.0;
        wealthMortgageDebtInnovation = 0.0;
    }

    public WealthHousing(WealthHousing original) {
        // used to generate lag objects
        wealthPrptyValue = original.wealthPrptyValue;
        wealthMortgageDebtValue = original.wealthMortgageDebtValue;
        inYearAccrualPty = original.inYearAccrualPty;
        inYearAccrualMtg = original.inYearAccrualMtg;
        wealthNetInnovation = original.wealthNetInnovation;
        wealthMortgageDebtInnovation = original.wealthMortgageDebtInnovation;
    }

    public WealthHousing(double wealthPrptyValue, double wealthMortgageDebtValue) {
        // used for initial population
        this.wealthPrptyValue = wealthPrptyValue;
        this.wealthMortgageDebtValue = wealthMortgageDebtValue;
        wealthNetInnovation = 0.0;
        wealthMortgageDebtInnovation = 0.0;
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectReturnAnnual(int year) {
        // projects returns - see BenefitUnit.setInvestmentIncomeAnnual()
        if (isHomeOwner()) {
            inYearAccrualPty = wealthPrptyValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealHousingReturn);
            if (isMortgageHolder())
                inYearAccrualMtg = wealthMortgageDebtValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealMortgageRate);
        }
        return inYearAccrualPty - inYearAccrualMtg;
    }

    public void projectValues(BenefitUnit benefitUnit, WealthHousing wealthHousingL1, double innovHomeownership, double innovNetValue,
                              double innovMortgageIncidence, double innovMortgageValue) {
        // projects values - see BenefitUnit.updateNonPensionWealth()

        boolean homeOwner = ManagerRegressions.getAnnualEventFromBiennial(benefitUnit, wealthHousingL1.isHomeOwner(), innovHomeownership, RegressionName.WealthHousingHW1a, RegressionName.WealthHousingHW1b);
        if (homeOwner) {

            double netHousing;
            double score, rmse, gauss;
            if (wealthHousingL1.isHomeOwner()) {
                score = Parameters.getRegHW1c().getScore(benefitUnit, BenefitUnit.Variables.class);
                rmse = Parameters.getRMSEForRegression("HW1c");
            } else {
                score = Parameters.getRegHW1d().getScore(benefitUnit, BenefitUnit.Variables.class);
                rmse = Parameters.getRMSEForRegression("HW1d");
            }
            gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovNetValue);
            wealthNetInnovation = gauss * rmse;
            netHousing = Math.sinh(score + wealthNetInnovation);
            if (!Parameters.isFinite(netHousing))
                throw new RuntimeException("projec" +
                        "tion for net housing value is not finite");

            wealthMortgageDebtValue = 0.0;
            wealthPrptyValue = netHousing;

            // consider mortgages
            boolean mortgageHolder;
            boolean newHomeOwner = !wealthHousingL1.isHomeOwner();
            if (newHomeOwner && !wealthHousingL1.isMortgageHolder()) {
                mortgageHolder = innovMortgageIncidence < ManagerRegressions.getProbability(benefitUnit, RegressionName.WealthHousingHW2a);
            } else {
                mortgageHolder = ManagerRegressions.getAnnualEventFromBiennial(benefitUnit, wealthHousingL1.isMortgageHolder(), innovMortgageIncidence, RegressionName.WealthHousingHW2a, RegressionName.WealthHousingHW2b);
            }
            if (mortgageHolder) {
                if (wealthHousingL1.isMortgageHolder()) {
                    score = Parameters.getRegHW2c().getScore(benefitUnit, BenefitUnit.Variables.class);
                    rmse = Parameters.getRMSEForRegression("HW2c");
                } else {
                    score = Parameters.getRegHW2d().getScore(benefitUnit, BenefitUnit.Variables.class);
                    rmse = Parameters.getRMSEForRegression("HW2d");
                }
                gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovMortgageValue);
                wealthMortgageDebtInnovation = gauss * rmse;
                double mortgageDebt = Math.exp(score + wealthMortgageDebtInnovation);
                if (!Parameters.isFinite(mortgageDebt))
                    throw new RuntimeException("projection for mortgage debt value is not finite");
                wealthMortgageDebtValue = mortgageDebt;
            }
            wealthPrptyValue = netHousing + wealthMortgageDebtValue;
        }
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getWealthNetInnovation() {
        return wealthNetInnovation;
    }

    public double getWealthMortgageDebtInnovation() {
        return wealthMortgageDebtInnovation;
    }

    public void setWealthNetInnovation( double val ) {
        wealthNetInnovation = val;
    }

    public double getInYearAccrualNet() {
        return inYearAccrualPty - inYearAccrualMtg;
    }

    public double getInYearAccrualMtg() {
        return inYearAccrualMtg;
    }

    public double getInYearAccrualPty() {
        return inYearAccrualPty;
    }

    public boolean isHomeOwner() {
        return (wealthPrptyValue > 0.0);
    }

    public boolean isMortgageHolder() {
        return (wealthMortgageDebtValue > 0.0);
    }

    public double getWealthMortgageDebtValue() {
        return wealthMortgageDebtValue;
    }

    public void setWealthMortgageDebtValue(double val) {
        wealthMortgageDebtValue = val;
    }

    public double getWealthPrptyValue() {
        return wealthPrptyValue;
    }

    public void setWealthPrptyValue(double val) {
        wealthPrptyValue = val;
    }

    public double getWealthNetHousing() {
        return wealthPrptyValue - wealthMortgageDebtValue;
    }
}
