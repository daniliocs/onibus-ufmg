<div align="center">
  <img src="./Morabus.png" alt="Logo Morabus UFMG" width="130" style="border-radius: 24px;" />
  <h1>🚌 Morabus UFMG</h1>
  <p><b>Horários em tempo real, rotas, GPS e alertas do ônibus das Moradias Estudantis da UFMG (Ouro Preto ↔ Campus Pampulha)</b></p>

  <p>
    <a href="https://daniliocs.github.io/onibus-ufmg/">
      <img src="https://img.shields.io/badge/Acessar_Aplicativo-006633?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Acessar App" />
    </a>
  </p>
</div>

---

## 💡 Sobre o Projeto

O **Morabus UFMG** é um aplicativo web progressivo (PWA) focado no mobile, criado para facilitar a rotina dos cerca de 1.000 estudantes que residem nas **Moradias Universitárias da UFMG (Moradias 1, 2 e 3)** no bairro Ouro Preto, em Belo Horizonte.

Com ele, é possível consultar rapidamente quanto tempo falta para o próximo ônibus, estimar o horário exato em que ele passará em cada ponto dentro do Campus Pampulha e encontrar o ponto mais próximo para voltar para casa.

🔗 **Link de acesso:** [https://daniliocs.github.io/onibus-ufmg/](https://daniliocs.github.io/onibus-ufmg/)

---

## ✨ Funcionalidades

- ⏱️ **Próximo Ônibus em Tempo Real:** Contagem regressiva automática com aviso de quantidade de veículos e painel duplo mostrando simultaneamente a próxima saída da **Moradia** e a próxima saída do **Campus (Escola de Música)**.
- 📍 **Chegada Estimada Ponto a Ponto:** Linha do tempo com a previsão de passagem em cada parada (Educação Física, Odonto, Veterinária, Farmácia, RU 1, RU 2, Engenharia, IGC, FACE, Reitoria, CAD 1, FaE, Belas Artes e Escola de Música).
- 🔎 **Simulador de Viagem:** Toque em qualquer horário da lista do dia para visualizar na linha do tempo a previsão de chegada daquele ônibus específico em cada unidade.
- 🔔 **Alertas de Saída (Notificações):** Ative o sininho ao lado de qualquer horário para receber uma notificação no celular **10 minutos antes** do ônibus sair.
- 🧭 **"Como volto pra casa?" (GPS + Rota a pé):** Detecta sua localização atual dentro da UFMG, indica qual é o ponto de retorno mais próximo, traça o caminho a pé no mapa e informa em quantos minutos o próximo ônibus passará lá.
- 🚌 **Posição Estimada no Mapa:** Acompanhe no mapa interativo a estimativa de onde o ônibus em andamento está no momento.
- 📴 **100% Funcional Offline (PWA):** Pode ser instalado na tela inicial do celular como um aplicativo nativo e funciona mesmo se você estiver sem internet ou sem dados móveis no ponto.
- 🌙 **Modo Escuro Automático:** Interface adaptada automaticamente ao tema claro ou escuro do seu celular, economizando bateria.
- 📅 **Calendário Inteligente:** Alternância automática entre grades de dias úteis, sábados, domingos e feriados de Belo Horizonte.

---

## 📲 Como instalar no celular

1. Acesse **[daniliocs.github.io/onibus-ufmg](https://daniliocs.github.io/onibus-ufmg/)** pelo navegador do celular.
2. **No Android (Chrome):** Toque no botão **"📥 Instalar App"** no topo da página (ou vá nos três pontinhos do navegador e selecione *"Adicionar à tela inicial"* / *"Instalar aplicativo"*).
3. **No iPhone (Safari):** Toque no ícone de **Compartilhar** (quadrado com seta para cima na barra inferior) e selecione **"Adicionar à Tela de Início"**.

---

## 🛠️ Tecnologias Utilizadas

- **HTML5, CSS3 & JavaScript (Vanilla JS)** — Leve, rápido e sem dependências pesadas.
- **PWA (Service Workers + Web App Manifest)** — Estratégia *Network-First* para atualização automática de horários com suporte total a uso offline.
- **Leaflet.js + OpenStreetMap** — Renderização de mapas interativos.
- **OSRM API (Open Source Routing Machine)** — Cálculo de rotas a pé até o ponto de ônibus mais próximo.
- **Web Notifications & Geolocation API** — Alertas locais e cálculo de distância geográfica (Fórmula de Haversine).

---

<div align="center">
  Desenvolvido por <a href="https://github.com/daniliocs">Danilio Costa</a> para a comunidade das Moradias Estudantis da UFMG.
</div>
