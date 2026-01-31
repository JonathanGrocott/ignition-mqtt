import { ApiResponse, MqttBrokerConfig, SparkplugPublishConfig } from './types';

const BASE_URL = '/data/mqtt-sparkplug-publisher';

export async function getBrokerConfig(): Promise<ApiResponse<MqttBrokerConfig[]>> {
    const response = await fetch(`${BASE_URL}/config/broker`);
    return response.json();
}

export async function saveBrokerConfig(config: MqttBrokerConfig): Promise<ApiResponse<MqttBrokerConfig>> {
    const response = await fetch(`${BASE_URL}/config/broker`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
    });
    return response.json();
}

export async function deleteBroker(id: number): Promise<ApiResponse<null>> {
    const response = await fetch(`${BASE_URL}/config/broker?id=${id}`, {
        method: 'DELETE'
    });
    return response.json();
}

export async function getPublishConfig(): Promise<ApiResponse<SparkplugPublishConfig[]>> {
    const response = await fetch(`${BASE_URL}/config/publish`);
    return response.json();
}

export async function savePublishConfig(config: SparkplugPublishConfig): Promise<ApiResponse<SparkplugPublishConfig>> {
    const response = await fetch(`${BASE_URL}/config/publish`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
    });
    return response.json();
}
