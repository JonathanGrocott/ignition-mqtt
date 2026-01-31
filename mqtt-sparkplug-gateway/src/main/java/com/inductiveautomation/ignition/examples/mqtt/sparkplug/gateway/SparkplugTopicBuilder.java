package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

public class SparkplugTopicBuilder {

    public static String nodeBirth(String groupId, String edgeNodeId) {
        return build(groupId, "NBIRTH", edgeNodeId, null);
    }

    public static String deviceBirth(String groupId, String edgeNodeId, String deviceId) {
        return build(groupId, "DBIRTH", edgeNodeId, deviceId);
    }

    public static String nodeData(String groupId, String edgeNodeId) {
        return build(groupId, "NDATA", edgeNodeId, null);
    }

    public static String deviceData(String groupId, String edgeNodeId, String deviceId) {
        return build(groupId, "DDATA", edgeNodeId, deviceId);
    }

    public static String nodeDeath(String groupId, String edgeNodeId) {
        return build(groupId, "NDEATH", edgeNodeId, null);
    }

    public static String nodeCommand(String groupId, String edgeNodeId) {
        return build(groupId, "NCMD", edgeNodeId, null);
    }

    public static String deviceCommand(String groupId, String edgeNodeId, String deviceId) {
        return build(groupId, "DCMD", edgeNodeId, deviceId);
    }

    public static String deviceCommandWildcard(String groupId, String edgeNodeId) {
        return "spBv1.0/" + groupId + "/DCMD/" + edgeNodeId + "/+";
    }

    private static String build(String groupId, String messageType, String edgeNodeId, String deviceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("spBv1.0/");
        sb.append(groupId);
        sb.append("/");
        sb.append(messageType);
        sb.append("/");
        sb.append(edgeNodeId);
        if (deviceId != null && !deviceId.isEmpty()) {
            sb.append("/");
            sb.append(deviceId);
        }
        return sb.toString();
    }
}
