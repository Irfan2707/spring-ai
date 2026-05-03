package com.spring.ai.demo.commons.toolCalling;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// AppTools.java
// This class holds all @Tool methods — think of this as your "toolbox"
// The LLM reads the @Tool descriptions to decide WHEN and HOW to call each one
@Component
public class AppTools {

  private final RestTemplate restTemplate;

  public AppTools(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // ── TOOL 1: Get Weather ───────────────────────────────────────────────────
  // WHY: LLM has no real-time data. User asks "should I carry umbrella in Bengaluru?"
  //      LLM sees this tool, decides to call it with city="Bengaluru"
  //      Gets back "28°C, Sunny" → answers "No umbrella needed!"
  // The description is CRITICAL — LLM reads this to decide if it should call the tool
  @Tool(
      description =
          "Get current weather for a given city. Use this when user asks about weather, temperature, rain, or whether to carry an umbrella.")
  public String getWeather(
      @ToolParam(description = "Name of the city, e.g. Bengaluru, Mumbai, London") String city) {
    // Using wttr.in — free, no API key needed
    // ?format=3 returns "Bengaluru: ⛅ +28°C" — clean single line
    String url = "https://wttr.in/" + city + "?format=3";
    try {
      return restTemplate.getForObject(url, String.class);
    } catch (Exception e) {
      return "Could not fetch weather for " + city;
    }
  }

  // ── TOOL 2: Currency Converter ────────────────────────────────────────────
  // WHY: LLM doesn't know live exchange rates. User asks "convert 100 USD to INR"
  //      LLM calls this tool with from="USD", to="INR", amount=100
  //      Gets back "100 USD = 8350.00 INR" → gives accurate answer
  @Tool(
      description =
          "Convert currency from one type to another. Use when user asks to convert money or asks about exchange rates.")
  public String convertCurrency(
      @ToolParam(description = "Source currency code, e.g. USD, EUR, GBP") String from,
      @ToolParam(description = "Target currency code, e.g. INR, JPY, AED") String to,
      @ToolParam(description = "Amount to convert as a number") double amount) {
    // Using open.er-api.com — free tier, no key needed for basic use
    String url = "https://open.er-api.com/v6/latest/" + from;
    try {
      // API returns JSON with rates map — parse just what we need
      Map response = restTemplate.getForObject(url, Map.class);
      Map<String, Double> rates = (Map<String, Double>) response.get("rates");
      double rate = rates.get(to.toUpperCase());
      double converted = amount * rate;
      return String.format("%.2f %s = %.2f %s (rate: %.4f)", amount, from, converted, to, rate);
    } catch (Exception e) {
      return "Could not convert " + from + " to " + to;
    }
  }

  // ── TOOL 3: Random Joke Fetcher ───────────────────────────────────────────
  // WHY: Shows "taking action" pattern — LLM fetches live content it doesn't know
  //      User: "Tell me a programming joke"
  //      LLM calls getJoke("programming") → returns fresh joke from API
  // Without tool: LLM tells the same cached jokes from training data
  // With tool: always fresh from the joke API
  @Tool(
      description =
          "Fetch a random joke. Use when user asks for a joke, wants to laugh, or asks for something funny. Category can be 'programming', 'general', 'pun'.")
  public String getJoke(
      @ToolParam(description = "Joke category: programming, general, or pun") String category) {
    // JokeAPI — free, no key needed
    String url = "https://v2.jokeapi.dev/joke/" + category + "?type=single";
    try {
      Map response = restTemplate.getForObject(url, Map.class);
      return (String) response.get("joke");
    } catch (Exception e) {
      return "Could not fetch a joke right now.";
    }
  }

  // ── TOOL 4: Current Date/Time ─────────────────────────────────────────────
  // WHY: Simplest example — LLM genuinely doesn't know today's date
  //      No external API needed, just Java time
  //      User: "What day is it?" or "Set reminder for next Monday"
  //      LLM calls this first to know today's date, then calculates
  @Tool(
      description =
          "Get the current date and time. Use when user asks what day/time it is, or needs to calculate dates like 'next Monday' or '3 days from now'.")
  public String getCurrentDateTime() {
    // Returns ISO format so LLM can do date arithmetic accurately
    return LocalDateTime.now()
        .atZone(ZoneId.of("Asia/Kolkata")) // IST timezone
        .toString();
  }
}
