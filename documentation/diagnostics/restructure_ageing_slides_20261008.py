"""Reorder existing ageing slides while preserving their contents and layouts."""
from pathlib import Path
import hashlib
import json
import re
import shutil
from datetime import datetime

deck = Path(r'D:\BaiduSyncdisk\Job CeMPA\CeMPA papers\ageing_and_inequality\step1_wealth_income_inequality\presentation HealthMod26Glasgow')
source = deck / 'Wealth_module.tex'
raw = source.read_bytes()
text = raw.decode('utf-8-sig')
# Blank full-line comments without changing character offsets.
visible = re.sub(r'(?m)^[ \t]*%[^\r\n]*', lambda m: ' ' * len(m.group()), text)
pattern = r'\\begin\{frame\}(?:\[[^\]]*\])?(?:\{[^\r\n]*?\})?[\s\S]*?\\end\{frame\}'
matches = list(re.finditer(pattern, visible))
frames = [text[m.start():m.end()] for m in matches]
assert len(frames) == 38, f'Unexpected slide count: {len(frames)}'
expected = {
    28: 'Bequest receipts per benefit unit across ages',
    29: 'Wealth per benefit unit across age bands',
    30: 'Which assets make up the wealth gap in 2070?',
    31: 'Net wealth across the distribution in 2070',
    32: 'Bequest receipts by age, 2027--2070',
    33: 'Income and consumption per benefit unit',
    34: 'Summary: What the ageing experiment shows',
    36: 'BACK UP: Older units hold more of all wealth',
}
for number, title in expected.items():
    assert '\\begin{frame}{' + title + '}' in frames[number - 1], (number, title)

order = list(range(1, 30)) + [36, 30, 33, 31, 34, 35, 32, 37, 38]
assert sorted(order) == list(range(1, 39))
reordered = [frames[n - 1] for n in order]
reordered[29] = reordered[29].replace('{BACK UP: Older units hold more of all wealth}', '{Older units hold more of all wealth}', 1)
reordered[35] = reordered[35].replace('{Bequest receipts by age, 2027--2070}', '{BACK UP: Bequest receipts by age, 2027--2070}', 1)
updated = text
for match, frame in reversed(list(zip(matches, reordered))):
    updated = updated[:match.start()] + frame + updated[match.end():]
assert updated.count('\\appendix') == text.count('\\appendix')
assert '\\appendix' in updated[updated.index('We welcome your questions and comments.'):]

backup = deck / ('archive_before_restructure_' + datetime.now().strftime('%Y%m%d_%H%M%S'))
backup.mkdir()
for name in ('Wealth_module.tex', 'Wealth_module.pdf'):
    path = deck / name
    if path.exists():
        shutil.copy2(path, backup / name)
assert source.read_bytes() == raw, 'Source changed while preparing the restructure'
encoding = 'utf-8-sig' if raw.startswith(b'\xef\xbb\xbf') else 'utf-8'
source.write_bytes(updated.encode(encoding))
manifest = {
    'source': str(source),
    'backup': str(backup),
    'sha256_before': hashlib.sha256(raw).hexdigest(),
    'sha256_after': hashlib.sha256(source.read_bytes()).hexdigest(),
    'main_slides': 35,
    'backup_slides': 3,
    'original_slide_order': order,
    'content_edits': ['Removed BACK UP from promoted wealth-share title', 'Added BACK UP to moved bequest-table title'],
}
output = Path(r'D:\GitHub\SimPaths\target\slide_restructure_20261008')
output.mkdir(parents=True, exist_ok=True)
(output / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
print(json.dumps(manifest, indent=2))
