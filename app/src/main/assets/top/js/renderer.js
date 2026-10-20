'use strict';

const SEARCH_URLS = {
  google: 'https://www.google.com/search?q=',
  duckduckgo: 'https://duckduckgo.com/?q=',
  bing: 'https://www.bing.com/search?q='
};
const LOCAL_SEARCH_PAGE = 'file:///android_asset/search/index.html?q=';

// "You" yazınca "YouTube" gibi anında eşleşen, sık kullanılan siteler
const POPULAR_SITES = [
  { name: 'YouTube', url: 'https://www.youtube.com' },
  { name: 'Instagram', url: 'https://www.instagram.com' },
  { name: 'WhatsApp Web', url: 'https://web.whatsapp.com' },
  { name: 'Facebook', url: 'https://www.facebook.com' },
  { name: 'Twitter / X', url: 'https://www.x.com' },
  { name: 'Gmail', url: 'https://mail.google.com' },
  { name: 'Wikipedia', url: 'https://www.wikipedia.org' },
  { name: 'GitHub', url: 'https://www.github.com' },
  { name: 'Netflix', url: 'https://www.netflix.com' },
  { name: 'Spotify', url: 'https://open.spotify.com' },
  { name: 'Amazon', url: 'https://www.amazon.com' },
  { name: 'LinkedIn', url: 'https://www.linkedin.com' },
  { name: 'TikTok', url: 'https://www.tiktok.com' },
  { name: 'Reddit', url: 'https://www.reddit.com' },
  { name: 'Discord', url: 'https://discord.com' },
  { name: 'ChatGPT', url: 'https://chat.openai.com' },
  { name: 'Google Haritalar', url: 'https://maps.google.com' },
  { name: 'Google Drive', url: 'https://drive.google.com' },
  { name: 'Trendyol', url: 'https://www.trendyol.com' },
  { name: 'Hepsiburada', url: 'https://www.hepsiburada.com' }
];

let tabs = [];
let activeTabId = null;
let customSearchEnabled = false;
let lastSuggestQuery = '';

const el = {
  tabs: document.getElementById('tabs'),
  newTabBtn: document.getElementById('newTabBtn'),
  newPrivateTabBtn: document.getElementById('newPrivateTabBtn'),
  addressBar: document.getElementById('addressBar'),
  backBtn: document.getElementById('backBtn'),
  forwardBtn: document.getElementById('forwardBtn'),
  reloadBtn: document.getElementById('reloadBtn'),
  starBtn: document.getElementById('starBtn'),
  lockIcon: document.getElementById('lockIcon'),
  menuBtn: document.getElementById('menuBtn'),
  suggestBox: document.getElementById('suggestBox')
};

(function init() {
  try {
    const s = JSON.parse(window.AndroidHost.getSettingsJson());
    if (s.accentColor) document.documentElement.style.setProperty('--accent', s.accentColor);
    customSearchEnabled = !!(s.customSearchKey && s.customSearchCx);
    window.searchEngine = s.searchEngine || 'google';
  } catch (e) {}
})();

function getTab(id) { return tabs.find(t => t.id === id); }
function getActiveTab() { return getTab(activeTabId); }

function isLikelyUrl(input) {
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(input)) return true;
  if (/^localhost(:\d+)?$/i.test(input)) return true;
  if (/^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(input)) return true;
  if (/^[^\s]+\.[a-z]{2,}([\/?#].*)?$/i.test(input) && !input.includes(' ')) return true;
  return false;
}
function toUrl(input) {
  if (isLikelyUrl(input)) return /^[a-z][a-z0-9+.-]*:\/\//i.test(input) ? input : 'https://' + input;
  // Her zaman yerel Nexus arama sayfası — asla google.com'a yönlendirme yapma
  return LOCAL_SEARCH_PAGE + encodeURIComponent(input);
}

function createTab(isPrivate) {
  const id = window.AndroidHost.createTab(!!isPrivate);
  tabs.push({ id, title: isPrivate ? 'Gizli sekme' : 'Yeni sekme', url: '', pinned: false, private: !!isPrivate, canGoBack: false, canGoForward: false });
  activeTabId = id;
  renderTabs();
  updateToolbarForActiveTab();
}

function switchTab(id) {
  activeTabId = id;
  window.AndroidHost.switchTab(id);
  renderTabs();
  updateToolbarForActiveTab();
}

function closeTab(id) {
  window.AndroidHost.closeTab(id);
  tabs = tabs.filter(t => t.id !== id);
  if (activeTabId === id) activeTabId = tabs.length ? tabs[tabs.length - 1].id : null;
  renderTabs();
  updateToolbarForActiveTab();
}

function goToInput(raw) {
  const tab = getActiveTab();
  if (!tab) return;
  window.AndroidHost.navigate(tab.id, toUrl(raw));
}

function renderTabs() {
  el.tabs.innerHTML = '';
  tabs.forEach(tab => {
    const div = document.createElement('div');
    div.className = 'tab' + (tab.id === activeTabId ? ' active' : '') + (tab.private ? ' private' : '');
    const letter = (tab.title || 'N').trim().charAt(0).toUpperCase() || 'N';
    div.innerHTML = `<div class="favicon">${letter}</div><div class="title">${escapeHtml(tab.title || 'Yeni sekme')}</div><div class="close"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></div>`;
    div.querySelector('.title').addEventListener('click', () => switchTab(tab.id));
    div.querySelector('.favicon').addEventListener('click', () => switchTab(tab.id));
    div.querySelector('.close').addEventListener('click', (e) => { e.stopPropagation(); closeTab(tab.id); });
    el.tabs.appendChild(div);
  });
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function updateToolbarForActiveTab() {
  const tab = getActiveTab();
  el.addressBar.value = tab ? (tab.url || '') : '';
  const isHttps = tab && tab.url && tab.url.startsWith('https://');
  el.lockIcon.style.stroke = (tab && tab.url) ? (isHttps ? '#23a55a' : '#c9752f') : '#7a7d94';
  el.backBtn.disabled = !(tab && tab.canGoBack);
  el.forwardBtn.disabled = !(tab && tab.canGoForward);
  const bookmarked = tab && tab.url ? window.AndroidHost.isBookmarked(tab.url) : false;
  el.starBtn.classList.toggle('active-toggle', !!bookmarked);
}

window.NexusTop = {
  onNavigated(tabId, url, title, canGoBack, canGoForward) {
    const tab = getTab(tabId);
    if (!tab) return;
    tab.url = url;
    tab.title = title || url || 'Yeni sekme';
    tab.canGoBack = !!canGoBack;
    tab.canGoForward = !!canGoForward;
    renderTabs();
    if (tabId === activeTabId) updateToolbarForActiveTab();
  },
  onPrivacyUpdate(_count) {},
  onLiveSuggestions(query, suggestionsJson) {
    if (query !== lastSuggestQuery) return; // kullanıcı bu arada yazmaya devam etmiş, eski sonucu at
    try {
      const extra = JSON.parse(suggestionsJson);
      appendLiveSuggestions(extra);
    } catch (e) {}
  }
};

el.newTabBtn.addEventListener('click', () => createTab(false));
el.newPrivateTabBtn.addEventListener('click', () => createTab(true));

el.addressBar.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && el.addressBar.value.trim()) {
    goToInput(el.addressBar.value.trim());
    el.addressBar.blur();
    hideSuggestions();
  }
});
el.addressBar.addEventListener('focus', () => el.addressBar.select());
el.addressBar.addEventListener('input', () => {
  const q = el.addressBar.value.trim();
  lastSuggestQuery = q;
  if (!q) { hideSuggestions(); return; }

  const results = [];
  const qLower = q.toLowerCase();

  POPULAR_SITES.forEach(site => {
    if (site.name.toLowerCase().startsWith(qLower) || site.url.toLowerCase().includes(qLower)) {
      results.push({ title: site.name, sub: site.url, action: () => goToInput(site.url) });
    }
  });

  try {
    const local = JSON.parse(window.AndroidHost.getSmartSuggestionsJson(q));
    local.forEach(item => {
      if (results.length >= 6) return;
      results.push({ title: item.title || item.url, sub: item.url, action: () => goToInput(item.url) });
    });
  } catch (e) {}

  renderSuggestions(results.slice(0, 6));
  window.AndroidHost.fetchLiveSuggestions(q);
});
el.addressBar.addEventListener('blur', () => setTimeout(hideSuggestions, 150));

let currentSuggestions = [];
function renderSuggestions(list) {
  currentSuggestions = list;
  if (!list.length) { hideSuggestions(); return; }
  el.suggestBox.innerHTML = list.map((item, i) => `
    <div class="suggest-item" data-i="${i}">
      <div class="s-title">${escapeHtml(item.title)}</div>
      <div class="s-url">${escapeHtml(item.sub || '')}</div>
    </div>
  `).join('');
  el.suggestBox.querySelectorAll('.suggest-item').forEach(node => {
    node.addEventListener('mousedown', (e) => {
      e.preventDefault();
      const item = currentSuggestions[parseInt(node.getAttribute('data-i'), 10)];
      if (item) item.action();
      hideSuggestions();
      el.addressBar.blur();
    });
  });
  el.suggestBox.classList.add('open');
}
function appendLiveSuggestions(strings) {
  if (!strings.length || !el.suggestBox.classList.contains('open')) {
    if (!strings.length) return;
  }
  const existingTitles = new Set(currentSuggestions.map(s => s.title.toLowerCase()));
  const extra = strings
    .filter(s => !existingTitles.has(String(s).toLowerCase()))
    .slice(0, 6 - currentSuggestions.length)
    .map(s => ({ title: s, sub: '', action: () => goToInput(s) }));
  if (!extra.length) return;
  renderSuggestions(currentSuggestions.concat(extra));
}
function hideSuggestions() { el.suggestBox.classList.remove('open'); el.suggestBox.innerHTML = ''; currentSuggestions = []; }

el.backBtn.addEventListener('click', () => { const t = getActiveTab(); if (t) window.AndroidHost.goBack(t.id); });
el.forwardBtn.addEventListener('click', () => { const t = getActiveTab(); if (t) window.AndroidHost.goForward(t.id); });
el.reloadBtn.addEventListener('click', () => { const t = getActiveTab(); if (t) window.AndroidHost.reload(t.id); });

el.starBtn.addEventListener('click', () => {
  const t = getActiveTab();
  if (!t || !t.url) return;
  const nowBookmarked = window.AndroidHost.toggleBookmark(t.title || t.url, t.url);
  el.starBtn.classList.toggle('active-toggle', !!nowBookmarked);
});

el.menuBtn.addEventListener('click', () => window.AndroidHost.showPanel('settings'));

// ---------- Sekme Geçişi Gesture (swipe sağ/sola) ----------
let touchStartX = 0;
let touchEndX = 0;
document.body.addEventListener('touchstart', (e) => { touchStartX = e.changedTouches[0].screenX; }, { passive: true });
document.body.addEventListener('touchend', (e) => {
  touchEndX = e.changedTouches[0].screenX;
  handleTabSwipe();
}, { passive: true });
function handleTabSwipe() {
  const diff = touchStartX - touchEndX;
  if (Math.abs(diff) < 60) return;
  const tabs = JSON.parse(window.AndroidHost.getOpenTabsJson());
  const activeId = window.AndroidHost.getActiveTabId();
  const idx = tabs.findIndex(t => t.id === activeId);
  if (diff > 0 && idx < tabs.length - 1) {
    window.AndroidHost.switchTab(tabs[idx + 1].id);
  } else if (diff < 0 && idx > 0) {
    window.AndroidHost.switchTab(tabs[idx - 1].id);
  }
}

createTab(false);
