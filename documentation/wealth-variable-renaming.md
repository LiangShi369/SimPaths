# Wealth variable names

The following Java names align the pension attributes with the codebook's `wealth` prefix. Economic definitions and calculations are unchanged.

| Previous Java name | Current Java name | Declaring class |
|---|---|---|
| `contRateOPEe` | `wealthPensOpEeContRate` | `Person`, `PrivatePension` |
| `contRateOPEr` | `wealthPensOpErContRate` | `Person`, `PrivatePension` |
| `contRatePP` | `wealthPensPpContRate` | `Person`, `PrivatePension` |
| `memberOP` | `wealthPensOpMemberFlag` | `PrivatePension` |
| `memberPP` | `wealthPensPpMemberFlag` | `PrivatePension` |
| `isPensioner` | `wealthPensAccessFlag` | `PrivatePension` |
| `lumpSumPayment` | `wealthPensLumpSumYear` | `PrivatePension` |

Accessors use the new field names, for example `getWealthPensOpEeContRate()` and `isWealthPensAccessFlag()`. The abbreviations are `pens` for pension, `op` for occupational pension, `pp` for personal pension, `ee` for employee, `er` for employer and `cont` for contribution. Contribution rates remain fractions of gross employment earnings. Pension access is distinct from labour-market retirement and state-pension eligibility. The lump sum is the payment in the current model year.

## Initial populations and databases

The Stata do-files and their CSV column names remain unchanged. `Parameters.PERSON_VARIABLES_INITIAL` continues to request `contRateOPEe`, `contRateOPEr` and `contRatePP`. Explicit `@Column` mappings on the renamed `Person` fields preserve these database column names.

Existing initial-population CSVs, input databases and processed-population caches therefore retain their existing contribution-rate schema. This rename requires no data regeneration or database migration. Input CSVs should continue to use the legacy column names; manually replacing their headers with the new Java names is not supported by this change.

## Outputs and consumers

The `Person.csv` exporter uses Java field names. Its three contribution-rate columns now use `wealthPensOpEeContRate`, `wealthPensOpErContRate` and `wealthPensPpContRate`. Downstream scripts reading these columns must use the new names. Database persistence continues to use the legacy column names through the explicit mappings.

The other four renamed fields remain nested in the transient `PrivatePension` object. They do not become separate `Person.csv` columns through this rename. Regression coefficient labels and equations retain their existing names and meanings.

The reviewed variable codebook is included in [SimPaths_Variable_Codebook.xlsx](SimPaths_Variable_Codebook.xlsx).

## Verification

- `mvn -B test`: 151 tests passed, including the database compatibility test that reads and updates legacy columns through the renamed Java fields.
- `mvn -B -DskipTests package`: passed.
- A UK 2019–2020 comparison against the unmodified base commit used model seed 100, target population 2,000 and the same processed training population. Fifteen of sixteen CSV files matched exactly after translating the three renamed contribution-rate headers, including every Person record, all wealth/inheritance outputs and all aggregate statistics.
- `BenefitUnit.csv` also matched in every field except `yBenLegacyReceivedFlag` and `yBenUCReceivedFlag`. The same two flags varied between two identically configured unmodified baseline runs. Exact equality of these flags is therefore a pre-existing repeatability limitation. No tax-benefit or random-number logic was changed.
- The final comparison used `-Djava.util.concurrent.ForkJoinPool.common.parallelism=0`; this temporary validation setting did not remove the flag variability. Fresh and cached population initialisations were kept separate for the comparison.
- All initial-population do-files remain unchanged. The reviewed codebook was added in a separate documentation commit.
