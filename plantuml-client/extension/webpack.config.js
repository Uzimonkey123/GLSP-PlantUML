'use strict';

const path = require("path");
const { merge } = require("webpack-merge");
const CopyPlugin = require("copy-webpack-plugin");
const common = require("../webpack.base.js");

module.exports = merge(common, {
  target: "node",
  mode: "development",
  entry: path.resolve(__dirname, "src/extension.ts"),
  output: {
    path: path.resolve(__dirname, "dist"),
    filename: "extension.js",
    libraryTarget: "commonjs2"
  },
  devtool: 'source-map',
  externals: {
    vscode: "commonjs vscode",
    ws: "commonjs ws"
  },
  plugins: [
    new CopyPlugin({
      patterns: [
        {
          from: path.resolve(__dirname, '..', 'webview', 'dist')
        }
      ]
    })
  ],
  ignoreWarnings: [/Can't resolve .* in '.*ws\/lib'/],
  performance: {
    hints: false
  }
});