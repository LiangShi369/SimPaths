"""Stage the current-population corrections for review, then apply if requested."""
import argparse
import re
import shutil
from pathlib import Path
from prepare_analysis_refresh_20261007 import STUDY, DIAG, STAGE

FILTER = '''function t = filter_active_benefit_units(t, studyDir, scenario)
% Keep units with at least one living adult (18+) at the closing-year export.
% Raw exports also retain exited, zero-wealth units until next-year cleanup.
persistent cache
if isempty(cache), cache = containers.Map('KeyType','char','ValueType','any'); end
code=string(scenario); if code=="baseline", code="benchmark"; end
keyDir=fullfile(studyDir,'matlab','current_population_inputs');
keyFile=fullfile(keyDir,"active_benefit_units_"+code+".csv");
key=char(keyFile);
if ~isKey(cache,key)
    manifest=readtable(fullfile(keyDir,'input_manifest.csv'),'TextType','string');
    saved=manifest(manifest.scenario==code,:); assert(height(saved)==1);
    personFile=fullfile(resolve_ageing_csv_dir(studyDir,code),'Person.csv');
    info=dir(personFile); jf=javaObject('java.io.File',char(personFile));
    assert(info.bytes==saved.person_bytes && abs(double(jf.lastModified())-saved.person_modified_ms)<2,...
        'Population inputs changed: run prepare_ageing_population_keys first.');
    keys=readtable(keyFile); years=unique(keys.year); ids=cell(2070-2021+1,1);
    for year=years', ids{year-2021+1}=keys.benefit_unit_id(keys.year==year); end
    cache(key)=ids;
end
ids=cache(key);
if ismember('time',t.Properties.VariableNames), year=toNumber(t.time);
else, year=toNumber(t.year); end
if ismember('id_BenefitUnit',t.Properties.VariableNames), id=toNumber(t.id_BenefitUnit);
else, id=toNumber(t.benefit_unit_id); end
keep=false(height(t),1);
for yr=unique(year)'
    rows=year==yr; keep(rows)=ismember(id(rows),ids{round(yr)-2021+1});
end
t=t(keep,:);
end
function value=toNumber(value)
if ~isnumeric(value), value=str2double(string(value)); end
end
'''

PREP = '''function prepare_ageing_population_keys
% Rebuild current-population keys from raw exports before refreshing analysis.
matlabDir=fileparts(mfilename('fullpath')); studyDir=fileparts(matlabDir);
python='C:/Users/shili/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe';
if ~isfile(python), python='python'; end
command=sprintf('"%s" "%s" --study "%s" --out "%s"',python,...
    fullfile(matlabDir,'check_current_population_20261007.py'),studyDir,...
    fullfile(matlabDir,'current_population_inputs'));
[status,output]=system(command); fprintf('%s\\n',output);
assert(status==0,'Current-population preparation failed.');
clear filter_active_benefit_units
end
'''


def main():
    p=argparse.ArgumentParser(description=__doc__); p.add_argument('--apply',action='store_true'); args=p.parse_args()
    stage=STAGE/'current_population_matlab'; stage.mkdir(exist_ok=True)
    codes={
        'explore_wealth_divergence.m':'names{scenario}',
        'inspect_ageing_consumption.m':'scenarios{s}',
        'inspect_income_financial_gap.m':'scenarios{s}',
        'inspect_unsecured_debt_2070.m':'names{s}',
        'inspect_wealth_components_income.m':'scenarios{s}',
        'inspect_wealth_distributions.m':'names{scenario}',
        'inspect_young_wealth_2070.m':'scenarios{s}',
        'plot_same_age_cohorts.m':'scenarioCodes{s}',
        'plot_wealth_by_age_24plus.m':'names{scenario}',
        'plot_wealth_decile_histogram_2070.m':'names(s)',
        'plot_wealth_decile_levels_2070.m':'names(s)'}
    for source in (STUDY/'matlab').glob('*.m'):
        text=source.read_text(encoding='utf-8-sig')
        if source.name in codes:
            # All ds reads in these files read BenefitUnit.csv.
            text=re.sub(r'(\w+)\s*=\s*read\(ds\);',lambda m:
                m.group(0)+f' {m.group(1)}=filter_active_benefit_units({m.group(1)},studyDir,{codes[source.name]});',text)
            # Include unit ID in the selected fields wherever it was absent.
            text=re.sub(r"ds.SelectedVariableNames\s*=\s*\{.*?\};",lambda m:
                m.group(0) if "'id_BenefitUnit'" in m.group(0) else
                m.group(0).replace("'time'", "'id_BenefitUnit','time'",1),text,flags=re.S)
            if source.name=='inspect_wealth_components_income.m':
                text=text.replace("[{'time'},vars]", "[{'time','id_BenefitUnit'},vars]")
            if source.name in ['inspect_income_financial_gap.m','inspect_young_wealth_2070.m']:
                text=text.replace('ds.SelectedVariableNames = selected;', "ds.SelectedVariableNames = unique([selected,{'id_BenefitUnit'}],'stable');")
        if source.name=='plot_ageing_scenarios.m':
            text=text.replace("{'time','demAge','wgt'}", "{'time','demAge','wgt','demExitSample'}")
            text=text.replace('t = read(ds);', 't = read(ds); t=t(string(t.demExitSample)=="NotYet",:);')
        if source.name=='inspect_income_sources_by_age.m':
            text=text.replace("personNames = {'time','idBu'", "personNames = {'demExitSample','time','idBu'")
            text=text.replace('b = bu{k}; p = pp{k};', 'b = filter_active_benefit_units(bu{k},studyDir,scenarios{s}); p = pp{k}; p=p(string(p.demExitSample)=="NotYet",:);')
            text=text.replace("assert(all(matched),'A person has no benefit unit in %s %d.', ...\n            scenarios{s},years(k));", "assert(~any(~matched & numeric(p.demAge)>=18),'A living adult has no current benefit unit.');\n        p=p(matched,:); loc=loc(matched);")
        if source.name in ['plot_person_age_by_wealth_band_2070.m','tabulate_person_age_wealth_2070.m']:
            code='scenarioNames{s}' if source.name.startswith('plot_') else 'names{s}'
            text=text.replace('t=read(buDs);', f't=read(buDs); t=filter_active_benefit_units(t,studyDir,{code});')
            text=text.replace("{'time','idBu','demAge','wgt'}", "{'time','idBu','demAge','wgt','demExitSample'}")
            text=text.replace('t=read(pDs);', 't=read(pDs); t=t(string(t.demExitSample)=="NotYet",:);')
            # Exclude any surviving minor-only unit with no responsible adult.
            text=text.replace("assert(all(linked),'Person without a matching benefit unit.');",
                "assert(~any(~linked & personAge>=18),'Living adult without a current benefit unit.');\n    personAge=personAge(linked); weights=weights(linked); idx=idx(linked);")
            text=text.replace("assert(all(linked),'Person without a benefit unit.');",
                "assert(~any(~linked & personAge>=18),'Living adult without a current benefit unit.');\n    personAge=personAge(linked); weights=weights(linked); idx=idx(linked);")
            text=text.replace("assert(all(linked),'Some persons lack a matching 2070 benefit unit.');",
                "assert(~any(~linked & personAge>=18),'Living adult without a current benefit unit.');\n    personAge=personAge(linked); personWeight=personWeight(linked); idx=idx(linked);")
        if source.name=='plot_wealth_components_income.m':
            text=text.replace("{'year','minor_trust_assets_2015_gbp'}", "{'year','benefit_unit_id','minor_trust_assets_2015_gbp'}")
            text=text.replace('chunk=read(ds);', 'chunk=read(ds); chunk=filter_active_benefit_units(chunk,fileparts(scriptDir),scenarioNames(s));')
        if source.name=='inspect_wealth_accumulation_channels.m':
            text=text.replace('t=read(ds);', 't=read(ds); t=filter_active_benefit_units(t,studyDir,scenarios(s));',1)
            text=text.replace('Other budget remainder','Disposable income less investment income and consumption')
            text=text.replace('Unexplained remainder','Unreconciled wealth change')
        if source.name=='refresh_all_figures.m':
            text=text.replace('addpath(studyMatlab);', 'addpath(studyMatlab);\nprepare_ageing_population_keys;')
        (stage/source.name).write_text(text,encoding='utf-8')
    (stage/'filter_active_benefit_units.m').write_text(FILTER,encoding='utf-8')
    (stage/'prepare_ageing_population_keys.m').write_text(PREP,encoding='utf-8')
    shutil.copy2(Path(__file__).with_name('check_current_population_20261007.py'),stage/'check_current_population_20261007.py')
    # Adapt unit denominators in the new bequest analysis too.
    recipient=DIAG/'diagnostics_20261007_recipient_age'
    text=(recipient/'analyse_bequests_by_age.py').read_text(encoding='utf-8')
    text=text.replace("method = (d/'inheritance_method.txt').read_text().strip()", "method = (d/'inheritance_method.txt').read_text().strip()\n    key_file=study/'matlab'/'current_population_inputs'/f'active_benefit_units_{scenario}.csv'\n    active_keys=pd.MultiIndex.from_frame(pd.read_csv(key_file)[['year','benefit_unit_id']])")
    text=text.replace("chunk['year'] = chunk.time.round().astype(int)\n        unit_parts.append", "chunk['year'] = chunk.time.round().astype(int)\n        chunk=chunk.loc[pd.MultiIndex.from_frame(chunk[['year','id_BenefitUnit']]).isin(active_keys)]\n        unit_parts.append")
    (STAGE/'recipient_age'/'analyse_bequests_by_age.py').write_text(text,encoding='utf-8')
    print('Staged current-population analysis corrections:',stage)
    if args.apply:
        for source in stage.iterdir(): shutil.copy2(source,STUDY/'matlab'/source.name)
        shutil.copy2(STAGE/'recipient_age'/'analyse_bequests_by_age.py',recipient/'analyse_bequests_by_age.py')


if __name__=='__main__': main()
