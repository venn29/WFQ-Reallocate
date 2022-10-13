package ch.ethz.systems.netbench.xpt.WFQDCTCP;

import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.core.network.Socket;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;

public class WFQDCTcpTransportLayer extends TransportLayer {

    public WFQDCTcpTransportLayer(int indentifier) {
        super(indentifier);
    }
    @Override
    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte) {
        return new WFQDCTcpSocket(this, flowId, this.identifier, destinationId, flowSizeByte,0,-1);
    }

    protected Socket createSocket(long flowId, int destinationId, long flowSizeByte,float weight,int flowset_num) {
        return new WFQDCTcpSocket(this, flowId, this.identifier, destinationId, flowSizeByte,weight,flowset_num);
    }

    @Override
    public void receive(Packet genericPacket) {

        FullExtTcpPacket packet = (FullExtTcpPacket) genericPacket;
        Socket socket = flowIdToSocket.get(packet.getFlowId());

        // If the socket does not yet exist, it is an incoming socket
        if (socket == null && !finishedFlowIds.contains(packet.getFlowId())) {

            // Create the socket instance in the other direction
            float weight = packet.getWeight();
            int flowset_num = packet.getFlowset_num();
            socket = createSocket(packet.getFlowId(), packet.getSourceId(),-1,weight,flowset_num);
            flowIdToReceiver.put(packet.getFlowId(), this);
            flowIdToSocket.put(packet.getFlowId(), socket);
        }

        // Give packet to socket (we do not care about stray packets)
        if (socket != null) {
            socket.handle(packet);
        }

    }
}

