/**
 * Type definitions for MQTT UNS Publisher configuration and API responses
 */

export interface MqttBrokerConfig {
    id?: number;
    name: string;
    brokerUrl: string;
    clientId: string;
    username?: string;
    password?: string;
    hasPassword?: boolean;
    useTls: boolean;
    tlsTrustMode: 'SYSTEM_DEFAULT' | 'UPLOADED_CA';
    caCertificatePem?: string;
    caCertificateConfigured?: boolean;
    caCertificates?: TlsCertificateInfo[];
    caCertificateError?: string;
    removeCaCertificate?: boolean;
    qos: number;
    retained: boolean;
    cleanSession: boolean;
    connectionTimeout: number;
    keepAliveInterval: number;
    slowReconnectIntervalSeconds: number;
    enabled: boolean;
}

export interface TlsCertificateInfo {
    subject: string;
    issuer: string;
    serialNumber: string;
    sha256Fingerprint: string;
    notBefore: string;
    notAfter: string;
    currentlyValid: boolean;
}

export interface MqttTagConfig {
    id?: number;
    name: string;
    enabled: boolean;
    tagProviders: string[];
    tagFolders: string[];
    topicMappings: TopicMapping[];  // New: Custom topic prefix mappings
    topicOverrides: Record<string, string>;
    payloadFields?: PayloadFieldConfig;
    includeMetadata: boolean;
    valueDeadband: number;
    publishOnQualityChange: boolean;
}

export interface PayloadFieldConfig {
    includeQuality: boolean;
    includeQualityCode: boolean;
    includeTagPath: boolean;
    properties: Record<string, boolean>;
}

export interface TopicMapping {
    id?: string;  // For UI tracking
    brokerId: number;  // Which broker this mapping publishes to
    sourcePattern: string;  // Tag pattern like "[default]TestTags" or "[default]"
    topicPrefix: string;    // UNS topic prefix like "enterprise/site1/area2"
    enabled: boolean;
    preserveTopicCase?: boolean;
    publishMode?: 'PER_TAG_TOPIC' | 'SINGLE_TOPIC';
    batchWindowMs?: number;
    maxBatchSize?: number;
    useDefaultPayloadFields?: boolean;
    payloadFields?: PayloadFieldConfig;
}

export interface ModuleStatus {
    healthy: boolean;
    healthLevel: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
    statusMessage: string;
    connectionState?: string;
    connectionStateDisplay?: string;
    activeBrokers: number;  // Number of brokers currently connected
    totalBrokers: number;   // Total number of brokers configured in database
    statistics?: ModuleStatistics;  // Optional - may be undefined during initialization
    monitoredTagCount: number;
}

export interface ModuleStatistics {
    messagesPublished: number;
    messagesFailed: number;
    publishSuccessRate: number;
    tagReadsSuccessful: number;
    tagReadsFailed: number;
    tagReadSuccessRate: number;
    uptimeMs: number;
    uptimeDisplay: string;
    batchMessagesPublished?: number;
    batchMetricsPublished?: number;
    batchFlushes?: number;
    batchMaxSize?: number;
}

export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
}

export interface TestConnectionRequest {
    id?: number;
    brokerUrl: string;
    clientId: string;
    username?: string;
    password?: string;
    useTls?: boolean;
    tlsTrustMode?: 'SYSTEM_DEFAULT' | 'UPLOADED_CA';
    caCertificatePem?: string;
    removeCaCertificate?: boolean;
    connectionTimeout?: number;
    keepAliveInterval?: number;
    cleanSession?: boolean;
}

export interface TestConnectionResult {
    connected: boolean;
    connectionTimeMs?: number;
    brokerUrl?: string;
    message?: string;
    error?: string;
    errorCode?: string;
}

export interface ActiveTagSubscription {
    tagPath: string;
    provider: string;
    folder: string;
    mqttTopic: string;
    lastPublished?: number;
    publishCount: number;
    currentValue?: any;
    quality?: string;
}

export interface SubscriptionStatus {
    totalTags: number;
    activeSubscriptions: ActiveTagSubscription[];
    topicMappings: Record<string, string>;  // sourcePattern -> topicPrefix
}
