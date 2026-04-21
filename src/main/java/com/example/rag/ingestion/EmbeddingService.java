package com.example.rag.ingestion;

import com.example.rag.config.Config;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class EmbeddingService {

    public static float[] getEmbedding(String text) throws Exception {

        URL url = new URL(Config.OLLAMA_BASE_URL + "/api/embeddings");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject json = new JSONObject();
        json.put("model", Config.OLLAMA_EMBEDDING_MODEL);
        json.put("prompt", text);

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes());
        os.flush();

        Scanner scanner = new Scanner(conn.getInputStream());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject res = new JSONObject(response);
        var arr = res.getJSONArray("embedding");

        float[] vector = new float[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            vector[i] = arr.getFloat(i);
        }

        return vector;
    }
}
