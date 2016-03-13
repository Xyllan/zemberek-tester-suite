package tester;

import com.google.common.collect.Lists;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.apps.TurkishSentenceParser;
import zemberek.morphology.apps.TurkishWordParserGenerator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.parser.SentenceMorphParse;
import zemberek.tokenizer.SentenceBoundaryDetector;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;
import zemberek.tokenizer.ZemberekLexer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Tester used for all evaluating Zemberek functionality.
 * @author xyllan
 * Date: 12.03.2016
 */
public class Tester {
    private ZemberekLexer lexer;
    private TurkishWordParserGenerator wordParser;
    private Z3MarkovModelDisambiguator disambiguator;
    private TurkishSentenceParser sentenceParser;
    private SentenceBoundaryDetector detector;
    public Tester() throws IOException {
        this.lexer = new ZemberekLexer();
        this.wordParser = TurkishWordParserGenerator.createWithDefaults();
        this.disambiguator = new Z3MarkovModelDisambiguator();
        this.sentenceParser = new TurkishSentenceParser(wordParser, disambiguator);
        this.detector = new SimpleSentenceBoundaryDetector();
    }
    public List<String> stems(Path in) {
        return stems(readFileToString(in));
    }
    public List<String> stems(String input) {
        return getSentences(input).stream().flatMap(sentence -> sentenceStems(sentence).stream()).collect(Collectors.toList());
    }
    public List<String> getSentences(String input) {
        return detector.getSentences(input);
    }
    public List<String> sentenceStems(String sentence) {
        List<String> stems = new ArrayList<>();
        SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
        sentenceParser.disambiguate(sentenceParse);
        for (SentenceMorphParse.Entry entry : sentenceParse) {
            if (entry.parses.size() > 0)
                if (!entry.parses.get(0).dictionaryItem.primaryPos.getStringForm().equals("Punc")) {
                    DictionaryItem item = entry.parses.get(0).dictionaryItem;
                    if(item.primaryPos.getStringForm().equals("Unk")) {
                        stems.add(entry.parses.get(0).getSurfaceForm() + "_" + item.primaryPos.getStringForm());
                    } else stems.add(item.lemma + "_" + item.primaryPos.getStringForm());
                }
        }
        return stems;
    }
    public static String readFileToString(Path in) {
        try {
            return new String(Files.readAllBytes(in), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    public static void main(String[] args) throws Exception {
        Tester tester = new Tester();
        generateSingleFile(Paths.get(args[0]), tester);
        //generateMultipleFiles(Paths.get(args[0]), tester);
    }
    public static void generateSingleFile(Path dir, Tester tester) throws IOException {
        Path out = Paths.get(dir.getParent().toString(), "zemberek_comparison.txt");
        List<String> lines = Files.walk(dir)
                .filter(Files::isRegularFile)
                .flatMap(path -> Lists.newArrayList(
                                "Path: "+path.toString()+"\n",
                                "Actual:",
                                readFileToString(path)+"\n",
                                "Stemmed:",
                                String.join(" ",tester.stems(path)),
                                "\n"
                        ).stream()
                ).collect(Collectors.toList());
        write(out, lines);
    }
    public static void generateMultipleFiles(Path dir, Tester tester) throws IOException {
        Files.walk(dir)
                .filter(Files::isRegularFile)
                .forEach(path -> writeSingle(outPath(path), tester.stems(path)));
    }
    public static Path outPath(Path in) {
        return Paths.get(in.getParent().toString(), "zemberek_"+in.getFileName().toString());
    }
    public static void writeSingle(Path out, List<String> tokens) {
        try {
            Files.write(out, Lists.newArrayList(String.join(" ",tokens)), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void write(Path out, List<String> lines) {
        try {
            Files.write(out, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
