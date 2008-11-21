package mt.translationtreebank;

import edu.stanford.nlp.util.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.tregex.*;
import edu.stanford.nlp.stats.*;
import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.ling.*;
import java.util.*;
import java.io.*;

class FullInformationFeaturizer extends AbstractFeaturizer {
  public List<String> extractFeatures(int deIdxInSent, TreePair validSent, Properties props) {
    Pair<Integer, Integer> chNPrange = validSent.parsedNPwithDEs_deIdx.get(deIdxInSent);
    Tree chTree = validSent.chParsedTrees.get(0);
    return extractFeatures(deIdxInSent, chNPrange, chTree, props);
  }

  public List<String> extractFeatures(int deIdxInSent, Pair<Integer, Integer> chNPrange , Tree chTree, Properties props) {
    String twofeatStr   = props.getProperty("2feat", "false");
    String revisedStr   = props.getProperty("revised", "false");
    String ngramStr     = props.getProperty("ngram", "false");
    String firstStr   = props.getProperty("1st", "false");
    String lastcharNStr = props.getProperty("lastcharN", "false");
    String lastcharNgramStr = props.getProperty("lastcharNgram", "false");
    String pwordStr     = props.getProperty("pword", "false");
    String pathStr      = props.getProperty("path", "false");
    String percentageStr= props.getProperty("percentage", "false");

    Boolean twofeat = Boolean.parseBoolean(twofeatStr);
    Boolean revised = Boolean.parseBoolean(revisedStr);
    Boolean ngram = Boolean.parseBoolean(ngramStr);
    Boolean first = Boolean.parseBoolean(firstStr);
    Boolean lastcharN = Boolean.parseBoolean(lastcharNStr);
    Boolean lastcharNgram = Boolean.parseBoolean(lastcharNgramStr);
    Boolean pword = Boolean.parseBoolean(pwordStr);
    Boolean path = Boolean.parseBoolean(pathStr);
    Boolean percentage = Boolean.parseBoolean(percentageStr);

    //Pair<Integer, Integer> chNPrange = validSent.parsedNPwithDEs_deIdx.get(deIdxInSent);
    //Tree chTree = validSent.chParsedTrees.get(0);
    Tree chNPTree = TranslationAlignment.getTreeWithEdges(chTree,chNPrange.first, chNPrange.second+1);
    int deIdxInPhrase = deIdxInSent-chNPrange.first;
    Tree maskedChNPTree = ExperimentUtils.maskIrrelevantDEs(chNPTree, deIdxInPhrase);
    Sentence<TaggedWord> npYield = maskedChNPTree.taggedYield();

    // (1) make feature list

    // make feature list
    List<String> featureList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();

    sb = new StringBuilder();
    String deTag = npYield.get(deIdxInPhrase).tag();
    if (twofeat) {
      sb.append("dePos:").append(deTag);
      featureList.add(sb.toString());
    }

    if (revised) {
      if ("DEC".equals(deTag)) {
        if (ExperimentUtils.hasVApattern(maskedChNPTree))
          featureList.add("hasVA");
      } else if ("DEG".equals(deTag)) {
        if (ExperimentUtils.hasADJPpattern(maskedChNPTree))
          featureList.add("hasADJP");
        if (ExperimentUtils.hasQPpattern(maskedChNPTree))
          featureList.add("hasQP");
        if (ExperimentUtils.hasNPPNpattern(maskedChNPTree))
          featureList.add("hasNPPN");
      }
    }

    // get deIndices
    Sentence<TaggedWord> sentence = chNPTree.taggedYield();
    int deIdx = deIdxInSent - chNPrange.first;
    
    if (ngram) {
      List<TaggedWord> beforeDE = new ArrayList<TaggedWord>();
      List<TaggedWord> afterDE  = new ArrayList<TaggedWord>();
      
      for (int i = 0; i < sentence.size(); i++) {
        TaggedWord w = sentence.get(i);
        if (i < deIdx) beforeDE.add(w);
        if (i > deIdx) afterDE.add(w);
      }
      
      featureList.addAll(posNgramFeatures(beforeDE, "beforeDE:"));
      featureList.addAll(posNgramFeatures(afterDE, "afterDE:"));
      if (beforeDE.size() > 0 && afterDE.size()>0)
        featureList.add("crossDE:"+beforeDE.get(beforeDE.size()-1).tag()+"-"+afterDE.get(0).tag());
    }
    
    // (1.X) features from first layer ==> first == false
    if (first) {
      Tree[] children = chNPTree.children();
      List<String> firstLayer = new ArrayList<String>();
      for (Tree t : children) {
        firstLayer.add(t.label().value());
      }
      featureList.addAll(ngramFeatures(firstLayer, "1st:"));
    }
    
    // (1.2) if the word before DE is a Noun, take the last character
    //       of the noun as a feature
    if (lastcharN) {
      if (deIdx-1 < 0)
        System.err.println("Nothing before DE: "+chNPTree);
      else 
        if (sentence.get(deIdx-1).tag().startsWith("N")) {
          String word = sentence.get(deIdx-1).word();
          char[] chars = word.toCharArray();
          featureList.add("Nchar-"+chars[chars.length-1]);
        }
    }

    // (1.X) features from last char in each word
    if (lastcharNgram) {
      List<String> lastChars = new ArrayList<String>();
      for (int i = 0; i < sentence.size(); i++) {
        String word = sentence.get(i).word();
        char[] chars = word.toCharArray();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(chars[chars.length-1]);
        lastChars.add(sb2.toString());
      }
      featureList.addAll(ngramFeatures(lastChars, "lastcngrams:"));
    }

    // (1.3) add the word of the PP before DE
    if (pword) {
      StringBuilder pwords = new StringBuilder();
      for (int i = 0; i < deIdx; i++) {
        String tag = sentence.get(i).tag();
        String word = sentence.get(i).word();
        if (tag.equals("P")) {
          pwords.append(word);
        }
      }
      featureList.add("Pchar-"+pwords.toString());
    }

    // (1.4) path features
    if (path) {
      featureList.addAll(extractAllPaths(chNPTree, "path:"));
    }

    // (1.X) if the QP is a "percentage"
    if (percentage) {
      if (deIdx == 1 && (sentence.get(0).word().startsWith("百分之") ||
                         sentence.get(0).word().endsWith("％"))) {
        featureList.add("PERCENTAGE");
      }
      if (deIdx == 1 && (sentence.get(0).tag().equals("NR"))) {
        featureList.add("NR");
      }
    }
    return featureList;
  }

  static List<String> posNgramFeatures(List<TaggedWord> words, String prefix) {
    List<String> pos = new ArrayList<String>();
    for (TaggedWord tw : words) {
      pos.add(tw.tag());
    }
    return ngramFeatures(pos, prefix);
  }

  static List<String> ngramFeatures(List<String> grams, String prefix) {
    List<String> features = new ArrayList<String>();
    StringBuilder sb;
    for (int i = -1; i < grams.size(); i++) {
      sb = new StringBuilder();
      sb.append(prefix).append(":");
      if (i == -1) sb.append(""); else sb.append(grams.get(i));
      sb.append("-");
      if (i+1 == grams.size()) sb.append(""); else sb.append(grams.get(i+1));
      features.add(sb.toString());

      if (i != -1) {
        sb = new StringBuilder();
        sb.append(prefix).append(":");
        sb.append(grams.get(i));
        features.add(sb.toString());
      }
    }
    return features;
  }

  private static List<String> extractAllPaths(Tree t) {
    return extractAllPaths(t, "");
  }

  private static List<String> extractAllPaths(Tree t, String prefix) {
    List<String> l = new ArrayList<String>();
    if (t.isPrePreTerminal()) {
      StringBuilder sb = new StringBuilder();
      sb.append(prefix).append(t.label().value());
      l.add(sb.toString());
      return l;
    } else {
      for (Tree c : t.children()) {
        StringBuilder newPrefix = new StringBuilder();
        newPrefix.append(prefix).append(t.label().value()).append("-");
        l.addAll(extractAllPaths(c, newPrefix.toString()));
      }
      return l;
    }
  }
}
