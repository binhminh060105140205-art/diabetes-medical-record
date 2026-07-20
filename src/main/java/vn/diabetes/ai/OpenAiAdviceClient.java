package vn.diabetes.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class OpenAiAdviceClient {
    private final ObjectMapper json;
    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final Duration timeout;
    private final String instructions;

    public OpenAiAdviceClient(ObjectMapper json,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5.6-terra}") String model,
            @Value("${app.openai.base-url:https://api.openai.com/v1/responses}") String endpoint,
            @Value("${app.openai.timeout-seconds:20}") int timeoutSeconds) {
        this.json = json;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.endpoint = URI.create(endpoint);
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        try {
            this.instructions = new ClassPathResource("prompts/patient-daily-advice-system.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Missing patient advice prompt", error);
        }
    }

    public boolean isConfigured() { return !apiKey.isBlank(); }
    public String model() { return model; }

    public GeneratedAdvice generate(PatientAdviceRuleEngine.Prepared prepared) {
        if (!isConfigured()) throw new IllegalStateException("OpenAI API key is not configured");
        try {
            ObjectNode requestBody = json.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("store", false);
            requestBody.put("instructions", instructions);
            requestBody.put("input", json.writeValueAsString(prepared.context()));
            requestBody.putObject("reasoning").put("effort", "none");
            ObjectNode text = requestBody.putObject("text");
            text.put("verbosity", "medium");
            text.set("format", responseFormat());

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI request failed with status " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request interrupted", error);
        } catch (IOException error) {
            throw new IllegalStateException("OpenAI request failed", error);
        }
    }

    GeneratedAdvice parseResponse(String body) {
        try {
            JsonNode root = json.readTree(body);
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if (content.hasNonNull("refusal")) throw new IllegalStateException("OpenAI refused the request");
                    if (content.hasNonNull("text")) return parseAdviceJson(content.path("text").asText());
                }
            }
            throw new IllegalStateException("OpenAI returned no structured advice");
        } catch (IOException error) {
            throw new IllegalStateException("Invalid OpenAI response", error);
        }
    }

    private GeneratedAdvice parseAdviceJson(String source) throws IOException {
        JsonNode node = json.readTree(source);
        String summary = clean(node.path("summary").asText(), 350);
        String severity = node.path("severity").asText();
        if (!List.of("low", "medium", "high").contains(severity)) severity = "medium";
        List<String> advice = new ArrayList<>();
        for (JsonNode item : node.path("advice")) {
            String value = clean(item.asText(), 300);
            if (!value.isBlank() && advice.size() < 8) advice.add(value);
        }
        if (summary.isBlank() || advice.isEmpty()) throw new IllegalStateException("Incomplete OpenAI advice");
        return new GeneratedAdvice(summary, advice, severity, node.path("doctor_recommendation").asBoolean(false));
    }

    private ObjectNode responseFormat() {
        ObjectNode format = json.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", "patient_daily_advice");
        format.put("strict", true);
        ObjectNode schema = format.putObject("schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("summary").put("type", "string").put("maxLength", 350);
        ObjectNode advice = properties.putObject("advice");
        advice.put("type", "array").put("minItems", 6).put("maxItems", 8);
        advice.putObject("items").put("type", "string").put("maxLength", 500);
        ObjectNode severity = properties.putObject("severity");
        severity.put("type", "string");
        ArrayNode severityValues = severity.putArray("enum");
        severityValues.add("low").add("medium").add("high");
        properties.putObject("doctor_recommendation").put("type", "boolean");
        schema.putArray("required").add("summary").add("advice").add("severity").add("doctor_recommendation");
        return format;
    }

    private String clean(String value, int maxLength) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim();
    }

    public record GeneratedAdvice(String summary, List<String> advice, String severity,
            boolean doctorRecommendation) {}
}
