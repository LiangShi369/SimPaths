# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

For Codex-specific instructions, see `AGENTS.md`. Codex may use this file as repository background, but `AGENTS.md` remains the controlling instruction file for Codex.

## Repository Path

The repository can be checked out to different local paths. On this workstation, the project root is commonly:

```text
D:\CeMPA\SimPaths
```

When paths contain spaces, wrap them in double quotes in shell commands, for example:

```powershell
cd "D:\CeMPA\SimPaths"
mvn -f "D:\CeMPA\SimPaths\pom.xml" compile
```

## Build & Run Commands

```powershell
# Compile only
mvn compile

# Run unit tests, excluding integration tests
mvn test

# Run a single test class
mvn test -Dtest=MahalanobisDistanceTest

# Run a single test method
mvn test -Dtest=PersonTest#methodName

# Run integration tests, requiring pre-built JARs
mvn failsafe:integration-test

# Build shaded JARs, producing singlerun.jar and multirun.jar in the project root
mvn package

# Run simulation headless via multirun.jar, using config/default.yml
java -jar multirun.jar -config config/default.yml

# Run database setup only
java -jar multirun.jar -DBSetup -config config/default.yml

# Run with GUI, single run
java -jar singlerun.jar
```

Java 19 is required, as configured in `pom.xml`. Dependencies are pulled from Maven Central and JitPack for JAS-mine.

## Architecture Overview

SimPaths is a **dynamic microsimulation model** built on the [JAS-mine](https://github.com/jasmineRepo) simulation framework. It projects individual and household life histories year by year across labour market, health, demographics, social care, and taxes/benefits domains.

### Simulation Entity Hierarchy

```text
Household
  - groups BenefitUnits sharing a dwelling, including parent-adult-child links
BenefitUnit
  - the tax/benefit assessment unit, either couple or single plus dependent children
Person
  - the individual agent; all life-course state is stored here
```

All three are JPA `@Entity` classes persisted via Hibernate/H2. Each carries a `PanelEntityKey`, made up of `id`, `simulation_time`, and `simulation_run`.

### Simulation Lifecycle

`SimPathsModel`, which extends `AbstractSimulationManager`, owns the main simulation loop. Each year it fires ordered events that update entities via `EventListener.onEvent()`. The sequence covers: mortality -> union formation/dissolution -> fertility -> education -> labour market -> taxes/benefits -> health -> social care -> statistics collection.

### Key Classes To Know

| Class | Role |
|---|---|
| `SimPathsModel` | Orchestrates yearly event schedule; holds all population lists |
| `SimPathsStart` | Entry point for single runs, with GUI or headless execution |
| `SimPathsMultiRun` | Entry point for multi-run and sensitivity analysis |
| `SimPathsCollector` | Collects and exports output statistics each year |
| `Parameters` | Static store for model parameters and regression coefficients loaded from Excel |
| `ManagerRegressions` | Routes regression evaluation calls to the correct JAS-mine regression objects |
| `Person` / `BenefitUnit` | Core simulation agents; most behavioural logic lives here |

### Tax-Benefit Imputation (`model/taxes/`)

Tax and benefit outcomes are not calculated analytically. Instead, the model uses **statistical matching**: simulated households are matched to a pre-built donor database derived from EUROMOD/UKMOD output. `TaxDonorDataParser` builds this database; `DonorTaxImputation` performs the matching using a nearest-neighbour key function from `KeyFunction1` to `KeyFunction4`.

### Intertemporal Optimisation (`model/decisions/`)

An optional backward-induction module solves for optimal lifetime consumption and labour supply. `ManagerSolveGrids` runs the solution over a multi-dimensional state-space grid covering age, health, education, pension, region, and other dimensions. `ManagerPopulateGrids` sets up the grid geometry. This is computationally expensive and disabled by default with `enableIntertemporalOptimisations: false`.

### Regression System

All behavioural equations are estimated externally and loaded at startup from Excel files in `input/`. `Parameters` loads them via JAS-mine's `ExcelAssistant` and `MultiKeyCoefficientMap`. `RegressionName` names every equation; `ManagerRegressions` dispatches calls by type, including linear, probit, ordered probit, and multinomial logit.

### Alignment

Several demographic processes are aligned to external targets, including ONS projections and LFS shares, via `ActivityAlignmentV2`, `FertilityAlignment`, `PartnershipAlignment`, `InSchoolAlignment`, and `SocialCareAlignment`. Alignment factors are exported to `AlignmentAdjustmentFactors1.csv` each run.

### Configuration

Runs are configured via YAML files in `config/`. `default.yml` documents all available keys with their defaults. CLI flags override YAML values. `SimPathsMultiRun` reads the YAML and reflectively sets fields on `SimPathsModel`, `SimPathsCollector`, and `Parameters`.

### Output

Output is written to `output/<run-timestamp>/csv/` by `SimPathsCollector`. Key files include:

- `Statistics1.csv`: income distribution, including Gini, percentiles, and S-Index
- `Statistics2.csv`: demographic validation, including partnership, employment, and health by age/gender
- `EmploymentStatistics.csv` and `HealthStatistics.csv`: domain-specific time series
- `AlignmentAdjustmentFactors1.csv`: alignment diagnostics

### Integration Tests

`RunSimPathsIntegrationTest` runs the full simulation end to end using the built JARs and compares CSV output against reference files in `src/test/java/simpaths/integrationtest/expected/`. If a substantive change shifts the output, update the expected files and commit them.

## Domain Knowledge

This section records design principles and non-obvious rationale accumulated through development. Update it as new insights emerge.

### Price Levels: SimPaths vs. The Tax Donor Database

All SimPaths financial variables are stored in **real 2015 prices** using `BASE_PRICE_YEAR`. The tax donor database, however, is denominated in **nominal prices of the respective policy year**.

Tax donor matching therefore requires a two-step price bridge:

1. Inflate SimPaths income to policy-year prices before matching.
2. Deflate the imputed financial values from the matched donor record back to 2015 prices.

The default uprating series is `TimeSeriesVariable.Inflation`, sourced from the `UK_inflation` worksheet in `input/time_series_factor.xlsx`. An alternative option, added in 2026-04, allows wage growth to be used instead of price growth for the initial matching step, controlled via a config flag.
