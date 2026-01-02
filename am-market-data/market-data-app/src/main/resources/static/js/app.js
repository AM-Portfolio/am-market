// --- State Management ---
let socket = null;
let isConnected = false;

// --- Initialization ---
setInterval(() => {
    document.getElementById('clock').innerText = new Date().toLocaleTimeString();
}, 1000);

const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
// Dynamically build WS URL but handle cases where app might be behind proxy if needed.
// For now, simpler is better.
const defaultWsUrl = `${protocol}//${window.location.host}/ws/market-data-stream`;
document.getElementById('wsUrl').value = defaultWsUrl;

// Auto-connect WS on load
connectWebSocket();

// --- Core Functions ---

function getSymbolsFromInput() {
    const raw = document.getElementById('symbols').value;
    const exchange = document.getElementById('exchange').value;
    const autoPrefix = document.getElementById('autoPrefix').checked;

    if (!raw.trim()) return [];

    return raw.split(',')
        .map(s => s.trim())
        .filter(s => s.length > 0)
        .map(s => {
            if (autoPrefix && exchange && !s.includes('|')) {
                return `${exchange}|${s.toUpperCase()}`;
            }
            return s.toUpperCase();
        });
}

async function startStream() {
    const provider = document.getElementById('provider').value;
    const symbolsList = getSymbolsFromInput();

    if (symbolsList.length === 0) {
        log("Please enter at least one symbol.", "error");
        return;
    }

    const payload = {
        instrumentKeys: symbolsList,
        mode: "FULL",
        provider: provider
    };

    log(`Starting stream for ${symbolsList.length} symbols: ${symbolsList.join(', ')}...`);

    try {
        const response = await fetch('/api/v1/market-data/stream/connect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            log("Stream Connect Request Sent: SC_OK", "success");
        } else {
            log(`Stream Connect Failed: ${await response.text()}`, "error");
        }
    } catch (e) {
        log(`Network Error: ${e.message}`, "error");
    }
}

async function stopStream() {
    const provider = document.getElementById('provider').value;
    try {
        await fetch(`/api/v1/market-data/stream/disconnect?provider=${provider}`, { method: 'POST' });
        log("Stream Stop Request Sent", "info");
    } catch (e) {
        log(`Error stopping stream: ${e.message}`, "error");
    }
}

async function getLoginUrl() {
    const provider = document.getElementById('provider').value;
    try {
        const res = await fetch(`/api/v1/market-data/auth/login-url?provider=${provider}`);
        const data = await res.json();
        const url = data.loginUrl || data.url || data.authUrl;

        const el = document.getElementById('loginUrlResult');
        if (url) {
            el.innerHTML = `<a href="${url}" target="_blank" style="color: var(--primary-color);">Click to Login</a>`;
            log("Generated login URL", "success");
        } else {
            el.innerHTML = `<span style="color:red">No URL found in response</span>`;
        }
    } catch (e) {
        log(`Auth Error: ${e.message}`, "error");
    }
}

// --- WebSocket Logic ---

function connectWebSocket() {
    if (socket) return;
    const url = document.getElementById('wsUrl').value;

    try {
        socket = new WebSocket(url);
        socket.onopen = () => updateStatus(true);
        socket.onclose = () => { updateStatus(false); socket = null; setTimeout(connectWebSocket, 5000); }; // Auto-reconnect
        socket.onerror = (e) => log("WebSocket Error", "error");
        socket.onmessage = (event) => handleMessage(event.data);
    } catch (e) {
        log(`WS Connection Failed: ${e.message}`, "error");
    }
}

function updateStatus(connected) {
    isConnected = connected;
    const badge = document.getElementById('connectionStatus');
    if (connected) {
        badge.innerText = "Connected";
        badge.className = "status-badge status-connected";
        log("WebSocket Connected", "success");
    } else {
        badge.innerText = "Disconnected";
        badge.className = "status-badge status-disconnected";
        log("WebSocket Disconnected", "error");
    }
}

function handleMessage(jsonStr) {
    try {
        const update = JSON.parse(jsonStr);
        if (update.quotes) {
            processQuotes(update.quotes);
        } else {
            // log("Received non-quote message: " + jsonStr, "info");
        }
    } catch (e) {
        log("Parse Error: " + e.message, "error");
    }
}

// --- UI Rendering ---

function processQuotes(quotes) {
    const tbody = document.querySelector('#dataTable tbody');
    const symbolList = Object.keys(quotes);

    // For each symbol in the update, PREPEND a row
    // We want to keep the newest data at the top

    for (const [key, data] of Object.entries(quotes)) {
        // If user wants filtered view, check filter here? 
        // We'll filter visually in filterTable(), but usually we just add all data 
        // and let the filter hide rows.

        const row = tbody.insertRow(0);
        row.className = "row-animate";

        // Determine symbol display (remove exchange if needed for cleaner look)
        // The key might be "NSE_EQ|INFY" or just "INFY" (based on our backend fix).
        const displaySymbol = key;

        // "data" is now QuoteUpdate { lastPrice, change, changePercent }
        const ltp = data.lastPrice || 0;
        const change = data.change || 0;
        const changePercent = data.changePercent || 0;

        // Format change for display
        const changeClass = change >= 0 ? 'text-success' : 'text-danger';
        const changeSign = change >= 0 ? '+' : '';
        const changeStr = `${changeSign}${change.toFixed(2)} (${changeSign}${changePercent.toFixed(2)}%)`;

        row.innerHTML = `
            <td class="mono-font" style="font-size: 0.8rem; color: var(--text-muted);">${new Date().toLocaleTimeString('en-US', { hour: 'numeric', minute: 'numeric', second: 'numeric', hour12: true })}</td>
            <td style="font-weight: 600;">${displaySymbol}</td>
            <td class="mono-font">${ltp.toFixed(2)}</td>
            <td class="mono-font ${changeClass}">${changeStr}</td>
        `;
    }

    // Prune old rows (limit to last 3 sets of updates)
    // If we receive N symbols, we keep 3*N rows.
    const MAX_ROWS = symbolList.length * 3;
    while (tbody.rows.length > MAX_ROWS) {
        tbody.deleteRow(tbody.rows.length - 1);
    }

    document.getElementById('rowCount').innerText = Math.min(tbody.rows.length, MAX_ROWS);

    // Re-apply filter if active
    filterTable();
}

function filterTable() {
    const input = document.getElementById('filterInput');
    const filter = input.value.toUpperCase();
    const table = document.getElementById('dataTable');
    const tr = table.getElementsByTagName('tr');

    for (let i = 1; i < tr.length; i++) { // Start from 1 to skip header
        const td = tr[i].getElementsByTagName('td')[1]; // Symbol column
        if (td) {
            const txtValue = td.textContent || td.innerText;
            if (txtValue.toUpperCase().indexOf(filter) > -1) {
                tr[i].style.display = "";
            } else {
                tr[i].style.display = "none";
            }
        }
    }
}

function log(msg, type = 'info') {
    const el = document.getElementById('debugLog');
    const time = new Date().toLocaleTimeString();
    let color = '#e2e8f0';
    if (type === 'error') color = '#ef4444';
    if (type === 'success') color = '#22c55e';

    const entry = document.createElement('div');
    entry.style.color = color;
    entry.style.borderBottom = '1px solid #334155';
    entry.style.padding = '4px 0';
    entry.innerText = `[${time}] ${msg}`;

    el.insertBefore(entry, el.firstChild);

    // limit logs
    if (el.children.length > 50) el.removeChild(el.lastChild);
}

// --- Instrument Search Logic ---

async function performSearch() {
    const query = document.getElementById('searchInput').value;
    if (!query) return;

    const provider = document.getElementById('provider').value;
    const resultsContainer = document.getElementById('searchResults');
    resultsContainer.innerHTML = '<div style="padding:10px; text-align:center;">Searching...</div>';

    const payload = {
        queries: [query],
        provider: provider
    };

    try {
        const response = await fetch('/api/instruments/search', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const results = await response.json();
            renderSearchResults(results);
            log(`Found ${results.length} instruments for query "${query}"`, "success");
        } else {
            resultsContainer.innerHTML = `<div style="padding:10px; color:red;">Search failed: ${response.statusText}</div>`;
            log("Search failed", "error");
        }
    } catch (e) {
        resultsContainer.innerHTML = `<div style="padding:10px; color:red;">Error: ${e.message}</div>`;
        log(`Search Error: ${e.message}`, "error");
    }
}

function renderSearchResults(results) {
    const container = document.getElementById('searchResults');
    container.innerHTML = '';

    if (results.length === 0) {
        container.innerHTML = '<div style="padding:10px; text-align:center;">No results found.</div>';
        return;
    }

    results.forEach(item => {
        // Adapt based on actual response structure of InstrumentSearchResult
        const symbol = item.tradingSymbol || item.symbol || "Unknown";
        const name = item.name || "";
        const exchange = item.exchange || "";
        const key = item.instrumentKey || "";

        const div = document.createElement('div');
        div.className = 'search-item';
        div.onclick = () => addSymbolToInput(key || symbol); // Prefer key if available for uniqueness

        div.innerHTML = `
            <div>
                <div class="search-item-title">${symbol} <span style="font-size:0.8em; color:#64748b;">(${exchange})</span></div>
                <div class="search-item-meta">${name}</div>
            </div>
            <div class="add-icon">+</div>
        `;
        container.appendChild(div);
    });
}

function addSymbolToInput(symbol) {
    const input = document.getElementById('symbols');
    let current = input.value.trim();
    if (current && !current.endsWith(',') && current.length > 0) current += ', ';
    input.value = current + symbol;

    // Visual feedback
    log(`Added ${symbol} to configuration`, "success");
}
