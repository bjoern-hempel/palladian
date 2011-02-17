/**
 *
 */
package ws.palladian.preprocessing.nlp;

import java.util.Arrays;

import org.apache.log4j.Logger;

import ws.palladian.helper.CollectionHelper;
import ws.palladian.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public abstract class AbstractSentenceDetector {

    /** the logger for this class */
    protected static final Logger LOGGER = Logger.getLogger(AbstractSentenceDetector.class);

    /** base model path */
    protected static final String MODEL_PATH = "data/models/";

    /**
     * @param args
     */
    public static void main(String[] args) {

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final LingPipeSentenceDetector lpsd = new LingPipeSentenceDetector();
        lpsd.loadDefaultModel();
        lpsd.detect("This is my sentence. This is another!");
        CollectionHelper.print(lpsd.getSentences());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());

    }

    /** holds the model. **/
    protected Object model;

    /** holds the name of the chunker. **/
    protected String name;

    /** holds the sentences. **/
    protected String[] sentences;

    /**
     * chunks a sentence and writes parts in @see {@link #chunks} and @see {@link #tokens}. Method returns
     * <code>this</code> instance of
     * AbstractSentenceDetector, to allow convenient concatenations of method
     * invocations, like: <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param sentence
     */
    public abstract AbstractSentenceDetector detect(String text);

    /**
     * chunks a senntence with given model file path and writes it into @see {@link #chunks} and @see {@link #tokens}.
     * Method returns <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param sentence
     * @param modelFilePath
     */
    public abstract AbstractSentenceDetector detect(String text, String modelFilePath);

    /**
     * @return
     */
    public final Object getModel() {
        return model;
    }

    /**
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * @return the sentences
     */
    public final String[] getSentences() {
        return Arrays.copyOf(sentences, sentences.length);
    }

    /**
     * loads the default chunker model into the chunker.Method returns <code>this</code> instance of
     * AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @return
     */
    public abstract AbstractSentenceDetector loadDefaultModel();

    /**
     * loads the chunker model into the chunker. Method returns <code>this</code> instance of AbstractSentenceDetector,
     * to allow
     * convenient concatenations of method invocations, like:
     * <code>new OpenNLPSentenceDetector().loadDefaultModel().detect(...).getTagAnnotations();</code>
     * 
     * @param modelFilePath
     * @return
     */
    public abstract AbstractSentenceDetector loadModel(String modelFilePath);

    /**
     * @param model
     */
    public final void setModel(Object model) {
        this.model = model;
    }

    /**
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * @param sentences
     *            the sentences to set
     */
    public final void setSentences(String[] sentences) {
        this.sentences = Arrays.copyOf(sentences, sentences.length);
    }

}