package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotated;

// FIXME this step must be done after person name detection!
class EntityPreprocessor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityPreprocessor.class);

    public static final Function<Annotated, String> ANNOTATION_TO_STRING = new Function<Annotated, String>() {
        @Override
        public String compute(Annotated input) {
            return input != null ? input.getValue() : null;
        }
    };

    /**
     * <p>
     * Tokenize into paragraphs. Paragraphs are assumed to be separated by at least two newline characters.
     * </p>
     * 
     * @param text The text to tokenize.
     * @return A {@link List} with paragraphs.
     */
    private static List<String> tokenizeParagraphs(String text) {
        Validate.notNull(text, "text must not be null");
        return Arrays.asList(text.split("\n{2,}"));
    }

    public static Map<String, String> correctAnnotations(String text, Map<String, Double> caseDictionary) {
        // Annotations annotations = StringTagger.getTaggedEntities(text);

        // CollectionHelper.print(annotations);
        // System.out.println("------");

        List<String> paragraphs = tokenizeParagraphs(text);
        // CollectionHelper.print(paragraphs);
        List<String> sentences = CollectionHelper.newArrayList();
        for (String paragraph : paragraphs) {
            List<String> currentSentences = Tokenizer.getSentences(paragraph);
            sentences.addAll(currentSentences);
        }

        List<String> tokens = Tokenizer.tokenize(text);
        // List<String> sentences = Tokenizer.getSentences(cleanText);

        List<Annotated> sentenceBeginAnnotations = CollectionHelper.newArrayList();
        List<Annotated> inSentenceAnnotations = CollectionHelper.newArrayList();

        for (String sentence : sentences) {

            // System.out.println(sentence);
            // System.out.println("/////");

            Annotations<ContextAnnotation> sentenceAnnotations = StringTagger.getTaggedEntities(sentence);
            for (Annotated annotation : sentenceAnnotations) {
                if (annotation.getStartPosition() == 0) {
                    sentenceBeginAnnotations.add(annotation);
                } else {
                    inSentenceAnnotations.add(annotation);
                }

                // XXX experimental
                // if (StringHelper.containsWord("of", annotation.getEntity())) {
                // System.out.println("**** 'of' anntation: " + annotation);
                // }
            }
        }

        // System.out.println("Sentence begin:");
        // CollectionHelper.print(sentenceBeginAnnotations);

        // System.out.println("In sentence:");
        // CollectionHelper.print(inSentenceAnnotations);

        // List<String> sentenceBeginStrings = CollectionHelper.convert(sentenceBeginAnnotations, ANNOTATION_TO_STRING,
        // new ArrayList<String>());
        List<String> inSentenceStrings = CollectionHelper.convert(inSentenceAnnotations, ANNOTATION_TO_STRING,
                new ArrayList<String>());

        Set<String> lowercaseTokens = CollectionHelper.filter(tokens, new Filter<String>() {
            @Override
            public boolean accept(String item) {
                return !StringHelper.startsUppercase(item);
            }
        }, new HashSet<String>());

        // now go through all sentence begin annotations
        Set<String> toRemove = CollectionHelper.newHashSet();
        Map<String, String> toModify = CollectionHelper.newHashMap();
        for (Annotated annotation : sentenceBeginAnnotations) {
            // System.out.println("processing " + annotation);

            if (inSentenceStrings.contains(annotation.getValue())) {
                LOGGER.debug("Everything fine with " + annotation.getValue());
                continue;
            }
            String value = annotation.getValue();
            String[] tokenValues = value.split("\\s");
            if (lowercaseTokens.contains(tokenValues[0].toLowerCase())) {
                if (tokenValues.length == 1) {
                    // System.out.println("**** remove " + annotation);
                    toRemove.add(annotation.getValue());
                } else {
                    // System.out.println("**** modify " + annotation);
                    String newValue = value.substring(tokenValues[0].length() + 1);
                    for (int i = 1; i < tokenValues.length; i++) {
                        String temp = tokenValues[i];
                        // System.out.println("> " + temp);
                        if (lowercaseTokens.contains(temp.toLowerCase())) {
                            newValue = newValue.substring(Math.min(newValue.length(), temp.length() + 1));
                        } else {
                            break;
                        }
                    }
                    toModify.put(annotation.getValue(), newValue);
                    continue;
                }
            }
            if (tokenValues.length > 1) {
                String newValue = value;
                for (int i = 0; i < tokenValues.length; i++) {
                    Double ratio = caseDictionary.get(tokenValues[i].toLowerCase());
                    LOGGER.debug("ratio for " + tokenValues[i] + " = " + ratio);
                    if (ratio != null && ratio > 1.0) {
                        newValue = newValue.substring(Math.min(newValue.length(), tokenValues[i].length() + 1));
                    } else {
                        break;
                    }
                }
                if (!newValue.equals(value)) {
                    LOGGER.debug("change value by dictionary > " + newValue);
                    toModify.put(annotation.getValue(), newValue);
                }
            }
            for (String inSentenceAnnotation : inSentenceStrings) {
                if (value.endsWith(inSentenceAnnotation)) {
                    // System.err.println("Special logic here! (for " + value + ", with " + inSentenceAnnotation + ")");
                    toModify.put(value, inSentenceAnnotation);
                    continue;
                }
            }
        }

        // System.out.println("To remove:");
        // CollectionHelper.print(toRemove);

        // System.out.println("To modify:");
        // CollectionHelper.print(toModify);

        Map<String, String> ret = CollectionHelper.newHashMap();
        ret.putAll(toModify);
        for (String toRemoveEntity : toRemove) {
            ret.put(toRemoveEntity, "");
        }

        // CollectionHelper.print(ret);

        return ret;
    }

    public static void main(String[] args) {
        String rawText = FileHelper
                .readFileToString("/Users/pk/Desktop/LocationLab/LocationExtractionDataset/text2.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);

        correctAnnotations(cleanText, PalladianLocationExtractor.CASE_DICTIONARY);
    }

}
