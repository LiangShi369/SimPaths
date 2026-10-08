"""Archive and adapt the ageing study analysis to the current baseline folder.

Run with --apply only after inspecting the staged MATLAB/Python changes.
Changes analysis scripts and documentation only, never simulation code/data.
"""
import argparse
import re
import shutil
from pathlib import Path

STUDY = Path('D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality')
DIAG = Path('D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration')
STAGE = Path('D:/GitHub/SimPaths/target/analysis_refresh_20261007')

HELPER = '''function csvDir = resolve_ageing_csv_dir(studyDir, scenario)
% Internal CSV scenario code "benchmark" denotes the current baseline folder.
% No fallback to archived outcomes: missing current inputs must fail visibly.
code = string(scenario);
if code == "benchmark" || code == "baseline"
    folder = "baseline";
elseif code == "counterfactual_no_ageing"
    folder = code;
else
    error('Unknown scenario code: %s', code);
end
csvDir = fullfile(studyDir, 'simulation outcomes', folder, 'csv');
assert(isfolder(csvDir), 'Missing current scenario input directory: %s', csvDir);
end
'''

PATTERN = re.compile(
    r"fullfile\(\s*(studyDir|fileparts\(scriptDir\))\s*,\s*'simulation outcomes'\s*,\s*(?:\.\.\.\s*)?"
    r"('(benchmark|counterfactual_no_ageing)'|(?:names|scenarios|scenarioNames|scenarioCodes)(?:\{(?:s|scenario)\}|\(s\))|char\(scenarioNames\(s\)\))"
    r"\s*,\s*(?:\.\.\.\s*)?'csv'(\s*,|\s*\))")


def adapt_matlab(text):
    def replace(match):
        call = f'resolve_ageing_csv_dir({match.group(1)}, {match.group(2)})'
        return call if match.group(4).strip() == ')' else f'fullfile({call},'
    return PATTERN.subn(replace, text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    staged = STAGE / 'matlab'
    staged.mkdir(parents=True, exist_ok=True)
    changes = []
    for source in (STUDY / 'matlab').glob('*.m'):
        text, count = adapt_matlab(source.read_text(encoding='utf-8-sig'))
        if source.name == 'inspect_wealth_accumulation_channels.m':
            text = text.replace('diagnostics_20261006_accumulation_channels', 'diagnostics_20261007_accumulation_channels')
            text = text.replace("assert(contains(method,'alignment_exits_trigger_bequests=false'),...\n        'This analysis expects the current runs with alignment bequests off.');",
                                "assert(contains(method,'alignment_deaths_trigger_bequests=true'),...\n        'This analysis expects current runs with alignment deaths triggering bequests.');\n    assert(contains(method,'emigration_exits_trigger_bequests=false'));")
        if source.name == 'refresh_all_figures.m':
            text = text.replace("    'plot_same_age_cohorts'", "    'plot_same_age_cohorts'\n    'plot_wealth_decile_levels_2070'\n    'plot_wealth_decile_histogram_2070'")
        (staged / source.name).write_text(text, encoding='utf-8')
        if count or text != source.read_text(encoding='utf-8-sig'):
            changes.append((source.name, count))
    (staged / 'resolve_ageing_csv_dir.m').write_text(HELPER, encoding='utf-8')
    bequests = STAGE / 'recipient_age'
    bequests.mkdir(exist_ok=True)
    source_dir = DIAG / 'diagnostics_20261006_recipient_age'
    text = (source_dir / 'analyse_bequests_by_age.py').read_text(encoding='utf-8')
    text = text.replace("def analyse(study, out, scenario):", "def csv_dir(study, scenario):\n    return study/'simulation outcomes'/('baseline' if scenario == 'benchmark' else scenario)/'csv'\n\ndef analyse(study, out, scenario):")
    text = text.replace("study/'simulation outcomes'/scenario/'csv'", 'csv_dir(study, scenario)')
    text = text.replace("args.study/'simulation outcomes'/s/'csv'", 'csv_dir(args.study, s)')
    text = text.replace("args.study/'simulation outcomes'/scenario/'csv'", 'csv_dir(args.study, scenario)')
    text = text.replace('args.csv_dir(study, scenario)', 'csv_dir(args.study, scenario)')
    text = text.replace("'alignment_exits_trigger_bequests=false'", "'alignment_deaths_trigger_bequests=true', 'emigration_exits_trigger_bequests=false'")
    text = text.replace("a.event.isin(['death_alignment_person_exit','emigration_alignment_person_exit'])", "a.event.isin(['emigration_alignment_person_exit'])")
    text = text.replace('An alignment exit generated an inheritance ledger entry.', 'An emigration exit generated an inheritance ledger entry.')
    text = text.replace("# Exited people may be absent", "assert cash.donor_exit.isin(['Death', 'DeathAlignment']).all(), 'Cash donor not a recorded death.'\n    if 'death_cause' in cash.columns:\n        assert cash.death_cause.eq(cash.donor_exit).all(), 'Payment death cause disagrees with Person.csv.'\n    # Exited people may be absent")
    text = text.replace("'alignment_overlap_rows':", "'emigration_overlap_rows':")
    (bequests / 'analyse_bequests_by_age.py').write_text(text, encoding='utf-8')
    for name in ['refine_bequests_24_54_55_74.py', 'plot_bequest_diagnostics.m']:
        shutil.copy2(source_dir / name, bequests / name)
    print('Adapted MATLAB source paths:', changes)
    print('Stage:', STAGE)
    if not args.apply:
        return
    archive = STUDY / 'analysis_before_refresh_20261007'
    archive.mkdir(exist_ok=False)
    for source in STUDY.glob('*.md'):
        shutil.copy2(source, archive / source.name)
    shutil.copytree(STUDY / 'matlab' / 'figures', archive / 'figures')
    (archive / 'matlab').mkdir()
    for source in (STUDY / 'matlab').glob('*.m'):
        shutil.copy2(source, archive / 'matlab' / source.name)
    for source in staged.glob('*.m'):
        shutil.copy2(source, STUDY / 'matlab' / source.name)
    new_recipient = DIAG / 'diagnostics_20261007_recipient_age'
    new_recipient.mkdir(exist_ok=False)
    for source in bequests.iterdir():
        shutil.copy2(source, new_recipient / source.name)
    # Add the requested housing finding immediately; further result updates
    # happen only after the MATLAB refresh completes.
    readme = STUDY / 'README.md'
    text = readme.read_text(encoding='utf-8-sig')
    text = text.replace('## Why run this experiment?',
        '## Current refresh status — 7 October 2026\n\n'
        '**The results below are being refreshed from the latest baseline and no-ageing outputs.** '
        'The previous numerical conclusions and 40% age-mix result apply to the archived run. '
        'The current runs include bequests from natural deaths and population-alignment deaths, '
        'while emigration exits do not trigger bequests.\n\n'
        '### Negative gross property: frequency, cause and implications\n\n'
        '| Current run, 2021–2070 | Baseline | No ageing |\n'
        '| --- | ---: | ---: |\n'
        '| Negative gross-property unit-year observations | 737 | 711 |\n'
        '| Share of all unit-year observations | 0.0471% | 0.0460% |\n'
        '| Distinct affected benefit units | 636 | 624 |\n'
        '| Affected units in 2070 | 4 of 33,978 | 8 of 32,994 |\n\n'
        'Negative net housing equity can be valid when a positive property value is below the mortgage. '
        '**Negative gross property cannot be valid.** The housing code predicts equity and mortgage '
        'separately, then adds them without enforcing `equity ≥ −mortgage`. Most invalid amounts are '
        'small, but both runs contain a common 2022 case of **−£964k** gross property, already present '
        'in the V3 archive. The financial balance is calculated as a residual, so this invalid housing '
        'value creates offsetting financial assets and can affect later returns and ownership.\n\n'
        '**We proceed with a provisional descriptive refresh, not a finding that the defect is harmless.** '
        'Its cumulative effect has not been measured. A housing correction and rerun are needed to '
        'establish robustness of the scenario wealth gap. '
        '[Full inspection and reproducible script](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_negative_housing/README.md>).\n\n'
        '## Why run this experiment?', 1)
    readme.write_text(text, encoding='utf-8')
    print('Archived prior analysis at:', archive)


if __name__ == '__main__':
    main()
