# First-death inheritance rule: historical design

**Status:** Historical design record. This first-death rule was implemented and checked in a small 2021–2070 run, but has been replaced by the single [pooled baseline rule](wealth-inheritance-baseline-design.md). It is no longer selectable in the current code.  
**Purpose:** Preserve the earlier assumptions and diagnostic evidence so past output can be interpreted; future demographic scenario runs use the pooled rule.

## What the rule represents

The non-pension allocation uses a simplified **England-and-Wales intestacy proxy** for every modelled UK couple. SimPaths records a couple, but does not distinguish a spouse or civil partner from another cohabiting partner. For this feature, every surviving modelled partner is treated as spouse-equivalent. The proxy applies in Scotland and Northern Ireland too; it is not a claim that succession law is identical throughout the UK.

The rule also assumes that each adult owns half of a couple's net non-pension wealth and that the deceased's share of a jointly occupied home can enter the inheritance calculation. This permits a child to acquire a deferred claim while the survivor remains in the home. It is a model assumption about beneficial ownership, not a reconstruction of a household's actual title or will. A home held as a beneficial joint tenancy may instead pass directly to its surviving owner under the law.

The **remaining, unaccessed private pension pot** is included when calculating the model's heir shares. Applying those shares to the pot as a lump-sum death benefit is an agreed simulation convention. Real pension beneficiaries depend on nominations and scheme rules; the statutory legacy does not itself determine their pension payments. An annuity already in payment has no remaining pot to distribute in this version. No present value of future annuity income is created at death.

Inheritance is a capital transfer. It is not labour earnings, pension income, or disposable income in the year of death. Subsequent returns and consumption can respond through the recipient's wealth state in later years.

## Estate and recipient shares

Net mortgages and unsecured debts against assets before calculating a distributable amount; this calculation alone does not erase a surviving unit's debt. The deceased owns 50% of the couple's net non-pension balance, or 100% if they are the sole responsible adult. Deduct any existing deferred claims against that ownership before forming a later estate, without deducting a claim twice if it is already reflected in the recorded balance. The distributable non-pension amount cannot be negative. A negative balance does not become an inheritance charged to a child.

Let **N** be the deceased's nonnegative distributable net non-pension amount, **P** their nonnegative remaining unaccessed pension pot, and **E = N + P**. Calculate shares **once** on E; do not apply the statutory-legacy amount once to N and again to P.

| Surviving eligible heirs | Model allocation |
| --- | --- |
| Partner and at least one child | Children together receive `max(E - L, 0) / 2`, split equally. Partner receives the rest. |
| Partner, no children | Partner receives E. |
| Children, no partner | Children split E equally. |
| Neither | Record E as unallocated; do not silently discard it or assign it to an unrelated person. |

Here **L** is the model's statutory-legacy proxy. For example, if E is £500,000 and L is £322,000 in the *same price year's pounds*, the partner receives £411,000 and the children share £89,000. Apply the resulting partner and child percentages to N and P separately; each recipient's share of P is paid as a lump sum. The £322,000 figure is the England-and-Wales statutory amount from July 2023, but including P in E is the simulation convention described above.

For a more detailed example, suppose that £400,000 of E is net non-pension wealth and £100,000 is an unaccessed pension pot. The children's group share is 17.8% of E: £71,200 of non-pension wealth and £17,800 of pension pot. If the deceased's net financial share provides only £20,000, pay that cash immediately and record a £51,200 deferred housing claim. This example uses a common price year for every amount; the actual implementation compares values in 2015-price pounds.

Use the historical nominal statutory amount in early model years (£270,000 until 2022; £322,000 from the end-of-2023 death event onward). From 2026, assume the nominal amount rises each year with the model's inflation index: `L_nominal(y) = 322000 * I(y) / I(2026)`. Convert this nominal amount to SimPaths' 2015-price pounds before comparison with E. This gives a constant real threshold after 2026. Annual indexation is a projection assumption, not a prediction of future statutory orders.

## Homes, immediate payments, and deferred claims

Pay a child's non-pension share first from the deceased's positive net financial share. Any remaining entitlement attributable to the deceased's net housing equity becomes a **fixed claim in 2015-price pounds** on the occupied home. The partner remains in the home, with the modelled homeownership and housing state unchanged at the first death. The claim is an illiquid asset for the child and an equal liability against the surviving homeowner's net wealth. It is not spendable cash or an investment-income-producing financial balance.

Pay the claim on the first of: a modelled sale or loss of that home, dissolution of the survivor's housing benefit unit that would otherwise lose the obligation, or the surviving partner's death. At that point, assume the home is sold if needed and pay the claim from net proceeds, after the mortgage; any remaining estate assets can also meet it. If available assets are insufficient, record the unpaid shortfall and do not create wealth to fund it. A claim still outstanding in the final simulation year remains recorded; the simulation end does not trigger an artificial sale.

The claim remains fixed in **real** terms. It does not automatically share in later house-price appreciation or mortgage repayment. This is a deferred cash entitlement, rather than a fractional ownership interest. If an heir dies before payment, their claim becomes part of their own estate and must be settled through the same inheritance process, without duplication.

The gross home value can remain in the survivor's benefit-unit housing state for occupancy and housing projection. For reporting **net wealth by age or benefit unit**, subtract outstanding claims from the survivor and add the same amounts to the children's illiquid-claim balances. Report financial wealth, net housing equity, deferred-claim assets, and deferred-claim liabilities separately so that the total is not double-counted.

### Keeping the wealth accounts consistent

Calculate each estate and its recipients before changing any balances, then apply the result as one recorded transaction. An immediate adult receipt must update both the recipient's personal non-pension balance and the financial balance of their benefit unit. A pension death benefit becomes an inherited lump-sum financial balance; it is not a continuing pension pot or annual pension income. If the recipient is a minor, hold their receipt in a separate personal, illiquid balance until age 18 rather than adding it to the guardian's spendable funds. Carry these changes into the next year's lagged wealth state so that a later move, partnership, or separation neither loses nor duplicates the inheritance.

Keep each deferred housing claim outside the spendable financial balance. Record the child or later heir's claim asset and the matching liability attached to the occupied home, and include those two entries with opposite signs in reported total net wealth. The claim must generate neither investment income nor current consumption resources. At sale, loss of the home, dissolution of the homeowner's unit, or the homeowner's death, settle the claim from available estate resources and record any shortfall. Reconcile the estate transaction and the population-wide claim assets and liabilities after every annual settlement.

## Identifying heirs and handling deaths

Use the recorded mother and father relationships to identify **all linked living children**, including adults in other benefit units. A child's age or location within the simulated population does not change their share. A minor's receipt is held as a personal, illiquid balance until age 18; it can appear in reported net wealth but must not enter their guardian's spendable financial balance. If a child died earlier, their living descendants take that child's branch share where a reliable link exists. Any share for a child who has left the simulated population or cannot be linked to a recipient is recorded as an out-of-sample or unidentified-heir transfer, not reassigned to another heir.

### Family links during population cloning

The 2021 **training CSV**, before the model resamples its starting population, has 11,561 recorded positive mother/father references. Of these, 11,301 point to another person in that file: 7,432 are within the same benefit unit, 3,756 cross benefit units within the same household, and 113 cross households. The remaining 260 point to a parent absent from the file. These are *references*, not counts of distinct children or estates. They describe the training CSV, not necessarily the loaded simulation population; repeat the audit after population construction and for immigrant clones.

Clone every benefit unit in a household, build a map from each source person ID to the corresponding new person ID, then remap `idMother` and `idFather` across the **whole cloned household**. This preserves the 3,756 cross-benefit-unit, same-household references as well as links within a benefit unit. Record parents once at birth. Preserve those ancestry links when a child moves out or adults in their benefit unit change: the current annual attribute update overwrites under-18 parent IDs with the current benefit-unit adults. If that update serves a caregiving or custody purpose elsewhere, retain that information separately rather than replacing ancestry. Check its callers, including household moves and union matching, before changing it.

Source households with a cross-household parent link are currently sampled independently, so several clones of a source parent or child may exist. Link a cross-household pair only when the sampled copies have an unambiguous, one-to-one relationship; do not choose an arbitrary clone. Record unresolved links and any affected inheritance as an unidentified or out-of-sample heir transfer under the rule above. Audit both the **number and value of estates** affected by unresolved links; a small number of references need not imply a small financial effect. If that effect is material, preserve linked household groups during sampling before using the mechanism for substantive comparisons. Missing source relationships cannot be recovered by code alone.

Report the post-cloning counts of valid and unresolved parent links and the value of unresolved or out-of-sample transfers for each scenario. The transfer ledger records estate values by recipient and reason; deaths with identified children can be derived by joining it to the person output.

Settle natural deaths and `DeathAlignment` exits; do not treat emigration as death. Gather all exits for the year before allocating estates. A person marked dead cannot inherit from another person in that same settlement batch. If both partners die in the same year, there is no surviving partner for allocation. Use a deterministic order and explicit lineage rules if multiple related people die together. Settle estates after mortality and population alignment but before the next year's garbage collection removes people and empty units.

## Tax and other boundaries

The existing non-pension wealth balance reflects income and taxes already modelled during life. This version applies **no additional inheritance tax, pension death-benefit tax, probate expense, or sale cost**. The transferred pension pot is therefore a gross modelled pot; it must not be described as an after-tax death benefit. The mechanism adds no bequests to parents, siblings, charities, or government. Amounts with no eligible simulated recipient remain visible in an unallocated ledger.

This document specifies an inheritance mechanism, not a calibration of wills, pension nominations, home-title types, or inheritance-tax behaviour. Its effects on the ageing experiment should be measured by rerunning both scenarios, not appended to their existing outputs.

## Running and checking the implementation

This section describes outputs from runs made with the earlier first-death code. The current [baseline](../config/wealth_inequality_baseline_2070.yml) and [no-ageing](../config/wealth_inequality_no_ageing_2070.yml) configurations use pooled bequests automatically and no longer contain an inheritance switch. The starting population is rebuilt from raw input because the older processed-population cache does not retain the ancestry audit.

Three additional CSVs appear in each run's `csv` output folder:

| File | Contents |
| --- | --- |
| `inheritance_transfers.csv` | Year, deceased and recipient IDs, asset, immediate/deferred/paid status, value in 2015-price pounds, and reason. Missing recipients and unpaid claim shortfalls remain visible. |
| `inheritance_balances.csv` | Annual benefit-unit balances of deferred claim assets and liabilities, minor trusts, and reported total net wealth. |
| `inheritance_family_links.csv` | Valid, resolved cross-household, and unresolved parent references at load, plus immigrant-clone link counts. |

The earlier implementation used [`InheritanceModule`](../src/main/java/simpaths/model/InheritanceModule.java) and [`SimPathsModel`](../src/main/java/simpaths/model/SimPathsModel.java). Those files and [`inheritance_smoke.yml`](../config/inheritance_smoke.yml) now implement and check the pooled rule. The earlier statutory-split and deferred-housing-claim tests have been replaced by tests for the pooled rule.

The family data and the model's property state limit interpretation. Unresolved source children retain an estate branch but their share goes to the out-of-sample ledger. An immigrant clone's inherited trust or claim is copied; where its original counterparty is not cloned, the clone has an external asset or liability. A deferred claim is paid when the original home's benefit unit loses the home, the homeowner moves, or the homeowner dies. If the homeowner has moved to a new benefit unit, settlement draws on that person's current unit resources because the model does not track the proceeds of an individual property across moves. Inspect claim shortfalls and unresolved-heir *values* before using the feature for substantive results. This version does not model a demand, wage, or house-price response to transfers.

A 2021–2070 smoke run using the small test configuration completed with 3,893 starting simulated people. Its starting population had 1,789 valid and 125 unresolved parent references; immigrant cloning added 4,901 valid and 2,813 unresolved references over the run. The ledger recorded five housing-claim shortfalls totalling about £40,000 and about £1.99 billion of out-of-sample or unallocated transfers over all 50 years, in unscaled 2015-price pounds. The latter is **material**: many sampled deaths have no identified simulated heir, and unresolved links also matter. These figures are diagnostics of this small run, not a result for the UK or an estimate of the ageing effect. Both full scenario reruns need the same audit before inheritance-adjusted wealth findings are presented.

## Implementation and acceptance checks

1. Add a dedicated annual estate-settlement process after both death processes. Keep estate calculation and recipient selection separate from mutation of wealth stocks.
2. Repair and audit family IDs during population construction and immigration cloning, using a household-wide source-to-clone map. Preserve ancestry through annual updates and household moves. Index children by parent ID for settlement; never search for heirs only within the deceased's benefit unit.
3. Add persistent balances for a child's illiquid claim and the corresponding survivor liability, plus transfer records by year, deceased ID, recipient ID, asset type, immediate or deferred status, and exit reason.
4. Apply each estate as one transaction and update current and next-year wealth states consistently. In particular, reconcile recipients' personal non-pension balances with their benefit-unit balances so later union formation or separation cannot lose or duplicate an inheritance. Keep minors' receipts and housing claims outside spendable financial wealth.
5. Verify estate accounting for a surviving partner with and without children; an independent adult child; a minor; a home with and without a mortgage; a home sale before the survivor dies; an unaccessed pension pot and an annuity already in payment; two same-year deaths; an alignment death; a missing heir; and an insolvent estate.
6. For each settlement, verify that the deceased's distributable amount equals immediate receipts plus new deferred claims plus recorded out-of-sample or unallocated amounts, subject only to an explicitly recorded shortfall or write-off. Across the population, do not count a child's claim without the matching survivor liability. Check the value of estates affected by unresolved parent links. Compare baseline and no-ageing runs using the same feature flag and parameters.

Relevant code: [`SimPathsModel.buildSchedule`](../src/main/java/simpaths/model/SimPathsModel.java), [`Person`](../src/main/java/simpaths/model/Person.java), [`BenefitUnit`](../src/main/java/simpaths/model/BenefitUnit.java), [`PrivatePension`](../src/main/java/simpaths/model/person/PrivatePension.java), and [`WealthNonPension`](../src/main/java/simpaths/model/benefitunit/WealthNonPension.java).

Sources for the legal reference point: [HMRC intestacy overview](https://www.gov.uk/hmrc-internal-manuals/inheritance-tax-manual/ihtm12111), [HMRC statutory-legacy history](https://www.gov.uk/hmrc-internal-manuals/inheritance-tax-manual/ihtm12122), [GOV.UK joint property ownership](https://www.gov.uk/joint-property-ownership), and [GOV.UK pension death benefits](https://www.gov.uk/tax-on-pension-death-benefits).
