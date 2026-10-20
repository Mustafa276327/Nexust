'use strict';

const WEAK_PASSWORDS = ['123456', 'password', '12345678', 'qwerty', '111111', 'abc123', '123456789', 'sifre', '1234'];
const ACCENT_SWATCHES = ['#5b5bf0', '#ef4759', '#23a55a', '#e08a2c', '#7a4fe0', '#0ea5b7'];

// ---------- Çoklu Dil Sistemi ----------
const LANG = {
  tr: {
    'menu.privacy': 'Gizlilik ve Güvenlik',
    'menu.performance': 'Performans ve Veri',
    'menu.personalize': 'Kişiselleştirme',
    'menu.ai': 'Yapay Zeka',
    'menu.search': 'Arama Ayarları',
    'menu.history': 'Geçmiş',
    'menu.bookmarks': 'Yer İmleri',
    'menu.tools': 'Araçlar',
    'menu.nexus': 'NEXUS Bağlantısı',
    'menu.recentlyClosed': 'Son Kapatılan Sekmeler',
    'menu.readerMode': 'Okuma Modu',
    'menu.console': 'Geliştirici Konsolu',
    'menu.cookies': 'Çerez Yöneticisi',
    'menu.userAgent': 'User-Agent',
    'menu.downloads': 'İndirmeler',
    'menu.tabSearch': 'Sekme Ara',
    'menu.qrScan': 'QR Kod Tarayıcı',
    'menu.update': 'Güncellemeleri Kontrol Et',
    'tools.screenshot': 'Ekran Görüntüsü Al',
    'tools.pdf': 'Sayfayı PDF Olarak Kaydet',
    'tools.split': 'Bölünmüş Ekran',
    'tools.clearCache': 'Önbelleği Temizle',
    'lang.title': 'Dil',
    'lang.tr': 'Türkçe',
    'lang.en': 'İngilizce',
    'common.save': 'Kaydet',
    'common.close': 'Kapat',
    'common.empty': 'Henüz içerik yok',
  },
  en: {
    'menu.privacy': 'Privacy & Security',
    'menu.performance': 'Performance & Data',
    'menu.personalize': 'Personalization',
    'menu.ai': 'Artificial Intelligence',
    'menu.search': 'Search Settings',
    'menu.history': 'History',
    'menu.bookmarks': 'Bookmarks',
    'menu.tools': 'Tools',
    'menu.nexus': 'NEXUS Connection',
    'menu.recentlyClosed': 'Recently Closed Tabs',
    'menu.readerMode': 'Reader Mode',
    'menu.console': 'Developer Console',
    'menu.cookies': 'Cookie Manager',
    'menu.userAgent': 'User-Agent',
    'menu.downloads': 'Downloads',
    'menu.tabSearch': 'Search Tabs',
    'menu.qrScan': 'QR Code Scanner',
    'menu.update': 'Check for Updates',
    'tools.screenshot': 'Take Screenshot',
    'tools.pdf': 'Save Page as PDF',
    'tools.split': 'Split Screen',
    'tools.clearCache': 'Clear Cache',
    'lang.title': 'Language',
    'lang.tr': 'Turkish',
    'lang.en': 'English',
    'common.save': 'Save',
    'common.close': 'Close',
    'common.empty': 'No content yet',
  },
};

let CURRENT_LANG = 'tr';
function t(key) {
  return LANG[CURRENT_LANG][key] || key;
}
function setLang(lang) {
  CURRENT_LANG = lang;
  window.AndroidHost.setSetting('lang', JSON.stringify(lang));
  render('settings');
}

// ---------- Emojisiz özel ikon seti (tek merkezden) ----------
const ICONS = {
  history: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 3"/></svg>',
  bookmark: '<svg viewBox="0 0 24 24"><path d="M12 17.3 6.2 21l1.6-6.6L2.5 9.7l6.7-.6L12 3l2.8 6.1 6.7.6-5.3 4.7L18 21z"/></svg>',
  key: '<svg viewBox="0 0 24 24"><circle cx="8" cy="15" r="4"/><path d="M10.6 12.4 20 3M16 7l3 3M13 10l2 2"/></svg>',
  shield: '<svg viewBox="0 0 24 24"><path d="M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6z"/></svg>',
  lightning: '<svg viewBox="0 0 24 24"><path d="M13 2 4 14h6l-1 8 9-12h-6z"/></svg>',
  palette: '<svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 0 0 18c1.1 0 2-.9 2-2 0-.5-.2-.9-.5-1.3-.3-.4-.5-.8-.5-1.2 0-1.1.9-2 2-2h2a4 4 0 0 0 4-4c0-4.4-4.5-7.5-9-7.5z"/><circle cx="7.5" cy="11" r="1"/><circle cx="10.5" cy="7.3" r="1"/><circle cx="15.2" cy="8" r="1"/><circle cx="17" cy="12" r="1"/></svg>',
  ai: '<svg viewBox="0 0 24 24"><rect x="7" y="7" width="10" height="10" rx="2"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2 2M17.1 17.1l2 2M19.1 4.9l-2 2M6.9 17.1l-2 2"/></svg>',
  search: '<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>',
  sparkle: '<svg viewBox="0 0 24 24"><path d="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5Z"/><path d="M19 15l.6 1.9 1.9.6-1.9.6-.6 1.9-.6-1.9-1.9-.6 1.9-.6Z"/></svg>',
  grid: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg>',
  camera: '<svg viewBox="0 0 24 24"><path d="M4 8h3l2-2h6l2 2h3v11H4Z"/><circle cx="12" cy="13.5" r="3.3"/></svg>',
  printer: '<svg viewBox="0 0 24 24"><path d="M6 9V3h12v6M6 18H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1h16a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-2M6 14h12v7H6Z"/></svg>',
  split: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 12h18"/></svg>',
  refresh: '<svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 1 1-3-6.7M21 4v6h-6"/></svg>',
  tools: '<svg viewBox="0 0 24 24"><path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.6 2.6-2-2Z"/></svg>',
  check: '<svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg>',
  rocket: '<svg viewBox="0 0 24 24"><path d="M6 18c.5-2.5 1.8-6 4-8 3-3 6-4 8-4s0 5-3 8c-2 2-5.5 3.5-8 4Z"/><circle cx="13.5" cy="9.5" r="1.6"/><path d="M8 15.5 5.5 18M6 13l-2.5.8M11 18l-.8 2.5"/></svg>',
  scale: '<svg viewBox="0 0 24 24"><path d="M12 3v18M5 21h14M4 7h16"/><path d="M6 7l-3 6a3 3 0 0 0 6 0Z"/><path d="M18 7l-3 6a3 3 0 0 0 6 0Z"/></svg>',
  battery: '<svg viewBox="0 0 24 24"><rect x="2" y="8" width="18" height="8" rx="2"/><path d="M22 10.5v3"/><path d="M6 11v2"/></svg>',
  data: '<svg viewBox="0 0 24 24"><path d="M4 20h16"/><path d="M6 17l2.5-5 3 2.5L15 9l3 4"/></svg>',
  chevronLeft: '<svg viewBox="0 0 24 24"><path d="M15 18l-6-6 6-6"/></svg>',
  chevronRight: '<svg viewBox="0 0 24 24"><path d="M9 18l6-6-6-6"/></svg>',
  close: '<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>'
};
function icon(name, cls) { return `<span class="ico ${cls || ''}">${ICONS[name] || ''}</span>`; }

const el = {
  backBtn: document.getElementById('backBtn'),
  closeBtn: document.getElementById('closeBtn'),
  headTitle: document.getElementById('headTitle'),
  body: document.getElementById('body')
};
el.backBtn.innerHTML = ICONS.chevronLeft;
el.closeBtn.innerHTML = ICONS.close;

(function applyAccent() {
  try {
    const s = JSON.parse(window.AndroidHost.getSettingsJson());
    if (s.accentColor) document.documentElement.style.setProperty('--accent', s.accentColor);
  } catch (e) {}
})();

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function isWeakPassword(pw) {
  if (!pw) return true;
  if (pw.length < 8) return true;
  if (WEAK_PASSWORDS.includes(pw.toLowerCase())) return true;
  return false;
}
function setSetting(key, value) { window.AndroidHost.setSetting(key, JSON.stringify(value)); }

function setHeader(title, showBack) {
  el.headTitle.textContent = title;
  el.backBtn.style.visibility = showBack ? 'visible' : 'hidden';
}

// Görünümler arası yumuşak geçiş
function setBody(html) {
  el.body.style.opacity = '0';
  el.body.style.transform = 'translateY(6px)';
  el.body.innerHTML = html;
  requestAnimationFrame(() => {
    el.body.style.opacity = '1';
    el.body.style.transform = 'translateY(0)';
  });
}

let navStack = ['settings'];
function go(view, title, showBack) {
  navStack.push(view);
  setHeader(title, showBack !== false);
  render(view);
}
function goBackOne() {
  navStack.pop();
  const prev = navStack[navStack.length - 1] || 'settings';
  if (prev === 'settings') setHeader('Menü', false);
  render(prev);
}

window.NexusOverlay = {
  open(name) { navStack = ['settings']; setHeader('Menü', false); render(name || 'settings'); },
  onSummaryResult(text) {
    const box = document.getElementById('summaryResult');
    if (box) box.textContent = text;
  },
  onBreachResults(json) {
    const list = JSON.parse(json);
    const box = document.getElementById('breachResult');
    if (!box) return;
    if (!list.length) { box.innerHTML = '<div class="empty">Kayıtlı şifre yok</div>'; return; }
    box.innerHTML = list.map(r => {
      let status;
      if (r.breached === true) status = '<span class="pw-badge-weak">SIZINTIDA BULUNDU</span>';
      else if (r.breached === false) status = '<span style="color:#23a55a;font-size:11px;font-weight:700;">Temiz</span>';
      else status = '<span style="color:var(--text-dim);font-size:11px;">Kontrol edilemedi</span>';
      return `<div class="list-item"><div class="l-title">${escapeHtml(r.site)}</div><div class="l-sub">${status}</div></div>`;
    }).join('');
  },
  onGroupingResult(text) {
    const box = document.getElementById('groupResult');
    if (!box) return;
    if (text === 'NOKEY') { box.innerHTML = '<div class="empty">Önce Yapay Zeka ayarlarından Groq API anahtarı ekleyin</div>'; return; }
    try {
      const clean = text.trim().replace(/^```json/i, '').replace(/```$/, '');
      const data = JSON.parse(clean);
      box.innerHTML = (data.gruplar || []).map(g => `
        <div class="list-item">
          <div class="l-title">${icon('grid', 'sm')} ${escapeHtml(g.isim)}</div>
          <div class="l-sub">${(g.sekme_id || []).length} sekme</div>
        </div>
      `).join('') || '<div class="empty">Grup bulunamadı</div>';
    } catch (e) {
      box.innerHTML = `<div class="empty">${escapeHtml(text)}</div>`;
    }
  },
  onNexusTestResult(sonuc) {
    const box = document.getElementById('nexusTestSonuc');
    if (!box) return;
    if (sonuc === 'TOKEN_YOK') box.innerHTML = '<div class="empty">Önce NEXUS panel şifresini (X-Token) girin</div>';
    else if (sonuc.startsWith('OK:')) {
      const botOk = sonuc.slice(3) === 'true';
      box.innerHTML = `<div class="empty" style="color:#23a55a;">✅ Bağlandı — panel yanıt veriyor<br>Bot: ${botOk ? '🟢 canlı' : '⚪ duruyor görünüyor'}</div>`;
    } else if (sonuc.startsWith('HATA:')) box.innerHTML = `<div class="empty">Panel yanıt verdi ama hata kodu: ${escapeHtml(sonuc.slice(5))}</div>`;
    else box.innerHTML = `<div class="empty">Ulaşılamadı (${escapeHtml(sonuc.split(':')[1] || '?')}) — panel çalışıyor mu? Adres doğru mu?</div>`;
  },
  onBiometricResult(ok) {
    if (ok) {
      document.getElementById('bioWait').style.display = 'none';
      document.getElementById('bioContent').style.display = 'block';
      renderPasswordsList();
    } else {
      document.getElementById('bioWait').innerHTML = '<div class="empty">Biyometrik onay başarısız oldu. Şifre listesi görünmez.</div>';
    }
  },
};

function render(view) {
  if (view === 'settings') renderSettings();
  else if (view === 'history') renderHistory();
  else if (view === 'bookmarks') renderBookmarks();
  else if (view === 'passwords') renderPasswords();
  else if (view === 'privacy-cat') renderPrivacyCategory();
  else if (view === 'privacy') renderPrivacy();
  else if (view === 'performance') renderPerformance();
  else if (view === 'ai') renderAiCategory();
  else if (view === 'search-settings') renderSearchSettings();
  else if (view === 'personalize') renderPersonalize();
  else if (view === 'tools') renderTools();
  else if (view === 'summary') renderSummary();
  else if (view === 'nexus') renderNexusBridge();
  else if (view === 'downloads') renderDownloads();
  else if (view === 'recently-closed') renderRecentlyClosed();
  else if (view === 'tab-search') renderTabSearch();
  else if (view === 'reader-mode') renderReaderMode();
  else if (view === 'console') renderConsole();
  else if (view === 'cookies') renderCookies();
  else if (view === 'user-agent') renderUserAgent();
  else if (view === 'group') renderGroup();
  else if (view === 'split') renderSplit();
  else renderSettings();
}

function menuRow(id, iconName, label, sub) {
  return `<div class="menu-item" id="${id}">
    ${icon(iconName)}
    <div class="m-text"><div class="m-label">${label}</div>${sub ? `<div class="m-sub">${sub}</div>` : ''}</div>
    ${icon('chevronRight', 'chev')}
  </div>`;
}

// ---------- Üst düzey menü (kategoriler) ----------
function renderSettings() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  const curLang = s.lang || 'tr';
  setBody(`
    <div class="section-title">${t('lang.title')}</div>
    <div class="menu-item switch-row">${icon('palette')}<div class="m-text"><div class="m-label">${t('lang.title')}</div></div><select id="selLang">
      <option value="tr" ${curLang === 'tr' ? 'selected' : ''}>${t('lang.tr')}</option>
      <option value="en" ${curLang === 'en' ? 'selected' : ''}>${t('lang.en')}</option>
    </select></div>

    <div class="section-title">Gizlilik &amp; Kişiselleştirme</div>
    ${menuRow('goPrivacyCat', 'shield', t('menu.privacy'), 'Reklam engelleme, şifreler, gizlilik panosu')}
    ${menuRow('goPerformance', 'lightning', t('menu.performance'), 'Mod, sekme uyutma, önbellek')}
    ${menuRow('goPersonalize', 'palette', t('menu.personalize'), 'Vurgu rengi')}

    <div class="section-title">Arama &amp; Yapay Zeka</div>
    ${menuRow('goAi', 'ai', t('menu.ai'), 'Sayfa özeti, sekme gruplama')}
    ${menuRow('goSearchSettings', 'search', t('menu.search'), 'Markalı arama sonuçları')}

    <div class="section-title">Tarayıcı</div>
    ${menuRow('goHistory', 'history', t('menu.history'))}
    ${menuRow('goBookmarks', 'bookmark', t('menu.bookmarks'))}
    ${menuRow('goTools', 'tools', t('menu.tools'), 'Ekran görüntüsü, PDF, bölünmüş ekran')}
    ${menuRow('goNexus', 'rocket', t('menu.nexus'), 'Cihaz köprüsü: bildirimler, pil/cihaz bilgisi')}
    ${menuRow('doRecentlyClosed', 'history', t('menu.recentlyClosed'), 'Son 10 kapatılan sekmeyi geri aç')}
    ${menuRow('doReaderMode', 'rocket', t('menu.readerMode'), 'Reklamları gizle, metni büyüt')}
    ${menuRow('doConsole', 'tools', t('menu.console'), 'JS konsolu ve hataları')}
    ${menuRow('doCookies', 'shield', t('menu.cookies'), 'Site çerezlerini görüntüle ve sil')}
    ${menuRow('doUserAgent', 'rocket', t('menu.userAgent'), 'Masaüstü / Mobil mod geçişi')}
    ${menuRow('doDownloads', 'data', t('menu.downloads'), 'İndirme geçmişi ve dosya yönetimi')}
    ${menuRow('doTabSearch', 'grid', t('menu.tabSearch'), 'Açık sekmeler içinde arama yapın')}
    ${menuRow('doQrScan', 'camera', t('menu.qrScan'), 'Kamera ile QR kodu tarayın')}
    ${menuRow('doUpdate', 'refresh', t('menu.update'), 'Güncelleme adresi ayarlanmışsa kontrol eder')}
  `);
  document.getElementById('selLang').addEventListener('change', (e) => setLang(e.target.value));
  document.getElementById('goPrivacyCat').onclick = () => go('privacy-cat', t('menu.privacy'));
  document.getElementById('goPerformance').onclick = () => go('performance', t('menu.performance'));
  document.getElementById('goPersonalize').onclick = () => go('personalize', t('menu.personalize'));
  document.getElementById('goAi').onclick = () => go('ai', t('menu.ai'));
  document.getElementById('goSearchSettings').onclick = () => go('search-settings', t('menu.search'));
  document.getElementById('goHistory').onclick = () => go('history', t('menu.history'));
  document.getElementById('goBookmarks').onclick = () => go('bookmarks', t('menu.bookmarks'));
  document.getElementById('goTools').onclick = () => go('tools', t('menu.tools'));
  document.getElementById('goNexus').onclick = () => go('nexus', t('menu.nexus'));
  document.getElementById('doRecentlyClosed').onclick = () => go('recently-closed', t('menu.recentlyClosed'));
  document.getElementById('doReaderMode').onclick = () => { const id = window.AndroidHost.getActiveTabId(); if (id) window.AndroidHost.toggleReaderMode(id); window.AndroidHost.closePanel(); };
  document.getElementById('doConsole').onclick = () => go('console', t('menu.console'));
  document.getElementById('doCookies').onclick = () => go('cookies', t('menu.cookies'));
  document.getElementById('doUserAgent').onclick = () => go('user-agent', t('menu.userAgent'));
  document.getElementById('doDownloads').onclick = () => go('downloads', t('menu.downloads'));
  document.getElementById('doTabSearch').onclick = () => go('tab-search', t('menu.tabSearch'));
  document.getElementById('doQrScan').onclick = () => { window.AndroidHost.scanQrCode(); window.AndroidHost.closePanel(); };
  document.getElementById('doUpdate').onclick = () => window.AndroidHost.checkForUpdate();
}


// ---------- Gizlilik ve Güvenlik ----------
function renderPrivacyCategory() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  setBody(`
    <div class="section-title">Koruma</div>
    <div class="menu-item switch-row">${icon('shield')}<div class="m-text"><div class="m-label">Reklam engelleme</div></div><input type="checkbox" id="tAdBlock" ${s.adBlock ? 'checked' : ''} /></div>
    <div class="menu-item switch-row">${icon('shield')}<div class="m-text"><div class="m-label">İzleyici engelleme</div></div><input type="checkbox" id="tTrackerBlock" ${s.trackerBlock ? 'checked' : ''} /></div>
    <div class="menu-item switch-row">${icon('lightning')}<div class="m-text"><div class="m-label">HTTPS zorunlu modu</div></div><input type="checkbox" id="tHttpsOnly" ${s.httpsOnly ? 'checked' : ''} /></div>

    <div class="section-title">Diğer</div>
    ${menuRow('goPasswords', 'key', 'Şifre Yöneticisi')}
    ${menuRow('goPrivacyDash', 'shield', 'Gizlilik Panosu')}
  `);
  document.getElementById('tAdBlock').addEventListener('change', (e) => setSetting('adBlock', e.target.checked));
  document.getElementById('tTrackerBlock').addEventListener('change', (e) => setSetting('trackerBlock', e.target.checked));
  document.getElementById('tHttpsOnly').addEventListener('change', (e) => setSetting('httpsOnly', e.target.checked));
  document.getElementById('goPasswords').onclick = () => go('passwords', 'Şifre Yöneticisi');
  document.getElementById('goPrivacyDash').onclick = () => go('privacy', 'Gizlilik Panosu');
}

// ---------- Araçlar ----------
function renderTools() {
  setBody(`
    ${menuRow('doScreenshot', 'camera', t('tools.screenshot'))}
    ${menuRow('doPdf', 'printer', t('tools.pdf'))}
    ${menuRow('goSplit', 'split', t('tools.split'))}
    ${menuRow('goDownloads', 'data', t('menu.downloads'), 'İndirme geçmişi ve dosya yönetimi')}
    ${menuRow('doClearCache', 'shield', t('tools.clearCache'), 'Tüm sekmelerin WebView önbelleğini siler')}
    ${menuRow('doRecentlyClosed', 'history', t('menu.recentlyClosed'), 'Son 10 kapatılan sekmeyi geri aç')}
    ${menuRow('doQrScan', 'camera', t('menu.qrScan'), 'Kamera ile QR kodu tarayın')}
    ${menuRow('doTabSearch', 'grid', t('menu.tabSearch'), 'Açık sekmeler içinde arama yapın')}
    ${menuRow('doReaderMode', 'rocket', t('menu.readerMode'), 'Reklamları gizle, metni büyüt')}
    ${menuRow('doConsole', 'tools', t('menu.console'), 'JS konsolu ve hataları')}
    ${menuRow('doCookies', 'shield', t('menu.cookies'), 'Site çerezlerini görüntüle ve sil')}
    ${menuRow('doUserAgent', 'rocket', t('menu.userAgent'), 'Masaüstü / Mobil mod geçişi')}
    ${menuRow('doUpdate', 'refresh', t('menu.update'))}
  `);
  document.getElementById('doScreenshot').onclick = () => {
    const id = window.AndroidHost.getActiveTabId();
    if (id) window.AndroidHost.takeScreenshot(id);
    window.AndroidHost.closePanel();
  };
  document.getElementById('doPdf').onclick = () => {
    const id = window.AndroidHost.getActiveTabId();
    if (id) window.AndroidHost.printPageAsPdf(id);
    window.AndroidHost.closePanel();
  };
  document.getElementById('goSplit').onclick = () => go('split', t('tools.split'));
  document.getElementById('goDownloads').onclick = () => go('downloads', t('menu.downloads'));
  document.getElementById('doClearCache').onclick = () => {
    window.AndroidHost.clearWebViewCache();
    window.AndroidHost.closePanel();
  };
  document.getElementById('doRecentlyClosed').onclick = () => go('recently-closed', 'Son Kapatılanlar');
  document.getElementById('doQrScan').onclick = () => {
    window.AndroidHost.scanQrCode();
    window.AndroidHost.closePanel();
  };
  document.getElementById('doTabSearch').onclick = () => go('tab-search', t('menu.tabSearch'));
  document.getElementById('doReaderMode').onclick = () => {
    const id = window.AndroidHost.getActiveTabId();
    if (id) window.AndroidHost.toggleReaderMode(id);
    window.AndroidHost.closePanel();
  };
  document.getElementById('doConsole').onclick = () => go('console', t('menu.console'));
  document.getElementById('doCookies').onclick = () => go('cookies', t('menu.cookies'));
  document.getElementById('doUserAgent').onclick = () => go('user-agent', t('menu.userAgent'));
  document.getElementById('doUpdate').onclick = () => window.AndroidHost.checkForUpdate();
}

// ---------- Geçmiş ----------
function renderHistory() {
  const list = JSON.parse(window.AndroidHost.getHistoryJson());
  setBody(!list.length ? '<div class="empty">Henüz geçmiş yok</div>' : list.map(h => `
    <div class="list-item"><div class="l-title">${escapeHtml(h.title || h.url)}</div><div class="l-sub">${escapeHtml(h.url)}</div></div>
  `).join(''));
}

// ---------- Yer imleri ----------
function renderBookmarks() {
  const list = JSON.parse(window.AndroidHost.getBookmarksJson());
  setBody(!list.length ? '<div class="empty">Henüz yer imi yok</div>' : list.map(b => `
    <div class="list-item"><div class="l-title">${escapeHtml(b.title || b.url)}</div><div class="l-sub">${escapeHtml(b.url)}</div></div>
  `).join(''));
}

// ---------- Şifre yöneticisi ----------
const shownPasswords = new Set();
function renderPasswords() {
  setBody(`
    <div id="bioWait" style="text-align:center;padding:40px 10px;">
      <div class="empty" style="font-size:14px;">Şifre kasası erişimi için biyometrik onay bekleniyor…</div>
    </div>
    <div id="bioContent" style="display:none;">
      <div class="pw-form">
      <input id="pwSite" placeholder="Site (örn. example.com)" />
      <input id="pwUser" placeholder="Kullanıcı adı / e-posta" />
      <input id="pwPass" placeholder="Şifre" type="password" />
      <div class="pw-strength" id="pwStrength"></div>
      <select id="pwCat" style="width:100%;padding:14px;border:1px solid var(--border);border-radius:14px;background:var(--surface);color:var(--text);font-size:14px;margin-bottom:8px;">
        <option value="Diğer">Diğer</option>
        <option value="Sosyal Medya">Sosyal Medya</option>
        <option value="Banka">Banka</option>
        <option value="E-ticaret">E-ticaret</option>
        <option value="İş">İş</option>
        <option value="E-posta">E-posta</option>
        <option value="Streaming">Streaming</option>
      </select>
      <button class="primary-btn" id="pwSaveBtn">Kaydet</button>
    </div>
    <div class="section-title">Şifre Oluşturucu</div>
    <div class="pw-form" style="margin-top:0;">
      <div style="display:flex;gap:8px;align-items:center;margin-bottom:8px;">
        <div class="m-label">Uzunluk</div>
        <input type="number" id="pwLen" value="16" min="8" max="64" style="width:60px;" />
      </div>
      <div style="display:flex;gap:10px;flex-wrap:wrap;margin-bottom:8px;">
        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:12px;"><input type="checkbox" id="pwUpper" checked /> Büyük harf</label>
        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:12px;"><input type="checkbox" id="pwLower" checked /> Küçük harf</label>
        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:12px;"><input type="checkbox" id="pwDigits" checked /> Rakam</label>
        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:12px;"><input type="checkbox" id="pwSymbols" checked /> Sembol</label>
      </div>
      <button class="secondary-btn" id="pwGenBtn">${icon('sparkle', 'sm')} Şifre Üret</button>
    </div>
    <button class="secondary-btn" id="breachBtn">${icon('shield', 'sm')} Tüm Şifrelerde Sızıntı Kontrolü</button>
    <div id="breachResult"></div>
    <div id="pwList"></div>
  </div>
  `);
  const pwPass = document.getElementById('pwPass');
  const pwStrength = document.getElementById('pwStrength');
  pwPass.addEventListener('input', () => {
    const pw = pwPass.value;
    if (!pw) { pwStrength.textContent = ''; pwStrength.className = 'pw-strength'; return; }
    if (isWeakPassword(pw)) { pwStrength.textContent = 'Zayıf şifre — en az 8 karakter ve tahmin edilmesi zor bir şifre kullanın'; pwStrength.className = 'pw-strength weak'; }
    else { pwStrength.textContent = 'Şifre gücü uygun görünüyor'; pwStrength.className = 'pw-strength ok'; }
  });
  document.getElementById('pwSaveBtn').addEventListener('click', () => {
    const site = document.getElementById('pwSite').value.trim();
    const user = document.getElementById('pwUser').value.trim();
    const pass = pwPass.value;
    if (!site || !pass) return;
    const cat = document.getElementById('pwCat').value;
    window.AndroidHost.savePassword(site, user, pass, cat);
    document.getElementById('pwSite').value = ''; document.getElementById('pwUser').value = ''; pwPass.value = '';
    pwStrength.textContent = ''; pwStrength.className = 'pw-strength';
    renderPasswordsList();
  });
  document.getElementById('pwGenBtn').addEventListener('click', () => {
    const len = parseInt(document.getElementById('pwLen').value || '16', 10);
    const upper = document.getElementById('pwUpper').checked;
    const lower = document.getElementById('pwLower').checked;
    const digits = document.getElementById('pwDigits').checked;
    const symbols = document.getElementById('pwSymbols').checked;
    const pw = window.AndroidHost.generatePassword(len, upper, lower, digits, symbols);
    pwPass.value = pw;
    pwStrength.textContent = 'Oluşturulan şifre — kaydetmek için "Kaydet" düğmesine tıklayın';
    pwStrength.className = 'pw-strength ok';
  });
  document.getElementById('breachBtn').addEventListener('click', () => {
    document.getElementById('breachResult').innerHTML = '<div class="empty">Kontrol ediliyor…</div>';
    window.AndroidHost.checkBreachesAsync();
  });
  window.AndroidHost.authenticateWithBiometrics();
  renderPasswordsList();
}

function renderPasswordsList() {
  const listEl = document.getElementById('pwList');
  if (!listEl) return;
  const list = JSON.parse(window.AndroidHost.getPasswordsJson());
  if (!list.length) { listEl.innerHTML = '<div class="empty">Henüz kayıtlı şifre yok</div>'; return; }
  const catColors = { 'Sosyal Medya': '#ef4759', Banka: '#23a55a', 'E-ticaret': '#e08a2c', İş: '#5b5bf0', 'E-posta': '#0ea5b7', Streaming: '#7a4fe0', Diğer: '#7a7d94' };
  listEl.innerHTML = list.map(p => {
    const weak = isWeakPassword(p.password);
    const masked = shownPasswords.has(p.id) ? escapeHtml(p.password) : '•'.repeat(Math.max(6, p.password.length));
    const cat = p.category || 'Diğer';
    const catColor = catColors[cat] || '#7a7d94';
    return `
      <div class="pw-item">
        <div class="pw-top">
          <div style="display:flex;align-items:center;gap:6px;">
            <span style="font-size:10px;font-weight:800;color:${catColor};border:1px solid ${catColor};padding:2px 8px;border-radius:999px;background:rgba(0,0,0,.05);">${escapeHtml(cat)}</span>
            ${weak ? '<span class="pw-badge-weak">ZAYIF</span>' : ''}
          </div>
          <div><div class="pw-site">${escapeHtml(p.site)}</div><div class="pw-user">${escapeHtml(p.username)}</div></div>
          <button class="pw-del" data-id="${p.id}">Sil</button>
        </div>
        <div style="display:flex;align-items:center;gap:6px;">
          <div class="pw-secret" data-show="${p.id}" style="flex:1;">${masked}</div>
          <button class="icon-btn copy-btn" data-password="${encodeURIComponent(p.password)}" title="Panoya kopyala">${icon('camera')}</button>
        </div>
      </div>`;
  }).join('');
  listEl.querySelectorAll('[data-show]').forEach(node => node.addEventListener('click', () => {
    const id = node.getAttribute('data-show');
    if (shownPasswords.has(id)) shownPasswords.delete(id); else shownPasswords.add(id);
    renderPasswordsList();
  }));
  listEl.querySelectorAll('.copy-btn').forEach(btn => btn.addEventListener('click', () => {
    window.AndroidHost.copyToClipboard(decodeURIComponent(btn.getAttribute('data-password')));
  }));
  listEl.querySelectorAll('.pw-del').forEach(btn => btn.addEventListener('click', () => {
    window.AndroidHost.deletePassword(btn.getAttribute('data-id'));
    renderPasswordsList();
  }));
}

// ---------- Gizlilik panosu ----------
function renderPrivacy() {
  const count = window.AndroidHost.getPrivacyCount();
  setBody(`<div class="privacy-stat">${icon('shield', 'xl')}<div class="privacy-number">${count}</div><div class="privacy-label">bu oturumda engellenen reklam / izleyici isteği</div></div>`);
}

// ---------- Performans & Veri ----------
function renderPerformance() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  const modes = [['performans', 'rocket', 'Performans'], ['dengeli', 'scale', 'Dengeli'], ['tasarruf', 'battery', 'Tasarruf'], ['veri', 'data', 'Veri Tasarrufu']];
  setBody(`
    <div class="section-title">Mod</div>
    ${modes.map(([val, ic, label]) => `
      <div class="menu-item mode-item ${s.perfMode === val ? 'mode-active' : ''}" data-mode="${val}">
        ${icon(ic)}<div class="m-text"><div class="m-label">${label}</div></div>${s.perfMode === val ? icon('check', 'sm accent') : ''}
      </div>
    `).join('')}

    <div class="section-title">Detaylı Ayarlar</div>
    <div class="menu-item switch-row">${icon('battery')}<div class="m-text"><div class="m-label">Sekme uyutma</div><div class="m-sub">Arka plandaki sekmeleri duraklat</div></div><input type="checkbox" id="tSleep" ${s.tabSleep ? 'checked' : ''} /></div>

    <div class="field-block">
      <div class="m-label">Resim yükleme sınırı (MB, 0 = sınırsız)</div>
      <input type="number" id="imgLimit" value="${s.imageLimitMB || 0}" min="0" class="text-input" />
    </div>
    <div class="field-block">
      <div class="m-label">Önbellek sınırı (MB, 0 = sınırsız)</div>
      <input type="number" id="cacheLimit" value="${s.cacheLimitMB || 0}" min="0" class="text-input" />
    </div>
    <button class="primary-btn" id="savePerfBtn">Kaydet</button>
  `);
  el.body.querySelectorAll('[data-mode]').forEach(node => node.addEventListener('click', () => {
    setSetting('perfMode', node.getAttribute('data-mode'));
    render('performance');
  }));
  document.getElementById('tSleep').addEventListener('change', (e) => setSetting('tabSleep', e.target.checked));
  document.getElementById('savePerfBtn').addEventListener('click', () => {
    setSetting('imageLimitMB', parseInt(document.getElementById('imgLimit').value || '0', 10));
    setSetting('cacheLimitMB', parseInt(document.getElementById('cacheLimit').value || '0', 10));
    window.AndroidHost.closePanel();
  });
}

// ---------- Yapay Zeka (Groq ayarları + eylemler) ----------
function renderAiCategory() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  setBody(`
    <div class="section-title">Groq API</div>
    <div class="pw-form">
      <input id="groqKey" placeholder="Groq API anahtarı" type="password" value="${escapeHtml(s.groqApiKey || '')}" />
      <input id="groqModel" placeholder="Model adı" value="${escapeHtml(s.groqModel || 'llama-3.3-70b-versatile')}" />
      <button class="primary-btn" id="saveAiBtn">Kaydet</button>
    </div>
    <div class="empty" style="margin:0 0 18px 0;">Anahtar sadece bu cihazda saklanır.</div>

    <div class="section-title">Eylemler</div>
    ${menuRow('goSummary', 'sparkle', 'Sayfayı Özetle')}
    ${menuRow('goGroup', 'grid', 'Sekmeleri Grupla')}
  `);
  document.getElementById('saveAiBtn').addEventListener('click', () => {
    setSetting('groqApiKey', document.getElementById('groqKey').value.trim());
    setSetting('groqModel', document.getElementById('groqModel').value.trim() || 'llama-3.3-70b-versatile');
    window.AndroidHost.closePanel();
  });
  document.getElementById('goSummary').onclick = () => go('summary', 'Sayfayı Özetle');
  document.getElementById('goGroup').onclick = () => go('group', 'Sekmeleri Grupla');
}

// ---------- Arama Ayarları (Custom Search) ----------
function renderSearchSettings() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  const engines = [['google', 'Google'], ['duckduckgo', 'DuckDuckGo'], ['bing', 'Bing']];
  setBody(`
    <div class="section-title">Arama Motoru</div>
    <div class="pw-form">
      <select id="searchEngine">
        ${engines.map(([v, l]) => `<option value="${v}" ${s.searchEngine === v ? 'selected' : ''}>${l}</option>`).join('')}
      </select>
      <div class="empty" style="font-size:12px;margin-top:6px;">Bu motor adres çubuğundaki aramalar için kullanılır.</div>
    </div>

    <div class="section-title">Google Custom Search (isteğe bağlı)</div>
    <div class="pw-form">
      <input id="csKey" placeholder="API anahtarı" type="password" value="${escapeHtml(s.customSearchKey || '')}" />
      <input id="csCx" placeholder="Arama motoru kimliği (cx)" value="${escapeHtml(s.customSearchCx || '')}" />
      <button class="primary-btn" id="saveCsBtn">Kaydet</button>
    </div>
    <div class="empty" style="margin:0;">Bu ikisi girilince arama sonuçları Google'a gitmeden, kendi tasarımımızda gösterilir; veri yine Google'dan gelir. Boş bırakırsan arama normal şekilde çalışmaya devam eder.</div>
  `);
  document.getElementById('searchEngine').addEventListener('change', (e) => {
    setSetting('searchEngine', e.target.value);
  });
  document.getElementById('saveCsBtn').addEventListener('click', () => {
    setSetting('customSearchKey', document.getElementById('csKey').value.trim());
    setSetting('customSearchCx', document.getElementById('csCx').value.trim());
    window.AndroidHost.closePanel();
  });
}

// ---------- Kişiselleştirme ----------
function renderPersonalize() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  const theme = s.theme || (s.darkMode ? 'dark' : 'light');
  setBody(`
    <div class="section-title">Tema</div>
    <div class="menu-item switch-row">${icon('palette')}<div class="m-text"><div class="m-label">Tema Modu</div><div class="m-sub">Arayüz renk şemasını seçin</div></div>
      <select id="tTheme">
        <option value="light" ${theme === 'light' ? 'selected' : ''}>Aydınlık</option>
        <option value="dark" ${theme === 'dark' ? 'selected' : ''}>Karanlık</option>
        <option value="system" ${theme === 'system' ? 'selected' : ''}>Sistem Oto</option>
      </select>
    </div>
    <div class="section-title">Vurgu Rengi</div>
    <div class="swatch-row">
      ${ACCENT_SWATCHES.map(c => `<div class="swatch" data-color="${c}" style="background:${c};${s.accentColor === c ? 'border-color:#000;' : ''}"></div>`).join('')}
    </div>
  `);
  document.getElementById('tTheme').addEventListener('change', (e) => {
    setSetting('theme', e.target.value);
    if (e.target.value !== 'system') setSetting('darkMode', e.target.value === 'dark');
  });
  el.body.querySelectorAll('[data-color]').forEach(node => node.addEventListener('click', () => {
    setSetting('accentColor', node.getAttribute('data-color'));
    render('personalize');
  }));
}

function renderSummary() {
  setBody(`<div class="empty" id="summaryResult">Analiz ediliyor… (birkaç saniye sürebilir)</div>`);
  const id = window.AndroidHost.getActiveTabId();
  if (id) window.AndroidHost.aiSummarizePage(id);
  else document.getElementById('summaryResult').textContent = 'Aktif sekme bulunamadı';
}

// ---------- Sekmeleri Grupla ----------
function renderGroup() {
  setBody(`<div class="empty" id="groupResult">Gruplanıyor…</div>`);
  try {
    const tabs = JSON.parse(window.AndroidHost.getOpenTabsJson());
    window.AndroidHost.groupTabsAI(JSON.stringify(tabs));
  } catch (e) {
    document.getElementById('groupResult').textContent = 'Hata: ' + e.message;
  }
}

// ---------- Bölünmüş Ekran ----------
function renderSplit() {
  const tabs = JSON.parse(window.AndroidHost.getOpenTabsJson());
  const activeId = window.AndroidHost.getActiveTabId();
  const others = tabs.filter(t => t.id !== activeId);
  setBody(`
    <button class="secondary-btn" id="exitSplitBtn" style="margin-bottom:16px;">Bölünmüş Ekranı Kapat</button>
    <div class="section-title">İkinci sekmeyi seçin</div>
    ${!others.length ? '<div class="empty">Bölmek için başka açık sekme yok</div>' : others.map(t => menuRow('pick-' + t.id, 'split', t.title)).join('')}
  `);
  document.getElementById('exitSplitBtn').addEventListener('click', () => { window.AndroidHost.exitSplitView(); window.AndroidHost.closePanel(); });
  others.forEach(t => {
    const node = document.getElementById('pick-' + t.id);
    if (node) node.addEventListener('click', () => { window.AndroidHost.enterSplitView(t.id); window.AndroidHost.closePanel(); });
  });
}

// ---------- Sekme Arama ----------
function renderTabSearch() {
  const tabs = JSON.parse(window.AndroidHost.getOpenTabsJson());
  const activeId = window.AndroidHost.getActiveTabId();
  setBody(`
    <div class="section-title">Sekme Ara</div>
    <div class="pw-form" style="margin-bottom:16px;">
      <input id="tabSearchInput" placeholder="Sekme başlığı veya URL…" type="text" />
    </div>
    <div id="tabSearchResults"></div>
  `);
  const input = document.getElementById('tabSearchInput');
  const results = document.getElementById('tabSearchResults');
  function renderSearch(q) {
    const filtered = tabs.filter(t =>
      (t.title || '').toLowerCase().includes(q.toLowerCase()) ||
      (t.url || '').toLowerCase().includes(q.toLowerCase())
    );
    if (!filtered.length) {
      results.innerHTML = '<div class="empty">Eşleşen sekme bulunamadı</div>';
      return;
    }
    results.innerHTML = filtered.map(t => `
      <div class="list-item" style="display:flex;justify-content:space-between;align-items:center;">
        <div style="flex:1;min-width:0;">
          <div class="l-title">${escapeHtml(t.title || 'Yeni sekme')}</div>
          <div class="l-sub">${escapeHtml(t.url || '')} ${t.private ? '• Gizli' : ''}</div>
        </div>
        <button class="icon-btn" onclick="window.AndroidHost.switchTab('${t.id}'); window.AndroidHost.closePanel();" title="Geç">${icon('chevronRight')}</button>
      </div>
    `).join('');
  }
  input.addEventListener('input', (e) => renderSearch(e.target.value));
  renderSearch('');
}

// ---------- Reader Mode ----------
function renderReaderMode() {
  const id = window.AndroidHost.getActiveTabId();
  const isOn = id ? window.AndroidHost.getReaderMode(id) : false;
  setBody(`
    <div class="section-title">Okuma Modu</div>
    <div class="menu-item switch-row">${icon('rocket')}<div class="m-text"><div class="m-label">Aktif</div><div class="m-sub">Reklamları gizle, metni büyüt</div></div><input type="checkbox" id="rmToggle" ${isOn ? 'checked' : ''} /></div>
    <div class="empty" style="margin-top:16px;">Aktif sekmede okuma modunu aç/kapatabilirsiniz.</div>
  `);
  document.getElementById('rmToggle').addEventListener('change', (e) => {
    const id = window.AndroidHost.getActiveTabId();
    if (id) window.AndroidHost.toggleReaderMode(id);
  });
}

// ---------- Geliştirici Konsolu ----------
function renderConsole() {
  const logs = JSON.parse(window.AndroidHost.getConsoleLogsJson());
  setBody(`
    <div class="section-title">Geliştirici Konsolu</div>
    <button class="secondary-btn" id="clearConsole" style="margin-bottom:16px;">🗑️ Konsolu Temizle</button>
    <div id="consoleLogs"></div>
  `);
  document.getElementById('clearConsole').onclick = () => {
    window.AndroidHost.clearConsoleLogs();
    document.getElementById('consoleLogs').innerHTML = '';
  };
  const listEl = document.getElementById('consoleLogs');
  if (!logs.length) {
    listEl.innerHTML = '<div class="empty">Henüz konsol kaydı yok</div>';
  } else {
    listEl.innerHTML = logs.map(l => {
      const isError = l.startsWith('[error]') || l.startsWith('[warn]');
      return `<div class="list-item" style="border-left:3px solid ${isError ? '#ef4759' : '#5b5bf0'};">
        <div style="font-family:monospace;font-size:12px;white-space:pre-wrap;color:${isError ? '#ef4759' : 'var(--text-dim)'}">${escapeHtml(l)}</div>
      </div>`;
    }).join('');
  }
}

// ---------- Çerez Yöneticisi ----------
function renderCookies() {
  const activeId = window.AndroidHost.getActiveTabId();
  const tabs = JSON.parse(window.AndroidHost.getOpenTabsJson());
  const activeTab = tabs.find(t => t.id === activeId);
  const url = activeTab ? (activeTab.url || '') : '';
  setBody(`
    <div class="section-title">Çerez Yöneticisi</div>
    ${!url ? '<div class="empty">Aktif sekme bulunamadı</div>' : ''}
    ${url ? `
    <div class="pw-form" style="margin-bottom:16px;">
      <div class="l-sub" style="margin-bottom:8px;">Site: ${escapeHtml(url)}</div>
      <button class="secondary-btn" id="refreshCookies" style="width:auto;padding:10px 16px;">↻ Yenile</button>
      <button class="secondary-btn" id="deleteAllCookies" style="width:auto;padding:10px 16px;color:var(--danger);margin-left:8px;">Tümünü Sil</button>
    </div>
    <div id="cookiesList"></div>
    ` : ''}
  `);
  if (!url) return;
  document.getElementById('refreshCookies').onclick = () => renderCookies();
  document.getElementById('deleteAllCookies').onclick = () => {
    window.AndroidHost.deleteAllCookies();
    renderCookies();
  };
  loadCookies(url);
}

async function loadCookies(url) {
  const raw = window.AndroidHost.getCookiesForUrl(url);
  let cookies;
  try { cookies = JSON.parse(raw); } catch (e) { cookies = []; }
  const listEl = document.getElementById('cookiesList');
  if (!cookies.length) {
    listEl.innerHTML = '<div class="empty">Çerez yok</div>';
    return;
  }
  listEl.innerHTML = cookies.map(c => `
    <div class="list-item" style="display:flex;justify-content:space-between;align-items:center;">
      <div style="flex:1;min-width:0;">
        <div class="l-title">${escapeHtml(c.name)}</div>
        <div class="l-sub" style="font-family:monospace;font-size:11px;white-space:pre-wrap;">${escapeHtml(c.value)}</div>
      </div>
      <button class="icon-btn" onclick="window.AndroidHost.deleteCookieForUrl('${escapeHtml(url)}', '${escapeHtml(c.name)}'); renderCookies();" title="Sil">${icon('close')}</button>
    </div>
  `).join('');
}

// ---------- User-Agent ----------
function renderUserAgent() {
  const activeId = window.AndroidHost.getActiveTabId();
  const ua = window.AndroidHost.getCurrentUserAgent(activeId);
  const isDesktop = ua.includes('Windows NT') || ua.includes('Macintosh');
  setBody(`
    <div class="section-title">User-Agent</div>
    <div class="menu-item switch-row">${icon('rocket')}<div class="m-text"><div class="m-label">Masaüstü Modu</div><div class="m-sub">Web sitelerine masaüstü sürümünü göster</div></div><input type="checkbox" id="uaDesktop" ${isDesktop ? 'checked' : ''} /></div>
    <div class="pw-form" style="margin-top:16px;">
      <div class="l-sub">Geçerli User-Agent:</div>
      <div style="font-family:monospace;font-size:11px;background:var(--surface);border:1px solid var(--border);border-radius:10px;padding:12px;word-break:break-all;color:var(--text-dim);">${escapeHtml(ua)}</div>
    </div>
  `);
  document.getElementById('uaDesktop').addEventListener('change', (e) => {
    const activeId = window.AndroidHost.getActiveTabId();
    if (e.target.checked) {
      window.AndroidHost.setDesktopUserAgent(activeId);
    } else {
      window.AndroidHost.setMobileUserAgent(activeId);
    }
    setTimeout(() => renderUserAgent(), 500);
  });
}

// ---------- Son Kapatılan Sekmeler ----------
function renderRecentlyClosed() {
  const list = JSON.parse(window.AndroidHost.getRecentlyClosedJson());
  if (!list.length) {
    setBody('<div class="empty">Son kapatılan sekme yok</div>');
    return;
  }
  setBody(`
    <div class="section-title">Son Kapatılan Sekmeler</div>
    ${list.map((item, i) => `
      <div class="list-item" style="display:flex;justify-content:space-between;align-items:center;">
        <div style="flex:1;min-width:0;">
          <div class="l-title">${escapeHtml(item.title || 'Yeni sekme')}</div>
          <div class="l-sub">${escapeHtml(item.url || '')} ${item.private ? '• Gizli' : ''}</div>
        </div>
        <button class="icon-btn" onclick="window.AndroidHost.restoreRecentlyClosed(${i}); window.AndroidHost.closePanel();" title="Geri aç">${icon('rocket')}</button>
      </div>
    `).join('')}
  `);
}

// ---------- İndirmeler ----------
function renderDownloads() {
  setBody(`
    <div class="section-title">İndirme Geçmişi</div>
    <button class="secondary-btn" id="refreshDownloads" style="margin-bottom:16px;">↻ Yenile</button>
    <div id="downloadsList"></div>
  `);
  document.getElementById('refreshDownloads').onclick = () => renderDownloads();
  loadDownloads();
}

async function loadDownloads() {
  const listEl = document.getElementById('downloadsList');
  const raw = window.AndroidHost.getDownloadsJson();
  let items;
  try { items = JSON.parse(raw); } catch (e) { items = []; }
  if (!items.length) {
    listEl.innerHTML = '<div class="empty">Henüz indirme yok</div>';
    return;
  }
  const statusColor = (s) => {
    switch (s) {
      case 'Tamamlandı': return 'color:#23a55a;font-weight:700;';
      case 'İndiriliyor': return 'color:#5b5bf0;font-weight:700;';
      case 'Hata': return 'color:#ef4759;font-weight:700;';
      case 'Durduruldu': return 'color:#e08a2c;font-weight:700;';
      case 'Bekliyor': return 'color:#7a7d94;font-weight:700;';
      default: return 'color:#7a7d94;';
    }
  };
  listEl.innerHTML = items.map(d => `
    <div class="list-item" style="position:relative;">
      <div style="flex:1;min-width:0;">
        <div class="l-title">${escapeHtml(d.title)}</div>
        <div class="l-sub">${d.uri || ''} • ${d.mime || 'bilinmiyor'}</div>
        <div style="margin-top:8px;">
          <div style="font-size:11px;color:var(--text-dim);margin-bottom:4px;display:flex;justify-content:space-between;">
            <span>${d.status}</span>
            <span>${d.percent}%</span>
          </div>
          <div style="height:6px;background:var(--border);border-radius:3px;overflow:hidden;">
            <div style="height:100%;width:${d.percent}%;background:var(--accent);transition:width .3s ease;"></div>
          </div>
        </div>
      </div>
      <div style="display:flex;gap:6px;flex-shrink:0;">
        <button class="icon-btn" onclick="window.AndroidHost.openDownload(${d.id})" title="Aç">${icon('camera')}</button>
        ${(d.status === 'Hata' || d.status === 'Durduruldu') ? `<button class="icon-btn" onclick="window.AndroidHost.retryDownload(${d.id})" title="Tekrar dene">${icon('refresh')}</button>` : ''}
        <button class="icon-btn" onclick="window.AndroidHost.deleteDownload(${d.id})" title="Sil" style="color:var(--danger);">${icon('close')}</button>
      </div>
    </div>
  `).join('');
}

// ---------- NEXUS Bağlantısı ----------
function renderNexusBridge() {
  const s = JSON.parse(window.AndroidHost.getSettingsJson());
  setBody(`
    <div class="section-title">NEXUS Panel Adresi</div>
    <div class="pw-form">
      <input id="nxUrl" placeholder="http://127.0.0.1:8080" value="${escapeHtml(s.nexusUrl || 'http://127.0.0.1:8080')}" />
      <input id="nxToken" placeholder="Panel şifresi (X-Token)" type="password" value="${escapeHtml(s.nexusToken || '')}" />
      <div class="menu-item switch-row">${icon('rocket')}<div class="m-text"><div class="m-label">Otomatik senkron</div><div class="m-sub">Cihaz bilgisi gönder + bildirimleri çek</div></div><input type="checkbox" id="nxSync" ${s.nexusSync !== false ? 'checked' : ''} /></div>
      <button class="primary-btn" id="nxSave">Kaydet</button>
      <button class="secondary-btn" id="nxTest">Bağlantıyı Test Et</button>
    </div>
    <div id="nexusTestSonuc"></div>
    <div class="empty" style="margin:14px 0 0 0;">Bağlanınca bu uygulama NEXUS köprüsü olarak çalışır: NEXUS bildirimleri gerçek Android bildirimi olarak gösterilir, pil/cihaz bilgisi panele iletilir. Aynı telefonda panel varsayılan olarak <b>http://127.0.0.1:8080</b>; şifre <b>~/.nexus_web_sifre</b> dosyasındadır.</div>
  `);
  document.getElementById('nxSave').addEventListener('click', () => {
    window.AndroidHost.nexusSetConfig(
      document.getElementById('nxUrl').value.trim(),
      document.getElementById('nxToken').value.trim(),
      document.getElementById('nxSync').checked
    );
  });
  document.getElementById('nxTest').addEventListener('click', () => {
    document.getElementById('nexusTestSonuc').innerHTML = '<div class="empty">Test ediliyor…</div>';
    window.AndroidHost.nexusSetConfig(
      document.getElementById('nxUrl').value.trim(),
      document.getElementById('nxToken').value.trim(),
      document.getElementById('nxSync').checked
    );
    window.AndroidHost.nexusTest();
  });
}

// ---------- Header ----------
el.backBtn.addEventListener('click', goBackOne);
el.closeBtn.addEventListener('click', () => window.AndroidHost.closePanel());

render('settings');
