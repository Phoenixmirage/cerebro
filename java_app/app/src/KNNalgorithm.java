import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class KNNalgorithm {

    private int numofSensors=14;
    private int training_size=37;
    private int k=7;
    private int numberofFiles=264;
    private String cvsSplitBy = ",";
    private String line = "";

    private double[][] trainingArray =new double[training_size][numofSensors];
    private double[] eucDistances =new double[training_size];
    private String[] labels=new String[training_size];

    private Map<String,Double> eucMap=new HashMap<String,Double>();


    public KNNalgorithm(String trainingFile,Vector[]v) throws InterruptedException {
        int cur_line = 0;
        MQTTclient cl = new MQTTclient();
        Consumer consumer = new Consumer(cl);
        Producer producer = new Producer();

        try (BufferedReader br = new BufferedReader(new FileReader(trainingFile))) {
            line = br.readLine();
            while ((line = br.readLine()) != null) {

                String[] line_sensors = line.split(cvsSplitBy);

                labels[cur_line] = line_sensors[0] + String.valueOf(cur_line + 1);
                for (int i = 1; i <= numofSensors; i++) {
                    trainingArray[cur_line][i - 1] = Float.parseFloat(line_sensors[i]);
                }
                cur_line++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Double efficiency=0.0;
        Double eff_counter=0.0;
        for (int j = 0; j < numberofFiles; j++) {
            System.out.println("-----------------------------------");
            for (int i = 0; i < cur_line; i++) {
                eucDistances[i] = EuclideanDistance(trainingArray[i], v[j]);
                //System.out.println("euclidean distance is " + eucDistances[i] + " , label=" + labels[i]);
                eucMap.put(labels[i], eucDistances[i]);                         //all euclidean distances unsorted
            }

            Map<String, Double> sortedeucMap = sortByValue(eucMap);                              //Sorted map with the euclidean distances and labels

            //for (Map.Entry<String, Double> entry : sortedeucMap.entrySet()) {
                //System.out.println("sorted euclidean distance with Key : " + entry.getKey()
                       // + " Value : " + entry.getValue());
            //}

            Map<String, Double> I = new HashMap<String, Double>();
            int i = 0;
            for (Map.Entry<String, Double> entry : sortedeucMap.entrySet()) {                   //I is a new map of only the k closest
                if (i < k) {
                   // System.out.println("Key in I: " + entry.getKey()
                          //  + " Value in I : " + entry.getValue());
                    I.put(entry.getKey(), entry.getValue());
                }
                i++;
            }

            int closed_counter = 0;
            int opened_counter = 0;
            Double w_opened = 0.0;
            Double w_closed = 0.0;
            String open = "EyesOpened";
            String closed = "EyesClosed";
            String[] l;
            String l2="";
            String[] ls;
            for (Map.Entry<String, Double> entry : I.entrySet()) {

                l = entry.getKey().split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                //System.out.println("Label Yi : " + l[0]);

                if (open.compareTo(l[0]) == 0) {                                                //add to Yi counter and add to Wyi
                    opened_counter++;
                    w_opened += 1 / entry.getValue();
                }
                if (closed.compareTo(l[0]) == 0) {
                    closed_counter++;
                    w_closed += 1 / entry.getValue();
                }
            }
            l2=(String)v[j].elementAt(numofSensors);
            ls=l2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            l2=ls[1].split("\\.")[1];
            System.out.println("Original label is " +l2);

            if ((opened_counter * w_opened) > (closed_counter * w_closed)) {
                producer.produce(consumer.getBuffer(), "turn On");
                if(open.compareTo(l2) == 0) {
                    eff_counter++;
                    System.out.println("Result is  OPEN :Success");
                }
                else{
                    System.out.println("Result is  OPEN :Failure");
                }
            }
            else if((opened_counter * w_opened) < (closed_counter * w_closed)) {
                producer.produce(consumer.getBuffer(), "turn Off");
                if(closed.compareTo(l2) == 0) {
                    eff_counter++;
                    System.out.println("Result is  CLOSED :Success");
                }
                else{
                    System.out.println("Result is  CLOSED :Failure");
                }
            }
            else{
                if(opened_counter>closed_counter){
                    producer.produce(consumer.getBuffer(), "turn On");
                    if(open.compareTo(l2) == 0) {
                        eff_counter++;
                        System.out.println("Result is  OPEN :Success");
                    }
                    else{
                        System.out.println("Result is  OPEN :Failure");
                    }
                }
                else{
                    producer.produce(consumer.getBuffer(), "turn Off");
                    if(closed.compareTo(l2) == 0) {
                        eff_counter++;
                        System.out.println("Result is  CLOSED :Success");
                    }
                    else{
                        System.out.println("Result is  CLOSED :Failure");
                    }
                }
            }
        }
        efficiency=(eff_counter/numberofFiles)*100;
        System.out.println("Efficiency is : " + efficiency +"%" );
        producer.produce(consumer.getBuffer(), "finish");
    }

    private  double EuclideanDistance(double[] trainingSet,Vector featureVector){
        double dist=0;
        for (int i=0;i<numofSensors;i++){
            double d=trainingSet[i]-(double)featureVector.elementAt(i);
            dist+=Math.pow(d, 2);
        }
        return Math.sqrt(dist);
    }

    private static Map<String, Double> sortByValue(Map<String, Double> eucMap) {

        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(eucMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}


