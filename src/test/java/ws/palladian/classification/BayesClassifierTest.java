package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.classification.BayesClassifier;
import ws.palladian.classification.UniversalInstance;

public class BayesClassifierTest {

    @Test
    public void testBayesClassifierNominal() {

        // create training instances from the "Play Dataset", see here on page 34
        // http://www.pierlucalanzi.net/wp-content/teaching/dmtm/DMTM0809-13-ClassificationIBLNaiveBayes.pdf
        // nominal features are: Outlook, Temp, Humidity, and Windy, nominal class is Play (true or false)
        List<String> nominalFeatures = null;

        BayesClassifier bc = new BayesClassifier();
        bc.trainFromCSV(BayesClassifierTest.class.getResource("/classifier/playData.txt").getFile());

        // create an instance to classify
        UniversalInstance newInstance = new UniversalInstance(null);
        nominalFeatures = new ArrayList<String>();
        nominalFeatures.add("Sunny");
        nominalFeatures.add("Cool");
        nominalFeatures.add("High");
        nominalFeatures.add("True");
        newInstance.setNominalFeatures(nominalFeatures);

        bc.train();

        // test saving and loading
        bc.setName("testBayesClassifier");
        bc.save("data/temp/");
        BayesClassifier loadedBC = BayesClassifier.load("data/temp/testBayesClassifier.gz");

        // classify
        loadedBC.classify(newInstance);

        Assert.assertEquals(0.795417348608838, newInstance.getMainCategoryEntry().getRelevance());
        Assert.assertEquals("No", newInstance.getMainCategoryEntry().getCategory().getName());

    }

}