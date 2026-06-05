package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.TimeVaryingRate;

public class WealthNonPension {

    WealthHousing wealthHousing;                // object to manage housing wealth
    private double inYearAccrualOth;            // accrued other wealth in the year
    WealthFinancial wealthFinancial;            // object to manage financial wealth


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthNonPension() {
        wealthHousing = new WealthHousing();
        wealthFinancial = new WealthFinancial();
        inYearAccrualOth = 0.0;
    }

    public WealthNonPension(WealthNonPension original) {
        wealthHousing = new WealthHousing(original.wealthHousing);
        wealthFinancial = new WealthFinancial(original.wealthFinancial);
        inYearAccrualOth = original.inYearAccrualOth;
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue) {

        wealthHousing = new WealthHousing(wealthPrptyValue,  wealthMortgageDebtValue);
        wealthFinancial = new WealthFinancial(wealthTotValue - wealthPrptyValue + wealthMortgageDebtValue - wealthPensValue);
        inYearAccrualOth = 0.0;
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectFinancialWealthIncomeAnnual(int year) {
        return wealthFinancial.projectIncomeAnnual(year);
    }

    public double projectHousingWealthReturnAnnual(int year) {
        return 0.0;
    }

    public void projectNonPensionWealth(WealthNonPension wealthL1, double disposableIncomeAnnual, double xConsumptionAnnual) {

        inYearAccrualOth = disposableIncomeAnnual - xConsumptionAnnual;
        wealthFinancial.setValue(wealthL1.getWealthNonPensionValue() + inYearAccrualOth);
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getInYearAccrualTotal() {

        return wealthHousing.getInYearAccrualPty() - wealthHousing.getInYearAccrualMtg() + inYearAccrualOth;
    }

    public double getWealthNonPensionValue() {
        return wealthHousing.getWealthPrptyValue() - wealthHousing.getWealthMortgageDebtValue() + wealthFinancial.getValue();
    }

    public double getNonPensionValue() {
        return wealthHousing.getWealthPrptyValue() - wealthHousing.getWealthMortgageDebtValue() + wealthFinancial.getValue();
    }

    public double getOtherValue() {
        return wealthFinancial.getValue();
    }

    public double getInYearAccrualOth() {
        return inYearAccrualOth;
    }

    public void setInYearAccrualOth(double inYearAccrualOth) {
        this.inYearAccrualOth = inYearAccrualOth;
    }

    public double getWealthFinancialValue() {
        return wealthFinancial.getValue();
    }

    public void setWealthFinancialValue(double wealthFinancialValue) {
        wealthFinancial.setValue(wealthFinancialValue);
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
