"""Export analysis-only current-population unit-year keys from Person.csv.

Person.csv includes annual exits before cleanup. Current population means
demExitSample == NotYet. A current benefit unit has a living member aged 18+,
matching the model's minimum responsible-adult age. Input exports are unchanged.
"""
import argparse
import json
from pathlib import Path
import numpy as np
import pandas as pd


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    report = {}
    manifest = []
    for code, folder in [('benchmark', 'baseline'), ('counterfactual_no_ageing', 'counterfactual_no_ageing')]:
        csv = args.study / 'simulation outcomes' / folder / 'csv'
        info = (csv/'Person.csv').stat()
        manifest.append(dict(scenario=code, person_bytes=info.st_size,
                             person_modified_ms=info.st_mtime_ns//1000000))
        parts, populations = [], []
        for frame in pd.read_csv(csv / 'Person.csv', usecols=['time','idBu','demAge','demExitSample'], chunksize=150000):
            alive = frame.loc[frame.demExitSample.eq('NotYet')]
            populations.append(alive.groupby('time').size())
            keys = alive.loc[alive.demAge.ge(18), ['time','idBu']].drop_duplicates()
            parts.append(keys)
        keys = pd.concat(parts).drop_duplicates().rename(columns={'time':'year', 'idBu':'benefit_unit_id'})
        keys.year = keys.year.astype(int)
        keys.benefit_unit_id = keys.benefit_unit_id.astype(int)
        keys.sort_values(['year','benefit_unit_id']).to_csv(args.out / f'active_benefit_units_{code}.csv', index=False)
        allowed = pd.MultiIndex.from_frame(keys)
        active_parts, inactive_parts = [], []
        for frame in pd.read_csv(csv/'BenefitUnit.csv', usecols=['time','id_BenefitUnit','wealthTotValue'], chunksize=150000):
            frame = frame.loc[frame.time.eq(2070)]
            mask = pd.MultiIndex.from_frame(frame[['time','id_BenefitUnit']]).isin(allowed)
            active_parts.append(frame.loc[mask])
            inactive_parts.append(frame.loc[~mask])
        active, inactive = pd.concat(active_parts), pd.concat(inactive_parts)
        ordered = np.sort(active.wealthTotValue.to_numpy())
        n = len(ordered)
        report[code] = dict(current_people_2070=int(pd.concat(populations).groupby(level=0).sum().loc[2070]),
            active_units_2070=n, inactive_units_2070=len(inactive),
            inactive_wealth_2070=float(inactive.wealthTotValue.sum()),
            inactive_positive_wealth_units=int(inactive.wealthTotValue.gt(0).sum()),
            active_mean_2070=float(ordered.mean()), active_median_2070=float(np.median(ordered)),
            active_gini_2070=float(2*np.dot(np.arange(1,n+1),ordered)/(n*ordered.sum())-(n+1)/n))
    (args.out/'current_population_check.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    pd.DataFrame(manifest).to_csv(args.out/'input_manifest.csv',index=False)
    print(json.dumps(report,indent=2))


if __name__ == '__main__':
    main()
