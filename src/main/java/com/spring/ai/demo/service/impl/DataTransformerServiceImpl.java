package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.DataTransformerService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformer.ContentFormatTransformer;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DataTransformerServiceImpl implements DataTransformerService {

  // ChatClient is needed by LLM-based enrichers (keyword + summary)
  // They internally call the LLM to generate keywords/summaries per document
  private final ChatClient groqChatClient;
  private final VectorStore vectorStore;
  private final ChatModel chatModel;

  // Explicitly tell Spring which ChatModel to use —
  // the same OpenAiChatModel that powers your groqChatClientWithoutChatMemory bea

  public DataTransformerServiceImpl(
      VectorStore vectorStore,
      @Qualifier("groqChatClientWithoutChatMemory") ChatClient groqChatClient,
      OpenAiChatModel chatModel) {
    this.vectorStore = vectorStore;
    this.groqChatClient = groqChatClient;
    this.chatModel = chatModel;
  }

  // ── TRANSFORMER 1: TextSplitter (character-based) ─────────────────────────
  // WHAT: Splits documents based on character count
  // WHY:  Simple and fast — no tokenizer needed.
  //       Use when you don't care about exact token counts,
  //       just want rough equal-sized chunks for small docs
  // EXAMPLE:
  //   Input:  1 Document with 2000 characters
  //   Output: ~4 Documents of ~500 characters each
  //           with 100-char overlap so context isn't lost at boundaries
  @Override
  public List<Document> splitByText(List<Document> documents) {
    TextSplitter splitter =
        new TextSplitter() {
          @Override
          // splitText is the core method we must implement
          // it receives the raw text and returns a list of chunk strings
          protected List<String> splitText(String text) {
            List<String> chunks = new ArrayList<>();
            int chunkSize = 500; // each chunk = 500 characters
            int overlap = 100; // last 100 chars of chunk N appear at start of chunk N+1
            // overlap prevents losing context at chunk edges

            int start = 0;
            while (start < text.length()) {
              // calculate end index — don't go past the text length
              int end = Math.min(start + chunkSize, text.length());
              // extract the chunk and add it to our list
              chunks.add(text.substring(start, end));
              // move start forward by (chunkSize - overlap)
              // so next chunk starts 100 chars before this one ended
              start += (chunkSize - overlap);
            }
            return chunks;
          }
        };
    // apply() calls splitText() on every Document and returns new Documents
    return splitter.apply(documents);
  }

  // ── TRANSFORMER 2: TokenTextSplitter ──────────────────────────────────────
  // WHAT: Splits documents based on token count (not character count)
  // WHY:  LLMs have token limits, not character limits.
  //       Token splitting is more accurate for RAG because:
  //       - "ChatGPT" = 1 token, "internationalization" = 5 tokens
  //       - Character splitting can accidentally create chunks too large for LLM
  // EXAMPLE:
  //   Input:  1 Document with 600 tokens
  //   Output: 4 Documents of ~150 tokens each, with 30-token overlap
  @Override
  public List<Document> splitByToken(List<Document> documents) {
    TokenTextSplitter splitter =
        TokenTextSplitter.builder()
            .withChunkSize(150) // target size of each chunk in tokens
            .withMinChunkSizeChars(30) // minimum overlap in chars between chunks
            // ensures context continuity at boundaries
            .withMinChunkLengthToEmbed(5) // discard any chunk shorter than 5 tokens
            // prevents storing noise like "---" or "   "
            .withMaxNumChunks(1000) // safety cap — never produce more than 1000 chunks
            // prevents runaway splitting on huge files
            .withKeepSeparator(true) // preserve sentence-ending punctuation (. ? !)
            // keeps chunks as complete sentences where possible
            .build();

    // apply() runs the splitter on every Document in the list
    return splitter.apply(documents);
  }

  // ── TRANSFORMER 3: ContentFormatTransformer ───────────────────────────────
  // WHAT: Wraps each document's content in a template string
  // WHY:  Embedding models like text-embedding-ada-002 produce better,
  //       more consistent embeddings when content follows a fixed format.
  //       "passage: ..." tells the model this is a retrieval passage.
  //       Without this, embeddings for short and long texts are less comparable.
  // EXAMPLE:
  //   Input:  Document content = "Refunds take 30 days"
  //   Output: Document content = "passage: Refunds take 30 days"
  @Override
  public List<Document> formatContent(List<Document> documents) {
    // ContentFormatter is a functional interface — lambda that takes a Document
    // and returns the formatted content string
    ContentFormatTransformer transformer =
        new ContentFormatTransformer(
            (document, metadataMode) -> "passage: " + document.getText()
            // document.getText() gets the raw content
            // we prepend "passage: " to tell the embedding model this is a retrieval passage
            );
    return transformer.apply(documents);
  }

  // ── TRANSFORMER 4: KeywordMetadataEnricher ────────────────────────────────
  // WHAT: Calls the LLM to extract N keywords from each document,
  //       then stores them in document metadata as "excerpt_keywords"
  // WHY:  Keywords in metadata enable hybrid search:
  //       vector similarity + keyword filter = much more precise results
  //       Example: search for "refund" AND similar meaning = best of both worlds
  //       Also useful for showing users what topics a document covers
  // EXAMPLE:
  //   Input:  Document("Our refund policy allows returns within 30 days of purchase")
  //   LLM extracts: ["refund", "policy", "returns", "30 days", "purchase"]
  //   Output: Same Document + metadata["excerpt_keywords"] = "refund, policy, returns, 30 days"
  // NOTE: This makes one LLM API call per document — can be slow for large batches
  @Override
  public List<Document> enrichWithKeywords(List<Document> documents) {
    KeywordMetadataEnricher enricher =
        new KeywordMetadataEnricher(
            chatModel, // .mutate() returns the Builder — required in M3
            5 // number of keywords to extract per document
            );
    return enricher.apply(documents);
  }

  // ── TRANSFORMER 5: SummaryMetadataEnricher ────────────────────────────────
  // WHAT: Calls the LLM to generate a summary of each document chunk,
  //       then stores it in metadata as "section_summary"
  // WHY:  Summaries enable "parent-child retrieval":
  //       - Embed the short summary (fast, accurate matching)
  //       - Return the full chunk to the LLM (rich context for answer generation)
  //       Also: you can show users a preview of matched documents before full load
  //       Also: adjacent chunk summaries give the LLM surrounding context
  // EXAMPLE:
  //   Input:  500-token chunk about company leave policy details
  //   LLM generates: "Covers annual leave, sick leave, carry-forward rules for employees"
  //   Output: Same Document +
  //           metadata["section_summary"]          = "Covers annual leave..."
  //           metadata["previous_section_summary"] = "...intro to HR policies"
  //           metadata["next_section_summary"]     = "...overtime and compensation"
  // NOTE: Makes one LLM API call per document — use after splitting, not before
  //       (you want summaries of small chunks, not entire raw pages)
  @Override
  public List<Document> enrichWithSummary(List<Document> documents) {
    SummaryMetadataEnricher enricher =
        new SummaryMetadataEnricher(
            chatModel, // LLM client — used to generate the summary text
            List.of(
                // PREVIOUS: adds summary of the previous chunk as metadata
                // WHY: gives the LLM context about what came before this chunk
                SummaryMetadataEnricher.SummaryType.PREVIOUS,

                // NEXT: adds summary of the next chunk as metadata
                // WHY: gives the LLM context about what comes after this chunk
                // Together, PREVIOUS + NEXT give the LLM a "window" of context
                // around the retrieved chunk — improves answer quality significantly
                SummaryMetadataEnricher.SummaryType.NEXT));
    // apply() processes documents sequentially so it can access prev/next chunks
    return enricher.apply(documents);
  }

  // ── APPLY ALL: Full transformer pipeline in correct order ─────────────────
  // WHY ORDER MATTERS:
  //   1. Split FIRST  — cheaper to enrich small chunks than large raw pages
  //   2. Format SECOND — standardize content before LLM sees it
  //   3. Keywords THIRD — extract keywords from clean formatted chunks
  //   4. Summary LAST  — summarize after splitting so summaries are chunk-sized
  //                      (summarizing a 50-page doc = bad, summarizing a chunk = good)
  @Override
  public List<Document> applyAll(List<Document> documents) {
    List<Document> split = splitByToken(documents); // 1. split into chunks
    List<Document> formatted = formatContent(split); // 2. format each chunk
    List<Document> keywords = enrichWithKeywords(formatted); // 3. add keywords
    List<Document> summaries = enrichWithSummary(keywords); // 4. add summaries
    return summaries;
  }
}
