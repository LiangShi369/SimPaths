# Matteo's pooled bequest proposal

**Status:** Discussion record. See the [SimPaths pooled baseline design](wealth-inheritance-baseline-design.md) for the implemented rule and the [historical first-death design](wealth-inheritance-first-death-alternative.md) for the superseded code.

## Main idea

Non-pension wealth is held by a **benefit unit**, rather than separately by each adult. If one adult in a couple dies, the survivor retains the unit's non-pension wealth. When no responsible adult survives in that unit, its remaining non-pension wealth passes to identified children. The receipt is a windfall added to the recipients' benefit units' **non-housing, non-pension financial wealth**. The idea therefore assumes that any net housing equity distributed at the last death is realised as cash.

The intended first-death assumption is **full ownership passes to the survivor**. Children of the first partner remain *eligible* to share whatever wealth is left when the survivor dies; they do not receive a fixed protected amount at the first death. The eligibility rule must remember children of a deceased partner even if the survivor later forms another couple. This is a model convention about pooled wealth, not a statement of UK succession law.

## If no child can be identified

Matteo proposes assigning the remaining wealth to `n` randomly selected simulated beneficiaries, initially trying `n = 2` or `n = 3`. A candidate would be younger than the last surviving adult by a uniform draw between 20 and 30 years, in the same region and at the same education level. The user proposes using the **last survivor's education** for this match and relaxing education by one adjacent level if an exact cell is empty. This is a synthetic-recipient rule: selected people need not be actual relatives.

SimPaths' education categories are `Low`, `Medium`, `High`, and `InEducation`. An operational rule should treat `Low`–`Medium`–`High` as the adjacent completed levels and treat `InEducation` separately; adding or subtracting one from an enum value would not be an appropriate education match. Candidate scarcity, duplicate recipients, minors, and cases with some known but other unlinked children also need explicit rules. The [baseline design](wealth-inheritance-baseline-design.md) proposes how to handle these.

Matteo also considered UKHLS Wave 15's `nchildlv` count of living children, but proposes **not** adding it for the current 2021-start simulation. A fixed `n` is a transparent initial assumption, not an estimate of actual family size.

The agreed [SimPaths baseline design](wealth-inheritance-baseline-design.md) sets `n = 2`, reserves a matched synthetic recipient for each child branch known in source data but unresolved after sampling, and gives equal shares to all recorded child branches carried through later partnerships. These are operational additions to the email's outline.

## Questions the email leaves outside its scope

The email concerns **non-pension wealth**. Pension pots belong to individuals and need their own death-benefit timing and beneficiary rule. It also does not specify how to preserve a deceased partner's children's eligibility across a new partnership, how to handle debt or a negative unit balance, or how the random-recipient rule should respond to partly missing family links. Those choices matter for measured inequality and should be documented and audited before using the proposed rule for the ageing experiment.

Random matching keeps more of the deceased units' wealth inside the simulated population than the earlier unallocated-ledger rule. It also creates synthetic intergenerational transfers whose distribution depends on `n` and the matching variables. Results should report the number and value of such transfers and test sensitivity to the matching assumptions.
