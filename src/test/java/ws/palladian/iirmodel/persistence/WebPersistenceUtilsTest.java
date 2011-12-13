/**
 * Created on: 11.11.2011 17:15:52
 */
package ws.palladian.iirmodel.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.Label;
import ws.palladian.iirmodel.LabelType;
import ws.palladian.iirmodel.Labeler;

/**
 * <p>
 * Tests whether the persistence layer necessary for the Effingo web presentation works correctly.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 3.0.0
 * @version 1.0.0
 */
public final class WebPersistenceUtilsTest {
    private static final String TEST_PERSISTENCE_UNIT_NAME = System.getProperty("persistenceunitname");
    private WebPersistenceUtils objectOfClassUnderTest;
    private EntityManagerFactory factory;

    // fixture
    private LabelType questionType;
    private LabelType answerType;
    private Label questionLabel01;
    private Label questionLabel02;
    private Label answerLabel01;
    private Author author1;
    private Item item1;
    private Author author2;
    private Item item2;
    private Author author3;
    private Item item3;
    private Author author4;
    private Item item4;

    @Before
    public void setUp() throws Exception {
        factory = Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT_NAME);
        objectOfClassUnderTest = new WebPersistenceUtils(factory.createEntityManager());
        questionType = new LabelType("QUESTION");
        answerType = new LabelType("ANSWER");

        author1 = new Author("testAuthor1", "http://www.test.de/");
        item1 = new Item("item1", author1, "http://www.test.de/test", "item1", new Date(), new Date(), "text1");
        author2 = new Author("testAuthor2", "http://www.test.de/");
        item2 = new Item("item2", author2, "http://www.test.de/test", "item2", new Date(), new Date(), "text2");
        author3 = new Author("testAuthor3", "http://www.test.de/");
        item3 = new Item("item3", author3, "http://www.test.de/test", "item3", new Date(), new Date(), "text3");
        author4 = new Author("testAuthor4", "http://www.test.de/");
        item4 = new Item("item4", author4, "http://www.test.de/test", "item4", new Date(), new Date(), "text4");

        ModelPersistenceLayer persistenceLayer = new ModelPersistenceLayer(factory.createEntityManager());
        persistenceLayer.saveItem(item1);
        persistenceLayer.saveItem(item2);
        persistenceLayer.saveItem(item3);
        persistenceLayer.saveItem(item4);

        persistenceLayer.shutdown();

        questionLabel01 = new Label(item1, questionType, "");
        questionLabel02 = new Label(item2, questionType, "");
        answerLabel01 = new Label(item3, answerType, "");
    }

    /**
     * <p>
     * Closes the connection to the in memory database thus resetting the whole data storage.
     * </p>
     * 
     * @throws Exception If some error occurs. Fails the test.
     */
    @After
    public void tearDown() throws Exception {
        objectOfClassUnderTest.shutdown();
        objectOfClassUnderTest = null;
        factory.close();
        factory = null;
        System.gc();
    }

    /**
     * <p>
     * Tests if saving of {@link Label}s with different {@link LabelType}s is successful or not.
     * </p>
     * 
     * @throws Exception If some error occurs. Fails the test.
     */
    @Test
    public void testCountLabelTypes() throws Exception {
        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabelType(answerType);
        objectOfClassUnderTest.saveLabel(answerLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        Map<String, String> result = objectOfClassUnderTest.countLabeledItemsByType();
        assertThat(result.size(), is(2));
        assertThat(result.get("ANSWER"), is("1"));
        assertThat(result.get("QUESTION"), is("2"));
    }

    /**
     * <p>
     * Tests whether saving a {@link Labeler} containing all {@code Label}s from the fixture is successful and checks by
     * loading that {@code Labeler} again to check the saved content.
     * </p>
     * 
     * @throws Exception If some error occurs. Fails the test.
     */
    @Test
    public void testSaveLabelers() throws Exception {
        Labeler labeler = new Labeler("test");
        labeler.addLabel(answerLabel01);
        labeler.addLabel(questionLabel01);
        labeler.addLabel(questionLabel02);
        objectOfClassUnderTest.saveLabelType(answerType);
        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabel(answerLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);

        objectOfClassUnderTest.saveLabeler(labeler);
        Labeler result = objectOfClassUnderTest.loadLabeler("test");
        assertThat(result, is(notNullValue()));
        assertThat(result.getName(), is("test"));
        assertThat(result.getLabels().size(), is(3));
        assertThat(result.getLabels(), hasItem(answerLabel01));
        assertThat(result.getLabels(), hasItem(questionLabel02));
        assertThat(result.getLabels(), hasItem(questionLabel01));
    }

    /**
     * <p>
     * Tests whether random loading of non self labeled items is working correctly.
     * </p>
     * 
     * @throws Exception Should not happen but fails the test on any unexpected error.
     * @see WebPersistenceUtils#getNextNonSelfLabeledItem(Labeler)
     */
    @Test
    public void testLoadRandomNonSelfLabeledItem() throws Exception {
        Labeler labeler1 = new Labeler("labeler1");
        Labeler labeler2 = new Labeler("labeler2");
        labeler1.addLabel(questionLabel01);
        labeler2.addLabel(questionLabel02);

        objectOfClassUnderTest.saveLabelType(questionType);
        objectOfClassUnderTest.saveLabelType(answerType);
        objectOfClassUnderTest.saveLabel(answerLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel01);
        objectOfClassUnderTest.saveLabel(questionLabel02);

        objectOfClassUnderTest.saveLabeler(labeler1);
        objectOfClassUnderTest.saveLabeler(labeler2);

        Item item = objectOfClassUnderTest.getNextNonSelfLabeledItem(labeler1);
        assertThat(item, is(item2));
    }
}
