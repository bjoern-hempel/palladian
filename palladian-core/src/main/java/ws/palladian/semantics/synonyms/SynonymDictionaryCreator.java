package ws.palladian.semantics.synonyms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Create a simple synonym dictionary that is fast and in-memory. This creator can parse the Open Office dictionary file
 * format and can therefore create {@link SnyonymDictionary} files in every language supported by OpenOffice (derived
 * from WordNet). The input for an English thesaurus can be downloaded here:
 * http://lingucomponent.openoffice.org/MyThes-1.zip (input file is the .dat file in that zip package).
 * </p>
 * 
 * @see <a href="http://stackoverflow.com/questions/4175335/where-can-i-download-a-free-synonyms-database">Stack
 *      Overflow: Where can I download a free synonyms database?</a>
 * @see <a href="http://lingucomponent.openoffice.org/MyThes-1.zip">Thesaurus file download</a>
 * 
 * 
 * @author David Urbansky
 * 
 */
public class SynonymDictionaryCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SynonymDictionaryCreator.class);

    public void createDictionary(File inputFile, File dictionaryFile) throws IOException {
        StopWatch stopWatch = new StopWatch();

        List<String> lines = FileHelper.readFileToArray(inputFile.getPath());

        String currentWord = "";
        SynonymDictionary dictionary = new SynonymDictionary();
        ProgressMonitor monitor = new ProgressMonitor(lines.size());
        for (String line : lines) {
            if (!line.startsWith("(")) {
                currentWord = line.replaceAll("\\|.*", "");
            } else {
                String[] synonyms = line.split("\\|");
                for (String synonym : synonyms) {
                    if (!synonym.startsWith("(")) {
                        dictionary.addSynonym(currentWord.trim().intern(), synonym.trim().intern());
                    }

                }
            }
            monitor.incrementAndPrintProgress();
        }

        LOGGER.info("saving dictionary to " + dictionaryFile.getName());
        FileHelper.serialize(dictionary, dictionaryFile.getPath());

        LOGGER.info("creating the dictionary took " + stopWatch.getElapsedTimeString());
    }

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        SynonymDictionaryCreator sfr = new SynonymDictionaryCreator();
        sfr.createDictionary(new File("dict.dat"), new File("dictionary.gz"));

        SynonymDictionary dictionary = (SynonymDictionary)FileHelper.deserialize("dictionary.gz");
        CollectionHelper.print(dictionary.get("best"));
    }

}
