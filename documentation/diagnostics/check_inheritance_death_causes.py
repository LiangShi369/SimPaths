"""Check death causes and recipient eligibility in a revised run's CSV folder.

Usage: python check_inheritance_death_causes.py PATH_TO_CSV_FOLDER
Reads Person.csv as a stream so full 2021-2070 exports fit in memory.
This checks exit classification and ledger consistency, not all wealth flows.
"""
import argparse
import csv
import json
import math
from pathlib import Path


def rows(folder, filename):
    with (folder / filename).open(newline="", encoding="utf-8-sig") as stream:
        yield from csv.DictReader(stream)


def check(folder):
    transfers = list(rows(folder, "inheritance_transfers.csv"))
    deaths = list(rows(folder, "inheritance_death_events.csv"))
    death_keys = {(int(x["year"]), int(x["deceased_id"])): x["death_cause"]
                  for x in deaths}
    assert len(death_keys) == len(deaths), "Duplicate settled deaths"
    assert set(death_keys.values()) <= {"Death", "DeathAlignment"}
    cash = [x for x in transfers if x["asset"] == "cash" and x["timing"] == "immediate"]
    recipient_keys = {(int(x["year"]), int(x["recipient_id"])) for x in cash}
    recipient_status = {}
    recipient_details = {}
    donor_details = {}
    exported_deaths = {}
    for person in rows(folder, "Person.csv"):
        key = (int(float(person["time"])), int(person["id_Person"]))
        cause = person["demExitSample"]
        if cause in ("Death", "DeathAlignment"):
            assert key not in exported_deaths, "Duplicate person-year death export"
            exported_deaths[key] = cause
            donor_details[key] = {"age": int(person['demAge']),
                                  "labour_state": person['labC4'],
                                  "adult_child": person['demAdultChildFlag']}
        if key in recipient_keys:
            assert key not in recipient_status, "Duplicate person-year recipient export"
            recipient_status[key] = cause
            recipient_details[key] = {"age": int(person['demAge']), "unit": int(person['idBu'])}
    assert death_keys == exported_deaths, "Death audit differs from Person export"
    assert recipient_keys == set(recipient_status), "Cash recipient missing from Person export"
    assert all(x == "NotYet" for x in recipient_status.values()), "Nonliving recipient paid"
    for transfer in transfers:
        value = float(transfer["value_2015_gbp"])
        assert math.isfinite(value) and value >= 0, "Invalid transfer amount"
        if int(transfer["deceased_id"]) >= 0:
            key = (int(transfer["year"]), int(transfer["deceased_id"]))
            assert key in death_keys, "Donor is not a settled death"
            assert transfer["death_cause"] == death_keys[key], "Incorrect transfer death cause"
            if transfer['reason'].startswith('nonpension_estate'):
                assert donor_details[key]['age'] >= 18, 'Dependent minor used as pooled estate reference'
                # Adult children living in their parents' household can head
                # their own benefit unit. This flag does not identify the
                # responsible-adult role within a unit (Person.leaveHome).
            if transfer['reason'].startswith('unaccessed_pension') and transfer['asset'] in ('cash', 'minor_trust'):
                assert donor_details[key]['labour_state'] != 'Retired', 'Retired donor paid unused pension pot'
    alignment_events = {}
    for flow in rows(folder, "alignment_wealth_flows.csv"):
        event = flow["event"]
        assert event not in ("unit_death_alignment", "death_alignment_person_exit"), \
            "Death estate also recorded as an external alignment outflow"
        alignment_events[event] = alignment_events.get(event, 0) + 1
    method = (folder / "inheritance_method.txt").read_text(encoding="utf-8")
    assert "alignment_deaths_trigger_bequests=true" in method
    assert "emigration_exits_trigger_bequests=false" in method
    summary = {"output": str(folder), "death_counts": {}, "transfer_rows": len(transfers),
               "immediate_cash_by_death_cause": {}, "alignment_events": alignment_events}
    summary['cash_2027_2070_by_recipient_own_age'] = {}
    for low, high, label in [(0, 23, 'under24'), (24, 54, '24-54'), (55, 74, '55-74'), (75, 999, '75plus')]:
        payments = [x for x in cash if 2027 <= int(x['year']) <= 2070 and
                    low <= recipient_details[(int(x['year']), int(x['recipient_id']))]['age'] <= high]
        summary['cash_2027_2070_by_recipient_own_age'][label] = {
            'payment_rows': len(payments),
            'recipient_years': len({(x['year'], x['recipient_id']) for x in payments}),
            'donor_recipient_years': len({(x['year'], x['deceased_id'], x['recipient_id']) for x in payments}),
            'value_2015_gbp': sum(float(x['value_2015_gbp']) for x in payments)}
    synthetic_rows = list(rows(folder, 'inheritance_synthetic_matches.csv'))
    allocations = {}
    for match in synthetic_rows:
        assert 0 <= int(match['actual_n']) <= int(match['intended_n'])
        if match['recipient_id']:
            key = (match['year'], match['deceased_id'], match['purpose'])
            ids = allocations.setdefault(key, set())
            assert match['recipient_id'] not in ids, 'Duplicate recipient within synthetic allocation'
            ids.add(match['recipient_id'])
    summary['synthetic_allocation_checks'] = {'rows': len(synthetic_rows), 'allocations_with_matches': len(allocations)}
    summary['out_of_sample_transfer_rows'] = sum(x['timing'] == 'out_of_sample' for x in transfers)
    summary['out_of_sample_transfer_value'] = sum(float(x['value_2015_gbp']) for x in transfers if x['timing'] == 'out_of_sample')
    for cause in ("Death", "DeathAlignment"):
        payments = [x for x in cash if x["death_cause"] == cause]
        summary["death_counts"][cause] = sum(x == cause for x in death_keys.values())
        summary["immediate_cash_by_death_cause"][cause] = {
            "payments": len(payments), "donors": len({x["deceased_id"] for x in payments}),
            "value_2015_gbp": sum(float(x["value_2015_gbp"]) for x in payments),
            "pension_payments": sum(x["reason"].startswith("unaccessed_pension") for x in payments)}
    return summary


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_folder", type=Path)
    args = parser.parse_args()
    print(json.dumps(check(args.csv_folder), indent=2))
