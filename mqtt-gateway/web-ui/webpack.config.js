const path = require('path');

module.exports = {
    entry: './src/index.tsx',
    mode: 'production',
    output: {
        filename: 'mqtt-config.js',
        path: path.resolve(__dirname, '../src/main/resources/mounted'),
        // UMD format - no external dependencies, self-contained
        library: {
            name: 'com.inductiveautomation.mqtt.uns.gateway',
            type: 'umd',
            umdNamedDefine: true,  // Creates: define("com.inductiveautomation.mqtt.uns.gateway", [], factory)
            export: 'default'      // Export the default export from index.tsx
        },
        globalObject: 'this'  // For UMD compatibility
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: {
                    loader: 'ts-loader',
                    options: {
                        compilerOptions: {
                            module: 'esnext'
                        }
                    }
                },
                exclude: /node_modules/
            },
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            }
        ]
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js', '.jsx']
    },
    // Use Ignition's React/ReactDOM from SystemJS
    // Use relative URLs so the module works on any hostname/IP address
    // These resolve to the current gateway's hostname automatically
    externals: {
        'react': '/res/sys/js/react.js',
        'react-dom': '/res/sys/js/react-dom.js'
    },
    devtool: 'source-map',
    optimization: {
        minimize: false  // Don't minify to make debugging easier
    }
};
