package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.RagVectorDatabaseService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RagVectorDatabaseServiceImpl implements RagVectorDatabaseService {

  @Value("classpath:/prompts/rag-system-prompt.st")
  private Resource systemMessage;

  @Value("classpath:/prompts/rag-user-prompt.st")
  private Resource userMessage;

  private final VectorStore vectorStore;
  private final ChatClient groqChatClient;

  public RagVectorDatabaseServiceImpl(
      VectorStore vectorStore,
      @Qualifier("groqChatClientWithoutChatMemory") ChatClient groqChatClient) {
    this.vectorStore = vectorStore;
    this.groqChatClient = groqChatClient;
  }

  @Override
  public void storeDataIntoVectorDatabase(List<String> companyDataList) {

    List<Document> documentList = companyDataList.stream().map(Document::new).toList();
    this.vectorStore.add(documentList);
  }

  @Override
  public String similaritySearchFromDatabase(String query) {

    //    We can also use QuestionAnswerAdvisor instead of creating context by ourselves.Spring ai
    // will do it for ourselvees.
    //    SearchRequest searchRequest = SearchRequest
    //            .builder()
    //                    .topK(5)
    //                            .similarityThreshold(.5)
    //            .query(query)
    //                                    .build();
    //
    //    List<Document> documentList = vectorStore.similaritySearch(searchRequest);
    //    List<@Nullable String> convertedDocuments =
    // documentList.stream().map(Document::getText).toList();
    //    String finalQuery = String.join("\n", convertedDocuments);

    // QuestionAnswerAdvisor automatically searches the vector store for similar documents
    // based on the query and provides them as context to the LLM. This enables RAG
    // (Retrieval-Augmented Generation)
    // without manually building the context. The advisor will:
    // 1. Search the vector database for top-3 most similar documents (topK=3)
    // 2. Filter results with similarity threshold of 0.5
    // 3. Pass the retrieved documents as context to the prompt
    // 4. Allow the LLM to generate answers based on the retrieved context
    return groqChatClient
        .prompt()
        .advisors(
            QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(3).similarityThreshold(0.5).build())
                .build())
        .user(promptUserSpec -> promptUserSpec.text(this.userMessage).param("query", query))
        .call()
        .content();
  }

  @Override
  public String retrievalAugmentationAdvisor(String query) {

    var advisor =
        RetrievalAugmentationAdvisor.builder()
            .documentRetriever(
                VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .topK(3)
                    .similarityThreshold(0.5)
                    .build())
            .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
            .build();
    return groqChatClient
        .prompt()
        .advisors(advisor)
        .user(promptUserSpec -> promptUserSpec.text(this.userMessage).param("query", query))
        .call()
        .content();
  }

  @Override
  public String retrievalAugmentationAdvisorWithQueryAugmentor(String userQuestion) {

    // ════════════════════════════════════════════════════════════
    // PHASE 1: PRE-RETRIEVAL — Transform the query BEFORE searching
    // Goal: make the query better so we find more relevant documents
    // ════════════════════════════════════════════════════════════

    // 1A. RewriteQueryTransformer
    // WHAT: Rewrites the user's raw question into a cleaner, more search-friendly query
    // WHY:  Users type sloppy queries like "tell me abt refund policy??"
    //       The transformer rewrites it to: "What is the refund policy?"
    //       Clean query → better vector similarity → better docs returned
    // EXAMPLE:
    //   Input:  "hey so like how do i cancel my order lol"
    //   Output: "How do I cancel an order?"
    QueryTransformer rewriteTransformer =
        RewriteQueryTransformer.builder().chatClientBuilder(groqChatClient.mutate()).build();

    // 1B. TranslationQueryTransformer
    // WHAT: Translates the query to the language your documents are stored in
    // WHY:  If your vector DB has English docs but the user asks in Hindi/Tamil,
    //       the embedding won't match. This normalizes everything to one language.
    // EXAMPLE:
    //   Input:  "मेरा ऑर्डर कहाँ है?" (Hindi: Where is my order?)
    //   Output: "Where is my order?" (English — same language as your docs)
    QueryTransformer translationTransformer =
        TranslationQueryTransformer.builder()
            .chatClientBuilder(groqChatClient.mutate())
            .targetLanguage("english")
            .build();

    // 1C. MultiQueryExpander
    // WHAT: Takes 1 question and generates N variations of it
    // WHY:  One query might miss relevant docs due to different wording.
    //       5 query variations cast a wider net → higher recall.
    // EXAMPLE:
    //   Input:  "refund policy"
    //   Output: [
    //     "What is the refund policy?",
    //     "How do I get my money back?",
    //     "Can I return a product?",
    //     "What are the return rules?",
    //     "How long do refunds take?"
    //   ]
    //   All 5 queries run against the vector DB, results are merged + deduplicated
    QueryExpander multiQueryExpander =
        MultiQueryExpander.builder()
            .chatClientBuilder(groqChatClient.mutate())
            .numberOfQueries(4) // generates 4 variations
            .build();

    // ════════════════════════════════════════════════════════════
    // PHASE 2: RETRIEVAL — Search the vector store
    // Goal: find the most semantically similar documents
    // ════════════════════════════════════════════════════════════

    // 2A. VectorStoreDocumentRetriever
    // WHAT: Converts the (transformed) query into an embedding vector,
    //       then does a cosine similarity search against your vector DB
    // WHY:  This is the core search step — finds semantically similar docs,
    //       not just keyword matches
    // EXAMPLE:
    //   Query embedding: [0.23, -0.81, 0.44, ...]  ← 1536-dimension vector
    //   Finds docs whose embeddings are closest in vector space
    //   Returns top-K matches (topK=5 means 5 docs)
    DocumentRetriever vectorRetriever =
        VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .topK(5) // fetch top 5 most similar docs
            .similarityThreshold(0.6) // only return docs with 60%+ similarity
            .build();

    // 2B. ConcatenationDocumentJoiner
    // WHAT: When you have results from multiple queries (from MultiQueryExpander),
    //       this merges all the document lists into one, removing duplicates
    // WHY:  MultiQueryExpander runs N queries → N lists of docs.
    //       You need to combine them into a single clean context.
    // EXAMPLE:
    //   Query 1 results: [doc1, doc2, doc3]
    //   Query 2 results: [doc2, doc4, doc5]   ← doc2 is duplicate
    //   Query 3 results: [doc1, doc5, doc6]   ← doc1, doc5 are duplicates
    //   After ConcatenationDocumentJoiner: [doc1, doc2, doc3, doc4, doc5, doc6]
    DocumentJoiner documentJoiner = new ConcatenationDocumentJoiner();

    // ════════════════════════════════════════════════════════════
    // PHASE 3: POST-RETRIEVAL — Process docs AFTER fetching them
    // Goal: filter noise, re-rank by relevance, trim context window
    // ════════════════════════════════════════════════════════════

    // 3A. DocumentPostProcessor (custom example below)
    // WHAT: Filters or transforms documents after retrieval
    // WHY:  Vector search isn't perfect — it may return loosely related docs.
    //       Post-processing removes noise before sending to the LLM.
    // EXAMPLE USE CASES:
    //   - Remove docs below a score threshold
    //   - Remove duplicate/redundant content
    //   - Trim docs that are too long (token budget management)
    //   - Inject metadata (add "Source: HR Policy v2" prefix to each doc)
    //   - Filter docs by date (only show docs from last 1 year)
    DocumentPostProcessor postProcessor =
        (query, documents) ->
            documents.stream()
                .filter(
                    doc -> {
                      String content = doc.getText();
                      return content != null && content.length() > 50;
                    })
                .map(
                    doc -> {
                      String source = (String) doc.getMetadata().getOrDefault("source", "unknown");
                      String enriched = "[Source: " + source + "]\n" + doc.getText();
                      return new Document(enriched, doc.getMetadata());
                    })
                .toList();

    // ════════════════════════════════════════════════════════════
    // PHASE 4: GENERATION — Build the prompt and call the LLM
    // Goal: combine the cleaned context + question into a final answer
    // ════════════════════════════════════════════════════════════

    // Now wire everything together using Spring AI's RetrievalAugmentationAdvisor
    // This is the glue that connects all phases in sequence:
    // query → pre-retrieval → retrieval → post-retrieval → prompt → LLM → answer

    RetrievalAugmentationAdvisor ragAdvisor =
        RetrievalAugmentationAdvisor.builder()
            // Pre-retrieval: chain all transformers in order
            .queryTransformers(rewriteTransformer, translationTransformer)
            .queryExpander(multiQueryExpander)
            // Retrieval
            .documentRetriever(vectorRetriever)
            .documentJoiner(documentJoiner)
            // Post-retrieval
            .documentPostProcessors(postProcessor)
            .build();

    // Fire the full RAG pipeline and return the answer
    return groqChatClient.prompt().advisors(ragAdvisor).user(userQuestion).call().content();
  }
}
