"""Generate the current slide guide from refreshed MATLAB tables.

Requires a completed refresh using current-population denominators. Historical
guides are archived by prepare_analysis_refresh_20261007.py. No deck is edited.
"""
import json
from pathlib import Path
import numpy as np
import pandas as pd
from prepare_analysis_refresh_20261007 import STUDY, DIAG, STAGE

CODES = ['benchmark', 'counterfactual_no_ageing']
NAMES = ['Baseline', 'No ageing']


def read(name):
    return pd.read_csv(STUDY/'matlab'/'figures'/name)


def table(headers, rows):
    return '\n'.join(['| '+' | '.join(headers)+' |', '| '+' | '.join(['---']*len(headers))+' |']+
                     ['| '+' | '.join(map(str,row))+' |' for row in rows])


def money(x):
    sign = '−' if x < 0 else ''
    return f'{sign}£{abs(x)/1e6:.3f}m' if abs(x)>=1e6 else f'{sign}£{abs(x)/1e3:.1f}k'


def row_pair(frame, **filters):
    for field, value in filters.items(): frame=frame.loc[frame[field].eq(value)]
    values=[]
    for code,name in zip(CODES,NAMES):
        selected=frame.loc[frame.scenario.isin([code,name])]
        assert len(selected)==1, (filters,code)
        values.append(selected.iloc[0])
    return values


def main():
    assert 'REFRESH COMPLETE' in (STAGE/'matlab_current_population_refresh.log').read_text(errors='replace')
    population=json.loads((STUDY/'matlab'/'current_population_inputs'/'current_population_check.json').read_text())
    wealth=read('wealth_distribution_annual.csv'); b,n=row_pair(wealth,year=2070)
    for code,row in zip(CODES,[b,n]):
        assert row.n_units==population[code]['active_units_2070'], 'Refresh still uses exited-unit denominators.'
        assert abs(row['mean']-population[code]['active_mean_2070'])<1e-6
        assert abs(row.gini_net_wealth-population[code]['active_gini_2070'])<1e-10
    component=read('wealth_components_plot_2025_2070.csv'); cb,cn=row_pair(component,year=2070)
    first_rows=component.loc[component.year.eq(2025)]
    assert len(first_rows)==1
    start=first_rows.iloc[0]
    annual=read('wealth_component_income_annual.csv'); ab,an=row_pair(annual,year=2070)
    initial,_=row_pair(annual,year=2025)
    age=read('wealth_by_reference_age_2070.csv')
    shares=read('older_wealth_shares_2070.csv')
    detailed=read('total_wealth_by_age_2070.csv')
    broad=read('income_consumption_broad_age_2070.csv')
    ub,un=row_pair(broad,reference_age_group='Under 65'); ob,on=row_pair(broad,reference_age_group='65+')
    means={}
    for code in CODES:
        a=age.loc[age.scenario.eq(code)]
        older=a.loc[a.age_group.isin(['65-74','75+','75-84','85+'])]
        younger=a.loc[~a.age_group.isin(['65-74','75+','75-84','85+'])]
        means[code]=[np.average(younger.mean_total,weights=younger.n_units),np.average(older.mean_total,weights=older.n_units)]
    ratios=[means[c][1]/means[c][0] for c in CODES]
    s65b,s65n=row_pair(shares,age_group='65+'); s75b,s75n=row_pair(shares,age_group='75+')
    mix=read('income_financial_age_mix_5year_2070.csv')
    mw=mix.loc[mix.metric.eq('mean_total_net_wealth')].iloc[0]
    mf=mix.loc[mix.metric.eq('mean_net_financial_balance')].iloc[0]
    income=read('income_financial_2025_2070.csv'); ib,inn=row_pair(income,year=2070)
    cohort=read('same_age_cohort_percentile_gaps.csv')
    gini=read('age_group_wealth_gini_2070.csv'); gb,gn=row_pair(gini)
    period=pd.read_csv(DIAG/'diagnostics_20261007_recipient_age'/'period_bequests_unit_age_24_54_55_74.csv')
    audit=json.loads((DIAG/'diagnostics_20261007_recipient_age'/'audit_results.json').read_text())
    totalcash=[next(x['cash_2027_2070'] for x in audit if x['scenario']==c) for c in CODES]
    p24b,p24n=row_pair(period,age_group='24-54'); p55b,p55n=row_pair(period,age_group='55-74')
    pop=read('population_age_summary_65plus.csv').query('year==2070').iloc[0]
    total=read('total_net_wealth_annual.csv'); tb,tn=row_pair(total,year=2070)
    band=read('adult_age_by_wealth_band_selected_2070.csv')
    report=[]
    def add(text): report.append(text.strip())
    def fig(filename,alt,caption): add(f'![{alt}](matlab/figures/{filename}.png)\n\n{caption}')
    add('''# Population ageing, wealth and income inequality

This is the **current guide for preparing the ageing-application slides**, refreshed on **7 October 2026** from the latest [simulation outcomes](simulation%20outcomes). The [updated slide outline](slide_outline_current_20261007.md) follows these results. The ageing tables and accompanying conclusions in [Wealth_module.tex](presentation%20HealthMod26Glasgow/Wealth_module.tex) have been refreshed. The [compiled review PDF](presentation%20HealthMod26Glasgow/Wealth_module_refreshed_20261007.pdf) has a new filename because the existing PDF was open or locked.

**Status: provisional descriptive results.** A real, infrequent housing consistency defect remains; Section “Housing check and interpretation limits” explains its frequency and possible consequences. Refreshing the analysis does not establish that its effect is negligible. The [previous analysis](analysis_before_refresh_20261007) and [V3 outputs](simulation%20outcomes%20bequest%20v3) are retained. Their figures and former 40% age-mix calculation must not supply numbers for the current slides.''')
    add('''## Purpose and experiment design

The wealth module follows financial assets, unsecured debt, net housing equity, private pension pots and bequests alongside earnings, taxes, benefits and consumption. This application asks: **How does a different population age structure change wealth levels, who holds wealth, and income and consumption across ages?**

The **baseline** uses the standard population projection. The **no-ageing counterfactual** retains each gender–region group's 2026 age shares in population targets from 2027 to 2070, while keeping projected annual population totals. Both scenarios align population, fertility and cohabitation and use the same wealth settings. Simulations start in 2021; the presentation concentrates on 2025–2070. There is **one run per scenario, seed 606**. This is a demographic scenario comparison, not a policy intervention.

Both current runs include bequests from **natural deaths and population-alignment deaths**. **Emigration exits do not trigger bequests.** Pooled financial and housing wealth transfers when the last responsible adult dies; the first partner's recorded child branches remain eligible for the final estate. Eligible pension pots transfer at their owner's death before modelled retirement, to a surviving partner first, otherwise to children or matched recipients. At retirement the model annuitises the pot and its remaining balance becomes zero. Unmatched estates use the agreed synthetic-recipient rule; receipts for minors are held in trust.''')
    add(f'''## Measurement and population conventions

- Wealth belongs to the **whole benefit unit**: one adult or a couple, with dependent children. A couple counts once. Unit age charts use the **reference person's age**, not individual ownership.
- Monetary amounts are in **2015-price pounds**. Wealth is a stock; earnings, pension income, investment income, consumption and receipts are annual flows. Unscaled totals refer to the simulated sample, not UK totals.
- Presentation population charts use **living people** (`demExitSample = NotYet`). Per-unit calculations use units with at least one living adult aged 18+. The export also retains exited people and empty units until cleanup. In 2070 we exclude **{population[CODES[0]]['inactive_units_2070']:,} baseline and {population[CODES[1]]['inactive_units_2070']:,} no-ageing units with zero remaining wealth**. This changes means, percentiles and Ginis, but leaves total wealth unchanged. Earlier inspection figures of £1.495m and £1.546m used all exported units.
- Age-specific means generally use **recorded** reference ages. Wealth-share charts recover missing ages from an unambiguous source-unit record and include all current units. Cohorts are approximated as simulation year minus reference-person age. These samples are labelled separately.
- Gross labour earnings in the income charts sum the annual earnings reported by **living members at year-end**. Disposable income is the model's annual after-tax-and-benefit budget measure, including labour, pension, investment and other income. Within-year exits and other income mean the displayed source rows are not an exact disposable-income reconciliation.
- Consumption means **modelled discretionary consumption**. Separately reported childcare and social-care spending are omitted; income less discretionary consumption is a budget surplus measure, not a complete saving rate.''')
    add(f'''## Main findings for the slides

1. **The populations separate.** In 2070 the old-age dependency ratio is **{pop.benchmark_ratio_per_100:.1f}** in the baseline and **{pop.no_ageing_ratio_per_100:.1f}** without ageing: people aged 65+ per 100 aged 16–64.
2. **Similar total wealth, different per-unit distributions.** Total simulated wealth is **£{tb.simulated_total_net_wealth/1e9:.2f}bn versus £{tn.simulated_total_net_wealth/1e9:.2f}bn**. No ageing has the higher mean (**{money(n['mean'])} versus {money(b['mean'])}**), while the baseline has the higher median (**{money(b.p50)} versus {money(n.p50)}**). More wealth at the mean does not mean more wealth throughout the distribution.
3. **More wealth sits in older-headed units in the baseline.** Units aged 65+ hold **{s65b.share_total_wealth_pct:.1f}% versus {s65n.share_total_wealth_pct:.1f}%** of wealth, and form **{s65b.share_units_pct:.1f}% versus {s65n.share_units_pct:.1f}%** of units. The corresponding 65+/under-65 mean wealth ratios are **{ratios[0]:.2f} versus {ratios[1]:.2f}**. The wealth-share comparison and per-unit age gap answer different questions.
4. **Bequests are active, with more cash reaching younger units without ageing.** Cash paid in 2027–2070 totals **£{totalcash[0]/1e9:.2f}bn baseline and £{totalcash[1]/1e9:.2f}bn no ageing**. Units aged 24–54 receive **£{p24b.cash_receipt/1e9:.2f}bn versus £{p24n.cash_receipt/1e9:.2f}bn**. This is evidence about transfers, not a controlled estimate of how much bequests caused their wealth gap.
5. **Income and consumption must be read alongside wealth.** Under-65 disposable income is **{money(ub.mean_disposable_income)} versus {money(un.mean_disposable_income)}** and discretionary consumption **{money(ub.mean_discretionary_consumption)} versus {money(un.mean_discretionary_consumption)}**. The source table below shows whether labour earnings or investment income accompany the difference.
6. **The previous 40% age-mix explanation is superseded.** Reweighting no ageing to baseline age shares now changes its age-known mean from **{money(mw.counterfactual_age_known_mean)} to {money(mw.counterfactual_at_benchmark_age_shares)}**, against **{money(mw.benchmark_age_known_mean)}** observed baseline. Age composition and within-age means work in opposite directions; do not reuse the former 40/60 split.''')
    add('## Figures and tables for the presentation\n\n### Demographic contrast')
    fig('ageing_scenarios_65plus_10year','Living population age shares in 2070 and old-age dependency ratios',
        '**Read:** the left panel compares living people’s 2070 age shares. The right follows both scenarios, with ratio `100 × people aged 65+ / people aged 16–64`. It measures demographic age structure, not employment or financial support. Both targets are common through 2026; realised ratios need not be perfectly flat in the counterfactual.')
    add('### Overall wealth: mean, median and distribution')
    fig('mean_net_wealth_per_unit_annual','Mean net wealth per current benefit unit, 2025–2070',
        f'**Read:** annual cross-sectional means per current benefit unit. Both scenarios start at **{money(start.mean_total_net_wealth)} in 2025**; their 2070 means are **{money(b["mean"])} baseline and {money(n["mean"])} no ageing**. This does not track the wealth growth of a fixed set of units. Similar total wealth can coexist with different means when unit counts differ.')
    fig('wealth_decile_levels_2070','2070 wealth thresholds at P10 through P90',
        '**Read:** bars show net-wealth **thresholds**, calculated separately in each scenario. P10 is the value below which 10% of units lie; P50 is the median; P90 is the value below which 90% lie. These nine cutoffs define ten deciles. Bar heights are wealth amounts, not frequency shares, and do not sum to 100%.')
    fig('wealth_inequality_2070','Baseline differences from no ageing at selected wealth percentiles',
        '**Read:** each bar is `100 × (baseline percentile − no-ageing percentile) / no-ageing percentile`. Positive means higher baseline wealth at that percentile. A large percentage at a low cutoff can represent a small pound difference. The bars compare separate distributions, not gains for matched units.')
    wealth_rows=[['Current benefit units',f'{int(b.n_units):,}',f'{int(n.n_units):,}'],['Mean',money(b['mean']),money(n['mean'])]]
    for key,label in [('p10','P10'),('p25','P25'),('p50','Median'),('p75','P75'),('p90','P90')]: wealth_rows.append([label,money(b[key]),money(n[key])])
    wealth_rows += [['Zero or negative wealth',f'{b.nonpositive_pct:.1f}%',f'{n.nonpositive_pct:.1f}%'],['Net-wealth Gini',f'{b.gini_net_wealth:.3f}',f'{n.gini_net_wealth:.3f}'],['Top 10% share',f'{b.top10_share_net_wealth_pct:.1f}%',f'{n.top10_share_net_wealth_pct:.1f}%'],['Equivalised disposable-income Gini',f'{ib.gini_equivalised_income:.3f}',f'{inn.gini_equivalised_income:.3f}']]
    add(table(['2070 measure','Baseline','No ageing'],wealth_rows))
    add('**Interpretation:** the baseline’s lower overall net-wealth Gini and top-10% wealth share indicate less relative dispersion across current benefit units in these runs. A higher median with a lower mean is consistent with that result, but does not establish it by itself. Neither metric measures inequality between generations. The income Gini is recalculated from microdata; zero summary-export Gini fields are placeholders. [Wealth data](matlab/figures/wealth_distribution_annual.csv); [income data](matlab/figures/income_financial_2025_2070.csv).')
    add('### Wealth within ages and where wealth sits')
    fig('wealth_by_age_24plus','Mean and P90 benefit-unit net wealth by reference-person age, 2070',
        '**Read:** this is a **2070 snapshot**, not an average over 2025–2070. Solid lines show mean wealth; dashed lines show P90, recalculated within each age band. Unit age is the reference person’s age, while wealth belongs to the whole unit. Means can rise without comparable changes at the median.')
    fig('older_wealth_shares_2070','Older groups’ shares of current units and net wealth',
        f'**Read:** at 65+, baseline units are **{s65b.share_units_pct:.1f}% of units and hold {s65b.share_total_wealth_pct:.1f}% of wealth**, versus **{s65n.share_units_pct:.1f}% and {s65n.share_total_wealth_pct:.1f}%** without ageing. Mean wealth for 65+ units is **{ratios[0]:.2f} times** the under-65 mean in the baseline and **{ratios[1]:.2f} times** without ageing. The baseline therefore has the larger relative old-to-young mean wealth gap in these runs, although no-ageing units are wealthier within both broad age groups. Wealth shares and per-unit mean gaps measure different aspects of the age distribution. Share calculations recover ages; mean ratios use recorded ages.')
    age_rows=[]
    for j,label in enumerate(['Mean, reference person under 65','Mean, reference person 65+']): age_rows.append([label,*[money(means[c][j]) for c in CODES]])
    age_rows += [['65+ / under-65 mean',*[f'{v:.2f}' for v in ratios]],['65+ minus under-65 mean',*[money(means[c][1]-means[c][0]) for c in CODES]]]
    for label,r1,r2 in [('65+',s65b,s65n),('75+',s75b,s75n)]:
        age_rows += [[f'{label} share of units',f'{r1.share_units_pct:.1f}%',f'{r2.share_units_pct:.1f}%'],[f'{label} share of net wealth',f'{r1.share_total_wealth_pct:.1f}%',f'{r2.share_total_wealth_pct:.1f}%']]
    add(table(['2070 age comparison','Baseline','No ageing'],age_rows))
    fig('wealth_share_by_age_2070','Shares of total net wealth by reference-person age',
        '**Read:** each bar is the age group’s share of all current-unit wealth in its own scenario. Missing exported reference ages are recovered from the source unit. This describes the age location of wealth, not wealth owned by each individual. [Share and age-recovery inputs](matlab/figures/total_wealth_by_age_2070.csv).')
    add(r'''#### Age-group-mean Gini: a supporting diagnostic

Let $p_g$ be age group $g$’s share of current benefit units and $\mu_g$ its mean net wealth. For the eight listed age groups:

$$G_{\mathrm{age}}=\frac{\sum_g\sum_h p_g p_h|\mu_g-\mu_h|}{2\sum_g p_g\mu_g}.$$

This assigns each unit its group’s mean, excluding inequality **within** age groups. Zero means equal group means. It compares ages in 2070, not birth cohorts at the same age.''')
    add(table(['Age-group-mean Gini','Baseline','No ageing'],[
        ['Actual scenario age shares',f'{gb.gini_group_means_observed_age_mix:.3f}',f'{gn.gini_group_means_observed_age_mix:.3f}'],
        ['Same pooled age shares',f'{gb.gini_group_means_pooled_age_mix:.3f}',f'{gn.gini_group_means_pooled_age_mix:.3f}']]))
    add('The second row uses the pooled unit-count shares in both scenarios while retaining each scenario’s group means. Different weights can change the ordering. The metric depends on age bands and excludes within-age dispersion; it cannot be subtracted from the overall Gini to obtain the share “explained by age.” [Results](matlab/figures/age_group_wealth_gini_2070.csv); [unrounded group means and weights](matlab/figures/age_group_wealth_gini_detail_2070.csv).')
    add('### Which assets make up the stock difference?')
    fig('wealth_components_2025_2070','Net financial balances, housing equity and remaining private pension pots',
        '**Read:** means per current unit, in thousands of 2015-price pounds. Housing equity is gross property less mortgage; net financial balance is financial assets after unsecured debt. Pension stock is the remaining modelled pot, not pension income. Small child-trust amounts are omitted from the plotted components but included in total wealth.')
    stockrows=[]
    for field,label in [('mean_net_financial_balance','Net financial balance'),('mean_net_housing','Net housing equity'),('mean_private_pension_stock','Remaining pension pot'),('mean_child_trust_assets','Child trusts'),('mean_total_net_wealth','Total net wealth')]:
        stockrows.append([label,*[f'{r[field]/1000:.1f}' for r in [start,cb,cn]],f'{(cb[field]-cn[field])/1000:+.1f}'])
    add(table(['Mean (£ thousands)','2025, both','2070 baseline','2070 no ageing','Baseline − no ageing'],stockrows))
    add(f'Total simulated wealth is **£{tb.simulated_total_net_wealth/1e9:.2f}bn versus £{tn.simulated_total_net_wealth/1e9:.2f}bn**, across **{int(b.n_units):,} versus {int(n.n_units):,} current units**. Net financial balances rise from **{money(start.mean_net_financial_balance)} in 2025** to **{money(cb.mean_net_financial_balance)} baseline and {money(cn.mean_net_financial_balance)} no ageing**. Investment income enters disposable income; the amount not consumed can add to later wealth. Bequests, housing outcomes and alignment entrants also matter. Stocks locate the gap but do not allocate it causally to those processes. [Component data](matlab/figures/wealth_components_plot_2025_2070.csv).')
    add('### Recalculate the age-mix comparison')
    fig('age_mix_wealth_bridge_2070','2070 mean wealth with observed and reweighted age distributions',
        '**Read:** take the no-ageing mean wealth within each age band, then average those means using the baseline’s proportions of units at those ages. Wealth within each no-ageing band stays unchanged. The middle column is a recalculation, not a new simulation. It uses recorded ages, under 25, five-year bands through 85–89, and 90+.')
    mixrows=[]
    for r,label in [(mw,'Total net wealth'),(mf,'Net financial balance')]:
        mixrows.append([label,*[f'{r[k]/1000:.1f}' for k in ['counterfactual_age_known_mean','counterfactual_at_benchmark_age_shares','benchmark_age_known_mean']],f'{r.age_mix_part/1000:+.1f}',f'{r.within_age_part/1000:+.1f}'])
    add(table(['2070 mean (£ thousands)','No ageing','No ageing with baseline age shares','Baseline','Age-mix difference','Within-age difference'],mixrows))
    add('The financial measure in this bridge is total wealth minus housing equity and pension pots, including small child-trust balances. The component table above separates those trusts; the samples also differ because this bridge requires a recorded age.')
    add(f'''For total wealth, changing **only age-band proportions** adds **{money(mw.age_mix_part)}** to the no-ageing mean. But baseline mean wealth remains **{money(abs(mw.within_age_part))} lower** than that reweighted value. The two terms sum to the observed baseline-minus-no-ageing gap of **{money(mw.benchmark_minus_counterfactual)}**.

The baseline’s older composition tends to raise its mean, while the lower within-age means offset that tendency and more. Consequently, the former **“40% age composition / 60% other factors” conclusion does not apply**. With opposing terms and a comparatively small observed gap, percentage-of-gap labels are misleading. Neither term identifies causal effects of saving, returns or bequests. [Unrounded bridge](matlab/figures/income_financial_age_mix_5year_2070.csv).''')
    add('### Income and consumption per unit')
    fig('income_consumption_by_age_2070','Annual labour earnings, investment income and discretionary consumption by age',
        '**Read:** annual mean flows per current unit with recorded reference age. Use the broad-age table below for a slide; keep this detailed figure for questions. Investment income is net of debt costs. Disposable income includes investment and pension income after the model’s tax-and-benefit calculation; do not add investment income a second time when describing saving.')
    income_fields=[('mean_gross_labour_earnings','Gross labour earnings'),('mean_private_pension_income','Private pension income'),('mean_investment_income','Investment income'),('mean_disposable_income','Disposable income'),('mean_discretionary_consumption','Discretionary consumption'),('mean_disposable_less_consumption','Disposable income less discretionary consumption')]
    add(table(['Annual mean (£ thousands)','Baseline 18–64','No ageing 18–64','Baseline 65+','No ageing 65+'],[
        [label,*[f'{r[field]/1000:.1f}' for r in [ub,un,ob,on]]] for field,label in income_fields]))
    consumption_ratios=[100*r.mean_discretionary_consumption/r.mean_disposable_income for r in [ub,un,ob,on]]
    add(f'''**Interpretation:** under-65 gross labour earnings are **{money(ub.mean_gross_labour_earnings)} versus {money(un.mean_gross_labour_earnings)}**. The no-ageing investment-income difference is **{money(un.mean_investment_income-ub.mean_investment_income)} per unit**; that source is important alongside its disposable-income difference. The four gross/source rows do not sum to disposable income because taxes, benefits, other income and within-year exits intervene.

Under-65 discretionary consumption is **{consumption_ratios[0]:.1f}% versus {consumption_ratios[1]:.1f}%** of disposable income; the 65+ ratios are **{consumption_ratios[2]:.1f}% versus {consumption_ratios[3]:.1f}%**. Do not describe these as complete saving rates. Positive income-minus-consumption balances at older ages can support further wealth accumulation, but do not show that pension income alone is being saved. Different unit sizes and large investment incomes matter. Consumption is a modelled household outcome, not an endogenous aggregate-demand or GDP response. [Broad-age table](matlab/figures/income_consumption_broad_age_2070.csv).''')
    flow=read('age_group_flow_ginis_2070.csv')
    flowrows=[]
    for measure in flow.measure.unique():
        x,y=row_pair(flow,measure=measure)
        flowrows.append([measure,f'{x.gini_group_means_observed_age_mix:.3f}',f'{y.gini_group_means_observed_age_mix:.3f}',f'{x.gini_group_means_pooled_age_mix:.3f}',f'{y.gini_group_means_pooled_age_mix:.3f}'])
    add('#### Between-age differences in flow means\n\n'+table(['Seven-age-band Gini','Baseline actual mix','No ageing actual mix','Baseline common mix','No ageing common mix'],flowrows))
    add('These Ginis use seven age-band means and omit dispersion within age groups. Retirement makes labour earnings differ across ages by design. They are descriptive life-cycle diagnostics, not welfare rankings; income and consumption are per unit, not equivalised. The wealth diagnostic uses eight bands, so compare each measure across scenarios, not its numerical size against the wealth grouped Gini.')
    add('### Bequest receipts: timing, recipients and scale')
    bequestrows=[['Cash receipts, all ages, 2027–2070',*[f'£{v/1e9:.2f}bn' for v in totalcash]]]
    for label,r1,r2 in [('24–54',p24b,p24n),('55–74',p55b,p55n)]:
        bequestrows.append([f'Cash receipts by units aged {label}, 2027–2070',f'£{r1.cash_receipt/1e9:.2f}bn',f'£{r2.cash_receipt/1e9:.2f}bn'])
    for label,r1,r2 in [('24–54',p24b,p24n),('55–74',p55b,p55n)]:
        bequestrows.append([f'Average receipts per unit per year, {label}',money(r1.annual_cash_per_unit),money(r2.annual_cash_per_unit)])
    for label,r1,r2 in [('24–54',p24b,p24n),('55–74',p55b,p55n)]:
        bequestrows.append([f'Average yearly receipts / average yearly wealth, {label}',f'{r1.annual_cash_to_same_year_wealth_pct:.2f}%',f'{r2.annual_cash_to_same_year_wealth_pct:.2f}%'])
    add(table(['Bequest comparison','Baseline','No ageing'],bequestrows))
    add('**Denominators:** ages 24–54 and 55–74 include both endpoints and use the unit’s reference person. Receipts include cash paid to any member. Average receipts per unit per year equal period cash divided by unit-year observations, **including units receiving nothing**; the same person need not inherit annually. The ratio divides summed yearly receipts by summed net-wealth stocks of the **same units, age band and years**. Equivalently it divides average yearly receipts by average yearly wealth using the same observations. It is not the share of 2070 wealth caused by inheritance.')
    fig('bequest_receipts_by_unit_age','Annual bequest receipts by unit age and 2070 age-band receipts',
        '**Read:** the left panel shows the separate mean receipt in **each year** for units aged 24–54 and 55–74; it is not a 2027–2070 average. The right panel is a **2070 snapshot**. Both include non-recipients. The table above uses averages over 2027–2070. For ages 55–74, no ageing has slightly higher receipts averaged over the period, but the baseline is higher in 2070; the ranking depends on the year and age range.')
    fig('bequest_donor_recipient_age_flows','Cash bequest flows from deceased age groups to recipient age groups',
        '**Read:** cumulative immediate cash in 2027–2070, unscaled £ billions at 2015 prices. These are the deceased and recipient’s **own ages** at payment. Minor-trust allocations and wealth retained by a surviving partner are excluded. Deaths include natural and alignment deaths; emigrants do not generate bequests.')
    add(f'The no-ageing run has **£{(totalcash[1]-totalcash[0])/1e9:.2f}bn more cash receipts overall** and **£{(p24n.cash_receipt-p24b.cash_receipt)/1e9:.2f}bn more received by units aged 24–54** over 2027–2070. This is compatible with higher younger-unit wealth, but does not measure the causal contribution: wealth also reflects opening balances, saving, returns, housing, earlier transfers and entrants. Bequests redistribute existing wealth; they are not newly created aggregate wealth. [Updated recipient-age diagnostics](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_recipient_age/README.md>).')
    add('### People living in units within common wealth bands')
    bandrows=[]
    labels={'Below P10':f'Below P10 (<{money(b.p10)})','P25-median':f'P25–median ({money(b.p25)}–{money(b.p50)})','Median-P75':f'Median–P75 ({money(b.p50)}–{money(b.p75)})','Above P90':f'Above P90 ({money(b.p90)}+)'}
    for key in labels:
        x,y=row_pair(band,wealth_band=key)
        for name,r in zip(NAMES,[x,y]): bandrows.append([labels[key],name,*[f'{r[k]:.1f}%' for k in ['share_age30_44_pct','share_age45_59_pct','share_age60_74_pct','share_age75plus_pct']]])
    add(table(['Benefit-unit wealth band','Scenario','30–44','45–59','60–74','75+'],bandrows))
    add('**Read:** units are assigned a wealth band using the **baseline’s current 2070 cutoffs in both scenarios**, then their living members are counted by **their own ages**. Each percentage is a share of all people in that band, not a share of units. People under 30 remain in the denominator but are not shown, so rows need not sum to 100%. The high-wealth rows describe who lives in richer units, not how much each individual owns. [Table data](matlab/figures/adult_age_by_wealth_band_selected_2070.csv); [formatted figure](matlab/figures/adult_age_by_wealth_band_table_2070.png).')
    add('### Birth cohorts at the same age')
    fig('same_age_cohort_scenario_gap','Scenario gaps in median wealth at ages 55 and 50',
        '**Read:** compare the same approximate birth cohort at exactly the same age and calendar window in each scenario. Positive bars mean higher baseline median wealth. This removes a comparison across different calendar periods **between scenarios**, but does not isolate a pure cohort effect or track matched units.')
    fig('same_age_cohort_median_wealth','Median wealth by approximate birth cohort at ages 55 and 50',
        '**Read:** 2015-price pounds remove inflation, not real growth in wages, property or financial assets. Comparing levels across birth cohorts still mixes cohort and calendar-period conditions; use the same-cohort scenario gaps as the main experiment comparison.')
    for target_age in [55,50]:
        cr=[]
        for cohort_label,t in cohort.loc[cohort.reference_age.eq(target_age)].groupby('cohort_label',sort=True):
            first=t.iloc[0]; values=t.set_index('percentile').baseline_minus_no_ageing_pct
            cr.append([cohort_label,f'{int(first.year_start)}–{int(first.year_end)}',*[f'{values[k]:+.1f}%' for k in ['P10','P25','Median','P75','P90']]])
        add(f'**Age {target_age}: baseline minus no ageing at selected percentiles (%)**\n\n'+table(['Birth cohort','Years observed','P10','P25','Median','P75','P90'],cr))
    latest=cohort.loc[cohort.reference_age.eq(55)&cohort.cohort_label.eq('2010-2014')].set_index('percentile')
    add(f'For 2010–14 at age 55, the median gap is **{latest.loc["Median","baseline_minus_no_ageing_pct"]:+.1f}%**, compared with **{latest.loc["P75","baseline_minus_no_ageing_pct"]:+.1f}% at P75** and **{latest.loc["P90","baseline_minus_no_ageing_pct"]:+.1f}% at P90**. No ageing has higher wealth at all three cutoffs; its proportional advantage is largest at the median. This reverses the former upper-tail baseline advantage. These are separately recalculated percentiles, not gains for matched units. Changing membership, alignment and sampling can affect the gaps; one seed gives no uncertainty estimate. [Levels and counts](matlab/figures/same_age_cohort_wealth_50_55.csv); [percentile gaps](matlab/figures/same_age_cohort_percentile_gaps.csv).')
    housing=(Path(__file__).with_name('negative-gross-property-inspection-20261007.md')).read_text(encoding='utf-8')
    frequency=housing.split('## 2. Frequency in the current runs\n\n',1)[1].split('\n\nThe all-year counts',1)[0]
    add('## Housing check and interpretation limits\n\n'+frequency)
    add('''These housing counts refer to **all raw exported unit-year observations**, as in the separate audit; current-population figures above exclude exited units. A negative **net equity** balance can be valid, but negative **gross property** is impossible. The code predicts equity and mortgage separately, then sets gross property to their sum without enforcing `equity ≥ −mortgage`.

Most cases are small. Both scenarios share one **−£964k gross property** case in 2022, already present in V3. Its equity is −£1.123m against a £159k mortgage. The financial balance is a residual after housing, so the invalid negative housing value creates an offsetting positive financial balance and can affect subsequent returns and ownership. In 2070 the direct negative values are small, but that does **not** establish a negligible cumulative effect.

**We can use this refresh to inspect and draft descriptive results. A housing correction and rerun are needed before claiming that the wealth-gap mechanism or magnitude is robust.** Do not remove only the final affected rows as a repair: their earlier histories can already influence later outcomes. [Full housing report and reproducible script](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_negative_housing/README.md>).

The death and payment ledgers agree on deaths and recipients, and closing wealth matches the unit export. No estate accounting failure was detected. However, source asset-and-debt components immediately before every settlement are not separately exported, so a complete **independent estate-to-recipient conservation audit** is unavailable. This is a verification limit, not evidence that the bequest mechanism failed.

The [updated accumulation diagnostic](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_accumulation_channels/README.md>) separates recorded annual budget balances, investment income, housing returns, cash receipts and pension-stock changes for continuing IDs, retaining opening stocks and an unreconciled remainder. It does not causally allocate the cross-sectional within-age gap. A persistent unit ID can contain different members.''')
    add('''## Reproduction and current files

Run `refresh_all_figures` from the [MATLAB folder](matlab). It prepares current-population keys from the raw Person exports, checks source metadata, then regenerates the figures and CSV tables. The small Python preparation uses pandas and NumPy; its bundled-runtime location is configured in `prepare_ageing_population_keys.m`.

Input files are [baseline CSVs](simulation%20outcomes/baseline/csv) and [no-ageing CSVs](simulation%20outcomes/counterfactual_no_ageing/csv). Some CSV scenario codes retain `benchmark` for compatibility; `resolve_ageing_csv_dir.m` maps this to the **current baseline folder** and does not fall back to an archive. Display labels use **Baseline**.

The recipient-age scripts and plotting program are saved in [7 October recipient diagnostics](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_recipient_age/README.md>). The accumulation diagnostic is rerun separately with `inspect_wealth_accumulation_channels`, writing to its new dated folder. Raw outputs are unchanged.

The configurations remain `D:\\GitHub\\SimPaths\\config\\wealth_inequality_baseline_2070.yml` and `D:\\GitHub\\SimPaths\\config\\wealth_inequality_no_ageing_2070.yml`. [Analysis provenance](analysis_refresh_20261007.json) records the current input folders, denominator correction and refreshed figures. The previous analysis and older emails are historical records. The ageing section of the presentation source has been updated; the email already sent remains unchanged. [Response to colleague comments](colleague_response_current_20261007.md) records the current pension-transfer and retired-unit checks.''')
    (STUDY/'README.md').write_text('\n\n'.join(report)+'\n',encoding='utf-8')
    write_outline(b,n,cb,cn,tb,tn,s65b,s65n,ratios,ub,un,ob,on,mw,totalcash,p24b,p24n,latest)
    write_bequest_note(totalcash,period,audit)
    manifest={'refreshed':'2026-10-07','population_definition':'Living Person records; benefit units with at least one living member aged 18+',
        'status':'Provisional: negative gross housing consistency defect unresolved',
        'population_check':population,'input_folders':[str(STUDY/'simulation outcomes'/p/'csv') for p in ['baseline','counterfactual_no_ageing']],
        'wealth_source':'matlab/figures/wealth_distribution_annual.csv','bequest_diagnostics':str(DIAG/'diagnostics_20261007_recipient_age'),
        'figure_files':[p.name for p in sorted((STUDY/'matlab'/'figures').glob('*.png'))]}
    (STUDY/'analysis_refresh_20261007.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print('Updated README, slide outline, bequest note and provenance.')


def write_outline(b,n,cb,cn,tb,tn,sb,sn,ratios,ub,un,ob,on,mw,totalcash,pb,pn,latest):
    content=f'''# SimPaths wealth module: current ageing-application outline

**Updated 7 October 2026.** Use the [study README](README.md) and refreshed [figures](matlab/figures). The ageing tables and conclusions in **Wealth_module.tex have been refreshed**; the compiled review is **Wealth_module_refreshed_20261007.pdf**. Results use current living people and benefit units with a living adult. The earlier 2 October outline and results are archived in [previous analysis](analysis_before_refresh_20261007).

**Status:** suitable for provisional analysis and slide drafting. Negative gross housing values remain an unresolved model consistency defect; do not claim the final wealth-gap mechanism is validated. One seed (606), 2015-price pounds, unscaled simulated totals.

## Story in one sentence

With death-triggered bequests active, the two scenarios end with similar total wealth, but no ageing has fewer benefit units, a higher mean and more wealth among younger-headed units; the baseline has the higher median and more wealth located in older units.

## Suggested main application sequence

### 1. Why use ageing to demonstrate the wealth module? · 1 minute

Ask how an older population changes accumulated resources and living standards. The module connects pension pots, housing equity, financial assets, debt, saving and inheritance to income and consumption. Frame this as a demonstration of model capabilities, not a policy forecast.

### 2. Experiment design · 2 minutes

Two columns: baseline standard population projection; no ageing fixed 2026 age shares within each gender–region group from 2027. Both retain projected population totals, the same wealth settings and seed, and population/fertility/cohabitation alignment. **Targets differ; the alignment processes are common.** Simulate 2021–2070; analyse 2025–2070. Natural and alignment deaths trigger bequests; emigration does not.

### 3. Measurement conventions · 1 minute

Wealth and income belong to the whole benefit unit. Age charts use reference-person age; demographic charts use each person's age. Stock versus annual flow, 2015-price pounds, per-unit versus unscaled sample total. Exclude departed people and empty exited units. Under 65 means reference ages **18–64**.

### 4. The populations diverge · 2 minutes

**Show:** [age profiles and dependency ratios](matlab/figures/ageing_scenarios_65plus_10year.png). Both scenarios begin with common targets. Explain `100 × population aged 65+ / population aged 16–64`. Both lines are now shown; the counterfactual's realised ratio need not be perfectly flat.

### 5. Similar total wealth, different means · 2 minutes

**Show:** [annual mean wealth](matlab/figures/mean_net_wealth_per_unit_annual.png). In 2070 total wealth is **£{tb.simulated_total_net_wealth/1e9:.2f}bn baseline versus £{tn.simulated_total_net_wealth/1e9:.2f}bn no ageing**, but mean wealth is **{money(b['mean'])} versus {money(n['mean'])}** across **{int(b.n_units):,} versus {int(n.n_units):,} current units**. Do not retain the former claim that ageing produces a much higher mean. The paths compare changing populations of units.

### 6. The distribution is not captured by its mean · 2 minutes

**Show:** [P10–P90 wealth thresholds](matlab/figures/wealth_decile_levels_2070.png). The baseline median is **{money(b.p50)} versus {money(n.p50)}**, despite its lower mean. Explain cutoffs, not frequency bins. Optional supporting figures: Gini **{b.gini_net_wealth:.3f} versus {n.gini_net_wealth:.3f}**; top-10% shares **{b.top10_share_net_wealth_pct:.1f}% versus {n.top10_share_net_wealth_pct:.1f}%**. These concern overall dispersion, not intergenerational inequality.

### 7. Wealth at different ages · 2 minutes

**Show:** [mean and P90 wealth by age, 24+](matlab/figures/wealth_by_age_24plus.png). Label it **2070 snapshot**. Explain which ages have higher no-ageing wealth, and distinguish mean from a typical unit's median. P90 is calculated within each age band; these are not tracked cohorts.

### 8. Where wealth sits across ages · 2 minutes

**Show:** [older-unit and wealth shares](matlab/figures/older_wealth_shares_2070.png). Units aged 65+ hold **{sb.share_total_wealth_pct:.1f}% versus {sn.share_total_wealth_pct:.1f}%** of wealth and form **{sb.share_units_pct:.1f}% versus {sn.share_units_pct:.1f}%** of units. The 65+/under-65 mean ratios are **{ratios[0]:.2f} versus {ratios[1]:.2f}**: the baseline has the larger relative old-to-young mean wealth gap. Larger older-population shares and per-unit wealth advantages are distinct.

### 9. Bequests are visible · 2 minutes

**Show:** [receipts by unit age](matlab/figures/bequest_receipts_by_unit_age.png). The left panel is a separate receipt mean for each year; the right is 2070. All means include non-recipients. In 2027–2070 cash receipts total **£{totalcash[0]/1e9:.2f}bn versus £{totalcash[1]/1e9:.2f}bn**; units aged 24–54 receive **£{pb.cash_receipt/1e9:.2f}bn versus £{pn.cash_receipt/1e9:.2f}bn**. Do not suggest everyone inherits every year. Keep the README period table and [donor–recipient flow chart](matlab/figures/bequest_donor_recipient_age_flows.png) as supporting material. Transfers redistribute existing wealth, not create it.

### 10. Which assets accompany the difference? · 2 minutes

**Show:** [component stocks](matlab/figures/wealth_components_2025_2070.png). Baseline-minus-no-ageing differences in means: net financial balance **{money(cb.mean_net_financial_balance-cn.mean_net_financial_balance)}**, housing equity **{money(cb.mean_net_housing-cn.mean_net_housing)}**, remaining pension stock **{money(cb.mean_private_pension_stock-cn.mean_private_pension_stock)}**. These are stock locations, not causal contributions from processes. Explain that returns and unconsumed income can add to financial wealth; bequests, housing allocation and sample entrants also affect it.

### 11. Income and consumption · 3 minutes

**Show:** the [README broad-age table](README.md#income-and-consumption-per-unit), rebuilt as a native slide table. Under 65, labour earnings are **{money(ub.mean_gross_labour_earnings)} versus {money(un.mean_gross_labour_earnings)}**, disposable income **{money(ub.mean_disposable_income)} versus {money(un.mean_disposable_income)}**, and consumption **{money(ub.mean_discretionary_consumption)} versus {money(un.mean_discretionary_consumption)}**. At 65+, disposable income is **{money(ob.mean_disposable_income)} versus {money(on.mean_disposable_income)}**. Explain investment income, taxes, benefits and other income; source rows do not add directly to disposable income. Consumption is discretionary, not GDP or an endogenous aggregate-demand effect.

### 12. What the application shows · 1½ minutes

Close with: **(1)** similar total wealth can coexist with different means and medians; **(2)** ageing changes where wealth sits, while no ageing can increase wealth in younger units; **(3)** bequest flows and income profiles make the model's mechanisms visible, but do not isolate their causal contributions. End with the evidence limit: one seed and unresolved housing consistency. Avoid describing ageing as intrinsically “good for wealth accumulation”. Welcome questions.

## Supporting slides

### A. People within common wealth bands

Use the refreshed [README table](README.md#people-living-in-units-within-common-wealth-bands). These are living people classified by their own age within the benefit unit's wealth band; all under-30 people stay in the denominator. Both scenarios use the baseline's wealth thresholds. Rows need not sum to 100% across the displayed age columns.

### B. Same-age birth cohorts

Use the age-55 selected-percentile table in the [README](README.md#birth-cohorts-at-the-same-age). The 2010–14 age-55 median gap is **{latest.loc['Median','baseline_minus_no_ageing_pct']:+.1f}%**, P75 **{latest.loc['P75','baseline_minus_no_ageing_pct']:+.1f}%** and P90 **{latest.loc['P90','baseline_minus_no_ageing_pct']:+.1f}%**. Replace the old 22% upper-tail claim. Keep age 50 as sensitivity; these are separate distributions at common ages and calendar years, not matched-unit gains.

### C. Age composition versus within-age means

Use a native table from the refreshed [age-mix comparison](README.md#recalculate-the-age-mix-comparison). **{money(mw.counterfactual_age_known_mean)} no ageing → {money(mw.counterfactual_at_benchmark_age_shares)} with baseline age shares → {money(mw.benchmark_age_known_mean)} observed baseline.** The first change reflects **proportions**, holding within-band means fixed. The within-age difference opposes it. Remove the old 40/60 percentages; offsetting terms are clearer in pound amounts.

### D. Annual accounting and limitations

The [7 October accumulation diagnostic](<D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_accumulation_channels/README.md>) shows continuing-unit annual changes. It cannot allocate the full cross-sectional wealth gap because samples differ and opening wealth carries past accumulation. Use “Disposable income less investment income and consumption” and “Unreconciled wealth change”; retain the reconciliation limit explicitly.

### E. Housing issue and grouped inequality

The [README housing section](README.md#housing-check-and-interpretation-limits) gives frequency and consequences. Actual versus common-age-share grouped Ginis are supporting diagnostics, not measures of birth-cohort inequality. The housing correction has not been applied; the current refresh is provisional.

## Presentation preparation

This application sequence is approximately **22–25 minutes** plus questions. Combine slides 7 and 8, and move slide 10 to backup for a shorter slot. Keep the existing module-structure and validation material before it. Read the README's latest values when rebuilding tables; the Beamer ageing tables now use these refreshed values. The email already sent remains a historical draft.
'''
    (STUDY/'slide_outline_current_20261007.md').write_text(content,encoding='utf-8')
    (STUDY/'slide_outline_current_20261002.md').write_text('# Superseded slide outline\n\nUse the [7 October current outline](slide_outline_current_20261007.md). The original 2 October outline is retained in [the previous analysis archive](analysis_before_refresh_20261007/slide_outline_current_20261002.md).\n',encoding='utf-8')


def write_bequest_note(totalcash,period,audit):
    directory=DIAG/'diagnostics_20261007_recipient_age'
    rows=[]
    for age in ['24-54','55-74']:
        x,y=row_pair(period,age_group=age)
        rows.append([age,f'£{x.cash_receipt/1e9:.2f}bn',f'£{y.cash_receipt/1e9:.2f}bn',money(x.annual_cash_per_unit),money(y.annual_cash_per_unit),f'{x.annual_cash_to_same_year_wealth_pct:.2f}%',f'{y.annual_cash_to_same_year_wealth_pct:.2f}%'])
    text='''# Bequest recipients and current-population denominators — 7 October 2026

This reruns the recipient-age analysis on the current baseline and no-ageing exports. Natural and population-alignment deaths generate bequests; emigrants do not. Current units have a living adult aged 18+, and period per-unit means include units receiving nothing. Money is unscaled simulated 2015-price pounds.

## Period results, 2027–2070

'''+table(['Reference-person age','Baseline cash','No-ageing cash','Baseline per unit/year','No ageing per unit/year','Baseline receipts/wealth','No ageing receipts/wealth'],rows)+f'''

All-age immediate cash totals are £{totalcash[0]/1e9:.2f}bn baseline and £{totalcash[1]/1e9:.2f}bn no ageing. Period means divide receipts by unit-years. Flow-to-stock ratios divide summed annual receipts by summed wealth of the same units and years; they are not the contribution of inheritance to 2070 wealth.

## Checks and interpretation

All immediate cash recipients were present and living in the same-year Person export. Donors were recorded natural or alignment deaths. There were no emigration-donor overlaps, duplicate ledger rows, unmatched synthetic payments or recipient payments outside the unit join. Transfer ledgers are identical through 2026. `audit_results.json` and `additional_checks.json` record the checks. They do not independently certify every pre-settlement estate value.

The no-ageing result has more cash flowing to younger units. This is compatible with their higher observed wealth, but the two scenario runs do not isolate inheritance's causal effect. The housing consistency defect described in the study README remains unresolved.

## Reproduction

1. Run the study MATLAB `prepare_ageing_population_keys` to regenerate the active-unit keys from current source exports.
2. Run `analyse_bequests_by_age.py --study STUDY --out THIS_FOLDER` with pandas and NumPy.
3. After the study's wealth-by-age figure data is refreshed, run `refine_bequests_24_54_55_74.py --study STUDY --out THIS_FOLDER`.
4. In MATLAB, add this folder to the path and call `plot_bequest_diagnostics(THIS_FOLDER, STUDY_MATLAB_FIGURES)`.

The two plotted figures are `bequest_receipts_by_unit_age.png` and `bequest_donor_recipient_age_flows.png`. The line chart shows a separate mean in each year; its right panel is 2070. The period table uses 2027–2070.
'''
    (directory/'README.md').write_text(text,encoding='utf-8')
    (STUDY/'bequest_recipient_age_results_20261007.md').write_text(text,encoding='utf-8')


if __name__=='__main__': main()
