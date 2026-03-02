package com.ducdo.ai_assistant.integration;

import com.ducdo.ai_assistant.repository.ChunkProjection;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import com.ducdo.ai_assistant.repository.QueryLogRepository;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import com.ducdo.ai_assistant.service.SandboxService;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "runIntegrationTests", matches = "true")
class RagFlowE2ETest {

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DocumentRepository documentRepository;

  @MockitoBean
  private DocumentChunkRepository chunkRepository;

  @MockitoBean
  private QueryLogRepository queryLogRepository;

  @MockitoBean
  private SandboxService sandboxService;

  @MockitoBean
  private SandboxResolver sandboxResolver;

  private UUID tenantId;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.ai.openai.base-url", wireMock::baseUrl);
    registry.add("spring.ai.openai.api-key", () -> "test-key");
  }

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    when(sandboxResolver.resolve(any())).thenReturn(tenantId);
    when(sandboxService.isValid(any())).thenReturn(true);

    ChunkProjection projection = mock(ChunkProjection.class);
    when(projection.getContent()).thenReturn("Spring Boot makes testing easy.");
    when(projection.getDocumentId()).thenReturn(UUID.randomUUID());
    when(projection.getPageNumber()).thenReturn(1);
    when(projection.getDocumentName()).thenReturn("spring-guide.pdf");

    when(chunkRepository.findTopKWithMetadata(any(), any(), anyInt()))
        .thenReturn(List.of(projection));
  }

  @Test
  void completeRagFlow_uploadThenAsk() throws Exception {
    // 1. Mock OpenAI Embedding API Response
    wireMock.stubFor(post(urlEqualTo("/v1/embeddings"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                  "data": [
                    {
                      "embedding": [0.1, 0.2, 0.3]
                    }
                  ]
                }
                """)));

    // 2. Mock OpenAI Chat API Response for Streaming
    wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/event-stream")
            .withBody("data: {\"choices\": [{\"delta\": {\"content\": \"Spring Boot \"}}]}\n\n" +
                "data: {\"choices\": [{\"delta\": {\"content\": \"makes testing easy.\"}}]}\n\n" +
                "data: [DONE]\n\n")));

    // 3. Upload Document
    PDDocument dummyPdf = new PDDocument();
    dummyPdf.addPage(new PDPage());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    dummyPdf.save(baos);
    dummyPdf.close();
    byte[] validPdfBytes = baos.toByteArray();

    MockMultipartFile file = new MockMultipartFile(
        "file", "spring-guide.pdf", MediaType.APPLICATION_PDF_VALUE, validPdfBytes);

    mockMvc.perform(multipart("/api/documents/upload")
        .file(file)
        .param("sandboxId", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"status\":\"PROCESSING\"")))
        .andExpect(content().string(containsString("\"documentId\"")));

    // Wait a bit for async processing if any, but our flow is likely sync right now
    // Thread.sleep(100);

    // 4. Ask Question
    mockMvc.perform(get("/api/ask/stream")
        .param("sandboxId", tenantId.toString())
        .param("question", "What does Spring Boot do?"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
        .andReturn();

    // Since this is SseEmitter, we wait for processing in the background.
    // Explicit streaming payload verification is tested tightly in RagServiceTest,
    // so here we confirm the integration layer correctly wires to start the SSE
    // stream.

    // Wait a brief moment to allow the asynchronous RagService executing in a
    // separate thread
    // to actually make the HTTP call to the mocked Chat API.
    Thread.sleep(1000);

    // 5. Verify WireMock interactions
    wireMock.verify(postRequestedFor(urlEqualTo("/v1/embeddings")));
    // Since the prompt uses retrieved documents, and we just ingested one, a chat
    // completion should trigger
    wireMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions")));
  }
}
