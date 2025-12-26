
# Checkstyle+

Checkstyle+ is an extended version of [Checkstyle](https://checkstyle.org/) that integrates Large Language Models (LLMs) for advanced code style evaluation of of 9 Google Java style rules: javadoc summary fragment, implementation comment formatting, class, method, constant, non-constant, parameter, local variable and type variable names.  
It retains full compatibility with existing Checkstyle rules while introducing a new module, **`LlmStyleCheck`**, which uses modern reasoning models (e.g., OpenAI GPT, Google Gemini) to detect nuanced style and readability issues that checkstyle often miss.


## Overview

Checkstyle+ functions as a **drop-in replacement** for Checkstyle.  
It runs all of Checkstyle's native checks, but when the `LlmStyleCheck` module is enabled, it offloads 9 rules enforcement to the configured LLM (full rule description available in the prompt at `llm-checks/src/main/resources/prompt-template.txt`):

### Code Style Guidelines

| **Section** | **Subsection** | **Rule ID** | **Short Description** |
|--------------|----------------|--------------|-----------------|
| **1. Documentation** | &nbsp;&nbsp;1.1 Javadocs | 1.1.1 | Summary fragment presence and formatting in Javadoc. |
| | &nbsp;&nbsp; | 1.1.2 | Detecting when implementation comments should be Javadoc. |
| **2. Naming Conventions** | &nbsp;&nbsp;2.1 Class Names | 2.1.1 | Class names — UpperCamelCase. |
| | &nbsp;&nbsp;2.2 Method Names | 2.2.1 | Method names — lowerCamelCase. |
| | &nbsp;&nbsp;2.3 Constant Names | 2.3.1 | Constants — UPPER_SNAKE_CASE. |
| | &nbsp;&nbsp; | 2.3.2 | Non-constant field names — lowerCamelCase. |
| | &nbsp;&nbsp;2.4 Parameter Names | 2.4.1 | Parameter names — lowerCamelCase. |
| | &nbsp;&nbsp;2.5 Local Variables | 2.5.1 | Local variable names — lowerCamelCase. |
| | &nbsp;&nbsp;2.6 Type Variable Names | 2.6.1 | Type variable names — capital letters or ClassT convention. |

## Quick Start

1. **Download the JAR**

    Download the executable from: `target/checkstyle-plus.jar`

2. **Copy the Config**

    Use the example `checkstyle.xml` provided in the repository (`llm-checks\src\main\resources\checkstyle.xml`).  
    It already includes the Google Java Style checks and the `LlmStyleCheck` module.

3. **Run the Tool**

    Run Checkstyle+ on your Java files or folders:
    `java -jar checkstyle-plus.jar src/main/java`


## Features

- **Universal Model Support:** Works with multiple providers via configurable API endpoints (OpenAI, Gemini, Claude, Mistral, Ollama, vLLM, etc.).  
- **Automatic Rule Coordination:** Disables overlapping built-in rules (e.g., `MethodName`, `TypeName`, `SummaryJavadoc`) when the LLM check is active.  
- **Fully Configurable:** All LLM properties are defined directly in `checkstyle.xml`.  
- **Cache and Reuse:** Responses are cached using SHA-256 hashes to reduce redundant API calls. Use `rm -rf ~/.llm-checks-cache`(macOS/Linux) or `Remove-Item -Recurse -Force "$env:USERPROFILE.llm-checks-cache"`(Windows) to clear cache.
- **Seamless CLI Integration:** Compatible with existing Checkstyle commands and CI pipelines.  
- **Fallback:** If no LLM is configured, the tool defaults to standard Checkstyle behavior.


## Installation

### 1. Prerequisites

Before building Checkstyle+, ensure that you have the following installed:

- **Java 21** or later  
- **Maven 3.8+**  

### 2. Build from Source

Clone the repository and build using Maven:

git clone https://github.com/yourusername/checkstyle-plus.git  
cd checkstyle-plus  
mvn clean package  

Upon successful compilation, Maven will generate two JAR files in the target/ directory:

`target/checkstyle-plus.jar` 

`target/checkstyle-plus-1.0.0.jar`  

- `checkstyle-plus.jar` – the shaded executable JAR (recommended for use)  
- `checkstyle-plus-1.0.0.jar` – the non-shaded build (for developers who want to manage dependencies manually)


### 3. Run Checkstyle+

You can invoke Checkstyle+ directly from the command line:

java -jar checkstyle-plus.jar -c checkstyle.xml src/main/java  

The -c flag specifies the configuration file to use (default: checkstyle.xml in the current directory).  
You can analyze a single file, multiple files, or an entire project directory.

Example:

java -jar checkstyle-plus.jar -c checkstyle.xml MyFile.java  

or to recursively check a folder:

java -jar checkstyle-plus.jar -c checkstyle.xml src/


### 4. Configuration File

Checkstyle+ uses the same configuration structure as standard Checkstyle but includes one additional module:

Add this module to your own configuration file or use the example one provided in `llm-checks/src/main/resources`

```xml
<module name="com.checkstyleplus.LlmStyleCheck">
  <property name="apiKey" value="YOUR_API_KEY_HERE"/>
  <property name="endpoint" value="https://api.openai.com/v1/chat/completions"/>
  <property name="model" value="gpt-4o"/>
  <property name="temperature" value="1.0"/>
  <property name="showWarnings" value="true"/>
  <property name="enabled" value="true"/>
</module>
```
Below is a summary of the available properties:

| Property | Required | Description |
|-----------|-----------|-------------|
| **apiKey** | Required | API key used to authenticate requests to the model provider (e.g., OpenAI, Google, Anthropic, Mistral, or local endpoint). |
| **endpoint** | Required | The base URL of the model API endpoint. This determines the type of model client used (e.g., OpenAI, Gemini, Claude, Mistral, Ollama, vLLM). |
| **model** | Optional | Some APIs (like OpenAI) require the model name to be specified explicitly (e.g., `gpt-4o`), while others (like Gemini or local endpoints) embed the model name in the endpoint itself.|
| **temperature** | Optional | Controls randomness in model output (range: 0–2). Default: `1.0`. |
| **maxOutputTokens** | Optional | The maximum number of tokens the model is allowed to return per request. By default, this uses the model’s maximum token limit. |
| **thinkingTokens** | Optional | Defines the number of tokens allocated for the model’s internal reasoning phase (used by reasoning-capable models like Gemini 2.5 Pro). |
| **showWarnings** | Optional | When set to `false`, Checkstyle+ suppresses non-critical recommendations, showing only strict guideline violations. Default: `true`. |
| **enabled** | Optional | Toggles the Checkstyle+ module on or off without removing it from the configuration. Useful for cost-controlled or comparative experiments. Default: `true`. |


### 5. Verifying Installation

To verify that Checkstyle+ is correctly installed:

`java -jar checkstyle-plus.jar -v `

You should see the Checkstyle+ and Checkstyle version.


### 6. Optional: Global Alias

To simplify usage, you can create a global alias:

`alias checkstyleplus='java -jar /path/to/checkstyle-plus.jar'` 

Then run it anywhere as:

`checkstyleplus -c checkstyle.xml src/`


## License

This project extends Checkstyle
 and is distributed under the same Apache 2.0 license.

## Acknowledgments

Checkstyle
 for the base style enforcement.

Google Java Style Guide for rule definitions.

LLM API providers for extended semantic analysis capabilities.