package vn.diabetes.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAiAdviceClientTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void parsesStructuredResponsesOutput() {
        OpenAiAdviceClient client = new OpenAiAdviceClient(json, "", "gpt-5.6-terra",
                "https://api.openai.com/v1/responses", 20);
        String body = """
                {"output":[{"type":"message","content":[{"type":"output_text","text":"{\\"summary\\":\\"Cần theo dõi thêm.\\",\\"advice\\":[\\"Ghi chỉ số đều đặn.\\",\\"Ăn đúng bữa.\\"],\\"severity\\":\\"medium\\",\\"doctor_recommendation\\":false}"}]}]}
                """;

        var result = client.parseResponse(body);
        assertEquals("Cần theo dõi thêm.", result.summary());
        assertEquals(2, result.advice().size());
        assertEquals("medium", result.severity());
        assertFalse(result.doctorRecommendation());
    }

    @Test
    void missingApiKeyKeepsExternalCallsDisabled() {
        OpenAiAdviceClient client = new OpenAiAdviceClient(json, "  ", "gpt-5.6-terra",
                "https://api.openai.com/v1/responses", 20);
        assertFalse(client.isConfigured());
    }

    @Test
    void limitsParsedAdviceToTenItems() throws Exception {
        OpenAiAdviceClient client = new OpenAiAdviceClient(json, "", "gpt-5.6-terra",
                "https://api.openai.com/v1/responses", 20);
        var generated = json.createObjectNode();
        generated.put("summary", "Kế hoạch hôm nay.");
        var advice = generated.putArray("advice");
        for (int index = 1; index <= 12; index++) advice.add("Lời khuyên số " + index);
        generated.put("severity", "low");
        generated.put("doctor_recommendation", false);

        var root = json.createObjectNode();
        var content = root.putArray("output").addObject().putArray("content").addObject();
        content.put("type", "output_text");
        content.put("text", json.writeValueAsString(generated));

        var result = client.parseResponse(json.writeValueAsString(root));

        assertEquals(10, result.advice().size());
        assertEquals("Lời khuyên số 10", result.advice().get(9));
    }
}
