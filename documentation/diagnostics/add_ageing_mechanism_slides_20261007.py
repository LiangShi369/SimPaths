"""Stage the requested median, pension and saving additions to current Beamer.

Uses refreshed CSV/JSON analysis, preserves other frames, and archives the
current source before applying. This never edits the simulation or its outputs.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil
import pandas as pd

STUDY=Path('D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality')
REPO=Path('D:/GitHub/SimPaths')
DECK=STUDY/'presentation HealthMod26Glasgow'
SOURCE=DECK/'Wealth_module.tex'
STAGE=REPO/'target/analysis_refresh_20261007/mechanism_slides'
FIG=STUDY/'matlab/figures'


def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()


def stage():
    STAGE.mkdir(parents=True,exist_ok=True)
    r=json.loads((REPO/'target/analysis_refresh_20261007/current_saving_pension_channels.json').read_text())
    s=SOURCE.read_text(encoding='utf-8')
    def replace_frame(title,body,newtitle=None,append=''):
        nonlocal s
        pattern=re.compile(r'(?m)^\\begin\{frame\}\{'+re.escape(title)+r'\}.*?^\\end\{frame\}',re.S)
        assert len(pattern.findall(s))==1,title
        replacement='\\begin{frame}{'+(newtitle or title)+'}\n\n'+body.strip()+'\n\n\\end{frame}'+append
        s=pattern.sub(lambda _:replacement,s)

    replace_frame('Divergence in wealth trajectories',r'''
\begin{center}
    \includegraphics[width=0.97\linewidth,height=0.65\textheight,keepaspectratio]{D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality/matlab/figures/mean_median_net_wealth_per_unit_annual.png}
\end{center}

\small
\begin{itemize}\setlength{\itemsep}{4pt}
    \item Annual cross-sectional wealth per current benefit unit, in 2015 prices.
    \item In 2070, no ageing has the higher mean, while the baseline has the higher median.
\end{itemize}
\normalsize
''','Mean and median wealth over time')
    replace_frame('How does age structure affect wealth accumulation?',r'''
Population ageing has effects on wealth that can work in opposite directions.

\vspace{8pt}
\small
\begin{itemize}\setlength{\itemsep}{6pt}
    \item \textbf{Age composition:} more older units, with larger accumulated balances, can raise average wealth.
    \item \textbf{Saving and returns:} assets earn returns while their owners are alive; income left after consumption can build wealth. Bequest recipients can also save and earn returns.
    \item \textbf{Pension access:} before annuitisation, the remaining pot can be inherited. After annuitisation, pension payments enter income while the recipient lives; the remaining pot is zero.
    \item \textbf{Wealth transfers:} deaths shift wealth to beneficiaries, changing who receives investment income and decides how much to consume. More cash reaches younger units without ageing in these runs.
\end{itemize}
\normalsize

\vspace{4pt}
{\footnotesize The channels interact; the two runs do not separately identify their causal contributions.\par}
''')

    bp=r['baseline']['payments_2027_2070']['pension_immediate_cash']
    np=r['counterfactual_no_ageing']['payments_2027_2070']['pension_immediate_cash']
    pension=r'''

% ------------------------------------------------------------
\begin{frame}{Bequests from pension pots before annuitisation}

\begin{center}
{\small
\setlength{\tabcolsep}{10pt}
\renewcommand{\arraystretch}{1.35}
\begin{tabular}{@{}lrr@{}}
\toprule
Cash transfers, 2027--2070 & Baseline & No ageing \\
\midrule
Remaining unannuitised pension pots & \poundsBASEbn & \poundsNOAGEbn \\
\bottomrule
\end{tabular}
}
\end{center}

\vspace{4pt}
\small
\begin{itemize}\setlength{\itemsep}{9pt}
    \item A remaining pot can pass to beneficiaries at death before pension access. Once annuitised, it is no longer an inheritable balance.
    \item Pension-pot cash transfers are about \textbf{INCREASE\% higher without ageing}. This is consistent with the pension-bequest channel, but death counts and pot sizes also matter.
\end{itemize}
\normalsize

\vspace{5pt}
{\footnotesize Unscaled simulated totals in 2015-price pounds. Immediate cash only; new allocations to minor trusts are reported separately.\par}

\end{frame}
'''.replace('BASE',f'{bp/1e9:.3f}').replace('NOAGE',f'{np/1e9:.3f}').replace('INCREASE',f'{100*(np/bp-1):.0f}')
    # Insert directly after the current bequest period table, before income.
    needle=r'\begin{frame}{Income and consumption per benefit unit}'
    assert s.count(needle)==1
    s=s.replace(needle,pension+'\n'+needle)

    keys=['at_least_one_nonretired_adult','all_living_members_18plus_retired']
    vals=[r[c]['groups'][k] for k in keys for c in ['baseline','counterfactual_no_ageing']]
    rows=[]
    for field,label in [('mean_total_net_wealth','Mean net wealth'),
                        ('mean_investment_income_annual','Annual investment income'),
                        ('mean_income_minus_discretionary_consumption','Annual income left after consumption')]:
        rows.append(label+' & '+' & '.join(f'{v[field]/1000:,.1f}' for v in vals)+r' \\')
    rows += [r'\addlinespace[2pt]',r'\textbf{Share of disposable income remaining} & '+
             ' & '.join(rf'\textbf{{{100*v["budget_surplus_over_income_ratio_of_sums"]:.1f}\%}}' for v in vals)+r' \\']
    saving=r'''

% ------------------------------------------------------------
\begin{frame}{How much income remains after consumption?}

\centering
{\footnotesize
\setlength{\tabcolsep}{4pt}
\renewcommand{\arraystretch}{1.15}
\begin{tabular}{@{}lrrrr@{}}
\toprule
 & \multicolumn{2}{c}{\shortstack{At least one adult\\not retired}}
 & \multicolumn{2}{c}{\shortstack{All adults\\retired}} \\
\cmidrule(lr){2-3}\cmidrule(l){4-5}
2070 mean & Baseline & No ageing & Baseline & No ageing \\
\midrule
'''+ '\n'.join(rows)+r'''
\bottomrule
\end{tabular}
}

\vspace{4pt}
{\footnotesize Amounts in thousand pounds per unit, 2015 prices; complete income/consumption records. Adults are living members aged 18+.\par}

\vspace{7pt}
\small
\begin{itemize}\setlength{\itemsep}{4pt}
    \item Share remaining \(=100\times(\sum Y-\sum C)/\sum Y\), with \(Y\) disposable income and \(C\) discretionary consumption.
    \item Retired units retain a larger income share than other units in both runs. Without ageing, retired units have higher wealth and investment income, but a slightly lower share remaining.
\end{itemize}
\normalsize

{\footnotesize Recurring budget measure; this is not a comprehensive saving rate.\par}

\end{frame}
'''
    needle=r'\begin{frame}{Summary: What the ageing experiment shows}'
    assert s.count(needle)==1
    s=s.replace(needle,saving+'\n'+needle)
    # Fit the refreshed bequest image within the existing slide body.
    s=s.replace(r'\includegraphics[width=0.86 \linewidth]{D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality/matlab/figures/bequest_receipts_by_unit_age.png}',
                r'\includegraphics[width=0.83 \linewidth]{D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality/matlab/figures/bequest_receipts_by_unit_age.png}')
    active='\n'.join(x for x in s.splitlines() if not x.lstrip().startswith('%'))
    assert active.count(r'\begin{frame}')==38 and active.count(r'\end{frame}')==38
    (STAGE/'Wealth_module.tex').write_text(s,encoding='utf-8')
    (STAGE/'manifest.json').write_text(json.dumps({'original_sha256':digest(SOURCE),'staged_sha256':digest(STAGE/'Wealth_module.tex'),'pages':38},indent=2),encoding='utf-8')
    # Save reusable table inputs alongside the staged slide source.
    rows=[]
    for code,contents in r.items():
        for group,v in contents['groups'].items(): rows.append({'scenario':code,'group':group,**v})
    pd.DataFrame(rows).to_csv(STAGE/'saving_propensity_2070.csv',index=False)
    print('Staged 38-page deck with mean/median chart and two new mechanism slides.')


def apply():
    m=json.loads((STAGE/'manifest.json').read_text())
    assert digest(SOURCE)==m['original_sha256'],'Source changed since staging.'
    backup=DECK/'archive_before_mechanism_updates_20261007'
    backup.mkdir(exist_ok=True)
    for suffix in ['.tex','.pdf']:
        p=SOURCE.with_suffix(suffix); dest=backup/p.name
        assert not dest.exists(),'Do not overwrite an earlier backup.'
        shutil.copy2(p,dest)
    shutil.copy2(STAGE/'Wealth_module.tex',SOURCE)
    print('Applied requested additions; source and existing PDF backed up.')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__); p.add_argument('--apply',action='store_true')
    args=p.parse_args(); apply() if args.apply else stage()
