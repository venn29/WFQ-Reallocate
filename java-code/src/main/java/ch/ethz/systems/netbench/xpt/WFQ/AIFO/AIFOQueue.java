package ch.ethz.systems.netbench.xpt.WFQ.AIFO;

import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import ch.ethz.systems.netbench.xpt.tcpbase.PriorityHeader;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AIFOQueue implements Queue{

    private final ArrayBlockingQueue aifo;

    private long queuelength;

    private Lock reentrantLock;

    private int ownId;

    private int targetId;

    private final Map last_finishTime;

    private long round;

    private long QueueOccupied;

    private double k = 0.1;

    private int windowSize = 40;

    private long[] window;

    private int windowPointer;

    public AIFOQueue(long queuelength, int targetId, int ownId){
        long perQueueCapacity = 320;
        this.ownId = ownId;
        this.targetId = targetId;

        this.queuelength = queuelength;
        this.aifo = new ArrayBlockingQueue((int)perQueueCapacity);
        this.reentrantLock = new ReentrantLock();

        this.last_finishTime = new HashMap();
        this.round = 0;
        this.QueueOccupied = 0;

        this.window = new long[windowSize];
        for (int i=0; i<windowSize; i++){
            window[i] = -1;
        }
        this.windowPointer = 0;
    }

    @Override
    public boolean offer(Object o){
        this.reentrantLock.lock();
        FullExtTcpPacket p = (FullExtTcpPacket) o;
        boolean result = true;

        try {
            String Id = p.getDiffFlowId3();
            long startTime = this.round;
            if(last_finishTime.containsKey(Id)){
                if((long)last_finishTime.get(Id) > round){
                    startTime = (long)last_finishTime.get(Id);
                }
            }
            float weight = p.getWeight();
            long rank = (long)(startTime + (p.getSizeBit()/(8*weight)));

            int quantileCounter = 0;
            int windowCounter = 0;
            for (int i=0; i<windowSize; i++){
                if (window[i] != (long)-1) {
                    windowCounter++;
                    if (rank > window[i]) {
                        quantileCounter++;
                    }
                }
            }
            double quantile;
            if(windowCounter == (long)0){
                quantile = 0;
            }
            else {
                quantile = quantileCounter*1.0/windowCounter;
            }

            window[windowPointer] = rank;
            windowPointer = (windowPointer+1)%windowSize;

            double threshhold = (queuelength-QueueOccupied)/((1-k)*queuelength);
            if (QueueOccupied <= k*queuelength || quantile <= threshhold){
                long bytesEstimate = QueueOccupied + p.getSizeBit()/8;
                if (bytesEstimate <= queuelength){
                    last_finishTime.put(Id, rank);
                    QueueOccupied = bytesEstimate;

                    PriorityHeader header = (PriorityHeader) p;
                    header.setPriority(rank);
                    result = aifo.offer(p);
                    if (!result) {
                        System.out.println("!!!maybe perQueueCapacity should be larger");
                    }
                }
                else {
                    result = false;
                }
            }
            else {
                result = false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Exception AIFO offer: " + e.getMessage() + e.getLocalizedMessage());
        } finally {
            this.reentrantLock.unlock();
            return result;
        }
    }

    @Override
    public Packet poll(){
        this.reentrantLock.lock();
        try {
            Packet packet = (Packet) aifo.poll();

            // Update round number
            this.updateRound(packet);
            QueueOccupied -= packet.getSizeBit()/8;
            return packet;
        } catch (Exception e){
            return null;
        } finally {
            this.reentrantLock.unlock();
        }
    }


    public void updateRound(Packet p){
        PriorityHeader header = (PriorityHeader) p;
        long rank = header.getPriority();
        this.round = rank;
    }

    public long updatePort(Packet o){
        FullExtTcpPacket p = (FullExtTcpPacket) o;
        String Id = p.getDiffFlowId3();
        long startTime = this.round;
        if(last_finishTime.containsKey(Id)){
            if((long)last_finishTime.get(Id) > round){
                startTime = (long)last_finishTime.get(Id);
            }
        }

        float weight = ((FullExtTcpPacket)p).getWeight();
        long finishingTime_update = (long)(startTime + (p.getSizeBit()/(8*weight)));
        last_finishTime.put(Id, finishingTime_update);
        this.round = finishingTime_update;
        window[windowPointer] = finishingTime_update;
        windowPointer = (windowPointer+1)%windowSize;
        return finishingTime_update;
    }

    @Override
    public int size() {
        return aifo.size();
    }

    @Override
    public boolean isEmpty() {
        return aifo.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] objects) {
        return new Object[0];
    }

    @Override
    public boolean add(Object o) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(Collection collection) {
        return false;
    }

    @Override
    public void clear() { }

    @Override
    public boolean retainAll(Collection collection) {
        return false;
    }

    @Override
    public boolean removeAll(Collection collection) {
        return false;
    }

    @Override
    public boolean containsAll(Collection collection) {
        return false;
    }

    @Override
    public Object remove() {
        return null;
    }

    @Override
    public Object element() {
        return null;
    }

    @Override
    public Object peek() {
        return null;
    }
}
