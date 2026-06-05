package simpaths.model.benefitunit;

import simpaths.data.Parameters;
import simpaths.model.enums.TimeVaryingRate;

public class WealthFinancial {

    // PLACEHOLDER - NEEDS TO BE ADAPTED TO ACCOMMODATE FINANCIAL WEALTH COMPONENTS
    private double wealthFinancialValue;            // placeholder for other wealth
    private double yWealthFinancialReturnYear;      // return to financial wealth during year


    /******************************************************
     * CONSTRUCTORS
     ******************************************************/

    public WealthFinancial() {
        wealthFinancialValue = 0.0;
        yWealthFinancialReturnYear = 0.0;
    }

    public WealthFinancial(WealthFinancial original) {
        wealthFinancialValue = original.wealthFinancialValue;
        yWealthFinancialReturnYear = original.yWealthFinancialReturnYear;
    }

    public WealthFinancial(double value) {
        wealthFinancialValue = value;
        yWealthFinancialReturnYear = 0.0;
    }


    /******************************************************
     * UTILITY METHODS
     ******************************************************/

    public double projectIncomeAnnual(int year) {
        // PLACEHOLDER - NEEDS TO BE ADAPTED TO REPORT NET INCOME FROM ALL NON-PENSION SOURCES
        yWealthFinancialReturnYear = wealthFinancialValue * Parameters.getTimeSeriesRate(year, TimeVaryingRate.RealSavingReturn);
        return yWealthFinancialReturnYear;
    }

    public void projectWealthValue(double inYearAccrual) {
        // PLACEHOLDER - NEEDS TO BE ADAPTED TO PROJECT FINANCIAL WEALTH COMPONENTS
        wealthFinancialValue += inYearAccrual;
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getValue() {
        return wealthFinancialValue;
    }

    public void setValue(double val) {
        wealthFinancialValue = val;
    }
}
