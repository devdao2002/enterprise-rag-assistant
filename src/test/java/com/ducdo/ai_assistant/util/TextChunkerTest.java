package com.ducdo.ai_assistant.util;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void chunkText_withOverlap_shouldReturnCorrectChunks() {
        String text = "1234567890";
        // chunk size 4, overlap 2
        // chunk 1: "1234" (start 0) -> next start 0 + (4 - 2) = 2
        // chunk 2: "3456" (start 2) -> next start 2 + (4 - 2) = 4
        // chunk 3: "5678" (start 4) -> next start 4 + (4 - 2) = 6
        // chunk 4: "7890" (start 6) -> next start 6 + (4 - 2) = 8
        // end == 10, loop breaks avoiding redundant "90" chunk.
        List<String> chunks = TextChunker.chunkText(text, 4, 2);

        assertThat(chunks).hasSize(4);
        assertThat(chunks).containsExactly("1234", "3456", "5678", "7890");
    }

    @Test
    void chunkText_textSmallerThanChunkSize_shouldReturnOneChunk() {
        String text = "123";
        List<String> chunks = TextChunker.chunkText(text, 5, 2);

        assertThat(chunks).hasSize(1);
        assertThat(chunks).containsExactly("123");
    }

    @Test
    void chunkText_exactChunkSize_shouldReturnOneChunk() {
        String text = "12345";
        List<String> chunks = TextChunker.chunkText(text, 5, 2);

        assertThat(chunks).hasSize(1);
        assertThat(chunks).containsExactly("12345");
    }

    @Test
    void chunkText_emptyText_shouldReturnEmptyList() {
        String text = "";
        List<String> chunks = TextChunker.chunkText(text, 5, 2);

        assertThat(chunks).isEmpty();
    }
}
