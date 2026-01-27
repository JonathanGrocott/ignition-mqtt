/**
 * Browser console test script for MQTT module POST endpoints
 * 
 * Usage:
 * 1. Install the new module build: MQTT-UNS-Publisher.unsigned.modl
 * 2. Open browser to Gateway config page
 * 3. Paste this entire script in browser console
 * 4. Run: testConnection()
 */

async function testConnection() {
    console.log('=== Testing POST /data/mqtt-uns-publisher/test-connection ===');
    
    const testData = {
        brokerUrl: 'tcp://localhost:1883',
        clientId: 'test-client-' + Date.now(),
        username: '',
        password: '',
        useTls: false,
        connectionTimeout: 30,
        keepAliveInterval: 60,
        cleanSession: true
    };
    
    console.log('Request payload:', testData);
    
    try {
        const response = await fetch('/data/mqtt-uns-publisher/test-connection', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(testData)
        });
        
        console.log('Response status:', response.status, response.statusText);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));
        
        const contentType = response.headers.get('content-type');
        console.log('Content-Type:', contentType);
        
        const text = await response.text();
        console.log('Raw response text:', text);
        console.log('Text length:', text.length);
        
        if (text) {
            try {
                const json = JSON.parse(text);
                console.log('✅ Parsed JSON:', json);
                
                if (json.success && json.data) {
                    console.log('✅ SUCCESS! Test connection response:', json.data);
                    return json;
                } else {
                    console.error('❌ API returned error:', json.error || json);
                    return json;
                }
            } catch (parseError) {
                console.error('❌ JSON parse error:', parseError);
                console.error('Failed to parse text:', text.substring(0, 500));
                return { error: 'JSON parse failed', text };
            }
        } else {
            console.error('❌ Empty response body');
            return { error: 'Empty response' };
        }
        
    } catch (error) {
        console.error('❌ Fetch error:', error);
        return { error: error.message };
    }
}

async function testGetConfig() {
    console.log('=== Testing GET /data/mqtt-uns-publisher/config/broker ===');
    
    try {
        const response = await fetch('/data/mqtt-uns-publisher/config/broker');
        console.log('Response status:', response.status);
        
        const text = await response.text();
        console.log('Raw response:', text);
        
        const json = JSON.parse(text);
        console.log('✅ Parsed JSON:', json);
        return json;
        
    } catch (error) {
        console.error('❌ Error:', error);
        return { error: error.message };
    }
}

async function testSaveConfig() {
    console.log('=== Testing POST /data/mqtt-uns-publisher/config/broker ===');
    
    const config = {
        brokerUrl: 'tcp://test.mosquitto.org:1883',
        clientId: 'ignition-test',
        username: '',
        password: '',
        useTls: false,
        qos: 1,
        retained: false,
        cleanSession: true,
        connectionTimeout: 30,
        keepAliveInterval: 60,
        enabled: false
    };
    
    console.log('Saving config:', config);
    
    try {
        const response = await fetch('/data/mqtt-uns-publisher/config/broker', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });
        
        console.log('Response status:', response.status);
        const text = await response.text();
        console.log('Raw response:', text);
        
        const json = JSON.parse(text);
        console.log('✅ Parsed JSON:', json);
        return json;
        
    } catch (error) {
        console.error('❌ Error:', error);
        return { error: error.message };
    }
}

console.log('✅ Test functions loaded!');
console.log('Available commands:');
console.log('  testConnection()  - Test MQTT connection endpoint');
console.log('  testGetConfig()   - Get broker configuration');
console.log('  testSaveConfig()  - Save broker configuration');
