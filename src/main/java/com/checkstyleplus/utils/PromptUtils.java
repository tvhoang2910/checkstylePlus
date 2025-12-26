package com.checkstyleplus.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for handling prompt-related operations.
 * Responsible for loading prompt templates and formatting source code.
 */
public class PromptUtils {

    /**
     * Load the prompt template text from resources.
     * This file should live in: src/main/resources/prompt-template.txt
     */
    public static String loadPromptTemplate() {
        try (InputStream is = PromptUtils.class.getClassLoader()
                .getResourceAsStream("prompt-template.txt")) {
            if (is == null) {
                System.err.println("PromptUtils: prompt-template.txt not found in classpath");
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("PromptUtils: failed to load prompt-template.txt");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Prepend line numbers to the given code string.
     * Example:
     *  1.public class Main {
     *  2.    ...
     *  }
     */
    public static String addLineNumbers(String code) {
        String[] lines = code.split("\\R");
        int width = String.valueOf(lines.length).length();  // padding for alignment
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            numbered.append(String.format("%" + width + "d", i + 1))
                    .append('.')
                    .append(lines[i])
                    .append('\n');
        }
        return numbered.toString();
    }

    /**
     * Builds the final prompt by combining the template and the code with line numbers.
     */
    public static String buildPrompt(String sourceCode) {
        return loadPromptTemplate() + "\n\nCode:\n" + addLineNumbers(sourceCode);
    }
}
