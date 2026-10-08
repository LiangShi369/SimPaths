"""Refresh existing Beamer ageing tables and conflicting narrative from CSVs.

Stages the source first; --apply checks its original SHA256 and backs up the
current source/PDF before changing the requested source. No model edits.
"""
import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path
import pandas as pd

STUDY = Path('D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality')
DECK = STUDY/'presentation HealthMod26Glasgow'
SOURCE = DECK/'Wealth_module.tex'
STAGE = Path('D:/GitHub/SimPaths/target/analysis_refresh_20261007/slide_update')
FIG = STUDY/'matlab/figures'
DIAG = Path('D:/BaiduSyncdisk/Job CeMPA/wealth and asset projection/07 bequest integration/diagnostics_20261007_recipient_age')


def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def stage():
    STAGE.mkdir(parents=True,exist_ok=True)
    s=SOURCE.read_text(encoding='utf-8')
    edits=[]
    def frame(title,body,newtitle=None):
        nonlocal s
        pattern=re.compile(r'(?m)^\\begin\{frame\}\{'+re.escape(title)+r'\}.*?^\\end\{frame\}',re.S)
        assert len(pattern.findall(s))==1,title
        replacement='\\begin{frame}{'+(newtitle or title)+'}\n\n'+body.strip()+'\n\n\\end{frame}'
        s=pattern.sub(lambda _:replacement,s)
        edits.append(newtitle or title)

    # Keep the agreed bequest rule consistent with the runs being shown.
    old='  \\item Population alignment may move persons out of simulation, which does not trigger bequests.'
    assert s.count(old)==1
    s=s.replace(old,'  \\item Natural deaths and population-alignment deaths trigger bequests; emigration exits do not.')
    edits.append('How bequests are transferred: exit treatment')
    old='  \\item \\textbf{Money:} amounts are in 2015-price pounds.'
    assert s.count(old)==1
    s=s.replace(old,old+'\n  \\item \\textbf{Population:} results count living people and units with a living adult aged 18+; exited units are excluded.')

    frame('Why is mean wealth higher in the baseline (ageing scenario)?',r'''
More older units can raise average wealth, but wealth within age groups also matters.

\vspace{8pt}
\small
\begin{itemize}\setlength{\itemsep}{6pt}
    \item \textbf{Age composition:} the baseline has more older units, which tend to hold larger accumulated balances.
    \item \textbf{Accumulation:} existing financial assets earn returns; income left after consumption and net housing returns can add to wealth.
    \item \textbf{Bequests:} transfers redistribute existing wealth. Without ageing, more cash reaches units aged 24--54, whose mean wealth is higher in 2070.
\end{itemize}
\normalsize
''','How does age structure affect wealth accumulation?')

    period=pd.read_csv(DIAG/'period_bequests_unit_age_24_54_55_74.csv')
    audit=json.loads((DIAG/'audit_results.json').read_text())
    def pr(code,age):
        row=period.loc[period.scenario.eq(code)&period.age_group.eq(age)]
        assert len(row)==1
        return row.iloc[0]
    codes=['benchmark','counterfactual_no_ageing']
    cash=[next(x['cash_2027_2070'] for x in audit if x['scenario']==c) for c in codes]
    rows=[rf'Total cash bequests received, all ages & \pounds{cash[0]/1e9:.2f}bn & \pounds{cash[1]/1e9:.2f}bn \\']
    for age in ['24-54','55-74']:
        a,b=[pr(c,age) for c in codes]
        label=age.replace('-','--')
        rows.append(rf'Average receipts per unit per year, ages {label} & \pounds{a.annual_cash_per_unit/1000:.2f}k & \pounds{b.annual_cash_per_unit/1000:.2f}k \\')
    rows.append(r'\addlinespace[2pt]')
    for age in ['24-54','55-74']:
        a,b=[pr(c,age) for c in codes]
        rows.append(rf'Average yearly receipts / average yearly net wealth, ages {age.replace("-","--")} & {a.annual_cash_to_same_year_wealth_pct:.2f}\% & {b.annual_cash_to_same_year_wealth_pct:.2f}\% \\')
    frame('Bequest receipts by age, 2027--2070',r'''
\begin{center}
{\small
\setlength{\tabcolsep}{6pt}
\renewcommand{\arraystretch}{1.15}
\begin{tabular}{@{}p{0.64\textwidth}rr@{}}
\toprule
Receipts, 2027--2070 & Baseline & No ageing \\
\midrule
'''+ '\n'.join(rows)+r'''
\bottomrule
\end{tabular}
}
\end{center}

\small
\begin{itemize}\setlength{\itemsep}{4pt}
    \item Ages refer to the unit's reference person. Yearly averages cover 2027--2070 and include units receiving nothing.
    \item The ratios use yearly receipts and net wealth of the same units and years; they do not measure inheritance's contribution to 2070 wealth.
    \item No-ageing units receive more per unit averaged over the period, especially at ages 24--54.
\end{itemize}
\normalsize
''')

    income=pd.read_csv(FIG/'income_consumption_broad_age_2070.csv')
    values=[income.loc[income.scenario.eq(c)&income.reference_age_group.eq(a)].iloc[0]
            for a in ['Under 65','65+'] for c in ['Baseline','No ageing']]
    rows=[]
    for field,label,bold in [('mean_gross_labour_earnings','Gross labour earnings',False),
                             ('mean_private_pension_income','Private pension income',False),
                             ('mean_investment_income','Investment income',False),
                             ('mean_disposable_income','Disposable income',True),
                             ('mean_discretionary_consumption','Discretionary consumption',True)]:
        label=rf'\textbf{{{label}}}' if bold else label
        cells=[f'{v[field]/1000:.1f}' for v in values]
        if bold: cells=[rf'\textbf{{{v}}}' for v in cells]
        rows.append(label+' & '+' & '.join(cells)+r' \\')
    frame('Income and consumption per benefit unit',r'''
\centering
{\footnotesize
\setlength{\tabcolsep}{6pt}
\renewcommand{\arraystretch}{1.15}
\begin{tabular}{@{}lcccc@{}}
\toprule
 & \multicolumn{2}{c}{Reference person aged 18--64}
 & \multicolumn{2}{c}{Reference person aged 65+} \\
\cmidrule(lr){2-3}\cmidrule(l){4-5}
Annual mean & Baseline & No ageing & Baseline & No ageing \\
\midrule
'''+ '\n'.join(rows)+r'''
\bottomrule
\end{tabular}
}

{\footnotesize Notes: 2070 annual means, thousand pounds per current unit in 2015 prices; recorded reference age.\par}

\vspace{4pt}
\small
\begin{itemize}\setlength{\itemsep}{4pt}
    \item Labour earnings are similar at ages 18--64. No-ageing units have higher investment income, disposable income and consumption in both broad age groups.
    \item Disposable income includes labour, investment, pension and other income after taxes, social contributions and benefits. Bequests and retirement lump sums are separate wealth inflows.
    \item Labour earnings sum living members' earnings at year-end. Taxes, benefits, other income and within-year exits mean the source rows do not add directly to disposable income.
\end{itemize}
\normalsize
''')

    frame('Summary: What the ageing experiment shows',r'''
\small
\begin{itemize}\setlength{\itemsep}{6pt}
    \item \textbf{Wealth levels:} total simulated wealth is similar (\pounds50.79bn baseline; \pounds50.99bn no ageing). No ageing has the higher mean per unit (\pounds1.588m versus \pounds1.525m); the baseline has the higher median (\pounds608k versus \pounds566k).
    \item \textbf{Where wealth sits:} units aged 65+ form 33.9\% of baseline units and hold 68.1\% of wealth, versus 26.0\% and 55.6\% without ageing.
    \item \textbf{Bequests:} more cash reaches younger units without ageing, alongside their higher mean wealth. These runs do not isolate the causal effect of inheritance.
    \item \textbf{Income and consumption:} no-ageing units have higher investment income, disposable income and consumption within both broad age groups; under-65 labour earnings are similar.
\end{itemize}
\normalsize

\vspace{4pt}
{\footnotesize Results are provisional: one run per scenario and an unresolved housing consistency issue.\par}
''')
    frame('BACK UP: Older units hold more of all wealth',r'''
\begin{figure}[!ht]
    \centering
    \includegraphics[width=0.78 \linewidth]{D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality/matlab/figures/older_wealth_shares_2070.png}
\end{figure}

\small
\begin{itemize}
    \item Older units hold a larger share of wealth in the baseline. Mean wealth at ages 65+ is 4.14 times the 18--64 mean, versus 3.57 without ageing: the baseline has the larger relative old-to-young mean gap.
\end{itemize}
\normalsize
''')

    cohort=pd.read_csv(FIG/'same_age_cohort_percentile_gaps.csv').query('reference_age==55')
    rows=[]
    for label,g in cohort.groupby('cohort_label',sort=True):
        first=g.iloc[0]
        v=g.set_index('percentile').baseline_minus_no_ageing_pct
        cells=[f'${v[p]:+.1f}\\%$' for p in ['P10','P25','Median','P75','P90']]
        years=f'{int(first.year_start)}--{int(first.year_end)}'
        a,b=label.split('-')
        rows.append(f'{a}--{b[-2:]} & {years} & '+' & '.join(cells)+r' \\')
    frame('BACK UP: At age 55, the wealth gap varies by birth cohort',r'''
\vspace{5pt}
{\centering
{\small
\setlength{\tabcolsep}{4pt}
\renewcommand{\arraystretch}{1.25}
\begin{tabular}{@{}lcrrrrr@{}}
\toprule
\shortstack[l]{Reference-person\\birth years} & \shortstack{Years\\at age 55}
& P10 & P25 & Median & P75 & P90 \\
\midrule
'''+ '\n'.join(rows)+r'''
\bottomrule
\end{tabular}
}
\par}

\small
\vspace{6pt}
\begin{itemize}\setlength{\itemsep}{4pt}
    \item Each row compares the same birth cohort at age 55 in the same calendar years; percentiles are calculated separately in each scenario.
    \item Gap = \(100\times(\mathrm{baseline}-\mathrm{no\ ageing})/\mathrm{no\ ageing}\).
    \item For 2010--14, baseline wealth is 30.6\% lower at the median, 28.2\% lower at P75 and 24.7\% lower at P90.
\end{itemize}
\normalsize
''')

    mix=pd.read_csv(FIG/'income_financial_age_mix_5year_2070.csv').set_index('metric')
    rows=[]
    for field,label in [('mean_total_net_wealth','Total net wealth'),('mean_net_financial_balance','Financial balance')]:
        r=mix.loc[field]
        cells=[f'{r[k]/1000:,.1f}' for k in ['counterfactual_age_known_mean','counterfactual_at_benchmark_age_shares','benchmark_age_known_mean']]
        cells += [f'${r[k]/1000:+,.1f}$' for k in ['age_mix_part','within_age_part']]
        rows.append(label+' & '+' & '.join(cells)+r' \\')
    frame('BACK UP: What changes when we swap the age shares?',r'''
\centering
{\footnotesize
\setlength{\tabcolsep}{4pt}
\renewcommand{\arraystretch}{1.35}
\begin{tabular}{@{}lccccc@{}}
\toprule
 & \shortstack{No\\ageing}
 & \shortstack{No ageing with\\baseline age shares}
 & Baseline
 & \shortstack{Age-mix\\difference}
 & \shortstack{Within-age\\difference} \\
\midrule
'''+ '\n'.join(rows)+r'''
\bottomrule
\end{tabular}
}

\vspace{4pt}
{\footnotesize Notes: 2070 mean, thousand pounds in 2015 prices; recorded reference age. Financial balance includes small child trusts.\par}

\vspace{8pt}
\small
\begin{itemize}\setlength{\itemsep}{5pt}
    \item The middle column changes only the \textbf{proportions of units in each age band}, keeping no-ageing mean wealth within each band unchanged.
    \item Baseline age shares add \pounds209.6k to mean wealth. Lower baseline wealth within age bands offsets this by \pounds275.8k, giving a net baseline-minus-no-ageing gap of $-\pounds66.2$k.
    \item The terms work in opposite directions. The former 40/60 split no longer applies; this is descriptive reweighting, not a causal decomposition.
\end{itemize}
\normalsize
''')
    active='\n'.join(x for x in s.splitlines() if not x.lstrip().startswith('%'))
    assert active.count(r'\begin{frame}')==36
    assert active.count(r'\end{frame}')==36
    (STAGE/'Wealth_module.tex').write_text(s,encoding='utf-8')
    manifest={'source':str(SOURCE),'original_sha256':digest(SOURCE),'updated_frames':edits,'staged_sha256':digest(STAGE/'Wealth_module.tex')}
    (STAGE/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(json.dumps(manifest,indent=2))


def apply():
    m=json.loads((STAGE/'manifest.json').read_text())
    assert digest(SOURCE)==m['original_sha256'],'Source changed since staging; restage against the current file.'
    backup=DECK/'archive_before_refresh_20261007'
    backup.mkdir(exist_ok=True)
    for suffix in ['.tex','.pdf']:
        p=SOURCE.with_suffix(suffix)
        dest=backup/p.name
        assert not dest.exists(),'Existing backup must not be overwritten.'
        shutil.copy2(p,dest)
    shutil.copy2(STAGE/'Wealth_module.tex',SOURCE)
    print('Updated existing source; original .tex and .pdf archived.')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply',action='store_true')
    args=parser.parse_args()
    apply() if args.apply else stage()
