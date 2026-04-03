package tokyo.lasttrain.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tokyo.lasttrain.config.OdptApiProperties;

import java.util.List;

@Component
public class OdptApiClient {

    private static final Logger log = LoggerFactory.getLogger(OdptApiClient.class);

    private final WebClient webClient;
    private final OdptApiProperties properties;
    private final ObjectMapper objectMapper;

    public OdptApiClient(WebClient odptWebClient, OdptApiProperties properties, ObjectMapper objectMapper) {
        this.webClient = odptWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Dump API 호출 - 특정 타입의 전체 데이터를 가져온다.
     * ODPT dump API는 301 redirect 후 JSON 파일을 반환.
     */
    public <T> List<T> fetchDump(String rdfType, TypeReference<List<T>> typeRef) {
        String url = String.format("%s/%s.json?acl:consumerKey=%s",
                properties.baseUrl(), rdfType, properties.consumerKey());

        log.info("Fetching ODPT dump: {}", rdfType);

        byte[] body = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        try {
            List<T> result = objectMapper.readValue(body, typeRef);
            log.info("Fetched {} {} records", result.size(), rdfType);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ODPT dump: " + rdfType, e);
        }
    }

    /**
     * 필터링 API 호출 - 쿼리 파라미터로 필터링된 데이터를 가져온다.
     */
    public <T> List<T> fetch(String rdfType, String queryParams, TypeReference<List<T>> typeRef) {
        String url = String.format("%s/%s?acl:consumerKey=%s&%s",
                properties.baseUrl(), rdfType, properties.consumerKey(), queryParams);

        byte[] body = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        try {
            return objectMapper.readValue(body, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ODPT response: " + rdfType, e);
        }
    }
}
