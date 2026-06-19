package com.example.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Spring AI Tools — enables the LLM to call real Java methods.
 *
 * Register these with ChatClient.defaultTools(...) or per-request
 * via .tools(new ApplicationTools()).
 *
 * The @Tool annotation exposes the method to the AI model.
 * The @ToolParam annotation documents each parameter for the model.
 */
@Component
public class ApplicationTools {

    /**
     * Tool: get current date/time
     * The AI calls this when the user asks "What time is it?" etc.
     */
    @Tool(description = "Get the current date and time in ISO format")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Tool: weather lookup (stub — replace with real API call)
     */
    @Tool(description = "Get the current weather for a given city")
    public Map<String, Object> getWeather(
            @ToolParam(description = "The city name, e.g. 'Chennai' or 'London'") String city) {
        // Replace with actual weather API (e.g., OpenWeatherMap)
        return Map.of(
                "city", city,
                "temperature", "28°C",
                "condition", "Partly Cloudy",
                "humidity", "72%",
                "note", "Stub response — integrate a real weather API"
        );
    }

    /**
     * Tool: currency conversion (stub)
     */
    @Tool(description = "Convert an amount from one currency to another")
    public Map<String, Object> convertCurrency(
            @ToolParam(description = "Amount to convert") double amount,
            @ToolParam(description = "Source currency code, e.g. USD") String from,
            @ToolParam(description = "Target currency code, e.g. EUR") String to) {
        // Replace with a live exchange-rate API
        return Map.of(
                "from", from,
                "to", to,
                "inputAmount", amount,
                "convertedAmount", amount * 0.92,   // stub rate
                "note", "Stub response — integrate a real FX API"
        );
    }

    /**
     * Tool: database lookup (stub — wire to your JPA repository)
     */
    @Tool(description = "Look up a product by its SKU in the product catalogue")
    public Map<String, Object> lookupProduct(
            @ToolParam(description = "The product SKU identifier") String sku) {
        // Replace with real ProductRepository.findBySku(sku)
        return Map.of(
                "sku", sku,
                "name", "Example Product",
                "price", 49.99,
                "stock", 142,
                "note", "Stub response — wire to your ProductRepository"
        );
    }
}
