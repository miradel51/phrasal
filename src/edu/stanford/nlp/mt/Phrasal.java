// Phrasal -- A Statistical Machine Translation Toolkit
// for Exploring New Model Features.
// Copyright (c) 2007-2010 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    java-nlp-user@lists.stanford.edu
//    http://nlp.stanford.edu/software/phrasal

package edu.stanford.nlp.mt;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import edu.stanford.nlp.mt.base.*;
import edu.stanford.nlp.mt.decoder.h.*;
import edu.stanford.nlp.mt.decoder.inferer.*;
import edu.stanford.nlp.mt.decoder.inferer.impl.DTUDecoder;
import edu.stanford.nlp.mt.decoder.recomb.*;
import edu.stanford.nlp.mt.decoder.util.*;
import edu.stanford.nlp.mt.metrics.*;
import edu.stanford.nlp.mt.decoder.annotators.Annotator;
import edu.stanford.nlp.mt.decoder.annotators.AnnotatorFactory;
import edu.stanford.nlp.mt.decoder.feat.*;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.StringUtils;

/**
 *
 * @author danielcer
 *
 */
public class Phrasal {

  public static final String TRANSLATION_TABLE_OPT = "ttable-file";
  public static final String LANGUAGE_MODEL_OPT = "lmodel-file";
  public static final String OPTION_LIMIT_OPT = "ttable-limit";
  public static final String DISTORTION_WT_OPT = "weight-d";
  public static final String LANGUAGE_MODEL_WT_OPT = "weight-l";
  public static final String TRANSLATION_MODEL_WT_OPT = "weight-t";
  public static final String WORD_PENALTY_WT_OPT = "weight-w";
  public static final String INPUT_FACTORS_OPT = "input-factors";
  public static final String FACTOR_DELIM_OPT = "factor-delimiter";
  public static final String MAPPING_OPT = "mapping";
  public static final String NBEST_LIST_OPT = "n-best-list";
  public static final String MOSES_NBEST_LIST_OPT = "moses-n-best-list";
  public static final String DISTINCT_NBEST_LIST_OPT = "distinct-n-best-list";
  public static final String CONSTRAIN_TO_REFS = "constrain-to-refs";
  public static final String BEAM_SIZE = "stack";
  public static final String DISTORTION_FILE = "distortion-file";
  public static final String HIER_DISTORTION_FILE = "hierarchical-distortion-file";
  public static final String WEIGHTS_FILE = "weights-file";
  public static final String CONFIG_FILE = "config-file";
  public static final String USE_DISCRIMINATIVE_LM = "discriminative-lm";
  public static final String USE_DISCRIMINATIVE_TM = "discriminative-tm";
  public static final String MAX_SENTENCE_LENGTH = "max-sentence-length";
  public static final String MIN_SENTENCE_LENGTH = "min-sentence-length";
  public static final String FORCE_DECODE_ONLY = "force-decode-only";
  public static final String DISTORTION_LIMIT = "distortion-limit";
  public static final String ADDITIONAL_FEATURIZERS = "additional-featurizers";
  public static final String ADDITIONAL_ANNOTATORS = "additional-annotators";
  public static final String DISABLED_FEATURIZERS = "disabled-featurizers";
  public static final String INLINE_WEIGHTS = "inline-weights";
  public static final String LOCAL_PROCS = "localprocs";
  public static final String ITER_LIMIT = "iter-limit";
  public static final String USE_ITG_CONSTRAINTS = "use-itg-constraints";
  public static final String RECOMBINATION_HEURISTIC = "recombination-heuristic";
  public static final String GAPS_OPT = "gaps";
  public static final String MAX_GAP_SPAN_OPT = "max-gap-span";
  public static final String MAX_PENDING_PHRASES_OPT = "max-pending-phrases";
  public static final String GAPS_IN_FUTURE_COST_OPT = "gaps-in-future-cost";
  public static final String ISTRING_VOC_OPT = "istring-vocabulary";
  public static final String MOSES_COMPATIBILITY_OPT = "moses-compatibility";
  public static final String LINEAR_DISTORTION_TYPE = "linear-distortion-type";
  public static final String DROP_UNKNOWN_WORDS = "drop-unknown-words";
  public static final String ADDITIONAL_PHRASE_GENERATOR = "additional-phrase-generator";

  public static final int DEFAULT_DISCRIMINATIVE_LM_ORDER = 0;
  public static final boolean DEFAULT_DISCRIMINATIVE_TM_PARAMETER = false;

  static final Set<String> REQUIRED_FIELDS = new HashSet<String>();
  static final Set<String> OPTIONAL_FIELDS = new HashSet<String>();
  static final Set<String> IGNORED_FIELDS = new HashSet<String>();
  static final Set<String> ALL_RECOGNIZED_FIELDS = new HashSet<String>();

  static final int DEFAULT_NUM_THREADS = 1;
  static final int DEFAULT_DISTORTION_LIMIT = 5;
  static final String DEFAULT_RECOMBINATION_HEURISTIC = RecombinationFilterFactory.CLASSICAL_TRANSLATION_MODEL;
  public static final boolean DROP_UNKNOWN_WORDS_DEFAULT = true;

  static {
    REQUIRED_FIELDS.addAll(Arrays.asList(TRANSLATION_TABLE_OPT,WEIGHTS_FILE));
    OPTIONAL_FIELDS.addAll(Arrays.asList(INLINE_WEIGHTS, ITER_LIMIT,
        DISTORTION_FILE, DISTORTION_LIMIT, ADDITIONAL_FEATURIZERS,
        DISABLED_FEATURIZERS, USE_DISCRIMINATIVE_TM, FORCE_DECODE_ONLY,
        OPTION_LIMIT_OPT, NBEST_LIST_OPT, MOSES_NBEST_LIST_OPT,
        DISTINCT_NBEST_LIST_OPT, CONSTRAIN_TO_REFS, 
        RECOMBINATION_HEURISTIC, HIER_DISTORTION_FILE,
        BEAM_SIZE, WEIGHTS_FILE, USE_DISCRIMINATIVE_LM, MAX_SENTENCE_LENGTH,
        MIN_SENTENCE_LENGTH, USE_ITG_CONSTRAINTS, 
        LOCAL_PROCS, GAPS_OPT, GAPS_IN_FUTURE_COST_OPT, MAX_GAP_SPAN_OPT,
        LINEAR_DISTORTION_TYPE, MAX_PENDING_PHRASES_OPT, ISTRING_VOC_OPT,
        MOSES_COMPATIBILITY_OPT, ADDITIONAL_ANNOTATORS, DROP_UNKNOWN_WORDS, ADDITIONAL_PHRASE_GENERATOR,
        LANGUAGE_MODEL_OPT, DISTORTION_WT_OPT, LANGUAGE_MODEL_WT_OPT,
        TRANSLATION_MODEL_WT_OPT, WORD_PENALTY_WT_OPT));
    IGNORED_FIELDS.addAll(Arrays.asList(INPUT_FACTORS_OPT, MAPPING_OPT,
        FACTOR_DELIM_OPT));
    ALL_RECOGNIZED_FIELDS.addAll(REQUIRED_FIELDS);
    ALL_RECOGNIZED_FIELDS.addAll(OPTIONAL_FIELDS);
    ALL_RECOGNIZED_FIELDS.addAll(IGNORED_FIELDS);
  }

  /**
   * Number of decoding threads. Setting this parameter to 0 enables
   * multithreading inside the main decoding loop. Generally, it is better
   * to set the desired number of threads here (i.e., set this parameter >= 1).
   */
  private static int numThreads = DEFAULT_NUM_THREADS;
  
  /**
   * Hard distortion limit for phrase-based decoder
   */
  private int distortionLimit = DEFAULT_DISTORTION_LIMIT;

  /**
   * DTU options
   */
  static List<String> gapOpts = null;
  public static boolean withGaps = false;

  /**
   * Inference objects, one per thread
   */
  public List<Inferer<IString, String>> inferers;

  /**
   * Holds the model weights
   */
  private Scorer<String> scorer;

  /**
   * Phrase table type
   */
  private PhraseGenerator<IString> phraseGenerator;
  
  /**
   * Whether to filter unknown words in the output
   */
  private boolean dropUnknownWords = DROP_UNKNOWN_WORDS_DEFAULT;
  
  /**
   * n-best list options
   */
  private boolean generateMosesNBestList = true;
  private final BufferedWriter nbestListWriter;
  private int nbestListSize;

  /**
   * References for force decoding
   */
  private List<List<Sequence<IString>>> constrainedToRefs = null;

  /**
   * Hard limits on inputs to be decoded
   */
  private int maxSentenceSize = Integer.MAX_VALUE;
  private int minSentenceSize = 0;

  static String recombinationHeuristic = DEFAULT_RECOMBINATION_HEURISTIC;

  /**
   * Access the decoder's scorer, which contains the model weights.
   * 
   * @return
   */
  public Scorer<String> getScorer() { return scorer; }

  public static void initStaticMembers(Map<String, List<String>> config) {

    if (config.containsKey(ISTRING_VOC_OPT))
      IString.load(config.get(ISTRING_VOC_OPT).get(0));

    withGaps = config.containsKey(GAPS_OPT)
        || config.containsKey(MAX_GAP_SPAN_OPT);
    if (withGaps)
      gapOpts = config.containsKey(MAX_GAP_SPAN_OPT) ? config
          .get(MAX_GAP_SPAN_OPT) : config.get(GAPS_OPT);
    FlatPhraseTable.createIndex(withGaps);
    if (config.containsKey(GAPS_IN_FUTURE_COST_OPT))
      DTUDecoder.gapsInFutureCost = Boolean.parseBoolean(config.get(
          GAPS_IN_FUTURE_COST_OPT).get(0));
    if (config.containsKey(DISTINCT_NBEST_LIST_OPT))
      if (!AbstractBeamInferer.DISTINCT_SURFACE_TRANSLATIONS)
        AbstractBeamInferer.DISTINCT_SURFACE_TRANSLATIONS = Boolean.parseBoolean(config.get(
            DISTINCT_NBEST_LIST_OPT).get(0));
    if (config.containsKey(LINEAR_DISTORTION_TYPE))
      ConcreteTranslationOption.setLinearDistortionType(config.get(
          LINEAR_DISTORTION_TYPE).get(0));
    else if (withGaps)
      ConcreteTranslationOption
          .setLinearDistortionType(ConcreteTranslationOption.LinearDistortionType.last_contiguous_segment
              .name());
    if (config.containsKey(LOCAL_PROCS))
      numThreads = Integer.parseInt(config.get(LOCAL_PROCS).get(0));

    if (withGaps)
      recombinationHeuristic = RecombinationFilterFactory.DTU_TRANSLATION_MODEL;
  }

  @SuppressWarnings("unchecked")
  public Phrasal(Map<String, List<String>> config) throws IOException,
      InstantiationException, IllegalAccessException, IllegalArgumentException,
      SecurityException, InvocationTargetException, NoSuchMethodException,
      ClassNotFoundException {
    if (!config.keySet().containsAll(REQUIRED_FIELDS)) {
      Set<String> missingFields = new HashSet<String>(REQUIRED_FIELDS);
      missingFields.removeAll(config.keySet());
      throw new RuntimeException(String.format(
          "The following required fields are missing: %s\n", missingFields));
    }

    if (!ALL_RECOGNIZED_FIELDS.containsAll(config.keySet())) {
      Set<String> extraFields = new HashSet<String>(config.keySet());
      extraFields.removeAll(ALL_RECOGNIZED_FIELDS);
      throw new RuntimeException(String.format(
          "The following fields are unrecognized: %s\n", extraFields));
    }

    Set<String> ignoredItems = new HashSet<String>(config.keySet());
    ignoredItems.retainAll(IGNORED_FIELDS);

    for (String ignored : ignoredItems) {
      System.err.printf("Ignoring Moses field: %s\n", ignored);
    }

    if (config.containsKey(RECOMBINATION_HEURISTIC)) {
      recombinationHeuristic = config.get(RECOMBINATION_HEURISTIC).get(0);
    }

    boolean mosesMode = config.containsKey(MOSES_COMPATIBILITY_OPT);

    if (config.containsKey(CONSTRAIN_TO_REFS)) {
      constrainedToRefs = Metrics.readReferences(config.get(CONSTRAIN_TO_REFS)
          .toArray(new String[config.get(CONSTRAIN_TO_REFS).size()]));
    }

    // int distortionLimit = -1;
    if (config.containsKey(DISTORTION_LIMIT)) {
      List<String> strDistortionLimit = config.get(DISTORTION_LIMIT);
      if (strDistortionLimit.size() != 1) {
        throw new RuntimeException(String.format(
            "Parameter '%s' takes one and only one argument", DISTORTION_LIMIT));
      }
      try {
        distortionLimit = Integer.parseInt(strDistortionLimit.get(0));
      } catch (NumberFormatException e) {
        throw new RuntimeException(
            String
                .format(
                    "Argument '%s' to parameter '%s' can not be parsed as an integer value\n",
                    strDistortionLimit.get(0), DISTORTION_LIMIT));
      }
    }

    MSDFeaturizer<IString, String> lexReorderFeaturizer = null;

    boolean msdRecombination = false;
    if (config.containsKey(DISTORTION_FILE)
        || config.containsKey(HIER_DISTORTION_FILE)) {
      if (config.containsKey(DISTORTION_FILE)
          && config.containsKey(HIER_DISTORTION_FILE))
        throw new UnsupportedOperationException(
            "Two distortion files instead of one. "
                + "To use more than one, please use " + ADDITIONAL_FEATURIZERS
                + " field.");
      boolean stdDistFile = config.containsKey(DISTORTION_FILE);
      msdRecombination = true;
      List<String> strDistortionFile = stdDistFile ? config
          .get(DISTORTION_FILE) : config.get(HIER_DISTORTION_FILE);
      String modelType;
      String modelFilename;
      if (strDistortionFile.size() == 2) {
        modelType = strDistortionFile.get(0);
        modelFilename = strDistortionFile.get(1);

      } else if (strDistortionFile.size() == 4) {
        modelType = strDistortionFile.get(1);
        modelFilename = strDistortionFile.get(3);
      } else {
        throw new RuntimeException(
            String
                .format(
                    "Parameter '%s' takes two arguments: distortion-model-type & model-filename)",
                    DISTORTION_FILE));
      }
      lexReorderFeaturizer = mosesMode || stdDistFile ? new LexicalReorderingFeaturizer(
          new LexicalReorderingTable(modelFilename, modelType))
          : new HierarchicalReorderingFeaturizer(modelFilename, modelType);
    }
    int discriminativeLMOrder;
    if (config.containsKey(USE_DISCRIMINATIVE_LM)) {
      String orderStr = config.get(USE_DISCRIMINATIVE_LM).get(0);
      try {
        discriminativeLMOrder = Integer.parseInt(orderStr);
      } catch (NumberFormatException e) {
        throw new RuntimeException(String.format(
            "Parameter %s to %s can not be parsed as an integer value",
            orderStr, USE_DISCRIMINATIVE_LM));
      }
    } else {
      discriminativeLMOrder = DEFAULT_DISCRIMINATIVE_LM_ORDER;
    }

    List<Annotator<IString>> additionalAnnotators = new ArrayList<Annotator<IString>>();
    if (config.containsKey(ADDITIONAL_ANNOTATORS)) {
    	// todo make some general method that can parse both additional annotators
    	// and additional featurizers
    	List<String> tokens = config.get(ADDITIONAL_ANNOTATORS);
        String annotatorName = null;
        String args = null;
        for (String token : tokens) {
          Annotator<IString> annotator = null;
          if (annotatorName == null) {
            if (token.endsWith("()")) {
              String name = token.replaceFirst("\\(\\)$", "");
              Class<Annotator<IString>> annotatorClass = AnnotatorFactory
                  .loadAnnotator(name);
              annotator = (Annotator<IString>) annotatorClass
                  .newInstance();
              additionalAnnotators.add(annotator);
            } else if (token.contains("(")) {
              if (token.endsWith(")")) {
                annotatorName = token.replaceFirst("\\(.*", "");
                args = token.replaceFirst("^.*\\(", "");
                args = args.substring(0, args.length() - 1);
                args = args.replaceAll("\\s*,\\s*", ",");
                args = args.replaceAll("^\\s+", "");
                args = args.replaceAll("\\s+$", "");
                String[] argsList = args.split(",");
                System.err.printf("Additional annotators: %s.\nArgs: %s\n",
                    annotatorName, Arrays.toString(argsList));
                Class<IncrementalFeaturizer<IString, String>> featurizerClass = FeaturizerFactory
                    .loadFeaturizer(annotatorName);
                annotator = (Annotator<IString>) featurizerClass
                    .getConstructor(argsList.getClass()).newInstance(
                        new Object[] { argsList });
                additionalAnnotators.add(annotator);
                annotatorName = null;
                args = null;
              } else {
                annotatorName = token.replaceFirst("\\(.*", "");
                args = token.replaceFirst(".*\\(", "");
              }
            } else {
              System.err.printf(
                  "Error: '(' expected immediately after annotator name %s", token);
              System.err
                  .printf("Note that no whitespace between '(' and the associated annotator name is allowed\n");
              System.exit(-1);
            }
          } else {
            if (token.endsWith(")")) {
              args += " " + token.substring(0, token.length() - 1);
              args = args.replaceAll("\\s*,\\s*", ",");
              args = args.replaceAll("^\\s+", "");
              args = args.replaceAll("\\s+$", "");
              String[] argsList = args.split(",");
              System.err.printf("args: %s\n", Arrays.toString(argsList));
              Class<Annotator<IString>> annotatorClass = AnnotatorFactory
                  .loadAnnotator(annotatorName);
              annotator = (Annotator<IString>) annotatorClass
                  .getConstructor(argsList.getClass()).newInstance(
                      (Object) argsList);
              additionalAnnotators.add(annotator);
              annotatorName = null;
              args = null;
            } else {
              args += " " + token;
            }
          }
        }
        if (annotatorName != null) {
          System.err.printf("Error: no ')' found for annotator %s\n",
              annotatorName);
          System.exit(-1);
        }
    }
    System.err.printf("Number of additional annotators loaded: %d\n", additionalAnnotators.size());

    List<IncrementalFeaturizer<IString, String>> additionalFeaturizers = new ArrayList<IncrementalFeaturizer<IString, String>>();
    if (config.containsKey(ADDITIONAL_FEATURIZERS)) {
      List<String> tokens = config.get(ADDITIONAL_FEATURIZERS);
      String featurizerName = null;
      String args = null;
      for (String token : tokens) {
        IncrementalFeaturizer<IString, String> featurizer = null;
        if (featurizerName == null) {
          if (token.endsWith("()")) {
            String name = token.replaceFirst("\\(\\)$", "");
            Class<IncrementalFeaturizer<IString, String>> featurizerClass = FeaturizerFactory
                .loadFeaturizer(name);
            featurizer = (IncrementalFeaturizer<IString, String>) featurizerClass
                .newInstance();
            additionalFeaturizers.add(featurizer);
          } else if (token.contains("(")) {
            if (token.endsWith(")")) {
              featurizerName = token.replaceFirst("\\(.*", "");
              args = token.replaceFirst("^.*\\(", "");
              args = args.substring(0, args.length() - 1);
              args = args.replaceAll("\\s*,\\s*", ",");
              args = args.replaceAll("^\\s+", "");
              args = args.replaceAll("\\s+$", "");
              String[] argsList = args.split(",");
              System.err.printf("Additional featurizer: %s.\nArgs: %s\n",
                  featurizerName, Arrays.toString(argsList));
              Class<IncrementalFeaturizer<IString, String>> featurizerClass = FeaturizerFactory
                  .loadFeaturizer(featurizerName);
              featurizer = (IncrementalFeaturizer<IString, String>) featurizerClass
                  .getConstructor(argsList.getClass()).newInstance(
                      new Object[] { argsList });
              additionalFeaturizers.add(featurizer);
              featurizerName = null;
              args = null;
            } else {
              featurizerName = token.replaceFirst("\\(.*", "");
              args = token.replaceFirst(".*\\(", "");
            }
          } else {
            System.err.printf(
                "Error: '(' expected immediately after feature name %s", token);
            System.err
                .printf("Note that no whitespace between '(' and the associated feature name is allowed\n");
            System.exit(-1);
          }
        } else {
          if (token.endsWith(")")) {
            args += " " + token.substring(0, token.length() - 1);
            args = args.replaceAll("\\s*,\\s*", ",");
            args = args.replaceAll("^\\s+", "");
            args = args.replaceAll("\\s+$", "");
            String[] argsList = args.split(",");
            System.err.printf("args: %s\n", Arrays.toString(argsList));
            Class<IncrementalFeaturizer<IString, String>> featurizerClass = FeaturizerFactory
                .loadFeaturizer(featurizerName);
            featurizer = (IncrementalFeaturizer<IString, String>) featurizerClass
                .getConstructor(argsList.getClass()).newInstance(
                    (Object) argsList);
            additionalFeaturizers.add(featurizer);
            featurizerName = null;
            args = null;
          } else {
            args += " " + token;
          }
        }
        if (featurizer instanceof AlignmentFeaturizer)
          Featurizable.enableAlignments();
        if (featurizer instanceof MSDFeaturizer)
          msdRecombination = true;
      }
      if (featurizerName != null) {
        System.err.printf("Error: no ')' found for featurizer %s\n",
            featurizerName);
        System.exit(-1);
      }
    }

    boolean discriminativeTMParameter;
    if (config.containsKey(USE_DISCRIMINATIVE_TM)) {
      discriminativeTMParameter = Boolean.parseBoolean(config.get(
          USE_DISCRIMINATIVE_TM).get(0));
    } else {
      discriminativeTMParameter = DEFAULT_DISCRIMINATIVE_TM_PARAMETER;
    }

    // Create Featurizer
    String lgModel = null, lgModelVoc = "";
    if (config.containsKey(LANGUAGE_MODEL_OPT)) {
      if (config.get(LANGUAGE_MODEL_OPT).size() == 1) {
        lgModel = config.get(LANGUAGE_MODEL_OPT).get(0);
      } else if (config.get(LANGUAGE_MODEL_OPT).size() == 2) {
        lgModel = config.get(LANGUAGE_MODEL_OPT).get(0);
        lgModelVoc = config.get(LANGUAGE_MODEL_OPT).get(1);
      } else if (config.get(LANGUAGE_MODEL_OPT).size() == 4) {
        List<String> lmOpts = config.get(LANGUAGE_MODEL_OPT);
        System.err.printf(
            "Ignoring Moses factor & model order information: %s, %s, %s\n",
            lmOpts.get(0), lmOpts.get(1), lmOpts.get(2));
        lgModel = lmOpts.get(3);
      } else {
        throw new RuntimeException("Unsupported configuration "
            + config.get(LANGUAGE_MODEL_OPT));
      }

      System.err.printf("Language model: %s\n", lgModel);
    }

    if (discriminativeLMOrder != 0) {
      System.err.printf("Discriminative LM order: %d\n", discriminativeLMOrder);
    }

    CombinedFeaturizer<IString, String> featurizer;

    if (discriminativeTMParameter) {
      System.err.printf("Using Discriminative TM\n");
    }

    String linearDistortion = withGaps ? DTULinearDistortionFeaturizer.class
        .getName() : (mosesMode ? LinearDistortionFeaturizer.class.getName()
        : LinearFutureCostFeaturizer.class.getName());

    FeaturizerFactory.GapType gapT = !withGaps ? FeaturizerFactory.GapType.none
        : ((gapOpts.size() > 1) ? FeaturizerFactory.GapType.both
            : FeaturizerFactory.GapType.source);
    String gapType = gapT.name();
    System.err.println("Gap type: " + gapType);

    if (lgModel != null) {
      featurizer = FeaturizerFactory.factory(
        FeaturizerFactory.PSEUDO_PHARAOH_GENERATOR,
        makePair(FeaturizerFactory.LINEAR_DISTORTION_PARAMETER,
            linearDistortion),
        makePair(FeaturizerFactory.GAP_PARAMETER, gapType),
        makePair(FeaturizerFactory.ARPA_LM_PARAMETER, lgModel),
        makePair(FeaturizerFactory.ARPA_LM_VOC_PARAMETER, lgModelVoc));
    } else {
      featurizer = FeaturizerFactory.factory(
          FeaturizerFactory.PSEUDO_PHARAOH_GENERATOR,
          makePair(FeaturizerFactory.LINEAR_DISTORTION_PARAMETER,
              linearDistortion),
          makePair(FeaturizerFactory.GAP_PARAMETER, gapType));
    }

    if (config.containsKey(DISABLED_FEATURIZERS)) {
      Set<String> disabledFeaturizers = new HashSet<String>();
      for (String f : config.get(DISABLED_FEATURIZERS))
        disabledFeaturizers.add(f);
      featurizer.deleteFeaturizers(disabledFeaturizers);
    }

    if (lexReorderFeaturizer != null) {
      additionalFeaturizers.add(lexReorderFeaturizer);
    }

    if (!additionalFeaturizers.isEmpty()) {
      List<IncrementalFeaturizer<IString, String>> allFeaturizers = new ArrayList<IncrementalFeaturizer<IString, String>>();
      allFeaturizers.addAll(featurizer.featurizers);
      allFeaturizers.addAll(additionalFeaturizers);
      featurizer = new CombinedFeaturizer<IString, String>(allFeaturizers);
    }

    // Create Scorer
    Counter<String> weightConfig = new ClassicCounter<String>();

    if (config.containsKey(WEIGHTS_FILE)) {
      System.err.printf("Weights file: %s\n", config.get(WEIGHTS_FILE).get(0));
      if (config.get(WEIGHTS_FILE).get(0).endsWith(".binwts")) {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
            config.get(WEIGHTS_FILE).get(0)));
        weightConfig = (Counter<String>) ois.readObject();
        ois.close();
      } else {

        BufferedReader reader = new BufferedReader(new FileReader(config.get(
            WEIGHTS_FILE).get(0)));
        for (String line; (line = reader.readLine()) != null;) {
          String[] fields = line.split("\\s+");
          weightConfig.incrementCount(fields[0], Double.parseDouble(fields[1]));
        }
        reader.close();
      }
    } else {
      if (config.containsKey(INLINE_WEIGHTS)) {
        List<String> inlineWts = config.get(TRANSLATION_MODEL_WT_OPT);
        for (String inlineWt : inlineWts) {
          String[] fields = inlineWt.split("=");
          weightConfig.setCount(fields[0], Double.parseDouble(fields[1]));
        }
      }

      if (config.containsKey(LANGUAGE_MODEL_WT_OPT)) {
        weightConfig.setCount(NGramLanguageModelFeaturizer.FEATURE_NAME,
          Double.parseDouble(config.get(LANGUAGE_MODEL_WT_OPT).get(0)));
      }
      if (config.containsKey(DISTORTION_WT_OPT)) {
        weightConfig.setCount(LinearDistortionFeaturizer.FEATURE_NAME,
          Double.parseDouble(config.get(DISTORTION_WT_OPT).get(0)));


        if (config.get(DISTORTION_WT_OPT).size() > 1) {
          int numAdditionalWts = config.get(DISTORTION_WT_OPT).size() - 1;
          if (lexReorderFeaturizer == null) {
            throw new RuntimeException(
                String
                    .format(
                        "Additional weights given for parameter %s but no lexical reordering file was specified",
                        DISTORTION_WT_OPT));
          }
          if (lexReorderFeaturizer instanceof LexicalReorderingFeaturizer) {
            LexicalReorderingFeaturizer mosesLexReorderFeaturizer = (LexicalReorderingFeaturizer) lexReorderFeaturizer;
            if (numAdditionalWts != mosesLexReorderFeaturizer.mlrt.positionalMapping.length) {
              throw new RuntimeException(
                  String
                      .format(
                          "%d re-ordering weights given with parameter %s, but %d expected",
                          numAdditionalWts, DISTORTION_WT_OPT,
                          mosesLexReorderFeaturizer.mlrt.positionalMapping.length));
            }
            for (int i = 0; i < mosesLexReorderFeaturizer.mlrt.positionalMapping.length; i++) {
              weightConfig.setCount(mosesLexReorderFeaturizer.featureTags[i],
                  Double.parseDouble(config.get(DISTORTION_WT_OPT).get(i + 1)));
            }
          }
        }
      }

      if (config.containsKey(WORD_PENALTY_WT_OPT)) {
        weightConfig.setCount(WordPenaltyFeaturizer.FEATURE_NAME,
            Double.parseDouble(config.get(WORD_PENALTY_WT_OPT).get(0)));
      }

      weightConfig.setCount(UnknownWordFeaturizer.FEATURE_NAME, 1.0);
      weightConfig.setCount(SentenceBoundaryFeaturizer.FEATURE_NAME, 1.0);

      if (config.containsKey(TRANSLATION_MODEL_WT_OPT)) {
        System.err.printf("Warning: Ignoring old translation model weights set with %s", TRANSLATION_MODEL_WT_OPT);
      }
    }

    if (config.containsKey(MAX_SENTENCE_LENGTH)) {
      try {
        maxSentenceSize = Integer.parseInt(config.get(MAX_SENTENCE_LENGTH).get(
            0));
        if (maxSentenceSize == 0)
          maxSentenceSize = Integer.MAX_VALUE;
      } catch (NumberFormatException e) {
        throw new RuntimeException(String.format(
            "Argument %s to %s can not be parsed as an integer",
            config.get(MAX_SENTENCE_LENGTH), MAX_SENTENCE_LENGTH));
      }
    }

    if (config.containsKey(MIN_SENTENCE_LENGTH)) {
      try {
        minSentenceSize = Integer.parseInt(config.get(MIN_SENTENCE_LENGTH).get(
            0));
      } catch (NumberFormatException e) {
        throw new RuntimeException(String.format(
            "Argument %s to %s can not be parsed as an integer",
            config.get(MIN_SENTENCE_LENGTH), MIN_SENTENCE_LENGTH));
      }
    }

    System.err.printf("WeightConfig: '%s' %s\n", Counters.toBiggestValuesFirstString(weightConfig, 100), (weightConfig.size() > 100 ? "..." : ""));
    scorer = ScorerFactory.factory(ScorerFactory.STATIC_SCORER, weightConfig);

    // Create phrase generator
    String phraseTable;
    if (config.get(TRANSLATION_TABLE_OPT).size() == 1) {
      phraseTable = config.get(TRANSLATION_TABLE_OPT).get(0);
    } else if (config.get(TRANSLATION_TABLE_OPT).size() == 4) {
      List<String> ptOpts = config.get(TRANSLATION_TABLE_OPT);
      System.err
          .printf(
              "Ignoring Moses factor & phrase feature count information: %s, %s, %s\n",
              ptOpts.get(0), ptOpts.get(1), ptOpts.get(2));
      phraseTable = ptOpts.get(3);
    } else {
      throw new RuntimeException("Unsupported configuration "
          + config.get(TRANSLATION_TABLE_OPT));
    }

    if (config.containsKey(MOSES_NBEST_LIST_OPT)) {
      generateMosesNBestList = Boolean.parseBoolean(config.get(
          MOSES_NBEST_LIST_OPT).get(0));
    }

    if (withGaps) {
      // Support for gaps:
      if (gapOpts.size() < 1 || gapOpts.size() > 2)
        throw new UnsupportedOperationException();
      int maxSourcePhraseSpan = Integer.parseInt(gapOpts.get(0));
      DTUTable.setMaxPhraseSpan(maxSourcePhraseSpan);

      int maxTargetPhraseSpan = (gapOpts.size() > 1) ? Integer.parseInt(gapOpts
          .get(1)) : -1;
      if (maxTargetPhraseSpan == -1) {
        System.err.println("Phrases with target gaps not loaded into memory.");
        DTUTable.maxNumberTargetSegments = 1;
      }
      if (gapT == FeaturizerFactory.GapType.target
          || gapT == FeaturizerFactory.GapType.both) {
        DTUHypothesis.setMaxTargetPhraseSpan(maxTargetPhraseSpan);
        //AbstractBeamInferer.DISTINCT_SURFACE_TRANSLATIONS = true; // TODO: restore?
      }

      // Support for floating phrases:
      if (config.containsKey(MAX_PENDING_PHRASES_OPT)) {
        List<String> floatOpts = config.get(MAX_PENDING_PHRASES_OPT);
        if (floatOpts.size() != 1)
          throw new UnsupportedOperationException();
        int maxPendingPhrases = Integer.parseInt(floatOpts.get(0));
        DTUHypothesis.setMaxPendingPhrases(maxPendingPhrases);
      }
    }


    if (config.containsKey(DROP_UNKNOWN_WORDS)) {
    	dropUnknownWords = Boolean.parseBoolean(config.get(DROP_UNKNOWN_WORDS).get(0));
    }

    String optionLimit = config.get(OPTION_LIMIT_OPT).get(0);
    System.err.printf("Phrase table: %s Unknown words policy: %s\n", phraseTable, (dropUnknownWords ? "Drop" : "Keep"));

    if (phraseTable.startsWith("bitext:")) {
      phraseGenerator = (optionLimit == null ? PhraseGeneratorFactory.factory(
          featurizer, scorer, false, PhraseGeneratorFactory.NEW_DYNAMIC_GENERATOR,
          phraseTable) : PhraseGeneratorFactory.factory(featurizer, scorer, false,
          PhraseGeneratorFactory.NEW_DYNAMIC_GENERATOR,
          phraseTable.replaceFirst("^bitext:", ""), optionLimit));
    } else if (phraseTable.endsWith(".db") || phraseTable.contains(".db:")) {

      System.err.println("Dyanamic pt\n========================");
      phraseGenerator = (optionLimit == null ? PhraseGeneratorFactory.factory(
          featurizer, scorer, false, PhraseGeneratorFactory.DYNAMIC_GENERATOR,
          phraseTable) : PhraseGeneratorFactory.factory(featurizer, scorer, false,
          PhraseGeneratorFactory.DYNAMIC_GENERATOR, phraseTable, optionLimit));
    } else {
      String generatorName = withGaps ? PhraseGeneratorFactory.DTU_GENERATOR
          : PhraseGeneratorFactory.PSEUDO_PHARAOH_GENERATOR;
      phraseGenerator = (optionLimit == null ? PhraseGeneratorFactory.factory(
          featurizer, scorer, false, generatorName, phraseTable)
          : PhraseGeneratorFactory.factory(featurizer, scorer, false, generatorName,
              phraseTable, optionLimit));
    }

    if (config.get(ADDITIONAL_PHRASE_GENERATOR) != null) {
       List<PhraseGenerator<IString>> pgens = new LinkedList<PhraseGenerator<IString>>();
       pgens.add(phraseGenerator);
       for (String pgenClasspath : config.get(ADDITIONAL_PHRASE_GENERATOR)) {
          PhraseGenerator<IString> pgen;
          try {
             pgen = (PhraseGenerator<IString>)Class.forName(pgenClasspath).
                getConstructor(IsolatedPhraseFeaturizer.class, Scorer.class).newInstance(featurizer, scorer);
          } catch (ClassNotFoundException e) {
             throw new RuntimeException("Invalid PhraseGenerator: "+pgenClasspath);
          }
          pgens.add(pgen);
       }
       phraseGenerator = new CombinedPhraseGenerator<IString>(pgens, CombinedPhraseGenerator.Type.CONCATENATIVE, Integer.parseInt(optionLimit));
    }

    phraseGenerator = new CombinedPhraseGenerator<IString>(
             Arrays.asList(phraseGenerator, new UnknownWordPhraseGenerator<IString, String>(featurizer, dropUnknownWords, scorer)),
             CombinedPhraseGenerator.Type.STRICT_DOMINANCE, Integer.parseInt(optionLimit));

    System.err.printf("Phrase Limit: %d\n",
        ((CombinedPhraseGenerator<IString>) phraseGenerator).getPhraseLimit());

    // Create Recombination Filter
    RecombinationFilter<Hypothesis<IString, String>> filter = RecombinationFilterFactory
        .factory(featurizer.getNestedFeaturizers(), msdRecombination,
            recombinationHeuristic);

    // Create Search Heuristic
    IsolatedPhraseFeaturizer<IString, String> isolatedPhraseFeaturizer = featurizer;
    SearchHeuristic<IString, String> heuristic = HeuristicFactory.factory(
        isolatedPhraseFeaturizer, scorer,
        withGaps ? HeuristicFactory.ISOLATED_DTU_FOREIGN_COVERAGE
            : HeuristicFactory.ISOLATED_PHRASE_FOREIGN_COVERAGE);
    // Create Inferer
    inferers = new ArrayList<Inferer<IString, String>>(numThreads == 0 ? 1
        : numThreads);

    boolean dtuDecoder = (gapT != FeaturizerFactory.GapType.none);
    // boolean dtuDecoder = (gapT == FeaturizerFactory.GapType.none || gapT ==
    // FeaturizerFactory.GapType.both);
    for (int i = 0; i < (numThreads == 0 ? 1 : numThreads); i++) {
      // Configure InfererBuilder
      AbstractBeamInfererBuilder<IString, String> infererBuilder = (AbstractBeamInfererBuilder<IString, String>) InfererBuilderFactory
          .factory(dtuDecoder ? InfererBuilderFactory.DTU_DECODER
              : InfererBuilderFactory.MULTIBEAM_DECODER);
      try {
    	infererBuilder.setAnnotators(additionalAnnotators);
        infererBuilder
            .setIncrementalFeaturizer((CombinedFeaturizer<IString, String>) featurizer
                .clone());
        infererBuilder
            .setPhraseGenerator((PhraseGenerator<IString>) phraseGenerator
                .clone());
        infererBuilder.setScorer(scorer);
        infererBuilder
            .setSearchHeuristic((SearchHeuristic<IString, String>) heuristic
                .clone());
        infererBuilder
            .setRecombinationFilter((RecombinationFilter<Hypothesis<IString, String>>) filter
                .clone());
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }

      infererBuilder.setBeamType(HypothesisBeamFactory.BeamType.sloppybeam);
      if (numThreads == 0) {
        infererBuilder.setInternalMultiThread(true);
      }

      if (distortionLimit != -1) {
        infererBuilder.setMaxDistortion(distortionLimit);
      }

      if (config.containsKey(USE_ITG_CONSTRAINTS)) {
        infererBuilder.useITGConstraints(Boolean.parseBoolean(config.get(
            USE_ITG_CONSTRAINTS).get(0)));
      }

      if (config.containsKey(BEAM_SIZE)) {
        try {
          int beamSize = Integer.parseInt(config.get(BEAM_SIZE).get(0));
          infererBuilder.setBeamCapacity(beamSize);
        } catch (NumberFormatException e) {
          throw new RuntimeException(
              String
                  .format(
                      "Beam size %s, as specified by argument %s, can not be parsed as an integer value\n",
                      config.get(BEAM_SIZE).get(0), BEAM_SIZE));
        }

      }

      inferers.add(infererBuilder.build());
    }
    if (numThreads == 0)
      numThreads = 1;
    System.err.printf("Inferer Count: %d\n", inferers.size());

    // determine if we need to generate n-best lists
    List<String> nbestOpt = config.get(NBEST_LIST_OPT);
    if (nbestOpt != null) {
      if (nbestOpt.size() != 2) {
        throw new RuntimeException(
            String
                .format(
                    "%s requires that 2 and only 2 values are passed as arguments, not %d",
                    NBEST_LIST_OPT, nbestOpt.size()));
      }
      String nbestListFilename = nbestOpt.get(0);

      try {
        nbestListSize = Integer.parseInt(nbestOpt.get(1));
      } catch (NumberFormatException e) {
        throw new RuntimeException(String.format(
            "%s size argument, %s, can not be parsed as an integer value",
            NBEST_LIST_OPT, nbestOpt.get(1)));
      }
      if (nbestListSize <= 0) {
        throw new RuntimeException(
            String.format("%s size argmument, %d, must be > 0", NBEST_LIST_OPT,
                nbestListSize));
      }
      System.err.printf("Generating n-best lists to: %s (size: %d)\n",
          nbestListFilename, nbestListSize);
      nbestListWriter = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(nbestListFilename), "UTF8"));
    } else {
      nbestListSize = -1;
      nbestListWriter = null;
    }
  }

  private static String makePair(String label, String value) {
    return String.format("%s:%s", label, value);
  }

  private class ProcDecode implements Runnable {

    int infererid;
    private List<String> lines;
    private List<Integer> ids;

    public ProcDecode(int infererid, List<String> lines, List<Integer> ids) {
      this.infererid = infererid;
      this.lines = lines;
      this.ids = ids;
    }

    @Override
    public void run() {
      int len = ids.size();
      try {
        for (int i = 0; i < len; i++) {
          String[] tokens = lines.get(i).split("\\s+");
          RichTranslation<IString, String> translation = decodeOnly(tokens,
              ids.get(i), infererid);

          if (translation != null) {
            // notice we reproduce the lameness of moses in that an extra space
            // is
            // inserted after each translation
            synchronized (System.out) {
              System.out
                  .printf("%d:%s \n", ids.get(i), translation.translation);
            }
            synchronized (System.err) {
              System.err.printf("Final Translation: %s\n",
                  translation.translation);
              System.err.printf("Score: %f\n", translation.score);
            }
          } else {
            synchronized (System.out) {
              System.out.printf("<<<decoder failure %d>>>\n", ids.get(i));
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }
  }

  private void decodeFromConsole() throws IOException {

    System.err.println("Entering main translation loop");

    LineNumberReader reader = new LineNumberReader(new InputStreamReader(
        System.in, "UTF-8"));
    int translationId = 0;
    long startTime = System.currentTimeMillis();
    if (numThreads == 1) {
      for (String line; (line = reader.readLine()) != null; translationId++) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length > maxSentenceSize || tokens.length < minSentenceSize) {
          System.err.printf("Skipping: %s%n", line);
          System.err.printf("Tokens: %d (min: %d max: %d)%n", tokens.length, minSentenceSize,
              maxSentenceSize);
          continue;
        }

        RichTranslation<IString, String> translation = decodeOnly(tokens,
            translationId, 0);

        // display results
        if (translation != null) {
          // notice we reproduce the lameness of moses in that an extra space is
          // inserted after each translation
          System.out.printf("%s \n", translation.translation);
          System.err.printf("Final Translation: %s\n", translation.translation);
          System.err.printf("Score: %f\n", translation.score);
        } else {
          System.out.println("<<<decoder failure>>>");
        }
      }
    } else {

      List<List<String>> lines = new ArrayList<List<String>>();
      List<List<Integer>> ids = new ArrayList<List<Integer>>();

      for (int i = 0; i < numThreads; i++) {
        lines.add(new ArrayList<String>());
        ids.add(new ArrayList<Integer>());
      }

      for (String line; (line = reader.readLine()) != null; translationId++) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length > maxSentenceSize || tokens.length < minSentenceSize) {
          System.err.printf("Skipping: %s%n", line);
          System.err.printf("Tokens: %d (min: %d max: %d)%n", tokens.length, minSentenceSize,
              maxSentenceSize);
          continue;
        }
        lines.get(translationId % numThreads).add(line);
        ids.get(translationId % numThreads).add(translationId);
      }

      List<Thread> threads = new ArrayList<Thread>();
      for (int i = 0; i < numThreads; i++) {
        threads.add(new Thread(new ProcDecode(i, lines.get(i), ids.get(i))));
        threads.get(i).start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads.get(i).join();
        } catch (InterruptedException e) {
          System.err.printf("Interrupted while waiting for thread %d\n", i);
        }
      }

    }
    long totalTime = System.currentTimeMillis() - startTime;
    System.err.printf("Total Decoding time: %.3f\n", totalTime / 1000.0);

    if (nbestListWriter != null) {
      System.err.printf("Closing n-best writer\n");
      nbestListWriter.close();
    }
  }

  public RichTranslation<IString, String> decodeOnly(String[] tokens,
      int translationId, int procid) throws IOException {

    Sequence<IString> foreign = new SimpleSequence<IString>(true,
        IStrings.toSyncIStringArray(tokens));
    // log foreign sentence
    synchronized (System.err) {
      System.err.printf("Translating(%d): %s\n", procid, foreign);
    }

    // do translation
    long startTime = System.currentTimeMillis();
    RichTranslation<IString, String> translation;

    ConstrainedOutputSpace<IString, String> constrainedOutputSpace = (constrainedToRefs == null ? null
        : new EnumeratedConstrainedOutputSpace<IString, String>(
            constrainedToRefs.get(translationId),
            phraseGenerator.longestForeignPhrase()));

    if (nbestListSize == -1) {
      translation = inferers.get(procid).translate(
          foreign,
          translationId,
          constrainedOutputSpace,
          (constrainedOutputSpace == null ? null : constrainedOutputSpace
              .getAllowableSequences()));
    } else {
      List<RichTranslation<IString, String>> translations = inferers
          .get(procid).nbest(
              foreign,
              translationId,
              constrainedOutputSpace,
              (constrainedOutputSpace == null ? null : constrainedOutputSpace
                  .getAllowableSequences()), nbestListSize);
      if (translations != null) {
        translation = translations.get(0);

        StringBuilder sb = new StringBuilder(translations.size() * 500); // initialize
                                                                         // it
                                                                         // as
                                                                         // reasonably
                                                                         // large
        for (RichTranslation<IString, String> tran : translations) {
          if (generateMosesNBestList) {
            tran.nbestToMosesStringBuilder(translationId, sb);
          } else {
            tran.nbestToStringBuilder(translationId, sb);
          }
          sb.append('\n');
        }
        synchronized (nbestListWriter) {
          nbestListWriter.append(sb.toString());
        }
      } else {
        translation = null;
      }
    }
    long translationTime = System.currentTimeMillis() - startTime;

    // log additional information to stderr
    synchronized (System.err) {
      if (translation != null) {
        System.err.printf("Best Translation: %s\n", translation.translation);
        System.err.printf("Final score: %.3f\n", (float) translation.score);
        if (translation.foreignCoverage != null) {
          System.err.printf("Coverage: %s\n", translation.foreignCoverage);
          System.err.printf(
              "Foreign words covered: %d (/%d)  - %.3f %%\n",
              translation.foreignCoverage.cardinality(),
              foreign.size(),
              translation.foreignCoverage.cardinality() * 100.0
                  / foreign.size());
        } else {
          System.err.println("Coverage: {}");
        }
      } else {
        System.err.println("No best Translation: <<<decoder failure>>>");
      }

      System.err.printf("Time: %f seconds\n", translationTime / (1000.0));
    }

    return translation;
  }

  /**
   * Load options from a Moses-style ini file.
   * 
   * @param filename
   * @return
   * @throws IOException
   */
  public static Map<String, List<String>> readConfig(String filename)
      throws IOException {
    Map<String, List<String>> config = new HashMap<String, List<String>>();
    LineNumberReader reader;
    try {
      reader = new LineNumberReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Can't open configuration file %s\n", filename));
    }
    for (String line; (line = reader.readLine()) != null;) {
      line = line.trim().replaceAll("#.*$", "");
      if (line.length() == 0)
        continue;
      if (line.charAt(0) != '[' || line.charAt(line.length() - 1) != ']') {
        throw new RuntimeException(
            String
                .format(
                    "Expected bracketing of option name by '[',']', line: %d label: %s",
                    reader.getLineNumber(), line));
      }
      String nextArgLine = line;

      while (nextArgLine != null) {
        String key = line.substring(1, nextArgLine.length() - 1);
        nextArgLine = null;
        List<String> entries = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
          if (line.matches("^\\s*$"))
            break;
          if (line.startsWith("[")) {
            nextArgLine = line;
            break;
          }
          if (line.charAt(0) == '#')
            break;
          line = line.replaceAll("#.*$", "");
          String[] fields = line.split("\\s+");
          entries.addAll(Arrays.asList(fields));
        }

        if (!entries.isEmpty())
          config.put(key, entries);
      }
    }
    reader.close();
    return config;
  }

  /**
   * Read a combination of config file and other command line arguments.
   * 
   * @param args
   * @return
   * @throws IOException
   */
  private static Map<String, List<String>> readArgs(String[] args) throws IOException {
    Map<String, List<String>> configArgs = new HashMap<String, List<String>>();
    Map<String, List<String>> configFile = new HashMap<String, List<String>>();
    Map<String, List<String>> configFinal = new HashMap<String, List<String>>();

    for (Map.Entry<Object, Object> e : StringUtils.argsToProperties(args)
        .entrySet()) {
      String key = e.getKey().toString();
      String value = e.getValue().toString();
      if (CONFIG_FILE.equals(key)) {
        configFile.putAll(readConfig(value));
      } else {
        configArgs.put(key, Arrays.asList(value.split(" ")));
      }
    }
    configFinal.putAll(configFile);
    configFinal.putAll(configArgs); // command line args overwrite config file options
    return configFinal;
  }
  
  /**
   * Run Phrasal from the command line.
   * 
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage:\n\tjava ...Phrasal (model.ini)");
      System.exit(-1);
    }

    // by default, exit on uncaught exception
    Thread
        .setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread t, Throwable ex) {
            System.err.println("Uncaught exception from thread: " + t.getName());
            System.err.println(ex.toString());
            ex.printStackTrace();
            System.exit(-1);
          }
        });

    Map<String, List<String>> config = (args.length == 1) ? readConfig(args[0])
        : readArgs(args);
    initStaticMembers(config);
    Phrasal p = new Phrasal(config);
    FlatPhraseTable.lockIndex();
    p.decodeFromConsole();
    System.exit(0);
  }
}
