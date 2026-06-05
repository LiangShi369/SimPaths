package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.TimeVaryingRate;

public class WealthNonPension {

    WealthHousing wealthHousing;                // object to manage housing wealth
    private double wealthOtherValue;            // placeholder for other wealth
    private double inYearAccrualOth;            // accrued other wealth in the year

    public WealthNonPension() {
        wealthHousing = new WealthHousing();
        wealthOtherValue = 0.0;
        inYearAccrualOth = 0.0;
    }

    public WealthNonPension(WealthNonPension original) {
        wealthHousing = new WealthHousing(original.wealthHousing);
        wealthOtherValue = original.wealthOtherValue;
        inYearAccrualOth = original.inYearAccrualOth;
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue) {

        wealthHousing = new WealthHousing(wealthPrptyValue,  wealthMortgageDebtValue);
        this.wealthOtherValue = wealthTotValue - wealthPrptyValue + wealthMortgageDebtValue - wealthPensValue;
    }

    public double projectWealthOtherIncomeAnnual(int year) {
        return wealthOtherValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealSavingReturn);
    }

    public void projectWealth(double wealthL1, double disposableIncomeAnnual, double xConsumptionAnnual) {

        inYearAccrualOth = disposableIncomeAnnual - xConsumptionAnnual;
        wealthOtherValue = wealthL1 + inYearAccrualOth;
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getInYearAccrualTotal() {

        return wealthHousing.getInYearAccrualPty() - wealthHousing.getInYearAccrualMtg() + inYearAccrualOth;
    }

    public double getWealthNonPensionTotalValue() {
        return wealthHousing.getWealthPrptyValue() - wealthHousing.getWealthMortgageDebtValue() + wealthOtherValue;
    }

    public double getNonPensionValue() {
        return wealthHousing.getWealthPrptyValue() - wealthHousing.getWealthMortgageDebtValue() + wealthOtherValue;
    }

    public void setOtherValue(double value) {
        wealthOtherValue = value;
    }

    public double getOtherValue() {
        return wealthOtherValue;
    }

    public double getInYearAccrualOth() {
        return inYearAccrualOth;
    }

    public void setInYearAccrualOth(double inYearAccrualOth) {
        this.inYearAccrualOth = inYearAccrualOth;
    }

    public double getWealthOtherValue() {
        return wealthOtherValue;
    }

    public void setWealthOtherValue(double wealthOtherValue) {
        this.wealthOtherValue = wealthOtherValue;
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
