package com.civicpulse.engine.duplicate;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TextSimilarityService {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
            "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
            "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once",
            "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own", "same", "she",
            "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them",
            "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
            "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself"
    );

    /**
     * Calculates combined textual similarity between two complaint items (title and description).
     *
     * @param title1 Title of first complaint
     * @param desc1 Description of first complaint
     * @param title2 Title of second complaint
     * @param desc2 Description of second complaint
     * @return Similarity score in [0.0, 1.0]
     */
    public double calculateSimilarity(String title1, String desc1, String title2, String desc2) {
        double titleJaccard = computeJaccardSimilarity(title1, title2);
        double titleTrigram = computeTrigramSimilarity(title1, title2);
        double titleScore = Math.max(titleJaccard, titleTrigram);

        String full1 = (title1 != null ? title1 : "") + " " + (desc1 != null ? desc1 : "");
        String full2 = (title2 != null ? title2 : "") + " " + (desc2 != null ? desc2 : "");
        double fullTextJaccard = computeJaccardSimilarity(full1, full2);

        // Score balances strongest title signal with full report content
        double combined = (0.65 * titleScore) + (0.35 * fullTextJaccard);
        return Math.round(combined * 100.0) / 100.0;
    }

    /**
     * Word-level Jaccard similarity: |A ∩ B| / |A ∪ B| after stopword removal.
     */
    public double computeJaccardSimilarity(String text1, String text2) {
        Set<String> tokens1 = tokenizeAndClean(text1);
        Set<String> tokens2 = tokenizeAndClean(text2);

        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0;
        }
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Trigram similarity (Sørensen–Dice coefficient on character 3-grams) for typo tolerance.
     */
    public double computeTrigramSimilarity(String text1, String text2) {
        String clean1 = normalize(text1);
        String clean2 = normalize(text2);

        if (clean1.equals(clean2)) {
            return 1.0;
        }
        if (clean1.length() < 3 || clean2.length() < 3) {
            return clean1.equalsIgnoreCase(clean2) ? 1.0 : 0.0;
        }

        Set<String> trigrams1 = extractTrigrams(clean1);
        Set<String> trigrams2 = extractTrigrams(clean2);

        int totalTrigrams = trigrams1.size() + trigrams2.size();
        if (totalTrigrams == 0) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(trigrams1);
        intersection.retainAll(trigrams2);

        return (2.0 * intersection.size()) / totalTrigrams;
    }

    private Set<String> tokenizeAndClean(String text) {
        if (text == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2 && !STOPWORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return " " + text.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim() + " ";
    }

    private Set<String> extractTrigrams(String text) {
        Set<String> trigrams = new HashSet<>();
        for (int i = 0; i <= text.length() - 3; i++) {
            trigrams.add(text.substring(i, i + 3));
        }
        return trigrams;
    }
}
