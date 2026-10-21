const fs = require('fs');
const path = require('path');

const indexPath = path.join(__dirname, '..', 'index.html');
let content = fs.readFileSync(indexPath, 'utf8');

// 1. preposicaoDePonto
const target1 = `    function preposicaoPonto(nome) {
      const masculinos = ["RU 1", "RU 2", "CAD 1", "IGC", "Ponto final"];
      return masculinos.includes(nome) ? "no" : "na";
    }`;

const repl1 = `    function preposicaoPonto(nome) {
      if (!nome) return "no";
      if (nome.startsWith("Moradia 2 e 3") || nome.startsWith("Moradias")) return "nas";
      const masculinos = ["RU 1", "RU 2", "CAD 1", "IGC", "Ponto final"];
      return masculinos.includes(nome) ? "no" : "na";
    }

    function preposicaoDePonto(nome) {
      if (!nome) return "do";
      if (nome.startsWith("Moradia 2 e 3") || nome.startsWith("Moradias")) return "das";
      const masculinos = ["RU 1", "RU 2", "CAD 1", "IGC", "Ponto final"];
      return masculinos.includes(nome) ? "do" : "da";
    }`;

// normaliza CRLF/LF temporariamente para match confiável
function replaceNormalized(src, target, repl) {
  const normSrc = src.replace(/\r\n/g, '\n');
  const normTarget = target.replace(/\r\n/g, '\n');
  const normRepl = repl.replace(/\r\n/g, '\n');
  if (!normSrc.includes(normTarget)) {
    throw new Error('Target not found:\n' + normTarget.slice(0, 80));
  }
  const result = normSrc.replace(normTarget, normRepl);
  return result.replace(/\n/g, '\r\n');
}

content = replaceNormalized(content, target1, repl1);

// 2. textoStatusLoc quando próximo de ponto
const target2 = `textoStatusLoc = \`📍 Próximo \${preposicaoPonto(prog.pontoProximo.nome)} <b>\${esc(prog.pontoProximo.nome)}</b>\${esc(txtVia)}\`;
        if (distMetros > 30 && prog.proximoPontoRota && prog.proximoPontoRota !== prog.pontoProximo.nome) {
          textoStatusLoc += \` → seguindo p/ <b>\${esc(prog.proximoPontoRota)}</b>\`;
        }`;

const repl2 = `textoStatusLoc = \`📍 Passando próximo \${preposicaoDePonto(prog.pontoProximo.nome)} <b>\${esc(prog.pontoProximo.nome)}</b>\${esc(txtVia)}\`;
        if (distMetros > 30 && prog.proximoPontoRota && prog.proximoPontoRota !== prog.pontoProximo.nome) {
          textoStatusLoc += \`, seguindo para <b>\${esc(prog.proximoPontoRota)}</b>\`;
        }`;

content = replaceNormalized(content, target2, repl2);

// 3. Hero card atraso
const target3 = `if (proximo.quando === "hoje") {
          const ehOUltimo = (proximo.hora === horarios[horarios.length - 1][0]);
          const etiquetaAviso = ehOUltimo ? \`<div class="etiqueta-ultimo">⚠️ ATENÇÃO: ÚLTIMO ÔNIBUS DE HOJE</div><br>\` : "";
          elProximo.innerHTML = etiquetaAviso + proximo.hora;
          elTempo.textContent = formatarTempo(proximo.min - atualMin);
        }`;

const repl3 = `if (proximo.quando === "hoje") {
          const ehOUltimo = (proximo.hora === horarios[horarios.length - 1][0]);
          const etiquetaAviso = ehOUltimo ? \`<div class="etiqueta-ultimo">⚠️ ATENÇÃO: ÚLTIMO ÔNIBUS DE HOJE</div><br>\` : "";
          const atrHero = obterAtrasoPropagado(proximo.hora);
          if (atrHero >= 2) {
            const minAtrasado = proximo.min + atrHero;
            elProximo.innerHTML = etiquetaAviso + \`<span class="hora-riscada" style="font-size:26px; vertical-align:middle;">\${proximo.hora}</span> <span class="texto-atraso">\${converterMinutosParaHHMM(minAtrasado)}</span>\`;
            elTempo.innerHTML = \`<span class="texto-atraso">⚠️ Faltam \${formatarHoraMinuto(minAtrasado - atualMin)} (+\${atrHero} min atrasado)</span>\`;
          } else {
            elProximo.innerHTML = etiquetaAviso + proximo.hora;
            elTempo.textContent = formatarTempo(proximo.min - atualMin);
          }
        }`;

content = replaceNormalized(content, target3, repl3);

fs.writeFileSync(indexPath, content, 'utf8');
console.log('Successfully applied all improvements to index.html!');
