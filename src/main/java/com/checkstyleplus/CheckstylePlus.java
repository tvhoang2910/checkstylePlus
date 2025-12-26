package com.checkstyleplus;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.Main;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.xml.sax.InputSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * CheckstylePlus — lightweight wrapper that runs Checkstyle with custom modules,
 * automatically disabling overlapping built-in checks when LlmStyleCheck is active.
 * Relies entirely on Checkstyle’s built-in logging system.
 */
public class CheckstylePlus {

    // Checkstyle's checks handled by LLM — auto-disabled when the custom module is active
    private static final Set<String> LLM_HANDLED_CHECKS = Set.of(
        "TypeName",
        "MethodName",
        "ConstantName",
        "MemberName",
        "ParameterName",
        "LocalVariableName",
        "AbbreviationAsWordInName",
        "SummaryJavadoc"
    );

    public static void main(String[] args) throws Exception {

        if (args.length == 1 && ("-v".equals(args[0]) || "--version".equals(args[0]))) {
            System.out.println("Checkstyle+ version 1.0.0");
            String checkstyleVersion = com.puppycrawl.tools.checkstyle.Checker.class
                    .getPackage()
                    .getImplementationVersion();
            System.out.println("Using Checkstyle " + (checkstyleVersion != null ? checkstyleVersion : "12.1.0"));
            return;
        }
        
        // If the user explicitly passed a config, delegate to Checkstyle's Main.
        if (hasConfigFlag(args)) {
            Main.main(args);
            return;
        }

        Path cfg = Paths.get("checkstyle.xml");
        if (!Files.exists(cfg)) {
            printMissingConfigHelp();
            System.exit(2);
        }

        Configuration configuration = ConfigurationLoader.loadConfiguration(
            new InputSource(cfg.toUri().toString()),
            new PropertiesExpander(System.getProperties()),
            ConfigurationLoader.IgnoredModulesOptions.EXECUTE
        );

        // Apply filtering if LlmStyleCheck is enabled
        Configuration effectiveConfig = maybeFilterChecks(configuration);

        Checker checker = new Checker();
        checker.setModuleClassLoader(Checker.class.getClassLoader());
        checker.addListener(new DefaultLogger(
            System.out, AbstractAutomaticBean.OutputStreamOptions.NONE,
            System.err, AbstractAutomaticBean.OutputStreamOptions.NONE
        ));

        checker.configure(effectiveConfig);

        List<File> filesToCheck = collectFileArgs(args);
        int errors = checker.process(filesToCheck);

        checker.destroy();

        if (errors > 0) {
            System.exit(1);
        }
    }

    private static boolean hasConfigFlag(String[] args) {
        for (String s : args) {
            if ("-c".equals(s) || "--config".equals(s)) return true;
        }
        return false;
    }

    private static List<File> collectFileArgs(String[] args) {
        List<File> files = new ArrayList<>();
        for (String a : args) {
            files.add(new File(a));
        }
        return files;
    }

    private static boolean containsEnabledLlmStyleCheck(Configuration config) {
        if ("LlmStyleCheck".equals(config.getName()) ||
            "com.checkstyleplus.LlmStyleCheck".equals(config.getName())) {
            String enabledAttr = null;
            try {
                enabledAttr = config.getProperty("enabled");
            } catch (Exception ignored) {
                // If property is missing or can't be read, default to enabled
            }
            return enabledAttr == null || Boolean.parseBoolean(enabledAttr);
        }
        for (Configuration child : config.getChildren()) {
            if (containsEnabledLlmStyleCheck(child)) return true;
        }
        return false;
    }

    private static Configuration maybeFilterChecks(Configuration config) {
        boolean llmEnabled = containsEnabledLlmStyleCheck(config);
        if (!llmEnabled) {
            return config;
        }
        return new FilteredConfiguration(config, LLM_HANDLED_CHECKS);
    }

    private static void printMissingConfigHelp() {
        System.err.println(
            "Missing checkstyle.xml\n" +
            "Place an external checkstyle.xml next to the JAR or pass it with -c.\n" +
            "\nExamples:\n" +
            "  java -jar checkstyle-plus.jar -c path/to/checkstyle.xml MyFile.java\n" +
            "  java -jar checkstyle-plus.jar MyFile.java\n" +
            "\nInside your checkstyle.xml, you can configure LLM properties, e.g.:\n" +
            "  <module name=\"com.checkstyleplus.LlmStyleCheck\">\n" +
            "      <property name=\"apiKey\" value=\"YOUR_KEY_HERE\"/>\n" +
            "      <property name=\"endpoint\" value=\"https://api.openai.com/v1/chat/completions\"/>\n" +
            "      <property name=\"model\" value=\"gpt-4o\"/>\n" +
            "      <property name=\"enabled\" value=\"true\"/>\n" +
            "  </module>"
        );
    }
}
