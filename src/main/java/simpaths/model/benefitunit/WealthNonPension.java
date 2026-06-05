package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.TimeVaryingRate;

public class WealthNonPension {

    WealthHousing wealthHousing;                // object to manage housing wealth
    private double wealthFinancialValue;            // placeholder for other wealth
    private double inYearAccrualOth;            // accrued other wealth in the year


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthNonPension() {
        wealthHousing = new WealthHousing();
        wealthFinancialValue = 0.0;
        inYearAccrualOth = 0.0;
    }

    public WealthNonPension(WealthNonPension original) {
        wealthHousing = new WealthHousing(original.wealthHousing);
        wealthFinancialValue = original.wealthFinancialValue;
        inYearAccrualOth = original.inYearAccrualOth;
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue) {

        wealthHousing = new WealthHousing(wealthPrptyValue,  wealthMortgageDebtValue);
        this.wealthFinancialValue = wealthTotValue - wealthPrptyValue + wealthMortgageDebtValue - wealthPensValue;
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectFinancialWealthIncomeAnnual(int year) {
        return wealthFinancialValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealSavingReturn);
    }

    public double projectHousingWealthReturnAnnual(int year) {
        return 0.0;
    }

    public void projectNonPensionWealth(WealthNonPension wealthL1, double disposableIncomeAnnual, double xConsumptionAnnual) {

        inYearAccrualOth = disposableIncomeAnnual - xConsumptionAnnual;
        wealthFinancialValue = wealthL1.getWealthNonPensionValue() + inYearAccrualOth;
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getInYearAccrualTotal() {

        return wealthHousing.getInYearAccrualPty() - wealthHousing.getInYearAccrualMtg() + inYearAccrualOth;
    }

    public double getWealthNonPensionValue() {
        return wealthHousing.getWealthPrptyValue() - wealthHousing.getWealthMortgageDebtValue() + wealthFinancialValue;
    }

    public double getNonPensionValue() {
        return wealthHousing.getWealthPrptyValue() - wealthHousing.getWealthMortgageDebtValue() + wealthFinancialValue;
    }

    public double getOtherValue() {
        return wealthFinancialValue;
    }

    public double getInYearAccrualOth() {
        return inYearAccrualOth;
    }

    public void setInYearAccrualOth(double inYearAccrualOth) {
        this.inYearAccrualOth = inYearAccrualOth;
    }

    public double getWealthFinancialValue() {
        return wealthFinancialValue;
    }

    public void setWealthFinancialValue(double wealthFinancialValue) {
        this.wealthFinancialValue = wealthFinancialValue;
    }

    public double getWealthPrptyValue() {
        return wealthHousing.getWealthPrptyValue();
    }

    public double  getWealthMortgageDebtValue() {
        return wealthHousing.getWealthMortgageDebtValue();
    }

    public boolean isHomeOwner() {
        return wealthHousing.isHomeOwner();
    }
}
