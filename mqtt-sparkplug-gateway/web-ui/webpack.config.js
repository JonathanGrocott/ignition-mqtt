const path = require('path');

module.exports = {
    entry: './src/index.tsx',
    mode: 'production',
    output: {
        filename: 'sparkplug-config.js',
        path: path.resolve(__dirname, '../src/main/resources/mounted'),
        library: {
            name: 'com.inductiveautomation.mqtt.sparkplugb.gateway',
            type: 'umd',
            umdNamedDefine: true,
            export: 'default'
        },
        globalObject: 'this'
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
    externals: {
        'react': '/res/sys/js/react.js',
        'react-dom': '/res/sys/js/react-dom.js'
    },
    devtool: 'source-map',
    optimization: {
        minimize: false
    }
};
