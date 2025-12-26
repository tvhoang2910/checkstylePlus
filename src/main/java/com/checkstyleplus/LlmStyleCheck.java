package com.checkstyleplus;

import com.puppycrawl.tools.checkstyle.api.*;
import com.checkstyleplus.adapters.*;
import com.checkstyleplus.utils.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * LlmStyleCheck — A custom module that uses LLMs to detect style violations for 9 guidelines.
 * It builds a prompt, sends it to a model adapter (Gemini, OpenAI, Claude, local, etc.),
 * and logs any returned violations in the Checkstyle logging system.
 */
public class LlmStyleCheck extends AbstractCheck {

    // ========================== Section Number → Tag Map ==========================
    private static final Map<String, String> SECTION_TAG_MAP = Map.ofEntries(
        // 1 - Documentation
        Map.entry("1.1.1", "SummaryJavadoc"),
        Map.entry("1.1.2", "JavadocRequired"),

        // 2 - Naming Conventions
        Map.entry("2.1.1", "ClassName"),
        Map.entry("2.2.1", "MethodName"),
        Map.entry("2.3.1", "ConstantName"),
        Map.entry("2.3.2", "MemberName"),
        Map.entry("2.4.1", "ParameterName"),
        Map.entry("2.5.1", "LocalVariableName"),
        Map.entry("2.6.1", "TypeVariableName")
    );

    // ========================== Configurable Properties ==========================
    private String apiKey = "";
    private String endpoint = "";
    private String model = null;
    private boolean enabled = true;
    private boolean showWarnings = true;
    private int llmTabWidth = 4;
    private int columnOffset = 0;
    private Double temperature = 1.0;
    private Integer seed = null;
    private Integer maxOutputTokens = null;
    private Integer thinkingTokens = null;

    // ========================== Internal State ==========================
    private List<String> currentFileLines;
    private static final Pattern FIRST_PARENS_NUMBER = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern QUOTED_IDENTIFIER   = Pattern.compile("'([A-Za-z_][A-Za-z0-9_]*)'");
    private static final Pattern BARE_IDENTIFIER     = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)");

    // ========================== Setters ==========================
    public void setApiKey(String k) { this.apiKey = k; }
    public void setEndpoint(String e) { this.endpoint = e; }
    public void setModel(String m) { this.model = m; }
    public void setEnabled(boolean b) { this.enabled = b; }
    public void setShowWarnings(boolean s) { this.showWarnings = s; }
    public void setLlmTabWidth(int w) { this.llmTabWidth = Math.max(1, w); }
    public void setColumnOffset(int o) { this.columnOffset = o; }
    public void setTemperature(double t) { this.temperature = t; }
    public void setSeed(int s) { this.seed = s; }
    public void setMaxOutputTokens(int t) { this.maxOutputTokens = t; }
    public void setThinkingTokens(int t) { this.thinkingTokens = t; }

    // ========================== Tokens ==========================
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.CLASS_DEF, TokenTypes.METHOD_DEF};
    }
    @Override public int[] getAcceptableTokens() { return getDefaultTokens(); }
    @Override public int[] getRequiredTokens() { return new int[0]; }

    // ========================== Processing ==========================
    @Override
    public void beginTree(DetailAST rootAST) {
        if (!enabled) return;
        try {
            currentFileLines = Files.readAllLines(Paths.get(getFilePath()));
            String sourceCode = String.join("\n", currentFileLines);

            // Build prompt
            String prompt = PromptUtils.buildPrompt(sourceCode);

            // Build client
            LlmClient client = LlmClientFactory.create(
                apiKey,
                endpoint,
                model,
                temperature,
                seed,
                maxOutputTokens,
                thinkingTokens
            );

            // Cache + call
            String cacheKey = CacheUtils.sha256(prompt);
            String llmReply = CacheUtils.readCache(cacheKey).orElseGet(() -> {
                try {
                    String resp = client.generateResponse(prompt);
                    if (resp != null) CacheUtils.writeCache(cacheKey, resp);
                    return resp;
                } catch (Exception e) {
                    System.err.println("LlmStyleCheck LLM call error: " + e.getMessage());
                    return null;
                }
            });

            if (llmReply != null && !llmReply.isBlank()) {
                handleLlmResponse(rootAST, llmReply);
            }

        } catch (Exception e) {
            System.err.println("LlmStyleCheck error: " + e.getMessage());
        }
    }

    @Override public void visitToken(DetailAST ast) { /* no-op */ }
    @Override public void finishTree(DetailAST rootAST) { currentFileLines = null; }

    // ========================== Response Handling ==========================
    private void handleLlmResponse(DetailAST rootAst, String llmReply) {
        String[] linesOut = llmReply.split("\\R");
        for (String raw : linesOut) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase(Locale.ROOT);
            boolean isError = lower.startsWith("[error]") || lower.startsWith("[violation]");
            boolean isWarn  = lower.startsWith("[warn]")  || lower.startsWith("[warning]");

            if (!(isError || (isWarn && showWarnings) || (!isError && !isWarn && showWarnings))) continue;

            Integer targetLine = extractLineNumber(line);
            String payload = lastParenContent(line);
            String tag = detectRecommendationTagFromText(line);
            String marker = (tag != null) ? "[" + tag + "]" : "[LLMStyle]";

            String msg = (payload != null ? payload : line) + " " + marker;

            // --- Handle Javadoc cases specially ---
            if ("JavadocRequired".equals(tag) || "SummaryJavadoc".equals(tag)) {
                String commentText = extractCommentText(targetLine);
                if (commentText != null && !commentText.isEmpty()) {
                    msg = "The comment starting with '// " + commentText + 
                          "' is used to describe the method's overall purpose, " +
                          "a Javadoc comment starting with '/**' should be used instead " + marker;
                }
                msg = escapeForJavadocCases(msg);
            }

            String ident = extractIdentifier(payload != null ? payload : line);

            // Log with proper column anchoring if possible
            if (targetLine != null) {
                DetailAST identAst = (ident != null)
                        ? AstUtils.findIdentAtLineWithText(rootAst, targetLine, ident)
                        : null;
                if (identAst != null) {
                    log(identAst, "{0}", msg);
                    continue;
                }

                Integer rawCol = findColumnRaw(targetLine, ident);
                if (rawCol != null) {
                    int visCol = toVisualColumn(safeGetLine(targetLine), rawCol, llmTabWidth);
                    log(targetLine, Math.max(0, visCol + columnOffset), "{0}", msg);
                } else {
                    log(targetLine, "{0}", msg);
                }
            } else {
                log(rootAst, "{0}", msg);
            }
        }
    }

    // ========================== Helpers ==========================
    private Integer extractLineNumber(String line) {
        Matcher m = FIRST_PARENS_NUMBER.matcher(line);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private String lastParenContent(String line) {
        int end = line.lastIndexOf(')');
        int start = line.lastIndexOf('(', end);
        return (start >= 0 && end > start) ? line.substring(start + 1, end).trim() : null;
    }

    private String extractIdentifier(String text) {
        Matcher m = QUOTED_IDENTIFIER.matcher(text);
        if (m.find()) return m.group(1);
        Matcher m2 = BARE_IDENTIFIER.matcher(text);
        String candidate = null;
        while (m2.find()) candidate = m2.group(1);
        return candidate;
    }

    private Integer findColumnRaw(int line, String identifier) {
        if (identifier == null || currentFileLines == null) return null;
        String text = currentFileLines.get(line - 1);
        Matcher m = Pattern.compile("\\b" + Pattern.quote(identifier) + "\\b").matcher(text);
        if (m.find()) return m.start();
        int idx = text.indexOf(identifier);
        return (idx >= 0) ? idx : null;
    }

    private int toVisualColumn(String line, int rawIndex, int tabW) {
        int col = 0;
        for (int i = 0; i < Math.min(rawIndex, line.length()); i++) {
            col += (line.charAt(i) == '\t') ? tabW - (col % tabW) : 1;
        }
        return col;
    }

    private String safeGetLine(int oneBasedLine) {
        return (oneBasedLine < 1 || oneBasedLine > currentFileLines.size())
                ? "" : currentFileLines.get(oneBasedLine - 1);
    }

    private String detectRecommendationTagFromText(String line) {
        if (line == null) return null;
        Matcher m = Pattern.compile("\\((\\d+(?:\\.\\d+)+)\\)").matcher(line);
        while (m.find()) {
            String section = m.group(1);
            if (SECTION_TAG_MAP.containsKey(section)) {
                return SECTION_TAG_MAP.get(section);
            }
        }
        return null;
    }

    /**
     * Extracts the comment text from the given line number if it starts with // or contains a block comment.
     */
    private String extractCommentText(Integer lineNumber) {
        if (lineNumber == null || currentFileLines == null ||
            lineNumber < 1 || lineNumber > currentFileLines.size()) {
            return null;
        }

        String line = currentFileLines.get(lineNumber - 1).trim();

        // Single-line comment
        if (line.startsWith("//")) {
            return line.substring(2).trim();
        }

        // Inline comment
        int idx = line.indexOf("//");
        if (idx >= 0) {
            return line.substring(idx + 2).trim();
        }

        // Basic block comment
        if (line.contains("/*")) {
            int start = line.indexOf("/*") + 2;
            int end = line.indexOf("*/");
            if (end > start) {
                return line.substring(start, end).trim();
            } else {
                return line.substring(start).trim();
            }
        }

        return null;
    }

    /**
     * Strong escaping for JavadocRequired / SummaryJavadoc messages.
     * These modules behave differently and often break with special characters.
     */
    private static String escapeForJavadocCases(String s) {
        if (s == null) return null;
        String escaped = s;
        escaped = escaped.replace("'", "''");
        escaped = escaped.replace("%", "%%");
        escaped = escaped.replace("{", "'{'").replace("}", "'}'");
        escaped = escaped.replace("(", "'('").replace(")", "')'");
        if (!(escaped.startsWith("'") && escaped.endsWith("'"))) {
            escaped = "'" + escaped + "'";
        }
        return escaped;
    }
}
