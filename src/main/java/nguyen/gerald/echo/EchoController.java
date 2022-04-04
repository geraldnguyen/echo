package nguyen.gerald.echo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpUtils;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/echo")
@Slf4j
public class EchoController {
    public static final String METHOD = "method";
    public static final String PATH = "path";
    public static final String PROTOCOL = "protocol";
    public static final String QUERY_STRING = "queryString";
    public static final String QUERY = "query";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String BODY = "body";
    public static final String FORM = "form";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String HEADERS = "headers";

    private final ObjectWriter objectWriter;

    public EchoController(ObjectMapper objectMapper) {
        this.objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }

    @RequestMapping("")
    public Map<String, Object> echo(HttpServletRequest request) throws IOException, ServletException {
        var details = extractDetails(request);

        logRequestDetails(details);
        return details;
    }

    protected void logRequestDetails(Map<String, Object> details) throws JsonProcessingException {
        var detailsAsStr = objectWriter.writeValueAsString(details);
        log.info("Request details: {}", detailsAsStr);
    }

    protected Map<String, Object> extractDetails(HttpServletRequest request) throws IOException, ServletException {
        Map<String, Object> details = new LinkedHashMap<>();

        // request
        details.put(METHOD, request.getMethod());
        details.put(PATH, request.getRequestURI());
        details.put(PROTOCOL, request.getProtocol());

        // query string (if any)
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            details.put(QUERY_STRING, queryString);
            details.put(QUERY, extractQuery(queryString));
        }

        // form-data if contentType = application/x-www-form-urlencoded or multi-part
        var contentType = request.getContentType();
        if (contentType != null){
            if (contentType.equals(APPLICATION_X_WWW_FORM_URLENCODED)) {
                String formData = new String(request.getInputStream().readAllBytes());
                details.put(BODY, formData);
                details.put(FORM, extractQuery(formData));
            } else if (contentType.startsWith(MULTIPART_FORM_DATA)) {
                var parts = request.getParts();
                details.put(BODY, describeMultipartBody(parts));

                Map<String, Object> formData = new HashMap<>();
                for (Part part: parts) {
                    var name = part.getName();
                    var value = describePart(part);
                    if (formData.containsKey(name)) {
                        var existingValue = formData.get(name);
                        if (existingValue instanceof String) {
                            formData.put(name, List.of(existingValue, value));
                        } else {
                            var newList = new ArrayList<>((List<String>) existingValue);
                            newList.add(value);
                            formData.put(name, newList);
                        }
                    } else {
                        formData.put(name, value);
                    }
                }
                details.put(FORM, formData);
            }
        }

        // headers
        details.put(HEADERS, extractHeaders(request));

        return details;
    }

    protected String describeMultipartBody(Collection<Part> parts) {
        return "<" + parts.size() + " parts>";
    }

    protected String describePart(Part part) throws IOException {
        var bytes = part.getInputStream().readAllBytes();
        var contentType = part.getContentType();
        if (contentType == null) {
            return new String(bytes);
        }
        return String.format("<file: %s, size: %d bytes>",  part.getSubmittedFileName(), bytes.length);
    }

    /** Supporting only default encoding because of the dependency on {@link HttpUtils#parseQueryString(String)} */
    // TODO: improve handling of malformed query
    protected Map<String, Object> extractQuery(String queryString) {

        var queryMap = HttpUtils.parseQueryString(queryString);
        if (queryMap.isEmpty()) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();
        for (Map.Entry<String, String[]> e : queryMap.entrySet()) {
            var key = e.getKey();
            var value = e.getValue();
            if (value.length == 1) {
                details.put(key, value[0]);
            } else {
                details.put(key, value);
            }
        }
        return details;
    }

    protected Map<String, Object> extractHeaders(HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        var headerNames = request.getHeaderNames();
        for (Iterator<String> it = headerNames.asIterator(); it.hasNext(); ) {
            String name = it.next();
            var values = Collections.list(request.getHeaders(name));
            if (values.size() == 1) {
                details.put(name, values.get(0));
            } else {
                details.put(name, values);
            }
        }

        return details;
    }
}
