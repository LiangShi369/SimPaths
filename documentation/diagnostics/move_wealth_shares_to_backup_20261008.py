"""Return the wealth-share slide to backup without changing slide wording."""
from pathlib import Path
from datetime import datetime
import hashlib
import json
import re
import shutil

deck = Path(r'D:\BaiduSyncdisk\Job CeMPA\CeMPA papers\ageing_and_inequality\step1_wealth_income_inequality\presentation HealthMod26Glasgow')
source = deck / 'Wealth_module.tex'
raw = source.read_bytes()
text = raw.decode('utf-8-sig')
visible = re.sub(r'(?m)^[ \t]*%[^\r\n]*', lambda m: ' ' * len(m.group()), text)
matches = list(re.finditer(r'\\begin\{frame\}(?:\[[^\]]*\])?(?:\{[^\r\n]*?\})?[\s\S]*?\\end\{frame\}', visible))
frames = [text[m.start():m.end()] for m in matches]
assert len(frames) == 38
assert frames[29].startswith(r'\begin{frame}{Older units hold more of all wealth}')
assert frames[34].startswith(r'\begin{frame}[c]')
assert frames[35].startswith(r'\begin{frame}{BACK UP: Bequest receipts by age, 2027--2070}')

# Move the slide through the appendix boundary, retaining its separate numbering.
# Move that boundary to just before the original closing-slide slot, which will
# now contain the first backup slide.
appendix_start = text.index(r'\renewcommand{\appendixname}{Backup slides}')
appendix_end = matches[35].start()
boundary = text[appendix_start:appendix_end]
assert r'\appendix' in boundary
assert matches[34].end() <= appendix_start
order = list(range(1, 30)) + [31, 32, 33, 34, 35, 30, 36, 37, 38]
assert sorted(order) == list(range(1, 39))
reordered = [frames[n - 1] for n in order]
reordered[34] = reordered[34].replace('{Older units hold more of all wealth}', '{BACK UP: Older units hold more of all wealth}', 1)

changes = [(m.start(), m.end(), f) for m, f in zip(matches, reordered)]
changes.append((appendix_start, appendix_end, ''))
changes.append((matches[34].start(), matches[34].start(), boundary))
# For identical start offsets, replace the frame before inserting the boundary.
updated = text
for start, end, replacement in sorted(changes, key=lambda item: (item[0], item[1]), reverse=True):
    updated = updated[:start] + replacement + updated[end:]
assert updated.count(r'\appendix') == text.count(r'\appendix')
assert updated.index('We welcome your questions and comments.') < updated.index(r'\appendix') < updated.index(r'\begin{frame}{BACK UP: Older units')

backup = deck / ('archive_before_wealth_share_backup_' + datetime.now().strftime('%Y%m%d_%H%M%S'))
backup.mkdir()
for name in ('Wealth_module.tex', 'Wealth_module.pdf'):
    if (deck / name).exists():
        shutil.copy2(deck / name, backup / name)
assert source.read_bytes() == raw, 'Source changed while preparing the move'
encoding = 'utf-8-sig' if raw.startswith(b'\xef\xbb\xbf') else 'utf-8'
source.write_bytes(updated.encode(encoding))
out = Path(r'D:\GitHub\SimPaths\target\wealth_share_backup_20261008')
out.mkdir(parents=True, exist_ok=True)
manifest = {'source': str(source), 'backup': str(backup), 'sha256_after': hashlib.sha256(source.read_bytes()).hexdigest(), 'main_slides': 34, 'backup_slides': 4, 'content_edits': ['Added BACK UP to the moved slide title']}
(out / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
print(json.dumps(manifest, indent=2))
