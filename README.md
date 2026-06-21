# 🇳🇱 NederLeer — Impara l'olandese

App Android per imparare l'olandese partendo dall'italiano (con supporto anche
all'inglese), nello stile immersivo di **Rosetta Stone** e con la
gamification di **Duolingo**. Vocabolario strutturato dal livello **A1 al C1**
con progressione passo passo.

> Interfaccia in italiano · parole in olandese · traduzioni in italiano e inglese.

---

## ✨ Caratteristiche

- **Mappa del corso a percorso** (stile Duolingo): livelli → unità tematiche → lezioni che si sbloccano una dopo l'altra.
- **Esercizi misti generati automaticamente** da ogni parola:
  - 🖼️ **Immagine → parola** (stile Rosetta Stone, con emoji come immagini)
  - 🔊 **Ascolto** — riconosci la parola pronunciata
  - 🔁 **Traduzione** a scelta multipla (NL→IT e IT→NL)
  - ⌨️ **Scrittura** — digita la parola in olandese
  - 🧩 **Abbinamento** di coppie
- **Pronuncia reale in olandese** tramite il motore Text-To-Speech nativo di Android (`nl-NL`), anche a velocità ridotta 🐢.
- **Gamification**: punti XP, serie giornaliera (streak), stelle per lezione, ranghi.
- **Progressi salvati** localmente (DataStore), funziona **offline**.
- **Tema chiaro/scuro** Material 3.

## 📚 Contenuti

| Livello | Tema |
|--------|------|
| **A1** | Saluti, persone e famiglia, numeri e colori, cibo, casa, tempo e giorni |
| **A2** | Verbi quotidiani, corpo e salute, vestiti, città e trasporti, lavoro, tempo libero |
| **B1** | Emozioni, opinioni e comunicazione, verbi utili, tempo e quantità |
| **B2** | Società e attualità, lavoro e affari, natura e ambiente, carattere e relazioni |
| **C1** | Idee astratte, modi di dire idiomatici, linguaggio accademico |

Attualmente **~390 voci** in **38 lezioni**. Il contenuto è in semplici file
JSON (`app/src/main/assets/curriculum/`) ed è facilissimo da ampliare: ogni
parola in più genera automaticamente nuovi esercizi.

---

## 📥 Come ottenere l'APK

L'APK viene compilato automaticamente da **GitHub Actions** a ogni push (in
questo ambiente i server Android di Google non sono raggiungibili, quindi la
build avviene su CI).

1. Vai nella scheda **Actions** del repository → workflow **“Build Android APK”**.
2. Apri l'esecuzione più recente e scarica l'artifact **`NederLeer-APK`**, **oppure**
3. Scarica direttamente dalla **Release** `latest-apk` il file `NederLeer.apk`.

Puoi anche avviare la build a mano da **Actions → Build Android APK → Run workflow**.

### Installazione sul telefono
1. Copia `NederLeer.apk` sul dispositivo Android.
2. Aprilo e consenti l'installazione da “origini sconosciute”.
3. Al primo avvio, se la voce olandese non si sente, installa/abilita i dati TTS
   olandesi: *Impostazioni → Sistema → Lingue → Sintesi vocale → installa olandese (Nederlands)*.

> È un APK **debug**, già firmato con la chiave di debug: installabile subito,
> senza configurare alcuna firma.

---

## 🛠️ Compilare in locale (Android Studio)

Requisiti: Android Studio (Koala o successivo), JDK 17, Android SDK 34.

```bash
git clone <repo>
cd Dutch---APP
./gradlew assembleDebug
# APK in: app/build/outputs/apk/debug/app-debug.apk
```

Oppure apri la cartella in Android Studio e premi ▶️ Run.

## 🧱 Stack tecnico

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Navigation Compose**, **DataStore** (progressi), **kotlinx.serialization** (curriculum)
- **TextToSpeech** nativo per la pronuncia olandese
- AGP 8.6 · Gradle 8.14 · minSdk 24 · targetSdk 34

## 📂 Struttura

```
app/src/main/
├── java/com/dutchapp/learn/
│   ├── MainActivity.kt          # navigazione + bottom bar
│   ├── data/                    # modelli, repository, generatore esercizi, progressi
│   ├── audio/TtsManager.kt      # pronuncia olandese (TTS)
│   ├── viewmodel/               # stato app + logica di sblocco
│   └── ui/                      # tema, componenti, schermate
└── assets/curriculum/*.json     # il vocabolario A1→C1 (modificabile)
```

## ➕ Aggiungere parole o lezioni

Apri il file del livello (es. `a1.json`) e aggiungi una voce a una lezione:

```json
{ "nl": "de hond", "it": "il cane", "en": "dog", "emoji": "🐶", "pos": "n" }
```

Campi: `nl` (olandese, obbligatorio), `it` (italiano, obbligatorio), `en`,
`emoji`, `exampleNl`, `exampleIt`, `pos`. Ricompila ed è subito nel corso.

---

*Nota didattica:* le immagini sono rese con **emoji** (scelta voluta: niente
download di foto, app leggera e offline). La pronuncia usa la voce olandese del
dispositivo, quindi è una voce reale e non un file audio incluso.
