package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.Document;
import com.ducdo.ai_assistant.model.SandboxSession;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentChunkRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private SandboxRepository sandboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private Document document;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
        sandboxRepository.deleteAll();

        SandboxSession session = SandboxSession.builder()
                .tenantId(tenantId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .ipAddress("127.0.0.1")
                .build();
        sandboxRepository.save(session);

        document = Document.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("test.pdf")
                .status("COMPLETED")
                .chunks(List.of())
                .build();
        documentRepository.save(document);
    }

    private String createVector(double... values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 1536; i++) {
            if (i < values.length) {
                sb.append(values[i]);
            } else {
                sb.append("0.0");
            }
            if (i < 1535) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Test
    void findTopKContent_shouldReturnClosestMatches() {
        // Arrange
        String queryEmbedding = createVector(1.0, 0.0, 0.0);

        chunkRepository.insertChunk(UUID.randomUUID(), document.getId(), tenantId, "Chunk 1", 1, 1, 10,
                createVector(1.0, 0.0, 0.0));
        chunkRepository.insertChunk(UUID.randomUUID(), document.getId(), tenantId, "Chunk 2", 2, 1, 10,
                createVector(0.9, 0.1, 0.0));
        chunkRepository.insertChunk(UUID.randomUUID(), document.getId(), tenantId, "Chunk 3", 3, 1, 10,
                createVector(0.0, 1.0, 0.0));

        // Act
        List<String> results = chunkRepository.findTopKContent(tenantId, queryEmbedding, 2);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo("Chunk 1");
        assertThat(results.get(1)).isEqualTo("Chunk 2");
    }

    @Test
    void deleteByDocumentId_shouldRemoveOnlySpecifiedChunks() {
        // Arrange
        chunkRepository.insertChunk(UUID.randomUUID(), document.getId(), tenantId, "Chunk 1", 1, 1, 10,
                createVector(1.0));

        Document doc2 = Document.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("other.pdf")
                .status("COMPLETED")
                .chunks(List.of())
                .build();
        documentRepository.save(doc2);

        chunkRepository.insertChunk(UUID.randomUUID(), doc2.getId(), tenantId, "Chunk 2", 1, 1, 10, createVector(1.0));

        // Act
        chunkRepository.deleteByDocument_Id(document.getId());

        // Assert
        List<UUID> remainingDocIds = jdbcTemplate.queryForList("SELECT document_id FROM document_chunks", UUID.class);
        assertThat(remainingDocIds).hasSize(1);
        assertThat(remainingDocIds.get(0)).isEqualTo(doc2.getId());
    }
}
