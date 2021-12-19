package ch.ethz.systems.netbench.xpt.ports.FIFO;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.*;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class FIFOOutputPort extends OutputPort {

    private final long maxQueueSize;

    FIFOOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSize) {
        super(ownNetworkDevice, targetNetworkDevice, link, new LinkedBlockingQueue<Packet>());
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * Enqueue the given packet.
     * Drops it if the queue is full (tail drop).
     * @param packet    Packet instance
     */
    @Override
    public void enqueue(Packet packet) {

        // If it is not sending, then the queue is empty at the moment,
        // so this packet can be immediately send
        if (!getIsSending()) {

            // Link is now being utilized
            getLogger().logLinkUtilized(true);

            // Add event when sending is finished
            Simulator.registerEvent(new PacketDispatchedEvent(
                    packet.getSizeBit() / getLink().getBandwidthBitPerNs(),
                    packet,
                    this
            ));

            // It is now sending again
            setIsSending();

        } else { // If it is still sending, the packet is added to the queue (if there is space)

            // We tag the enqueue time to the packet, before offering it to FIFO
            FullExtTcpPacket p = (FullExtTcpPacket) packet;
            p.setEnqueueTime(Simulator.getCurrentTime());

            // Tail-drop enqueue
            if (getQueueSize() <= maxQueueSize-1) {

                // Check whether there is an inversion for the packet enqueued
                if (SimulationLogger.hasInversionsTrackingEnabled()){

                    // We compute the perceived rank
                    Object[] contentFIFO = super.getQueue().toArray();
                    if (contentFIFO.length > 0){
                        Arrays.sort(contentFIFO);
                        FullExtTcpPacket packet_maxrank = (FullExtTcpPacket) contentFIFO[contentFIFO.length-1];
                        int rank_perceived = (int)packet_maxrank.getPriority();

                        // We measure the inversion
                        if (rank_perceived > p.getPriority()){
                            SimulationLogger.logInversionsPerRank(this.getOwnId(), (int) p.getPriority(), 1);
                        }
                    }
                }

                // Enqueue to the FIFO queue
                getQueue().add(packet);

                // Update buffer size with enqueued packet
                increaseBufferOccupiedBits(packet.getSizeBit());
                getLogger().logQueueState(getQueue().size(), getBufferOccupiedBits());

            } else {

                // Logging dropped packet
                SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED");
                if (p.getSourceId() == this.getOwnId()) {
                    SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED_AT_SOURCE");
                }
            }
        }
    }
}
