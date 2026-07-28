package simpaths.model.benefitunit;

import simpaths.model.BenefitUnit;

public class WealthNonPension {

    private double inYearSavings;               // excess of disposable income to consumption in the year
    private double inYearHousingAccrual;        // accrued housing wealth in the year
    private double wealthNonPensionValue;       // total value of non-pension wealth
    WealthHousing wealthHousing;                // object to manage housing wealth
    WealthFinancial wealthFinancial;            // object to manage financial wealth


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthNonPension() {
        // used to initialise to zero
        inYearSavings = 0.0;
        inYearHousingAccrual = 0.0;
        wealthNonPensionValue = 0.0;
        wealthHousing = new WealthHousing();
        wealthFinancial = new WealthFinancial();
    }

    public WealthNonPension(WealthNonPension original) {
        // used to generate lag objects
        inYearSavings = original.inYearSavings;
        inYearHousingAccrual = original.inYearHousingAccrual;
        wealthNonPensionValue = original.getWealthNonPensionValue();
        wealthHousing = new WealthHousing(original.wealthHousing);
        wealthFinancial = new WealthFinancial(original.wealthFinancial);
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue) {
        this(wealthTotValue, wealthPrptyValue, wealthMortgageDebtValue, wealthPensValue, 0.0, 0.0);
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue,
                            double wealthUnsecuredDebtLowValue, double wealthUnsecuredDebtHighValue) {
        // used for initial population
        inYearSavings = 0.0;
        inYearHousingAccrual = 0.0;
        wealthNonPensionValue = wealthTotValue - wealthPensValue;
        wealthHousing = new WealthHousing(wealthPrptyValue,  wealthMortgageDebtValue);
        wealthFinancial = new WealthFinancial(wealthNonPensionValue - wealthHousing.getWealthNetHousing(),
                wealthUnsecuredDebtLowValue, wealthUnsecuredDebtHighValue);
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectFinancialWealthIncomeAnnual(int year) {
        // used to evaluate taxable income - see BenefitUnit.setInvestmentIncomeAnnual()
        return wealthFinancial.projectIncomeAnnual(year);
    }

    public double projectHousingWealthReturnAnnual(int year) {
        return wealthHousing.projectReturnAnnual(year);
    }

    public void projectNonPensionWealthBeforeUnsecuredDebt(BenefitUnit benefitUnit,
                                        WealthNonPension wealthNonPensionL1,
                                        double disposableIncomeAnnual, double xConsumptionAnnual,
                                        double innovHousingIncidence, double innovHousingNetValue,
                                        double innovMortgageIncidence, double innovMortgageValue) {

        // project total non-pension wealth
        inYearSavings = disposableIncomeAnnual - xConsumptionAnnual;
        wealthNonPensionValue = wealthNonPensionL1.getWealthNonPensionValue() + inYearSavings + wealthNonPensionL1.getWealthHousing().getInYearAccrualNet();

        // allocate share of non-pension wealth to housing
        wealthHousing.projectValues(benefitUnit, wealthNonPensionL1.getWealthHousing(), innovHousingIncidence, innovHousingNetValue,
                innovMortgageIncidence, innovMortgageValue);

        // Establish current net financial wealth for every benefit unit before
        // any FW2a transition is evaluated.  The model assigns current-year
        // cross-sectional deciles between this phase and projectUnsecuredDebt.
        wealthFinancial.setValue(wealthNonPensionValue - wealthHousing.getWealthNetHousing());
    }

    public void projectUnsecuredDebt(BenefitUnit benefitUnit,
                                     WealthNonPension wealthNonPensionL1,
                                     double innovUnsecuredDebtState,
                                     double innovLowDebtValue,
                                     double innovHighDebtValue) {
        wealthFinancial.projectUnsecuredDebt(benefitUnit,
                wealthNonPensionL1.getWealthFinancial(),
                innovUnsecuredDebtState, innovLowDebtValue, innovHighDebtValue);
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getInYearAccrualTotal() {

        return wealthHousing.getInYearAccrualNet() + inYearSavings;
    }

    public WealthHousing getWealthHousing() {return wealthHousing;}

    public WealthFinancial getWealthFinancial() {return wealthFinancial;}

    public double getWealthNonPensionValue() {
        return updateWealthNonPensionValue();
    }

    public double getWealthNonPensionValueDirect() {
        return wealthNonPensionValue;
    }

    public double getInYearSavings() {
        return inYearSavings;
    }

    public void setInYearSavings(double val) {
        inYearSavings = val;
    }

    public void setWealthFinancialValue(double val) {
        wealthFinancial.setValue(val);
        updateWealthNonPensionValue();
    }

    public double updateWealthNonPensionValue() {
        return wealthNonPensionValue = wealthHousing.getWealthNetHousing() + wealthFinancial.getValue();
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
