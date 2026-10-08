package simpaths.model;

import simpaths.data.Parameters;
import simpaths.model.enums.Education;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.SampleExit;
import simpaths.model.person.PrivatePension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The wealth module's pooled bequest rule. Non-pension wealth stays with a
 * surviving responsible adult; the remaining unit estate passes at the last
 * adult's death. A remaining modelled pension pot may be settled before its
 * owner's retirement.
 */
public final class InheritanceModule {

    private static final double EPS = 1.0e-6;
    private final Map<Long, Person> knownPeople = new HashMap<>();
    private final Map<Long, List<Person>> childrenByParent = new HashMap<>();
    private final Map<Long, Set<Long>> sourceChildren = new HashMap<>();
    private final Map<Long, Map<String, ChildBranch>> carriedBranches = new HashMap<>();
    private final Map<Long, Double> minorTrusts = new HashMap<>();
    private final Set<Long> settledDeaths = new HashSet<>();
    private final List<Transfer> transfers = new ArrayList<>();
    private final List<BalanceSnapshot> balanceSnapshots = new ArrayList<>();
    private final List<SyntheticMatch> syntheticMatches = new ArrayList<>();
    private final List<AlignmentFlow> alignmentFlows = new ArrayList<>();
    private final List<DeathEvent> deathEvents = new ArrayList<>();
    private Set<Long> currentPopulationIds = Set.of();
    private FamilyLinkAudit familyLinkAudit;
    private long seed = 606L;
    private int syntheticRecipientCount = 2;
    private boolean transferDcPensionAtDeath = false;

    public record Transfer(int year, long deceasedId, Long recipientId, String asset,
                           String timing, double value, String reason, String deathCause) { }
    public record DeathEvent(int year, long deceasedId, long benefitUnitId,
                             SampleExit deathCause) { }
    public record BalanceSnapshot(int year, long benefitUnitId, double claimAssets,
                                  double claimLiabilities, double minorTrustAssets,
                                  double totalNetWealth) { }
    public record FamilyLinkAudit(int year, long validReferences,
                                  long resolvedAcrossHouseholds, long unresolvedReferences,
                                  long validImmigrantReferences,
                                  long unresolvedImmigrantReferences) { }
    public record SyntheticMatch(int year, long deceasedId, Long recipientId, String purpose,
                                 int intendedCount, int actualCount, int slot, int ageGap, String tier,
                                 double value) { }
    public record AlignmentFlow(int year, String event, long benefitUnitId, Long personId,
                                double financial, double housing, double pension,
                                double minorTrust) { }
    private record ChildBranch(String key, Long childId, String origin) { }
    private record Heir(Person person, double weight, String origin) { }
    private record Match(Person person, int ageGap, String tier) { }
    private record UnitSnapshot(double financial, double housing, int adults) {
        double net() { return financial + housing; }
    }

    public void setSeed(long seed) { this.seed = seed; }

    public void setSyntheticRecipientCount(int count) {
        if (count < 1)
            throw new IllegalArgumentException("At least one synthetic recipient is required");
        syntheticRecipientCount = count;
    }

    public int getSyntheticRecipientCount() { return syntheticRecipientCount; }

    public void setTransferDcPensionAtDeath(boolean enabled) {
        transferDcPensionAtDeath = enabled;
    }

    public boolean isTransferDcPensionAtDeath() { return transferDcPensionAtDeath; }

    /** Index source families before independent sampling obscures some links. */
    public void indexSourceFamilies(List<Household> inputHouseholds) {
        sourceChildren.clear();
        for (Household household : inputHouseholds)
            for (BenefitUnit unit : household.getBenefitUnits())
                for (Person child : unit.getMembers()) {
                    addSourceChild(child.getIdMother(), child.getId());
                    addSourceChild(child.getIdFather(), child.getId());
                }
    }

    private void addSourceChild(Long parentId, long childId) {
        if (parentId != null && parentId > 0 && parentId != childId)
            sourceChildren.computeIfAbsent(parentId, ignored -> new TreeSet<>()).add(childId);
    }

    public double getIlliquidAssetWealth(BenefitUnit unit) {
        double result = 0.0;
        for (Person person : unit.getMembers())
            if (SampleExit.NotYet.equals(person.getSampleExit()))
                result += minorTrusts.getOrDefault(person.getId(), 0.0);
        return result;
    }

    public List<Transfer> getTransfers() { return List.copyOf(transfers); }
    public List<AlignmentFlow> getAlignmentFlows() { return List.copyOf(alignmentFlows); }
    public List<DeathEvent> getDeathEvents() { return List.copyOf(deathEvents); }

    public void setFamilyLinkAudit(FamilyLinkAudit audit) { familyLinkAudit = audit; }

    public void updateImmigrantLinkAudit(long valid, long unresolved) {
        if (familyLinkAudit == null)
            throw new IllegalStateException("Starting family-link audit is unavailable");
        familyLinkAudit = new FamilyLinkAudit(familyLinkAudit.year(),
                familyLinkAudit.validReferences(), familyLinkAudit.resolvedAcrossHouseholds(),
                familyLinkAudit.unresolvedReferences(), valid, unresolved);
    }

    /** Copy trusts and lineage when population alignment clones an existing unit. */
    public void cloneBalances(Map<Long, Person> clonedPersons) {
        for (Map.Entry<Long, Person> entry : clonedPersons.entrySet()) {
            double trust = minorTrusts.getOrDefault(entry.getKey(), 0.0);
            if (trust > EPS)
                minorTrusts.put(entry.getValue().getId(), trust);
            Map<String, ChildBranch> original = carriedBranches.get(entry.getKey());
            if (original == null)
                continue;
            Map<String, ChildBranch> copy = new LinkedHashMap<>();
            for (ChildBranch branch : original.values()) {
                Person linked = branch.childId() == null ? null : clonedPersons.get(branch.childId());
                ChildBranch cloned = linked == null
                        ? new ChildBranch(branch.key(), null, "unresolved_cloned_child")
                        : new ChildBranch("person:" + linked.getId(), linked.getId(), branch.origin());
                copy.put(cloned.key(), cloned);
            }
            carriedBranches.put(entry.getValue().getId(), copy);
        }
    }

    /** Wealth brought into the simulated sample by an alignment-created unit. */
    public void recordAlignmentEntry(int year, BenefitUnit unit) {
        UnitSnapshot stock = snapshot(unit);
        alignmentFlow(year, "unit_entry", unit, null, stock.financial(),
                stock.housing(), unit.getWealthPensValue(false), trustStock(unit));
    }

    public void settleYear(int year, Set<Person> persons, Set<BenefitUnit> units) {
        Set<Long> activeIds = new HashSet<>();
        for (Person person : persons) {
            activeIds.add(person.getId());
            if (knownPeople.putIfAbsent(person.getId(), person) == null) {
                registerChild(person.getIdMother(), person);
                if (!Objects.equals(person.getIdMother(), person.getIdFather()))
                    registerChild(person.getIdFather(), person);
            }
        }
        currentPopulationIds = activeIds;
        externaliseExitedTrusts(year);
        releaseAdultTrusts(year);

        List<Person> exits = persons.stream()
                .filter(person -> isDeath(person) || isEmigrationExit(person))
                .filter(person -> !settledDeaths.contains(person.getId()))
                .sorted(Comparator.comparingLong(Person::getId)).toList();
        Map<BenefitUnit, UnitSnapshot> snapshots = new LinkedHashMap<>();
        for (Person exited : exits)
            snapshots.computeIfAbsent(exited.getBenefitUnit(), this::snapshot);

        // Both mortality and residual population-alignment deaths settle under
        // the same rule. Only emigration removes estate stock from the sample.
        for (Person exited : exits) {
            BenefitUnit unit = exited.getBenefitUnit();
            Person partner = livingPartner(exited);
            if (isDeath(exited)) {
                deathEvents.add(new DeathEvent(year, exited.getId(), unit.getId(),
                        exited.getSampleExit()));
                settlePension(year, exited, partner);
            } else {
                double pensionStock = exited.getWealthPensValue(false);
                alignmentFlow(year, "emigration_alignment_person_exit", unit, exited,
                        0.0, 0.0, pensionStock, 0.0);
                unit.removeDeceasedPensionStock(exited);
            }
            if (partner != null) {
                double retained = snapshots.get(unit).net() /
                        snapshots.get(unit).adults();
                partner.addInheritanceToPersonalFinancialBalance(retained);
                if (isDeath(exited)) {
                    mergeBranches(partner.getId(), allBranches(exited));
                    record(year, exited, partner, "nonpension_wealth", "retained_in_unit",
                            Math.max(0.0, retained), "surviving_partner");
                } else {
                    // The pooled balance is already in the surviving unit;
                    // this only keeps the survivor's personal balance aligned.
                    alignmentFlow(year, "partner_retained_bookkeeping", unit, exited,
                            retained, 0.0, 0.0, 0.0);
                }
            }
            settledDeaths.add(exited.getId());
        }

        for (Map.Entry<BenefitUnit, UnitSnapshot> entry : snapshots.entrySet()) {
            BenefitUnit unit = entry.getKey();
            if (hasLivingResponsibleAdult(unit))
                continue;
            List<Person> unitExits = exits.stream().filter(p -> p.getBenefitUnit() == unit).toList();
            if (unitExits.isEmpty())
                continue;
            if (unitExits.stream().anyMatch(this::isEmigrationExit)) {
                UnitSnapshot stock = entry.getValue();
                alignmentFlow(year, "unit_emigration", unit, null,
                        stock.financial(), stock.housing(),
                        unit.getWealthPensValue(false), 0.0);
                unit.clearEstateWealth();
                continue;
            }
            Person estateAdult = estateReferenceAdult(unit, unitExits);
            Map<String, ChildBranch> branches = new LinkedHashMap<>();
            for (Person deceased : unitExits)
                branches.putAll(allBranches(deceased));
            double raw = entry.getValue().net();
            double estate = Math.max(0.0, raw);
            if (raw < -EPS)
                record(year, estateAdult, null, "nonpension_wealth", "write_off", -raw,
                        "insolvent_unit");
            if (estate > EPS)
                distribute(year, estateAdult, estate, branches, "nonpension_estate");
            unit.clearEstateWealth();
        }

        for (Person exited : exits)
            carriedBranches.remove(exited.getId());
        moveOrphanTrustsOutOfSample(year, snapshots.keySet());
        for (BenefitUnit unit : units)
            unit.updateTotalWealth();
        captureBalances(year, units);
    }

    private void registerChild(Long parentId, Person child) {
        if (parentId != null)
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(child);
    }

    private UnitSnapshot snapshot(BenefitUnit unit) {
        var wealth = unit.getWealthNonPension();
        int adults = (unit.getMale() == null ? 0 : 1) + (unit.getFemale() == null ? 0 : 1);
        if (adults < 1)
            throw new IllegalStateException("Estate has no responsible adult in unit " + unit.getId());
        return new UnitSnapshot(wealth.getWealthFinancial().getValue(),
                wealth.getWealthHousing().getWealthNetHousing(), adults);
    }

    private Person livingPartner(Person deceased) {
        Person partner = deceased.getPartner();
        return partner != null && isLiving(partner) ? partner : null;
    }

    private boolean hasLivingResponsibleAdult(BenefitUnit unit) {
        return unit.getMale() != null && isLiving(unit.getMale()) ||
                unit.getFemale() != null && isLiving(unit.getFemale());
    }

    private Person estateReferenceAdult(BenefitUnit unit, List<Person> unitExits) {
        // Annual settlement has no within-year death order. Preserve the
        // deterministic highest-ID convention among deceased responsible
        // adults only; a dependent must never supply estate matching traits.
        return unitExits.stream().filter(this::isDeath)
                .filter(p -> p == unit.getMale() || p == unit.getFemale())
                .max(Comparator.comparingLong(Person::getId))
                .orElseThrow(() -> new IllegalStateException(
                        "Estate has no deceased responsible adult in unit " + unit.getId()));
    }

    private void settlePension(int year, Person deceased, Person partner) {
        PrivatePension pension = deceased.getPrivatePension();
        double pot = transferDcPensionAtDeath && !pension.isRetired() &&
                !Les_c4.Retired.equals(deceased.getLabC4())
                ? Math.max(0.0, pension.getRemainingDcFund()) : 0.0;
        deceased.getBenefitUnit().removeDeceasedPensionStock(deceased);
        if (pot <= EPS)
            return;
        if (partner != null) {
            creditCash(year, deceased, partner, pot, "unaccessed_pension_partner");
            return;
        }
        Person exitingPartner = deceased.getPartner();
        if (exitingPartner != null && isEmigrationExit(exitingPartner)) {
            // Mortality precedes alignment in the annual schedule. The partner
            // was alive at death but leaves the sample before settlement.
            alignmentFlow(year, "partner_pension_exit", deceased.getBenefitUnit(),
                    exitingPartner, 0.0, 0.0, pot, 0.0);
            return;
        }
        distribute(year, deceased, pot, ownBranches(deceased), "unaccessed_pension");
    }

    private Map<String, ChildBranch> allBranches(Person person) {
        Map<String, ChildBranch> result = ownBranches(person);
        result.putAll(carriedBranches.getOrDefault(person.getId(), Map.of()));
        return deduplicateSourceBranches(result);
    }

    /** Remove an unresolved source branch already represented by a linked child.
     * Keep distinct simulated people, including clones sharing a source ID.
     */
    private Map<String, ChildBranch> deduplicateSourceBranches(Map<String, ChildBranch> branches) {
        Map<String, ChildBranch> result = new LinkedHashMap<>(branches);
        Set<Long> linkedSources = new HashSet<>();
        for (ChildBranch branch : result.values()) {
            if (branch.childId() == null)
                continue;
            Person child = knownPeople.get(branch.childId());
            if (child != null && child.getInheritanceSourceId() != null)
                linkedSources.add(child.getInheritanceSourceId());
        }
        for (Long sourceId : linkedSources)
            result.remove("source:" + sourceId);
        return result;
    }

    private Map<String, ChildBranch> ownBranches(Person person) {
        Map<String, ChildBranch> result = new LinkedHashMap<>();
        Set<Long> representedSources = new HashSet<>();
        for (Person child : childrenByParent.getOrDefault(person.getId(), List.of())) {
            Long sourceId = child.getInheritanceSourceId();
            if (sourceId != null)
                representedSources.add(sourceId);
            ChildBranch branch = new ChildBranch("person:" + child.getId(), child.getId(),
                    "identified_child");
            result.put(branch.key(), branch);
        }
        Long sourceParent = person.getInheritanceSourceId();
        if (sourceParent != null)
            for (Long sourceChild : sourceChildren.getOrDefault(sourceParent, Set.of()))
                if (!representedSources.contains(sourceChild)) {
                    ChildBranch branch = new ChildBranch("source:" + sourceChild, null,
                            "unresolved_child");
                    result.put(branch.key(), branch);
                }
        return result;
    }

    private void mergeBranches(long recipientId, Map<String, ChildBranch> branches) {
        Map<String, ChildBranch> carried = carriedBranches.computeIfAbsent(recipientId,
                ignored -> new LinkedHashMap<>());
        carried.putAll(branches);
    }

    private void distribute(int year, Person deceased, double amount,
                            Map<String, ChildBranch> branches, String purpose) {
        // Parents' separate lists may represent their shared child by different
        // keys. Deduplicate again after pooling, before computing equal shares.
        branches = deduplicateSourceBranches(branches);
        List<List<Heir>> eligible = new ArrayList<>();
        for (ChildBranch branch : branches.values()) {
            List<Heir> heirs = resolveBranch(branch, new HashSet<>());
            if (!heirs.isEmpty())
                eligible.add(heirs);
        }
        Set<Long> reservedRecipients = new HashSet<>();
        for (List<Heir> branch : eligible)
            for (Heir heir : branch)
                if (heir.person() != null)
                    reservedRecipients.add(heir.person().getId());
        if (eligible.isEmpty()) {
            distributeSynthetic(year, deceased, amount, purpose,
                    syntheticRecipientCount, reservedRecipients);
            return;
        }
        double perBranch = amount / eligible.size();
        for (List<Heir> branch : eligible)
            for (Heir heir : branch) {
                double value = perBranch * heir.weight();
                if (heir.person() == null)
                    distributeSynthetic(year, deceased, value,
                            purpose + "_" + heir.origin(), 1, reservedRecipients);
                else
                    creditCash(year, deceased, heir.person(), value, purpose + "_child");
            }
    }

    private List<Heir> resolveBranch(ChildBranch branch, Set<Long> visited) {
        if (branch.childId() == null)
            return List.of(new Heir(null, 1.0,
                    branch.origin() + "_" + branch.key().replace(':', '_')));
        Person child = knownPeople.get(branch.childId());
        if (child == null || !visited.add(child.getId()))
            return List.of(new Heir(null, 1.0, "unresolved_child"));
        if (isLiving(child))
            return List.of(new Heir(child, 1.0, branch.origin()));
        if (!isDeath(child))
            return List.of(new Heir(null, 1.0, "out_of_sample_child"));
        // Representation follows the deceased child's own descendants, not
        // stepchildren carried from an unrelated later partner.
        Map<String, ChildBranch> descendants = ownBranches(child);
        if (descendants.isEmpty())
            return List.of();
        List<List<Heir>> livingBranches = new ArrayList<>();
        for (ChildBranch descendant : descendants.values()) {
            List<Heir> resolved = resolveBranch(descendant, new HashSet<>(visited));
            if (!resolved.isEmpty())
                livingBranches.add(resolved);
        }
        List<Heir> heirs = new ArrayList<>();
        for (List<Heir> descendant : livingBranches)
            for (Heir heir : descendant)
                heirs.add(new Heir(heir.person(),
                        heir.weight() / livingBranches.size(), heir.origin()));
        return heirs;
    }

    private void distributeSynthetic(int year, Person deceased, double amount,
                                     String purpose, int count, Set<Long> used) {
        // A childless person's remaining pension pot and final pooled estate
        // should go to the same stand-in beneficiaries when both are settled
        // at this death. Keep the payment purposes distinct in the ledger.
        String matchingPurpose = purpose.equals("unaccessed_pension")
                ? "nonpension_estate" : purpose;
        SplittableRandom rng = new SplittableRandom(seed ^ (deceased.getId() * 0x9E3779B97F4A7C15L)
                ^ ((long) year << 32) ^ matchingPurpose.hashCode());
        List<Match> matches = new ArrayList<>();
        for (int slot = 1; slot <= count; slot++) {
            int ageGap = 20 + rng.nextInt(11);
            Match match = matchRecipient(deceased, ageGap, used, rng);
            matches.add(match);
            double share = amount / count;
            if (match.person() == null) {
                record(year, deceased, null, "cash", "unmatched", share,
                        purpose + "_synthetic");
            } else {
                used.add(match.person().getId());
                creditCash(year, deceased, match.person(), share,
                        purpose + "_synthetic_" + match.tier());
            }
        }
        int actual = (int) matches.stream().filter(m -> m.person() != null).count();
        for (int slot = 1; slot <= count; slot++) {
            Match match = matches.get(slot - 1);
            syntheticMatches.add(new SyntheticMatch(year, deceased.getId(),
                    match.person() == null ? null : match.person().getId(), purpose,
                    count, actual, slot, match.ageGap(), match.tier(), amount / count));
        }
    }

    private Match matchRecipient(Person deceased, int ageGap, Set<Long> used,
                                 SplittableRandom rng) {
        int targetAge = deceased.getDemAge() - ageGap;
        List<Person> pool = knownPeople.values().stream()
                .filter(this::isLiving).filter(p -> p != deceased)
                .filter(p -> !used.contains(p.getId()))
                .sorted(Comparator.comparingLong(Person::getId)).toList();
        for (int tier = 0; tier <= 4; tier++) {
            final int currentTier = tier;
            List<Person> candidates = pool.stream().filter(p -> matchTier(
                    deceased, p, targetAge, currentTier)).toList();
            if (!candidates.isEmpty()) {
                if (tier == 3) {
                    int distance = candidates.stream().mapToInt(p ->
                            Math.abs(p.getDemAge() - targetAge)).min().orElseThrow();
                    candidates = candidates.stream().filter(p ->
                            Math.abs(p.getDemAge() - targetAge) == distance).toList();
                }
                List<Person> differentUnits = candidates.stream()
                        .filter(p -> p.getBenefitUnit() != deceased.getBenefitUnit()).toList();
                if (!differentUnits.isEmpty())
                    candidates = differentUnits;
                return new Match(candidates.get(rng.nextInt(candidates.size())), ageGap,
                        switch (tier) {
                            case 0 -> "same_education";
                            case 1 -> "adjacent_education";
                            case 2 -> "any_education";
                            case 3 -> "age_relaxed";
                            default -> "national";
                        });
            }
        }
        return new Match(null, ageGap, "unmatched");
    }

    private boolean matchTier(Person donor, Person candidate, int targetAge, int tier) {
        boolean region = Objects.equals(donor.getRegion(), candidate.getRegion());
        boolean age = candidate.getDemAge() == targetAge;
        Education donorEducation = donor.getEduHighestC4();
        Education candidateEducation = candidate.getEduHighestC4();
        if (tier == 0)
            return region && age && donorEducation == candidateEducation;
        if (tier == 1)
            return region && age && adjacentEducation(donorEducation, candidateEducation);
        if (tier == 2)
            return region && age;
        if (tier == 3)
            return region;
        return true;
    }

    private boolean adjacentEducation(Education a, Education b) {
        return a == Education.Low && b == Education.Medium ||
                a == Education.Medium && (b == Education.Low || b == Education.High) ||
                a == Education.High && b == Education.Medium;
    }

    private void creditCash(int year, Person deceased, Person recipient,
                            double value, String reason) {
        if (value <= EPS)
            return;
        if (!isLiving(recipient)) {
            record(year, deceased, recipient, "cash", "out_of_sample", value, reason);
        } else if (recipient.getDemAge() < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            minorTrusts.merge(recipient.getId(), value, Double::sum);
            record(year, deceased, recipient, "minor_trust", "deferred", value, reason);
        } else {
            recipient.getBenefitUnit().addInheritanceCash(value);
            recipient.addInheritanceToPersonalFinancialBalance(value);
            record(year, deceased, recipient, "cash", "immediate", value, reason);
        }
    }

    private void releaseAdultTrusts(int year) {
        for (Long id : new ArrayList<>(minorTrusts.keySet())) {
            Person holder = knownPeople.get(id);
            if (holder != null && isLiving(holder) &&
                    holder.getDemAge() >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                double value = minorTrusts.remove(id);
                holder.getBenefitUnit().addInheritanceCash(value);
                holder.addInheritanceToPersonalFinancialBalance(value);
                record(year, null, holder, "minor_trust", "released", value,
                        "age_of_majority");
            }
        }
    }

    private void externaliseExitedTrusts(int year) {
        for (Long id : new ArrayList<>(minorTrusts.keySet())) {
            Person holder = knownPeople.get(id);
            if (holder != null && !isLiving(holder)) {
                double value = minorTrusts.remove(id);
                record(year, null, holder, "minor_trust", "out_of_sample", value,
                        isDeath(holder) ? "minor_died" : "holder_left_population");
                if (isEmigrationExit(holder))
                    alignmentFlow(year, "minor_trust_alignment_exit",
                            holder.getBenefitUnit(), holder, 0.0, 0.0, 0.0, value);
            }
        }
    }

    private void moveOrphanTrustsOutOfSample(int year, Set<BenefitUnit> affected) {
        for (BenefitUnit unit : affected) {
            if (hasLivingResponsibleAdult(unit))
                continue;
            for (Person person : unit.getMembers()) {
                if (!isLiving(person))
                    continue;
                Double trust = minorTrusts.remove(person.getId());
                if (trust != null)
                    record(year, null, person, "minor_trust", "out_of_sample", trust,
                            "no_responsible_adult_in_unit");
                if (trust != null && unit.getMembers().stream().anyMatch(this::isEmigrationExit))
                    alignmentFlow(year, "orphan_trust_alignment_exit", unit, person,
                            0.0, 0.0, 0.0, trust);
            }
        }
    }

    private void captureBalances(int year, Set<BenefitUnit> units) {
        for (BenefitUnit unit : units) {
            double trusts = 0.0;
            for (Person person : unit.getMembers())
                if (isLiving(person))
                    trusts += minorTrusts.getOrDefault(person.getId(), 0.0);
            balanceSnapshots.add(new BalanceSnapshot(year, unit.getId(), 0.0, 0.0,
                    trusts, unit.getWealthTotValue(false)));
        }
    }

    private boolean isLiving(Person person) {
        return person != null && currentPopulationIds.contains(person.getId()) &&
                SampleExit.NotYet.equals(person.getSampleExit());
    }

    private boolean isDeath(Person person) {
        return SampleExit.Death.equals(person.getSampleExit()) ||
                SampleExit.DeathAlignment.equals(person.getSampleExit());
    }

    private boolean isEmigrationExit(Person person) {
        return SampleExit.EmigrationAlignment.equals(person.getSampleExit());
    }

    private double trustStock(BenefitUnit unit) {
        double total = 0.0;
        for (Person person : unit.getMembers())
            total += minorTrusts.getOrDefault(person.getId(), 0.0);
        return total;
    }

    private void alignmentFlow(int year, String event, BenefitUnit unit, Person person,
                               double financial, double housing, double pension,
                               double minorTrust) {
        if (!Double.isFinite(financial) || !Double.isFinite(housing) ||
                !Double.isFinite(pension) || !Double.isFinite(minorTrust))
            throw new IllegalArgumentException("Non-finite population-alignment wealth flow");
        alignmentFlows.add(new AlignmentFlow(year, event, unit.getId(),
                person == null ? null : person.getId(), financial, housing,
                pension, minorTrust));
    }

    private void record(int year, Person deceased, Person recipient, String asset,
                        String timing, double value, String reason) {
        if (!Double.isFinite(value) || value < -EPS)
            throw new IllegalArgumentException("Invalid inheritance ledger value");
        transfers.add(new Transfer(year, deceased == null ? -1L : deceased.getId(),
                recipient == null ? null : recipient.getId(), asset, timing, value, reason,
                deceased == null ? "not_applicable" : deceased.getSampleExit().name()));
    }

    public void writeLedger(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        List<String> lines = new ArrayList<>();
        lines.add("year,deceased_id,recipient_id,asset,timing,value_2015_gbp,reason,death_cause");
        for (Transfer t : transfers)
            lines.add(t.year() + "," + t.deceasedId() + "," +
                    (t.recipientId() == null ? "" : t.recipientId()) + "," +
                    t.asset() + "," + t.timing() + "," + t.value() + "," + t.reason() +
                    "," + t.deathCause());
        Files.write(outputDirectory.resolve("inheritance_transfers.csv"), lines);

        lines.clear();
        lines.add("year,deceased_id,benefit_unit_id,death_cause");
        for (DeathEvent death : deathEvents)
            lines.add(death.year() + "," + death.deceasedId() + "," +
                    death.benefitUnitId() + "," + death.deathCause());
        Files.write(outputDirectory.resolve("inheritance_death_events.csv"), lines);

        lines.clear();
        lines.add("year,benefit_unit_id,claim_assets_2015_gbp,claim_liabilities_2015_gbp," +
                "minor_trust_assets_2015_gbp,total_net_wealth_2015_gbp");
        for (BalanceSnapshot b : balanceSnapshots)
            lines.add(b.year() + "," + b.benefitUnitId() + ",0,0," +
                    b.minorTrustAssets() + "," + b.totalNetWealth());
        Files.write(outputDirectory.resolve("inheritance_balances.csv"), lines);

        lines.clear();
        lines.add("year,valid_parent_references,resolved_cross_household_references," +
                "unresolved_parent_references,valid_immigrant_parent_references," +
                "unresolved_immigrant_parent_references");
        if (familyLinkAudit != null)
            lines.add(familyLinkAudit.year() + "," + familyLinkAudit.validReferences() + "," +
                    familyLinkAudit.resolvedAcrossHouseholds() + "," +
                    familyLinkAudit.unresolvedReferences() + "," +
                    familyLinkAudit.validImmigrantReferences() + "," +
                    familyLinkAudit.unresolvedImmigrantReferences());
        Files.write(outputDirectory.resolve("inheritance_family_links.csv"), lines);

        lines.clear();
        lines.add("year,deceased_id,recipient_id,purpose,intended_n,actual_n,slot,age_gap,match_tier,value_2015_gbp");
        for (SyntheticMatch m : syntheticMatches)
            lines.add(m.year() + "," + m.deceasedId() + "," +
                    (m.recipientId() == null ? "" : m.recipientId()) + "," + m.purpose() +
                    "," + m.intendedCount() + "," + m.actualCount() + "," +
                    m.slot() + "," + m.ageGap() +
                    "," + m.tier() + "," + m.value());
        Files.write(outputDirectory.resolve("inheritance_synthetic_matches.csv"), lines);
        lines.clear();
        lines.add("year,event,benefit_unit_id,person_id,net_financial_2015_gbp," +
                "net_housing_2015_gbp,pension_stock_2015_gbp,minor_trust_2015_gbp");
        for (AlignmentFlow flow : alignmentFlows)
            lines.add(flow.year() + "," + flow.event() + "," + flow.benefitUnitId() + "," +
                    (flow.personId() == null ? "" : flow.personId()) + "," +
                    flow.financial() + "," + flow.housing() + "," + flow.pension() +
                    "," + flow.minorTrust());
        Files.write(outputDirectory.resolve("alignment_wealth_flows.csv"), lines);
        Files.writeString(outputDirectory.resolve("inheritance_method.txt"),
                "pooled_last_adult; transfer_dc_pension_at_death=" + transferDcPensionAtDeath +
                        "; synthetic_recipient_count=" + syntheticRecipientCount +
                        "; natural_deaths_trigger_bequests=true" +
                        "; alignment_deaths_trigger_bequests=true" +
                        "; emigration_exits_trigger_bequests=false" +
                        "; pooled_estate_reference=highest_id_deceased_responsible_adult" +
                        "; matching_seed=" + seed + System.lineSeparator());
    }
}
