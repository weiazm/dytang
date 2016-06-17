package model.nn_composition;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import dataprepare.Data;
import dataprepare.Funcs;
import duyuNN.LinearLayer;
import duyuNN.SoftmaxLayer;
import duyuNN.combinedLayer.LookupLinearTanh;
import duyuNN.combinedLayer.SimplifiedLSTMLayer;
import evaluationMetric.Metric;
import why.ResultWriter;
import why.SerializeUtil;

// running shell is:
//
// -embeddingLength 200
// -embeddingFile data/sswe-hybrid-prediction-yelp-2013-200dms-round-2
// -windowSizeWordLookup1 1
// -windowSizeWordLookup2 2
// -windowSizeWordLookup3 3
// -outputLengthWordLookup 50
// -classNum 10
// -roundNum 70
// -probThreshold 0.001
// -learningRate 0.03
// -randomizeBase -0.01
// -trainFile data/yelp-2013-train.txt.ss
// -testFile data/yelp-2013-test.txt.ss
// -outputFile data/result/result.txt

public class DocSimplifiedLSTM123Main {
    private ResultWriter resultWriter;
    private static String testFile;

    LookupLinearTanh xseedLLT1;
    LookupLinearTanh xseedLLT2;
    LookupLinearTanh xseedLLT3;

    SimplifiedLSTMLayer seedSimplifiedLSTM;

    private LinearLayer xseedInputLinear;
    private LinearLayer xseedForgetLinear;
    private LinearLayer xseedCandidateStatelinear;

    LinearLayer linearForSoftmax;
    SoftmaxLayer softmax;

    HashMap<String, Integer> wordVocab = null;

    public DocSimplifiedLSTM123Main(String embeddingFileWord, int embeddingLengthWord, int windowSizeWordLookup1,
        int windowSizeWordLookup2, int windowSizeWordLookup3, int outputLengthWordLookup, int classNum,
        String trainFile, String testFile, double randomizeBase, String outputFile) throws Exception {
        loadData(trainFile, testFile);

        wordVocab = new HashMap<String, Integer>();

        int embeddingLineCount = Funcs.lineCounter(embeddingFileWord, "utf8");
        double[][] table = new double[embeddingLineCount][];
        Funcs.loadEmbeddingFile(embeddingFileWord, embeddingLengthWord, "utf8", false, wordVocab, table);

        xseedLLT1 =
            new LookupLinearTanh(windowSizeWordLookup1, wordVocab.size(), outputLengthWordLookup, embeddingLengthWord);
        xseedLLT1.lookup.setEmbeddings(table);

        xseedLLT2 =
            new LookupLinearTanh(windowSizeWordLookup2, wordVocab.size(), outputLengthWordLookup, embeddingLengthWord);
        xseedLLT2.lookup.setEmbeddings(table);

        xseedLLT3 =
            new LookupLinearTanh(windowSizeWordLookup3, wordVocab.size(), outputLengthWordLookup, embeddingLengthWord);
        xseedLLT3.lookup.setEmbeddings(table);

        xseedInputLinear = new LinearLayer(outputLengthWordLookup * 2, outputLengthWordLookup);
        xseedForgetLinear = new LinearLayer(outputLengthWordLookup * 2, outputLengthWordLookup);
        xseedCandidateStatelinear = new LinearLayer(outputLengthWordLookup * 2, outputLengthWordLookup);

        Random rnd = new Random();
        xseedInputLinear.randomize(rnd, -1.0 * randomizeBase, randomizeBase);
        xseedForgetLinear.randomize(rnd, -1.0 * randomizeBase, randomizeBase);
        xseedCandidateStatelinear.randomize(rnd, -1.0 * randomizeBase, randomizeBase);

        seedSimplifiedLSTM = new SimplifiedLSTMLayer(xseedInputLinear, xseedForgetLinear, xseedCandidateStatelinear,
            outputLengthWordLookup);

        linearForSoftmax = new LinearLayer(outputLengthWordLookup, classNum);

        softmax = new SoftmaxLayer(classNum);
        linearForSoftmax.link(softmax);

        xseedLLT1.randomize(rnd, -1.0 * randomizeBase, randomizeBase);
        xseedLLT2.randomize(rnd, -1.0 * randomizeBase, randomizeBase);
        xseedLLT3.randomize(rnd, -1.0 * randomizeBase, randomizeBase);
        linearForSoftmax.randomize(rnd, -1.0 * randomizeBase, randomizeBase);

        resultWriter = new ResultWriter(outputFile);
    }

    ArrayList<Data> trainDataList;
    ArrayList<Data> testDataList;

    public void loadData(String trainFile, String testFile) {
        System.out.println("================ start loading corpus ==============");

        trainDataList = new ArrayList<Data>();
        Funcs.loadCorpus(trainFile, "utf8", trainDataList);

        // testDataList = new ArrayList<Data>();
        // Funcs.loadCorpus(testFile, "utf8", testDataList);

        System.out.println("training size: " + trainDataList.size());
        // System.out.println("testDataList size: " + testDataList.size());

        System.out.println("================ finsh loading corpus ==============");
    }

    public void run(int roundNum, double probThreshould, double learningRate, int classNum) throws Exception {
        double lossV = 0.0;
        int lossC = 0;
        for (int round = 1; round <= roundNum; round++) {
            System.out.println("============== running round: " + round + " ===============");
            Collections.shuffle(trainDataList, new Random());
            System.out.println("Finish shuffling training data.");

            for (int idxData = 0; idxData < trainDataList.size(); idxData++) {
                Data data = trainDataList.get(idxData);

                String[] sentences = data.reviewText.split("<sssss>");
                int[][] wordIdMatrix = Funcs.fillDocument(sentences, wordVocab);

                DocSimplifiedLSTM docSimplifiedLSTM =
                    new DocSimplifiedLSTM(xseedLLT1, xseedLLT2, xseedLLT3, wordIdMatrix, seedSimplifiedLSTM);

                if (docSimplifiedLSTM.sentenceConvList.size() == 0) {
                    System.out.println(idxData + ":" + data.toString() + "docAverage.sentenceConvList.size() == 0");
                    continue;
                }
                // link. important.
                docSimplifiedLSTM.link(linearForSoftmax);

                docSimplifiedLSTM.forward();
                linearForSoftmax.forward();
                softmax.forward();

                // set cross-entropy error
                // we minus 1 because the saved goldRating is in range 1~5, while what we need is in range 0~4
                int goldRating = data.goldRating - 1;
                lossV += -Math.log(softmax.output[goldRating]);
                lossC += 1;

                for (int k = 0; k < softmax.outputG.length; k++)
                    softmax.outputG[k] = 0.0;

                if (softmax.output[goldRating] < probThreshould)
                    softmax.outputG[goldRating] = 1.0 / probThreshould;
                else
                    softmax.outputG[goldRating] = 1.0 / softmax.output[goldRating];

                // backward
                softmax.backward();
                linearForSoftmax.backward();
                docSimplifiedLSTM.backward();

                // update
                linearForSoftmax.update(learningRate);
                docSimplifiedLSTM.update(learningRate);

                // clearGrad
                docSimplifiedLSTM.clearGrad();
                linearForSoftmax.clearGrad();
                softmax.clearGrad();

                if (idxData % 10 == 0) {
                    System.out.println("running idxData = " + idxData + "/" + trainDataList.size() + "\t "
                        + "lossV/lossC = " + String.format("%.4f", lossV) + "/" + lossC + "\t" + " = "
                        + String.format("%.4f", lossV / lossC) + "\t"
                        + DateFormat.getDateTimeInstance().format(new Date()));
                }
            }

            System.out.println("============= finish training round: " + round + " ==============");
            // dump(round);
            // SerializeUtil.serializeToFile(resultWriter, "resultWriter");
            // resultWriter = null;
            SerializeUtil.serializeToFile(testFile, "testFile");
            // testFile = null;
            SerializeUtil.serializeToFile(xseedLLT1, "xseedLLT1");
            // xseedLLT1 = null;
            SerializeUtil.serializeToFile(xseedLLT2, "xseedLLT2");
            // xseedLLT2 = null;
            SerializeUtil.serializeToFile(xseedLLT3, "xseedLLT3");
            // xseedLLT3 = null;
            SerializeUtil.serializeToFile(seedSimplifiedLSTM, "seedSimplifiedLSTM");
            // seedSimplifiedLSTM = null;
            SerializeUtil.serializeToFile(xseedInputLinear, "xseedInputLinear");
            // xseedInputLinear = null;
            SerializeUtil.serializeToFile(xseedForgetLinear, "xseedForgetLinear");
            // xseedForgetLinear = null;
            SerializeUtil.serializeToFile(xseedCandidateStatelinear, "xseedCandidateStatelinear");
            // xseedCandidateStatelinear = null;
            SerializeUtil.serializeToFile(linearForSoftmax, "linearForSoftmax");
            // linearForSoftmax = null;
            SerializeUtil.serializeToFile(softmax, "softmax");
            // softmax = null;
            SerializeUtil.serializeToFile(wordVocab, "wordVocab");
            // wordVocab = null;
            SerializeUtil.serializeToFile(trainDataList, "trainDataList");
            // trainDataList = null;
            predict(round, roundNum);
        }
    }

    public void predict(int round, int roundNum) throws Exception {
        System.out.println("=========== predicting round: " + round + " ===============");
        if (testDataList == null) {
            testDataList = new ArrayList<Data>();
            Funcs.loadCorpus(testFile, "utf8", testDataList);
        }

        List<Integer> goldList = new ArrayList<Integer>();// save memory
        List<Integer> predList = new ArrayList<Integer>();

        for (int idxData = 0; idxData < testDataList.size(); idxData++) {
            Data data = testDataList.get(idxData);

            String[] sentences = data.reviewText.split("<sssss>");
            int[][] wordIdMatrix = Funcs.fillDocument(sentences, wordVocab);

            DocSimplifiedLSTM docSimplifiedLSTM =
                new DocSimplifiedLSTM(xseedLLT1, xseedLLT2, xseedLLT3, wordIdMatrix, seedSimplifiedLSTM);

            if (docSimplifiedLSTM.sentenceConvList.size() == 0) {
                System.out.println(idxData + ":" + data.toString() + "docAverage.sentenceConvList.size() == 0");
                continue;
            }

            // link. important.
            docSimplifiedLSTM.link(linearForSoftmax);

            docSimplifiedLSTM.forward();
            linearForSoftmax.forward();
            softmax.forward();

            int predClass = -1;
            double maxPredProb = -1.0;
            for (int ii = 0; ii < softmax.length; ii++) {
                if (softmax.output[ii] > maxPredProb) {
                    maxPredProb = softmax.output[ii];
                    predClass = ii;
                }
            }
            int predictedRating = predClass + 1;
            predList.add(predictedRating);
            goldList.add(data.goldRating);// save memory
            /**
             * ���һ�ֽ�����Ԥ����dataд���ļ�
             */
            if (round != roundNum) {
                // this.resultWriter.write(data.toPredString());
                if (idxData % 100 == 0)
                    System.out.println("round:" + round + " " + idxData);
            }
            // System.out.println(predictedRating);
            if (round == roundNum) {
                // this.resultWriter.write(data.toPredString());
                if (idxData % 100 == 0)
                    System.out.println(idxData);
                this.resultWriter.write(
                    data.userStr + "\t\t" + data.productStr + "\t\t" + predictedRating + "\t\t" + data.reviewText);
            }
        }
        if (round == roundNum) {
            this.resultWriter.close();
        }

        Metric.calcMetric(goldList, predList);
        System.out.println("============== finish predicting =================");
    }

    public static void main(String[] args) throws Exception {
        HashMap<String, String> argsMap = Funcs.parseArgs(args);

        System.out.println("==== begin configuration ====");
        for (String key : argsMap.keySet()) {
            System.out.println(key + "\t\t" + argsMap.get(key));
        }
        System.out.println("==== end configuration ====");

        int embeddingLength = Integer.parseInt(argsMap.get("-embeddingLength"));
        String embeddingFile = argsMap.get("-embeddingFile");
        // windowsize = 1, 2 and 3 works well
        int windowSizeWordLookup1 = Integer.parseInt(argsMap.get("-windowSizeWordLookup1"));
        int windowSizeWordLookup2 = Integer.parseInt(argsMap.get("-windowSizeWordLookup2"));
        int windowSizeWordLookup3 = Integer.parseInt(argsMap.get("-windowSizeWordLookup3"));
        int outputLengthWordLookup = Integer.parseInt(argsMap.get("-outputLengthWordLookup"));
        int classNum = Integer.parseInt(argsMap.get("-classNum"));

        int roundNum = Integer.parseInt(argsMap.get("-roundNum"));
        double probThreshold = Double.parseDouble(argsMap.get("-probThreshold"));
        double learningRate = Double.parseDouble(argsMap.get("-learningRate"));
        double randomizeBase = Double.parseDouble(argsMap.get("-randomizeBase"));

        String trainFile = argsMap.get("-trainFile");
        String testFile = argsMap.get("-testFile");
        String outputFile = argsMap.get("-outputFile");

        DocSimplifiedLSTM123Main main = new DocSimplifiedLSTM123Main(embeddingFile, embeddingLength,
            windowSizeWordLookup1, windowSizeWordLookup2, windowSizeWordLookup3, outputLengthWordLookup, classNum,
            trainFile, testFile, randomizeBase, outputFile);

        main.run(roundNum, probThreshold, learningRate, classNum);

    }
}
