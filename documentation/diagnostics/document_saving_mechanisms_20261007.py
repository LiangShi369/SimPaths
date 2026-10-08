"""Finish reproducible saving/retirement tables and update the study guide.

This writes only requested analysis/documentation/presentation artifacts.
"""
import json
import re
import shutil
from pathlib import Path
import pandas as pd

REPO=Path('D:/GitHub/SimPaths')
STUDY=Path('D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality')
DIAG=Path('D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration')
OUT=DIAG/'diagnostics_20261007_saving_propensity'
STAGE=REPO/'target/analysis_refresh_20261007/mechanism_slides'
r=json.loads((REPO/'target/analysis_refresh_20261007/current_saving_pension_channels.json').read_text())
codes=['baseline','counterfactual_no_ageing']


def table(headers,rows):
    return '\n'.join(['| '+' | '.join(headers)+' |','| '+' | '.join(['---']*len(headers))+' |']+
                     ['| '+' | '.join(map(str,x))+' |' for x in rows])


def values(field):
    return [r[c]['groups'][k][field] for k in ['at_least_one_nonretired_adult','all_living_members_18plus_retired'] for c in codes]


rows=[]
for field,label in [('units','Complete unit observations'),('mean_total_net_wealth','Mean net wealth'),
                     ('mean_investment_income_annual','Annual investment income'),('mean_disposable_annual','Annual disposable income'),
                     ('mean_discretionary_consumption_annual','Annual discretionary consumption'),
                     ('mean_income_minus_discretionary_consumption','Annual income left after consumption')]:
    rows.append([label,*[(f'{int(v):,}' if field=='units' else f'{v/1000:,.1f}') for v in values(field)]])
rows.append(['Share of disposable income remaining',*[f'{100*v:.1f}%' for v in values('budget_surplus_over_income_ratio_of_sums')]])
retired_table=table(['2070 measure, £ thousands unless stated','Other units: baseline','Other units: no ageing','Retired units: baseline','Retired units: no ageing'],rows)

ages=[]
for key,label in [('reference_age_under_65','18–64'),('reference_age_65plus','65+'),
                  ('reference_age_24_54','24–54'),('reference_age_55_74','55–74'),('reference_age_75plus','75+')]:
    a,b=[r[c]['groups'][key] for c in codes]
    ages.append([label,f'{100*a["budget_surplus_over_income_ratio_of_sums"]:.1f}%',f'{100*b["budget_surplus_over_income_ratio_of_sums"]:.1f}%'])
age_table=table(['Reference-person age','Baseline income share remaining','No-ageing income share remaining'],ages)
section='''#### Retirement and the share of income remaining after consumption

Retirement status is more informative for this question than treating everyone aged 65+ as retired. A **retired unit** has all living adults aged 18+ recorded as retired; **other units** have at least one living adult recorded as not retired. Other units can include unemployed, inactive and student adults, and couples with one retired partner; they are not necessarily employed households.

'''+retired_table+r'''

Define the share remaining as:

$$100\times\frac{\sum_u (Y_u-C_u)}{\sum_u Y_u},$$

where $Y_u$ is annual disposable income and $C_u$ is annual discretionary consumption. This is a **ratio of sums**, not an average of individual saving rates. It is the recurring budget-surplus measure; housing returns, pension-stock changes, retirement lump sums and bequest receipts are separate wealth changes. It is not a comprehensive saving rate or a wealth-growth rate.

**Interpretation:** fully retired units retain a larger share of income than other units in both runs, despite their higher wealth and investment incomes. Within the retired group, no ageing has higher wealth and investment income but a slightly lower share remaining (**41.3% versus 43.4%**). Its surplus is nevertheless slightly higher in pounds (**£42.59k versus £42.14k**). This illustrates why a higher saving amount need not mean a higher saving fraction. These comparisons do not establish that wealth or investment income causes saving behaviour.

The age classifications provide a complementary comparison:

'''+age_table+'''

The overlapping broad and detailed age rows answer different questions and must not be added together. The share remaining is higher at older ages in both runs; there is no simple decline with age in this budget measure. The projected magnitudes still require validation.

**Coverage:** the retirement comparison uses complete income/consumption and investment/pension-income records. Of 33,311 current baseline units, 32,866 have the required flows (445 excluded for missing flows); of 32,108 no-ageing units, 31,512 do (596 excluded). Missing-flow exclusions are separate from the exclusion of departed units. Counts and exclusions by group are saved with the results.

[Reproducible saving/retirement diagnostics](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_saving_propensity/README.md>); [full table](matlab/figures/saving_propensity_by_age_retirement_2070.csv).
'''

OUT.mkdir(exist_ok=True)
shutil.copy2(REPO/'documentation/diagnostics/inspect_saving_pension_channels.py',OUT/'inspect_saving_pension_channels.py')
shutil.copy2(REPO/'target/analysis_refresh_20261007/current_saving_pension_channels.json',OUT/'current_saving_pension_channels.json')
shutil.copy2(STAGE/'saving_propensity_2070.csv',OUT/'saving_propensity_2070.csv')
shutil.copy2(STAGE/'saving_propensity_2070.csv',STUDY/'matlab/figures/saving_propensity_by_age_retirement_2070.csv')
for name in ['inspect_saving_pension_channels.py','current_saving_pension_channels.json']:
    shutil.copy2(OUT/name,DIAG/'diagnostics_20261007_recipient_age'/name)
(OUT/'README.md').write_text('# Saving, age and retirement: current 2070 results\n\n'+section+'''

## Reproduction

Run `inspect_saving_pension_channels.py --study-dir "D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality" --year 2070` with pandas. It streams the current Person and BenefitUnit exports and reads the pension-transfer ledger. Input files are never changed. The two retirement categories are mutually exclusive. The exported flow fields are not complete for every current unit; those exclusions are explicitly reported.

The current runs include natural and population-alignment deaths as inheritance triggers; emigrants do not trigger bequests. The pension cash result is £895.69m baseline and £1,574.30m no ageing over 2027–2070. The same JSON records pension allocations to minor trusts separately.
''',encoding='utf-8')
(STUDY/'saving_retirement_results_20261007.md').write_text('# Saving and retirement comparisons\n\n'+section,encoding='utf-8')

p=STUDY/'README.md'; s=p.read_text(encoding='utf-8')
old='![Mean net wealth per current benefit unit, 2025–2070](matlab/figures/mean_net_wealth_per_unit_annual.png)'
new='![Mean and median net wealth per current benefit unit, 2025–2070](matlab/figures/mean_median_net_wealth_per_unit_annual.png)'
assert old in s
s=s.replace(old,new)
s=s.replace('**Read:** annual cross-sectional means per current benefit unit.','**Read:** the left panel shows annual cross-sectional means and the right shows medians per current benefit unit. The mean is more sensitive to large balances; the median is the middle unit in each annual distribution.')
anchor='#### Between-age differences in flow means'
assert s.count(anchor)==1
s=s.replace(anchor,section+'\n\n'+anchor)
s=s.replace('Their 2070 means','Their 2070 means')
s=s.replace('**£1.525m baseline and £1.588m no ageing**. This does not track','**£1.525m baseline and £1.588m no ageing**. The 2070 medians are **£608k baseline versus £566k no ageing**, so the ordering differs between mean and median. This does not track')
s=s.replace('Wealth_module_refreshed_20261007.pdf','Wealth_module_mechanisms_20261007.pdf')
p.write_text(s,encoding='utf-8')

p=STUDY/'slide_outline_current_20261007.md'; s=p.read_text(encoding='utf-8')
s=s.replace('Wealth_module_refreshed_20261007.pdf','Wealth_module_mechanisms_20261007.pdf')
s=s.replace('[annual mean wealth](matlab/figures/mean_net_wealth_per_unit_annual.png)','[mean and median wealth trajectories](matlab/figures/mean_median_net_wealth_per_unit_annual.png)')
s=s.replace('Do not retain the former claim that ageing produces a much higher mean.','The baseline median is higher (£608k versus £566k), despite its lower mean. Do not retain the former claim that ageing produces a much higher mean.')
for old,new in [(12,14),(11,12),(10,11)]:
    s=s.replace(f'### {old}.',f'### {new}.')
pension='''### 10. Pension pots can be inherited before annuitisation · 1 minute

**Show:** a native table of immediate pension-pot cash, **£0.896bn baseline versus £1.574bn no ageing**, 2027–2070. The no-ageing total is approximately **76% higher**. This is consistent with the unused-pot channel; death counts and pot sizes also affect it. Minor trusts are separate. After annuitisation the remaining pot is zero; pension payments are income while the recipient lives.

'''
s=s.replace('### 11. Which assets',pension+'### 11. Which assets')
saving='''### 13. Income remaining after consumption · 2 minutes

**Show:** the [retirement comparison table](README.md#retirement-and-the-share-of-income-remaining-after-consumption). Fully retired units retain **43.4% baseline versus 41.3% no ageing** of disposable income; units with at least one non-retired adult retain **31.1% versus 31.5%**. State the ratio-of-sums definition and complete-flow sample. Higher wealth does not imply a lower share retained across these groups. Retired no-ageing units have higher wealth but a slightly lower fraction retained than retired baseline units. Use the age-specific rates in the README as supporting detail.

'''
s=s.replace('### 14. What the application',saving+'### 14. What the application')
s=s.replace('approximately **22–25 minutes**','approximately **25–28 minutes**')
p.write_text(s,encoding='utf-8')

deck=STUDY/'presentation HealthMod26Glasgow'
shutil.copy2(STAGE/'Wealth_module.pdf',deck/'Wealth_module_mechanisms_20261007.pdf')
try:
    shutil.copy2(STAGE/'Wealth_module.pdf',deck/'Wealth_module.pdf')
    print('Updated the existing PDF as well as the dated review PDF.')
except PermissionError:
    print('Existing PDF is locked; dated review PDF contains the new 38-page deck.')
print('Saved detailed saving tables and diagnostics; updated README and outline.')
