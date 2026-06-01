package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.TimeVaryingRate;

public class WealthNonPension {

    private boolean isHomeOwner;                // indicator of whether the person is a homeowner
    private double wealthPrptyValue;            // value of the property
    private double inYearAccrualPty;            // accrued property wealth in the year
    private boolean isMortgageHolder;           // indicator of whether the person is a mortgage holder
    private double inYearAccrualMtg;            // accrued mortgage debt in the year
    private double wealthMortgageDebtValue;     // value of outstanding mortgage debt
    private double wealthOtherValue;            // placeholder for other wealth
    private double inYearAccrualOth;            // accrued other wealth in the year

    public WealthNonPension() {
        isHomeOwner = false;
        wealthPrptyValue = 0.0;
        isMortgageHolder = false;
        wealthMortgageDebtValue = 0.0;
        wealthOtherValue = 0.0;
        inYearAccrualPty = 0.0;
        inYearAccrualMtg = 0.0;
        inYearAccrualOth = 0.0;
    }

    public WealthNonPension(WealthNonPension original) {
        isHomeOwner = original.isHomeOwner;
        wealthPrptyValue = original.wealthPrptyValue;
        isMortgageHolder = original.isMortgageHolder;
        wealthMortgageDebtValue = original.wealthMortgageDebtValue;
        wealthOtherValue = original.wealthOtherValue;
        inYearAccrualPty = original.inYearAccrualPty;
        inYearAccrualMtg = original.inYearAccrualMtg;
        inYearAccrualOth = original.inYearAccrualOth;
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue) {

        this.wealthPrptyValue = wealthPrptyValue;
        this.wealthMortgageDebtValue = wealthMortgageDebtValue;
        this.wealthOtherValue = wealthTotValue - wealthPrptyValue + wealthMortgageDebtValue - wealthPensValue;
        isHomeOwner = (wealthPrptyValue > 0.0);
        isMortgageHolder = (wealthMortgageDebtValue > 0.0);
    }

    public double projectWealthOtherIncomeAnnual(int year) {
        return wealthOtherValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealSavingReturns);
    }

    public void projectWealth(double wealthL1, double disposableIncomeAnnual, double xConsumptionAnnual) {

        inYearAccrualOth = disposableIncomeAnnual - xConsumptionAnnual;
        wealthOtherValue = wealthL1 + inYearAccrualOth;
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getInYearAccrualTotal() {

        return inYearAccrualPty - inYearAccrualMtg + inYearAccrualOth;
    }

    public double getWealthTotalValue() {
        return wealthPrptyValue - wealthMortgageDebtValue + wealthOtherValue;
    }

    public double getNonPensionValue() {
        return wealthPrptyValue - wealthMortgageDebtValue + wealthOtherValue;
    }

    public void setOtherValue(double value) {
        wealthOtherValue = value;
    }

    public double getOtherValue() {
        return wealthOtherValue;
    }

    public double getInYearAccrualMtg() {
        return inYearAccrualMtg;
    }

    public void setInYearAccrualMtg(double inYearAccrualMtg) {
        this.inYearAccrualMtg = inYearAccrualMtg;
    }

    public double getInYearAccrualOth() {
        return inYearAccrualOth;
    }

    public void setInYearAccrualOth(double inYearAccrualOth) {
        this.inYearAccrualOth = inYearAccrualOth;
    }

    public double getInYearAccrualPty() {
        return inYearAccrualPty;
    }

    public void setInYearAccrualPty(double inYearAccrualPty) {
        this.inYearAccrualPty = inYearAccrualPty;
    }

    public boolean isHomeOwner() {
        return isHomeOwner;
    }

    public void setHomeOwner(boolean homeOwner) {
        isHomeOwner = homeOwner;
    }

    public boolean isMortgageHolder() {
        return isMortgageHolder;
    }

    public void setMortgageHolder(boolean mortgageHolder) {
        isMortgageHolder = mortgageHolder;
    }

    public double getWealthMortgageDebtValue() {
        return wealthMortgageDebtValue;
    }

    public void setWealthMortgageDebtValue(double wealthMortgageDebtValue) {
        this.wealthMortgageDebtValue = wealthMortgageDebtValue;
    }

    public double getWealthOtherValue() {
        return wealthOtherValue;
    }

    public void setWealthOtherValue(double wealthOtherValue) {
        this.wealthOtherValue = wealthOtherValue;
    }

    public double getWealthPrptyValue() {
        return wealthPrptyValue;
    }

    public void setWealthPrptyValue(double wealthPrptyValue) {
        this.wealthPrptyValue = wealthPrptyValue;
    }
}
