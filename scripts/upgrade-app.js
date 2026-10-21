const fs = require('fs');
const path = require('path');

const indexPath = path.join(__dirname, '..', 'index.html');
let html = fs.readFileSync(indexPath, 'utf8');

// =========================================================================
// 1. SINCRONIZAÇÃO DE ATRASOS NO FIREBASE E PERSISTÊNCIA APÓS TRANSMISSÃO
// =========================================================================

// A. Atualizar registrarAtrasoDoHorario para persistir no Firebase
const targetRegistrar = `function registrarAtrasoDoHorario(horaStr, atrasoMin) {
      const chaveHoje = obterChaveDataHoje();
      if (historicoAtrasosHoje.data !== chaveHoje || !historicoAtrasosHoje.mapa) {
        historicoAtrasosHoje = { data: chaveHoje, mapa: {} };
      }
      historicoAtrasosHoje.mapa[horaStr] = Math.max(0, Math.round(atrasoMin));
      localStorage.setItem('morabus_atrasos_dia', JSON.stringify(historicoAtrasosHoje));
    }`;

const replRegistrar = `function registrarAtrasoDoHorario(horaStr, atrasoMin) {
      if (!horaStr) return;
      const chaveHoje = obterChaveDataHoje();
      if (historicoAtrasosHoje.data !== chaveHoje || !historicoAtrasosHoje.mapa) {
        historicoAtrasosHoje = { data: chaveHoje, mapa: {} };
      }
      const valor = Math.max(0, Math.round(atrasoMin));
      const valorAnterior = historicoAtrasosHoje.mapa[horaStr] || 0;
      historicoAtrasosHoje.mapa[horaStr] = Math.max(valor, valorAnterior);
      localStorage.setItem('morabus_atrasos_dia', JSON.stringify(historicoAtrasosHoje));

      // Sincroniza o atraso consolidado no Firebase para que TODOS os alunos
      // continuem vendo o horário corrigido mesmo após o transmissor parar de compartilhar!
      if (valor >= 2 && valor > valorAnterior) {
        const chaveHora = horaStr.replace(':', '_');
        fetch(\`\${FIREBASE_DB_URL}/atrasos_consolidados/\${chaveHoje}/\${chaveHora}.json\`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            hora: horaStr,
            atrasoMinutos: valor,
            timestamp: { '.sv': 'timestamp' }
          }),
          keepalive: true
        }).catch(() => {});
      }
    }`;

// B. Atualizar lerDadosTempoRealDaNuvem para puxar atrasos_consolidados
const targetLerNuvem = `async function lerDadosTempoRealDaNuvem() {
      // Na web comum, economiza dados se a aba estiver oculta e o radar desligado; no app nativo ou com radar ativo, continua atualizando
      if (document.hidden && !ehAppNativo() && localStorage.getItem('morabus_radar_permanente') !== 'ativo') return;

      try {
        const [telemetria, al] = await Promise.all([
          buscarTelemetria(Date.now() - 120000),
          fetch(\`\${FIREBASE_DB_URL}/alertas_comunidade/ultimo.json\`).then(r => r.ok ? r.json() : null)
        ]);`;

const replLerNuvem = `async function lerDadosTempoRealDaNuvem() {
      // Na web comum, economiza dados se a aba estiver oculta e o radar desligado; no app nativo ou com radar ativo, continua atualizando
      if (document.hidden && !ehAppNativo() && localStorage.getItem('morabus_radar_permanente') !== 'ativo') return;

      try {
        const [telemetria, al, atrasosNuvem] = await Promise.all([
          buscarTelemetria(Date.now() - 120000),
          fetch(\`\${FIREBASE_DB_URL}/alertas_comunidade/ultimo.json\`).then(r => r.ok ? r.json() : null),
          fetch(\`\${FIREBASE_DB_URL}/atrasos_consolidados/\${obterChaveDataHoje()}.json\`).then(r => r.ok ? r.json() : null)
        ]);

        // Carrega atrasos consolidados salvos por qualquer colaborador no dia
        if (atrasosNuvem && typeof atrasosNuvem === 'object') {
          const chaveHoje = obterChaveDataHoje();
          if (historicoAtrasosHoje.data !== chaveHoje || !historicoAtrasosHoje.mapa) {
            historicoAtrasosHoje = { data: chaveHoje, mapa: {} };
          }
          let mudouHistorico = false;
          for (const item of Object.values(atrasosNuvem)) {
            if (item && item.hora && Number.isFinite(item.atrasoMinutos)) {
              const atr = Math.round(item.atrasoMinutos);
              if (atr > (historicoAtrasosHoje.mapa[item.hora] || 0)) {
                historicoAtrasosHoje.mapa[item.hora] = atr;
                mudouHistorico = true;
              }
            }
          }
          if (mudouHistorico) {
            localStorage.setItem('morabus_atrasos_dia', JSON.stringify(historicoAtrasosHoje));
          }
        }`;

// C. Se não há ninguém transmitindo ao vivo, mas a viagem atual tem atraso consolidado conhecido, exibe no banner!
const targetBanner = `if (!ev.ativo && !ev.alertaComunidade) {
        banner.style.display = "none";
        contador.textContent = "⚪ Sem sinal ao vivo";
        return;
      }`;

const replBanner = `const atualMin = minutosAgora();
      const horarios = horariosHoje();
      const viagemEmAndamentoComAtraso = (!ev.ativo) ? horarios.find(h => {
        const minS = minutos(h[0]);
        const dur = duracaoRota(obterRota(h[1], h[2]));
        const atr = atrasoConhecido(h[0]);
        return (atualMin >= minS && atualMin <= (minS + dur + atr + 5)) && atr >= 2;
      }) : null;

      if (!ev.ativo && !ev.alertaComunidade && !viagemEmAndamentoComAtraso) {
        banner.style.display = "none";
        contador.textContent = "⚪ Sem sinal ao vivo";
        return;
      }`;

const targetBannerSemGps = `} else {
        contador.textContent = "⚪ Sem GPS ativo";
        banner.className = "status-ao-vivo-banner status-atrasado";
      }`;

const replBannerSemGps = `} else if (viagemEmAndamentoComAtraso) {
        const horaV = viagemEmAndamentoComAtraso[0];
        const atrV = atrasoConhecido(horaV);
        contador.textContent = "🕒 Atraso registrado";
        banner.className = "status-ao-vivo-banner status-atrasado";
        htmlBanner += \`⚠️ <b>Atraso registrado (+\\\${atrV} min)</b> — Ônibus das <b>\${horaV}</b> (\\\${esc(viagemEmAndamentoComAtraso[1])})<br>\`;
        htmlBanner += \`ℹ️ Horários das paradas atualizados com base no último monitoramento.\`;
      } else {
        contador.textContent = "⚪ Sem GPS ativo";
        banner.className = "status-ao-vivo-banner status-atrasado";
      }`;

function normalizeAndReplace(src, target, repl) {
  const normSrc = src.replace(/\r\n/g, '\n');
  const normTarget = target.replace(/\r\n/g, '\n');
  const normRepl = repl.replace(/\r\n/g, '\n');
  if (!normSrc.includes(normTarget)) {
    throw new Error('Target not found:\n' + normTarget.slice(0, 100));
  }
  return normSrc.replace(normTarget, normRepl).replace(/\n/g, '\r\n');
}

html = normalizeAndReplace(html, targetRegistrar, replRegistrar);
html = normalizeAndReplace(html, targetLerNuvem, replLerNuvem);
html = normalizeAndReplace(html, targetBanner, replBanner);
html = normalizeAndReplace(html, targetBannerSemGps, replBannerSemGps);

// =========================================================================
// 2. MODERNIZAÇÃO VISUAL COMPLETA (CSS DE ALTO NÍVEL)
// =========================================================================

const novoCSS = `
    :root {
      --primary: #006633;
      --primary-dark: #004d26;
      --primary-light: #e8f5e9;
      --primary-glow: rgba(0, 102, 51, 0.12);
      --accent: #e67e22;
      --accent-light: #fff7ed;
      --bg: #f8fafc;
      --surface: #ffffff;
      --surface-subtle: #f1f5f9;
      --border: #e2e8f0;
      --border-focus: #006633;
      --text-main: #0f172a;
      --text-muted: #64748b;
      --danger: #dc2626;
      --danger-light: #fef2f2;
      --shadow-sm: 0 1px 3px rgba(0,0,0,0.05), 0 1px 2px rgba(0,0,0,0.03);
      --shadow-md: 0 4px 12px rgba(0,0,0,0.05), 0 2px 4px rgba(0,0,0,0.02);
      --shadow-lg: 0 10px 25px -5px rgba(0,102,51,0.08), 0 8px 10px -6px rgba(0,0,0,0.03);
      --radius-sm: 8px;
      --radius-md: 12px;
      --radius-lg: 18px;
      --radius-pill: 9999px;
    }

    * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }

    body {
      font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
      margin: 0 auto;
      padding: 16px 14px 40px 14px;
      background: var(--bg);
      color: var(--text-main);
      max-width: 580px;
      line-height: 1.5;
      -webkit-font-smoothing: antialiased;
    }

    /* Cabeçalho estilo App Nativo */
    .header-app {
      text-align: center;
      margin-bottom: 18px;
      padding-top: max(8px, env(safe-area-inset-top));
    }

    .header-logo-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      background: white;
      border-radius: 14px;
      box-shadow: 0 4px 14px rgba(0, 102, 51, 0.15);
      margin-bottom: 8px;
      border: 1px solid rgba(0, 102, 51, 0.15);
    }

    .header-logo-badge img {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      object-fit: cover;
    }

    h1 {
      color: var(--primary);
      font-size: 24px;
      font-weight: 800;
      margin: 2px 0;
      letter-spacing: -0.6px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }

    .subtitulo-app {
      font-size: 13px;
      color: var(--text-muted);
      font-weight: 500;
      margin-bottom: 12px;
      letter-spacing: -0.2px;
    }

    .pilula-relogio {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: var(--surface);
      color: var(--primary-dark);
      font-size: 12px;
      font-weight: 600;
      padding: 6px 14px;
      border-radius: var(--radius-pill);
      margin-bottom: 14px;
      border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    }

    .ponto-pulsante {
      width: 8px;
      height: 8px;
      background: #16a34a;
      border-radius: 50%;
      display: inline-block;
      animation: pulsar 2s infinite ease-in-out;
      box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
    }

    @keyframes pulsar {
      0% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(0.85); }
      100% { opacity: 1; transform: scale(1); }
    }

    .acoes-topo { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }

    .btn-topo {
      width: auto;
      padding: 8px 16px;
      font-size: 13px;
      font-weight: 600;
      border-radius: var(--radius-pill);
      background: var(--surface);
      color: var(--primary);
      border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
      margin-bottom: 0;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .btn-topo:active { transform: scale(0.97); }

    .btn-topo.instalar {
      background: var(--primary);
      color: white;
      border-color: var(--primary);
      box-shadow: 0 4px 10px rgba(0, 102, 51, 0.25);
    }

    /* Cards Gerais com design moderno */
    .card {
      background: var(--surface);
      padding: 20px;
      margin: 14px 0;
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-md);
      border: 1px solid var(--border);
      transition: box-shadow 0.2s ease;
    }

    .card h2 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 700;
      color: #334155;
      border-bottom: 1px solid var(--border);
      padding-bottom: 12px;
      letter-spacing: -0.3px;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .card-header-flex {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border);
      padding-bottom: 12px;
      margin-bottom: 16px;
      gap: 8px;
    }

    .card-header-flex h2 { border-bottom: none; padding-bottom: 0; margin-bottom: 0; }

    /* Barra de Ponto Favorito */
    .favorito-bar {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 10px 14px;
      margin-bottom: 14px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      box-shadow: var(--shadow-sm);
    }

    .favorito-bar label {
      font-size: 13px;
      font-weight: 700;
      color: #334155;
      white-space: nowrap;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .favorito-select {
      flex: 1;
      padding: 8px 12px;
      font-size: 13px;
      font-family: inherit;
      font-weight: 600;
      border-radius: 10px;
      border: 1px solid #cbd5e1;
      background: var(--surface-subtle);
      color: var(--text-main);
      outline: none;
      transition: border-color 0.2s;
    }

    .favorito-select:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px var(--primary-glow);
    }

    /* Radar Bar */
    .radar-bar {
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
      padding: 16px;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
    }

    .radar-topo { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
    .radar-titulo { font-size: 14px; font-weight: 700; color: #1e293b; }
    .radar-descricao { font-size: 12px; color: var(--text-muted); line-height: 1.45; }

    #statusRadarPermanente {
      font-size: 11px;
      font-weight: 700;
      color: #64748b;
      background: #e2e8f0;
      padding: 4px 10px;
      border-radius: var(--radius-pill);
      white-space: nowrap;
    }

    #statusRadarPermanente.ativo {
      color: #15803d;
      background: #dcfce7;
      border: 1px solid #86efac;
    }

    /* HERO CARD: O Próximo Ônibus */
    .hero-card {
      border-top: 5px solid var(--primary);
      box-shadow: var(--shadow-lg);
      background: linear-gradient(180deg, #ffffff 0%, #fdfdfd 100%);
    }

    .hero-main { text-align: center; padding: 6px 0 2px 0; }

    #proximo {
      font-size: 44px;
      font-weight: 800;
      color: var(--primary);
      margin: 2px 0;
      line-height: 1.1;
      letter-spacing: -1.5px;
      font-variant-numeric: tabular-nums;
    }

    #tempo {
      font-size: 18px;
      font-weight: 700;
      color: var(--accent);
      margin-top: 6px;
    }

    .etiqueta-ultimo {
      background: var(--danger);
      color: white;
      padding: 5px 12px;
      border-radius: var(--radius-pill);
      font-size: 12px;
      font-weight: 800;
      display: inline-block;
      margin-bottom: 8px;
      letter-spacing: 0.3px;
    }

    .hero-meta {
      display: flex;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 12px;
    }

    #origem { font-size: 14px; color: var(--text-muted); font-weight: 500; }

    #qtd-onibus {
      display: inline-block;
      padding: 4px 12px;
      background: var(--primary-light);
      border-radius: var(--radius-pill);
      font-size: 12px;
      font-weight: 700;
      color: var(--primary-dark);
      border: 1px solid rgba(0, 102, 51, 0.15);
    }

    .box-meu-ponto {
      margin-top: 16px;
      background: var(--accent-light);
      border: 1px solid #fed7aa;
      border-radius: var(--radius-md);
      padding: 12px 14px;
      font-size: 13.5px;
      color: #9a3412;
      text-align: center;
      line-height: 1.45;
      box-shadow: 0 1px 3px rgba(234, 88, 12, 0.05);
    }

    /* Painel Duplo: Próximo por Sentido */
    .resumo-sentidos {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 18px;
      padding-top: 16px;
      border-top: 1px dashed var(--border);
    }

    .box-sentido {
      background: #f8fafc;
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 12px 10px;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      text-align: center;
    }

    .box-sentido:active { transform: scale(0.97); }
    .box-sentido:hover {
      border-color: var(--primary);
      box-shadow: 0 4px 12px var(--primary-glow);
    }
    .box-sentido:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }

    .box-sentido-titulo { font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; }
    .box-sentido-hora { font-size: 22px; font-weight: 800; color: #1e293b; font-variant-numeric: tabular-nums; }
    .box-sentido-falta { font-size: 12px; font-weight: 700; color: var(--primary); margin-top: 3px; }

    .hora-riscada { text-decoration: line-through; opacity: 0.45; font-size: 14px; margin-right: 4px; font-weight: 600; }
    .texto-atraso { color: #ea580c; font-weight: 700; }

    /* CARD COLABORATIVO (GPS em tempo real) */
    .card-waze {
      border: 1.5px solid #86efac;
      background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%);
    }

    #contadorColaboradores {
      font-size: 12px;
      font-weight: 700;
      color: var(--primary);
      background: white;
      padding: 4px 10px;
      border-radius: var(--radius-pill);
      border: 1px solid #bbf7d0;
    }

    .status-ao-vivo-banner {
      display: none;
      padding: 12px 14px;
      border-radius: var(--radius-md);
      margin-bottom: 14px;
      font-size: 13.5px;
      font-weight: 600;
      line-height: 1.5;
    }

    .status-ao-vivo-banner hr { border: 0; border-top: 1px solid rgba(0,0,0,0.08); margin: 8px 0; }

    .status-no-horario {
      background: #f0fdf4;
      color: #166534;
      border: 1px solid #bbf7d0;
    }

    .status-atrasado {
      background: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }

    .btn-transmitir {
      background: linear-gradient(180deg, #006633 0%, #004d26 100%);
      margin-bottom: 10px;
      box-shadow: 0 4px 12px rgba(0, 102, 51, 0.25);
    }

    .btn-transmitir.transmitindo {
      background: linear-gradient(180deg, #dc2626 0%, #b91c1c 100%);
      animation: pulsarBotao 2s infinite ease-in-out;
      box-shadow: 0 4px 12px rgba(220, 38, 38, 0.35);
    }

    @keyframes pulsarBotao {
      0% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.6); }
      70% { box-shadow: 0 0 0 12px rgba(220, 38, 38, 0); }
      100% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0); }
    }

    .rotulo-ocorrencia {
      font-size: 12px;
      font-weight: 700;
      color: var(--text-muted);
      margin-top: 12px;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }

    .botoes-ocorrencia { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 8px; }

    .btn-ocorrencia {
      padding: 10px 8px;
      font-size: 12.5px;
      font-weight: 600;
      background: var(--surface);
      color: #334155;
      border: 1px solid var(--border);
      border-radius: 10px;
      margin-bottom: 0;
      box-shadow: var(--shadow-sm);
      transition: all 0.15s;
    }

    .btn-ocorrencia:active { transform: scale(0.97); }
    .btn-ocorrencia.largo { grid-column: span 2; }

    /* Linha do tempo visual para as chegadas */
    #infoViagem {
      font-size: 13.5px;
      color: var(--text-muted);
      margin-bottom: 16px;
      line-height: 1.5;
    }

    .badge-andamento {
      display: inline-block;
      background: var(--primary-light);
      color: var(--primary-dark);
      font-weight: 700;
      font-size: 12px;
      padding: 4px 10px;
      border-radius: var(--radius-pill);
      margin-bottom: 8px;
      border: 1px solid rgba(0, 102, 51, 0.15);
    }

    .badge-andamento.simulado {
      background: #e0f2fe;
      color: #0369a1;
      border-color: #bae6fd;
    }

    #chegadas {
      list-style: none;
      padding-left: 22px;
      border-left: 3px solid #cbd5e1;
      margin-left: 12px;
      margin-bottom: 0;
    }

    #chegadas li {
      position: relative;
      padding: 8px 10px;
      margin-bottom: 8px;
      font-size: 14.5px;
      color: #334155;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-radius: 10px;
      transition: background 0.15s;
    }

    #chegadas li::before {
      content: '';
      position: absolute;
      left: -29.5px;
      top: 14px;
      width: 12px;
      height: 12px;
      background: white;
      border: 3px solid #94a3b8;
      border-radius: 50%;
    }

    #chegadas li.ponto-concluido { opacity: 0.45; }
    #chegadas li.ponto-concluido::before { background: var(--primary); border-color: var(--primary); }

    #chegadas li.ponto-atual {
      font-weight: 700;
      color: var(--primary-dark);
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
    }
    #chegadas li.ponto-atual::before {
      background: #ea580c;
      border-color: #ea580c;
      transform: scale(1.3);
      box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.25);
    }

    #chegadas li.ponto-favorito-destaque {
      background: #fffbeb;
      border: 1px solid #fde68a;
      font-weight: 700;
    }

    .link-maps-ponto {
      text-decoration: none;
      font-size: 11px;
      font-weight: 600;
      color: var(--primary);
      background: #f1f5f9;
      padding: 4px 8px;
      border-radius: 6px;
      white-space: nowrap;
      flex-shrink: 0;
      border: 1px solid #e2e8f0;
      transition: all 0.15s;
    }

    .link-maps-ponto:hover { background: #e2e8f0; }
    .link-acao { font-size: 12px; color: var(--primary); font-weight: 600; margin-left: 6px; }

    /* Seletor de Grade */
    .abas-grade {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 6px;
      background: #f1f5f9;
      padding: 4px;
      border-radius: 12px;
      margin-bottom: 14px;
    }

    .btn-aba-grade {
      margin: 0;
      padding: 9px;
      font-size: 13px;
      font-weight: 700;
      border-radius: 9px;
      background: transparent;
      color: #64748b;
      box-shadow: none;
      border: none;
      transition: all 0.2s;
    }

    .btn-aba-grade.ativa {
      background: white;
      color: var(--primary);
      box-shadow: 0 2px 6px rgba(0,0,0,0.06);
    }

    .aviso-grade-diferente {
      font-size: 12px;
      background: #e0f2fe;
      color: #0369a1;
      padding: 8px 12px;
      border-radius: 10px;
      margin-bottom: 14px;
      font-weight: 600;
      text-align: center;
      border: 1px solid #bae6fd;
    }

    /* Lista de Horários do Dia */
    #lista { list-style-type: none; padding: 0; margin: 0; }

    #lista li {
      padding: 12px 10px;
      border-bottom: 1px solid #f1f5f9;
      color: #334155;
      font-size: 14.5px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
      border-radius: 10px;
      transition: background 0.15s;
    }

    #lista li:last-child { border-bottom: none; }
    #lista li.horario-passado { opacity: 0.42; }

    #lista li.horario-proximo {
      background: #f0fdf4;
      border-left: 4px solid var(--primary);
      padding-left: 12px;
      font-weight: 600;
    }

    .selo-proximo {
      font-size: 10px;
      font-weight: 800;
      text-transform: uppercase;
      background: var(--primary);
      color: white;
      padding: 2px 7px;
      border-radius: 6px;
      margin-left: 6px;
      vertical-align: middle;
      letter-spacing: 0.4px;
    }

    .info-horario-clicavel { cursor: pointer; flex: 1; }
    .icones-comboio { font-size: 12px; letter-spacing: 3px; }

    /* Tags de cores */
    .tag-moradia, .tag-escola, .tag-passagem {
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 6px;
      display: inline-block;
      font-size: 12.5px;
    }

    .tag-moradia { color: #166534; background: #dcfce7; }
    .tag-escola { color: #1e40af; background: #dbeafe; }
    .tag-passagem { color: #9a3412; background: #ffedd5; }

    /* Botão Padrão */
    button {
      width: 100%;
      padding: 13px 16px;
      font-size: 15px;
      font-weight: 700;
      font-family: inherit;
      border: none;
      border-radius: var(--radius-md);
      background: var(--primary);
      color: white;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      margin-bottom: 12px;
      box-shadow: 0 4px 10px rgba(0,102,51,0.2);
    }

    button:active { transform: scale(0.98); }
    button.perigo { background: var(--danger); box-shadow: 0 4px 10px rgba(220,38,38,0.25); }
    button.radar-ativo { background: #0284c7; box-shadow: 0 4px 10px rgba(2,132,199,0.25); }

    #btnRadarPermanente { margin-bottom: 0; padding: 11px 14px; font-size: 13.5px; }

    .btn-volta {
      background: linear-gradient(180deg, #ea580c 0%, #c2410c 100%);
      box-shadow: 0 4px 12px rgba(234,88,12,0.25);
    }

    .filtros-container { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }

    .btn-filtro {
      width: auto;
      padding: 7px 14px;
      font-size: 12.5px;
      font-weight: 600;
      border-radius: var(--radius-pill);
      background: #f1f5f9;
      color: #475569;
      margin-bottom: 0;
      box-shadow: none;
      border: 1px solid var(--border);
    }

    .btn-filtro.ativo {
      background: var(--primary);
      color: white;
      border-color: var(--primary);
    }

    .btn-ocultar-passados {
      width: auto;
      padding: 5px 12px;
      font-size: 12px;
      font-weight: 600;
      border-radius: var(--radius-pill);
      background: #f1f5f9;
      color: #64748b;
      margin-bottom: 0;
      box-shadow: none;
      border: 1px solid var(--border);
    }

    .btn-alarme {
      width: auto;
      padding: 6px 12px;
      margin-bottom: 0;
      font-size: 13px;
      background: #f1f5f9;
      color: #475569;
      border-radius: 8px;
      box-shadow: none;
      border: 1px solid var(--border);
      flex-shrink: 0;
    }

    .btn-alarme.agendado {
      background: #dcfce7;
      color: #166534;
      border-color: #86efac;
    }

    .card-caminhada {
      margin-top: 12px;
      padding: 12px 14px;
      border-radius: var(--radius-md);
      background: #f8fafc;
      border: 1px solid var(--border);
      font-size: 13.5px;
      line-height: 1.5;
    }

    .txt-erro { color: #dc2626; font-weight: 700; }
    .txt-alerta { color: #ea580c; font-weight: 700; }
    .txt-ok { color: #16a34a; font-weight: 700; }
    .destaque-espera { font-size: 20px; font-weight: 800; color: #ea580c; }
    .destaque-espera.verde { color: #16a34a; }

    #gps, #resultadoVolta { font-size: 13.5px; line-height: 1.5; color: #475569; }

    .texto-subtitulo { font-size: 13.5px; color: var(--text-muted); margin-top: 0; }
    .texto-dia-seguinte { color: #334155; font-size: 22px; margin-top: 10px; display: block; }
    .msg-sem-onibus { font-size: 20px; color: #dc2626; font-weight: 700; }

    #mapa, #mapaVolta {
      height: 300px;
      width: 100%;
      margin-top: 15px;
      border-radius: var(--radius-md);
      display: none;
      z-index: 1;
      border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    }

    .rodape-compartilhar { text-align: center; padding: 20px 10px 30px 10px; font-size: 13px; color: #94a3b8; }
    .rodape-compartilhar a { color: var(--primary); font-weight: 600; text-decoration: none; }

    @media (prefers-reduced-motion: reduce) {
      .ponto-pulsante, .btn-transmitir.transmitindo { animation: none; }
    }

    /* --- MODO ESCURO AUTOMÁTICO REFINADO (SLATE DARK) --- */
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #0b1320;
        --surface: #131c2e;
        --surface-subtle: #1e293b;
        --border: #26354a;
        --text-main: #f1f5f9;
        --text-muted: #94a3b8;
        --primary-light: #064e3b;
        --accent-light: #431407;
      }

      body { background: var(--bg); color: var(--text-main); }
      .header-logo-badge { background: #131c2e; border-color: rgba(74, 222, 128, 0.2); }
      .subtitulo-app { color: var(--text-muted); }
      .pilula-relogio { background: #131c2e; color: #4ade80; border-color: var(--border); }
      .favorito-bar { background: var(--surface); border-color: var(--border); }
      .favorito-bar label { color: #f1f5f9; }
      .favorito-select { background: #1e293b; border-color: #334155; color: #f8fafc; }
      #statusRadarPermanente { background: #1e293b; color: #94a3b8; }
      #statusRadarPermanente.ativo { color: #4ade80; background: rgba(74, 222, 128, 0.15); border-color: rgba(74, 222, 128, 0.3); }

      .card { background: var(--surface); border-color: var(--border); box-shadow: 0 6px 18px rgba(0,0,0,0.4); }
      .hero-card { border-top-color: #4ade80; background: linear-gradient(180deg, #131c2e 0%, #111827 100%); }
      .card h2, .card-header-flex { color: #f1f5f9; border-bottom-color: var(--border); }

      .btn-topo { background: #131c2e; color: #4ade80; border-color: var(--border); }
      .btn-topo.instalar { background: #10b981; color: #0b1320; border-color: #10b981; }

      .box-meu-ponto { background: #2a1b0b; border-color: #7c2d12; color: #fdba74; }
      .resumo-sentidos { border-top-color: var(--border); }
      .box-sentido { background: #182338; border-color: var(--border); }
      .box-sentido-titulo { color: #94a3b8; }
      .box-sentido-hora { color: #f8fafc; }
      .box-sentido-falta { color: #4ade80; }

      h1, #proximo { color: #4ade80; }
      #tempo { color: #fb923c; }
      #origem, #gps, #resultadoVolta, #infoViagem, .texto-subtitulo, .rotulo-ocorrencia { color: var(--text-muted); }
      .txt-ok, .destaque-espera.verde, .link-acao, .rodape-compartilhar a { color: #4ade80; }

      .badge-andamento { background: #064e3b; color: #6ee7b7; border-color: rgba(110, 231, 183, 0.2); }
      .badge-andamento.simulado { background: #0c4a6e; color: #7dd3fc; border-color: #0284c7; }

      #chegadas { border-left-color: #334155; }
      #chegadas li { color: #cbd5e1; }
      #chegadas li::before { background: #131c2e; border-color: #64748b; }
      #chegadas li.ponto-concluido::before { background: #4ade80; border-color: #4ade80; }
      #chegadas li.ponto-atual { color: #4ade80; background: #092615; border-color: #14532d; }
      #chegadas li.ponto-favorito-destaque { background: #2a1b0b; border-color: #7c2d12; color: #fdba74; }
      .link-maps-ponto { background: #1e293b; color: #4ade80; border-color: #334155; }

      .abas-grade { background: #0f172a; }
      .btn-aba-grade { color: #94a3b8; }
      .btn-aba-grade.ativa { background: #1e293b; color: #4ade80; }
      .aviso-grade-diferente { background: #082f49; color: #7dd3fc; border-color: #0369a1; }

      #lista li { color: #cbd5e1; border-bottom-color: #1e293b; }
      #lista li.horario-proximo { background: #092615; border-left-color: #4ade80; }
      .selo-proximo { background: #4ade80; color: #0b1320; }

      .card-caminhada { background: #182338; border-color: var(--border); color: #e2e8f0; }
      #mapa, #mapaVolta { border-color: var(--border); }
      #qtd-onibus { background: #064e3b; color: #6ee7b7; border-color: rgba(110, 231, 183, 0.2); }

      .tag-moradia { background: #064e3b; color: #6ee7b7; }
      .tag-escola { background: #1e3a8a; color: #93c5fd; }
      .tag-passagem { background: #451a03; color: #fdba74; }

      .btn-filtro, .btn-alarme, .btn-ocultar-passados { background: #1e293b; color: #cbd5e1; border-color: var(--border); }
      .btn-filtro.ativo { background: #10b981; color: #0b1320; border-color: #10b981; }
      .btn-alarme.agendado { background: #064e3b; color: #6ee7b7; border-color: #10b981; }

      .card-waze { background: linear-gradient(180deg, #092615 0%, #131c2e 100%); border-color: #059669; }
      #contadorColaboradores { background: #131c2e; color: #4ade80; border-color: #059669; }

      .status-no-horario { background: #092615; color: #6ee7b7; border-color: #047857; }
      .status-atrasado { background: #450a0a; color: #fca5a5; border-color: #991b1b; }
      .status-ao-vivo-banner hr { border-top-color: rgba(255,255,255,0.12); }
      .btn-ocorrencia { background: #1e293b; color: #e2e8f0; border-color: var(--border); }
    }
`;

// Substitui o bloco <style>...</style>
const cssRegex = /<style>[\s\S]*?<\/style>/;
if (!cssRegex.test(html)) {
  throw new Error('Bloco <style> não encontrado');
}
html = html.replace(cssRegex, '<style>' + novoCSS + '</style>');

// Salva o index.html atualizado
fs.writeFileSync(indexPath, html, 'utf8');
console.log('App successfully upgraded with Firebase delay persistence and modern UI design!');
