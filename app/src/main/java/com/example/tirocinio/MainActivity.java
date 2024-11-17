package com.example.tirocinio;

import static android.app.PendingIntent.getActivity;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity  {

    // Nome del file SharedPreferences
    public static final String MyPREFERENCES = "MyPreferences";
    // chiave per l'UUID nello SharedPreferences
    public static final String UUID_KEY = "deviceUUID";
    // Nome del file da cui leggere le informazioni statiche della CPU
    public static final String CPUINFO_FILE_NAME = "/proc/cpuinfo";
    // Nome del file da cui leggere le informazioni dinamiche della CPU
    public static final String CPUACTIVITY_FILE_NAME = "/proc/stat";
    // Tempo di attesa (in millisecondi) tra due campionamenti della CPU per ricavarne la percentuale di utilizzo
    public static final int CPU_SAMPLING_INTERVAL = 5000;

    // Frequenza di aggiornamento della lista di metriche in millisecondi
    public  static int updateFrequency = 10000;

    // Coppia indirizzo IP e numero di porta del server a cui inviare le metriche
    public static String serverBaseUrl = "http://137.204.57.80:31548/";

    // Tempo di inizio invio dati (in ms)
    public static long startTime = 0;

    // Tempo di ricezione della risposta (in ms)
    public static long endTime = 0;

    // Tempo che intercorre tra l'invio dei dati al server e la ricezione della risposta (in ms)
    public static long delay = 0;

    // Handler per gestire l'esecuzione periodica
    Handler handler = new Handler();

    // Runnable per eseguire le funzioni di monitoraggio ogni UPDATE_INTERVAL secondi
    final Runnable r1 = new Runnable(){
        public void run(){

            EditText updateFrequencyText = findViewById(R.id.updateFrequency);
            if( ! TextUtils.isEmpty(updateFrequencyText.getText()) && TextUtils.isDigitsOnly(updateFrequencyText.getText()) ){
                try{
                    updateFrequency = Integer.parseInt(updateFrequencyText.getText().toString());
                } catch ( NumberFormatException e ){
                    updateFrequency = 10000;
                }
            }
            else{
                updateFrequency = 10000;
            }


            getId(null);
            batteryMonitoring(null);
            memoryMonitoring(null);
            networkMonitoring(null);
            cpuMonitoringStatic(null);
            //cpuMonitoringDynamic(null);

            handler.postDelayed(r1,updateFrequency);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Avvia il runnable appena l'activity viene creata
        handler.post(r1);

    }

    public void getId(PhoneMetrics phoneMetricsIn){
        // Metodo per ottenere e memorizzare l'UUID del dispositivo

        // Ottiene le SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE);
        // Recupera l'UUID se già esistente, altrimenti genera un nuovo UUID e lo salva
        String uuid = sharedPreferences.getString(UUID_KEY, null);
        if (uuid == null){
            uuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(UUID_KEY, uuid);
            editor.apply();
        }

        TextView battery_view = findViewById(R.id.id);
        battery_view.setText(String.format("Device id: %s", uuid));

        if(phoneMetricsIn != null){
            phoneMetricsIn.setId(uuid);
        }
    }

    public void batteryMonitoring(PhoneMetrics phoneMetricsIn){
        // Metodo per monitorare il livello di batteria

        // Registra un BroadcastReceiver per monitorare lo stato della batteria
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        // Ottiene il livello attuale della batteria
        int level = 0;
        int scale = 1;
        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        }
        float batteryPct = level * 100 / (float)scale;

        TextView battery_view = findViewById(R.id.battery);
        battery_view.setText(String.format("Available battery: %s", Float.toString(batteryPct)));

        if(phoneMetricsIn != null){
            phoneMetricsIn.setBattery(Float.toString(batteryPct));
        }
    }

    public void memoryMonitoring(PhoneMetrics phoneMetricsIn) {
        // Metodo per monitorare la memoria

        // Utilizza la classe ActivityManager per monitorare la memoria
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        // Memoria RAM disponibile e totale (byte)
        long availableMemory = memoryInfo.availMem;
        long totalMemory = memoryInfo.totalMem;

        TextView memory_view = findViewById(R.id.memory);
        memory_view.setText(String.format("Available memory: %s", Long.toString(availableMemory)));
        memory_view.append("\nTotal memory: " + Long.toString(totalMemory));

        if (phoneMetricsIn != null) {
            phoneMetricsIn.setAvailableMemory(Long.toString(availableMemory));
            phoneMetricsIn.setTotalMemory(Long.toString(totalMemory));
        }
    }

    public void networkMonitoring(PhoneMetrics phoneMetricsIn) {
        // Metodo per monitorare la rete

        // Utilizza la classe ConnectivityManager per monitorare la connessione di rete
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);

        // Ricava larghezza di banda di download e upload in kilobit per secondo
        TextView bandwidth_view = findViewById(R.id.netBandwidth);
        if (caps == null){
            bandwidth_view.setText("Downstream bandwidth in Kbps: unknown");
            bandwidth_view.append("\nUpstream bandwidth in Kbps: unknown");
        }
        else{
            int downStreamBandwidth = caps.getLinkDownstreamBandwidthKbps();
            int upStreamBandwidth = caps.getLinkUpstreamBandwidthKbps();
            bandwidth_view.setText(String.format("Downstream bandwidth in Kbps: %s", String.valueOf(downStreamBandwidth)));
            bandwidth_view.append("\nUpstream bandwidth in Kbps: " + String.valueOf(upStreamBandwidth));

            if (phoneMetricsIn != null){
                phoneMetricsIn.setDownstreamBandwidth(String.valueOf(downStreamBandwidth));
                phoneMetricsIn.setUpstreamBandwidth(String.valueOf(upStreamBandwidth));
            }
        }

        // Ricava la forza del segnale (dBm), se disponibile
        TextView signal_strength_view = findViewById(R.id.netSignalStrength);
        if (caps == null){
            signal_strength_view.setText("Signal strength: unknown");
        }
        else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                int signalStrength = caps.getSignalStrength();
                signal_strength_view.setText(String.format("Signal strength (dBm): %s", String.valueOf(signalStrength)));

                if (phoneMetricsIn != null) {
                    phoneMetricsIn.setSignalStrength(String.valueOf(signalStrength));
                }
            }
        }

        // Ricava altre capacità della rete
        TextView capabilities_view = findViewById(R.id.netCapabilities);
        if (caps == null){
            capabilities_view.setText("The network is able to reach internet: unknown");
            capabilities_view.append("\nThe network is not congested: unknown");
            capabilities_view.append("\nThe network is not suspended: unknown");
        }
        else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Boolean isReachingInternet = caps.hasCapability(NET_CAPABILITY_INTERNET);
                Boolean isNotCongested = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
                Boolean isNotSuspended = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
                capabilities_view.setText(String.format("The network is able to reach internet: %s", String.valueOf(isReachingInternet)));
                capabilities_view.append("\nThe network is not congested: " + String.valueOf(isNotCongested));
                capabilities_view.append("\nThe network is not suspended: " + String.valueOf(isNotSuspended));

                if (phoneMetricsIn != null) {
                    phoneMetricsIn.setIsReachingInternet(String.valueOf(isReachingInternet));
                    phoneMetricsIn.setIsNotCongested(String.valueOf(isNotCongested));
                    phoneMetricsIn.setIsNotSuspended(String.valueOf(isNotSuspended));
                }
            }
        }

    }

    public void cpuMonitoringStatic(PhoneMetrics phoneMetricsIn){
        // Metodo per monitorare staticamente la CPU

        // Numero di core disponibili
        int numOfCores = Runtime.getRuntime().availableProcessors();

        // ArrayList per frequenze, dimensioni della cache e flags dei core
        ArrayList<String> cpuFrequencies = new ArrayList<>();
        ArrayList<String> cpuCacheSizes = new ArrayList<>();
        ArrayList<String> cpuFlags = new ArrayList<>();

        String line = null;

        String currentFrequency = null;
        String currentCacheSize = null;
        String currentFlags = null;

        try {
            // Legge il file CPUINFO_FILE_NAME
            BufferedReader br = new BufferedReader(new FileReader(CPUINFO_FILE_NAME));

            // Scorre riga per riga il file CPUINFO_FILE_NAME
            while ((line = br.readLine()) != null) {
                if (line.startsWith("cpu MHz")) {
                    // Estrae la frequenza (in MHz) e la aggiunge all'ArrayList
                    currentFrequency = line.split(":\\s+", 2)[1].trim();
                    cpuFrequencies.add(currentFrequency);
                }
                else if (line.startsWith("cache size")) {
                    // Estrae la dimensione della cache (in KB) e la aggiunge all'ArrayList
                    currentCacheSize = line.split(":\\s+", 2)[1].trim();
                    cpuCacheSizes.add(currentCacheSize);
                }
                else if (line.startsWith("flags")) {
                    // Estrae i flags e li aggiunge all'ArrayList
                    currentFlags = line.split(":\\s+", 2)[1].trim();
                    cpuFlags.add(currentFlags);
                }
            }
            br.close();
        } catch (IOException e){
            Log.d("CPU INFO", "Errore nella lettura di " + CPUINFO_FILE_NAME);
        }

        TextView cpu_view = findViewById(R.id.cpu);
        cpu_view.setText(String.format("Number of Cores: " + numOfCores));
        // Separate da virgole, si trovano in sequenza le informazioni su frequenza, dimensione dalla cache e flags dei core della cpu (in ordine dal primo all'ultimo)
        cpu_view.append("\nCPU cores frequencies (MHz): " + TextUtils.join(", ", cpuFrequencies));
        cpu_view.append("\nCPU cores cache sizes (KB): " +  TextUtils.join(", ", cpuCacheSizes));
        cpu_view.append("\nCPU cores flags: " + TextUtils.join(", ", cpuFlags));

        if(phoneMetricsIn != null){
            phoneMetricsIn.setNumOfCores(Integer.toString(numOfCores));
            phoneMetricsIn.setCpuFrequencies(TextUtils.join(", ", cpuFrequencies));
            phoneMetricsIn.setCpuCacheSizes(TextUtils.join(", ", cpuCacheSizes));
            phoneMetricsIn.setCpuFlags(TextUtils.join(", ", cpuFlags));
        }
    }

    public void cpuMonitoringDynamic(PhoneMetrics phoneMetricsIn){
        new Thread(r2).start();
    }

    public Runnable r2 = new Runnable() {
        @Override
        public void run() {
            // Numero di core disponibili
            int numOfCores = Runtime.getRuntime().availableProcessors();

            ArrayList<String> cpuUsagePercentages = new ArrayList<>();

            String line = null;

            // all'indice 0 degli array si trovano i dati relativi all'utilizzo complessivo della CPU
            // all'indice i-esimo si trovano i dati relativi all'utilizzo del core i-esimo della CPU
            int[] currentUser1 = new int[numOfCores+1];
            int[] currentNice1 = new int[numOfCores+1];
            int[] currentSystem1 = new int[numOfCores+1];
            int[] currentIdle1 = new int[numOfCores+1];
            int[] currentIowait1 = new int[numOfCores+1];
            int[] currentIrq1 = new int[numOfCores+1];
            int[] currentSoftIrq1 = new int[numOfCores+1];

            int[] currentUser2 = new int[numOfCores+1];
            int[] currentNice2 = new int[numOfCores+1];
            int[] currentSystem2 = new int[numOfCores+1];
            int[] currentIdle2 = new int[numOfCores+1];
            int[] currentIowait2 = new int[numOfCores+1];
            int[] currentIrq2 = new int[numOfCores+1];
            int[] currentSoftIrq2 = new int[numOfCores+1];

            int[] cpuUsageTime = new int[numOfCores+1];
            int[] cpuTotalTime = new int[numOfCores+1];
            float[] cpuUsage = new float[numOfCores+1];

            int index = 0;

            try{
                // Legge il file CPUACTIVITY_FILE_NAME
                BufferedReader br = new BufferedReader(new FileReader(CPUACTIVITY_FILE_NAME));

                // Scorre riga per riga il file CPUACTIVITY_FILE_NAME
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("cpu")) {
                        String[] buffer = line.split("\\s+");
                        currentUser1[index] = Integer.parseInt(buffer[1]);
                        currentNice1[index] = Integer.parseInt(buffer[2]);
                        currentSystem1[index] = Integer.parseInt(buffer[3]);
                        currentIdle1[index] = Integer.parseInt(buffer[4]);
                        currentIowait1[index] = Integer.parseInt(buffer[5]);
                        currentIrq1[index] = Integer.parseInt(buffer[6]);
                        currentSoftIrq1[index] = Integer.parseInt(buffer[7]);
                        index += 1;
                    }
                }
                br.close();

                index = 0;
                try{
                    Thread.sleep(CPU_SAMPLING_INTERVAL);

                    br = new BufferedReader(new FileReader(CPUACTIVITY_FILE_NAME));

                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("cpu")) {
                            String[] buffer = line.split("\\s+");
                            currentUser2[index] = Integer.parseInt(buffer[1]);
                            currentNice2[index] = Integer.parseInt(buffer[2]);
                            currentSystem2[index] = Integer.parseInt(buffer[3]);
                            currentIdle2[index] = Integer.parseInt(buffer[4]);
                            currentIowait2[index] = Integer.parseInt(buffer[5]);
                            currentIrq2[index] = Integer.parseInt(buffer[6]);
                            currentSoftIrq2[index] = Integer.parseInt(buffer[7]);
                            index += 1;
                        }
                    }
                    br.close();

                    for (index=0; index<=numOfCores+1; index++){
                        cpuUsageTime[index] = currentUser2[index] - currentUser1[index] + currentNice2[index] - currentNice1[index] +
                                currentSystem2[index] - currentSystem1[index] + currentIrq2[index] - currentIrq1[index] + currentSoftIrq2[index] - currentSoftIrq1[index];
                        cpuTotalTime[index] = cpuUsageTime[index] + currentIdle2[index] - currentIdle1[index] + currentIowait2[index] - currentIowait1[index];
                        if (cpuTotalTime[index]!=0){
                            cpuUsage[index] = ( (float) (cpuUsageTime[index] / cpuTotalTime[index] ) ) * 100;
                        }
                        else{
                            cpuUsage[index]=0;
                        }
                        Log.d("CPU INFO", "Usage time: "+cpuUsageTime[index] + " \ntotal time: " + cpuTotalTime[index]);
                        cpuUsagePercentages.add(String.valueOf(cpuUsage[index]));
                    }

                    TextView cpu_view = findViewById(R.id.cpu);
                    cpu_view.append("\nCPU usage percentages (total first and single core after): " + TextUtils.join(", ", cpuUsagePercentages));

                    /*
                    if(phoneMetricsIn != null){
                        phoneMetricsIn.setCpuUsage(TextUtils.join(", ", cpuUsagePercentages));
                    }
                    */
                } catch (InterruptedException e) {
                    Log.d("CPU INFO", "Errore nel thread utilizzato per la lettura di " + CPUACTIVITY_FILE_NAME);
                    e.printStackTrace();
                }


            } catch (IOException e){
                Log.d("CPU INFO", "Errore nella lettura di " + CPUACTIVITY_FILE_NAME);
                e.printStackTrace();
            }

        }
    };

    public void sendMetrics(View v){
        // Imposta un listener sul pulsante per inviare i dati
        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Crea un nuovo oggetto PhoneMetrics
                PhoneMetrics phoneMetrics = new PhoneMetrics();

                // Chiama i metodi di monitoraggio fornendo come parametro l'oggetto phoneMetrics iin cui vengono salvate le metriche del dispositivo
                getId(phoneMetrics);
                batteryMonitoring(phoneMetrics);
                memoryMonitoring(phoneMetrics);
                networkMonitoring(phoneMetrics);
                cpuMonitoringStatic(phoneMetrics);

                // Il ritardo riportato fa riferimento all'invio dell'oggetto phoneMetrics precedente (0 per il primo invio di dati al server)
                phoneMetrics.setDelay(Long.toString(delay));

                // Converte l'oggetto PhoneMetrics in una stringa JSON
                Gson gson = new Gson();
                String phoneMetricsString = gson.toJson(phoneMetrics);

                // Controlla se la rete è disponibile e registra il callback
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        // Quando la rete è disponibile, crea un thread per inviare i dati al server
                        ExecutorService executor = Executors.newSingleThreadExecutor();

                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                HttpURLConnection client = null;
                                URL url = null;
                                try {
                                    // Costruisce l'URL a cui inviare le metriche dal campo di input
                                    EditText serverUrlText = findViewById(R.id.serverUrl);
                                    if (!TextUtils.isEmpty(serverUrlText.getText().toString())){
                                        serverBaseUrl = serverUrlText.getText().toString();
                                    }
                                    String uuid = phoneMetrics.getId();
                                    String serverUrl = serverBaseUrl + uuid + "/metrics";
                                    try{
                                        url = new URL(serverUrl);
                                    } catch (MalformedURLException e){
                                        Log.d("URL_ERROR", "URL non valido: "+ e.getMessage());
                                        return;
                                    }

                                    // Apre una connessione HTTP con metodo POST
                                    client = (HttpURLConnection) url.openConnection();
                                    client.setRequestMethod("POST");
                                    //impostazione content type e accept type
                                    client.setRequestProperty("Content-Type", "application/json");
                                    client.setRequestProperty("Accept", "application/json");
                                    // Abilita l'invio dei dati
                                    client.setDoOutput(true);

                                    // creazione di un output stream e posting dei dati
                                    try (OutputStream os = client.getOutputStream()) {
                                        byte[] input = phoneMetricsString.getBytes(StandardCharsets.UTF_8);
                                        // Registra l'ora di inizio dell'invio delle metriche in millisecondi
                                        startTime = System.currentTimeMillis();
                                        os.write(input, 0, input.length);
                                    }

                                    // lettura risposta del server
                                    try (BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {

                                        StringBuilder response = new StringBuilder();
                                        String responseLine = null;
                                        while ((responseLine = br.readLine()) != null) {
                                            response.append(responseLine.trim());
                                        }
                                        // Registra l'ora di fine lettura della risposta del server in millisecondi
                                        endTime = System.currentTimeMillis();
                                        // Calcola il ritardo come differenza tra l'ora di fine e l'ora di inizio (nullo per la prima metrica inviata)
                                        delay = endTime - startTime;

                                        // Usa runOnUiThread per aggiornare la UI (TextView) nel thread principale
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                TextView serverResponseView = findViewById(R.id.serverResponse);
                                                serverResponseView.setText(response.toString());
                                                // Il ritardo riportato fa riferimento alle metriche di cui il server sta confermando la ricezione
                                                serverResponseView.append("\nDelay (ms): " + delay);
                                            }
                                        });

                                    }


                                } catch (Exception e) {
                                    Log.d("HTTP_POST", "Errore durante l'invio del json", e);
                                } finally {
                                    // Chiusura della connessione
                                    if (client != null) {
                                        client.disconnect();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        // Quando la connessione è persa, logga il messaggio
                        Log.d("STATO DELLA RETE", "Nessuna connessione attiva");
                    }
                };

                // Registra il callback per la connessione di rete
                NetworkRequest networkRequest = new NetworkRequest.Builder().build();
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            };
        });

    }



}

