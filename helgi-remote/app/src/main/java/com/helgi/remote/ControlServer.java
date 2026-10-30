package com.helgi.remote;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ControlServer extends NanoHTTPD {

    private final Context context;
    private final AudioManager audioManager;
    private final DevicePolicyManager dpm;
    private final ComponentName adminComponent;

    public ControlServer(int port, Context context) {
        super(port);
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, AdminReceiver.class);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, List<String>> params = session.getParameters();

        // CORS headers
        Response response;

        try {
            if (uri.equals("/") && method == Method.GET) {
                response = serveHtml();
            } else if (uri.equals("/status") && method == Method.GET) {
                response = handleStatus();
            } else if (uri.equals("/volume/up") && method == Method.POST) {
                response = handleVolumeUp();
            } else if (uri.equals("/volume/down") && method == Method.POST) {
                response = handleVolumeDown();
            } else if (uri.equals("/volume/set") && method == Method.POST) {
                String level = getParam(params, "level", "50");
                response = handleVolumeSet(Integer.parseInt(level));
            } else if (uri.equals("/volume/mute") && method == Method.POST) {
                response = handleMute();
            } else if (uri.equals("/brightness/set") && method == Method.POST) {
                String level = getParam(params, "level", "50");
                response = handleBrightnessSet(Integer.parseInt(level));
            } else if (uri.equals("/screen/on") && method == Method.POST) {
                response = handleScreenOn();
            } else if (uri.equals("/screen/off") && method == Method.POST) {
                response = handleScreenOff();
            } else if (uri.equals("/close-apps") && method == Method.POST) {
                response = handleCloseApps();
            } else if (uri.equals("/shutdown") && method == Method.POST) {
                response = handleShutdown();
            } else if (uri.equals("/reboot") && method == Method.POST) {
                response = handleReboot();
            } else {
                response = newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                        "{\"status\":\"error\",\"message\":\"Endpoint non trovato\"}");
            }
        } catch (Exception e) {
            response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        return response;
    }

    // ─── Volume ────────────────────────────────────────────────────────────────

    private Response handleVolumeUp() {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            int vol = getVolumePercent();
            return ok("Volume aumentato (" + vol + "%)");
        } catch (Exception e) {
            return error("Errore volume: " + e.getMessage());
        }
    }

    private Response handleVolumeDown() {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            int vol = getVolumePercent();
            return ok("Volume diminuito (" + vol + "%)");
        } catch (Exception e) {
            return error("Errore volume: " + e.getMessage());
        }
    }

    private Response handleVolumeSet(int percent) {
        try {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int target = (int) (maxVol * percent / 100.0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI);
            return ok("Volume impostato al " + percent + "%");
        } catch (Exception e) {
            return error("Errore volume: " + e.getMessage());
        }
    }

    private Response handleMute() {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
            return ok("Audio silenziato");
        } catch (Exception e) {
            return error("Errore mute: " + e.getMessage());
        }
    }

    private int getVolumePercent() {
        int cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return (max > 0) ? (int) (cur * 100.0 / max) : 0;
    }

    // ─── Luminosità ────────────────────────────────────────────────────────────

    private Response handleBrightnessSet(int percent) {
        try {
            if (!Settings.System.canWrite(context)) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                        "{\"status\":\"error\",\"message\":\"Permesso mancante: vai in Impostazioni → App → Helgi Remote → Modifica impostazioni di sistema e attivalo\"}");
            }
            int brightness = (int) (255 * percent / 100.0);
            brightness = Math.max(1, Math.min(255, brightness));
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
            return ok("Luminosità impostata al " + percent + "%");
        } catch (Exception e) {
            return error("Errore luminosità: " + e.getMessage());
        }
    }

    // ─── Schermo ───────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private Response handleScreenOn() {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "helgi:wakelock");
            wl.acquire(3000);
            return ok("Schermo acceso");
        } catch (Exception e) {
            return error("Errore accensione schermo: " + e.getMessage());
        }
    }

    private Response handleScreenOff() {
        try {
            if (!dpm.isAdminActive(adminComponent)) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json",
                        "{\"status\":\"error\",\"message\":\"Per bloccare lo schermo devi prima attivare Helgi Remote come Amministratore: Impostazioni → Sicurezza → Amministratori dispositivo → Helgi Remote ✓\"}");
            }
            dpm.lockNow();
            return ok("Schermo spento");
        } catch (Exception e) {
            return error("Errore spegnimento schermo: " + e.getMessage());
        }
    }

    // ─── App / Sistema ─────────────────────────────────────────────────────────

    private Response handleCloseApps() {
        try {
            // Torna alla home (chiude l'app corrente in primo piano)
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);
            return ok("Tornato alla home — tutte le app in primo piano chiuse");
        } catch (Exception e) {
            return error("Errore chiusura app: " + e.getMessage());
        }
    }

    private Response handleShutdown() {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
            return ok("Spegnimento in corso...");
        } catch (Exception e) {
            try {
                Runtime.getRuntime().exec("reboot -p");
                return ok("Spegnimento in corso...");
            } catch (Exception e2) {
                return error("Spegnimento non supportato su questo dispositivo (richiede root o firmware Helgi)");
            }
        }
    }

    private Response handleReboot() {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            return ok("Riavvio in corso...");
        } catch (Exception e) {
            try {
                Runtime.getRuntime().exec("reboot");
                return ok("Riavvio in corso...");
            } catch (Exception e2) {
                return error("Riavvio non supportato su questo dispositivo (richiede root o firmware Helgi)");
            }
        }
    }

    // ─── Status ────────────────────────────────────────────────────────────────

    private Response handleStatus() {
        int vol = getVolumePercent();
        String ip = RemoteService.getLocalIpAddress();
        String device = Build.MODEL;
        String android = Build.VERSION.RELEASE;
        long uptime = android.os.SystemClock.elapsedRealtime() / 1000;
        boolean adminActive = dpm.isAdminActive(adminComponent);
        String json = "{\"status\":\"ok\","
                + "\"volume\":" + vol + ","
                + "\"ip\":\"" + ip + "\","
                + "\"deviceName\":\"" + device + "\","
                + "\"androidVersion\":\"" + android + "\","
                + "\"uptime\":" + uptime + ","
                + "\"adminActive\":" + adminActive + "}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    // ─── HTML Dashboard ────────────────────────────────────────────────────────

    private Response serveHtml() {
        String html = "<!DOCTYPE html><html lang='it'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>Helgi Remote</title>"
            + "<style>"
            + "*{box-sizing:border-box;margin:0;padding:0}"
            + "body{background:#1a1a2e;color:#eee;font-family:system-ui,sans-serif;padding:16px}"
            + "h1{text-align:center;color:#a855f7;margin-bottom:8px;font-size:1.4em}"
            + "#status-bar{background:#16213e;border-radius:12px;padding:12px;margin-bottom:16px;font-size:.85em}"
            + "#status-bar span{display:inline-block;background:#0f3460;border-radius:8px;padding:4px 10px;margin:2px}"
            + ".card{background:#16213e;border-radius:14px;padding:16px;margin-bottom:12px}"
            + ".card h2{font-size:.9em;color:#a855f7;margin-bottom:10px;text-transform:uppercase;letter-spacing:.5px}"
            + "button{border:none;border-radius:10px;padding:12px 16px;font-size:.95em;cursor:pointer;margin:4px;transition:.2s}"
            + "button:active{transform:scale(.95)}"
            + ".btn-primary{background:#a855f7;color:#fff}"
            + ".btn-success{background:#22c55e;color:#fff}"
            + ".btn-danger{background:#ef4444;color:#fff}"
            + ".btn-warning{background:#f59e0b;color:#fff}"
            + ".btn-gray{background:#374151;color:#eee}"
            + ".btn-row{display:flex;flex-wrap:wrap;gap:4px}"
            + "input[type=range]{width:100%;margin:8px 0;accent-color:#a855f7}"
            + ".toast{position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:#333;color:#fff;"
            + "padding:10px 20px;border-radius:10px;font-size:.9em;opacity:0;transition:.3s;z-index:999;max-width:90%}"
            + ".toast.show{opacity:1}"
            + ".toast.ok{background:#22c55e}"
            + ".toast.err{background:#ef4444}"
            + ".admin-warn{background:#7c2d12;border-radius:10px;padding:10px;font-size:.8em;margin-top:8px;display:none}"
            + "</style></head><body>"
            + "<h1>📺 Helgi Remote</h1>"
            + "<div id='status-bar'>"
            + "  <span id='s-ip'>📡 ...</span>"
            + "  <span id='s-vol'>🔊 ...</span>"
            + "  <span id='s-dev'>📱 ...</span>"
            + "  <span id='s-and'>🤖 Android ...</span>"
            + "  <span id='s-admin'>🔐 ...</span>"
            + "</div>"

            // Volume
            + "<div class='card'><h2>🔊 Volume</h2>"
            + "<div class='btn-row'>"
            + "<button class='btn-gray' onclick='post(\"/volume/down\")'>➖</button>"
            + "<button class='btn-gray' onclick='post(\"/volume/up\")'>➕</button>"
            + "<button class='btn-warning' onclick='post(\"/volume/mute\")'>🔇 Muto</button>"
            + "</div>"
            + "<input type='range' id='vol-slider' min='0' max='100' value='50' oninput='setVolume(this.value)'>"
            + "<div style='text-align:center;font-size:.85em;color:#aaa' id='vol-label'>50%</div>"
            + "</div>"

            // Luminosità
            + "<div class='card'><h2>💡 Luminosità</h2>"
            + "<input type='range' id='bri-slider' min='1' max='100' value='50' oninput='setBrightness(this.value)'>"
            + "<div style='text-align:center;font-size:.85em;color:#aaa' id='bri-label'>50%</div>"
            + "</div>"

            // Schermo
            + "<div class='card'><h2>📺 Schermo</h2>"
            + "<div class='btn-row'>"
            + "<button class='btn-success' onclick='post(\"/screen/on\")'>▶️ Accendi</button>"
            + "<button class='btn-gray' onclick='post(\"/screen/off\")'>⏸️ Blocca</button>"
            + "</div>"
            + "<div class='admin-warn' id='admin-warn'>⚠️ Per bloccare lo schermo: <b>Impostazioni → Sicurezza → Amministratori dispositivo → Helgi Remote ✓</b></div>"
            + "</div>"

            // App
            + "<div class='card'><h2>📱 Applicazioni</h2>"
            + "<div class='btn-row'>"
            + "<button class='btn-primary' onclick='post(\"/close-apps\")'>🏠 Vai alla Home</button>"
            + "</div></div>"

            // Sistema
            + "<div class='card'><h2>⚙️ Sistema</h2>"
            + "<div class='btn-row'>"
            + "<button class='btn-warning' onclick='confirmAction(\"reboot\")'>🔄 Riavvia</button>"
            + "<button class='btn-danger' onclick='confirmAction(\"shutdown\")'>⏹️ Spegni</button>"
            + "</div></div>"

            + "<div class='toast' id='toast'></div>"

            + "<script>"
            + "let volTimer, briTimer;"
            + "function showToast(msg,ok){"
            + "  const t=document.getElementById('toast');"
            + "  t.textContent=msg; t.className='toast show '+(ok?'ok':'err');"
            + "  setTimeout(()=>t.className='toast',2500);"
            + "}"
            + "async function post(url){"
            + "  try{"
            + "    const r=await fetch(url,{method:'POST'});"
            + "    const j=await r.json();"
            + "    if(j.status==='error'){"
            + "      showToast('❌ '+j.message,false);"
            + "      if(url==='/screen/off') document.getElementById('admin-warn').style.display='block';"
            + "    } else {"
            + "      showToast('✅ '+j.message,true);"
            + "      document.getElementById('admin-warn').style.display='none';"
            + "    }"
            + "  } catch(e){ showToast('❌ Connessione persa',false); }"
            + "  loadStatus();"
            + "}"
            + "function setVolume(v){"
            + "  document.getElementById('vol-label').textContent=v+'%';"
            + "  clearTimeout(volTimer);"
            + "  volTimer=setTimeout(()=>fetch('/volume/set?level='+v,{method:'POST'}),300);"
            + "}"
            + "function setBrightness(v){"
            + "  document.getElementById('bri-label').textContent=v+'%';"
            + "  clearTimeout(briTimer);"
            + "  briTimer=setTimeout(()=>fetch('/brightness/set?level='+v,{method:'POST'}),300);"
            + "}"
            + "function confirmAction(action){"
            + "  const labels={reboot:'riavviare',shutdown:'spegnere'};"
            + "  if(confirm('Sei sicuro di voler '+labels[action]+' il dispositivo?')) post('/'+action);"
            + "}"
            + "async function loadStatus(){"
            + "  try{"
            + "    const j=await (await fetch('/status')).json();"
            + "    document.getElementById('s-ip').textContent='📡 '+j.ip+':8080';"
            + "    document.getElementById('s-vol').textContent='🔊 '+j.volume+'%';"
            + "    document.getElementById('s-dev').textContent='📱 '+j.deviceName;"
            + "    document.getElementById('s-and').textContent='🤖 Android '+j.androidVersion;"
            + "    document.getElementById('s-admin').textContent=j.adminActive?'🔐 Admin ✓':'🔐 Admin ✗';"
            + "    document.getElementById('vol-slider').value=j.volume;"
            + "    document.getElementById('vol-label').textContent=j.volume+'%';"
            + "  } catch(e){}"
            + "}"
            + "loadStatus();"
            + "setInterval(loadStatus,5000);"
            + "</script></body></html>";

        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Response ok(String message) {
        return newFixedLengthResponse(Response.Status.OK, "application/json",
                "{\"status\":\"ok\",\"message\":\"" + message + "\"}");
    }

    private Response error(String message) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                "{\"status\":\"error\",\"message\":\"" + message.replace("\"", "'") + "\"}");
    }

    private String getParam(Map<String, List<String>> params, String key, String def) {
        List<String> vals = params.get(key);
        return (vals != null && !vals.isEmpty()) ? vals.get(0) : def;
    }
}
