package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.TimeVaryingRate;

public class WealthHousing {

    private boolean isHomeOwner;                // indicator of whether the person is a homeowner
    private double wealthPrptyValue;            // value of the property
    private double inYearAccrualPty;            // accrued property wealth in the year
    private boolean isMortgageHolder;           // indicator of whether the person is a mortgage holder
    private double inYearAccrualMtg;            // accrued mortgage debt in the year
    private double wealthMortgageDebtValue;     // value of outstanding mortgage debt


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthHousing() {
        isHomeOwner = false;
        wealthPrptyValue = 0.0;
        isMortgageHolder = false;
        wealthMortgageDebtValue = 0.0;
        inYearAccrualPty = 0.0;
        inYearAccrualMtg = 0.0;
    }

    public WealthHousing(WealthHousing original) {
        isHomeOwner = original.isHomeOwner;
        wealthPrptyValue = original.wealthPrptyValue;
        isMortgageHolder = original.isMortgageHolder;
        wealthMortgageDebtValue = original.wealthMortgageDebtValue;
        inYearAccrualPty = original.inYearAccrualPty;
        inYearAccrualMtg = original.inYearAccrualMtg;
    }

    public WealthHousing(double wealthPrptyValue, double wealthMortgageDebtValue) {

        this.wealthPrptyValue = wealthPrptyValue;
        this.wealthMortgageDebtValue = wealthMortgageDebtValue;
        isHomeOwner = (wealthPrptyValue > 0.0);
        isMortgageHolder = (wealthMortgageDebtValue > 0.0);
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectReturnAnnual(int year) {

        if (isHomeOwner) {
            inYearAccrualPty = wealthPrptyValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealHousingReturn);
            if (isMortgageHolder)
                inYearAccrualMtg = wealthMortgageDebtValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealMortgageRate);
        }
        return inYearAccrualPty - inYearAccrualMtg;
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

    public void setInYearAccrualMtg(double inYearAccrualMtg) {
        this.inYearAccrualMtg = inYearAccrualMtg;
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

    public double getWealthPrptyValue() {
        return wealthPrptyValue;
    }

    public void setWealthPrptyValue(double wealthPrptyValue) {
        this.wealthPrptyValue = wealthPrptyValue;
    }

    public double getWealthNetHousing() {
        return wealthPrptyValue - wealthMortgageDebtValue;
    }
}
