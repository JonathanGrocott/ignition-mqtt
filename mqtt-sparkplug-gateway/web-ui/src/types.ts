export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
}

export interface MqttBrokerConfig {
    id?: number;
    name: string;
    brokerUrl: string;
    clientId: string;
    username?: string;
    password?: string;
    useTls: boolean;
    qos: number;
    retained: boolean;
    cleanSession: boolean;
    connectionTimeout: number;
    keepAliveInterval: number;
    enabled: boolean;
}

export interface SparkplugDeviceMapping {
    id?: string;
    sourcePattern: string;
    groupId?: string;
    edgeNodeId?: string;
    deviceId: string;
    enabled: boolean;
}

export interface SparkplugPublishConfig {
    id?: number;
    name: string;
    enabled: boolean;
    brokerId: number;
    groupId?: string;
    edgeNodeId?: string;
    deviceMappings: SparkplugDeviceMapping[];
}
