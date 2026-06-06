package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.TimeVaryingRate;

public class WealthHousing {

    private double wealthPrptyValue;            // value of the property
    private double inYearAccrualPty;            // accrued property wealth in the year
    private double inYearAccrualMtg;            // accrued mortgage debt in the year
    private double wealthMortgageDebtValue;     // value of outstanding mortgage debt


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthHousing() {
        wealthPrptyValue = 0.0;
        wealthMortgageDebtValue = 0.0;
        inYearAccrualPty = 0.0;
        inYearAccrualMtg = 0.0;
    }

    public WealthHousing(WealthHousing original) {
        wealthPrptyValue = original.wealthPrptyValue;
        wealthMortgageDebtValue = original.wealthMortgageDebtValue;
        inYearAccrualPty = original.inYearAccrualPty;
        inYearAccrualMtg = original.inYearAccrualMtg;
    }

    public WealthHousing(double wealthPrptyValue, double wealthMortgageDebtValue) {

        this.wealthPrptyValue = wealthPrptyValue;
        this.wealthMortgageDebtValue = wealthMortgageDebtValue;
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

    public void projectValues() {
        // projects values - see BenefitUnit.updateNonPensionWealth()
        
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

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
