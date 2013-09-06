package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.location.ContextClassifier.ClassificationMode;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;

public class ContextClassifierTest {

    @Test
    public void testContextClassifier() {
        Tagger tagger = new StringTagger();
        ContextClassifier classifier = new ContextClassifier(ClassificationMode.ISOLATED);

        String text = "The coast of east Sri Lanka is nice. I have been in Sri Lanka.";
        List<? extends Annotation> annotations = tagger.getAnnotations(text);
        List<ClassifiedAnnotation> classifiedAnnotations = classifier.classify(annotations, text);
        assertEquals("LOC", classifiedAnnotations.get(1).getCategoryEntries().getMostLikelyCategory());
        assertEquals("LOC", classifiedAnnotations.get(2).getCategoryEntries().getMostLikelyCategory());
        // CollectionHelper.print(classifiedAnnotations);

        text = "Well, accommodation is nice in Sri Lanka.";
        annotations = tagger.getAnnotations(text);
        classifiedAnnotations = classifier.classify(annotations, text);
        assertEquals("LOC", classifiedAnnotations.get(1).getCategoryEntries().getMostLikelyCategory());
        // CollectionHelper.print(classifiedAnnotations);

    }

}