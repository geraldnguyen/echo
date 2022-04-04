package nguyen.gerald.echo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Part;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nguyen.gerald.echo.EchoController.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(EchoController.class)
class EchoControllerTest {
    public static final String ECHO_PATH = "/echo";

    private final MockMvc mockMvc;

    @Autowired
    EchoControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @ParameterizedTest
    @ValueSource(strings = { "/echo", "/echo/" })
    void supportEndingWithOrWithoutSlash(String path) throws Exception {
        this.mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @Disabled
    void canCaptureHashFragment() throws Exception {
        mockMvc.perform(get(ECHO_PATH + "#abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("hash", is("abc")))
                // TODO: more verification
        ;

    }

    @Nested
    class TestHttpMethod {
        @Test
        void echoGet() throws Exception {
            doEchoAndAssert(get(ECHO_PATH), "GET");
        }

        @Test
        void echoPost() throws Exception {
            doEchoAndAssert(post(ECHO_PATH), "POST");
        }

        @Test
        void echoPut() throws Exception {
            doEchoAndAssert(put(ECHO_PATH), "PUT");
        }

        @Test
        void echoPatch() throws Exception {
            doEchoAndAssert(patch(ECHO_PATH), "PATCH");
        }

        @Test
        void echoDelete() throws Exception {
            doEchoAndAssert(delete(ECHO_PATH), "DELETE");
        }

        private void doEchoAndAssert(MockHttpServletRequestBuilder requestBuilder, String method) throws Exception {
            mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(METHOD, is(method)));
        }
    }

    @Nested
    class TestQueryString {

        @Test
        @Disabled
        void canHandleMalformedQueryString() throws Exception {
            mockMvc.perform(get(ECHO_PATH + "?abc"))    // missing "="
                    .andExpect(status().isOk())
                    // TODO: more verification
                    ;

        }

        @Test
        void echoGetAndDelete() throws Exception {
            doEchoAndAssert(queryStr -> get(ECHO_PATH + "?" + queryStr));
            doEchoAndAssert(queryStr -> delete(ECHO_PATH + "?" + queryStr));
        }

        @Test
        void echoPostPutPatch() throws Exception {
            var bodyParams = new LinkedMultiValueMap<String, String>();
            bodyParams.put("def", List.of("456"));
            bodyParams.put("jkl", List.of("789"));

            doEchoAndAssert((String qs) ->
                    post(ECHO_PATH + "?" + qs).params(bodyParams));
            doEchoAndAssert((String qs) ->
                    put(ECHO_PATH + "?" + qs).params(bodyParams));
            doEchoAndAssert((String qs) ->
                    patch(ECHO_PATH + "?" + qs).params(bodyParams));
        }

        protected void doEchoAndAssert(Function<String, MockHttpServletRequestBuilder> createRequestBuilder) throws Exception {
            var queryMap = new LinkedMultiValueMap<String, String>();
            queryMap.put("abc", List.of("123", "321"));
            queryMap.put("def", List.of("456"));
            queryMap.put("ghi", List.of(""));       // for simplicity, use empty string to denote missing value
            Function<String, MockHttpServletRequestBuilder> newBuilder = (String qs) -> {
                assertEquals("abc=123&abc=321&def=456&ghi=", qs);
                return createRequestBuilder.apply(qs);
            };
            doEchoAndAssert(newBuilder, queryMap);
        }

        protected void doEchoAndAssert(Function<String, MockHttpServletRequestBuilder> createRequestBuilder,
                                       MultiValueMap<String, String> queryMap) throws Exception {

            var queryString = queryMap.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v -> String.format("%s=%s", e.getKey(), v)))
                    .collect(Collectors.joining("&"));

            var resultAction = mockMvc.perform(createRequestBuilder.apply(queryString))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(QUERY_STRING, is(queryString)))
                    ;
            queryMap.forEach((key, value) -> {
                String path = QUERY + "." + key;
                try {
                    if (value.size() == 1) {
                        resultAction.andExpect(jsonPath(path, is(value.get(0))));
                    } else {
                        resultAction.andExpect(jsonPath(path, is(value)));
                    }
                } catch (Exception e) {
                    fail(String.format("Exception while matching jsonpath %s. Expected %s", path, value), e);
                }
            });
        }
    }

    @Nested
    class TestHeader {
        @Test
        void returnHeadersAsProvided() throws Exception {
            var request = get(ECHO_PATH)
                    .header("connection", "keep-alive")
                    .header("X-ABC", "custom header")
                    ;

            mockMvc.perform(request)
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(HEADERS + ".X-ABC", is("custom header")))
                    ;
        }

        @Test
        void someHeaderMayArriveModified() throws Exception {
            var request = get(ECHO_PATH)
                    .header("Content-Type", "application/json")
                    ;

            mockMvc.perform(request)
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(HEADERS + ".Content-Type", is("application/json;charset=UTF-8")))
                    ;
        }

        @Test
        void multiValueHeader() throws Exception {
            var request = get(ECHO_PATH)
                    .header("Accept-Encoding", "gzip", "compress", "br")
                    ;

            mockMvc.perform(request)
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(HEADERS + ".Accept-Encoding", is(List.of("gzip", "compress", "br"))))
                    ;
        }
    }

    @Nested
    class TestUrlEncodedForm {

        @Test
        void returnFormDataAsSubmitted() throws Exception {
            var paramMap = new LinkedMultiValueMap<String, String>();
            paramMap.put("abc", List.of("123", "321"));
            paramMap.put("def", List.of("456"));
            paramMap.put("ghi", List.of(""));       // for simplicity, use empty string to denote missing value

            var bodyParamString = paramMap.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v -> String.format("%s=%s", e.getKey(), v)))
                    .collect(Collectors.joining("&"));

            MockHttpServletRequestBuilder request = post(ECHO_PATH + "?abc=blah&xyz=blahblah")
                    .contentType(APPLICATION_X_WWW_FORM_URLENCODED)
                    .content(bodyParamString)
                    ;
            var resultAction = mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(BODY, is(bodyParamString)))
                    ;
            paramMap.forEach((key, value) -> {
                String path = FORM + "." + key;
                try {
                    if (value.size() == 1) {
                        resultAction.andExpect(jsonPath(path, is(value.get(0))));
                    } else {
                        resultAction.andExpect(jsonPath(path, is(value)));
                    }
                } catch (Exception e) {
                    fail(String.format("Exception while matching jsonpath %s. Expected %s", path, value), e);
                }
            });
        }
    }

    @Nested
    class TestMutiPartForm {

        @Test
        void returnFormDataAsSubmitted() throws Exception {
            var paramMap = new LinkedMultiValueMap<String, String>();
            paramMap.put("abc", List.of("123", "321"));
            paramMap.put("def", List.of("456"));
            paramMap.put("ghi", List.of(""));       // for simplicity, use empty string to denote missing value

            var paramParts = paramMap.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v -> new MockPart(e.getKey(), v.getBytes(StandardCharsets.UTF_8))))
                    .collect(Collectors.toList());
            var filePart = new MockPart("uploadFile", "file.txt", "content".getBytes(StandardCharsets.UTF_8));
            filePart.getHeaders().setContentType(MediaType.TEXT_PLAIN);

            MockHttpServletRequestBuilder request = multipart(ECHO_PATH + "?abc=blah&xyz=blahblah")
                    .part(paramParts.toArray(Part[]::new))
                    .part(filePart)
                    .contentType("multipart/form-data; boundary=--------------------------034560171062851546911824")
                    ;
            var resultAction = mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(BODY, is("<5 parts>")))
                    ;
            paramMap.forEach((key, value) -> {
                String path = FORM + "." + key;
                try {
                    if (value.size() == 1) {
                        resultAction.andExpect(jsonPath(path, is(value.get(0))));
                    } else {
                        resultAction.andExpect(jsonPath(path, is(value)));
                    }
                } catch (Exception e) {
                    fail(String.format("Exception while matching jsonpath %s. Expected %s", path, value), e);
                }
            });
            resultAction.andExpect(jsonPath(FORM + "." + filePart.getName(),
                    is("<file: file.txt, size: " + filePart.getSize() + " bytes>")));
        }
    }
}