# Helgi Remote

Applicazione Android per la gestione e il controllo remoto del dispositivo tramite server integrato, servizio persistente in background e privilegi di **Amministratore Dispositivo** (*Device Administrator*).

---

## Indice

1. [Panoramica e Funzionalità](#-panoramica-e-funzionalità)
2. [Architettura del Progetto](#-architettura-del-progetto)
3. [Requisiti e Versioni Consigliate](#-requisiti-e-versioni-consigliate)
4. [Struttura del Repository](#-struttura-del-repository)
5. [Guida all'Installazione](#-guida-allinstallazione)
   - [Opzione 1: Installazione rapida tramite APK precompilato (`helgi-remote/`)](#opzione-1-installazione-rapida-tramite-apk-precompilato-helgi-remote)
   - [Opzione 2: Compilazione dai sorgenti (`helgi-fixed/`)](#opzione-2-compilazione-dai-sorgenti-helgi-fixed)
6. [Configurazione Post-Installazione su Android](#-configurazione-post-installazione-su-android)
7. [Guida all'Utilizzo](#-guida-allutilizzo)
8. [Risoluzione dei Problemi (Troubleshooting)](#-risoluzione-dei-problemi-troubleshooting)
9. [Sicurezza e Avvertenze Legali](#-sicurezza-e-avvertenze-legali)

---

## Panoramica e Funzionalità

**Helgi Remote** (`com.helgi.remote`) trasforma un dispositivo Android in un nodo gestibile da remoto tramite rete locale (Wi-Fi/LAN) o VPN privata. L'app è progettata per avviarsi automaticamente con il sistema e mantenere attivo un server di controllo senza richiedere interazioni continue sullo schermo del telefono.

### Caratteristiche principali
* **Server di Controllo Integrato (`ControlServer`):** gestisce le richieste in ingresso e l'esecuzione dei comandi remoti sul dispositivo.
* **Servizio Persistente (`RemoteService`):** mantiene operativo il server di controllo in background/primo piano, proteggendolo dalla chiusura automatica da parte del sistema operativo.
* **Amministrazione Dispositivo (`AdminReceiver`):** integra le API ufficiali *Android Device Administration* (`device_admin.xml`) per consentire operazioni privilegiate di gestione e sicurezza sul terminale.
* **Avvio Automatico al Boot (`BootReceiver`):** intercetta l'evento di sistema `BOOT_COMPLETED` per riavviare automaticamente il servizio subito dopo l'accensione o il riavvio dello smartphone.

---

## Architettura del Progetto

Il codice sorgente presente in `helgi-fixed/app/src/main/` è organizzato nei seguenti moduli principali:

| File / Componente | Percorso | Ruolo |
| :--- | :--- | :--- |
| **`MainActivity.java`** | `java/com/helgi/remote/` | Activity iniziale per l'avvio manuale del servizio e l'inizializzazione dell'app. |
| **`RemoteService.java`** | `java/com/helgi/remote/` | `Service` Android che gestisce il ciclo di vita del server remoto in background/foreground. |
| **`ControlServer.java`** | `java/com/helgi/remote/` | Nucleo logico dell'applicazione: apre il socket/server di ascolto, analizza i comandi ricevuti ed esegue le relative azioni. |
| **`AdminReceiver.java`** | `java/com/helgi/remote/` | Estende `DeviceAdminReceiver` per gestire l'attivazione e i criteri di amministrazione del dispositivo. |
| **`BootReceiver.java`** | `java/com/helgi/remote/` | `BroadcastReceiver` che avvia `RemoteService` automaticamente al termine dell'avvio di Android. |
| **`device_admin.xml`** | `res/xml/` | Dichiara le policy richieste dall'app come Amministratore Dispositivo. |
| **`ic_notification.xml`** | `res/drawable/` | Icona vettoriale utilizzata per la notifica persistente del servizio in primo piano. |
| **`AndroidManifest.xml`** | `src/main/` | Definisce permessi, ricevitori (`AdminReceiver`, `BootReceiver`), `RemoteService` e `MainActivity`. |

---

## Requisiti e Versioni Consigliate

Per garantire la massima stabilità del servizio in background, del `BootReceiver` e dei privilegi di amministrazione, fare riferimento alle seguenti specifiche di versione:

### Dispositivo Android (Target Runtime)
* **Versione Minima (`minSdk`):** **Android 8.0 Oreo (API 26)** — richiesta per la gestione moderna dei `ForegroundService` e dei canali di notifica (`NotificationChannel`).
* **Versione Consigliata (`targetSdk`):** **Android 13 (API 33) – Android 14 (API 34)** *(compatibile anche con Android 15 / API 35)*.
* **Note specifiche per versione Android:**
  * **Android 10+ (API 29+):** l'avvio di servizi/activity dal background dopo il boot richiede che l'app sia avviata almeno una volta manualmente dall'utente dopo l'installazione.
  * **Android 13+ (API 33+):** è necessario concedere esplicitamente il permesso `POST_NOTIFICATIONS` affinché la notifica di stato di `RemoteService` sia visibile e il sistema non termini il processo.
  * **Android 14+ (API 34+):** richiede la dichiarazione esplicita del `foregroundServiceType` nel `AndroidManifest.xml` (es. `connectedDevice` / `dataSync` / `specialUse`).

### Ambiente di Sviluppo (per compilare dai sorgenti)
* **JDK:** **Java 17** (LTS) — obbligatorio per Android Gradle Plugin 8.x.
* **Android Studio:** **Android Studio Iguana (2023.2.1)** o **Ladybug (2024.2.1)** o versioni successive.
* **Android Gradle Plugin (AGP):** `8.2.2` – `8.6.0`
* **Gradle:** `8.4` – `8.9`
* **Android SDK Build-Tools:** `34.0.0` (con `compileSdk 34`)

---

## Struttura del Repository

```text
├── helgi-remote/                                      # Cartella con il file .apk già compilato e pronto all'uso
│   └── app-debug.apk (o helgi-remote.apk)             # Pacchetto installabile direttamente su Android
├── helgi-fixed/                                       # Codice sorgente completo del progetto Android
│   └── app/
│       └── src/
│           └── main/
│               ├── AndroidManifest.xml
│               ├── java/com/helgi/remote/
│               │   ├── AdminReceiver.java
│               │   ├── BootReceiver.java
│               │   ├── ControlServer.java
│               │   ├── MainActivity.java
│               │   └── RemoteService.java
│               └── res/
│                   ├── drawable/ic_notification.xml
│                   ├── values/strings.xml
│                   └── xml/device_admin.xml
└── README.md                                          # Documentazione completa
```

---

## Guida all'Installazione

### Opzione 1: Installazione rapida tramite APK precompilato (`helgi-remote/`)

Se non desideri compilare il progetto da zero, puoi installare direttamente il pacchetto già pronto:

1. Apri la cartella **[`helgi-remote/`](./helgi-remote/)** presente in questo repository.
2. Scarica il file **`.apk`** al suo interno sul tuo dispositivo Android (su GitHub clicca sul file e poi su **Download** / **View raw**).
3. Apri il file `.apk` dal gestore file del telefono (es. *Files di Google* o *Archivio*).
4. Se richiesto dal sistema, tocca **Impostazioni** e attiva l'interruttore **Consenti da questa origine** per autorizzare l'installazione di app esterne al Play Store.
5. Conferma toccando **Installa**.
   > *Nota:* Se compare l'avviso di **Google Play Protect** perché l'APK è firmato con una chiave di sviluppo/personale, tocca **Ulteriori dettagli → Installa comunque**.

### Opzione 2: Compilazione dai sorgenti (`helgi-fixed/`)

1. Clona il repository o scarica l'archivio sul tuo computer.
2. Apri **Android Studio**, seleziona **Open** e scegli la cartella **`helgi-fixed/`**.
3. Attendi il completamento della sincronizzazione di Gradle (verifica che nelle impostazioni sia selezionato **JDK 17**).
4. Per generare l'APK da terminale nella cartella `helgi-fixed/`:
   * **Linux / macOS:**
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```
   * **Windows:**
     ```bat
     gradlew.bat assembleDebug
     ```
5. L'APK compilato verrà salvato in:
   `helgi-fixed/app/build/outputs/apk/debug/app-debug.apk`

---

## Configurazione Post-Installazione su Android

Dopo aver installato l'APK, esegui questi **4 passaggi** sul dispositivo Android affinché l'app funzioni senza interruzioni:

### 1. Primo avvio manuale
Apri **Helgi Remote** dal drawer delle applicazioni almeno una volta. Su Android, un'app appena installata rimane in stato *Stopped* e non riceve l'evento di avvio (`BOOT_COMPLETED`) finché non viene avviata manualmente dall'utente.

### 2. Attivazione dei privilegi di Amministratore Dispositivo (`AdminReceiver`)
Per abilitare le funzionalità gestite da `AdminReceiver`:
1. Apri le **Impostazioni** di Android.
2. Vai in **Sicurezza e privacy → Altre impostazioni di sicurezza → App di amministrazione dispositivo** *(il percorso può variare leggermente in base al produttore, es. Password e sicurezza → Privacy)*.
3. Seleziona **Helgi Remote** e tocca **Attiva questa app di amministrazione dispositivo**.

### 3. Permesso Notifiche (Android 13 e superiori)
1. Tieni premuta l'icona di **Helgi Remote** → **Informazioni app** → **Notifiche**.
2. Assicurati che le notifiche siano **Attive** affinché `RemoteService` possa mostrare l'icona di stato (`ic_notification.xml`) e restare attivo in primo piano.

### 4. Esclusione dall'ottimizzazione della batteria
Per evitare che Android sospenda `ControlServer` quando lo schermo è spento:
1. Vai in **Informazioni app** di **Helgi Remote** → **Utilizzo della batteria per l'app** (o *Batteria*).
2. Seleziona **Senza restrizioni** (*Unrestricted*).
3. *(Solo su Xiaomi MIUI/HyperOS, Oppo, Vivo, Huawei)*: attiva anche la voce **Avvio automatico** (*Autostart*) nelle impostazioni dell'app.

---

## Guida all'Utilizzo

1. **Verifica l'avvio del servizio:**
   Una volta aperta l'app, `MainActivity` avvia `RemoteService`, il quale inizializza `ControlServer` in background. Nella barra delle notifiche comparirà l'icona di stato del servizio attivo.
2. **Individua l'indirizzo IP del dispositivo Android:**
   * Vai su **Impostazioni → Wi-Fi** (tocca la rete connessa) oppure **Impostazioni → Informazioni sul telefono → Stato → Indirizzo IP**.
   * Annota l'IP locale assegnato al telefono (es. `192.168.1.50`).
3. **Connettiti al `ControlServer`:**
   * Dal PC o da un altro dispositivo connesso alla **stessa rete Wi-Fi / LAN** (o alla stessa rete VPN, es. Tailscale/WireGuard), collega il client o il browser all'indirizzo IP del telefono sulla porta configurata in `ControlServer.java` (es. `http://192.168.1.50:<PORTA>`).
   * *Nota per gli sviluppatori:* per verificare o modificare la porta di ascolto, eventuali token di accesso e l'elenco esatto degli endpoint/comandi supportati, consulta il file `helgi-fixed/app/src/main/java/com/helgi/remote/ControlServer.java`.
4. **Test di persistenza al riavvio:**
   Riavvia lo smartphone Android; al termine dell'avvio, `BootReceiver` avvierà automaticamente `RemoteService` senza bisogno di riaprire manualmente l'app.

---

## Risoluzione dei Problemi (Troubleshooting)

| Problema | Causa probabile | Soluzione |
| :--- | :--- | :--- |
| **Il server non risponde dopo qualche minuto a schermo spento** | Il risparmio energetico di Android ha sospeso il processo. | Imposta la batteria su **Senza restrizioni** in *Informazioni app → Batteria* e verifica che la notifica del servizio sia attiva. |
| **L'app non si avvia automaticamente dopo il riavvio del telefono** | L'app non è mai stata aperta manualmente dopo l'installazione oppure il produttore blocca l'autostart. | Apri almeno una volta **Helgi Remote** e abilita **Avvio automatico** nelle impostazioni di sistema. |
| **Impossibile disinstallare l'app** | Android impedisce la disinstallazione finché i permessi di *Amministratore Dispositivo* sono attivi. | Vai in **Impostazioni → App di amministrazione dispositivo**, disattiva **Helgi Remote**, poi disinstalla normalmente l'app. |
| **Errore di connessione dal PC** | I dispositivi non sono sulla stessa sottorete oppure il router ha l'isolamento client (*AP Isolation*) attivo. | Verifica che PC e smartphone siano sulla stessa rete Wi-Fi (non rete ospite) e controlla che la porta in `ControlServer.java` sia corretta. |

---

## Sicurezza e Avvertenze Legali

* **Sicurezza di rete:** `ControlServer` espone un servizio di controllo sul dispositivo. Utilizza l'applicazione esclusivamente su reti locali fidate o tramite tunnel crittografati (es. WireGuard / Tailscale), evitando di esporre direttamente la porta su reti pubbliche non protette.
* **Uso consentito:** Questo software è destinato esclusivamente alla gestione remota di dispositivi di propria proprietà o per i quali si dispone di esplicita autorizzazione amministrativa. L'autore declina ogni responsabilità per un uso improprio o non conforme alle normative vigenti.
