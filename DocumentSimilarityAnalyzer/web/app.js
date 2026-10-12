const docsEl = document.querySelector('#documents');
const countEl = document.querySelector('#docCount');
const statusEl = document.querySelector('#status');
const thresholdEl = document.querySelector('#threshold');
const MAX_DOCS = 10;
let nextNumber = 1;
let lastResult = null;

function message(text, error = false) {
  statusEl.textContent = text;
  statusEl.style.color = error ? '#a4493e' : '';
}
function addDocument(name = '', text = '', wordBase64 = null) {
  if (docsEl.children.length >= MAX_DOCS) { message('Maximum 10 documents.', true); return; }
  const card = document.createElement('div');
  card.className = 'doc-card';
  card.wordBase64 = wordBase64;
  const header = document.createElement('div');
  header.className = 'doc-card-top';
  const label = document.createElement('span');
  label.textContent = `DOCUMENT ${nextNumber++}`;
  const remove = document.createElement('button');
  remove.className = 'remove'; remove.type = 'button'; remove.textContent = 'Remove ×';
  remove.addEventListener('click', () => { card.remove(); updateCount(); });
  header.append(label, remove);
  const input = document.createElement('input');
  input.placeholder = 'Document name'; input.setAttribute('aria-label', 'Document name');
  input.maxLength = 80; input.value = name || `Document ${nextNumber - 1}`;
  const area = document.createElement('textarea');
  area.placeholder = 'Paste your text here...'; area.setAttribute('aria-label', 'Document text');
  area.maxLength = 2500;
  area.value = wordBase64 ? 'Word file ready. Text will be extracted during analysis.' : text;
  area.readOnly = Boolean(wordBase64);
  const size = document.createElement('small');
  area.addEventListener('input', () => size.textContent = `${area.value.length} / 2,500 characters`);
  size.textContent = wordBase64 ? 'DOCX · up to 2,500 extracted characters' : `${area.value.length} / 2,500 characters`;
  card.append(header, input, area, size); docsEl.append(card); updateCount();
}
function updateCount() { countEl.textContent = `${docsEl.children.length} / ${MAX_DOCS}`; }
function readDocuments() {
  return [...docsEl.children].map(card => ({
    name: card.querySelector('input').value.trim(),
    text: card.querySelector('textarea').value,
    wordBase64: card.wordBase64
  }));
}
function readWordFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(',')[1]);
    reader.onerror = () => reject(new Error(`Could not read ${file.name}.`));
    reader.readAsDataURL(file);
  });
}
function pct(score) { return `${Number(score).toFixed(1)}%`; }
function element(tag, className, text) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  if (text !== undefined) el.textContent = text;
  return el;
}
function stat(label, value) {
  const box = element('div', 'stat');
  box.append(element('span', '', label), element('strong', '', value));
  return box;
}
function render(data) {
  lastResult = data;
  const section = document.querySelector('#results'); section.hidden = false;
  document.querySelector('#resultSubtitle').textContent = `${data.names.length} documents · ${data.pairs.length} unique comparisons · ${data.parallel ? 'parallel' : 'sequential'} mode`;
  const highest = data.pairs.reduce((best, pair) => pair.final > best.final ? pair : best);
  const duplicates = data.pairs.filter(pair => pair.duplicate).length;
  const stats = document.querySelector('#stats'); stats.replaceChildren(
    stat('DOCUMENTS', String(data.names.length)),
    stat('COMPARISONS', String(data.pairs.length)),
    stat('HIGHEST MATCH', pct(highest.final)),
    stat('POSSIBLE DUPLICATES', String(duplicates))
  );
  const table = document.querySelector('#matrix'); table.replaceChildren();
  const header = element('tr'); header.append(element('th', '', ''));
  data.names.forEach(name => header.append(element('th', '', name)));
  const head = element('thead'); head.append(header); table.append(head);
  const body = element('tbody');
  data.matrix.forEach((row, i) => {
    const tr = element('tr'); tr.append(element('th', '', data.names[i]));
    row.forEach((score, j) => tr.append(element('td', i === j ? 'self' : score >= data.threshold ? 'high' : '', pct(score))));
    body.append(tr);
  }); table.append(body);
  const target = document.querySelector('#target'); target.replaceChildren();
  data.names.forEach((name, i) => { const option = element('option', '', name); option.value = String(i); target.append(option); });
  showRanking();
  const cards = document.querySelector('#pairCards'); cards.replaceChildren();
  data.pairs.slice().sort((a,b) => b.final-a.final).forEach(pair => {
    const card = element('div', 'pair');
    const head = element('div', 'pair-head');
    head.append(element('strong', '', `${pair.a}  ↔  ${pair.b}`), element('b', '', pct(pair.final)));
    const badge = element('span', `badge ${pair.duplicate ? 'duplicate' : ''}`, `${pair.category}${pair.duplicate ? ' · Above threshold' : ''}`);
    const metrics = element('div', 'metrics');
    metrics.append(element('span', '', `Suffix score: ${pct(pair.suffix)}`), element('span', '', `Edit similarity: ${pct(pair.edit)}`), element('span', '', `Edit distance: ${pair.distance}`));
    const quote = element('blockquote', '', pair.longest ? `Longest shared text (${pair.longestLength} characters): “${pair.longest}”` : 'No shared substring found.');
    card.append(head, badge, metrics, quote); cards.append(card);
  });
  const groups = document.querySelector('#groups'); groups.replaceChildren();
  data.groups.forEach((group, i) => {
    const block = element('div', 'group');
    block.append(element('strong', '', `Group ${i + 1} · ${group.length} ${group.length === 1 ? 'document' : 'documents'}`), element('p', '', group.join(' · ')));
    groups.append(block);
  });
  section.scrollIntoView({behavior:'smooth', block:'start'});
}
function showRanking() {
  if (!lastResult) return;
  const index = Number(document.querySelector('#target').value);
  const list = document.querySelector('#ranking'); list.replaceChildren();
  const sorted = lastResult.rankings[index]; // Ranked by Java's merge sort.
  sorted.forEach((item, i) => {
    const li = element('li'); li.append(element('b', '', String(i + 1).padStart(2,'0')), element('span', '', item.name), element('strong', '', pct(item.score))); list.append(li);
  });
}
document.querySelector('#target').addEventListener('change', showRanking);
document.querySelector('#addBtn').addEventListener('click', () => addDocument());
thresholdEl.addEventListener('input', () => document.querySelector('#thresholdValue').textContent = `${thresholdEl.value}%`);
document.querySelector('#fileInput').addEventListener('change', async event => {
  const files = [...event.target.files];
  // Replace the two unused starting cards when files are uploaded.
  if (files.length && [...docsEl.children].every(card => !card.wordBase64 && !card.querySelector('textarea').value.trim())) {
    docsEl.replaceChildren(); nextNumber = 1; updateCount();
  }
  for (const file of files) {
    const lowerName = file.name.toLowerCase();
    if (!lowerName.endsWith('.txt') && !lowerName.endsWith('.docx')) {
      message('Only TXT and DOCX files are supported.', true); continue;
    }
    if (docsEl.children.length >= MAX_DOCS) { message('Maximum 10 documents.', true); break; }
    try {
      if (lowerName.endsWith('.docx')) {
        if (file.size > 1000000) { message(`${file.name} exceeds the 1 MB Word file limit.`, true); continue; }
        addDocument(file.name, '', await readWordFile(file));
      } else {
        const text = await file.text();
        if (text.length > 2500) { message(`${file.name} exceeds 2,500 characters.`, true); continue; }
        addDocument(file.name, text);
      }
    } catch (error) { message(error.message, true); }
  }
  event.target.value = '';
});
document.querySelector('#sampleBtn').addEventListener('click', () => {
  docsEl.replaceChildren(); nextNumber = 1;
  addDocument('Machine learning A.txt', 'Machine learning helps computers recognize patterns in data and make useful predictions. Researchers test these models on many examples.');
  addDocument('Machine learning B.txt', 'Machine learning helps computers recognize patterns in data and make useful predictions. Researchers improve these models with new examples.');
  addDocument('Data science.txt', 'Data science studies patterns in data. Researchers collect examples, clean the data, and make predictions using statistical methods.');
  addDocument('Football.txt', 'A football team trains each week to improve passing, defending and shooting. The players compete on a grass field.');
  message('Sample documents loaded.'); document.querySelector('#workspace').scrollIntoView({behavior:'smooth'});
});
document.querySelector('#analyzeBtn').addEventListener('click', async () => {
  const docs = readDocuments();
  if (docs.length < 2) { message('Please add at least two documents.', true); return; }
  if (docs.some(doc => !doc.name || (!doc.wordBase64 && !doc.text.trim()))) { message('Each document needs a name and text.', true); return; }
  if (new Set(docs.map(doc => doc.name.toLowerCase())).size !== docs.length) { message('Give each document a unique name.', true); return; }
  const params = new URLSearchParams(); params.set('count', String(docs.length));
  params.set('threshold', thresholdEl.value); params.set('parallel', String(document.querySelector('#parallel').checked));
  docs.forEach((doc,i) => {
    params.set(`name${i}`, doc.name);
    if (doc.wordBase64) {
      params.set(`format${i}`, 'docx'); params.set(`docx${i}`, doc.wordBase64);
    } else {
      params.set(`format${i}`, 'text'); params.set(`text${i}`, doc.text);
    }
  });
  const button = document.querySelector('#analyzeBtn'); button.disabled = true; message('Analyzing documents...');
  try {
    const response = await fetch('/api/analyze', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'}, body:params.toString()});
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Analysis failed.');
    render(data); message('Analysis completed successfully.');
  } catch (error) { message(error.message || 'Analysis failed.', true); }
  finally { button.disabled = false; }
});
addDocument('Document 1'); addDocument('Document 2');
