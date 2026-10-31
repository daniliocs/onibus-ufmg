<div align="center">
  <img src="./Morabus.png" alt="Logo Morabus UFMG" width="130" style="border-radius: 24px;" />
  <h1>🚌 Morabus UFMG</h1>
  <p><b>Aplicativo oficial e colaborativo para os estudantes das Moradias Universitárias da UFMG (Ouro Preto ↔ Campus Pampulha)</b></p>

  <p>
    <a href="https://raw.githubusercontent.com/daniliocs/onibus-ufmg/main/morabus.apk">
      <img src="https://img.shields.io/badge/Download_APK-v3.1.1-006633?style=for-the-badge&logo=android&logoColor=white" alt="Baixar APK v3.1.1" />
    </a>
    <a href="https://daniliocs.github.io/onibus-ufmg/">
      <img src="https://img.shields.io/badge/Versão_Web_(PWA)-Online-2563eb?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Acessar PWA" />
    </a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin Jetpack Compose" />
    <img src="https://img.shields.io/badge/Material_3-Modern_UI-388e3c?style=flat-square" alt="Material Design 3" />
    <img src="https://img.shields.io/badge/Mapas-100%25_Offline_Cache-0284c7?style=flat-square&logo=openstreetmap&logoColor=white" alt="OpenStreetMap Offline" />
    <img src="https://img.shields.io/badge/Backend-Firebase_Realtime-FFA611?style=flat-square&logo=firebase&logoColor=black" alt="Firebase Realtime" />
    <img src="https://img.shields.io/badge/Android-8.0_a_16%2F17_Ready-brightgreen?style=flat-square&logo=android&logoColor=white" alt="Android Compatibility" />
  </p>
</div>

---

## 💡 Sobre o Morabus UFMG

O **Morabus UFMG** foi concebido e desenvolvido para atender os mais de **1.000 estudantes** que residem nas **Moradias Universitárias da UFMG (Moradias 1, 2 e 3)** no bairro Ouro Preto, em Belo Horizonte, facilitando o deslocamento diário de ida e volta para o **Campus Pampulha**.

Além de consultar os horários oficiais do ônibus interno e o tempo restante até a próxima partida, o aplicativo oferece **previsão ponto a ponto**, **acompanhamento no mapa**, **alertas de comunidade em tempo real**, **integração com os Restaurantes Universitários (RU)** e um **atualizador interno over-the-air (OTA)**.

---

## ✨ Principais Funcionalidades

### 🏠 1. Painel Inicial (Início)
- **Saídas Simultâneas:** Cards destacados exibindo em tempo real a próxima saída da **Moradia** e do **Campus**, com contagem regressiva em minutos ou horas.
- **Linha do Tempo Ponto a Ponto:** Estimativa do horário exato de chegada do ônibus em cada faculdade e instituto do campus.
- **Dicas do Restaurante Universitário (RU):** Recomendações inteligentes de almoço e jantar nos RUs Setorial I e II, com indicação precisa do melhor ônibus para embarcar a tempo e avisos diferenciados aos sábados.
- **Card de Avisos em Destaque:** Alertas urgentes ou importantes sobre o itinerário, aulas ou clima exibidos discretamente no topo.

### 🗺️ 2. Mapa Interativo com Cache 100% Offline
- **Mapa da Rota e Paradas:** Visualização de todos os pontos de embarque e desembarque nas Moradias e no Campus Pampulha.
- **Rastreamento em Tempo Real:** Posição estimada e ao vivo do ônibus em trânsito com telemetria compartilhada.
- **Cache de Disco Nativo:** Os blocos do corredor Moradias ↔ Campus (OpenStreetMap) são salvos no armazenamento local do aparelho, permitindo abrir e navegar no mapa mesmo sem sinal de internet ou com 4G fraco.
- **Suporte ao Modo Escuro:** Adaptação de alto contraste para uso noturno sem duplicar o consumo de dados móveis.

### ⏱️ 3. Grade Completa de Horários
- **Filtros Inteligentes:** Alterne entre horários de saída da Moradia, do Campus, paradas intermediárias ou a grade completa.
- **Ocultar Horários Já Passados:** Botão com retenção persistente de preferência para exibir apenas as viagens futuras.
- **Lembretes e Alarmes:** Toque no sininho de qualquer viagem para programar um alerta sonoro e vibratório 10 minutos antes da partida.
- **Simulador de Viagem:** Toque em qualquer viagem do dia para ver a simulação do horário previsto em cada ponto de parada.

### 📢 4. Mural de Avisos e Comunicados
- **Histórico Organizado:** Veja os comunicados emitidos pela FUMP, UFMG, Defesa Civil (chuvas) ou administração das Moradias.
- **Severidade Visual:** Destaques coloridos em verde (normal), amarelo (importante) e vermelho (urgente).
- **Retenção Customizável:** Escolha nas configurações o período de retenção que deseja manter visível (último dia, última semana ou último mês).

### ⚙️ 5. Configurações & Comunidade (Mais)
- **Contador Comunitário Real:** Indicador dinâmico de moradores conectados com o app instalado em todas as versões.
- **Filtros de Notificação:** Escolha quais fontes de avisos deseja receber (FUMP, UFMG, Alertas Climáticos) e o nível mínimo de severidade para disparar notificações no aparelho.
- **Seletor de Tema:** Alterne entre Modo Sistema, Modo Escuro e Modo Claro.
- **📲 Atualizador Remoto Integrado (OTA):** Verificação e download direto de novas versões sem burocracia de lojas de aplicativos.

---

## ⚡ Recursos Nativos do Android

- **Atalhos Rápidos no Launcher (App Shortcuts):** Ao segurar o dedo sobre o ícone do Morabus na tela inicial do celular, você acessa diretamente:
  - 🏠 *Saindo da Moradia*
  - 🎓 *Saindo do Campus*
  - ⏱️ *Grade de Horários*
  - 📢 *Mural de Avisos*
- **Widget de Tela Inicial (AppWidget):** Card elegante para fixar na home screen do Android com o horário da próxima viagem.
- **Serviço de Segundo Plano (Foreground Service):** Notificações persistentes de viagem e suporte a geolocalização precisa.
- **Radar Campus Colaborativo:** Envio de relatos rápidos de passagem ("ônibus acabou de passar") e alertas de trânsito/lotação via Firebase.

---

## 📲 Como Instalar

### Opção A: Aplicativo Android Nativo (.APK) — *Recomendado*
1. Baixe o arquivo **[morabus.apk](https://raw.githubusercontent.com/daniliocs/onibus-ufmg/main/morabus.apk)** diretamente no seu celular.
2. Abra o arquivo baixado e confirme a instalação (se solicitado, autorize a instalação de fontes desconhecidas para o navegador ou gerenciador de arquivos).
3. Todas as atualizações futuras poderão ser feitas diretamente pelo menu **Verificar Atualização** dentro do próprio aplicativo!

### Opção B: Versão Web / PWA (Android e iPhone/iOS)
1. Acesse **[daniliocs.github.io/onibus-ufmg](https://daniliocs.github.io/onibus-ufmg/)** no navegador do seu smartphone.
2. **Android (Chrome):** Toque em *"Adicionar à tela inicial"* ou *"Instalar aplicativo"*.
3. **iPhone (Safari):** Toque no botão Compartilhar (ícone com seta para cima) e escolha *"Adicionar à Tela de Início"*.

---

## 🛠️ Arquitetura e Tecnologias

- **Linguagem & UI:** Kotlin 1.9 + Jetpack Compose + Material Design 3
- **Camada Assíncrona:** Kotlin Coroutines & StateFlow / SharedFlow
- **Banco de Dados em Tempo Real:** Firebase Realtime Database (REST + WebSockets)
- **Mapas & Geodados:** Leaflet.js + OpenStreetMap com `MapTileCacheManager` nativo em disco
- **Instalador OTA:** `FileProvider` e `PackageInstaller` do Android com verificação remota via JSON
- **PWA Web:** HTML5, CSS3, JavaScript Vanilla e Service Workers com cache offline

---

## 💻 Como Compilar o Projeto

### Pré-requisitos:
- **Android SDK** (API 34 / 35 instalada)
- **JDK 17** configurado no `JAVA_HOME`
- **Gradle** (incluído via `gradlew`)

### Passos de compilação:

```bash
# Clone o repositório
git clone https://github.com/daniliocs/onibus-ufmg.git
cd onibus-ufmg

# Acesse a pasta do projeto Android
cd android

# Compile a versão Debug do APK
./gradlew assembleDebug
```

O arquivo gerado estará em `android/app/build/outputs/apk/debug/morabus.apk` e na raiz como `morabus.apk`.

Para instalar diretamente no celular conectado via USB com depuração ativada:

```bash
adb install -r morabus.apk
```

---

## 👥 Comunidade & Autoria

Desenvolvido por **[Danilio Costa](https://github.com/daniliocs)** para e com o apoio da comunidade de moradores das Moradias Universitárias da UFMG.

Projeto livre e de código aberto sob a licença [MIT](LICENSE).
