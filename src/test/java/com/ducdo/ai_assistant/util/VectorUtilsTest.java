package com.ducdo.ai_assistant.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VectorUtilsTest {

    @Test
    void toPgVector_shouldFormatFloatArrayCorrectly() {
        float[] embedding = { 0.1f, -0.2f, 0.35f, 1.0f };
        String pgVector = VectorUtils.toPgVector(embedding);

        assertThat(pgVector).isEqualTo("[0.1,-0.2,0.35,1.0]");
    }

    @Test
    void toPgVector_singleElement_shouldFormatCorrectly() {
        float[] embedding = { 0.5f };
        String pgVector = VectorUtils.toPgVector(embedding);

        assertThat(pgVector).isEqualTo("[0.5]");
    }

    @Test
    void toPgVector_emptyArray_shouldReturnEmptyBrackets() {
        float[] embedding = {};
        String pgVector = VectorUtils.toPgVector(embedding);

        assertThat(pgVector).isEqualTo("[]");
    }
}
