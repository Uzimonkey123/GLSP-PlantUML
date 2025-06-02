const path = require("path");
const { merge } = require("webpack-merge");
const webpack = require("webpack");
const CircularDependencyPlugin = require("circular-dependency-plugin");
const common = require("../webpack.base.js");

const outputPath = path.resolve(__dirname, "dist");

module.exports = merge(common, {
  target: "web",
  entry: path.resolve(__dirname, "src/index.ts"),
  output: {
    filename: "webview.js",
    path: outputPath
  },
  mode: process.env.NODE_ENV === "production" ? "production" : "development",
  devtool: process.env.NODE_ENV === "production" ? "source-map" : "eval-source-map",
  resolve: {
    // The common part already defined extensions & CSS; here we just add fallbacks/aliases
    extensions: [".ts", ".tsx", ".js", ".css"],
    alias: {
      process: "process/browser"
    },
    fallback: {
      fs: false,
      net: false
    }
  },
  plugins: [
    new webpack.ProvidePlugin({
      process: "process/browser"
    }),
    new CircularDependencyPlugin({
      exclude: /node_modules/,
      failOnError: false
    })
  ],
  ignoreWarnings: [
    /Failed to parse source map/,
    /Can't resolve .* in '.*ws\/lib'/
  ],
  performance: {
    hints: false
  }
});