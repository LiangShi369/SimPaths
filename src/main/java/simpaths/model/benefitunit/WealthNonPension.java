package simpaths.model.benefitunit;

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
        wealthNonPensionValue = original.wealthNonPensionValue;
        wealthHousing = new WealthHousing(original.wealthHousing);
        wealthFinancial = new WealthFinancial(original.wealthFinancial);
    }

    public WealthNonPension(double wealthTotValue, double wealthPrptyValue, double wealthMortgageDebtValue, double wealthPensValue) {
        // used to set for initial population
        inYearSavings = 0.0;
        inYearHousingAccrual = 0.0;
        wealthNonPensionValue = wealthTotValue - wealthPensValue;
        wealthHousing = new WealthHousing(wealthPrptyValue,  wealthMortgageDebtValue);
        wealthFinancial = new WealthFinancial(wealthNonPensionValue - wealthHousing.getWealthNetHousing());
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

        inYearSavings = disposableIncomeAnnual - xConsumptionAnnual;
        wealthFinancial.setValue(wealthL1.getWealthNonPensionValue() + inYearSavings);
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getInYearAccrualTotal() {

        return wealthHousing.getInYearAccrualPty() - wealthHousing.getInYearAccrualMtg() + inYearSavings;
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

    public double getInYearSavings() {
        return inYearSavings;
    }

    public void setInYearSavings(double inYearSavings) {
        this.inYearSavings = inYearSavings;
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
