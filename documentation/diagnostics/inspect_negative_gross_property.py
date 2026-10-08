"""Read-only inspection of gross property values in current and archived runs.

Amounts are unscaled 2015-price pounds. A unit-year count is an observation,
not a count of distinct units. Writes diagnostic outputs only; source microdata
and simulation code are not changed. Requires pandas and numpy.
"""
import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd


COLS = ['run', 'time', 'id_BenefitUnit', 'idBuOriginal', 'ageRefPerson',
        'demCreatedByConstructor', 'wealthPrptyFlag', 'wealthPrptyValue',
        'wealthMortgageDebtValue', 'wealthPensValue', 'wealthTotValue',
        'yHousingReturnAnnual', 'yInvestAnnual']


def inspect(folder):
    annual, bad_frames, wealth_2070 = {}, [], []
    negative_equity_observations = 0
    for frame in pd.read_csv(folder / 'BenefitUnit.csv', usecols=COLS,
                             na_values=['null'], chunksize=150_000):
        assert np.isfinite(frame[['wealthPrptyValue', 'wealthMortgageDebtValue']]).all().all()
        for year, group in frame.groupby('time'):
            key = str(int(year))
            annual[key] = annual.get(key, 0) + len(group)
        negative_equity_observations += int((frame.wealthPrptyValue - frame.wealthMortgageDebtValue < -1e-6).sum())
        bad = frame.loc[frame.wealthPrptyValue < -1e-6].copy()
        if not bad.empty:
            bad['net_housing_equity'] = bad.wealthPrptyValue - bad.wealthMortgageDebtValue
            bad_frames.append(bad)
        wealth_2070.append(frame.loc[frame.time == 2070, 'wealthTotValue'])
    bad = pd.concat(bad_frames, ignore_index=True) if bad_frames else pd.DataFrame(columns=COLS + ['net_housing_equity'])
    total = sum(annual.values())
    annual_bad = {}
    for year, denominator in sorted(annual.items()):
        group = bad.loc[bad.time == int(year)]
        annual_bad[year] = dict(units=int(len(group)), all_units=int(denominator),
                                percent=100.0 * len(group) / denominator,
                                minimum=float(group.wealthPrptyValue.min()) if len(group) else 0.,
                                negative_stock_sum=float(group.wealthPrptyValue.sum()))
    ordered = np.sort(pd.concat(wealth_2070).to_numpy())
    n = len(ordered)
    # With equal weights and n not divisible by 10, count all of the top ceil(n/10)
    # units except for the fractional boundary weight, so exactly 10% is used.
    top_n = n / 10.0
    whole = int(top_n)
    top_value = ordered[-whole:].sum() + (top_n - whole) * ordered[-whole - 1]
    result = dict(csv_folder=str(folder), total_unit_year_observations=total,
                  negative_gross_property_unit_years=int(len(bad)),
                  negative_gross_property_percent=100.0 * len(bad) / total,
                  distinct_affected_unit_ids=int(bad.id_BenefitUnit.nunique()),
                  negative_equity_unit_years=negative_equity_observations,
                  mortgage_positive_negative_gross=int((bad.wealthMortgageDebtValue > 1e-6).sum()),
                  negative_gross_severity_counts={str(threshold): int((bad.wealthPrptyValue < -threshold).sum())
                                                  for threshold in [1, 100, 1000, 10000, 100000]},
                  annual=annual_bad,
                  worst_observation=bad.loc[bad.wealthPrptyValue.idxmin()].to_dict() if len(bad) else None,
                  top_10_percent_wealth_share_2070=float(top_value / ordered.sum()),
                  gini_2070=float(2 * np.dot(np.arange(1, n + 1), ordered) / (n * ordered.sum()) - (n + 1) / n))
    return result, bad


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study-dir', type=Path, default=Path(
        'D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality'))
    parser.add_argument('--output-dir', type=Path, required=True)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    results = {}
    for version, baseline in [('simulation outcomes', 'baseline'),
                              ('simulation outcomes bequest v3', 'benchmark')]:
        results[version] = {}
        for scenario in [baseline, 'counterfactual_no_ageing']:
            folder = args.study_dir / version / scenario / 'csv'
            result, bad = inspect(folder)
            results[version][scenario] = result
            label = ('current' if version == 'simulation outcomes' else 'v3') + '_' + scenario
            bad.to_csv(args.output_dir / (label + '_negative_property_rows.csv'), index=False)
    # Trace the worst baseline unit through its opening year and first few
    # projected years. These rows establish observable history, not regression
    # scores or latent innovations (which are not in the exports).
    current = results['simulation outcomes']['baseline']
    worst = current['worst_observation']
    history = []
    if worst:
        for frame in pd.read_csv(Path(current['csv_folder']) / 'BenefitUnit.csv',
                                 usecols=COLS, na_values=['null'], chunksize=150_000):
            history.append(frame.loc[(frame.id_BenefitUnit == worst['id_BenefitUnit']) &
                                     (frame.time <= worst['time'] + 3)])
        trace = pd.concat(history, ignore_index=True).sort_values('time')
        trace['net_housing_equity'] = trace.wealthPrptyValue - trace.wealthMortgageDebtValue
        trace.to_csv(args.output_dir / 'worst_baseline_unit_history.csv', index=False)
    (args.output_dir / 'negative_property_inspection.json').write_text(
        json.dumps(results, indent=2), encoding='utf-8')
    for version, scenarios in results.items():
        for scenario, result in scenarios.items():
            brief = {key: value for key, value in result.items() if key not in ['annual', 'worst_observation']}
            brief['2021'] = result['annual']['2021']
            brief['2022'] = result['annual']['2022']
            brief['2070'] = result['annual']['2070']
            brief['worst_observation'] = result['worst_observation']
            print(version, scenario, json.dumps(brief))


if __name__ == '__main__':
    main()
