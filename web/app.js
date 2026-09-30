/**
 * =============================================================================
 * Retail Reorder Point Predictor - Frontend Application Logic
 * Pure Vanilla JavaScript (Zero Dependencies, 100% Offline)
 * Features:
 *   - SPA Navigation & Dark Mode
 *   - Live REST API Synchronization
 *   - Custom SVG Visualizations:
 *       1. Stock vs Forecast Demand Bar Chart
 *       2. Interactive AVL Self-Balancing Tree with Rotations
 *       3. Supplier Network Graph with Animated BFS/DFS/Dijkstra
 *       4. 30-Day Sales Line Chart with Moving-Average Projection
 * =============================================================================
 */

// Global Application State
const state = {
  inventory: [],
  filteredInventory: [],
  avlTree: null,
  graph: null,
  activeTab: 'dashboard',
  sortCol: 'stock',
  sortAsc: true,
  selectedForecastSku: null,
  activeGraphAlgo: 'bfs',
  currentRestockPath: null,
  isTraversing: false,
  theme: localStorage.getItem('apex_theme') || 'light'
};

// API Base URL
const API_BASE = '/api';

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  refreshAllData();
});

function initTheme() {
  document.documentElement.setAttribute('data-theme', state.theme);
  const btn = document.getElementById('themeToggleBtn');
  if (btn) btn.textContent = state.theme === 'dark' ? '☀️' : '🌙';
}

function toggleTheme() {
  state.theme = state.theme === 'light' ? 'dark' : 'light';
  localStorage.setItem('apex_theme', state.theme);
  initTheme();
  // Redraw charts if needed
  renderDashboardChart();
  if (state.avlTree) renderAvlTreeSvg(state.avlTree);
  if (state.graph) renderSupplierGraphSvg(state.graph, state.currentRestockPath);
  if (state.selectedForecastSku) renderForecastDetailView();
}

// --- Tab Switching ---
function switchTab(tabId) {
  state.activeTab = tabId;

  // Update navbar buttons
  document.querySelectorAll('.nav-tab-btn').forEach(btn => {
    btn.classList.remove('active');
  });
  const activeBtn = document.getElementById(`tab-btn-${tabId}`);
  if (activeBtn) activeBtn.classList.add('active');

  // Update tab sections
  document.querySelectorAll('.tab-content').forEach(section => {
    section.classList.remove('active');
  });
  const activeSection = document.getElementById(`tab-${tabId}`);
  if (activeSection) activeSection.classList.add('active');

  // Tab-specific refreshes
  if (tabId === 'tree') {
    loadAvlTree();
  } else if (tabId === 'graph') {
    loadSupplierGraph();
  } else if (tabId === 'forecast') {
    if (!state.selectedForecastSku && state.inventory.length > 0) {
      state.selectedForecastSku = state.inventory[0].id;
    }
    populateForecastSkuDropdown();
    renderForecastDetailView();
  } else if (tabId === 'dashboard') {
    renderDashboardChart();
  }
}

// --- REST API Data Fetching ---
async function refreshAllData() {
  try {
    await fetchInventory();
    await fetchStatus();
    renderDashboard();
    renderInventoryTable();
    populateForecastSkuDropdown();
  } catch (err) {
    showToast('Failed to load initial data: ' + err.message, 'error');
  }
}

async function fetchInventory() {
  const res = await fetch(`${API_BASE}/inventory`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  state.inventory = await res.json();
  state.filteredInventory = [...state.inventory];
}

async function fetchStatus() {
  try {
    const res = await fetch(`${API_BASE}/status`);
    if (res.ok) {
      const stats = await res.json();
      updateKpis(stats);
    }
  } catch (e) {
    console.warn('Status fetch error:', e);
  }
}

function updateKpis(stats) {
  const elTotal = document.getElementById('kpiTotalSkus');
  const elReorder = document.getElementById('kpiReorderCount');
  const elHealthy = document.getElementById('kpiHealthyCount');
  const elLowestVal = document.getElementById('kpiLowestStockVal');
  const elLowestName = document.getElementById('kpiLowestStockName');
  const elUrgentBadge = document.getElementById('urgentBadgeCount');

  if (elTotal) elTotal.textContent = stats.totalSKUs;
  if (elReorder) elReorder.textContent = stats.reorderTodayCount;
  if (elHealthy) elHealthy.textContent = stats.healthyStockCount;
  if (elLowestVal) elLowestVal.textContent = `${stats.lowestStockValue} units`;
  if (elLowestName) elLowestName.textContent = `${stats.lowestStockSku} (${stats.lowestStockSkuId})`;
  if (elUrgentBadge) elUrgentBadge.textContent = `${stats.reorderTodayCount} Items Critical`;
}

// --- Dashboard View ---
function renderDashboard() {
  renderDashboardChart();
  renderUrgentReorderTable();
}

function renderUrgentReorderTable() {
  const tbody = document.getElementById('urgentReorderTableBody');
  if (!tbody) return;

  const urgentItems = state.inventory.filter(s => s.reorderStatus === 'REORDER TODAY');
  if (urgentItems.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--success); padding: 1.5rem;">🎉 All items have healthy stock levels! No reorders needed today.</td></tr>`;
    return;
  }

  tbody.innerHTML = urgentItems.map(sku => {
    const deficit = Math.max(0, Math.round((sku.reorderThreshold - sku.currentStock) * 10) / 10);
    return `
      <tr>
        <td><strong>${sku.id}</strong></td>
        <td>${sku.name}</td>
        <td><span class="badge badge-category">${sku.category}</span></td>
        <td style="color: var(--danger); font-weight: 700;">${sku.currentStock} units</td>
        <td>${sku.forecastDemand} / day</td>
        <td>${sku.leadTimeDays} days</td>
        <td style="font-weight: 600;">${sku.reorderThreshold} units</td>
        <td><span class="badge badge-reorder">-${deficit} units</span></td>
        <td>
          <button class="btn btn-primary btn-sm" onclick="quickRestock('${sku.id}', 20)">⚡ Restock (+20)</button>
        </td>
      </tr>
    `;
  }).join('');
}

function renderDashboardChart() {
  const container = document.getElementById('dashboardChartContainer');
  if (!container || state.inventory.length === 0) return;

  const width = Math.max(900, state.inventory.length * 75);
  const height = 280;
  const margin = { top: 20, right: 30, bottom: 65, left: 50 };
  const chartW = width - margin.left - margin.right;
  const chartH = height - margin.top - margin.bottom;

  let maxVal = 0;
  state.inventory.forEach(s => {
    if (s.currentStock > maxVal) maxVal = s.currentStock;
    if (s.reorderThreshold > maxVal) maxVal = s.reorderThreshold;
  });
  maxVal = Math.ceil(maxVal * 1.15) || 50;

  const groupW = chartW / state.inventory.length;
  const barW = Math.max(12, groupW * 0.35);

  let gridSvg = '';
  for (let i = 0; i <= 4; i++) {
    const yVal = Math.round((maxVal / 4) * i);
    const yPos = margin.top + chartH - (yVal / maxVal) * chartH;
    gridSvg += `
      <line x1="${margin.left}" y1="${yPos}" x2="${margin.left + chartW}" y2="${yPos}" stroke="var(--border-color)" stroke-dasharray="3,3" stroke-width="1"/>
      <text x="${margin.left - 8}" y="${yPos + 4}" fill="var(--text-muted)" font-size="10" text-anchor="end">${yVal}</text>
    `;
  }

  let barsSvg = '';
  state.inventory.forEach((sku, idx) => {
    const groupX = margin.left + idx * groupW + (groupW - (barW * 2 + 4)) / 2;
    
    // Stock bar
    const stockBarH = (sku.currentStock / maxVal) * chartH;
    const stockBarY = margin.top + chartH - stockBarH;

    // Threshold bar
    const threshH = (sku.reorderThreshold / maxVal) * chartH;
    const threshY = margin.top + chartH - threshH;

    const shortName = sku.name.length > 11 ? sku.name.substring(0, 9) + '..' : sku.name;
    const isUrgent = sku.reorderStatus === 'REORDER TODAY';

    barsSvg += `
      <!-- Group: ${sku.id} -->
      <g class="chart-bar-group" data-sku="${sku.id}" style="cursor: pointer;" onclick="inspectSkuForecast('${sku.id}')">
        <!-- Stock Bar -->
        <rect x="${groupX}" y="${stockBarY}" width="${barW}" height="${stockBarH}" rx="3" fill="#1D5FD1">
          <title>${sku.name} (${sku.id})\nCurrent Stock: ${sku.currentStock} units</title>
        </rect>
        <text x="${groupX + barW/2}" y="${stockBarY - 4}" fill="#1D5FD1" font-size="10" font-weight="700" text-anchor="middle">${sku.currentStock}</text>

        <!-- Threshold Bar -->
        <rect x="${groupX + barW + 4}" y="${threshY}" width="${barW}" height="${threshH}" rx="3" fill="#F97316">
          <title>${sku.name} (${sku.id})\nReorder Threshold: ${sku.reorderThreshold} units\nForecast: ${sku.forecastDemand}/d x ${sku.leadTimeDays}d</title>
        </rect>
        <text x="${groupX + barW + 4 + barW/2}" y="${threshY - 4}" fill="#F97316" font-size="10" font-weight="700" text-anchor="middle">${Math.round(sku.reorderThreshold)}</text>

        <!-- X Axis Label -->
        <text x="${groupX + barW + 2}" y="${margin.top + chartH + 18}" fill="${isUrgent ? 'var(--accent)' : 'var(--text-primary)'}" font-size="11" font-weight="${isUrgent ? '700' : '500'}" text-anchor="middle">${shortName}</text>
        <text x="${groupX + barW + 2}" y="${margin.top + chartH + 32}" fill="var(--text-muted)" font-size="9" text-anchor="middle">${sku.id}</text>
      </g>
    `;
  });

  container.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" style="width: 100%; min-width: ${width}px; height: ${height}px;">
      ${gridSvg}
      ${barsSvg}
      <!-- Axes Lines -->
      <line x1="${margin.left}" y1="${margin.top}" x2="${margin.left}" y2="${margin.top + chartH}" stroke="var(--border-color)" stroke-width="1.5"/>
      <line x1="${margin.left}" y1="${margin.top + chartH}" x2="${margin.left + chartW}" y2="${margin.top + chartH}" stroke="var(--border-color)" stroke-width="1.5"/>
    </svg>
  `;
}

function inspectSkuForecast(skuId) {
  state.selectedForecastSku = skuId;
  switchTab('forecast');
}

// --- Trigger Python Forecast Execution ---
async function runPythonForecast() {
  const spinner = document.getElementById('forecastSpinner');
  const btn = document.getElementById('btnRunForecastDash');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await fetch(`${API_BASE}/forecast/run`, { method: 'POST' });
    const result = await res.json();

    if (result.success) {
      showToast('Python forecast engine executed successfully!', 'success');
      await fetchInventory();
      await fetchStatus();
      renderDashboard();
      renderInventoryTable();
      if (state.avlTree) loadAvlTree();
      if (state.activeTab === 'forecast') renderForecastDetailView();
    } else {
      showToast('Forecast Error: ' + (result.errorMessage || 'Unknown error'), 'error');
      openForecastLogModal(result.errorMessage || result.outputLog);
    }
  } catch (e) {
    showToast('Failed to invoke forecast: ' + e.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function openForecastLogModal(logText) {
  const content = document.getElementById('forecastLogContent');
  if (content) content.textContent = logText;
  openModal('forecastLogModal');
}

// --- Inventory Management Tab ---
function renderInventoryTable() {
  const tbody = document.getElementById('inventoryTableBody');
  if (!tbody) return;

  if (state.filteredInventory.length === 0) {
    tbody.innerHTML = `<tr><td colspan="10" style="text-align: center; padding: 2rem; color: var(--text-muted);">No matching inventory items found.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.filteredInventory.map(sku => {
    const isUrgent = sku.reorderStatus === 'REORDER TODAY';
    return `
      <tr>
        <td><strong>${sku.id}</strong></td>
        <td><strong>${sku.name}</strong></td>
        <td><span class="badge badge-category">${sku.category}</span></td>
        <td style="font-weight: 700; color: ${isUrgent ? 'var(--accent)' : 'var(--text-primary)'};">${sku.currentStock}</td>
        <td>${sku.leadTimeDays} days</td>
        <td>₹ ${sku.unitPrice.toFixed(2)}</td>
        <td>${sku.forecastDemand.toFixed(2)} / d</td>
        <td><strong>${sku.reorderThreshold.toFixed(1)}</strong></td>
        <td>
          <span class="badge ${isUrgent ? 'badge-reorder' : 'badge-ok'}">
            ${isUrgent ? '🚨 REORDER TODAY' : '✅ OK'}
          </span>
        </td>
        <td>
          <div style="display: flex; gap: 0.35rem;">
            <button class="btn btn-secondary btn-sm" title="Quick Restock +10" onclick="quickRestock('${sku.id}', 10)">+10</button>
            <button class="btn btn-secondary btn-sm" title="Edit SKU" onclick="openEditSkuModal('${sku.id}')">✏️</button>
            <button class="btn btn-danger btn-sm" title="Delete SKU" onclick="confirmDeleteSku('${sku.id}', '${sku.name}')">🗑️</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function filterInventoryTable() {
  const search = (document.getElementById('inventorySearchInput')?.value || '').toLowerCase().trim();
  const cat = document.getElementById('categoryFilterSelect')?.value || 'ALL';
  const status = document.getElementById('statusFilterSelect')?.value || 'ALL';

  state.filteredInventory = state.inventory.filter(sku => {
    const matchSearch = !search ||
      sku.id.toLowerCase().includes(search) ||
      sku.name.toLowerCase().includes(search) ||
      sku.category.toLowerCase().includes(search);

    const matchCat = (cat === 'ALL') || (sku.category.toLowerCase() === cat.toLowerCase());
    const matchStatus = (status === 'ALL') || (sku.reorderStatus === status);

    return matchSearch && matchCat && matchStatus;
  });

  sortInventoryList();
  renderInventoryTable();
}

function sortInventoryTable(column) {
  if (state.sortCol === column) {
    state.sortAsc = !state.sortAsc;
  } else {
    state.sortCol = column;
    state.sortAsc = true;
  }
  sortInventoryList();
  renderInventoryTable();
}

function sortInventoryList() {
  state.filteredInventory.sort((a, b) => {
    let valA = a[state.sortCol];
    let valB = b[state.sortCol];

    if (typeof valA === 'string') {
      valA = valA.toLowerCase();
      valB = valB.toLowerCase();
    }

    if (valA < valB) return state.sortAsc ? -1 : 1;
    if (valA > valB) return state.sortAsc ? 1 : -1;
    return 0;
  });
}

async function quickRestock(skuId, qty) {
  const sku = state.inventory.find(s => s.id === skuId);
  if (!sku) return;

  const newStock = sku.currentStock + qty;
  try {
    const res = await fetch(`${API_BASE}/inventory/${skuId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ stock: newStock.toString() })
    });
    if (res.ok) {
      showToast(`Restocked ${sku.name} (+${qty} units)`, 'success');
      await refreshAllData();
    } else {
      showToast('Failed to update stock', 'error');
    }
  } catch (e) {
    showToast('Error: ' + e.message, 'error');
  }
}

// --- Add / Edit / Delete SKU Modals ---
function openAddSkuModal() {
  // Suggest next SKU ID
  const maxNum = state.inventory.reduce((max, s) => {
    const match = s.id.match(/SKU-(\d+)/);
    return match ? Math.max(max, parseInt(match[1])) : max;
  }, 100);
  document.getElementById('addSkuId').value = `SKU-${maxNum + 1}`;
  document.getElementById('addSkuForm').reset();
  document.getElementById('addSkuId').value = `SKU-${maxNum + 1}`;
  openModal('addSkuModal');
}

async function submitAddSku(e) {
  e.preventDefault();
  const id = document.getElementById('addSkuId').value.trim().toUpperCase();
  const name = document.getElementById('addSkuName').value.trim();
  const category = document.getElementById('addSkuCategory').value;
  const stock = document.getElementById('addSkuStock').value;
  const leadTimeDays = document.getElementById('addSkuLeadTime').value;
  const unitPrice = document.getElementById('addSkuPrice').value;
  const reorderLevel = document.getElementById('addSkuReorderLevel').value;

  try {
    const res = await fetch(`${API_BASE}/inventory`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, name, category, stock, leadTimeDays, unitPrice, reorderLevel })
    });

    if (res.ok) {
      closeModal('addSkuModal');
      showToast(`SKU ${id} added successfully!`, 'success');
      await refreshAllData();
    } else {
      const err = await res.json();
      showToast(err.error || 'Failed to add SKU', 'error');
    }
  } catch (err) {
    showToast('Error: ' + err.message, 'error');
  }
}

function openEditSkuModal(skuId) {
  const sku = state.inventory.find(s => s.id === skuId);
  if (!sku) return;

  document.getElementById('editSkuId').value = sku.id;
  document.getElementById('editSkuIdDisplay').value = sku.id;
  document.getElementById('editSkuNameDisplay').value = sku.name;
  document.getElementById('editSkuStock').value = sku.currentStock;
  document.getElementById('editSkuLeadTime').value = sku.leadTimeDays;
  document.getElementById('editSkuPrice').value = sku.unitPrice;

  openModal('editSkuModal');
}

async function submitEditSku(e) {
  e.preventDefault();
  const id = document.getElementById('editSkuId').value;
  const stock = document.getElementById('editSkuStock').value;
  const leadTimeDays = document.getElementById('editSkuLeadTime').value;
  const unitPrice = document.getElementById('editSkuPrice').value;

  try {
    const res = await fetch(`${API_BASE}/inventory/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ stock, leadTimeDays, unitPrice })
    });

    if (res.ok) {
      closeModal('editSkuModal');
      showToast(`Updated stock for ${id}`, 'success');
      await refreshAllData();
    } else {
      showToast('Update failed', 'error');
    }
  } catch (err) {
    showToast('Error: ' + err.message, 'error');
  }
}

async function confirmDeleteSku(id, name) {
  if (confirm(`Are you sure you want to delete "${name}" (${id}) from inventory and AVL tree?`)) {
    try {
      const res = await fetch(`${API_BASE}/inventory/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showToast(`SKU ${id} deleted successfully.`, 'info');
        await refreshAllData();
      } else {
        showToast('Failed to delete SKU', 'error');
      }
    } catch (e) {
      showToast('Error: ' + e.message, 'error');
    }
  }
}

function exportInventoryCsv() {
  if (state.inventory.length === 0) return;
  let csv = 'SKU_ID,Product_Name,Category,Current_Stock,Lead_Time_Days,Unit_Price,Forecast_Daily_Demand,Reorder_Threshold,Status\n';
  state.inventory.forEach(s => {
    csv += `"${s.id}","${s.name}","${s.category}",${s.currentStock},${s.leadTimeDays},${s.unitPrice},${s.forecastDemand},${s.reorderThreshold},"${s.reorderStatus}"\n`;
  });

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `retail_inventory_${new Date().toISOString().slice(0,10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// --- TAB 3: AVL TREE VISUALIZER ---
async function loadAvlTree() {
  try {
    const res = await fetch(`${API_BASE}/tree`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.avlTree = await res.json();
    renderAvlTreeSvg(state.avlTree);
    updateTreeStats(state.avlTree);
  } catch (e) {
    console.error('AVL Tree load error:', e);
  }
}

function updateTreeStats(tree) {
  const elTotal = document.getElementById('treeTotalNodes');
  const elHeight = document.getElementById('treeHeight');
  const elMin = document.getElementById('treeMinStock');
  const elLast = document.getElementById('treeLastRotation');

  if (elTotal) elTotal.textContent = `${tree.size} SKUs`;
  if (elHeight) elHeight.textContent = tree.height;
  if (elMin) elMin.textContent = `${tree.minStock} units`;
  if (elLast) elLast.textContent = tree.lastRotation || 'Balanced';

  // Render inorder list
  const inorderContainer = document.getElementById('treeInorderList');
  if (inorderContainer) {
    const sorted = [];
    traverseInorder(tree.root, sorted);
    inorderContainer.innerHTML = sorted.map((item, idx) => `
      <span style="background: var(--bg-card-secondary); border: 1px solid var(--border-color); padding: 0.2rem 0.5rem; border-radius: 4px; ${idx === 0 ? 'color: var(--accent); font-weight: bold;' : ''}">
        ${item.stock} (${item.skus.map(s => s.id).join(', ')})
      </span>
    `).join(' &rarr; ');
  }
}

function traverseInorder(node, list) {
  if (!node) return;
  traverseInorder(node.left, list);
  list.push({ stock: node.stock, skus: node.skus });
  traverseInorder(node.right, list);
}

function renderAvlTreeSvg(tree) {
  const svg = document.getElementById('avlTreeSvg');
  if (!svg) return;

  if (!tree.root) {
    svg.innerHTML = `<text x="500" y="250" fill="var(--text-muted)" font-size="18" text-anchor="middle">AVL Tree is empty. Add inventory SKUs to visualize tree.</text>`;
    return;
  }

  const width = 1000;
  const height = Math.max(480, (tree.height + 1) * 95);
  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);

  let linksSvg = '';
  let nodesSvg = '';

  function layoutTree(node, depth, leftBound, rightBound) {
    if (!node) return null;

    const x = (leftBound + rightBound) / 2;
    const y = 60 + depth * 85;

    let leftPos = null;
    let rightPos = null;

    if (node.left) {
      leftPos = layoutTree(node.left, depth + 1, leftBound, x);
      linksSvg += `<line x1="${x}" y1="${y}" x2="${leftPos.x}" y2="${leftPos.y}" stroke="var(--border-color)" stroke-width="2.5"/>`;
    }
    if (node.right) {
      rightPos = layoutTree(node.right, depth + 1, x, rightBound);
      linksSvg += `<line x1="${x}" y1="${y}" x2="${rightPos.x}" y2="${rightPos.y}" stroke="var(--border-color)" stroke-width="2.5"/>`;
    }

    const isMin = node.stock === tree.minStock;
    const skuNames = node.skus.map(s => `${s.id}: ${s.name}`).join('\n');
    const bf = node.balanceFactor;
    const bfColor = bf === 0 ? 'var(--text-muted)' : (Math.abs(bf) === 1 ? 'var(--info)' : 'var(--danger)');

    nodesSvg += `
      <g class="avl-node-group" style="cursor: pointer;" onclick="showNodeDetails(${node.stock}, '${encodeURIComponent(JSON.stringify(node.skus))}')">
        <!-- Node Circle -->
        <circle cx="${x}" cy="${y}" r="26" fill="${isMin ? '#FFEDD5' : 'var(--bg-card)'}" stroke="${isMin ? '#F97316' : '#1D5FD1'}" stroke-width="${isMin ? '3.5' : '2.5'}">
          <title>Stock: ${node.stock}\nHeight: ${node.height}\nBalance Factor: ${bf}\n\nSKUs:\n${skuNames}</title>
        </circle>
        <!-- Stock Label -->
        <text x="${x}" y="${y + 5}" fill="${isMin ? '#C2410C' : 'var(--text-primary)'}" font-size="14" font-weight="800" text-anchor="middle">${node.stock}</text>
        <!-- Balance & Height Badge -->
        <rect x="${x - 24}" y="${y + 28}" width="48" height="16" rx="4" fill="var(--bg-card-secondary)" stroke="var(--border-color)" stroke-width="1"/>
        <text x="${x}" y="${y + 40}" fill="${bfColor}" font-size="9" font-weight="700" text-anchor="middle">h:${node.height} bf:${bf}</text>
        ${node.skus.length > 1 ? `<circle cx="${x + 20}" cy="${y - 18}" r="9" fill="#1D5FD1"/><text x="${x + 20}" y="${y - 14}" fill="#FFFFFF" font-size="9" font-weight="bold" text-anchor="middle">+${node.skus.length}</text>` : ''}
      </g>
    `;

    return { x, y };
  }

  layoutTree(tree.root, 0, 40, width - 40);
  svg.innerHTML = `<g>${linksSvg}${nodesSvg}</g>`;
}

function showNodeDetails(stock, encodedSkus) {
  try {
    const skus = JSON.parse(decodeURIComponent(encodedSkus));
    const itemsList = skus.map(s => `• ${s.id}: ${s.name} (${s.reorderStatus})`).join('\n');
    showToast(`Stock Level ${stock} (${skus.length} SKU${skus.length > 1 ? 's' : ''}):\n${itemsList}`, 'info');
  } catch (e) {
    showToast(`Stock Level: ${stock}`, 'info');
  }
}

function highlightLowestStockNode() {
  if (state.avlTree && state.avlTree.minStock !== -1) {
    showToast(`Lowest Stock in AVL Tree: ${state.avlTree.minStock} units (findMin O(log n))`, 'info');
    renderAvlTreeSvg(state.avlTree);
  }
}

function searchAvlTree() {
  const input = document.getElementById('treeSearchInput');
  const val = parseInt(input?.value);
  if (isNaN(val)) return;

  function findRec(node, target) {
    if (!node) return null;
    if (node.stock === target) return node;
    if (target < node.stock) return findRec(node.left, target);
    return findRec(node.right, target);
  }

  const found = findRec(state.avlTree?.root, val);
  if (found) {
    const names = found.skus.map(s => `${s.id} (${s.name})`).join(', ');
    showToast(`[FOUND] Stock = ${val} contains: ${names}`, 'success');
  } else {
    showToast(`[NOT FOUND] No item in AVL Tree has stock = ${val}`, 'error');
  }
}

async function deleteNodeFromTree() {
  const input = document.getElementById('treeDeleteInput');
  const val = parseInt(input?.value);
  if (isNaN(val)) return;

  // Find a SKU with this stock
  const sku = state.inventory.find(s => s.currentStock === val);
  if (sku) {
    await confirmDeleteSku(sku.id, sku.name);
  } else {
    showToast(`No SKU found with current stock = ${val}`, 'error');
  }
}

// --- TAB 4: SUPPLIER GRAPH VISUALIZER ---
async function loadSupplierGraph() {
  try {
    const res = await fetch(`${API_BASE}/graph`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.graph = await res.json();
    await selectGraphAlgo(state.activeGraphAlgo);
  } catch (e) {
    console.error('Graph load error:', e);
  }
}

async function selectGraphAlgo(algo) {
  state.activeGraphAlgo = algo;

  // Update UI buttons
  ['bfs', 'dijkstra', 'dfs'].forEach(a => {
    const btn = document.getElementById(`btnAlgo${a.charAt(0).toUpperCase() + a.slice(1)}`);
    if (btn) {
      if (a === algo) {
        btn.className = 'btn btn-primary btn-sm';
      } else {
        btn.className = 'btn btn-secondary btn-sm';
      }
    }
  });

  try {
    const res = await fetch(`${API_BASE}/path?algo=${algo}&start=WH&end=STORE`);
    if (res.ok) {
      state.currentRestockPath = await res.json();
      updateGraphStats(state.currentRestockPath);
      renderSupplierGraphSvg(state.graph, state.currentRestockPath);
    }
  } catch (e) {
    console.error('Path error:', e);
  }
}

function updateGraphStats(pathData) {
  const elAlgo = document.getElementById('graphActiveAlgo');
  const elHops = document.getElementById('graphTotalHops');
  const elDist = document.getElementById('graphTotalDist');
  const elStr = document.getElementById('graphPathStr');
  const elSteps = document.getElementById('graphStepLog');

  if (elAlgo) elAlgo.textContent = pathData.algorithm;
  if (elHops) elHops.textContent = `${pathData.totalHops} transit hops`;
  if (elDist) elDist.textContent = `${pathData.totalDistanceKm} km`;
  if (elStr) elStr.textContent = pathData.pathNodes.join(' ➔ ');

  if (elSteps && pathData.traversalOrder) {
    elSteps.innerHTML = pathData.traversalOrder.map((nodeId, idx) => `
      <span style="background: var(--bg-card-secondary); border: 1px solid var(--border-color); padding: 0.2rem 0.55rem; border-radius: 4px;">
        ${idx + 1}. <strong>${nodeId}</strong>
      </span>
    `).join(' ');
  }
}

function renderSupplierGraphSvg(graph, activePath, animatedVisitedSet = null, animatedPathNodes = null) {
  const svg = document.getElementById('supplierGraphSvg');
  if (!svg || !graph) return;

  const width = 800;
  const height = 450;
  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);

  const nodeMap = {};
  graph.nodes.forEach(n => nodeMap[n.id] = n);

  const pathEdges = new Set();
  if (activePath && activePath.pathNodes) {
    for (let i = 0; i < activePath.pathNodes.length - 1; i++) {
      pathEdges.add(`${activePath.pathNodes[i]}->${activePath.pathNodes[i+1]}`);
      pathEdges.add(`${activePath.pathNodes[i+1]}->${activePath.pathNodes[i]}`);
    }
  }

  // Draw Edges
  let edgesSvg = '';
  graph.edges.forEach(edge => {
    const fromNode = nodeMap[edge.from];
    const toNode = nodeMap[edge.to];
    if (!fromNode || !toNode) return;

    const isPathEdge = pathEdges.has(`${edge.from}->${edge.to}`);
    const strokeColor = isPathEdge ? '#F97316' : 'var(--border-color)';
    const strokeW = isPathEdge ? 4 : 2;

    const midX = (fromNode.x + toNode.x) / 2;
    const midY = (fromNode.y + toNode.y) / 2;

    edgesSvg += `
      <line x1="${fromNode.x}" y1="${fromNode.y}" x2="${toNode.x}" y2="${toNode.y}" stroke="${strokeColor}" stroke-width="${strokeW}" stroke-dasharray="${isPathEdge ? 'none' : 'none'}"/>
      <!-- Edge Distance Badge -->
      <rect x="${midX - 18}" y="${midY - 10}" width="36" height="20" rx="4" fill="var(--bg-card)" stroke="var(--border-color)" stroke-width="1"/>
      <text x="${midX}" y="${midY + 4}" fill="${isPathEdge ? '#C2410C' : 'var(--text-muted)'}" font-size="10" font-weight="700" text-anchor="middle">${edge.distance}k</text>
    `;
  });

  // Draw Nodes
  let nodesSvg = '';
  graph.nodes.forEach(node => {
    const isStart = node.id === 'WH';
    const isEnd = node.id === 'STORE';
    const isInPath = activePath && activePath.pathNodes && activePath.pathNodes.includes(node.id);
    const isVisitedInAnim = animatedVisitedSet && animatedVisitedSet.has(node.id);

    let fillColor = 'var(--bg-card)';
    let strokeColor = '#1D5FD1';

    if (isStart) {
      fillColor = '#0B2A5B';
      strokeColor = '#0B2A5B';
    } else if (isEnd) {
      fillColor = '#10B981';
      strokeColor = '#047857';
    } else if (isInPath) {
      fillColor = '#FFEDD5';
      strokeColor = '#F97316';
    }

    if (isVisitedInAnim) {
      strokeColor = '#F97316';
    }

    const textColor = (isStart || isEnd) ? '#FFFFFF' : 'var(--text-primary)';

    nodesSvg += `
      <g class="graph-node-group" style="cursor: pointer;" onclick="showToast('${node.name} (${node.type})', 'info')">
        <circle cx="${node.x}" cy="${node.y}" r="24" fill="${fillColor}" stroke="${strokeColor}" stroke-width="${isInPath || isVisitedInAnim ? 4 : 2.5}">
          <title>${node.name} (${node.id})\nType: ${node.type}</title>
        </circle>
        <text x="${node.x}" y="${node.y + 5}" fill="${textColor}" font-size="12" font-weight="800" text-anchor="middle">${node.id}</text>
        <text x="${node.x}" y="${node.y + 36}" fill="var(--text-secondary)" font-size="10" font-weight="600" text-anchor="middle">${node.name.length > 14 ? node.name.substring(0, 12) + '..' : node.name}</text>
      </g>
    `;
  });

  svg.innerHTML = `<g>${edgesSvg}${nodesSvg}</g>`;
}

function animateGraphTraversal() {
  if (!state.currentRestockPath || state.isTraversing) return;
  state.isTraversing = true;

  const traversal = state.currentRestockPath.traversalOrder || [];
  const visitedSet = new Set();
  let step = 0;

  const interval = setInterval(() => {
    if (step < traversal.length) {
      visitedSet.add(traversal[step]);
      renderSupplierGraphSvg(state.graph, null, visitedSet, null);
      step++;
    } else {
      clearInterval(interval);
      // Reveal final path
      setTimeout(() => {
        renderSupplierGraphSvg(state.graph, state.currentRestockPath);
        state.isTraversing = false;
        showToast(`Traversal complete! Optimal route found via ${state.currentRestockPath.totalHops} hops.`, 'success');
      }, 400);
    }
  }, 450);
}

// --- TAB 5: DEMAND FORECAST VIEW ---
function populateForecastSkuDropdown() {
  const select = document.getElementById('forecastSkuSelect');
  if (!select) return;

  select.innerHTML = state.inventory.map(sku => `
    <option value="${sku.id}" ${sku.id === state.selectedForecastSku ? 'selected' : ''}>
      ${sku.id} - ${sku.name} (${sku.category})
    </option>
  `).join('');
}

async function renderForecastDetailView() {
  const select = document.getElementById('forecastSkuSelect');
  const skuId = select ? select.value : state.selectedForecastSku;
  if (!skuId) return;

  state.selectedForecastSku = skuId;

  try {
    const res = await fetch(`${API_BASE}/sales/${skuId}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    const sku = data.sku || state.inventory.find(s => s.id === skuId);
    const forecast = data.forecast;
    const sales = data.sales || [];

    // Update Status Badge
    const badge = document.getElementById('forecastSelectedStatusBadge');
    if (badge && sku) {
      const isUrgent = sku.reorderStatus === 'REORDER TODAY';
      badge.className = `badge ${isUrgent ? 'badge-reorder' : 'badge-ok'}`;
      badge.textContent = isUrgent ? '🚨 REORDER TODAY' : '✅ OK (HEALTHY)';
    }

    // Update Chart Title
    const title = document.getElementById('forecastChartTitle');
    if (title && sku) {
      title.textContent = `📈 30-Day Sales Trend for ${sku.name} (${sku.id})`;
    }

    // Render Line Chart
    renderSalesLineChart(sales, forecast ? forecast.forecastDemand : 0);

    // Update Formula Card
    const formulaMath = document.getElementById('formulaMathDisplay');
    const formulaSub = document.getElementById('formulaSubstitutedDisplay');
    if (forecast) {
      if (formulaMath) formulaMath.textContent = `Forecast Demand = (Sales[T-2] + Sales[T-1] + Sales[T]) / 3`;
      if (formulaSub) formulaSub.textContent = forecast.formula;
    }

    // Update Reorder Rule Card
    const ruleDisp = document.getElementById('reorderRuleDisplay');
    const decDisp = document.getElementById('reorderDecisionDisplay');
    if (sku && forecast) {
      const threshold = (forecast.forecastDemand * sku.leadTimeDays).toFixed(2);
      if (ruleDisp) {
        ruleDisp.innerHTML = `
          Reorder Threshold = Forecast Demand × Lead Time<br>
          Threshold = ${forecast.forecastDemand.toFixed(2)} units/day × ${sku.leadTimeDays} days = <strong>${threshold} units</strong><br>
          Current Warehouse Stock = <strong>${sku.currentStock} units</strong>
        `;
      }
      if (decDisp) {
        const isUrgent = sku.currentStock < parseFloat(threshold);
        decDisp.innerHTML = `Decision: <span class="badge ${isUrgent ? 'badge-reorder' : 'badge-ok'}">${isUrgent ? `🚨 REORDER TODAY (Deficit: ${(threshold - sku.currentStock).toFixed(1)} units)` : '✅ OK (Adequate Stock Available)'}</span>`;
      }
    }

  } catch (e) {
    console.error('Forecast detail load error:', e);
  }
}

function renderSalesLineChart(salesArray, forecastDemand) {
  const container = document.getElementById('salesLineChartContainer');
  if (!container || !salesArray || salesArray.length === 0) return;

  const width = 900;
  const height = 280;
  const margin = { top: 25, right: 50, bottom: 40, left: 45 };
  const chartW = width - margin.left - margin.right;
  const chartH = height - margin.top - margin.bottom;

  let maxVal = Math.max(...salesArray, forecastDemand);
  maxVal = Math.ceil(maxVal * 1.2) || 40;

  // Grid Lines
  let gridSvg = '';
  for (let i = 0; i <= 4; i++) {
    const yVal = Math.round((maxVal / 4) * i);
    const yPos = margin.top + chartH - (yVal / maxVal) * chartH;
    gridSvg += `
      <line x1="${margin.left}" y1="${yPos}" x2="${margin.left + chartW}" y2="${yPos}" stroke="var(--border-color)" stroke-dasharray="3,3" stroke-width="1"/>
      <text x="${margin.left - 8}" y="${yPos + 4}" fill="var(--text-muted)" font-size="10" text-anchor="end">${yVal}</text>
    `;
  }

  // Points & Path calculation
  const totalDays = salesArray.length;
  const stepX = chartW / (totalDays + 1);

  const points = salesArray.map((val, idx) => {
    const x = margin.left + (idx + 1) * stepX;
    const y = margin.top + chartH - (val / maxVal) * chartH;
    return { x, y, val, day: idx + 1 };
  });

  const pathD = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');

  // Gradient area
  const areaD = `${pathD} L ${points[points.length - 1].x} ${margin.top + chartH} L ${points[0].x} ${margin.top + chartH} Z`;

  // Projection Point (Day 31)
  const projX = margin.left + (totalDays + 1) * stepX;
  const projY = margin.top + chartH - (forecastDemand / maxVal) * chartH;
  const lastP = points[points.length - 1];

  const projLineD = `M ${lastP.x} ${lastP.y} L ${projX} ${projY}`;

  let pointsSvg = points.map(p => `
    <circle cx="${p.x}" cy="${p.y}" r="4" fill="#1D5FD1" stroke="#FFFFFF" stroke-width="2">
      <title>Day ${p.day}: ${p.val} units sold</title>
    </circle>
  `).join('');

  container.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" style="width: 100%; height: ${height}px;">
      <defs>
        <linearGradient id="chartAreaGrad" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="#1D5FD1" stop-opacity="0.25"/>
          <stop offset="100%" stop-color="#1D5FD1" stop-opacity="0.0"/>
        </linearGradient>
      </defs>
      ${gridSvg}
      <path d="${areaD}" fill="url(#chartAreaGrad)"/>
      <path d="${pathD}" fill="none" stroke="#1D5FD1" stroke-width="3" stroke-linecap="round"/>
      <path d="${projLineD}" fill="none" stroke="#F97316" stroke-width="2.5" stroke-dasharray="4,4"/>
      
      <!-- Projection Point -->
      <circle cx="${projX}" cy="${projY}" r="6" fill="#F97316" stroke="#FFFFFF" stroke-width="2">
        <title>Predicted Day 31 Demand: ${forecastDemand.toFixed(2)} units</title>
      </circle>
      <text x="${projX}" y="${projY - 10}" fill="#F97316" font-size="11" font-weight="800" text-anchor="middle">${forecastDemand.toFixed(1)}</text>
      <text x="${projX}" y="${margin.top + chartH + 18}" fill="#F97316" font-size="10" font-weight="700" text-anchor="middle">Day 31 (MA)</text>

      ${pointsSvg}

      <!-- Day markers along X -->
      <text x="${points[0].x}" y="${margin.top + chartH + 18}" fill="var(--text-muted)" font-size="10" text-anchor="middle">Day 1</text>
      <text x="${points[14].x}" y="${margin.top + chartH + 18}" fill="var(--text-muted)" font-size="10" text-anchor="middle">Day 15</text>
      <text x="${lastP.x}" y="${margin.top + chartH + 18}" fill="var(--text-muted)" font-size="10" text-anchor="middle">Day 30</text>
    </svg>
  `;
}

// --- Modals & Toast Utility ---
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('active');
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.remove('active');
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span style="font-size: 1.25rem;">${type === 'success' ? '✅' : (type === 'error' ? '❌' : 'ℹ️')}</span>
    <span style="flex: 1; white-space: pre-line;">${message}</span>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}
