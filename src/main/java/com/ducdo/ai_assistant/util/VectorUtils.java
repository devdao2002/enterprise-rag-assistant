package com.ducdo.ai_assistant.util;

public class VectorUtils {

    public static String toPgVector(float[] embedding) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}