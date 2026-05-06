package simpaths.model;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import simpaths.data.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BenefitUnit pension contribution deduction helper")
public class BenefitUnitPensionContributionTest {

    private BenefitUnit benefitUnit;

    @BeforeEach
    void setUp() {
        benefitUnit = new BenefitUnit(1L, 123L);
        Parameters.projectPensionWealth = true;
        Parameters.pensionContributionRate = 0.05;
    }

    @AfterEach
    void tearDown() {
        Parameters.projectPensionWealth = false;
        Parameters.pensionContributionRate = 0.0;
    }

    @Test
    @DisplayName("no deduction when pension wealth projection is disabled")
    void noDeductionWhenProjectionDisabled() {
        Parameters.projectPensionWealth = false;
        Person mockPerson = Mockito.mock(Person.class);
        Mockito.when(mockPerson.isPensionContributor()).thenReturn(true);
        assertEquals(0.0, benefitUnit.pensionContributionPerMonth(mockPerson, 1000.0), 1e-10);
    }

    @Test
    @DisplayName("no deduction when person is not a pension contributor")
    void noDeductionForNonContributor() {
        Person mockPerson = Mockito.mock(Person.class);
        Mockito.when(mockPerson.isPensionContributor()).thenReturn(false);
        assertEquals(0.0, benefitUnit.pensionContributionPerMonth(mockPerson, 1000.0), 1e-10);
    }

    @Test
    @DisplayName("deduction equals contribution rate times earnings for a contributor")
    void deductionMatchesRateTimesEarnings() {
        Person mockPerson = Mockito.mock(Person.class);
        Mockito.when(mockPerson.isPensionContributor()).thenReturn(true);
        assertEquals(50.0, benefitUnit.pensionContributionPerMonth(mockPerson, 1000.0), 1e-10);
    }

    @Test
    @DisplayName("deduction is zero when earnings are zero even for a contributor")
    void noDeductionWhenEarningsAreZero() {
        Person mockPerson = Mockito.mock(Person.class);
        Mockito.when(mockPerson.isPensionContributor()).thenReturn(true);
        assertEquals(0.0, benefitUnit.pensionContributionPerMonth(mockPerson, 0.0), 1e-10);
    }

    @Test
    @DisplayName("deduction scales linearly with earnings")
    void deductionScalesLinearlyWithEarnings() {
        Person mockPerson = Mockito.mock(Person.class);
        Mockito.when(mockPerson.isPensionContributor()).thenReturn(true);
        assertEquals(250.0, benefitUnit.pensionContributionPerMonth(mockPerson, 5000.0), 1e-10);
    }
}
