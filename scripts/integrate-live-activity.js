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

// 1. Adicionar funções auxiliares para o plugin nativo MorabusLiveActivity e atualizar fecharNotificacaoAoVivo
const targetFecharNotif = `    async function fecharNotificacaoAoVivo() {
      const localNotif = obterPluginCapacitor('LocalNotifications');
      if (ehAppNativo() && localNotif) {
        try { await localNotif.cancel({ notifications: [{ id: 999001 }] }); } catch (e) {}
      }
      if (!('serviceWorker' in navigator)) return;
      try {
        const reg = await navigator.serviceWorker.ready;
        (await reg.getNotifications({ tag: TAG_LIVE })).forEach(n => n.close());
      } catch (e) {}
    }`;

const replFecharNotif = `    function obterPluginLiveActivity() {
      return window.Capacitor?.Plugins?.MorabusLiveActivity || null;
    }

    async function atualizarLiveActivityNativa(dados) {
      const plugin = obterPluginLiveActivity();
      if (!plugin || !plugin.updateLiveActivity) return;
      try {
        await plugin.updateLiveActivity(dados);
      } catch (e) {
        console.warn('Falha Live Activity nativa:', e);
      }
    }

    async function encerrarLiveActivityNativa() {
      const plugin = obterPluginLiveActivity();
      if (!plugin || !plugin.stopLiveActivity) return;
      try {
        await plugin.stopLiveActivity();
      } catch (e) {}
    }

    async function fecharNotificacaoAoVivo() {
      await encerrarLiveActivityNativa();
      const localNotif = obterPluginCapacitor('LocalNotifications');
      if (ehAppNativo() && localNotif) {
        try { await localNotif.cancel({ notifications: [{ id: 999001 }] }); } catch (e) {}
      }
      if (!('serviceWorker' in navigator)) return;
      try {
        const reg = await navigator.serviceWorker.ready;
        (await reg.getNotifications({ tag: TAG_LIVE })).forEach(n => n.close());
      } catch (e) {}
    }`;

html = normalizeAndReplace(html, targetFecharNotif, replFecharNotif);

// 2. Adicionar o gatilho da Live Activity no fim de avaliarGatilhosInteligentesEBarraAoVivo
const targetFimGatilhos = `      // REGRA 4: Último ônibus de volta do campus (se o aluno ainda estiver fora da Moradia)
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

const replFimGatilhos = `      // REGRA 4: Último ônibus de volta do campus (se o aluno ainda estiver fora da Moradia)
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

      // INTEGRAÇÃO NATIVA: NOW BAR (SAMSUNG ONE UI 7/8.5/9) & NOTIFICAÇÕES AO VIVO (MOTOROLA / XIAOMI / ANDROID 17)
      // Mantém um único bloco dinâmico em tempo real sem vibrar ou tocar som
      if (infoProx && infoProx.espera <= 25) {
        const esperaReal = infoProx.espera;
        const horaRealChegada = infoProx.horaChegadaReal;
        const progressoPct = Math.min(100, Math.max(5, Math.round(((25 - esperaReal) / 25) * 100)));

        const ev = estadoAoVivoGlobal;
        const textoLocLimpo = (ev.ativo && ev.horarioViagem === infoProx.horaSaidaOrigem && ev.textoStatusLoc)
          ? ev.textoStatusLoc.replace(/<[^>]+>/g, '')
          : '';

        const ondeEstaTexto = textoLocLimpo
          ? \`\${textoLocLimpo}\${ev.velocidadeKmh > 0 ? \` (\${ev.velocidadeKmh} km/h)\` : ''}\`
          : \`Saída \${infoProx.origem}: \${infoProx.horaSaidaOrigem}\`;

        const atrasoStr = atrasoDaViagem >= 2 ? \` (+\${atrasoDaViagem}m atraso)\` : '';
        const tituloLive = esperaReal === 0
          ? \`🚌 Chegando agora \${preposicaoDePonto(pontoAlvo)} \${pontoAlvo}!\`
          : \`🚌 Chega em ~\${esperaReal} min • \${pontoAlvo}\${atrasoStr}\`;

        const subtextoLive = \`Previsão: \${horaRealChegada} • Linha Campus ⇄ Moradias\`;

        atualizarLiveActivityNativa({
          title: tituloLive,
          message: ondeEstaTexto,
          subtext: subtextoLive,
          progress: progressoPct,
          etaMinutes: esperaReal,
          isDelayed: atrasoDaViagem >= 2,
          delayMinutes: atrasoDaViagem
        });
      } else {
        encerrarLiveActivityNativa();
      }
    }`;

html = normalizeAndReplace(html, targetFimGatilhos, replFimGatilhos);

fs.writeFileSync(indexPath, html, 'utf8');
console.log('Live Activity / Now Bar integration applied to index.html successfully!');
