# 📱 Guia do App Nativo Android — Morabus UFMG

Este projeto agora está preparado para gerar um **aplicativo Android nativo (.apk)** usando **Capacitor**, mantendo **100% de compatibilidade** com o GitHub Pages (PWA) a partir do mesmo arquivo `index.html`.

---

## 🚀 O que muda no App Nativo Android?

1. **GPS em 2º Plano Real (`@capacitor-community/background-geolocation`):**
   - No navegador comum, quando o estudante bloqueia a tela ou abre o WhatsApp, o Android congela o GPS em ~1 minuto.
   - No App Nativo, o rastreio cria um **Serviço de Primeiro Plano (Foreground Service)** do Android: o GPS continua enviando a posição exata para o Firebase mesmo com a tela apagada no bolso!
2. **Notificações Locais e Barra Ao Vivo Nativas (`@capacitor/local-notifications`):**
   - Notificações fixas (`ongoing: true`) na bandeja do Android que não somem sozinhas e vibram mesmo com o app fechado.

---

## 🛠️ Passo a Passo para Gerar e Testar no seu Celular

### 1. Instalar as dependências do Capacitor
Abra o terminal na pasta do projeto (`onibus-ufmg`) e execute:

> **Dica para Windows PowerShell:** Se aparecer erro de *Execution Policy* no `npm`, use `npm.cmd` e `npx.cmd`:

```powershell
npm.cmd install
```

### 2. Criar a pasta nativa Android (só precisa na 1ª vez)
```powershell
npm.cmd run build:www
npx.cmd cap add android
```

### 3. Configurar as Permissões de GPS em 2º Plano no Android
Abra o arquivo `android/app/src/main/AndroidManifest.xml` (criado no passo anterior) e certifique-se de que estas permissões estejam antes da tag `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### 4. Sincronizar alterações do `index.html` com o Android
Sempre que você editar o `index.html`, rode:
```powershell
npm.cmd run android:sync
```

### 5. Rodar no seu celular ou gerar o `.APK`
Você tem duas opções:

- **Opção A (Com Android Studio):**
  ```powershell
  npm.cmd run android:open
  ```
  Conecte seu celular via cabo USB (com *Depuração USB* ativada) ou Wi-Fi e clique no botão **Run (▶️)** no Android Studio.

- **Opção B (Gerar arquivo `.apk` direto pelo terminal):**
  ```powershell
  npm.cmd run android:apk
  ```
  O arquivo instalável estará em:
  `android/app/build/outputs/apk/debug/app-debug.apk`
  Basta enviar esse `.apk` para o seu celular (via WhatsApp/Telegram/USB) e instalar!
