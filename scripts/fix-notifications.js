const fs = require('fs');
const path = require('path');

const indexPath = path.join(__dirname, '..', 'index.html');
let html = fs.readFileSync(indexPath, 'utf8');

function normalizeAndReplace(src, target, repl) {
  const normSrc = src.replace(/\r\n/g, '\n');
  const normTarget = target.replace(/\r\n/g, '\n');
  const normRepl = repl.replace(/\r\n/g, '\n');
  if (!normSrc.includes(normTarget)) {
    throw new Error('Target not found:\n' + normTarget.slice(0, 100));
  }
  return normSrc.replace(normTarget, normRepl).replace(/\n/g, '\r\n');
}

// 1. Atualizar o Card do Radar no HTML (removendo a promessa de barra contínua que irritava com vibração)
const targetHtmlRadar = `<span class="radar-titulo">🛰️ Radar & Barra Ao Vivo</span>
      <span id="statusRadarPermanente">⚪ Desativado</span>
    </div>
    <div class="radar-descricao">
      Enquanto o app estiver <b>aberto na tela</b>, acompanha seu GPS, avisa se o ônibus do seu ponto atrasar,
      quando sair caminhando e mostra uma <b>notificação de aproximação ao vivo</b>.
    </div>
    <button id="btnRadarPermanente" onclick="alternarRadarPermanente()">
      🔔 Ativar Radar e Barra Ao Vivo
    </button>`;

const replHtmlRadar = `<span class="radar-titulo">🛰️ Radar Inteligente (Alertas no Ponto)</span>
      <span id="statusRadarPermanente">⚪ Desativado</span>
    </div>
    <div class="radar-descricao">
      Acompanha o trajeto e <b>avisa com notificação pontual no celular</b> apenas quando necessário: se o ônibus <b>atrasar</b> ou quando estiver a <b>5 minutos</b> de chegar no seu ponto.
    </div>
    <button id="btnRadarPermanente" onclick="alternarRadarPermanente()">
      🔔 Ativar Radar de Notificações
    </button>`;

html = normalizeAndReplace(html, targetHtmlRadar, replHtmlRadar);

// 2. Atualizar o texto do botão na função atualizarVisualRadar
const targetVisualRadar = `btn.textContent = ativo ? "🛰️ Radar Ativo (Toque p/ desligar)" : "🔔 Ativar Radar e Barra Ao Vivo";`;
const replVisualRadar = `btn.textContent = ativo ? "🛰️ Radar Ativo (Toque p/ desligar)" : "🔔 Ativar Radar de Notificações";`;
html = normalizeAndReplace(html, targetVisualRadar, replVisualRadar);

// 3. Atualizar a função enviarNotificacao com canal nativo e sem vibração repetitiva
const targetEnviarNotif = `    // Usa LocalNotifications no Android Nativo (Capacitor) ou showNotification do Service Worker na Web
    async function enviarNotificacao(titulo, mensagem, opcoesExtras = {}) {
      const localNotif = obterPluginCapacitor('LocalNotifications');
      if (ehAppNativo() && localNotif) {
        try {
          const perm = await localNotif.checkPermissions();
          if (perm.display !== 'granted') {
            const req = await localNotif.requestPermissions();
            if (req.display !== 'granted') return;
          }
          const idNum = opcoesExtras.tag === TAG_LIVE
            ? 999001
            : Math.abs(String(opcoesExtras.tag || titulo).split('').reduce((a, c) => ((a << 5) - a) + c.charCodeAt(0), 0)) % 900000 + 1;
          await localNotif.schedule({
            notifications: [{
              id: idNum,
              title: titulo,
              body: mensagem,
              ongoing: Boolean(opcoesExtras.requireInteraction),
              autoCancel: !opcoesExtras.requireInteraction
            }]
          });
          return;
        } catch (e) {
          console.warn('Fallback para notificação web:', e);
        }
      }

      if (!('Notification' in window) || Notification.permission !== 'granted') return;
      const opcoes = { body: mensagem, icon: './Morabus.png', badge: './Morabus.png', ...opcoesExtras };
      try {
        if ('serviceWorker' in navigator) {
          const reg = await navigator.serviceWorker.ready;
          await reg.showNotification(titulo, opcoes);
          return;
        }
        new Notification(titulo, opcoes);
      } catch (e) {
        console.log('Falha ao notificar:', e);
      }
    }`;

const replEnviarNotif = `    let canalNotificacoesCriado = false;
    async function garantirCanalNotificacoes(localNotif) {
      if (canalNotificacoesCriado || !localNotif || !localNotif.createChannel) return;
      try {
        await localNotif.createChannel({
          id: 'morabus_alertas',
          name: 'Alertas do Ônibus (Morabus)',
          description: 'Avisos quando o ônibus atrasar ou estiver a 5 minutos do seu ponto',
          importance: 4, // IMPORTANCE_HIGH
          visibility: 1, // VISIBILITY_PUBLIC
          vibration: true
        });
        canalNotificacoesCriado = true;
      } catch (e) {}
    }

    // Trava em memória para nunca disparar a mesma notificação repetidamente
    const historicoDisparosMemoria = new Set();

    // Envia notificações PONTUAIS (apenas nos momentos importantes)
    async function enviarNotificacao(titulo, mensagem, opcoesExtras = {}) {
      const chaveDeduplicacao = String(opcoesExtras.tag || (titulo + mensagem));
      if (historicoDisparosMemoria.has(chaveDeduplicacao)) return;
      historicoDisparosMemoria.add(chaveDeduplicacao);

      const localNotif = obterPluginCapacitor('LocalNotifications');
      if (ehAppNativo() && localNotif) {
        try {
          const perm = await localNotif.checkPermissions();
          if (perm.display !== 'granted') {
            const req = await localNotif.requestPermissions();
            if (req.display !== 'granted') return;
          }
          await garantirCanalNotificacoes(localNotif);
          const idNum = Math.abs(chaveDeduplicacao.split('').reduce((a, c) => ((a << 5) - a) + c.charCodeAt(0), 0)) % 900000 + 1;
          await localNotif.schedule({
            notifications: [{
              id: idNum,
              title: titulo,
              body: mensagem,
              channelId: 'morabus_alertas',
              ongoing: false,
              autoCancel: true
            }]
          });
          return;
        } catch (e) {
          console.warn('Fallback para notificação web:', e);
        }
      }

      if (!('Notification' in window) || Notification.permission !== 'granted') return;
      const opcoes = {
        body: mensagem,
        icon: './Morabus.png',
        badge: './Morabus.png',
        tag: opcoesExtras.tag || 'morabus-alerta',
        renotify: false,
        silent: Boolean(opcoesExtras.silent),
        ...opcoesExtras
      };
      try {
        if ('serviceWorker' in navigator) {
          const reg = await navigator.serviceWorker.ready;
          await reg.showNotification(titulo, opcoes);
          return;
        }
        new Notification(titulo, opcoes);
      } catch (e) {
        console.log('Falha ao notificar:', e);
      }
    }`;

html = normalizeAndReplace(html, targetEnviarNotif, replEnviarNotif);

// 4. Reformular a função avaliarGatilhosInteligentesEBarraAoVivo para ter APENAS:
// - Alerta de atraso (uma vez)
// - Alerta de 5 minutos antes da chegada no ponto (uma vez)
// - Alerta de último ônibus de volta (uma vez)
// E ELIMINAR COMPLETAMENTE o loop que chamava enviarNotificacao a cada 10 segundos!
const targetGatilhos = `    function avaliarGatilhosInteligentesEBarraAoVivo() {
      if (localStorage.getItem('morabus_radar_permanente') !== 'ativo') return;
      if (!posicaoUsuarioRadar.naRegiaoUFMGMoradia) {
        fecharNotificacaoAoVivo();
        return;
      }

      const atualMin = minutosAgora();
      const horarios = horariosHoje();
      if (horarios.length === 0) {
        fecharNotificacaoAoVivo();
        return;
      }

      // Ponto-alvo: favorito ou, se não houver, o mais próximo do GPS
      let pontoAlvo = pontoFavorito;
      if (!pontoAlvo && posicaoUsuarioRadar.lat) {
        const mp = pontoMaisProximo(posicaoUsuarioRadar.lat, posicaoUsuarioRadar.lon);
        if (mp.ponto) pontoAlvo = mp.ponto.nome;
      }
      if (!pontoAlvo) return;

      const infoProx = buscarProximoNoPonto(pontoAlvo, false);
      let atrasoDaViagem = 0;

      if (infoProx) {
        atrasoDaViagem = infoProx.atraso || 0;

        // GATILHO 1: atraso no ponto do aluno (a partir de +3 min)
        if (atrasoDaViagem >= 3 && infoProx.espera <= 35) {
          const chaveAtraso = \`atraso_\${infoProx.horaSaidaOrigem}_\${pontoAlvo}\`;
          if (!jaDisparouAlertaAutomatico(chaveAtraso)) {
            marcarAlertaAutomaticoDisparado(chaveAtraso);
            const novaHora = infoProx.horaChegadaReal;
            const motivo = diagnosticarCausaAtraso(atrasoDaViagem, estadoAoVivoGlobal.velocidadeKmh);
            enviarNotificacao(
              \`⚠️ Ônibus atrasado (+\${atrasoDaViagem} min)\`,
              \`Previsão \${preposicaoPonto(pontoAlvo)} \${pontoAlvo} mudou para \${novaHora}. Motivo: \${motivo}\`,
              { tag: chaveAtraso, vibrate: [200, 100, 200] }
            );
          }
        }

        // GATILHO 2: hora de sair caminhando
        const coordAlvo = pontos.find(p => p.nome === pontoAlvo);
        if (coordAlvo && posicaoUsuarioRadar.lat) {
          const distKm = distancia(posicaoUsuarioRadar.lat, posicaoUsuarioRadar.lon, coordAlvo.lat, coordAlvo.lon);
          const minCaminhada = Math.max(1, Math.ceil((distKm * 1000) / VELOCIDADE_CAMINHADA_METROS_MIN));
          const esperaComAtraso = infoProx.espera; // já inclui o atraso

          if (distKm >= 0.08 && distKm <= 1.8 && esperaComAtraso >= minCaminhada && esperaComAtraso <= minCaminhada + 2) {
            const chaveSaida = \`sair_\${infoProx.horaSaidaOrigem}_\${pontoAlvo}\`;
            if (!jaDisparouAlertaAutomatico(chaveSaida)) {
              marcarAlertaAutomaticoDisparado(chaveSaida);
              enviarNotificacao(
                \`🚶 Hora de sair para o ponto!\`,
                \`O ônibus passa \${preposicaoPonto(pontoAlvo)} \${pontoAlvo} em \${esperaComAtraso} min (sua caminhada leva ~\${minCaminhada} min).\`,
                { tag: chaveSaida, vibrate: [200, 100, 200] }
              );
            }
          }
        }
      }

      // GATILHO 3: alerta crítico da comunidade
      const alc = estadoAoVivoGlobal.alertaComunidade;
      if (alc && ALERTAS_CRITICOS.includes(alc.codigo)) {
        const chaveCom = \`com_\${alc.horaEnvio}_\${alc.codigo}\`;
        if (!jaDisparouAlertaAutomatico(chaveCom)) {
          marcarAlertaAutomaticoDisparado(chaveCom);
          enviarNotificacao(\`🚨 Aviso Importante na Rota (\${alc.horaEnvio})\`, alc.texto, { tag: chaveCom });
        }
      }

      // GATILHO 4: último ônibus que VOLTA do campus (só se o aluno não estiver na Moradia)
      const ultimoVolta = [...horarios].reverse().find(h => h[1].includes("Escola"));
      if (ultimoVolta && !posicaoUsuarioRadar.naMoradia) {
        const faltaUlt = minutos(ultimoVolta[0]) - atualMin;
        if (faltaUlt > 0 && faltaUlt <= 20) {
          const chaveUlt = \`ultimo_onibus_\${ultimoVolta[0]}\`;
          if (!jaDisparouAlertaAutomatico(chaveUlt)) {
            marcarAlertaAutomaticoDisparado(chaveUlt);
            enviarNotificacao(
              \`⚠️ Último ônibus saindo em \${faltaUlt} min!\`,
              \`Você ainda está fora da Moradia. O último ônibus de volta (\${ultimoVolta[0]}, \${ultimoVolta[1]}) sairá em breve!\`,
              { tag: chaveUlt, vibrate: [300, 100, 300] }
            );
          }
        }
      }

      // BARRA AO VIVO: quando faltam <= 22 min para o ônibus chegar no ponto do aluno
      if (infoProx && infoProx.espera <= 22) {
        chaveLiveAtual = \`\${obterChaveDataHoje()}_\${infoProx.horaSaidaOrigem}_\${pontoAlvo}\`;
        if (localStorage.getItem('morabus_live_dispensada') === chaveLiveAtual) return; // usuário fechou a barra

        const esperaReal = infoProx.espera; // já inclui atraso
        const horaRealChegada = infoProx.horaChegadaReal;
        const progressoPct = Math.min(100, Math.max(5, Math.round(((22 - esperaReal) / 22) * 100)));

        const ev = estadoAoVivoGlobal;
        const textoLocLimpo = ev.textoStatusLoc ? ev.textoStatusLoc.replace(/<[^>]+>/g, '') : '';
        const ondeEstaTexto = (ev.ativo && ev.horarioViagem === infoProx.horaSaidaOrigem && textoLocLimpo)
          ? \`\${textoLocLimpo}\${ev.velocidadeKmh > 0 ? \` (\${ev.velocidadeKmh} km/h)\` : ''}\`
          : \`🕒 Saída \${infoProx.origem}: \${infoProx.horaSaidaOrigem}\`;

        const tagAtrasoTitulo = atrasoDaViagem >= 2 ? \` (+\${atrasoDaViagem} min atraso)\` : \`\`;
        const tituloLive = esperaReal === 0
          ? \`🚌 Chegando agora \${preposicaoPonto(pontoAlvo)} \${pontoAlvo}!\`
          : \`🚌 Chega em \${esperaReal} min • \${pontoAlvo}\${tagAtrasoTitulo}\`;

        enviarNotificacao(tituloLive, \`Chegada: \${horaRealChegada} • \${ondeEstaTexto}\\n\${montarBarraProgressoVisual(progressoPct)}\`, {
          tag: TAG_LIVE,
          renotify: false,
          silent: true,
          requireInteraction: true,
          actions: [
            { action: 'abrir', title: '🗺️ Ver Mapa' },
            { action: 'parar_live', title: '✖️ Fechar barra' }
          ]
        });
      } else {
        chaveLiveAtual = null;
        fecharNotificacaoAoVivo();
      }
    }`;

const replGatilhos = `    // Avalia e dispara APENAS notificações pontuais nos momentos solicitados:
    // 1) Quando o ônibus atrasar (uma única vez)
    // 2) Quando faltar 5 minutos para o ônibus chegar no ponto do aluno (uma única vez)
    // 3) Se for o último ônibus de retorno e o aluno ainda estiver fora da Moradia
    function avaliarGatilhosInteligentesEBarraAoVivo() {
      if (localStorage.getItem('morabus_radar_permanente') !== 'ativo') return;
      if (!posicaoUsuarioRadar.naRegiaoUFMGMoradia) return;

      const atualMin = minutosAgora();
      const horarios = horariosHoje();
      if (horarios.length === 0) return;

      // Ponto-alvo: favorito ou, se não houver, o mais próximo do GPS
      let pontoAlvo = pontoFavorito;
      if (!pontoAlvo && posicaoUsuarioRadar.lat) {
        const mp = pontoMaisProximo(posicaoUsuarioRadar.lat, posicaoUsuarioRadar.lon);
        if (mp.ponto) pontoAlvo = mp.ponto.nome;
      }
      if (!pontoAlvo) return;

      const infoProx = buscarProximoNoPonto(pontoAlvo, false);

      if (infoProx) {
        const atrasoDaViagem = infoProx.atraso || 0;

        // REGRA 1: Notifica se o ônibus atrasar (dispara UMA ÚNICA VEZ por viagem)
        if (atrasoDaViagem >= 3 && infoProx.espera <= 40) {
          const chaveAtraso = \`atraso_\${obterChaveDataHoje()}_\${infoProx.horaSaidaOrigem}_\${pontoAlvo}_\${atrasoDaViagem}\`;
          if (!jaDisparouAlertaAutomatico(chaveAtraso)) {
            marcarAlertaAutomaticoDisparado(chaveAtraso);
            const novaHora = infoProx.horaChegadaReal;
            const motivo = diagnosticarCausaAtraso(atrasoDaViagem, estadoAoVivoGlobal.velocidadeKmh);
            enviarNotificacao(
              \`⚠️ Ônibus das \${infoProx.horaSaidaOrigem} atrasado (+\${atrasoDaViagem} min)\`,
              \`Previsão \${preposicaoPonto(pontoAlvo)} \${pontoAlvo} mudou para \${novaHora}. Motivo: \${motivo}\`,
              { tag: chaveAtraso, vibrate: [200, 100, 200] }
            );
          }
        }

        // REGRA 2: Notifica quando estiver faltando 5 min para o ônibus chegar (dispara UMA ÚNICA VEZ)
        if (infoProx.espera <= 5 && infoProx.espera >= 1) {
          const chave5min = \`chegando_5min_\${obterChaveDataHoje()}_\${infoProx.horaSaidaOrigem}_\${pontoAlvo}\`;
          if (!jaDisparouAlertaAutomatico(chave5min)) {
            marcarAlertaAutomaticoDisparado(chave5min);
            const ev = estadoAoVivoGlobal;
            const textoLoc = (ev.ativo && ev.horarioViagem === infoProx.horaSaidaOrigem && ev.textoStatusLoc)
              ? \` (\${ev.textoStatusLoc.replace(/<[^>]+>/g, '')})\`
              : '';
            enviarNotificacao(
              \`🚌 Ônibus chegando em ~5 min!\`,
              \`O ônibus das \${infoProx.horaSaidaOrigem} está a caminho \${preposicaoDePonto(pontoAlvo)} \${pontoAlvo}\${textoLoc}. Chegada prevista: \${infoProx.horaChegadaReal}.\`,
              { tag: chave5min, vibrate: [300, 150, 300] }
            );
          }
        }
      }

      // REGRA 3: Alerta crítico da comunidade (acidente/defeito grave)
      const alc = estadoAoVivoGlobal.alertaComunidade;
      if (alc && ALERTAS_CRITICOS.includes(alc.codigo)) {
        const chaveCom = \`com_\${obterChaveDataHoje()}_\${alc.horaEnvio}_\${alc.codigo}\`;
        if (!jaDisparouAlertaAutomatico(chaveCom)) {
          marcarAlertaAutomaticoDisparado(chaveCom);
          enviarNotificacao(\`🚨 Aviso Importante na Rota (\${alc.horaEnvio})\`, alc.texto, { tag: chaveCom });
        }
      }

      // REGRA 4: Último ônibus de volta do campus (se o aluno ainda estiver fora da Moradia)
      const ultimoVolta = [...horarios].reverse().find(h => h[1].includes("Escola"));
      if (ultimoVolta && !posicaoUsuarioRadar.naMoradia) {
        const faltaUlt = minutos(ultimoVolta[0]) - atualMin;
        if (faltaUlt > 0 && faltaUlt <= 15) {
          const chaveUlt = \`ultimo_onibus_\${obterChaveDataHoje()}_\${ultimoVolta[0]}\`;
          if (!jaDisparouAlertaAutomatico(chaveUlt)) {
            marcarAlertaAutomaticoDisparado(chaveUlt);
            enviarNotificacao(
              \`⚠️ Último ônibus saindo em \${faltaUlt} min!\`,
              \`Você está fora da Moradia. O último ônibus de volta (\${ultimoVolta[0]}, \${ultimoVolta[1]}) sairá em breve!\`,
              { tag: chaveUlt, vibrate: [300, 100, 300] }
            );
          }
        }
      }
    }`;

html = normalizeAndReplace(html, targetGatilhos, replGatilhos);

fs.writeFileSync(indexPath, html, 'utf8');
console.log('Notifications successfully fixed to be punctual (delayed & 5 min before arrival only)!');
