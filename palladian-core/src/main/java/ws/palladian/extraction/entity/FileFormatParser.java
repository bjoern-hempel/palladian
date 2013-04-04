package ws.palladian.extraction.entity;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.DatasetManager;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * Transform file formats for NER learning.
 * 
 * @author David Urbansky
 * 
 */
public final class FileFormatParser {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetManager.class);

    private FileFormatParser() {
        // no instances.
    }

    /**
     * Get all tags that are used in the given file. For example ORG, LOC, PER, and MISC in the ConLL 2003 file.
     * 
     * @param trainingFilePath The path to the training file.
     * @param separator The separator used.
     * @return A set with all tags used.
     */
    public static Set<String> getTagsFromColumnFile(String trainingFilePath, final String separator) {

        final Set<String> tags = new HashSet<String>();

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                if (line.length() == 0) {
                    return;
                }
                String[] parts = line.split(separator);
                if (parts.length != 2) {
                    return;
                }
                tags.add(parts[parts.length - 1]);
            }
        };

        FileHelper.performActionOnEveryLine(trainingFilePath, la);

        return tags;
    }

    private static String getTextFromXML(String inputFilePath) {
        String xmlText = FileHelper.readFileToString(inputFilePath);
        return HtmlHelper.stripHtmlTags(xmlText);
    }

    public static String getText(String inputFilePath, TaggingFormat format) {

        if (format.equals(TaggingFormat.XML)) {
            return getTextFromXML(inputFilePath);
        } else if (format.equals(TaggingFormat.COLUMN)) {
            String outputFilePath = FileHelper.appendToFileName(inputFilePath, "_temp");
            columnToXml(inputFilePath, outputFilePath, "\t");
            return getText(outputFilePath, TaggingFormat.XML);
        }

        return "";
    }

    /**
     * Transform column format to XML.<br>
     * word [tab] type<br>
     * word2 [tab] type<br>
     * word3 [tab] type2<br>
     * =><br>
     * &lt;type&gt;word word2&lt;/type&gt;<br>
     * 
     * @param inputFilePath The location of the input file.
     * @param outputFilePath The location where the transformed file should be written to.
     * @param columnSeparator The separator for the columns.
     */
    public static void columnToXml(String inputFilePath, String outputFilePath, final String columnSeparator) {

        final StringBuilder xml = new StringBuilder();

        // the currently open tag
        final String[] currentTag = {"o"};

        // whether the last line was a break
        final boolean[] previousLineBreak = new boolean[] {true};

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split(columnSeparator);

                // skip empty lines at the beginning of the file
                if (parts.length < 2 && xml.length() == 0) {
                    return;
                }

                if (parts.length < 2) {

                    // add breaks for empty lines
                    if (line.length() == 0) {
                        if (!currentTag[0].equalsIgnoreCase("o") && lineNumber > 1) {
                            xml.append("</").append(currentTag[0]).append(">");
                            currentTag[0] = "o";
                        }
                        xml.append("\n");
                        previousLineBreak[0] = true;
                    }

                    return;
                }

                boolean openTag = false;

                String tag = parts[1];
                String value = parts[0];

                if (!currentTag[0].equalsIgnoreCase(tag)) {

                    if (!currentTag[0].equalsIgnoreCase("o") && lineNumber > 1) {
                        xml.append("</").append(currentTag[0]).append(">");
                    }

                    if (!tag.equalsIgnoreCase("o")) {
                        if (lineNumber > 1 && !previousLineBreak[0]) {
                            xml.append(" ");
                        }
                        xml.append("<").append(tag).append(">");
                        openTag = true;
                    }

                }

                currentTag[0] = tag;

                if (parts.length > 0 && value.length() > 0
                        && (Character.isLetterOrDigit(value.charAt(0)) || StringHelper.isBracket(value.charAt(0)))
                        && !openTag && lineNumber > 1 && !previousLineBreak[0]) {
                    xml.append(" ");
                }
                xml.append(value);
                previousLineBreak[0] = false;

            }
        };

        FileHelper.performActionOnEveryLine(inputFilePath, la);

        FileHelper.writeToFile(outputFilePath, xml);
    }

    /**
     * Transform column format to XML.<br>
     * word [tab] type<br>
     * word2 [tab] type<br>
     * word3 [tab] type2<br>
     * =><br>
     * &lt;type&gt;word&lt;/type&gt;<br>
     * &lt;type&gt;word2&lt;/type&gt;<br>
     * 
     * @param inputFilePath The location of the input file.
     * @param outputFilePath The location where the transformed file should be written to.
     * @param columnSeparator The separator for the columns.
     */
    public static void columnToXmlTokenBased(String inputFilePath, String outputFilePath, final String columnSeparator) {

        final StringBuilder transformedText = new StringBuilder();

        // whether the last line was a break
        final boolean[] previousLineBreak = new boolean[] {true};

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split(columnSeparator);

                if (parts.length < 2) {

                    // add breaks for empty lines
                    if (line.length() == 0) {
                        transformedText.append("\n");
                        previousLineBreak[0] = true;
                    }

                    return;
                }

                if (parts.length > 0
                        && parts[0].length() > 0
                        && (Character.isLetterOrDigit(parts[0].charAt(0)) || StringHelper.isBracket(parts[0].charAt(0)))
                        && lineNumber > 1 && !previousLineBreak[0]) {
                    transformedText.append(" ");
                }

                transformedText.append("<").append(parts[1]).append(">");
                transformedText.append(parts[0]);
                transformedText.append("</").append(parts[1]).append(">");

                previousLineBreak[0] = false;
            }
        };

        FileHelper.performActionOnEveryLine(inputFilePath, la);

        FileHelper.writeToFile(outputFilePath, transformedText);
    }

    public static void columnToBracket(String inputFilePath, String outputFilePath, final String columnSeparator) {

        final StringBuilder transformedText = new StringBuilder();

        // the currently open tag
        final String[] currentTag = {""};

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split(columnSeparator);

                if (parts.length < 2) {
                    return;
                }

                boolean openTag = false;

                if (!currentTag[0].equalsIgnoreCase(parts[1])) {

                    if (!currentTag[0].equalsIgnoreCase("o") && lineNumber > 1) {
                        transformedText.append(" ]");
                    }

                    if (!parts[1].equalsIgnoreCase("o")) {
                        if (lineNumber > 1) {
                            transformedText.append(" ");
                        }
                        transformedText.append("[").append(parts[1]).append(" ");
                        openTag = true;
                    }

                }

                currentTag[0] = parts[1];

                if (Character.isLetterOrDigit(parts[0].charAt(0)) && !openTag) {
                    transformedText.append(" ");
                }
                transformedText.append(parts[0]);

            }
        };

        FileHelper.performActionOnEveryLine(inputFilePath, la);

        FileHelper.writeToFile(outputFilePath, transformedText.toString());
    }

    public static void columnToColumnBio(String inputFilePath, String outputFilePath, final String columnSeparator) {

        // the bio format string
        final StringBuilder sb = new StringBuilder();

        // the last tag
        final String[] lastTag = {""};

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split(columnSeparator);

                if (parts.length < 2) {
                    return;
                }

                // sometimes the column file has several columnSeparators and we should take the last part, for example
                // a line could be "15. Apr DATE" so we have two white spaces but only DATE is the tag token.
                int lastIndex = parts.length - 1;

                // the content before the tag
                String tokenContent = "";
                for (int i = 0; i < lastIndex; i++) {
                    if (i > 0) {
                        tokenContent += columnSeparator;
                    }
                    tokenContent += parts[i];
                }

                String bioTag = "O";

                if (!parts[lastIndex].equalsIgnoreCase("o")) {

                    if (!lastTag[0].equalsIgnoreCase(parts[lastIndex])) {

                        bioTag = "B-" + parts[lastIndex];

                    } else if (lastTag[0].equalsIgnoreCase(parts[lastIndex])) {

                        bioTag = "I-" + parts[lastIndex];

                    }

                }

                // assign last tag
                lastTag[0] = parts[lastIndex];

                // create transformed line
                sb.append(tokenContent).append(columnSeparator).append(bioTag).append("\n");

            }
        };

        FileHelper.performActionOnEveryLine(inputFilePath, la);

        FileHelper.writeToFile(outputFilePath, sb);
    }

    public static void columnBioToColumn(String inputFilePath, String outputFilePath, final String columnSeparator) {

        final StringBuilder sb = new StringBuilder();

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split(columnSeparator);

                if (parts.length < 2) {
                    return;
                }

                String tag = parts[1];

                // remove BIO tag parts (B- or I-)
                tag = tag.replaceFirst("B-", "").replaceFirst("I-", "");

                sb.append(parts[0]).append(columnSeparator).append(tag).append("\n");

            }
        };

        FileHelper.performActionOnEveryLine(inputFilePath, la);

        FileHelper.writeToFile(outputFilePath, sb.toString());
    }

    public static void xmlToColumn(String inputFilePath, String outputFilePath, String columnSeparator) {

        // String inputText = FileHelper.readFileToString(inputFilePath);
        //
        // List<String> tokens = Tokenizer.tokenize(inputText);
        //
        // StringBuilder columnFile = new StringBuilder();
        //
        // String openTag = "O";
        // for (String token : tokens) {
        // if (token.startsWith("</")) {
        // openTag = "O";
        // } else if (token.startsWith("<")) {
        // openTag = StringHelper.getSubstringBetween(token, "<", ">");
        // } else {
        // columnFile.append(token).append(columnSeparator).append(openTag).append("\n");
        // }
        // }
        //
        // // CollectionHelper.print(tokens);
        //
        // FileHelper.writeToFile(outputFilePath, columnFile);

        StringBuilder columnFile = new StringBuilder();

        List<String> lines = FileHelper.readFileToArray(inputFilePath);

        for (String line : lines) {

            List<String> tokens = Tokenizer.tokenize(line);

            String openTag = "O";
            for (String token : tokens) {
                if (token.startsWith("</")) {
                    openTag = "O";
                } else if (token.startsWith("<")) {
                    openTag = StringHelper.getSubstringBetween(token, "<", ">");
                } else {
                    columnFile.append(token).append(columnSeparator).append(openTag).append("\n");
                }
            }

            columnFile.append("\n");
        }

        // CollectionHelper.print(tokens);

        FileHelper.writeToFile(outputFilePath, columnFile);
    }

    public static void slashToXml(String slashFilePath, String xmlFilePath) {

        slashToColumn(slashFilePath, xmlFilePath, "\t");
        columnToXml(xmlFilePath, xmlFilePath, "\t");

    }

    public static void slashToColumn(String slashFilePath, String columnFilePath, String columnSeparator) {

        StringBuilder columnString = new StringBuilder();

        String inputString = FileHelper.readFileToString(slashFilePath);

        Pattern pattern = Pattern.compile("(.+?)/([A-Z0-9_]{1,100}?)\\s", Pattern.DOTALL);

        Matcher matcher = pattern.matcher(inputString);
        while (matcher.find()) {
            columnString.append(matcher.group(1));
            columnString.append(columnSeparator);
            columnString.append(matcher.group(2));
            columnString.append("\n");
        }

        FileHelper.writeToFile(columnFilePath, columnString);
    }

    public static void columnToSlash(String columnFilePath, String slashFilePath, String columnSeparator) {
        columnToSlash(columnFilePath, slashFilePath, columnSeparator, "|");
    }

    public static void columnToSlash(String columnFilePath, String slashFilePath, final String columnSeparator,
            final String slashSign) {
        final StringBuilder slashFile = new StringBuilder();

        LineAction la = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] parts = line.split(columnSeparator);

                if (parts.length < 2) {
                    return;
                }

                String tag = parts[1];
                slashFile.append(parts[0]).append(slashSign).append(tag).append(" ");
            }
        };

        FileHelper.performActionOnEveryLine(columnFilePath, la);

        FileHelper.writeToFile(slashFilePath, slashFile);
    }

    public static void bracketToXml(String inputFilePath, String outputFilePath) {
        String inputText = FileHelper.readFileToString(inputFilePath);
        String outputText = bracketToXmlText(inputText);
        FileHelper.writeToFile(outputFilePath, outputText);
    }

    public static String bracketToXmlText(String inputText) {

        String outputText = inputText;

        Pattern pattern = Pattern.compile("\\[(\\w+)\\s(.+?)(\\s(.+?))*?\\s{1,2}\\]", Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(inputText);
        while (matcher.find()) {
            String tagName = StringHelper.getSubstringBetween(matcher.group(0), "[", " ");
            String tagContent = StringHelper.getSubstringBetween(matcher.group(0), " ", " ]");

            String xmlTag = "<" + tagName + ">" + tagContent.trim() + "</" + tagName + ">";
            outputText = outputText.replace(matcher.group(0), xmlTag);
        }

        return outputText;

    }

    public static void bracketToColumn(String inputFilePath, String outputFilePath, String columnSeparator) {

        bracketToXml(inputFilePath, outputFilePath);
        xmlToColumn(outputFilePath, outputFilePath, columnSeparator);

    }

    public static void columnTrainingToTest(String inputFilePath, String outputFilePath, String columnSeparator) {
        String inputFile = FileHelper.readFileToString(inputFilePath);
        inputFile = inputFile.replaceAll(columnSeparator, columnSeparator + columnSeparator);
        FileHelper.writeToFile(outputFilePath, inputFile);
    }

    public static void removeWhiteSpaceInFirstColumn(String inputFilePath, String outputFilePath, String replacement) {
        String inputFile = FileHelper.readFileToString(inputFilePath);
        // inputFile = inputFile.replaceAll("(?<=.+) (?=.+)", replacement);
        inputFile = inputFile.replace(" ", replacement);
        FileHelper.writeToFile(outputFilePath, inputFile);
    }

    public static void tsvToSsv(String inputFilePath, String outputFilePath) {
        String inputFile = FileHelper.readFileToString(inputFilePath);
        inputFile = inputFile.replaceAll("\\t", " ");
        FileHelper.writeToFile(outputFilePath, inputFile);
    }

    public static void textToColumn(String inputFilePath, String outputFilePath, String separator) {

        String inputFile = FileHelper.readFileToString(inputFilePath);
        List<String> tokens = Tokenizer.tokenize(inputFile);

        StringBuilder columnFile = new StringBuilder();
        for (String token : tokens) {
            columnFile.append(token).append(separator).append("X").append("\n");
        }

        FileHelper.writeToFile(outputFilePath, columnFile);
    }

    public static Annotations getAnnotations(String taggedTextFilePath, TaggingFormat format) {

        if (format.equals(TaggingFormat.XML)) {
            return getAnnotationsFromXmlFile(taggedTextFilePath);
        } else if (format.equals(TaggingFormat.COLUMN)) {
            return getAnnotationsFromColumn(taggedTextFilePath);
        } else {
            LOGGER.error("format {} not supported for getAnnotations", format);
        }

        return null;
    }

    public static Annotations getAnnotationsFromColumn(String taggedTextFilePath) {
        columnToXml(taggedTextFilePath, FileHelper.appendToFileName(taggedTextFilePath, "_t"), "\t");
        return getAnnotationsFromXmlFile(FileHelper.appendToFileName(taggedTextFilePath, "_t"));
    }

    public static Annotations getAnnotationsFromColumnTokenBased(String taggedTextFilePath) {
        columnToXmlTokenBased(taggedTextFilePath, FileHelper.appendToFileName(taggedTextFilePath, "_t"), "\t");
        return getAnnotationsFromXmlFile(FileHelper.appendToFileName(taggedTextFilePath, "_t"));
    }

    /**
     * Get XML annotations from a text. Nested annotations are discarded.
     * 
     * @param taggedText The XML tagged text. For example "The &lt;PHONE&gt;iphone 4&lt;/PHONE&gt; is a phone."
     * @return A list of annotations that were found in the text.
     */
    public static Annotations getAnnotationsFromXmlText(String taggedText) {
        Annotations annotations = new Annotations();

        // count offset that is caused by the tags, this should be taken into account when calculating the offset of the
        // entities in the plain text
        int cumulatedTagOffset = 0;

        // remove nested annotations
        // XXX
        // text <PERSON><PHONE>John J</PHONE>. Smith</PERSON> lives
        // text <PERSON><PHONE>John J</PHONE>. <PHONE>Smith</PHONE></PERSON> lives
        // text <PERSON><PHONE>John <PERSON>J</PERSON></PHONE>. <PHONE>Smith</PHONE></PERSON> lives

        // get locations of annotations
        Pattern pattern = Pattern.compile("\\<(.*?)(?:\\s.*?)?\\>(.{1,1000}?)\\</\\1\\>", Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE);
        // Pattern pattern = Pattern.compile("\\<(.*?)(\\s.*?)\\>(.{1,1000}?)\\</\\1\\>", Pattern.DOTALL
        // | Pattern.CASE_INSENSITIVE);
        // Pattern pattern = Pattern.compile("(?<=[.?!]\\s?)([A-Z][A-Za-z]*)", Pattern.DOTALL |
        // Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(taggedText);
        while (matcher.find()) {

            // if opening and closing tag are not the same continue
            /*
             * if (!matcher.group(1).equals(matcher.group(3))) {
             * Logger.getRootLogger()
             * .warn("annotations are not well formed (start tag does not match end tag at " + matcher.start()
             * + ")");
             * continue;
             * }
             */

            int windowSize = Annotation.WINDOW_SIZE;

            // get the left and right context of the annotation
            String leftContext = HtmlHelper.stripHtmlTags(taggedText.substring(
                    Math.max(0, matcher.start() - windowSize), matcher.start()));
            String rightContext = HtmlHelper.stripHtmlTags(taggedText.substring(matcher.end(),
                    Math.min(taggedText.length(), matcher.end() + windowSize)));

            String conceptName = matcher.group(1);
            String entityName = matcher.group(2);

            // count number of characters of possibly nested tags (add to cumulated offset)
            int nestedTagLength = HtmlHelper.countTagLength(entityName);

            // remove nested tags
            entityName = HtmlHelper.stripHtmlTags(entityName);
            entityName = entityName.replaceAll("\n", "");

            // add tag < + name + > to cumulated tag offset
            int tagOffset = conceptName.length() + 2;
            cumulatedTagOffset += tagOffset;

            int offset = matcher.start() + tagOffset - cumulatedTagOffset;

            Annotation annotation = new Annotation(offset, entityName, conceptName, annotations);
            annotation.setLeftContext(leftContext.trim());
            annotation.setRightContext(rightContext.trim());
            // annotation.createFeatures();
            annotations.add(annotation);

            // add tag </ + name + > and nested tag length to cumulated tag offset
            cumulatedTagOffset += nestedTagLength + conceptName.length() + 3;

            // ProgressHelper.showProgress(cumulatedTagOffset, taggedText.length(), 1);
        }

        return annotations;
    }

    public static Annotations getAnnotationsFromXmlFile(String taggedTextFilePath) {
        String taggedText = FileHelper.readFileToString(taggedTextFilePath);

        // throw out special characters that might disturb tokenization such as "'" or "=".
        // taggedText = taggedText.replace("'", "").replace("=", "");
        return getAnnotationsFromXmlText(taggedText);
    }

    /**
     * <p>
     * Get a list of annotations from a tagged file.
     * </p>
     * 
     * @param annotatedFilePath The path to the tagged file. The file must be in a tab (\t) separated column column
     *            format where the first column is the term and the second column is the concept.
     * @param numberOfSeedsPerConcept The number of annotations that have to be found for each concept. If set to -1 all
     *            annotations of the file are taken.
     * @return Annotations with numberOfSeedsPerConcept entries per concept.
     */
    public static Annotations getSeedAnnotations(String annotatedFilePath, int numberOfSeedsPerConcept) {
        Annotations annotations = new Annotations();

        // count the number of collected seeds per concept
        CountMap<String> conceptSeedCount = CountMap.create();

        // store entities in a set to avoid duplicates
        Set<String> entitySet = new HashSet<String>();

        Annotations allAnnotations = getAnnotationsFromColumn(annotatedFilePath);

        // iterate through the annotations and collect numberOfSeedsPerConcept
        for (Annotation annotation : allAnnotations) {

            String conceptName = annotation.getTargetClass();
            int numberOfSeeds = conceptSeedCount.getCount(conceptName);

            if ((numberOfSeeds < numberOfSeedsPerConcept || numberOfSeedsPerConcept == -1)
                    && !entitySet.contains(annotation.getEntity())) {
                annotations.add(annotation);
                entitySet.add(annotation.getEntity());
                conceptSeedCount.add(conceptName);
            }

        }

        return annotations;
    }

    /**
     * The output of the named entity recognition is not well formatted and we need to align it with the input data.
     * 
     * @param file The file where the prediction output is written in BIO format. This file will be overwritten.
     */
    public static void alignContent(File alignFile, String correctContent) {
        // transform to XML
        String alignFilePath = alignFile.getPath();
        FileFormatParser.columnToXml(alignFilePath, alignFilePath, "\t");
        String alignedContent = FileHelper.readFileToString(alignFilePath);

        alignContentText(alignedContent, correctContent);
        FileHelper.writeToFile(alignFilePath, alignedContent);
    }

    /**
     * 
     * @param inputContent
     * @param correctContent
     * @return
     */
    public static String alignContentText(String inputContent, String correctContent) {

        String alignedContent = inputContent;

        // compare contents, ignore tags and align content with inputText (correctContent)
        // the index for the aligned context is different because of the tags
        int alignIndex = 0;
        boolean jumpOne = false;
        for (int i = 0; i < correctContent.length(); i++, alignIndex++) {
            Character correctCharacter = correctContent.charAt(i);
            Character alignedCharacter = alignedContent.charAt(alignIndex);
            Character nextAlignedCharacter = 0;
            if (i < correctContent.length() - 1) {
                if (alignIndex + 1 >= alignedContent.length()) {
                    LOGGER.warn("Length error when aligning; aligned content is shorter than expected.");
                    break;
                }
                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);
            }

            // if same, continue
            if (correctCharacter.equals(alignedCharacter)) {
                continue;
            }

            // don't distinguish between " and '
            if ((correctCharacter.charValue() == 34 || correctCharacter.charValue() == 39)
                    && (alignedCharacter.charValue() == 34 || alignedCharacter.charValue() == 39)) {
                continue;
            }

            // characters are different

            // if tag "<" skip it
            if (alignedCharacter.charValue() == 60
                    && (!Character.isWhitespace(correctCharacter) || nextAlignedCharacter.charValue() == 47 || jumpOne)) {
                do {
                    alignIndex++;
                    alignedCharacter = alignedContent.charAt(alignIndex);
                } while (alignedCharacter.charValue() != 62);

                if (jumpOne) {
                    alignIndex++;
                    jumpOne = false;
                }
                alignedCharacter = alignedContent.charAt(++alignIndex);

                if (alignedCharacter.charValue() == 60) {
                    do {
                        alignIndex++;
                        alignedCharacter = alignedContent.charAt(alignIndex);
                    } while (alignedCharacter.charValue() != 62);
                    alignedCharacter = alignedContent.charAt(++alignIndex);
                }

                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);

                // check again if the characters are the same
                if (correctCharacter.equals(alignedCharacter)) {
                    continue;
                }
            }

            if (correctCharacter.charValue() == 10) {
                alignedContent = alignedContent.substring(0, alignIndex) + "\n"
                        + alignedContent.substring(alignIndex, alignedContent.length());
                // alignIndex--;
            } else if (Character.isWhitespace(alignedCharacter)) {

                alignedContent = alignedContent.substring(0, alignIndex)
                        + alignedContent.substring(alignIndex + 1, alignedContent.length());
                if (nextAlignedCharacter.charValue() == 60) {
                    alignIndex--;
                    jumpOne = true;
                } else {
                    jumpOne = false;
                }

            } else {
                alignedContent = alignedContent.substring(0, alignIndex) + " "
                        + alignedContent.substring(alignIndex, alignedContent.length());
            }
        }

        return alignedContent;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // FileFormatParser.xmlToColumn("data/datasets/ner/taggedTextTraining.xml",
        // "data/datasets/ner/taggedTextTrainingColumn.tsv", "\t");
        // FileFormatParser.columnToXML("data/datasets/ner/taggedTextTesting.xml",
        // "data/datasets/ner/taggedTextTestingColumn.tsv", "\t");
        // FileFormatParser.xmlToColumn("data/datasets/ner/all.xml", "data/datasets/ner/all.tsv", "\t");
        // FileFormatParser.columnToXML("data/datasets/ner/all.tsv", "data/datasets/ner/allBack.xml", "\t");
        // FileFormatParser.columnToXmlTokenBased("data/datasets/ner/all.tsv", "data/datasets/ner/allBack2.xml", "\t");
        String a = "asdfasdf <CITY role=\"main\">Dresden</CITY> asdfasdf asdf asdf <C>Berlin</C> asdfk <CITY>Berlin</CITY>";
        // FileHelper.writeToFile("a.xml", a);
        CollectionHelper.print(FileFormatParser.getAnnotationsFromXmlText(a));
        System.exit(0);

        FileFormatParser.columnToXml("data/temp/columnFormat.tsv", "data/temp/xmlFormat.xml", "\\t");
        FileFormatParser.xmlToColumn("data/temp/xmlFormat.xml", "data/temp/columnFormat2.tsv", "\\t");
        FileFormatParser.xmlToColumn("data/temp/allTagged.xml", "data/temp/allTaggedColumn.tsv", "\\t");

        FileFormatParser.xmlToColumn("data/datasets/ner/mobilephone/text/all.xml",
                "data/datasets/ner/mobilephone/text/allColumn.tsv", "\t");

        FileFormatParser.columnTrainingToTest("data/temp/allColumn.tsv", "data/temp/allColumnTest.tsv", "\t");

        FileFormatParser.columnToColumnBio("data/temp/allColumn.tsv", "data/temp/allColumnBIO.tsv", "\t");

        FileFormatParser.columnToBracket("data/temp/allColumn.tsv", "data/temp/allBracket.tsv", "\t");
        FileFormatParser.bracketToXml("data/temp/allBracket.tsv", "data/temp/allXMLFromBracket.tsv");
        FileFormatParser.bracketToColumn("data/temp/allBracket.tsv", "data/temp/allColumnFromBracket.tsv", "\t");

        FileFormatParser.columnToXml("data/temp/allColumn.tsv", "data/temp/allXML.xml", "\t");
        FileFormatParser.xmlToColumn("data/temp/allXML.xml", "data/temp/allColumnFromXML.tsv", "\t");

        FileFormatParser.slashToXml("data/temp/slashedText.txt", "data/temp/xmlFromSlashed.xml");
        FileFormatParser.slashToColumn("data/temp/slashedText.txt", "data/temp/columnFromSlashed.tsv", "\t");

        Annotations annotations = FileFormatParser.getAnnotationsFromXmlFile("data/temp/xmlFromSlashed.xml");
        CollectionHelper.print(annotations);
    }

}