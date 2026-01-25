/**
 * Type definitions for MQTT UNS Publisher configuration and API responses
 */

export interface MqttBrokerConfig {
    id?: number;
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

export interface MqttTagConfig {
    id?: number;
    name: string;
    enabled: boolean;
    tagProviders: string[];
    tagFolders: string[];
    topicMappings: TopicMapping[];  // New: Custom topic prefix mappings
    topicOverrides: Record<string, string>;
    payloadTemplate?: string;
    includeMetadata: boolean;
    valueDeadband: number;
    publishOnQualityChange: boolean;
}

export interface TopicMapping {
    id?: string;  // For UI tracking
    sourcePattern: string;  // Tag pattern like "[default]TestTags" or "[default]"
    topicPrefix: string;    // UNS topic prefix like "enterprise/site1/area2"
    enabled: boolean;
}

export interface ModuleStatus {
    healthy: boolean;
    healthLevel: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
    statusMessage: string;
    connectionState?: string;
    connectionStateDisplay?: string;
    brokerUrl?: string;
    reconnectAttempts?: number;
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
}

export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
}

export interface TestConnectionRequest {
    brokerUrl: string;
    clientId: string;
    username?: string;
    password?: string;
    useTls?: boolean;
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
    errorCode?: number;
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
