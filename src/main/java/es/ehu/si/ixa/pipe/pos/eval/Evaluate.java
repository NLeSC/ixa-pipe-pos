/*
 * Copyright 2014 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package es.ehu.si.ixa.pipe.pos.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import opennlp.tools.cmdline.postag.POSEvaluationErrorListener;
import opennlp.tools.cmdline.postag.POSTaggerFineGrainedReportListener;
import opennlp.tools.postag.POSEvaluator;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerEvaluationMonitor;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.postag.WordTagSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.eval.EvaluationMonitor;
import es.ehu.si.ixa.pipe.pos.train.InputOutputUtils;

/**
 * Evaluation class mostly inspired by {@link POSEvaluator}.
 *
 * @author ragerri
 * @version 2014-07-08
 */
public class Evaluate {

  /**
   * The reference corpus to evaluate against.
   */
  private ObjectStream<POSSample> testSamples;
  /**
   * Static instance of {@link TokenNameFinderModel}.
   */
  private static POSModel posModel;
  /**
   * An instance of the probabilistic {@link POSTaggerME}.
   */
  private POSTaggerME posTagger;

  /**
   * Construct an evaluator. The features are encoded in the model itself.
   *
   * @param testData
   *          the reference data to evaluate against
   * @param model
   *          the model to be evaluated
   * @param beamsize
   *          the beam size for decoding
   * @throws IOException
   *           if input data not available
   */
  public Evaluate(final String testData, final String model, final int beamsize)
      throws IOException {

    ObjectStream<String> testStream = InputOutputUtils.readInputData(testData);
    testSamples = new WordTagSampleStream(testStream);
    InputStream trainedModelInputStream = null;
    try {
      if (posModel == null) {
        trainedModelInputStream = new FileInputStream(model);
        posModel = new POSModel(trainedModelInputStream);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (trainedModelInputStream != null) {
        try {
          trainedModelInputStream.close();
        } catch (IOException e) {
          System.err.println("Could not load model!");
        }
      }
    }
    posTagger = new POSTaggerME(posModel, beamsize, 0);
  }

  /**
   * Evaluate and print precision, recall and F measure.
   *
   * @throws IOException
   *           if test corpus not loaded
   */
  public final void evaluate() throws IOException {
    POSEvaluator evaluator = new POSEvaluator(posTagger);
    evaluator.evaluate(testSamples);
    System.out.println(evaluator.getWordAccuracy());
  }

  /**
   * Detail evaluation of a model, outputting the report a file.
   *
   * @param outputFile
   *          the file to output the report.
   * @throws IOException
   *           the io exception if not output file provided
   */
  public final void detailEvaluate(final String outputFile) throws IOException {
    File reportFile = new File(outputFile);
    OutputStream reportOutputStream = new FileOutputStream(reportFile);
    List<EvaluationMonitor<POSSample>> listeners = new LinkedList<EvaluationMonitor<POSSample>>();
    POSTaggerFineGrainedReportListener detailedFListener = new POSTaggerFineGrainedReportListener(
        reportOutputStream);
    listeners.add(detailedFListener);
    POSEvaluator evaluator = new POSEvaluator(posTagger,
        listeners.toArray(new POSTaggerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(testSamples);
    detailedFListener.writeReport();
    reportOutputStream.close();
  }

  /**
   * Evaluate and print every error.
   *
   * @throws IOException
   *           if test corpus not loaded
   */
  public final void evalError() throws IOException {
    List<EvaluationMonitor<POSSample>> listeners = new LinkedList<EvaluationMonitor<POSSample>>();
    listeners.add(new POSEvaluationErrorListener());
    POSEvaluator evaluator = new POSEvaluator(posTagger,
        listeners.toArray(new POSTaggerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(testSamples);
    System.out.println(evaluator.getWordAccuracy());
  }

}
