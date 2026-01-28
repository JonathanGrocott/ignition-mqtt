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
        console.log('[API] Fetching:', url);
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options?.headers
            }
        });

        console.log('[API] Response status:', response.status, response.statusText);
        console.log('[API] Content-Type:', response.headers.get('content-type'));
        
        // Get the raw text first to see what we're dealing with
        const text = await response.text();
        console.log('[API] Raw response text:', text);
        console.log('[API] Response text length:', text.length);
        
        // Handle empty response (e.g., 204 No Content or empty body)
        if (!text || text.trim().length === 0) {
            console.warn('[API] Empty response body received');
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
            console.log('[API] Parsed data:', data);
        } catch (parseError) {
            console.error('[API] JSON parse error:', parseError);
            console.error('[API] Failed to parse text:', text);
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
        console.error('[API] apiFetch error:', error);
        return {
            success: false,
            error: error instanceof Error ? error.message : 'Network error'
        };
    }
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
    return apiFetch<MqttBrokerConfig>(`${API_BASE}/config/broker/${id}`);
}

/**
 * Save broker configuration (create new or update existing)
 */
export async function saveBrokerConfig(config: MqttBrokerConfig): Promise<ApiResponse<MqttBrokerConfig>> {
    return apiFetch<MqttBrokerConfig>(`${API_BASE}/config/broker`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
}

/**
 * Delete a broker by ID
 */
export async function deleteBroker(id: number): Promise<ApiResponse<void>> {
    return apiFetch<void>(`${API_BASE}/config/broker/${id}`, {
        method: 'DELETE'
    });
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
export async function saveTagConfig(config: MqttTagConfig): Promise<ApiResponse<MqttTagConfig>> {
    return apiFetch<MqttTagConfig>(`${API_BASE}/config/tags`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
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
export async function testConnection(config: TestConnectionRequest): Promise<ApiResponse<TestConnectionResult>> {
    return apiFetch<TestConnectionResult>(`${API_BASE}/test-connection`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
}

/**
 * Get all configuration (brokers + tags)
 */
export async function getAllConfig(): Promise<ApiResponse<{ brokers: MqttBrokerConfig[], tags: MqttTagConfig | null }>> {
    return apiFetch(`${API_BASE}/config`);
}
