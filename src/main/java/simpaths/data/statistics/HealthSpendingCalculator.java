package simpaths.data.statistics;

import simpaths.data.HealthCostProfile;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.SampleExit;

import java.util.Collections;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Calculates public health spending from the realised single-age population.
 * Create one instance per simulation run so its 2028 age shares cannot leak
 * between runs. All monetary amounts are annual real-2015 pounds.
 */
public final class HealthSpendingCalculator {

    private final HealthCostProfile costProfile;
    private SortedMap<Integer, Double> baseYearAgeShares;

    public HealthSpendingCalculator(HealthCostProfile costProfile) {
        this.costProfile = Objects.requireNonNull(costProfile, "Health cost profile must not be null");
    }

    /**
     * Prices the realised single-age population using this year's cost profile.
     * Once the run reaches 2028, it also estimates what the same year's spending
     * would be if age shares stayed at their realised 2028 values. Population
     * size and per-person costs still change each year; only age shares are fixed.
     * A run beginning after 2028 has no fixed-age counterfactual.
     */
    public SpendingResult calculate(PopulationSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Population snapshot must not be null");
        int year = snapshot.getYear();
        // Validate the year even for an empty population.
        costProfile.cost(year, 0);
        if (year == HealthCostProfile.FIRST_YEAR) {
            if (baseYearAgeShares != null) {
                throw new IllegalStateException("2028 health-spending baseline has already been captured");
            }
            if (snapshot.getTotalPopulation() <= 0.0) {
                throw new IllegalArgumentException("2028 population must be positive to form age shares");
            }
        }

        SortedMap<Integer, Double> spendingByAge = new TreeMap<>();
        double benchmark = 0.0;
        // Benchmark: sum(actual expanded people at age a * this year's cost at age a).
        for (var entry : snapshot.getPopulationByAge().entrySet()) {
            double ageSpending = entry.getValue() * costProfile.cost(year, entry.getKey());
            benchmark += ageSpending;
            if (!Double.isFinite(ageSpending) || !Double.isFinite(benchmark)) {
                throw new IllegalArgumentException("Non-finite health spending in year " + year);
            }
            spendingByAge.put(entry.getKey(), ageSpending);
        }

        SortedMap<Integer, Double> sharesForThisYear = baseYearAgeShares;
        if (year == HealthCostProfile.FIRST_YEAR) {
            // Capture age shares from this run's 2028 population, not from an input file.
            // Each share is the expanded population at that age / total 2028 population.
            SortedMap<Integer, Double> shares = new TreeMap<>();
            for (var entry : snapshot.getPopulationByAge().entrySet()) {
                shares.put(entry.getKey(), entry.getValue() / snapshot.getTotalPopulation());
            }
            sharesForThisYear = Collections.unmodifiableSortedMap(shares);
        }

        OptionalDouble fixed = OptionalDouble.empty();
        OptionalDouble ageingEffect = OptionalDouble.empty();
        if (sharesForThisYear != null) {
            // Counterfactual: apply the fixed 2028 age shares to the current total
            // population, then price those hypothetical people at current-year costs.
            double fixedSpending = 0.0;
            for (var entry : sharesForThisYear.entrySet()) {
                double ageSpending = snapshot.getTotalPopulation() * entry.getValue()
                        * costProfile.cost(year, entry.getKey());
                fixedSpending += ageSpending;
                if (!Double.isFinite(ageSpending) || !Double.isFinite(fixedSpending)) {
                    throw new IllegalArgumentException("Non-finite fixed-age health spending in year " + year);
                }
            }
            fixed = OptionalDouble.of(fixedSpending);
            // The two totals differ only because their age distributions differ.
            ageingEffect = OptionalDouble.of(benchmark - fixedSpending);
        }
        if (year == HealthCostProfile.FIRST_YEAR) {
            // Retain these shares for later years of this simulation run.
            baseYearAgeShares = sharesForThisYear;
        }
        return new SpendingResult(year, snapshot.getTotalPopulation(), spendingByAge,
                benchmark, fixed, ageingEffect);
    }

    /**
     * Takes one snapshot of the model's realised population for the just-simulated year.
     * Call this after annual population alignment, not before it.
     */
    public static PopulationSnapshot snapshot(int year, SimPathsModel model) {
        Objects.requireNonNull(model, "SimPaths model must not be null");
        return snapshot(year, model.getPersons(), model.isUseWeights(), model.getScalingFactor());
    }

    // Package-private so the weighting and exit rules can be tested without running a simulation.
    static PopulationSnapshot snapshot(int year, Iterable<Person> persons,
                                       boolean useWeights, double scalingFactor) {
        Objects.requireNonNull(persons, "Persons must not be null");
        if (!Double.isFinite(scalingFactor) || scalingFactor <= 0.0) {
            throw new IllegalArgumentException("Scaling factor must be finite and positive: " + scalingFactor);
        }

        SortedMap<Integer, Double> populationByAge = new TreeMap<>();
        for (Person person : persons) {
            Objects.requireNonNull(person, "Persons must not contain null");
            if (!SampleExit.NotYet.equals(person.getSampleExit())) {
                continue;
            }

            int age = person.getDemAge();
            if (age < 0) {
                throw new IllegalArgumentException("Active person has negative age " + age + " in year " + year);
            }
            double weight = person.getWeight();
            if (!Double.isFinite(weight) || weight < 0.0) {
                throw new IllegalArgumentException("Invalid active-person weight at age " + age
                        + " in year " + year + ": " + weight);
            }
            // Convert each active simulated person to the population it represents.
            double effectiveWeight = useWeights ? weight : weight * scalingFactor;
            if (!Double.isFinite(effectiveWeight)) {
                throw new IllegalArgumentException("Non-finite effective weight at age " + age
                        + " in year " + year);
            }
            double ageTotal = populationByAge.getOrDefault(age, 0.0) + effectiveWeight;
            if (!Double.isFinite(ageTotal)) {
                throw new IllegalArgumentException("Non-finite population total at age " + age
                        + " in year " + year);
            }
            populationByAge.put(age, ageTotal);
        }

        double totalPopulation = 0.0;
        for (double ageTotal : populationByAge.values()) {
            totalPopulation += ageTotal;
            if (!Double.isFinite(totalPopulation)) {
                throw new IllegalArgumentException("Non-finite total population in year " + year);
            }
        }
        return new PopulationSnapshot(year, populationByAge, totalPopulation);
    }

    /** Expanded people at their actual single-year ages, including ages above 101. */
    public static final class PopulationSnapshot {
        private final int year;
        private final SortedMap<Integer, Double> populationByAge;
        private final double totalPopulation;

        private PopulationSnapshot(int year, SortedMap<Integer, Double> populationByAge,
                                   double totalPopulation) {
            this.year = year;
            this.populationByAge = Collections.unmodifiableSortedMap(new TreeMap<>(populationByAge));
            this.totalPopulation = totalPopulation;
        }

        public int getYear() {
            return year;
        }

        public SortedMap<Integer, Double> getPopulationByAge() {
            return populationByAge;
        }

        public double getTotalPopulation() {
            return totalPopulation;
        }

        public double getPopulationAtAge(int age) {
            if (age < 0) {
                throw new IllegalArgumentException("Age must be non-negative: " + age);
            }
            return populationByAge.getOrDefault(age, 0.0);
        }
    }

    /** A single year's benchmark and, if available, fixed-age counterfactual. */
    public static final class SpendingResult {
        private final int year;
        private final double totalPopulation;
        private final SortedMap<Integer, Double> spendingByAge;
        private final double benchmarkExpenditure;
        private final OptionalDouble fixedAgeExpenditure;
        private final OptionalDouble ageingEffect;

        private SpendingResult(int year, double totalPopulation,
                               SortedMap<Integer, Double> spendingByAge,
                               double benchmarkExpenditure, OptionalDouble fixedAgeExpenditure,
                               OptionalDouble ageingEffect) {
            this.year = year;
            this.totalPopulation = totalPopulation;
            this.spendingByAge = Collections.unmodifiableSortedMap(new TreeMap<>(spendingByAge));
            this.benchmarkExpenditure = benchmarkExpenditure;
            this.fixedAgeExpenditure = fixedAgeExpenditure;
            this.ageingEffect = ageingEffect;
        }

        public int getYear() {
            return year;
        }

        public double getTotalPopulation() {
            return totalPopulation;
        }

        /** Actual ages, including 102+, rather than pooled cost-lookup ages. */
        public SortedMap<Integer, Double> getSpendingByAge() {
            return spendingByAge;
        }

        public double getBenchmarkExpenditure() {
            return benchmarkExpenditure;
        }

        /** Empty if this calculator has not observed the run's 2028 population. */
        public OptionalDouble getFixedAgeExpenditure() {
            return fixedAgeExpenditure;
        }

        /** Benchmark less fixed-age expenditure; empty without a 2028 baseline. */
        public OptionalDouble getAgeingEffect() {
            return ageingEffect;
        }
    }
}
