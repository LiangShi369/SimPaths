package simpaths.model.wealth;

import simpaths.model.enums.Les_c4;

public class PensionWealth {

    private double value;

    public PensionWealth(double initialValue) {
        this.value = initialValue;
    }

    public double getValue() {
        return value;
    }

    public double computeInvestmentReturn(double growthRate) {
        return value * growthRate;
    }

    /**
     * Returns the pension contribution for the year.
     * Zero if the person is not a pension contributor this year, or if not in employment.
     */
    public double computeContribution(boolean isPensionContributor, Les_c4 labC4, double annualEmploymentIncome, double contributionRate) {
        if (!isPensionContributor) return 0.0;
        if (!Les_c4.EmployedOrSelfEmployed.equals(labC4)) return 0.0;
        return contributionRate * annualEmploymentIncome;
    }

    /**
     * Projects pension wealth forward one year:
     * new value = prior value + investment return + pension contribution.
     */
    public void projectYear(double growthRate, boolean isPensionContributor, Les_c4 labC4, double annualEmploymentIncome, double contributionRate) {
        value += computeInvestmentReturn(growthRate)
                + computeContribution(isPensionContributor, labC4, annualEmploymentIncome, contributionRate);
    }
}
