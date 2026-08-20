/**
 * API client for MQTT UNS Publisher configuration and status endpoints
 */

import {
    ApiResponse,
    MqttBrokerConfig,
    MqttTagConfig,
    ModuleStatus,
    TestConnectionRequest,
    TestConnectionResult
} from './types';

// Data routes use the mount path alias (from getMountPathAlias)
const API_BASE = '/data/mqtt-uns-publisher';

/**
 * Generic fetch wrapper with error handling
 */
async function apiFetch<T>(url: string, options?: RequestInit): Promise<ApiResponse<T>> {
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options?.headers
            }
        });

        const text = await response.text();
        
        // Handle empty response (e.g., 204 No Content or empty body)
        if (!text || text.trim().length === 0) {
            if (!response.ok) {
                return {
                    success: false,
                    error: `HTTP ${response.status}: ${response.statusText} (empty response body)`
                };
            }
            // Empty response but status OK - return generic success
            return {
                success: true,
                data: undefined as any
            };
        }
        
        // Try to parse it
        let data;
        try {
            data = JSON.parse(text);
        } catch (parseError) {
            return {
                success: false,
                error: `Invalid JSON response: ${parseError instanceof Error ? parseError.message : 'Parse error'}`
            };
        }

        if (!response.ok) {
            return {
                success: false,
                error: data.error || `HTTP ${response.status}: ${response.statusText}`
            };
        }

        return data as ApiResponse<T>;

    } catch (error) {
        return {
            success: false,
            error: error instanceof Error ? error.message : 'Network error'
        };
    }
}

function writeOptions(method: 'POST' | 'DELETE', csrfToken: string, body?: string): RequestInit {
    return {
        method,
        headers: {
            'X-CSRF-Token': csrfToken
        },
        body
    };
}

/**
 * Get all broker configurations
 */
export async function getBrokerConfig(): Promise<ApiResponse<MqttBrokerConfig[]>> {
    return apiFetch<MqttBrokerConfig[]>(`${API_BASE}/config/broker`);
}

/**
 * Get a specific broker configuration by ID
 */
export async function getBrokerById(id: number): Promise<ApiResponse<MqttBrokerConfig>> {
    return apiFetch<MqttBrokerConfig>(`${API_BASE}/config/broker?id=${id}`);
}

/**
 * Save broker configuration (create new or update existing)
 */
export async function saveBrokerConfig(config: MqttBrokerConfig, csrfToken: string): Promise<ApiResponse<MqttBrokerConfig>> {
    return apiFetch<MqttBrokerConfig>(
        `${API_BASE}/config/broker`,
        writeOptions('POST', csrfToken, JSON.stringify(config))
    );
}

/**
 * Delete a broker by ID
 */
export async function deleteBroker(id: number, csrfToken: string): Promise<ApiResponse<void>> {
    return apiFetch<void>(`${API_BASE}/config/broker?id=${id}`, writeOptions('DELETE', csrfToken));
}

/**
 * Get tag configuration
 */
export async function getTagConfig(): Promise<ApiResponse<MqttTagConfig>> {
    return apiFetch<MqttTagConfig>(`${API_BASE}/config/tags`);
}

/**
 * Save tag configuration
 */
export async function saveTagConfig(config: MqttTagConfig, csrfToken: string): Promise<ApiResponse<MqttTagConfig>> {
    return apiFetch<MqttTagConfig>(
        `${API_BASE}/config/tags`,
        writeOptions('POST', csrfToken, JSON.stringify(config))
    );
}

/**
 * Get module status and statistics
 */
export async function getModuleStatus(): Promise<ApiResponse<ModuleStatus>> {
    return apiFetch<ModuleStatus>(`${API_BASE}/status`);
}

/**
 * Test MQTT broker connection
 */
export async function testConnection(config: TestConnectionRequest, csrfToken: string): Promise<ApiResponse<TestConnectionResult>> {
    return apiFetch<TestConnectionResult>(
        `${API_BASE}/test-connection`,
        writeOptions('POST', csrfToken, JSON.stringify(config))
    );
}

/**
 * Get all configuration (brokers + tags)
 */
export async function getAllConfig(): Promise<ApiResponse<{ brokers: MqttBrokerConfig[], tags: MqttTagConfig | null }>> {
    return apiFetch(`${API_BASE}/config`);
}
