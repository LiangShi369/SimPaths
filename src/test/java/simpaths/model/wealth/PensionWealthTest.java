package simpaths.model.wealth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import simpaths.model.enums.Les_c4;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PensionWealth")
public class PensionWealthTest {

    @Test
    @DisplayName("investment return is zero when wealth is zero")
    void investmentReturnIsZeroWhenWealthIsZero() {
        PensionWealth pw = new PensionWealth(0.0);
        assertEquals(0.0, pw.computeInvestmentReturn(0.05), 1e-10);
    }

    @Test
    @DisplayName("investment return scales correctly with growth rate")
    void investmentReturnScalesByRate() {
        PensionWealth pw = new PensionWealth(10000.0);
        assertEquals(500.0, pw.computeInvestmentReturn(0.05), 1e-10);
    }

    @Test
    @DisplayName("contribution is zero when isPensionContributor flag is false")
    void contributionIsZeroWhenNotAPensionContributor() {
        PensionWealth pw = new PensionWealth(5000.0);
        assertEquals(0.0, pw.computeContribution(false, Les_c4.EmployedOrSelfEmployed, 30000.0, 0.05), 1e-10);
    }

    @Test
    @DisplayName("contribution is zero when not employed even if contributor flag is set")
    void contributionIsZeroWhenNotEmployedEvenIfContributor() {
        PensionWealth pw = new PensionWealth(5000.0);
        assertEquals(0.0, pw.computeContribution(true, Les_c4.NotEmployed, 30000.0, 0.05), 1e-10);
    }

    @Test
    @DisplayName("contribution is zero when retired even if contributor flag is set")
    void contributionIsZeroWhenRetiredEvenIfContributor() {
        PensionWealth pw = new PensionWealth(5000.0);
        assertEquals(0.0, pw.computeContribution(true, Les_c4.Retired, 30000.0, 0.05), 1e-10);
    }

    @Test
    @DisplayName("contribution is zero when student even if contributor flag is set")
    void contributionIsZeroWhenStudentEvenIfContributor() {
        PensionWealth pw = new PensionWealth(5000.0);
        assertEquals(0.0, pw.computeContribution(true, Les_c4.Student, 30000.0, 0.05), 1e-10);
    }

    @Test
    @DisplayName("contribution is zero when labC4 is null even if contributor flag is set")
    void contributionIsZeroWhenLabC4IsNull() {
        PensionWealth pw = new PensionWealth(5000.0);
        assertEquals(0.0, pw.computeContribution(true, null, 30000.0, 0.05), 1e-10);
    }

    @Test
    @DisplayName("contribution matches rate times income when employed and contributor flag is set")
    void contributionMatchesRateWhenEmployedAndContributor() {
        PensionWealth pw = new PensionWealth(5000.0);
        assertEquals(1500.0, pw.computeContribution(true, Les_c4.EmployedOrSelfEmployed, 30000.0, 0.05), 1e-10);
    }

    @Test
    @DisplayName("projectYear accumulates growth and contribution correctly")
    void projectYearAccumulatesGrowthAndContribution() {
        PensionWealth pw = new PensionWealth(10000.0);
        // growth: 10000 * 0.05 = 500; contribution: 30000 * 0.05 = 1500; total: 12000
        pw.projectYear(0.05, true, Les_c4.EmployedOrSelfEmployed, 30000.0, 0.05);
        assertEquals(12000.0, pw.getValue(), 1e-10);
    }

    @Test
    @DisplayName("projectYear applies only growth when contributor flag is false")
    void projectYearWithNoContributionStillAppliesGrowth() {
        PensionWealth pw = new PensionWealth(10000.0);
        pw.projectYear(0.05, false, Les_c4.EmployedOrSelfEmployed, 30000.0, 0.05);
        assertEquals(10500.0, pw.getValue(), 1e-10);
    }

    @Test
    @DisplayName("projectYear starting from zero accumulates only contributions when initial value is zero")
    void projectYearFromZeroAccumulatesContributionOnly() {
        PensionWealth pw = new PensionWealth(0.0);
        // growth: 0; contribution: 20000 * 0.05 = 1000
        pw.projectYear(0.05, true, Les_c4.EmployedOrSelfEmployed, 20000.0, 0.05);
        assertEquals(1000.0, pw.getValue(), 1e-10);
    }

    @Test
    @DisplayName("getValue returns the initial value before any projection")
    void getValueReturnsInitialValue() {
        PensionWealth pw = new PensionWealth(7500.0);
        assertEquals(7500.0, pw.getValue(), 1e-10);
    }
}
