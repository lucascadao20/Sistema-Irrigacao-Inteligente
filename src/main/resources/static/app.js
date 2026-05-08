const POLLING_MS = 3000;
const GAUGE_CIRCUMFERENCE = 534; // 2 * π * 85

let chart = null;
let culturasInfo = {};
let alertasIds = new Set();

// =============== Relógio ===============
function tickClock() {
    const now = new Date();
    document.getElementById('clock').textContent = now.toLocaleTimeString('pt-BR', { hour12: false });
}
setInterval(tickClock, 1000);
tickClock();

// =============== Conexão ===============
function setConnState(online) {
    const pill = document.getElementById('conn-pill');
    const text = document.getElementById('conn-text');
    pill.classList.toggle('online', online);
    pill.classList.toggle('offline', !online);
    text.textContent = online ? 'Conectado' : 'Sem conexão';
}

// =============== Culturas ===============
async function carregarCulturas() {
    const res = await fetch('/api/culturas');
    const lista = await res.json();
    const select = document.getElementById('cultura');
    select.innerHTML = '';
    lista.forEach(c => {
        culturasInfo[c.id] = c;
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.nome;
        select.appendChild(opt);
    });
    select.addEventListener('change', async (e) => {
        await fetch('/api/cultura', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ cultura: e.target.value })
        });
        atualizar();
    });
}

function atualizarInfoCultura(culturaAtiva) {
    const info = culturasInfo[culturaAtiva];
    if (!info) return;
    document.getElementById('cultura').value = culturaAtiva;
    document.getElementById('cultura-min').textContent = `${info.umidadeMinima}%`;
    document.getElementById('cultura-ideal').textContent = `${info.umidadeIdeal}%`;
    document.getElementById('cultura-kc').textContent = info.coeficienteCultura.toFixed(2);
    document.getElementById('cultura-nec').textContent = `${info.necessidadeHidrica} mm`;
}

// =============== Gauge ===============
function atualizarGauge(umidade, cultura) {
    const fill = document.getElementById('gauge-fill');
    const pct = Math.min(100, Math.max(0, umidade));
    const offset = GAUGE_CIRCUMFERENCE * (1 - pct / 100);
    fill.style.strokeDashoffset = offset;

    let cor = '#4caf50';
    if (cultura) {
        if (umidade < cultura.umidadeMinima * 0.7) cor = '#d32f2f';
        else if (umidade < cultura.umidadeMinima) cor = '#f57c00';
        else if (umidade < cultura.umidadeIdeal) cor = '#fbc02d';
        else cor = '#43a047';
    }
    fill.style.stroke = cor;
    document.getElementById('umidade-solo').textContent = umidade.toFixed(1);
}

// =============== Status ===============
function atualizarStatus(irrigacao, estrategia) {
    const badge = document.getElementById('status-badge');
    const icon = document.getElementById('status-icon');
    const text = document.getElementById('status-text');
    const motivo = document.getElementById('status-motivo');

    if (!irrigacao) {
        badge.className = 'status-badge-lg';
        icon.textContent = '⏳';
        text.textContent = 'Aguardando primeiro ciclo';
        motivo.textContent = 'Os dados aparecerão em alguns segundos.';
        return;
    }

    badge.className = `status-badge-lg ${irrigacao.status}`;
    const icons = { ATIVADA: '💦', SUSPENSA: '⏸️', AGUARDANDO: '✓' };
    icon.textContent = icons[irrigacao.status] || '•';
    text.textContent = irrigacao.statusDescricao;
    motivo.textContent = irrigacao.motivo;
    document.getElementById('estrategia').textContent = estrategia || '—';
}

// =============== Clima ===============
function emojiClima(descricao, previsaoChuva) {
    const d = (descricao || '').toLowerCase();
    if (previsaoChuva || d.includes('chuva') || d.includes('garoa')) return '🌧️';
    if (d.includes('nuvem') || d.includes('nublado')) return '☁️';
    if (d.includes('limpo') || d.includes('sol')) return '☀️';
    if (d.includes('parcial')) return '⛅';
    if (d.includes('neblina') || d.includes('nevoeiro')) return '🌫️';
    if (d.includes('trovoada') || d.includes('tempest')) return '⛈️';
    return '🌤️';
}

function atualizarClima(clima) {
    if (!clima) return;
    document.getElementById('clima-cidade').textContent = clima.cidade;
    document.getElementById('clima-temp').textContent = clima.temperatura.toFixed(0);
    document.getElementById('clima-desc').textContent = clima.descricao;
    document.getElementById('clima-emoji').textContent = emojiClima(clima.descricao, clima.previsaoChuva);
    document.getElementById('clima-vento').textContent = `${clima.velocidadeVento.toFixed(1)} m/s`;
    document.getElementById('clima-umar').textContent = `${clima.umidadeAr.toFixed(0)}%`;
    document.getElementById('clima-chuva').textContent = clima.previsaoChuva
        ? `Sim · ${clima.volumeChuva.toFixed(1)} mm`
        : 'Sem previsão';
}

// =============== Gráfico ===============
function inicializarGrafico() {
    const ctx = document.getElementById('grafico-umidade').getContext('2d');
    const grad = ctx.createLinearGradient(0, 0, 0, 280);
    grad.addColorStop(0, 'rgba(76, 175, 80, 0.32)');
    grad.addColorStop(1, 'rgba(76, 175, 80, 0.02)');

    chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Umidade do solo',
                    data: [],
                    borderColor: '#2e7d32',
                    backgroundColor: grad,
                    tension: 0.35,
                    fill: true,
                    yAxisID: 'y',
                    borderWidth: 2.5,
                    pointRadius: 0,
                    pointHoverRadius: 5,
                    pointHoverBackgroundColor: '#2e7d32',
                    pointHoverBorderColor: '#fff',
                    pointHoverBorderWidth: 2
                },
                {
                    label: 'Volume de água (L)',
                    data: [],
                    borderColor: '#0288d1',
                    backgroundColor: 'rgba(2, 136, 209, 0.08)',
                    tension: 0.35,
                    fill: false,
                    yAxisID: 'y1',
                    borderWidth: 2,
                    pointRadius: 0,
                    pointHoverRadius: 5,
                    borderDash: [4, 4]
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'circle',
                        padding: 16,
                        font: { family: 'Inter', size: 12, weight: '600' },
                        color: '#6b7763'
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(31, 42, 28, 0.95)',
                    titleFont: { family: 'Inter', size: 12, weight: '600' },
                    bodyFont: { family: 'Inter', size: 12 },
                    padding: 10,
                    cornerRadius: 8,
                    boxPadding: 4
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: {
                        font: { family: 'JetBrains Mono', size: 10 },
                        color: '#99a292',
                        maxRotation: 0,
                        autoSkipPadding: 16
                    }
                },
                y: {
                    type: 'linear', position: 'left',
                    min: 0, max: 100,
                    grid: { color: 'rgba(227, 234, 219, 0.7)' },
                    ticks: {
                        font: { family: 'Inter', size: 11 },
                        color: '#99a292',
                        callback: (v) => v + '%'
                    },
                    title: { display: false }
                },
                y1: {
                    type: 'linear', position: 'right',
                    grid: { drawOnChartArea: false },
                    ticks: {
                        font: { family: 'Inter', size: 11 },
                        color: '#99a292',
                        callback: (v) => v + ' L'
                    }
                }
            }
        }
    });
}

function formatarHora(iso) {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}
function formatarHoraCurta(iso) {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

// =============== Alertas ===============
function atualizarAlertas(alertas) {
    const ul = document.getElementById('lista-alertas');
    document.getElementById('alertas-count').textContent =
        alertas.length === 0 ? 'Nenhuma' : `${alertas.length} mensagens`;

    if (alertas.length === 0) {
        ul.innerHTML = `
            <li class="alerta-empty">
                <div class="alerta-empty-icon">📡</div>
                <div>
                    <strong>Aguardando o primeiro ciclo de avaliação</strong>
                    <p>Os alertas do motor de regras aparecerão aqui em tempo real.</p>
                </div>
            </li>`;
        return;
    }

    ul.innerHTML = '';
    alertas.forEach(a => {
        const li = document.createElement('li');
        li.className = a.nivel;
        li.innerHTML = `
            <div class="alerta-msg">${a.mensagem}</div>
            <div class="timestamp">${formatarHora(a.timestamp)}</div>`;
        ul.appendChild(li);
    });
}

// =============== Loop principal ===============
async function atualizar() {
    try {
        const res = await fetch('/api/state');
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const s = await res.json();
        setConnState(true);

        atualizarInfoCultura(s.culturaAtiva);
        atualizarGauge(s.umidadeSolo, culturasInfo[s.culturaAtiva]);
        atualizarStatus(s.irrigacao, s.estrategia);
        atualizarClima(s.clima);

        if (s.irrigacao) {
            document.getElementById('volume-agua').textContent = s.irrigacao.volumeAgua.toFixed(1);
        }

        if (chart && s.historico) {
            chart.data.labels = s.historico.map(h => formatarHoraCurta(h.timestamp));
            chart.data.datasets[0].data = s.historico.map(h => h.umidadeSolo);
            chart.data.datasets[1].data = s.historico.map(h => h.volumeAgua);
            chart.update('none');
        }

        atualizarAlertas(s.alertas || []);
    } catch (e) {
        console.error('Erro ao atualizar:', e);
        setConnState(false);
    }
}

(async function init() {
    await carregarCulturas();
    inicializarGrafico();
    atualizar();
    setInterval(atualizar, POLLING_MS);
})();
