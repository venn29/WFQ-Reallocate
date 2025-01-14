package ch.ethz.systems.netbench.ext.poissontraffic;

import ch.ethz.systems.netbench.core.Simulator;

import java.util.Random;

//the weight[] should be int[] ,
//get weight ,return double
public class WeightDistribution {
    private int[] weights;

    private int weight_num;

    private Random ownRng;
    private int total_weight;
    public WeightDistribution(String distribution, int weight_num){
        this.ownRng = Simulator.selectIndependentRandom("weight_distribute"+distribution);
        switch (distribution) {
            //need to be modified
            case "uniform":
                this.weight_num = weight_num;
                this.weights = new int[this.weight_num];
                int weight_each = 1;
                for(int i=0;i<this.weight_num;i++){
                    this.weights[i] = weight_each;
                }
                break;
            case "linear":
                this.weight_num = weight_num;
                this.weights = new int[this.weight_num];
                int[] multiples = new int[this.weight_num];
                for(int i=0;i<this.weight_num;i++) {
                    multiples[i] = i+1;
                }
                int base_weight = 1;
                for(int i=0;i<this.weight_num;i++){
                    this.weights[i] = base_weight*multiples[i];
                }
                break;
            default:
                break;
        }
    }
    public int[] get_weights(){
        return this.weights;
    }
    public int getTotal_weight(){return this.total_weight;}
    public int get_random_weight_uniform(){
        int random = this.ownRng.nextInt(weight_num);
        return this.weights[random];
    }
    public double[] get_weights_uniformly(int flownum){
        int[] weights_int =  new int[flownum];
        int weight_total = 0;
        for(int i=0;i<flownum;i++){
            int weight = this.get_random_weight_uniform();
            weight_total += weight;
            weights_int[i] = weight;
        }
        double[] weights_real = new double[flownum];
        for(int i=0;i<flownum;i++){
            weights_real[i] = weights_int[i]*1.0/weight_total;
        }
        return weights_real;
    }
}
