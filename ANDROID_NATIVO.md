# 📱 Guia de Desenvolvimento do App Nativo Android — Morabus UFMG

Este documento descreve a arquitetura nativa do aplicativo **Morabus UFMG** em **Kotlin + Jetpack Compose**, sua estrutura de diretórios e o fluxo de compilação.

---

## 🏛️ Arquitetura do App Nativo

O Morabus UFMG foi arquitetado para aliar a reatividade e beleza do **Jetpack Compose (Material 3)** com o baixo consumo de dados e confiabilidade do motor de horários offline:

```
android/app/src/main/java/br/ufmg/morabus/app/
├── MainActivity.kt                      # Ponto de entrada e tratamento de App Shortcuts
├── data/
│   ├── AppUpdateManager.kt              # Gerenciador OTA de download e instalação de APKs
│   ├── CampusRadarEngine.kt             # Lógica de telemetria e avisos de trânsito/chuva
│   ├── FirebaseRealtimeRepository.kt    # Integração REST com Firebase RTDB (installs, alertas)
│   ├── MapTileCacheManager.kt           # Cache nativo em disco de blocos OpenStreetMap
│   ├── RuEngine.kt                      # Motor de horários e recomendações dos RUs Setorial I e II
│   ├── ScheduleEngine.kt                # Cálculo de partidas, tempo restante e pontos
│   └── UserPreferencesRepository.kt     # Persistência via SharedPreferences com StateFlow
├── notifications/
│   ├── LiveTripModels.kt                # Modelos de dados de viagens ativas
│   ├── LiveTripNotifier.kt              # Notificações em primeiro plano (Foreground)
│   ├── LiveTripService.kt               # Serviço contínuo de rastreio de viagem
│   └── CampusRadarNotifier.kt           # Notificações de comunicados urgentes
├── reminders/
│   └── TripReminderReceiver.kt          # BroadcastReceiver de alarmes agendados
├── ui/
│   ├── MorabusMainApp.kt                # Scaffold principal, barra de navegação de 5 abas
│   ├── components/
│   │   ├── LiveBusCard.kt               # Cards duplos de saída simultânea (Moradia/Campus)
│   │   ├── RuComponents.kt              # Card e BottomSheet modal dos Restaurantes Universitários
│   │   ├── CampusRadarBanner.kt         # Banner de avisos urgentes na Home
│   │   └── FavoriteStopBottomSheet.kt   # Seleção rápida do ponto favorito
│   ├── screens/
│   │   ├── HomeScreen.kt                # Aba 1: Início (status, cards, RU, timeline)
│   │   ├── MapScreen.kt                 # Aba 2: Mapa interativo Leaflet com cache offline
│   │   ├── SchedulesScreen.kt           # Aba 3: Grade completa com filtros persistentes
│   │   ├── AvisosScreen.kt              # Aba 4: Mural com histórico de comunicados
│   │   └── MoreScreen.kt                # Aba 5: Configurações, comunidade e atualizador
│   └── theme/
│       ├── MorabusIcons.kt              # Coleção de ícones vetoriais Material 3
│       └── Theme.kt                     # Cores, tipografia e suporte Claro/Escuro
└── widget/
    └── MorabusWidgetProvider.kt         # AppWidget para tela inicial do Android
```

---

## 🚀 Como Compilar e Gerar o APK

### 1. Compilação direta via terminal:
Na raiz do projeto, acesse a pasta `android` e execute:

```powershell
cd android
./gradlew assembleDebug
```

O arquivo compilado é nomeado automaticamente como **`morabus.apk`** e gerado em:
- `android/app/build/outputs/apk/debug/morabus.apk`

### 2. Copiar para a raiz para distribuição:
```powershell
Copy-Item android/app/build/outputs/apk/debug/morabus.apk ../morabus.apk -Force
```

### 3. Instalar no celular conectado via cabo USB:
Certifique-se de que a **Depuração USB** está ativada no celular:

```powershell
adb install -r ../morabus.apk
```

---

## 📲 Sistema de Atualização Remota (In-App OTA)

O aplicativo conta com verificação interna de novas versões sem depender da Google Play Store:

1. **Metadados:** Armazenados no Firebase (`/app_version.json`) e no GitHub (`version.json`).
2. **Download:** O `AppUpdateManager` baixa o `.apk` com acompanhamento de progresso em tempo real.
3. **Instalação:** Invoca a `ACTION_VIEW` com `FileProvider` (`br.ufmg.morabus.app.fileprovider`), solicitando autorização nativa do usuário para concluir a atualização.
