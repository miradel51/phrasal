package mt.metrics;

import java.util.*;
import java.io.*;

import mt.base.*;
import mt.decoder.recomb.RecombinationFilter;
import mt.decoder.util.State;
import mt.metrics.IncrementalEvaluationMetric;


/**
 * 
 * @author Michel Galley
 *
 * @param <TK>
 */
public class NISTMetric<TK,FV> extends AbstractMetric<TK,FV> {
	static public final int DEFAULT_MAX_NGRAM_ORDER = 4;
	
	final List<Map<Sequence<TK>, Integer>> maxReferenceCounts;
	final int[][] refLengths;
	final int order;
	final double multiplier;
	final boolean smooth;
	
	/**
	 * 
	 * @param referencesList
	 */
	public NISTMetric(double multiplier, List<List<Sequence<TK>>> referencesList) {
		this.order = DEFAULT_MAX_NGRAM_ORDER;
		maxReferenceCounts = new ArrayList<Map<Sequence<TK>, Integer>>(referencesList.size());
		refLengths = new int[referencesList.size()][];
		init(referencesList);	
		this.multiplier = multiplier;
		smooth = false;
	}
	
	public NISTMetric(List<List<Sequence<TK>>> referencesList) {
		this(referencesList, false);
	}
	
	/**
	 * 
	 * @param referencesList
	 */
	public NISTMetric(List<List<Sequence<TK>>> referencesList, boolean smooth) {
		this.order = DEFAULT_MAX_NGRAM_ORDER;
		maxReferenceCounts = new ArrayList<Map<Sequence<TK>, Integer>>(referencesList.size());
		refLengths = new int[referencesList.size()][];
		multiplier = 1;
		init(referencesList);
		this.smooth = smooth;
	}
	
	/**
	 * 
	 * @param referencesList
	 * @param order
	 */
	public NISTMetric(List<List<Sequence<TK>>> referencesList, int order) {
		this.order = order;
		maxReferenceCounts = new ArrayList<Map<Sequence<TK>, Integer>>(referencesList.size());
		refLengths = new int[referencesList.size()][];
		multiplier = 1;
		init(referencesList);
		smooth = false;
	}
	
	private void init(List<List<Sequence<TK>>> referencesList) {
		int listSz = referencesList.size();
		
		for (int listI = 0; listI < listSz; listI++) {
			List<Sequence<TK>> references = referencesList.get(listI);
			
			int refsSz = references.size();
			if (refsSz == 0) {
				throw new RuntimeException(String.format("No references found for data point: %d\n", listI));
			}
			
			refLengths[listI] = new int[refsSz];
			// TODO 
			Map<Sequence<TK>, Integer> maxReferenceCount = Metrics.getMaxNGramCounts(references, order);
			/* for (Sequence<TK> ngram :  maxReferenceCount.keySet()) {
				System.out.printf("%s : %d\n", ngram, maxReferenceCount.get(ngram));
			} */
			maxReferenceCounts.add(maxReferenceCount);
			refLengths[listI][0] = references.get(0).size();
			
			for (int refI = 1; refI < refsSz; refI++) {
				refLengths[listI][refI] = references.get(refI).size();
				Map<Sequence<TK>,Integer> altCounts = Metrics.getNGramCounts(references.get(refI), order);
				for (Sequence<TK> sequence : new HashSet<Sequence<TK>>(altCounts.keySet())) {
					Integer cnt = maxReferenceCount.get(sequence);
					Integer altCnt = altCounts.get(sequence);
					if (cnt == null || cnt.compareTo(altCnt) < 0) {
						maxReferenceCount.put(sequence, altCnt);
					}
				}
			}
		}
	}
	
	@Override
	public NISTIncrementalMetric getIncrementalMetric() {
		return new NISTIncrementalMetric();
	}
	
	@Override
	public NISTIncrementalMetric getIncrementalMetric(NBestListContainer<TK, FV> nbestList) {
		return new NISTIncrementalMetric(nbestList);
	}
	
	/**
	 * 
	 * @param index
	 * @param candidateLength
	 * @return
	 */
	private int bestMatchLength(int index, int candidateLength) {
		int best = refLengths[index][0];
		for (int i = 1; i < refLengths[index].length; i++) {
			if (Math.abs(candidateLength - best) > Math.abs(candidateLength - refLengths[index][i])) {
				best = refLengths[index][i];
			}
		}
		return best;
	}
	
	@Override
	public RecombinationFilter<IncrementalEvaluationMetric<TK,FV>>  getIncrementalMetricRecombinationFilter() {
		//return new NISTIncrementalMetricRecombinationFilter<TK,FV>();
    throw new UnsupportedOperationException();
  }
	
	private static int maxIncrementalId = 0;
	
	public class NISTIncrementalMetric implements IncrementalEvaluationMetric<TK,FV> {
		private final int id = maxIncrementalId++; 
		final List<Sequence<TK>> sequences; 
		final int[] matchCounts = new int[order];
		final int[] possibleMatchCounts = new int[order];
		final int[][] futureMatchCounts;
		final int[][] futurePossibleCounts;
		int r, c;
		
		public NISTIncrementalMetric clone() {
			return new NISTIncrementalMetric(this);
		}
		
		
		double[] precisions() {
			double[] r = new double[order];
			for (int i = 0; i < r.length; i++) {
				r[i] = matchCounts[i]/(double)possibleMatchCounts[i];
			}
			return r;
		}
		
		NISTIncrementalMetric() {
			futureMatchCounts = null;
			futurePossibleCounts = null;
			r = 0;
			c = 0;
			this.sequences = new ArrayList<Sequence<TK>>(maxReferenceCounts.size());
		}
		

		NISTIncrementalMetric(NBestListContainer<TK, FV> nbest) {
			r = 0;
			c = 0;
			List<List<ScoredFeaturizedTranslation<TK,FV>>>  nbestLists = nbest.nbestLists();
			
			futureMatchCounts = new int[nbestLists.size()][];
			futurePossibleCounts = new int[nbestLists.size()][];
			for (int i = 0; i < futureMatchCounts.length; i++) {
				futureMatchCounts[i] = new int[order];
				futurePossibleCounts[i] = new int[order];
				for (int j = 0; j < order; j++) {
					futurePossibleCounts[i][j] = Integer.MAX_VALUE;
				}
				List<ScoredFeaturizedTranslation<TK,FV>> nbestList = nbestLists.get(i);
				for (ScoredFeaturizedTranslation<TK,FV> tran : nbestList) {
					int seqSz = tran.translation.size();
					if (futurePossibleCounts[i][0] > seqSz) {
						for (int j = 0; j < order; j++) {
							futurePossibleCounts[i][j] = possibleMatchCounts(j, seqSz);		
						}
					}
					Map<Sequence<TK>, Integer> canidateCounts = Metrics.getNGramCounts(tran.translation, order);
					Metrics.clipCounts(canidateCounts, maxReferenceCounts.get(i));		
					int[] localCounts = localMatchCounts(canidateCounts);
					for (int j = 0; j < order; j++) {
						if (futureMatchCounts[i][j] < localCounts[j]) {
							futureMatchCounts[i][j] = localCounts[j];
						}
					}
					
				}
			}
			
			System.err.println("Estimated Future Match Counts");
			for (int i = 0; i < futureMatchCounts.length; i++) {
				System.err.printf("%d:", i);
				for (int j = 0; j < futureMatchCounts[i].length; j++) {
					System.err.printf(" %d/%d", futureMatchCounts[i][j], futurePossibleCounts[i][j]);
				}
				System.err.println();
			}
			
			this.sequences = new ArrayList<Sequence<TK>>(maxReferenceCounts.size());
		}
	
		public double getMultiplier() {
			return multiplier;
		}
		
		/**
		 * 
		 * @param m
		 */
		private NISTIncrementalMetric(NISTIncrementalMetric m) {
			this.futureMatchCounts = m.futureMatchCounts;
			this.futurePossibleCounts = m.futurePossibleCounts;
			this.r = m.r;
			this.c = m.c;
			System.arraycopy(m.matchCounts, 0, matchCounts, 0, m.matchCounts.length);
			System.arraycopy(m.possibleMatchCounts, 0, possibleMatchCounts, 0, m.possibleMatchCounts.length);
			this.sequences = new ArrayList<Sequence<TK>>(m.sequences);
		}
		
		@Override
		public int compareTo(IncrementalEvaluationMetric<TK,FV> o) {
      throw new UnsupportedOperationException();
		}

		private int possibleMatchCounts(int order, int length) {
			return length - order;
		}

		private int[] localMatchCounts(Map<Sequence<TK>, Integer> clippedCounts) {
			int[] counts = new int[order];
			for (Map.Entry<Sequence<TK>,Integer> entry : clippedCounts.entrySet()) {
				int len = entry.getKey().size();
				int cnt = entry.getValue();
				counts[len-1] += cnt;
			}

			return counts;
		}

    private void incCounts(Map<Sequence<TK>, Integer> clippedCounts, Sequence<TK> sequence, int mul) {
			int seqSz = sequence.size();
			for (int i = 0; i < order; i++) {
				possibleMatchCounts[i] += mul*possibleMatchCounts(i, seqSz); 
			}
			
			int[] localCounts = localMatchCounts(clippedCounts);
			for (int i = 0; i < order; i++) {
				//System.err.printf("local Counts[%d]: %d\n", i, localCounts[i]);
				matchCounts[i] += mul*localCounts[i];
			}
		}
		
		private void incCounts(Map<Sequence<TK>, Integer> clippedCounts, Sequence<TK> sequence) {
			incCounts(clippedCounts, sequence, 1);
		}
		
		private void decCounts(Map<Sequence<TK>, Integer> clippedCounts, Sequence<TK> sequence) {
			incCounts(clippedCounts, sequence, -1);
		}
		
		@Override
		public IncrementalEvaluationMetric<TK,FV> add(ScoredFeaturizedTranslation<TK,FV> tran) {
			int pos = sequences.size();
			if (pos >= maxReferenceCounts.size()) {
				throw new RuntimeException(String.format("Attempt to add more candidates, %d, than references, %d.", pos+1, maxReferenceCounts.size()));
			}
			if (tran != null) {
				Map<Sequence<TK>, Integer> canidateCounts = Metrics.getNGramCounts(tran.translation, order);
				Metrics.clipCounts(canidateCounts, maxReferenceCounts.get(pos));
				sequences.add(tran.translation);
				incCounts(canidateCounts, tran.translation);
				c += tran.translation.size();
				r += bestMatchLength(pos, tran.translation.size());
			} else {
				sequences.add(null);
			}
			return this;
		}

		@Override
		public IncrementalEvaluationMetric<TK,FV> replace(int index,
				ScoredFeaturizedTranslation<TK, FV> trans) {
			if (index > sequences.size()) {
				throw new IndexOutOfBoundsException(String.format("Index: %d >= %d", index, sequences.size()));
			}
			Map<Sequence<TK>, Integer> canidateCounts = (trans == null ? new HashMap<Sequence<TK>,Integer> () : Metrics.getNGramCounts(trans.translation, order));
			Metrics.clipCounts(canidateCounts, maxReferenceCounts.get(index));
			if (sequences.get(index) != null) {
				Map<Sequence<TK>, Integer> oldCanidateCounts = Metrics.getNGramCounts(sequences.get(index), order);
				Metrics.clipCounts(oldCanidateCounts, maxReferenceCounts.get(index));
				decCounts(oldCanidateCounts, sequences.get(index));
				c -= sequences.get(index).size();
				r -= bestMatchLength(index, sequences.get(index).size());
			}
			sequences.set(index, (trans == null ? null : trans.translation));
			if (trans != null) {
				incCounts(canidateCounts, trans.translation);
				c += sequences.get(index).size();
				r += bestMatchLength(index, sequences.get(index).size());
			}
		
			return this;
		}

		@Override
		public double score() {
			/*double B = 0;
			for (int i = 1; i <= 4; i++) {
				B += Math.exp(logScore(i))/(2<<(4-i+1));
			}
			return multiplier*B; 
			
			*/
			//return trueScore();
			double s;
			if (smooth) {
				s = multiplier*Math.exp(smoothLogScore(matchCounts.length));
			} else {
				s = trueScore();
			}
			return (s != s ? 0 : s);
		}
		
		public double trueScore() {
			return multiplier*Math.exp(logScore());
		}
		
		/**
		 * 
		 * @return
		 */
		public double logScore() {
			return logScore(matchCounts.length);
		}
		
		private double smoothLogScore(int max) {
			double ngramPrecisionScore = 0;

			double[] precisions = smoothNgramPrecisions();
			double wt = 1.0/max;
			for (int i = 0; i < max; i++) {
				ngramPrecisionScore += wt*Math.log(precisions[i]); 
			}
			return logBrevityPenalty()+ngramPrecisionScore;
		}
		
		private double logScore(int max) {
			double ngramPrecisionScore = 0;

			double[] precisions = ngramPrecisions();
			double wt = 1.0/max;
			for (int i = 0; i < max; i++) {
				ngramPrecisionScore += wt*Math.log(precisions[i]); 
			}
			return logBrevityPenalty()+ngramPrecisionScore;
		}
		
		
		/**
		 * 
		 * @return
		 */
		private int maxNonZeroPrecision() {
			double[] precisions = ngramPrecisions();
			for (int i = precisions.length-1; i >= 0; i--) {
				if (precisions[i] != 0) return i;
			}
			return -1;
		}
		
		/**
		 * 
		 * @return
		 */
		public double[] ngramPrecisions() {
			double[] p = new double[matchCounts.length];
			for (int i = 0; i < matchCounts.length; i++) {
				int matchCount = matchCounts[i];
				int possibleMatchCount = possibleMatchCounts[i];
				if (futureMatchCounts != null) {
					int futureMatchCountsLength = futureMatchCounts.length;
					for (int j = sequences.size(); j < futureMatchCountsLength; j++) {
						matchCount += futureMatchCounts[j][i];
						possibleMatchCount += futurePossibleCounts[j][i];
					}
				}
				p[i] = (1.0*matchCount)/(possibleMatchCount);
			}
			return p;
		}
		
		public double[] smoothNgramPrecisions() {
			double[] p = new double[matchCounts.length];
			double priorEffectiveMatchCount = 1;
			double priorEffectivePossibleMatchCount = 1;
			for (int i = 0; i < matchCounts.length; i++) {
				int matchCount = matchCounts[i];
				int possibleMatchCount = possibleMatchCounts[i];
				if (futureMatchCounts != null) {
					int futureMatchCountsLength = futureMatchCounts.length;
					for (int j = sequences.size(); j < futureMatchCountsLength; j++) {
						matchCount += futureMatchCounts[j][i];
						possibleMatchCount += futurePossibleCounts[j][i];
					}
				}
				
				double effectiveMatchCount = matchCount + 0.1*priorEffectiveMatchCount;
				double effectivePossibleMatchCount = possibleMatchCount + 0.2*priorEffectivePossibleMatchCount;
				p[i] = (1.0*effectiveMatchCount)/(effectivePossibleMatchCount);
				priorEffectiveMatchCount = effectiveMatchCount;
				priorEffectivePossibleMatchCount = effectivePossibleMatchCount;
			}
			return p;
		}
		/**
		 * 
		 * @return
		 */
		public int[][] ngramPrecisionCounts() {
			int[][] counts = new int[matchCounts.length][];
			for (int i = 0; i < matchCounts.length; i++) {
				counts[i] = new int[2];
				counts[i][0] = matchCounts[i];
				counts[i][1] = possibleMatchCounts[i];
			}
			return counts;
		}
		
		/**
		 * 
		 * @return
		 */
		public double logBrevityPenalty() {
			if (c < r) {
				return 1-r/(1.0*c);
			}
			return 0.0;
		}
		
		public double brevityPenalty() {
			return Math.exp(logBrevityPenalty());
		}
		
		/**
		 * 
		 * @return
		 */
		public int candidateLength() {
			return c;
		}
		
		/**
		 * 
		 * @return
		 */
		public int effectiveReferenceLength() {
			return r;
		}
		
		@Override 
		public double maxScore() {
			return multiplier*1.0;
		}

		@Override
		public int size() {
			return sequences.size();
		}


		@Override
		public State<IncrementalEvaluationMetric<TK, FV>> parent() {
			throw new UnsupportedOperationException();
		}


		@Override
		public double partialScore() {
			throw new UnsupportedOperationException();
		}


		@Override
		public int depth() {
			return sequences.size();
		}
	}

	static public void main(String args[]) throws IOException {
		if (args.length == 0) {
			System.err.println("Usage:\n\tjava NISTMetric (ref 1) (ref 2) ... (ref n) < canidateTranslations\n");
			System.exit(-1);
		}
		List<List<Sequence<IString>>> referencesList = Metrics.readReferences(args);
				
		NISTMetric<IString,String> bleu = new NISTMetric<IString,String>(referencesList);
		NISTMetric<IString,String>.NISTIncrementalMetric incMetric = bleu.getIncrementalMetric();
		
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));
		
		for (String line; (line = reader.readLine()) != null; ) {
			line = line.replaceAll("\\s+$", "");
			line = line.replaceAll("^\\s+", "");
			Sequence<IString> translation = new RawSequence<IString>(IStrings.toIStringArray(line.split("\\s+")));
			ScoredFeaturizedTranslation<IString, String> tran = new ScoredFeaturizedTranslation<IString, String>(translation, null, 0); 
			incMetric.add(tran);
		}
		
		reader.close();
		
		double[] ngramPrecisions = incMetric.ngramPrecisions();
		System.out.printf("NIST = %.3f, ", 100*incMetric.score());
		for (int i = 0; i < ngramPrecisions.length; i++) {
			if (i != 0) {
				System.out.print("/");
			}
			System.out.printf("%.3f", ngramPrecisions[i]*100);
		}
		System.out.printf(" (BP=%.3f, ration=%.3f %d/%d)\n", incMetric.brevityPenalty(), ((1.0*incMetric.candidateLength())/incMetric.effectiveReferenceLength()),
				 incMetric.candidateLength(), incMetric.effectiveReferenceLength());
		
		System.out.printf("\nPrecision Details:\n");
		int[][] precCounts = incMetric.ngramPrecisionCounts();
		for (int i = 0; i < ngramPrecisions.length; i++) {
			System.out.printf("\t%d:%d/%d\n", i, precCounts[i][0], precCounts[i][1]);
		}
	}

	@Override
	public double maxScore() {
		return 1.0;
	}
}

